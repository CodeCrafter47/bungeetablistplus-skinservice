package codecrafter47.skinservice;

import codecrafter47.skinservice.database.MinecraftAccount;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import de.janmm14.minecraftchangeskin.api.Callback;
import de.janmm14.minecraftchangeskin.api.SkinChangeParams;
import de.janmm14.minecraftchangeskin.api.SkinChanger;
import de.janmm14.minecraftchangeskin.api.SkinChangerResult;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class SkinManager {

    private final ThreadLocal<Gson> gson = new ThreadLocal<Gson>() {
        @Override
        protected Gson initialValue() {
            return new Gson();
        }
    };

    private final Database database;
    private final MojangAPI mojangAPI;
    private final Provider<StatsTracker> statsTrackerProvider;
    private final BlockingQueue<SkinRequest> queue = new LinkedBlockingQueue<>();
    private final List<SkinRequest> secondaryQueue = Collections.synchronizedList(new LinkedList<>());
    private final Cache<Head, SkinRequest> requestedHeads = CacheBuilder.newBuilder().expireAfterAccess(120, TimeUnit.MINUTES).build();
    private int accounts = 0;

    @Inject
    public SkinManager(Database database, MojangAPI mojangAPI, Provider<StatsTracker> statsTrackerProvider) {
        this.database = database;
        this.mojangAPI = mojangAPI;
        this.statsTrackerProvider = statsTrackerProvider;
        for (MinecraftAccount minecraftAccount : database.getAccounts()) {
            new SkinUpdater(minecraftAccount);
            accounts++;
        }
    }

    public int getQueueSize() {
        return queue.size() + secondaryQueue.size();
    }

    @Synchronized
    @SneakyThrows
    public SkinRequest requestSkin(byte[] head, String ip) {
        return requestedHeads.get(Head.of(head), () -> {
            SkinRequest request = new SkinRequest(head, ip);
            codecrafter47.skinservice.database.Head head1 = database.getHead(head);
            if (head1 != null) {
                request.setResult(new Skin(head1.getSkin(), head1.getSignature()));
                request.setFinished(true);
                return request;
            }
            secondaryQueue.add(request);
            updateQueues();
            return request;
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
        for (SkinRequest skinRequest : queue) {
            skinRequest.setTimeLeft(accounts > 0 ? (timeLeft++)/accounts : Integer.MAX_VALUE);
        }
        for (SkinRequest skinRequest : secondaryQueue) {
            skinRequest.setTimeLeft(accounts > 0 ? (timeLeft++)/accounts : Integer.MAX_VALUE);
        }
    }

    @Data
    @RequiredArgsConstructor
    public class SkinRequest {
        private final byte[] head;
        private final String ip;
        private boolean finished = false;
        private boolean error = false;
        private int timeLeft = 1;
        private Skin result = null;
    }

    private static final class Head {
        private final byte[] bytes;

        private Head(byte[] bytes) {
            this.bytes = bytes;
        }

        static Head of(byte[] bytes) {
            return new Head(bytes);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(bytes);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Head && Arrays.equals(bytes, ((Head) obj).bytes);
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
                    stop = true;
                    skinRequest.setError(true);
                }
            }
            accounts--;
        }

        private void createSkin(SkinRequest skinRequest) throws InterruptedException, IOException {
            // save image
            int[] rgb = new int[64];
            ByteBuffer.wrap(skinRequest.getHead()).asIntBuffer().get(rgb);

            BufferedImage skin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            skin.setRGB(8, 8, 8, 8, rgb, 0, 8);
            File tempFile = File.createTempFile("skin", "png");
            ImageIO.write(skin, "png", tempFile);

            // change skin
            SkinChangeParams params = SkinChangeParams.Builder.create()
                    .email(account.getEmail())
                    .password(account.getPassword())
                    .image(tempFile)
                    .build();

            AtomicBoolean finished = new AtomicBoolean(false);
            AtomicReference<SkinChangerResult> result = new AtomicReference<>();

            SkinChanger.changeSkin(params, new Callback<SkinChangerResult>() {
                @Override
                public void done(@Nullable SkinChangerResult skinChangerResult, @Nullable Throwable throwable) {
                    result.set(skinChangerResult);
                    finished.set(true);
                    if (throwable != null) {
                        log.error("Error fetching skin " + skinRequest, throwable);
                    }
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
            while (System.currentTimeMillis() - lastSkinRequest < 90000) {
                Thread.sleep(1000);
            }

            // fetch new skin
            Skin newSkin = mojangAPI.fetchSkin(account.getUuid());

            String json = new String(Base64.getDecoder().decode(newSkin.getSkin()));
            Map<String, Object> map = gson.get().fromJson(json, (Type) LinkedTreeMap.class);
            String skinURL = (String) ((Map) ((Map) map.get("textures")).get("SKIN")).get("url");
            BufferedImage image = ImageIO.read(new URL(skinURL));
            BufferedImage head = image.getSubimage(8, 8, 8, 8);
            int[] rgb2 = head.getRGB(0, 0, 8, 8, null, 0, 8);

            if (!Arrays.equals(rgb, rgb2)) {
                log.error("Skin is different from requested skin!");
                skinRequest.setError(true);
                return;
            }

            skinRequest.setResult(newSkin);
            skinRequest.setFinished(true);
            database.saveHead(skinRequest.getHead(), newSkin.getSkin(), newSkin.getSignature());
            statsTrackerProvider.get().onMojangRequest();
        }
    }
}
