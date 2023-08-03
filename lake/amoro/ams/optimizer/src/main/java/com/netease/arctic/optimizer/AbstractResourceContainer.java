package com.netease.arctic.optimizer;

import com.netease.arctic.ams.api.PropertyNames;
import com.netease.arctic.ams.api.resource.Resource;
import com.netease.arctic.ams.api.resource.ResourceContainer;
import com.netease.arctic.ams.api.resource.ResourceStatus;
import com.netease.arctic.optimizer.util.PropertyUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractResourceContainer implements ResourceContainer {
  private String containerName;
  private Map<String, String> containerProperties;

  @Override
  public String name() {
    return containerName;
  }

  @Override
  public void init(String name, Map<String, String> containerProperties) {
    this.containerName = name;
    this.containerProperties = containerProperties;
  }

  @Override
  public void requestResource(Resource resource) {
    Map<String, String> startupStats = doScaleOut(buildOptimizerStartupArgsString(resource));
    resource.getProperties().putAll(startupStats);
  }

  protected abstract Map<String, String> doScaleOut(String startUpArgs);

  protected String getAMSHome() {
    return PropertyUtil.checkAndGetProperty(containerProperties, PropertyNames.AMS_HOME);
  }

  protected String getAMSUrl() {
    return PropertyUtil.checkAndGetProperty(containerProperties, PropertyNames.OPTIMIZER_AMS_URL);
  }

  public Map<String, String> getContainerProperties() {
    return containerProperties;
  }

  protected String buildOptimizerStartupArgsString(Resource resource) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(" -a ")
        .append(getAMSUrl())
        .append(" -p ")
        .append(resource.getThreadCount())
        .append(" -g ")
        .append(resource.getGroupName());
    if (resource.getProperties().containsKey(PropertyNames.OPTIMIZER_HEART_BEAT_INTERVAL)) {
      stringBuilder.append(" -hb ")
          .append(resource.getProperties().get(PropertyNames.OPTIMIZER_HEART_BEAT_INTERVAL));
    }
    if (org.apache.iceberg.util.PropertyUtil.propertyAsBoolean(
        resource.getProperties(),
        PropertyNames.OPTIMIZER_EXTEND_DISK_STORAGE,
        PropertyNames.OPTIMIZER_EXTEND_DISK_STORAGE_DEFAULT)) {
      stringBuilder.append(" -eds -dsp ")
          .append(PropertyUtil.checkAndGetProperty(
              resource.getProperties(),
              PropertyNames.OPTIMIZER_DISK_STORAGE_PATH));
      if (resource.getProperties().containsKey(PropertyNames.OPTIMIZER_MEMORY_STORAGE_SIZE)) {
        stringBuilder.append(" -msz ")
            .append(resource.getProperties().get(PropertyNames.OPTIMIZER_MEMORY_STORAGE_SIZE));
      }
    }
    if (resource.getResourceId() != null && resource.getResourceId().length() > 0) {
      stringBuilder.append(" -id ").append(resource.getResourceId());
    }
    return stringBuilder.toString();
  }

  protected List<String> exportSystemProperties() throws IOException {
    List<String> cmds = new ArrayList<>();
    if (containerProperties != null) {
      for (Map.Entry<String, String> entry : containerProperties.entrySet()) {
        if (entry.getKey().startsWith(PropertyNames.EXPORT_PROPERTY_PREFIX)) {
          String exportPropertyName = entry.getKey().substring(PropertyNames.EXPORT_PROPERTY_PREFIX.length());
          String exportValue = entry.getValue();
          cmds.add(String.format("export %s=%s", exportPropertyName, exportValue));
        }
      }
    }
    return cmds;
  }

  @Override
  public ResourceStatus getStatus(String resourceId) {
    return null;
  }
}
