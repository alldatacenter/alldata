package com.netease.arctic.ams.api.resource;

import java.util.List;

public interface ResourceManager {

  void createResourceGroup(ResourceGroup resourceGroup);

  void updateResourceGroup(ResourceGroup resourceGroup);

  void deleteResourceGroup(String groupName);

  void createResource(Resource resource);

  void deleteResource(String resourceId);

  List<ResourceGroup> listResourceGroups();

  List<ResourceGroup> listResourceGroups(String containerName);

  ResourceGroup getResourceGroup(String groupName);

  List<Resource> listResourcesByGroup(String groupName);

  Resource getResource(String resourceId);
}
