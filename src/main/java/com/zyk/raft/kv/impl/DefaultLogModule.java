package com.zyk.raft.kv.impl;

import com.alibaba.fastjson.JSON;
import com.zyk.raft.kv.LogModule;
import com.zyk.raft.kv.entity.LogEntry;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * 默认的日志实现
 */
@Getter
@Setter
@Slf4j
public class DefaultLogModule implements LogModule {

    public static final byte[] LAST_INDEX_KEY = "LAST_INDEX_KEY".getBytes(StandardCharsets.UTF_8);

    public String dbDir;

    public String logsDir;

    private RocksDB logDb;


    ReentrantLock lock = new ReentrantLock();


    private DefaultLogModule() {
        if (dbDir == null) {
            dbDir = "./rocksDB-raft/" + System.getProperty("serverPort");
        }
        if (logsDir == null) {
            logsDir = dbDir + "/logModule";
        }
        RocksDB.loadLibrary();
        Options options = new Options();

        options.setCreateIfMissing(true);
        File file = new File(logsDir);
        boolean success = false;

        if (!file.exists()) {
            success = file.mkdirs();
        }
        if (success) {
            log.warn("make a new dir : " + logsDir);
        }

        try {
            logDb = RocksDB.open(options, logsDir);
        } catch (RocksDBException e) {
            log.warn(e.getMessage());
        }
    }

    public static DefaultLogModule getInstance() {
        return DefaultLogsLazyHolder.INSTANCE;
    }

    @Override
    public void init() throws Throwable {

    }

    @Override
    public void destroy() throws Throwable {
        logDb.close();
        log.info("destroy success");
    }

    @Override
    public void write(LogEntry logEntry) {
        boolean success = false;

        try {
            lock.tryLock(3000, MILLISECONDS);
            logEntry.setIndex(getLastIndex() + 1);
            logDb.put(logEntry.getIndex().toString().getBytes(StandardCharsets.UTF_8), JSON.toJSONBytes(logEntry));
            success = true;
            log.info("DefaultLogModule write rocksDB success, logEntry info : [{}]", logEntry);
        } catch (InterruptedException | RocksDBException e) {
            e.printStackTrace();
        } finally {
            if (success) {
                updateLastIndex(logEntry.getIndex());
            }
            lock.unlock();
        }
    }

    @Override
    public LogEntry read(Long index) {
        try {
            byte[] result = logDb.get(convert(index));
            if (result == null) {
                return null;
            }
            return JSON.parseObject(result, LogEntry.class);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void removeOnStartIndex(long startIndex) {
        boolean success = false;
        int count = 0;

        try {
            lock.tryLock(3000, MILLISECONDS);
            for (long i = startIndex; i <= getLastIndex(); i++) {
                logDb.delete(String.valueOf(i).getBytes(StandardCharsets.UTF_8));
                ++count;
            }
            success = true;
            log.warn("rocksDB removeOnStartIndex success, count={} startIndex={}, lastIndex={}", count, startIndex, getLastIndex());
        } catch (InterruptedException | RocksDBException e) {
            e.printStackTrace();
            log.warn(e.getMessage());
        } finally {
            if (success) {
                updateLastIndex(getLastIndex() - count);
            }
            lock.unlock();
        }

    }

    @Override
    public LogEntry getLast() {
        try {
            byte[] result = logDb.get(convert(getLastIndex()));
            if (result == null) {
                return null;
            }
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Long getLastIndex() {
        byte[] lastIndex = "-1".getBytes(StandardCharsets.UTF_8);

        try {
            lastIndex = logDb.get(LAST_INDEX_KEY);
            if (lastIndex == null) {
                lastIndex = "-1".getBytes(StandardCharsets.UTF_8);
            }

        } catch (RocksDBException e) {
            e.printStackTrace();
        }


        return Long.valueOf(new String(lastIndex));
    }

    private void updateLastIndex(Long index) {
        try {
            logDb.put(LAST_INDEX_KEY, index.toString().getBytes(StandardCharsets.UTF_8));
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    private byte[] convert(Long index) {
        return index.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static class DefaultLogsLazyHolder {
        private static final DefaultLogModule INSTANCE = new DefaultLogModule();
    }
}
