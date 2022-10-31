package com.zyk.raft.kv.current;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SleepHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(SleepHelper.class);


    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            LOGGER.warn(e.getMessage());
        }

    }
}
