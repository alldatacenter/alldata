package com.platform.website.module;

public class PlatformDimension {
    private int id;
    private String platform;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public PlatformDimension() {
        super();
    }

    public PlatformDimension(int id) {
        super();
        this.id = id;
    }

    public PlatformDimension(String platform) {
        super();
        this.platform = platform;
    }

    public PlatformDimension(int id, String platform) {
        super();
        this.id = id;
        this.platform = platform;
    }

}
