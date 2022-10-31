package com.zyk.raft.kv.current;

import com.alipay.remoting.NamedThreadFactory;

import java.util.concurrent.*;

public class RaftThreadPool {
    private static final int CUP = Runtime.getRuntime().availableProcessors();

    private static final int MAX_POOL_SIZE = CUP * 2;

    private static final int QUEUE_SIZE = 1024;
    private static final long KEEP_TIME = 1000 * 60;
    private static TimeUnit KEEP_TIME_UNIT = TimeUnit.MILLISECONDS;


    private static final ScheduledExecutorService scheduledExecutorService = getScheduled();

    private static ThreadPoolExecutor threadPoolExecutor = getThreadPool();

    private static ThreadPoolExecutor getThreadPool() {
        return new RaftThreadPoolExecutor(CUP, MAX_POOL_SIZE, KEEP_TIME, KEEP_TIME_UNIT,
                new LinkedBlockingQueue<>(QUEUE_SIZE), new NameThreadFactory());
    }

    private static ScheduledExecutorService getScheduled() {
        return new ScheduledThreadPoolExecutor(CUP, new NamedThreadFactory());
    }


    public static void scheduleAtFixedRate(Runnable r, long initDelay, long delay) {
        scheduledExecutorService.scheduleAtFixedRate(r, initDelay, delay, TimeUnit.MILLISECONDS);
    }


    public static void scheduleWithFixedDelay(Runnable r, long delay) {
        scheduledExecutorService.scheduleWithFixedDelay(r, 0, delay, TimeUnit.MILLISECONDS);
    }


    @SuppressWarnings("unchecked")
    public static <T> Future<T> submit(Callable r) {
        return threadPoolExecutor.submit(r);
    }

    public static void execute(Runnable r) {
        threadPoolExecutor.execute(r);
    }

    public static void execute(Runnable r, boolean sync) {
        if (sync) {
            r.run();
        } else {
            threadPoolExecutor.execute(r);
        }
    }

    static class NameThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new RaftThread("Raft thread", r);
            t.setDaemon(true);
            t.setPriority(5);
            return t;
        }
    }
}
