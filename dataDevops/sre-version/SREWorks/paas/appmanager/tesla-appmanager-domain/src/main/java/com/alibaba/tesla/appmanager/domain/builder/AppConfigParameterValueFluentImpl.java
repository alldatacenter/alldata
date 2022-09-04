package com.alibaba.tesla.appmanager.domain.builder;

import java.util.ArrayList;
import java.util.List;

public class AppConfigParameterValueFluentImpl<A extends AppConfigParameterValueFluent<A>>
        extends BaseFluent<A>
        implements AppConfigParameterValueFluent<A> {

    private String name;
    private Object value;
    private List<String> toFieldPaths;

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public A withName(String name) {
        this.name = name;
        return (A) this;
    }

    @Override
    public Object getValue() {
        return this.value;
    }

    @Override
    public A withValue(Object value) {
        this.value = value;
        return (A) this;
    }

    @Override
    public List<String> getToFieldPaths() {
        return this.toFieldPaths == null ? new ArrayList<>() : this.toFieldPaths;
    }

    @Override
    public A withToFieldPaths(List<String> toFieldPaths) {
        this.toFieldPaths = toFieldPaths;
        return (A) this;
    }
}
