package codecrafter47.skinservice;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class StatsTracker {
    private final static int MAX_TIME = 10080;

    private final Provider<SkinManager> skinManager;

    @Inject
    public StatsTracker(Provider<SkinManager> skinManager) {
        this.skinManager = skinManager;
        new Thread(this::monitorQueueStats, "Stats Tracker Thread 1").start();
    }

    private int[] queueSize = new int[MAX_TIME];
    private int[] mojangRequests = new int[MAX_TIME];
    private int index = 0;

    public void onMojangRequest() {
        synchronized (this) {
            mojangRequests[index]++;
        }
    }

    @SneakyThrows
    private void monitorQueueStats() {
        while (true) {
            Thread.sleep(60000);
            synchronized (this) {
                queueSize[index] = skinManager.get().getQueueSize();
                index++;
                if (index >= MAX_TIME) {
                    index = 0;
                }
                queueSize[index] = 0;
                mojangRequests[index] = 0;
            }
        }
    }

    public int getMaxQueueSize(int minutes) {
        int maxQueueSize = 0;
        for (int i = 0; i < minutes; i++) {
            int i1 = index - i;
            if (i1 < 0) {
                i1 = i1 + MAX_TIME;
            }
            maxQueueSize = Math.max(maxQueueSize, queueSize[i1]);
        }
        return maxQueueSize;
    }

    public int getAvrgQueueSize(int minutes) {
        int avrgQueueSize = 0;
        for (int i = 0; i < minutes; i++) {
            int i1 = index - i;
            if (i1 < 0) {
                i1 = i1 + MAX_TIME;
            }
            avrgQueueSize += queueSize[i1];
        }
        return avrgQueueSize / minutes;
    }

    public int getMojangRequests(int minutes) {
        int result = 0;
        for (int i = 0; i < minutes; i++) {
            int i1 = index - i;
            if (i1 < 0) {
                i1 = i1 + MAX_TIME;
            }
            result += mojangRequests[i1];
        }
        return result;
    }
}
