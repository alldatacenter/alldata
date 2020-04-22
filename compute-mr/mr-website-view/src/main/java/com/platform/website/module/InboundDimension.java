package com.platform.website.module;

public class InboundDimension {
    private int id;
    private String name;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public InboundDimension() {
        super();
    }

    public InboundDimension(int id) {
        super();
        this.id = id;
    }

    public InboundDimension(String name) {
        super();
        this.name = name;
    }

    public InboundDimension(int id, String name) {
        super();
        this.id = id;
        this.name = name;
    }

}
