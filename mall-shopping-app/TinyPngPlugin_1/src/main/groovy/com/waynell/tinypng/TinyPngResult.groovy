package com.waynell.tinypng

public class TinyPngResult {
    private long beforeSize
    private long afterSize
    private boolean error
    private ArrayList<TinyPngInfo> results

    TinyPngResult(long beforeSize, long afterSize, boolean error, ArrayList<TinyPngInfo> results) {
        this.beforeSize = beforeSize
        this.afterSize = afterSize
        this.error = error
        this.results = results
    }

    long getBeforeSize() {
        return beforeSize
    }

    long getAfterSize() {
        return afterSize
    }

    boolean getError() {
        return error
    }

    ArrayList<TinyPngInfo> getResults() {
        return results
    }
}