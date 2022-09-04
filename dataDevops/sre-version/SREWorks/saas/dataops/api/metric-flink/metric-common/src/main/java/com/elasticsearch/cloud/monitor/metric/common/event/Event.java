package com.elasticsearch.cloud.monitor.metric.common.event;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by colin on 2017/7/12.
 */
public class Event {
    private String id;
    private String title;
    private String text;

    private  long time;
    private String service;
    private Set<String> tags;
    private String type;
    private String source;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void addTag(String tag){
        if (tags == null){
            tags = new HashSet<>();
        }
        tags.add(tag);
    }
}
