package com.alibaba.tesla.appmanager.domain.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BaseFluent<F extends Fluent<F>> implements Fluent<F> {

    public static <T> ArrayList<T> build(List<? extends Builder<? extends T>> list) {
        return list == null ? null : new ArrayList<>(list.stream().map(Builder::build).collect(Collectors.toList()));
    }
}
