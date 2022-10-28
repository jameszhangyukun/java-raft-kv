package com.zyk.raft.kv.impl;

import com.zyk.raft.kv.StateMachine;
import com.zyk.raft.kv.entity.LogEntry;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.File;

/**
 * 默认状态机实现
 */
@Slf4j
public class DefaultStateMachine implements StateMachine {

    public String dbDir;
    public String stateMachineDir;

    public RocksDB machineDb;

    private DefaultStateMachine() {
        dbDir = "./rocksDB-raft/" + System.getProperty("serverPort");
        stateMachineDir = dbDir + "/stateMachine";

        RocksDB.loadLibrary();

        File file = new File(stateMachineDir);
        boolean success = false;

        if (!file.exists()) {
            success = file.mkdirs();
        }
        if (success) {
            log.warn("make a new dir : " + stateMachineDir);
        }

        Options options = new Options();
        options.setCreateIfMissing(true);
        try {
            machineDb = RocksDB.open(stateMachineDir);
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    public static DefaultStateMachine getInstance() {
        return DefaultStateMachineLazyHolder.INSTANCE;
    }

    public static class DefaultStateMachineLazyHolder {
        public static final DefaultStateMachine INSTANCE = new DefaultStateMachine();
    }


    @Override
    public void init() throws Throwable {


    }

    @Override
    public void destroy() throws Throwable {
        this.machineDb.close();
        log.info("machineDb close");
    }

    @Override
    public void apply(LogEntry logEntry) {

    }

    @Override
    public LogEntry get(String key) {
        return null;
    }

    @Override
    public String getString(String key) {
        return null;
    }

    @Override
    public void setString(String key, String value) {

    }

    @Override
    public void delString(String... key) {

    }
}
