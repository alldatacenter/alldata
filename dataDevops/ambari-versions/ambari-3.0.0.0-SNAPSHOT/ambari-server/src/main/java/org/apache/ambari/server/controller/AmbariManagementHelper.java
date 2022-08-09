/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.persistence.RollbackException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.dao.ExtensionDAO;
import org.apache.ambari.server.orm.dao.ExtensionLinkDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.ExtensionEntity;
import org.apache.ambari.server.orm.entities.ExtensionLinkEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.stack.ExtensionHelper;
import org.apache.ambari.server.stack.StackManager;
import org.apache.ambari.server.state.ExtensionInfo;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.stack.ExtensionMetainfoXml;
import org.apache.ambari.server.utils.VersionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AmbariManagementHelper {

  private final static Logger LOG =
      LoggerFactory.getLogger(AmbariManagementHelper.class);

  private ExtensionLinkDAO linkDAO;
  private ExtensionDAO extensionDAO;
  private StackDAO stackDAO;

  @Inject
  public AmbariManagementHelper(StackDAO stackDAO, ExtensionDAO extensionDAO, ExtensionLinkDAO linkDAO) {
    this.stackDAO = stackDAO;
    this.extensionDAO = extensionDAO;
    this.linkDAO = linkDAO;
  }

  /**
   * This method will create a link between an extension version and a stack version (Extension Link).
   *
   * An extension version is like a stack version but it contains custom services.  Linking an extension
   * version to the current stack version allows the cluster to install the custom services contained in
   * the extension version.
   */
  public void createExtensionLink(StackManager stackManager, StackInfo stackInfo, ExtensionInfo extensionInfo) throws AmbariException {
    validateCreateExtensionLinkRequest(stackInfo, extensionInfo);
    ExtensionHelper.validateCreateLink(stackManager, stackInfo, extensionInfo);
    ExtensionLinkEntity linkEntity = createExtensionLinkEntity(stackInfo, extensionInfo);
    stackManager.linkStackToExtension(stackInfo, extensionInfo);

    try {
      linkDAO.create(linkEntity);
    } catch (RollbackException e) {
      String message = "Unable to create extension link";
      LOG.debug(message, e);
      String errorMessage = message
              + ", stackName=" + stackInfo.getName()
              + ", stackVersion=" + stackInfo.getVersion()
              + ", extensionName=" + extensionInfo.getName()
              + ", extensionVersion=" + extensionInfo.getVersion();
      LOG.warn(errorMessage);
      throw new AmbariException(errorMessage, e);
    }
  }

  /**
   * This method will create a link between an extension version and a stack version (Extension Link).
   *
   * An extension version is like a stack version but it contains custom services.  Linking an extension
   * version to the current stack version allows the cluster to install the custom services contained in
   * the extension version.
   */
  public void createExtensionLinks(StackManager stackManager, List<ExtensionInfo> extensions) throws AmbariException {
    Map<String, List<StackInfo>> stackMap = stackManager.getStacksByName();
    for (List<StackInfo> stacks : stackMap.values()) {
      Collections.sort(stacks);
      Collections.reverse(stacks);
    }

    Collections.sort(extensions);
    Collections.reverse(extensions);
    for (ExtensionInfo extension : extensions) {
      if (extension.isActive() && extension.isAutoLink()) {
        LOG.debug("Autolink - looking for matching stack versions for extension:{}/{} ", extension.getName(), extension.getVersion());
        for (ExtensionMetainfoXml.Stack supportedStack : extension.getStacks()) {
          List<StackInfo> stacks = stackMap.get(supportedStack.getName());
          for (StackInfo stack : stacks) {
            // If the stack version is not currently linked to a version of the extension and it meets the minimum stack version then link them
            if (stack.getExtension(extension.getName()) == null && VersionUtils.compareVersions(stack.getVersion(), supportedStack.getVersion()) > -1) {
              LOG.debug("Autolink - extension: {}/{} stack: {}/{}", extension.getName(), extension.getVersion(),
                       stack.getName(), stack.getVersion());
              createExtensionLink(stackManager, stack, extension);
            }
            else {
              LOG.debug("Autolink - not a match extension: {}/{} stack: {}/{}", extension.getName(), extension.getVersion(),
                       stack.getName(), stack.getVersion());
            }
          }
        }
      }
      else {
        LOG.debug("Autolink - skipping extension: {}/{}.  It is either not active or set to autolink.", extension.getName(), extension.getVersion());
      }
    }
  }

  /**
   * Validates the stackInfo and extensionInfo parameters are valid.
   * If they are then it confirms that the stack and extension are not already linked.
   */
  private void validateCreateExtensionLinkRequest(StackInfo stackInfo, ExtensionInfo extensionInfo) throws AmbariException {
    if (stackInfo == null) {
      throw new IllegalArgumentException("Stack should be provided");
    }
    if (extensionInfo == null) {
      throw new IllegalArgumentException("Extension should be provided");
    }
    if (StringUtils.isBlank(stackInfo.getName())
            || StringUtils.isBlank(stackInfo.getVersion())
            || StringUtils.isBlank(extensionInfo.getName())
            || StringUtils.isBlank(extensionInfo.getVersion())) {

      throw new IllegalArgumentException("Stack name, stack version, extension name and extension version should be provided");
    }

    ExtensionLinkEntity entity = linkDAO.findByStackAndExtension(stackInfo.getName(), stackInfo.getVersion(),
		extensionInfo.getName(), extensionInfo.getVersion());

    if (entity != null) {
      throw new AmbariException("The stack and extension are already linked"
                + ", stackName=" + stackInfo.getName()
                + ", stackVersion=" + stackInfo.getVersion()
                + ", extensionName=" + extensionInfo.getName()
                + ", extensionVersion=" + extensionInfo.getVersion());
    }
  }

  /**
   * Updates the extension version of the currently linked extension to the stack version
   */
  public void updateExtensionLink(StackManager stackManager, ExtensionLinkEntity linkEntity, StackInfo stackInfo,
                                  ExtensionInfo oldExtensionInfo, ExtensionInfo newExtensionInfo) throws AmbariException {
    //validateUpdateExtensionLinkRequest(stackInfo, extensionInfo);
    ExtensionHelper.validateUpdateLink(stackManager, stackInfo, oldExtensionInfo, newExtensionInfo);

    ExtensionEntity extension = extensionDAO.find(newExtensionInfo.getName(), newExtensionInfo.getVersion());
    linkEntity.setExtension(extension);

    try {
      linkEntity = linkDAO.merge(linkEntity);
    } catch (RollbackException e) {
      String message = "Unable to update extension link";
      LOG.debug(message, e);
      String errorMessage = message
              + ", stackName=" + stackInfo.getName()
              + ", stackVersion=" + stackInfo.getVersion()
              + ", extensionName=" + newExtensionInfo.getName()
              + ", extensionVersion=" + newExtensionInfo.getVersion();
      LOG.warn(errorMessage);
      throw new AmbariException(errorMessage, e);
    }
  }

  private ExtensionLinkEntity createExtensionLinkEntity(StackInfo stackInfo, ExtensionInfo extensionInfo) throws AmbariException {
    StackEntity stack = stackDAO.find(stackInfo.getName(), stackInfo.getVersion());
    ExtensionEntity extension = extensionDAO.find(extensionInfo.getName(), extensionInfo.getVersion());

    ExtensionLinkEntity linkEntity = new ExtensionLinkEntity();
    linkEntity.setStack(stack);
    linkEntity.setExtension(extension);
    return linkEntity;
  }

}
