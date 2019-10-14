package top.omooo.tinypng_plugin

class TinyPngExtension {

    ArrayList<String> resourceDir
    ArrayList<String> resourcePattern
    ArrayList<String> whiteList
    String apiKey

    TinyPngExtension() {
        resourceDir = []
        resourcePattern = []
        whiteList = []
        apiKey = null
    }


    @Override
    public String toString() {
        return "TinyPngExtension.groovy{" +
                "resourceDir=" + resourceDir +
                ", resourcePattern=" + resourcePattern +
                ", whiteList=" + whiteList +
                ", apiKey='" + apiKey + '\'' +
                '}'
    }
}