package com.netease.arctic.ams.api.resource;

import com.netease.arctic.ams.api.OptimizerRegisterInfo;
import org.apache.curator.shaded.com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Resource {
  private String resourceId;
  private String containerName;
  private String groupName;
  private int threadCount;
  private int memoryMb;
  private Map<String, String> properties;
  private ResourceType type;

  protected Resource() {
  }

  private Resource(Builder builder) {
    this.resourceId = UUID.randomUUID().toString();
    this.containerName = builder.containerName;
    this.groupName = builder.groupName;
    this.threadCount = builder.threadCount;
    this.memoryMb = builder.memoryMb;
    this.properties = builder.properties;
    this.type = builder.type;
  }

  protected Resource(OptimizerRegisterInfo registerInfo, String containerName) {
    this.resourceId = registerInfo.getResourceId();
    this.groupName = registerInfo.getGroupName();
    this.threadCount = registerInfo.getThreadCount();
    this.memoryMb = registerInfo.getMemoryMb();
    this.properties = registerInfo.getProperties();
    this.type = ResourceType.OPTIMIZER;
    this.containerName = containerName;
  }

  public String getResourceId() {
    return resourceId;
  }

  public String getContainerName() {
    return containerName;
  }

  public String getGroupName() {
    return groupName;
  }

  public int getThreadCount() {
    return threadCount;
  }

  public int getMemoryMb() {
    return memoryMb;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public ResourceType getType() {
    return type;
  }

  public static class Builder {
    private final String containerName;
    private final String groupName;
    private final ResourceType type;
    private int threadCount;
    private int memoryMb;
    private Map<String, String> properties = new HashMap<>();

    //build resource object
    public Builder(String containerName, String groupName, ResourceType type) {
      this.containerName = containerName;
      this.groupName = groupName;
      this.type = type;
    }

    public Builder(OptimizerRegisterInfo optimizerRegisterInfo, String containerName) {
      this.containerName = containerName;
      this.groupName = optimizerRegisterInfo.getGroupName();
      this.threadCount = optimizerRegisterInfo.getThreadCount();
      this.memoryMb = optimizerRegisterInfo.getMemoryMb();
      this.properties = optimizerRegisterInfo.getProperties();
      this.type = ResourceType.OPTIMIZER;
    }

    public Builder setThreadCount(int threadCount) {
      this.threadCount = threadCount;
      return this;
    }

    public Builder setMemoryMb(int memoryMb) {
      this.memoryMb = memoryMb;
      return this;
    }

    //generate addProperties method
    public Builder addProperties(String key, String value) {
      this.properties.put(key, value);
      return this;
    }

    public Resource build() {
      Preconditions.checkArgument(
          containerName != null && groupName != null && type != null,
          "containerName, groupName and type should not be null");
      return new Resource(this);
    }

    public Builder setProperties(Map<String, String> properties) {
      this.properties = properties;
      return this;
    }
  }
}
