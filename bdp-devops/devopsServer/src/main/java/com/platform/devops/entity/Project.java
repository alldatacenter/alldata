package com.platform.devops.entity;

public class Project {
    private int id;
    private String name;
    private String alias;
    public Project() {}
    public Project(int id, String name, String alias) {
        this.id = id;
        this.name = name;
        this.alias = alias;
    }
}
