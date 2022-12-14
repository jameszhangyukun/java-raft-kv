package com.zyk.raft.kv.entity;


import lombok.Getter;

import java.util.Objects;

@Getter
public class Peer {
    private final String addr;

    public Peer(String addr) {
        this.addr = addr;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Peer peer = (Peer) o;
        return Objects.equals(addr, peer.addr);
    }

    @Override
    public int hashCode() {

        return Objects.hash(addr);
    }

    @Override
    public String toString() {
        return "Peer{" +
                "addr='" + addr + '\'' +
                '}';
    }
}