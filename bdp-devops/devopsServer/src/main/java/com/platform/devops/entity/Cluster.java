package com.platform.devops.entity;

public class Cluster {
    private int id;
    private String name;
    private String alias;
    public Cluster() {}
    public Cluster(int id, String name, String alias) {
        this.id = id;
        this.name = name;
        this.alias = alias;
    }
}
