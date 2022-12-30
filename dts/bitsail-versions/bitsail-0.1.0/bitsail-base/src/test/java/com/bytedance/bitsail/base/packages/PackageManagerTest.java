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

package com.bytedance.bitsail.base.packages;

import com.bytedance.bitsail.base.execution.ExecutionEnviron;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PackageManagerTest {

  private final String[] plugins = {"plugin/test1", "plugin/test2", "plugin/test3"};
  private BitSailConfiguration jobConf;
  private ExecutionEnviron mockedEnv;

  @Before
  public void init() throws URISyntaxException {
    jobConf = BitSailConfiguration.newDefault();
    jobConf.set(CommonOptions.DRY_RUN, false);
    jobConf.set(CommonOptions.PRINT_LOADED_URLS, false);
    jobConf.set(CommonOptions.ENABLE_DYNAMIC_LOADER, true);
    jobConf.set(CommonOptions.JOB_PLUGIN_LIB_PATH, "plugin");
    jobConf.set(CommonOptions.JOB_PLUGIN_CONF_PATH, "plugin_conf");
    jobConf.set(CommonOptions.STATIC_LIB_DIR, "plugin");
    jobConf.set(CommonOptions.STATIC_LIB_CONF_FILE, "static_lib.json");
    jobConf.set(CommonOptions.JOB_PLUGIN_ROOT_PATH,
        Objects.requireNonNull(Paths.get(PackageManager.class.getResource("/classloader/").toURI()).toString()));

    mockedEnv = Mockito.mock(ExecutionEnviron.class);
    Mockito.doNothing().when(mockedEnv).registerLibraries(Mockito.anyList());
  }

  @Test
  public void testDynamicLoadPackage() {
    PackageManager packageManager = PackageManager.getInstance(mockedEnv, jobConf);
    packageManager.loadDynamicLibrary("test2", (Function<ClassLoader, Object>) Object::toString);
    verifyRegisterLib();
  }

  private void verifyRegisterLib() {
    ArgumentCaptor<?> argumentCaptor = ArgumentCaptor.forClass(List.class);
    Mockito.verify(mockedEnv, Mockito.times(1)).registerLibraries((List<URI>) argumentCaptor.capture());
    List<String> uriList = ((List<?>) argumentCaptor.getValue()).stream()
        .map(Object::toString)
        .collect(Collectors.toList());

    final AtomicInteger index = new AtomicInteger(0);
    do {
      AtomicBoolean foundPlugin = new AtomicBoolean(false);
      uriList.forEach(uri -> {
        if (uri.contains(plugins[index.get()])) {
          foundPlugin.set(true);
        }
      });
      Assert.assertTrue(foundPlugin.get());
    } while (index.incrementAndGet() < plugins.length);
  }
}
