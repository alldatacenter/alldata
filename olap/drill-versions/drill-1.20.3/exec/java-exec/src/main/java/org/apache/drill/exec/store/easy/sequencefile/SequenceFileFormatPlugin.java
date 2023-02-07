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
package org.apache.drill.exec.store.easy.sequencefile;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.physical.impl.scan.file.FileScanFramework.FileReaderFactory;
import org.apache.drill.exec.physical.impl.scan.file.FileScanFramework.FileScanBuilder;
import org.apache.drill.exec.physical.impl.scan.file.FileScanFramework.FileSchemaNegotiator;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.store.dfs.easy.EasyFormatPlugin;
import org.apache.drill.exec.store.dfs.easy.EasySubScan;
import org.apache.hadoop.conf.Configuration;

public class SequenceFileFormatPlugin extends EasyFormatPlugin<SequenceFileFormatConfig> {

  public static final String OPERATOR_TYPE = "SEQUENCE_SUB_SCAN";

  public SequenceFileFormatPlugin(String name,
                                  DrillbitContext context,
                                  Configuration fsConf,
                                  StoragePluginConfig storageConfig,
                                  SequenceFileFormatConfig formatConfig) {
    super(name, easyConfig(fsConf, formatConfig), context, storageConfig, formatConfig);
  }

  private static EasyFormatConfig easyConfig(Configuration fsConf, SequenceFileFormatConfig pluginConfig) {
    return EasyFormatConfig.builder()
        .readable(true)
        .writable(false)
        .blockSplittable(true)
        .compressible(true)
        .extensions(pluginConfig.getExtensions())
        .fsConf(fsConf)
        .readerOperatorType(OPERATOR_TYPE)
        .useEnhancedScan(true)
        .supportsLimitPushdown(true)
        .supportsProjectPushdown(true)
        .defaultName(SequenceFileFormatConfig.NAME)
        .build();
  }

  private static class SequenceFileReaderFactory extends FileReaderFactory {

    private final SequenceFileFormatConfig config;
    private final EasySubScan scan;

    public SequenceFileReaderFactory(SequenceFileFormatConfig config, EasySubScan scan) {
      this.config = config;
      this.scan = scan;
    }

    @Override
    public ManagedReader<? extends FileSchemaNegotiator> newReader() {
      return new SequenceFileBatchReader(config, scan);
    }

  }

  @Override
  public ManagedReader<? extends FileSchemaNegotiator> newBatchReader(EasySubScan scan, OptionManager options)
      throws ExecutionSetupException {
    return new SequenceFileBatchReader(formatConfig, scan);
  }

  @Override
  protected FileScanBuilder frameworkBuilder(OptionManager options, EasySubScan scan) throws ExecutionSetupException {
    FileScanBuilder builder = new FileScanBuilder();
    builder.setReaderFactory(new SequenceFileReaderFactory(formatConfig, scan));

    initScanBuilder(builder, scan);
    builder.nullType(Types.optional(MinorType.VARCHAR));
    return builder;
  }
}