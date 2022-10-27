package com.zyk.raft.kv;

import com.zyk.raft.kv.entity.LogEntry;

/**
 * 日志模块
 */
public interface LogModule extends LifeCycle {
    void write(LogEntry logEntry);

    LogEntry read(Long index);

    void removeOnStartIndex(long startIndex);

    LogEntry getLast();

    Long getLastIndex();
}
