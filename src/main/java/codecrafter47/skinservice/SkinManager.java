package codecrafter47.skinservice;

import codecrafter47.skinservice.database.Database;
import codecrafter47.skinservice.database.MinecraftAccount;
import codecrafter47.skinservice.util.ImageWrapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.janmm14.minecraftchangeskin.api.SkinChangeParams;
import de.janmm14.minecraftchangeskin.api.SkinChanger;
import de.janmm14.minecraftchangeskin.api.SkinChangerResult;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class SkinManager {

    private final Database database;
    private final MojangAPI mojangAPI;
    private final StatsTracker statsTracker;
    private final BlockingQueue<SkinRequest> queue = new LinkedBlockingQueue<>();
    private final List<SkinRequest> secondaryQueue = Collections.synchronizedList(new LinkedList<>());

    private final Cache<ImageWrapper, SkinRequest> requestMap =
            CacheBuilder.newBuilder()
                    .concurrencyLevel(Runtime.getRuntime().availableProcessors())
                    .expireAfterAccess(30, TimeUnit.MINUTES).build();

    private final AtomicInteger accounts = new AtomicInteger(0);

    @Inject
    public SkinManager(Database database, MojangAPI mojangAPI, StatsTracker statsTracker) {
        this.database = database;
        this.mojangAPI = mojangAPI;
        this.statsTracker = statsTracker;
        for (MinecraftAccount minecraftAccount : database.getAccounts()) {
            new SkinUpdater(minecraftAccount);
            accounts.getAndIncrement();
        }
    }

    public int getQueueSize() {
        return queue.size() + secondaryQueue.size();
    }

    @Synchronized
    @SneakyThrows
    public SkinRequest requestSkin(ImageWrapper image, String ip) {
        return requestMap.get(image, () -> {
            SkinRequest skinRequest = new SkinRequest(image, ip);

            SkinInfo skinInfo = database.getSkin(image);
            if (skinInfo != null) {
                skinRequest.setResult(skinInfo);
                skinRequest.setFinished(true);
                return skinRequest;
            }

            requestMap.put(image, skinRequest);
            secondaryQueue.add(skinRequest);
            updateQueues();
            return skinRequest;
        });
    }

    @Synchronized
    private void updateQueues() {
        Set<String> ipBlackList = queue.stream().map(SkinRequest::getIp).distinct().collect(Collectors.toSet());
        for (Iterator<SkinRequest> iterator = secondaryQueue.iterator(); iterator.hasNext(); ) {
            SkinRequest skinRequest = iterator.next();
            if (!ipBlackList.contains(skinRequest.getIp())) {
                ipBlackList.add(skinRequest.getIp());
                queue.add(skinRequest);
                iterator.remove();
            }
        }

        int timeLeft = 1;
        int accounts = this.accounts.get();
        for (SkinRequest skinRequest : queue) {
            skinRequest.setTimeLeft(accounts > 0 ? (timeLeft++)/ accounts : Integer.MAX_VALUE);
        }
        for (SkinRequest skinRequest : secondaryQueue) {
            skinRequest.setTimeLeft(accounts > 0 ? (timeLeft++)/ accounts : Integer.MAX_VALUE);
        }
    }

    private class SkinUpdater implements Runnable {
        private final MinecraftAccount account;
        private final Thread thread;
        private boolean stop = false;
        private long lastSkinRequest = System.currentTimeMillis();

        private SkinUpdater(MinecraftAccount account) {
            this.account = account;
            thread = new Thread(this, "Skin Update Task for " + account.getUuid());
            thread.start();
        }

        @Override
        @SneakyThrows
        public void run() {
            while (!stop) {
                SkinRequest skinRequest = queue.take();
                updateQueues();
                try {
                    createSkin(skinRequest);
                } catch (Throwable th) {
                    log.error("Unexpected Exception", th);
                    skinRequest.setError(true);
                    skinRequest.setFinished(true);
                    accounts.getAndDecrement();
                    Thread.sleep(900000);
                    accounts.getAndIncrement();
                }
            }
            accounts.getAndDecrement();
        }

        private void createSkin(SkinRequest skinRequest) throws InterruptedException, IOException {
            // save image
            File tempFile = File.createTempFile("skin", "png");
            ImageIO.write(skinRequest.getImage().getImage(), "png", tempFile);

            // change skin
            SkinChangeParams params = SkinChangeParams.Builder.create()
                    .email(account.getEmail())
                    .password(account.getPassword())
                    .image(tempFile)
                    .build();

            AtomicBoolean finished = new AtomicBoolean(false);
            AtomicReference<SkinChangerResult> result = new AtomicReference<>();

            SkinChanger.changeSkin(params, (skinChangerResult, throwable) -> {
                result.set(skinChangerResult);
                finished.set(true);
                if (throwable != null) {
                    log.error("Error fetching skin " + skinRequest, throwable);
                }
            });

            // wait until skin is changed
            while (!finished.get()) {
                Thread.sleep(1000);
            }
            tempFile.delete();

            SkinChangerResult skinChangerResult = result.get();
            if (skinChangerResult == null) {
                log.error("SkinChangerResult is null " + skinRequest);
                stop = true;
                skinRequest.setError(true);
                return;
            }

            if (skinChangerResult == SkinChangerResult.SECURITY_QUESTIONS) {
                log.error("Security questions for " + account);
                stop = true;
                skinRequest.setError(true);
                return;
            }

            if (skinChangerResult == SkinChangerResult.UNKNOWN_ERROR) {
                log.error("Unknown error " + account);
                stop = true;
                skinRequest.setError(true);
                return;
            }

            // wait 25 seconds for mojang servers to update skin
            Thread.sleep(25000);

            // check whether we are allowed to fetch data for this skin again?
            while (System.currentTimeMillis() - lastSkinRequest < 60000) {
                Thread.sleep(1000);
            }

            // fetch new skin
            SkinInfo newSkin = mojangAPI.fetchSkin(account.getUuid());

            lastSkinRequest = System.currentTimeMillis();

            BufferedImage image = ImageIO.read(new URL(newSkin.getSkinURL()));

            if (!skinRequest.getImage().equals(new ImageWrapper(image))) {
                log.error("Skin is different from requested skin!");
                skinRequest.setError(true);
                return;
            }

            skinRequest.setResult(newSkin);
            skinRequest.setFinished(true);
            database.saveSkin(skinRequest.getImage(), newSkin.getSkinURL(), newSkin.getTexturePropertyValue(), newSkin.getTexturePropertySignature());
            statsTracker.onMojangRequest();
        }
    }
}
