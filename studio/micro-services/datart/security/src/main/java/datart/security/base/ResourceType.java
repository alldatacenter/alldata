package datart.security.base;

public enum ResourceType {

    SOURCE,
    VIEW,

    DATACHART("shareChart"),
    WIDGET,
    DASHBOARD("shareDashboard"),
    FOLDER,
    STORYBOARD("shareStoryPlayer"),
    VIZ,

    SCHEDULE,

    ROLE,
    USER;

    private String shareRoute;

    ResourceType() {
    }

    ResourceType(String shareRoute) {
        this.shareRoute = shareRoute;
    }

    public String getShareRoute() {
        return shareRoute;
    }
}