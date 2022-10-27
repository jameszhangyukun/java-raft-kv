package com.zyk.raft.kv.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
@Builder
public class Command implements Serializable {
    String key;
    String value;
}
