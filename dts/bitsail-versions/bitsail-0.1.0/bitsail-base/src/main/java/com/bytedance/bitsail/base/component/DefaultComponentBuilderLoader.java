/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.base.component;

import com.bytedance.bitsail.base.extension.Component;
import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.util.Preconditions;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Created 2022/8/31
 */
public class DefaultComponentBuilderLoader<T> implements Serializable {
  private static final Logger LOG = LoggerFactory.getLogger(DefaultComponentBuilderLoader.class);
  private final Class<T> clazz;
  public Map<String, T> components = Maps.newHashMap();
  private volatile boolean loaded;

  public DefaultComponentBuilderLoader(Class<T> clazz) {
    this.clazz = Preconditions.checkNotNull(clazz);
  }

  public T loadComponent(String componentName) {
    if (!loaded) {
      loadAllComponents();
      loaded = true;
    }
    componentName = StringUtils.lowerCase(componentName);
    if (!components.containsKey(componentName)) {
      throw new BitSailException(CommonErrorCode.CONFIG_ERROR,
          String.format("Component %s not in interface %s support until now.", componentName, clazz));
    }
    return components.get(componentName);
  }

  private void loadAllComponents() {
    ServiceLoader<T> loadedComponents = ServiceLoader.load(clazz);
    for (T component : loadedComponents) {
      if (!(component instanceof Component)) {
        LOG.warn("Component {} not implement from interface component, skip load it.", component);
        continue;
      }
      String componentName = ((Component) component).getComponentName();
      LOG.info("Component {} loaded in clazz {}.", componentName, clazz);
      components.put(StringUtils.lowerCase(componentName), component);
    }
  }
}
