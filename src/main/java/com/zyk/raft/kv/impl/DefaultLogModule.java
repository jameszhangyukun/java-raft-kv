package com.zyk.raft.kv.impl;

import com.zyk.raft.kv.LogModule;
import com.zyk.raft.kv.entity.LogEntry;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.RocksDB;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReentrantLock;

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

    private RocksDB rocksDB;


    ReentrantLock lock = new ReentrantLock();

    private DefaultLogModule() {
        if (dbDir == null) {
            dbDir = "./rocksDB-raft/" + System.getProperty("serverPort");
        }
        if (logsDir == null) {
            logsDir = dbDir + "/logModule";

        }
    }

    @Override
    public void init() throws Throwable {

    }

    @Override
    public void destroy() throws Throwable {

    }

    @Override
    public void write(LogEntry logEntry) {

    }

    @Override
    public LogEntry read(Long index) {
        return null;
    }

    @Override
    public void removeOnStartIndex(long startIndex) {

    }

    @Override
    public LogEntry getLast() {
        return null;
    }

    @Override
    public Long getLastIndex() {
        return null;
    }
}
