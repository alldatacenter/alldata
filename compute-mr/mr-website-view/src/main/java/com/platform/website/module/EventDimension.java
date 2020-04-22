package com.platform.website.module;

public class EventDimension {
    private int id;
    private String category;
    private String action;

    public EventDimension() {
        super();
    }

    public EventDimension(int id) {
        super();
        this.id = id;
    }

    public EventDimension(String category, String action) {
        super();
        this.category = category;
        this.action = action;
    }

    public EventDimension(int id, String category, String action) {
        super();
        this.id = id;
        this.category = category;
        this.action = action;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
