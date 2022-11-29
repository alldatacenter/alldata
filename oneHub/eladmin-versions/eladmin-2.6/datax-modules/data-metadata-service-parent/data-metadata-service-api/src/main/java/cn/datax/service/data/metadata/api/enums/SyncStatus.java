package cn.datax.service.data.metadata.api.enums;

public enum SyncStatus {

    NotSync("0"),
    InSync("1"),
    IsSync("2"),
	SyncError("3");

    private final String key;

    SyncStatus(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
