package com.platform.ds.plugin.task.java;

import org.apache.dolphinscheduler.spi.task.AbstractParameters;
import org.apache.dolphinscheduler.spi.task.ResourceInfo;
import org.apache.dolphinscheduler.spi.utils.StringUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * java parameter
 */
public class JavaParameters extends AbstractParameters {
    /**
     * url
     */
    private String url;
 
    /**
     *  java params
     */
    private List<JavaProperty> javaParams;
    

    @Override
    public boolean checkParameters() {
        return StringUtils.isNotEmpty(url);
    }

    @Override
    public List<ResourceInfo> getResourceFilesList() {
        return new ArrayList<>();
    }

    public List<JavaProperty> getJavaParams() {
        return javaParams;
    }

    public void setJavaParams(List<JavaProperty> javaParams) {
        this.javaParams = javaParams;
    }
}
