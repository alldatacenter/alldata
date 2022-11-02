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
import org.apache.ambari.server.state.ThemeInfo;
import org.apache.ambari.server.state.theme.Theme;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThemeModule extends BaseModule<ThemeModule, ThemeInfo> implements Validable {

  private static final Logger LOG = LoggerFactory.getLogger(ThemeModule.class);
  private static final ObjectMapper mapper = new ObjectMapper();

  public static final String THEME_KEY = "Theme";

  private ThemeInfo moduleInfo;
  private boolean valid = true;
  private Set<String> errors = new HashSet<>();

  public ThemeModule(File themeFile) {
    this(themeFile, new ThemeInfo());
  }

  public ThemeModule(File themeFile, ThemeInfo moduleInfo) {
    this.moduleInfo = moduleInfo;
    if (!moduleInfo.isDeleted() && themeFile != null) {
      LOG.debug("Looking for theme in {}", themeFile.getAbsolutePath());
      FileReader reader = null;
      try {
        reader = new FileReader(themeFile);
      } catch (FileNotFoundException e) {
        LOG.error("Theme file not found");
      }
      try {
        Theme theme = mapper.readValue(reader, Theme.class);
        Map<String, Theme> map = new HashMap<>();
        map.put(THEME_KEY, theme);
        moduleInfo.setThemeMap(map);
        LOG.debug("Loaded theme: {}", moduleInfo);
      } catch (IOException e) {
        LOG.error("Unable to parse theme file ", e);
        setValid(false);
        addError("Unable to parse theme file " + themeFile);
      }
    }
  }

  public ThemeModule(ThemeInfo moduleInfo) {
    this.moduleInfo = moduleInfo;
  }

  @Override
  public void resolve(ThemeModule parent, Map<String, StackModule> allStacks,
		  Map<String, ServiceModule> commonServices, Map<String, ExtensionModule> extensions) throws AmbariException {
    ThemeInfo parentModuleInfo = parent.getModuleInfo();

    if (parent.getModuleInfo() != null && !moduleInfo.isDeleted()) {
      if (moduleInfo.getThemeMap() == null || moduleInfo.getThemeMap().isEmpty()) {
        moduleInfo.setThemeMap(parentModuleInfo.getThemeMap());
      } else if(parentModuleInfo.getThemeMap() != null && !parentModuleInfo.getThemeMap().isEmpty()) {
        Theme childTheme = moduleInfo.getThemeMap().get(THEME_KEY);
        Theme parentTheme = parentModuleInfo.getThemeMap().get(THEME_KEY);
        childTheme.mergeWithParent(parentTheme);
      }
    }
  }

  @Override
  public ThemeInfo getModuleInfo() {
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
