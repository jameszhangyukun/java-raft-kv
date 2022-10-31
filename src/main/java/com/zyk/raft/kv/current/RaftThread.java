package com.zyk.raft.kv.current;


import org.slf4j.LoggerFactory;
import org.slf4j.Logger;


public class RaftThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(RaftThread.class);
    private static final UncaughtExceptionHandler uncaughtExceptionHandler = (t, e)
            -> LOGGER.warn("Exception occurred from thread {}", t.getName(), e);

    public RaftThread(String threadName, Runnable r) {
        super(r, threadName);
        setUncaughtExceptionHandler(uncaughtExceptionHandler);
    }
}
