package com.quarkonium.qpocket.model.transaction.bean;

public class EthGas {
    private float fastest;
    private float fast;
    private float safeLow;
    private float average;

    private float block_time;
    private float speed;
    private long blockNum;


    private float fastestWait;
    private float fastWait;
    private float avgWait;
    private float safeLowWait;

    public float getFastest() {
        return fastest / 10;
    }

    public void setFastest(float fastest) {
        this.fastest = fastest;
    }

    public float getFast() {
        return fast / 10f;
    }

    public void setFast(float fast) {
        this.fast = fast;
    }

    public float getSafeLow() {
        return safeLow / 10f;
    }

    public void setSafeLow(float safeLow) {
        this.safeLow = safeLow;
    }

    public float getAverage() {
        return average / 10;
    }

    public void setAverage(float average) {
        this.average = average;
    }

    public float getBlock_time() {
        return block_time;
    }

    public void setBlock_time(float block_time) {
        this.block_time = block_time;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public long getBlockNum() {
        return blockNum;
    }

    public void setBlockNum(long blockNum) {
        this.blockNum = blockNum;
    }

    public float getFastestWait() {
        return fastestWait;
    }

    public void setFastestWait(float fastestWait) {
        this.fastestWait = fastestWait;
    }

    public float getFastWait() {
        return fastWait;
    }

    public void setFastWait(float fastWait) {
        this.fastWait = fastWait;
    }

    public float getAvgWait() {
        return avgWait;
    }

    public void setAvgWait(float avgWait) {
        this.avgWait = avgWait;
    }

    public float getSafeLowWait() {
        return safeLowWait;
    }

    public void setSafeLowWait(float safeLowWait) {
        this.safeLowWait = safeLowWait;
    }
}
