package com.aliyun.oss.model;

import java.util.HashMap;
import java.util.Map;

public class TagSet extends GenericResult {
    private Map<String, String> tags;

    public TagSet() {
        this.tags = new HashMap<String, String>(1);
    }

    public TagSet(Map<String, String> tags) {
        this.tags = new HashMap<String, String>(1);
        this.tags.putAll(tags);
    }

    public String getTag(String key) {
        return this.tags.get(key);
    }

    public void setTag(String key, String value) {
        this.tags.put(key, value);
    }

    public Map<String, String> getAllTags() {
        return this.tags;
    }

    public void clear() {
        this.tags.clear();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("{");
        sb.append("Tags: " + this.getAllTags());
        sb.append("}");
        return sb.toString();
    }
}
