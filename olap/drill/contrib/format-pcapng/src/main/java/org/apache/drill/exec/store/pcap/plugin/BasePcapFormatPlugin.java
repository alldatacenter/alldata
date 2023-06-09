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
package org.apache.drill.exec.store.pcap.plugin;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.physical.impl.scan.file.FileScanFramework.FileReaderFactory;
import org.apache.drill.exec.physical.impl.scan.file.FileScanFramework.FileScanBuilder;
import org.apache.drill.exec.physical.impl.scan.file.FileScanFramework.FileSchemaNegotiator;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedScanFramework;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.dfs.easy.EasyFormatPlugin;
import org.apache.drill.exec.store.dfs.easy.EasySubScan;
import org.apache.drill.exec.store.pcap.PcapBatchReader;
import org.apache.drill.exec.store.pcap.decoder.PacketDecoder;
import org.apache.drill.exec.store.pcapng.PcapngBatchReader;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public abstract class BasePcapFormatPlugin<T extends PcapFormatConfig> extends EasyFormatPlugin<T> {

  static final Logger logger = LoggerFactory.getLogger(ManagedScanFramework.class);
  private static PacketDecoder.FileFormat fileFormat = PacketDecoder.FileFormat.UNKNOWN;

  public BasePcapFormatPlugin(String name,
                              DrillbitContext context,
                              Configuration fsConf,
                              StoragePluginConfig storageConfig,
                              T formatConfig) {
    super(name, easyConfig(fsConf, formatConfig), context, storageConfig, formatConfig);
  }

  private static EasyFormatConfig easyConfig(Configuration fsConf, PcapFormatConfig pluginConfig) {
    return EasyFormatConfig.builder()
        .readable(true)
        .writable(false)
        .blockSplittable(false)
        .compressible(true)
        .extensions(pluginConfig.getExtensions())
        .fsConf(fsConf)
        .useEnhancedScan(true)
        .supportsLimitPushdown(true)
        .supportsProjectPushdown(true)
        .defaultName(PcapFormatConfig.NAME)
        .build();
  }

  private static class PcapReaderFactory extends FileReaderFactory {

    private final PcapFormatConfig config;
    private final EasySubScan scan;

    public PcapReaderFactory(PcapFormatConfig config, EasySubScan scan) {
      this.config = config;
      this.scan = scan;
    }

    /**
     * Reader creator. If file format can't be detected try to use default PCAP format plugin
     *
     * @return PCAP or PCAPNG batch reader
     */
    @Override
    public ManagedReader<? extends FileSchemaNegotiator> newReader() {
      if (fileFramework().isPresent()) { // todo: can be simplified with java9 ifPresentOrElse
        Path path = scan.getWorkUnits().stream()
                .findFirst()
                .orElseThrow(() -> UserException.
                        dataReadError()
                        .addContext("There are no files for scanning")
                        .build(logger))
                .getPath();
        fileFormat = getFileFormat(fileFramework().get().fileSystem(), path);
        if (config.getExtensions().stream()
                .noneMatch(f -> f.equals(fileFormat.name().toLowerCase()))) {
          logger.error("File format {} is not within plugin extensions: {}. Trying to use default PCAP format plugin to " +
                  "read the file", fileFormat, config.getExtensions());
        }
      } else {
        logger.error("It is not possible to detect file format, because the File Framework is not initialized. " +
                "Trying to use default PCAP format plugin to read the file");
      }
      return createReader(scan, config);
    }
  }

  @Override
  public ManagedReader<? extends FileSchemaNegotiator> newBatchReader(EasySubScan scan, OptionManager options) {
    return createReader(scan, formatConfig);
  }

  private static ManagedReader<? extends FileSchemaNegotiator> createReader(EasySubScan scan, PcapFormatConfig config) {
    switch(fileFormat) {
      case PCAPNG: return new PcapngBatchReader(config, scan);
      case PCAP:
      case UNKNOWN:
      default: return new PcapBatchReader(config, scan.getMaxRecords());
    }
  }

  @Override
  protected FileScanBuilder frameworkBuilder(OptionManager options, EasySubScan scan) {
    FileScanBuilder builder = new FileScanBuilder();
    builder.setReaderFactory(new PcapReaderFactory(formatConfig, scan));

    initScanBuilder(builder, scan);
    builder.nullType(Types.optional(MinorType.VARCHAR));
    return builder;
  }

  /**
   * Helper method to detect PCAP or PCAPNG file format based on file Magic Number
   *
   * @param dfs for obtaining InputStream
   * @return PCAP/PCAPNG file format
   */
  private static PacketDecoder.FileFormat getFileFormat(DrillFileSystem dfs, Path path) {
    try (InputStream inputStream = dfs.openPossiblyCompressedStream(path)) {
      PacketDecoder decoder = new PacketDecoder(inputStream);
      return decoder.getFileFormat();
    } catch (IOException io) {
      throw UserException
              .dataReadError(io)
              .addContext("File name:", path.toString())
              .build(logger);
    }
  }
}
