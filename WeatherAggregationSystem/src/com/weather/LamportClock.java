package com.weather;

public class LamportClock {
    private long value;

    public LamportClock() {
        this.value = 0;
    }

    public synchronized void tick() {
        value++;
    }

    public synchronized void update(long receivedTimestamp) {
        value = Math.max(value, receivedTimestamp) + 1;
    }

    public synchronized long getValue() {
        return value;
    }
}