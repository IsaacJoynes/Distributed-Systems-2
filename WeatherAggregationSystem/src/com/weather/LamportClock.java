package com.weather;

public class LamportClock {
    private long value;

    public LamportClock() {
        this.value = 0;
    }

    public synchronized long getValue() {
        return value;
    }

    public synchronized void tick() {
        value++;
    }

    public synchronized void update(long receivedValue) {
        value = Math.max(value, receivedValue) + 1;
    }
}