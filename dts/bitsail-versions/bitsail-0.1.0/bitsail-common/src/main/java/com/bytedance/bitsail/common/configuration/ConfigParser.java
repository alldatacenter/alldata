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

package com.bytedance.bitsail.common.configuration;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.exception.FrameworkErrorCode;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.common.option.ConfigOption;
import com.bytedance.bitsail.common.option.ReaderOptions;
import com.bytedance.bitsail.common.option.WriterOptions;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.bytedance.bitsail.common.option.CommonOptions.JOB_COMMON;
import static com.bytedance.bitsail.common.option.ReaderOptions.BaseReaderOptions.READER_PARALLELISM_NUM;

public class ConfigParser {

  public static BitSailConfiguration fromRawConfPath(final String path) {
    String jobContent = getJobContent(path);
    return BitSailConfiguration.from(jobContent);
  }

  private static String getJobContent(String jobResource) {
    String jobContent;
    // jobResource only support local file
    try {
      jobContent = FileUtils.readFileToString(new File(jobResource));
    } catch (IOException e) {
      throw BitSailException.asBitSailException(FrameworkErrorCode.CONFIG_ERROR, "Can't get configuration info: " + jobResource, e);
    }

    if (jobContent == null) {
      throw BitSailException.asBitSailException(FrameworkErrorCode.CONFIG_ERROR, "Can't get configuration info: " + jobResource);
    }
    return jobContent;
  }

  public static BitSailConfiguration getCommonConf(BitSailConfiguration conf) {
    return conf.getConfiguration(JOB_COMMON).clone();
  }

  public static BitSailConfiguration getSysCommonConf(BitSailConfiguration conf) {
    return conf.getConfiguration(JOB_COMMON).clone()
        .merge(conf.getConfiguration(BitSailSystemConfiguration.DEFAULT_SYSTEM_OPTIONAL_KEY), false);
  }

  public static BitSailConfiguration getInputConf(BitSailConfiguration conf) {
    return conf.getConfiguration(ReaderOptions.JOB_READER).clone();
  }

  public static BitSailConfiguration getOutputConf(BitSailConfiguration conf) {
    return conf.getConfiguration(WriterOptions.JOB_WRITER).clone();
  }

  public static long getJobId(BitSailConfiguration conf) {
    return conf.getNecessaryOption(CommonOptions.JOB_ID, FrameworkErrorCode.REQUIRED_VALUE);
  }

  public static long getInstanceId(BitSailConfiguration conf) {
    return conf.getNecessaryOption(CommonOptions.INSTANCE_ID, FrameworkErrorCode.REQUIRED_VALUE);
  }

  public static Integer getReaderParallelismNum(BitSailConfiguration conf) {
    return conf.get(READER_PARALLELISM_NUM);
  }

  public static String getUnnecessaryKeyFromExtraProp(BitSailConfiguration conf, String key, String defaultValue) {
    Map<String, String> extraProp = conf.getUnNecessaryMap(CommonOptions.EXTRA_PROPERTIES);
    return extraProp.getOrDefault(key, defaultValue);
  }

  public static List<BitSailConfiguration> getInputConfList(BitSailConfiguration conf) {
    if (conf.fieldExists(ReaderOptions.READER_CONFIG_LIST)) {
      return getConfList(conf, ReaderOptions.READER_CONFIG_LIST);
    }
    return Arrays.asList(getInputConf(conf));
  }

  public static List<BitSailConfiguration> getOutputConfList(BitSailConfiguration conf) {
    if (conf.fieldExists(WriterOptions.WRITER_CONFIG_LIST)) {
      return getConfList(conf, WriterOptions.WRITER_CONFIG_LIST);
    }
    return Arrays.asList(getOutputConf(conf));
  }

  private static List<BitSailConfiguration> getConfList(BitSailConfiguration conf,
                                                        ConfigOption<List<Map<String, Object>>> option) {
    List<Map<String, Object>> subConfMapList = conf.getNecessaryOption(option, CommonErrorCode.CONFIG_ERROR);
    return subConfMapList.stream().map(subConfMap -> {
      BitSailConfiguration subConf = BitSailConfiguration.newDefault();
      subConfMap.forEach(subConf::set);
      return subConf;
    }).collect(Collectors.toList());
  }
}
