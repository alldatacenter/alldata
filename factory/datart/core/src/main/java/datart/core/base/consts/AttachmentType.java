package datart.core.base.consts;

public enum AttachmentType {

    EXCEL(".xlsx"),

    IMAGE(".png"),

    PDF(".pdf");

    private String suffix;

    AttachmentType(String suffix) {
        this.suffix = suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getSuffix() {
        return suffix;
    }
}
