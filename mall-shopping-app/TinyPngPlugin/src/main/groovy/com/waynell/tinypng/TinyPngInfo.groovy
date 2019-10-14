package com.waynell.tinypng

/**
 * Create On 16/12/2016
 * @author Wayne
 */
class TinyPngInfo {
    String path
    String preSize;
    String postSize
    String md5

    TinyPngInfo() {
    }

    TinyPngInfo(String path, String preSize, String postSize, String md5) {
        this.path = path
        this.preSize = preSize
        this.postSize = postSize
        this.md5 = md5
    }

}