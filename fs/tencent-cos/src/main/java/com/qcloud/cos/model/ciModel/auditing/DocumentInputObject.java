package com.qcloud.cos.model.ciModel.auditing;

/**
 * 输入文件在cos中的位置
 * 例 cos根目录下的1.txt文件  则object = 1.txt
 * cos根目录下test文件夹中的1.txt文件 object = test/1.txt
 */
public class DocumentInputObject {
    private String url;
    private String object;
    private String type;
    private String dataId;

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("DocumentInputObject{");
        sb.append("url='").append(url).append('\'');
        sb.append(", object='").append(object).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", dataId='").append(dataId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
