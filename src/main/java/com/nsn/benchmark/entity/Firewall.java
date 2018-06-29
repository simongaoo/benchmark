package com.nsn.benchmark.entity;

import java.util.Arrays;

public class Firewall {
    private byte[] privateHost;
    private int privatePort;
    private byte[] destinationHost;
    private int destinationPort;
    private byte[] sourceHost;
    private int sourcePort;
    private long startTime;

    public byte[] getPrivateHost() {
        return privateHost;
    }

    public void setPrivateHost(byte[] privateHost) {
        this.privateHost = privateHost;
    }

    public int getPrivatePort() {
        return privatePort;
    }

    public void setPrivatePort(int privatePort) {
        this.privatePort = privatePort;
    }

    public byte[] getDestinationHost() {
        return destinationHost;
    }

    public void setDestinationHost(byte[] destinationHost) {
        this.destinationHost = destinationHost;
    }

    public int getDestinationPort() {
        return destinationPort;
    }

    public void setDestinationPort(int destinationPort) {
        this.destinationPort = destinationPort;
    }

    public byte[] getSourceHost() {
        return sourceHost;
    }

    public void setSourceHost(byte[] sourceHost) {
        this.sourceHost = sourceHost;
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(int sourcePort) {
        this.sourcePort = sourcePort;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    @Override
    public String toString() {
        return "Firewall{" +
                "privateHost=" + Arrays.toString(privateHost) +
                ", privatePort=" + privatePort +
                ", destinationHost=" + Arrays.toString(destinationHost) +
                ", destinationPort=" + destinationPort +
                ", sourceHost=" + Arrays.toString(sourceHost) +
                ", sourcePort=" + sourcePort +
                ", startTime=" + startTime +
                '}';
    }
}
