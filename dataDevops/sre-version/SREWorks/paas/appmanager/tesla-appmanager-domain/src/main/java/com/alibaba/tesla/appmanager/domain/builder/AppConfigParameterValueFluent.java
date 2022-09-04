package com.alibaba.tesla.appmanager.domain.builder;

import java.util.List;

public interface AppConfigParameterValueFluent<A extends AppConfigParameterValueFluent<A>> extends Fluent<A> {

    String getName();

    A withName(String name);

    Object getValue();

    A withValue(Object value);

    List<String> getToFieldPaths();

    A withToFieldPaths(List<String> toFieldPaths);
}
