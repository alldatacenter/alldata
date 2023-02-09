package com.qcloud.cos.transfer;

public class DownloadPart {
    public final long start;
    public final long end;
    public final long crc64;

    public DownloadPart(long start, long end, long crc64) {
        this.start = start;
        this.end = end;
        this.crc64 = crc64;
    }

    public long getContentLength() {
        return end + 1 - start;
    }
}
