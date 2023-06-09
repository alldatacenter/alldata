/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.metastore;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.metastore.config.MetastoreConfigConstants;
import org.apache.drill.metastore.config.MetastoreConfigFileInfo;
import org.apache.drill.metastore.exceptions.MetastoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Class is responsible for returning instance of {@link Metastore} class
 * which will be initialized based on {@link MetastoreConfigConstants#IMPLEMENTATION_CLASS} config property value.
 * Metastore initialization is delayed until {@link #get()} method is called.
 * Metastore implementation must have constructor which accepts {@link DrillConfig}.
 */
public class MetastoreRegistry implements AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger(MetastoreRegistry.class);

  private final DrillConfig config;
  // used only for testing to avoid searching overridden configuration files
  private final boolean useProvided;
  private volatile Metastore metastore;

  public MetastoreRegistry(DrillConfig config) {
    this.config = config;
    this.useProvided = config.hasPath(MetastoreConfigConstants.USE_PROVIDED_CONFIG)
      && config.getBoolean(MetastoreConfigConstants.USE_PROVIDED_CONFIG);
  }

  public Metastore get() {
    if (metastore == null) {
      synchronized (this) {
        if (metastore == null) {
          metastore = initMetastore();
        }
      }
    }
    return metastore;
  }

  private Metastore initMetastore() {
    DrillConfig metastoreConfig = useProvided ? config : createMetastoreConfig(config);
    String metastoreClass = metastoreConfig.getString(MetastoreConfigConstants.IMPLEMENTATION_CLASS);
    if (metastoreClass == null) {
      throw new MetastoreException(
        String.format("Drill Metastore class config is absent [%s]", MetastoreConfigConstants.IMPLEMENTATION_CLASS));
    }
    MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();
    MethodHandle constructor;
    try {
      MethodType methodType = MethodType.methodType(void.class, DrillConfig.class);
      constructor = publicLookup.findConstructor(Class.forName(metastoreClass), methodType);
    } catch (ClassNotFoundException e) {
      throw new MetastoreException(
        String.format("Unable to find Metastore implementation class [%s]", metastoreClass));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new MetastoreException(
        String.format("Metastore implementation class [%s] must have constructor which accepts [%s]",
          metastoreClass, metastoreConfig.getClass().getSimpleName()));
    }

    Object instance;
    try {
      instance = constructor.invokeWithArguments(metastoreConfig);
    } catch (Throwable e) {
      throw new MetastoreException(
        String.format("Unable to init Drill Metastore class [%s]", metastoreClass), e);
    }

    if (!(instance instanceof Metastore)) {
      throw new MetastoreException(
        String.format("Created instance of [%s] does not implement [%s] interface",
          instance.getClass().getSimpleName(), Metastore.class.getSimpleName()));
    }

    logger.info("Drill Metastore is initiated using {} class", metastoreClass);
    return (Metastore) instance;
  }

  /**
   * Creates Metastore Config and substitutes config values from Drill main config
   * for default and module configs only.
   *
   * For example, if Iceberg module config defines relative path based on Drill Zk root:
   * drill.metastore.iceberg.location.relative_path: ${drill.exec.zk.root}"/metastore/iceberg",
   * and Drill main config defines drill.exec.zk.root as "drill",
   * resulting Iceberg table relative path will be drill/metastore/iceberg.
   *
   * @param config main Drill config
   * @return metastore config
   */
  private DrillConfig createMetastoreConfig(DrillConfig config) {
    return DrillConfig.create(null, null, true, new MetastoreConfigFileInfo(), config.root());
  }

  @Override
  public void close() throws Exception {
    if (metastore != null) {
      metastore.close();
    }
  }
}
