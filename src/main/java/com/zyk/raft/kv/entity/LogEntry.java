package com.zyk.raft.kv.entity;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 日志条目
 */
@Data
@Builder
@Getter
@Setter
public class LogEntry implements Serializable, Comparable {

    private Long index;
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
