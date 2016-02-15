package codecrafter47.skinservice;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;

@Slf4j
@Singleton
public class StatsTracker {
    private final SkinManager skinManager;

    @Inject
    public StatsTracker(SkinManager skinManager) {
        this.skinManager = skinManager;
        new Thread(this::monitorQueueStats, "Stats Tracker Thread 1").start();
        new Thread(this::printStats, "Stats Tracker Thread 2").start();
    }

    private int maxQueueLength = 0;
    private TIntIntMap queueSize = new TIntIntHashMap(20, 0.5f, -1, 0);
    private int requestsServed = 0;
    private int cachedRequests = 0;
    private int mojangRequests = 0;

    public void onRequest() {
        synchronized (this) {
            requestsServed++;
        }
    }

    public void onCachedRequest() {
        synchronized (this) {
            cachedRequests++;
        }
    }

    public void onMojangRequest() {
        synchronized (this) {
            mojangRequests++;
        }
    }

    @SneakyThrows
    private void monitorQueueStats() {
        while (true) {
            Thread.sleep(60000);
            synchronized (this) {
                maxQueueLength = Math.max(maxQueueLength, skinManager.getQueueSize());
                queueSize.increment(skinManager.getQueueSize());
            }
        }
    }

    @SneakyThrows
    private void printStats() {
        Thread.sleep(300000);
        while (true) {
            synchronized (this) {
                log.info("Stats:");
                log.info("Max. queue length: " + maxQueueLength);
                log.info("Avg. queue length: " + Arrays.stream(queueSize.keys()).mapToDouble(size -> size * queueSize.get(size)).sum() / Arrays.stream(queueSize.values()).sum());
                log.info("Requests: " + requestsServed);
                log.info("Served from cache: " + cachedRequests);
                log.info("from Mojang: " + mojangRequests);

                maxQueueLength = 0;
                queueSize.clear();
                requestsServed = 0;
                cachedRequests = 0;
                mojangRequests = 0;
            }
            Thread.sleep(24 * 3600 * 1000);
        }
    }
}
