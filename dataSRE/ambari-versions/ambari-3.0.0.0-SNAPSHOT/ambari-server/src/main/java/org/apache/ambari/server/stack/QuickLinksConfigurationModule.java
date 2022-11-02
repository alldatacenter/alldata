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
package org.apache.ambari.server.stack;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.QuickLinksConfigurationInfo;
import org.apache.ambari.server.state.quicklinks.QuickLinks;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuickLinksConfigurationModule extends BaseModule<QuickLinksConfigurationModule, QuickLinksConfigurationInfo> implements Validable {

  private static final Logger LOG = LoggerFactory.getLogger(QuickLinksConfigurationModule.class);
  private static final ObjectMapper mapper = new ObjectMapper();

  public static final String QUICKLINKS_CONFIGURATION_KEY = "QuickLinksConfiguration";

  private QuickLinksConfigurationInfo moduleInfo;
  private boolean valid = true;
  private Set<String> errors = new HashSet<>();

  public QuickLinksConfigurationModule(File quickLinksConfigurationFile) {
    this(quickLinksConfigurationFile, new QuickLinksConfigurationInfo());
  }

  public QuickLinksConfigurationModule(File quickLinksConfigurationFile, QuickLinksConfigurationInfo moduleInfo) {
    this.moduleInfo = moduleInfo;
    if (!moduleInfo.isDeleted() && quickLinksConfigurationFile != null) {
      LOG.debug("Looking for quicklinks in {}", quickLinksConfigurationFile.getAbsolutePath());
      FileReader reader = null;
      try {
        reader = new FileReader(quickLinksConfigurationFile);
      } catch (FileNotFoundException e) {
        LOG.error("Quick links file not found");
      }
      try {
        QuickLinks quickLinksConfig = mapper.readValue(reader, QuickLinks.class);
        Map<String, QuickLinks> map = new HashMap<>();
        map.put(QUICKLINKS_CONFIGURATION_KEY, quickLinksConfig);
        moduleInfo.setQuickLinksConfigurationMap(map);
        LOG.debug("Loaded quicklinks configuration: {}", moduleInfo);
      } catch (IOException e) {
        String errorMessage = String.format("Unable to parse quicklinks configuration file %s", quickLinksConfigurationFile.getAbsolutePath());
        LOG.error(errorMessage,  e);
        setValid(false);
        addError(errorMessage);
      }
    }
  }

  public QuickLinksConfigurationModule(QuickLinksConfigurationInfo moduleInfo) {
    this.moduleInfo = moduleInfo;
  }

  @Override
  public void resolve(QuickLinksConfigurationModule parent, Map<String, StackModule> allStacks, Map<String, ServiceModule> commonServices, Map<String, ExtensionModule> allExtensions) throws AmbariException {
    QuickLinksConfigurationInfo parentModuleInfo = parent.getModuleInfo();

    if (parent.getModuleInfo() != null && !moduleInfo.isDeleted()) {
      if (moduleInfo.getQuickLinksConfigurationMap() == null || moduleInfo.getQuickLinksConfigurationMap().isEmpty()) {
        moduleInfo.setQuickLinksConfigurationMap(parentModuleInfo.getQuickLinksConfigurationMap());
      } else if(parentModuleInfo.getQuickLinksConfigurationMap() != null && !parentModuleInfo.getQuickLinksConfigurationMap().isEmpty()) {
        QuickLinks child = moduleInfo.getQuickLinksConfigurationMap().get(QUICKLINKS_CONFIGURATION_KEY);
        QuickLinks parentConfig= parentModuleInfo.getQuickLinksConfigurationMap().get(QUICKLINKS_CONFIGURATION_KEY);
        child.mergeWithParent(parentConfig);
      }
    }
  }

  @Override
  public QuickLinksConfigurationInfo getModuleInfo() {
    return moduleInfo;
  }

  @Override
  public boolean isDeleted() {
    return false;
  }

  @Override
  public String getId() {
    return moduleInfo.getFileName();
  }

  @Override
  public boolean isValid() {
    return valid;
  }

  @Override
  public void setValid(boolean valid) {
    this.valid = valid;
  }

  @Override
  public void addError(String error) {
    errors.add(error);
  }

  @Override
  public void addErrors(Collection<String> errors) {
    this.errors.addAll(errors);
  }

  @Override
  public Collection<String> getErrors() {
    return errors;
  }
}
