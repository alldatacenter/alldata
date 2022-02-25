package org.apache.ambari.funtest.server.orm;

/**
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

import java.io.File;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ControllerModule;

import com.google.inject.AbstractModule;

public class InMemoryDefaultTestModule extends AbstractModule {

  /**
   * Saves all {@link ControllerModule} logic, but changes bean discovery mechanism.
   * In this implementation scan for {@link org.apache.ambari.server.EagerSingleton}
   * and {@link org.apache.ambari.server.StaticallyInject} and
   * {@link org.apache.ambari.server.AmbariService} annotations will not be run for every test.
   */
  private static class BeanDefinitionsCachingTestControllerModule extends ControllerModule {

    // Access should be synchronised to allow concurrent test runs.
    private static final AtomicReference<Set<Class<?>>> matchedAnnotationClasses = new AtomicReference<>(
        null);

    public BeanDefinitionsCachingTestControllerModule(Properties properties) throws Exception {
      super(properties);
    }

    @Override
    protected Set<Class<?>> bindByAnnotation(Set<Class<?>> matchedClasses) {
      Set<Class<?>> newMatchedClasses = super.bindByAnnotation(matchedAnnotationClasses.get());
      matchedAnnotationClasses.compareAndSet(null, Collections.unmodifiableSet(newMatchedClasses));
      return null;
    }
  }

  Properties properties = new Properties();

  @Override
  protected void configure() {
    String stacks = "src/test/resources/stacks";
    String version = "src/test/resources/version";
    String sharedResourcesDir = "src/test/resources/";
    if (System.getProperty("os.name").contains("Windows")) {
      stacks = ClassLoader.getSystemClassLoader().getResource("stacks").getPath();
      version = new File(new File(ClassLoader.getSystemClassLoader().getResource("").getPath()).getParent(), "version").getPath();
      sharedResourcesDir = ClassLoader.getSystemClassLoader().getResource("").getPath();
    }

    properties.setProperty(Configuration.SERVER_PERSISTENCE_TYPE.getKey(), "in-memory");
    properties.setProperty(Configuration.METADATA_DIR_PATH.getKey(), stacks);
    properties.setProperty(Configuration.SERVER_VERSION_FILE.getKey(), version);
    properties.setProperty(Configuration.OS_VERSION.getKey(), "centos6");
    properties.setProperty(Configuration.SHARED_RESOURCES_DIR.getKey(), sharedResourcesDir);

    try {
      install(new BeanDefinitionsCachingTestControllerModule(properties));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Properties getProperties() {
    return properties;
  }
}
