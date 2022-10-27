package com.zyk.raft.kv.entity;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 日志条目
 */
@Data
@Builder
public class LogEntry implements Serializable, Comparable {

    private long index;
    private long term;
    private Command command;

    @Override
    public int compareTo(Object o) {
        if (o == null) {
            return -1;
        }

        return this.getIndex() > ((LogEntry) o).getIndex() ? 1 : -1;
    }
}
