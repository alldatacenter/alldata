package com.alibaba.tesla.tkgone.server.common;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Range {

    int start = 0;
    int stop = 1;
    int step = 1;

    public Range(int start, int stop, int step) {
        this.start = start;
        this.stop = stop;
        this.step = step;
    }

    public Range(int start, int stop) {
        this(start, stop, 1);
    }

    public Range(int stop) {
        this(0, stop);
    }

    public List<Integer> toList() {
        List<Integer> list = new ArrayList<>();
        for (int index = start; index < stop; index+=step) {
            list.add(index);
        }
        return list;
    }
}
