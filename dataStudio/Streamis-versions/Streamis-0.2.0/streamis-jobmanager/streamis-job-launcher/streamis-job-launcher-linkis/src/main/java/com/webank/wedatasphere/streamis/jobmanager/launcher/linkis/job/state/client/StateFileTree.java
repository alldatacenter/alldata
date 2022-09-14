package com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.job.state.client;

import java.util.HashMap;
import java.util.List;

public class StateFileTree {

    private String name;
    private String path;
    private HashMap<String, String> properties;
    private List<StateFileTree> children;
    private Boolean isLeaf = false;
    private String parentPath;

    public Boolean getIsLeaf() {
        return isLeaf;
    }

    public void setIsLeaf(Boolean isLeaf) {
        this.isLeaf = isLeaf;
    }

    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public HashMap<String, String> getProperties() {
        return properties;
    }

    public void setProperties(HashMap<String, String> properties) {
        this.properties = properties;
    }

    public List<StateFileTree> getChildren() {
        return children;
    }

    public void setChildren(List<StateFileTree> children) {
        this.children = children;
    }
}