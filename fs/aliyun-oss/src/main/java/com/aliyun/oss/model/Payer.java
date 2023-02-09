package com.aliyun.oss.model;

public enum Payer {

    /**
     * The bucket owner pays for the rquest
     */
    BucketOwner("BucketOwner"),

    /**
     * The rquester pays for the request
     */
    Requester("Requester");

    private String payerString;

    private Payer(String payerString) {
        this.payerString = payerString;
    }

    @Override
    public String toString() {
        return this.payerString;
    }

    public static Payer parse(String payer) {
        for (Payer o : Payer.values()) {
            if (o.toString().toLowerCase().equals(payer.toLowerCase())) {
                return o;
            }
        }

        throw new IllegalArgumentException("Unable to parse the payer text " + payer);
    }
}