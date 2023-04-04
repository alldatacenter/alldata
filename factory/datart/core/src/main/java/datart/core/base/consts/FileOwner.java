package datart.core.base.consts;

public enum FileOwner {

    USER_AVATAR("resources/user/avatar/"),

    ORG_AVATAR("resources/org/avatar/"),

    DATACHART("resources/image/datachart/"),

    DASHBOARD("resources/image/dashboard/"),

    DATA_SOURCE("resources/data/source/"),

    SCHEDULE("schedule/files/"),

    DOWNLOAD("download/"),

    EXPORT("export/");

    private final String path;

    FileOwner(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}