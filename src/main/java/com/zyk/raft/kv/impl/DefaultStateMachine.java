package com.zyk.raft.kv.impl;

import com.alibaba.fastjson.JSON;
import com.zyk.raft.kv.StateMachine;
import com.zyk.raft.kv.entity.Command;
import com.zyk.raft.kv.entity.LogEntry;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.nio.charset.StandardCharsets;

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
        try {

            Command command = logEntry.getCommand();
            if (command == null) {
                throw new IllegalArgumentException("command can not be null, logEntry : " + logEntry.toString());
            }
            machineDb.put(command.getKey().getBytes(StandardCharsets.UTF_8), JSON.toJSONBytes(logEntry));
        } catch (RocksDBException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public LogEntry get(String key) {
        try {
            byte[] result = machineDb.get(key.getBytes(StandardCharsets.UTF_8));
            if (result == null) {
                return null;
            }
            return JSON.parseObject(result, LogEntry.class);
        } catch (RocksDBException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public String getString(String key) {
        try {
            byte[] result = machineDb.get(key.getBytes(StandardCharsets.UTF_8));
            if (result != null) {
                return new String(result);
            }
        } catch (RocksDBException e) {
            log.error(e.getMessage());
        }
        return "";
    }

    @Override
    public void setString(String key, String value) {
        try {
            machineDb.put(key.getBytes(StandardCharsets.UTF_8), value.getBytes(StandardCharsets.UTF_8));
        } catch (RocksDBException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public void delString(String... key) {
        try {
            for (String s : key) {
                machineDb.delete(s.getBytes(StandardCharsets.UTF_8));
            }
        } catch (RocksDBException e) {
            log.error(e.getMessage());
        }
    }
}
