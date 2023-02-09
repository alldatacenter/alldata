package com.aliyun.oss.model;

public enum SubDirType {
    /**
     * the owner of the bucket
     */
    Redirect("0"),
    NoSuchKey("1"),
    Index("2");

    private String mType;

    SubDirType(String type) {
        this.mType = type;
    }

    @Override
    public String toString() {
        return this.mType;
    }

    public static SubDirType parse(String payerString) {
        for (SubDirType type : SubDirType.values()) {
            if (type.toString().equals(payerString)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unable to parse " + payerString);
    }
}