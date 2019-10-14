package top.omooo.tinypng_plugin

class TinyPngInfo {
    String path
    String preSize
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