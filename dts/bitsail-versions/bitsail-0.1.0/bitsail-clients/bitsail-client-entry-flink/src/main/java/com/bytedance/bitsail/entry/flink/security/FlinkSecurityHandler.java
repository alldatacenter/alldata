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

package com.bytedance.bitsail.entry.flink.security;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.component.format.security.kerberos.option.KerberosOptions;
import com.bytedance.bitsail.entry.flink.utils.FlinkPackageResolver;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.GlobalConfiguration;
import org.apache.flink.configuration.SecurityOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FlinkSecurityHandler {
  private static final Logger LOG = LoggerFactory.getLogger(FlinkSecurityHandler.class);

  private static final String CONTEXT_CLIENT = "Client";
  private static final String ENV_PROP_FLINK_CONF_DIR = "FLINK_CONF_DIR";

  public static void processSecurity(BitSailConfiguration sysConfiguration,
                                     ProcessBuilder processBuilder,
                                     Path flinkDir) throws IOException {
    if (!sysConfiguration.get(KerberosOptions.KERBEROS_ENABLE)) {
      return;
    }
    LOG.info("Kerberos (global) is enabled.");
    String principal = sysConfiguration.get(KerberosOptions.KERBEROS_PRINCIPAL);
    String keytabPath = sysConfiguration.get(KerberosOptions.KERBEROS_KEYTAB_PATH);
    String krb5Path = sysConfiguration.get(KerberosOptions.KERBEROS_KRB5_CONF_PATH);

    Configuration flinkConfiguration = loadFlinkConfiguration(FlinkPackageResolver.getFlinkConfDir(flinkDir));

    flinkConfiguration.set(SecurityOptions.KERBEROS_LOGIN_KEYTAB, keytabPath);
    flinkConfiguration.set(SecurityOptions.KERBEROS_LOGIN_PRINCIPAL, principal);
    flinkConfiguration.setString("security.kerberos.krb5-conf.path", krb5Path);

    String loginContexts = flinkConfiguration.get(SecurityOptions.KERBEROS_LOGIN_CONTEXTS);
    if (StringUtils.isNotEmpty(loginContexts)) {
      Set<String> contextSet = Arrays.stream(loginContexts.split(",")).collect(Collectors.toSet());
      contextSet.add(CONTEXT_CLIENT);
      loginContexts = contextSet.stream().collect(Collectors.joining(","));
      flinkConfiguration.set(SecurityOptions.KERBEROS_LOGIN_CONTEXTS, loginContexts);
    } else {
      flinkConfiguration.set(SecurityOptions.KERBEROS_LOGIN_CONTEXTS, CONTEXT_CLIENT);
    }

    Path tmpFlinkConfDir = writeConfToTmpFile(flinkConfiguration);
    symbolicLinkFlinkLog4j(flinkDir, tmpFlinkConfDir);
    exposeFlinkConfDir(processBuilder, tmpFlinkConfDir);
  }

  static Configuration loadFlinkConfiguration(Path flinkConfDir) {
    LOG.info("Load flink configuration from path: {}.", flinkConfDir);
    return GlobalConfiguration.loadConfiguration(flinkConfDir.toString());
  }

  public static Path writeConfToTmpFile(Configuration flinkConfiguration) throws IOException {
    File tmpDir = Files.createTempDir();

    File tmpFlinkConf = Paths.get(tmpDir.getPath(),
        UUID.randomUUID().toString(),
        FlinkPackageResolver.FLINK_CONF_FILE).toFile();

    if (tmpFlinkConf.exists()) {
      FileUtils.deleteQuietly(tmpFlinkConf);
    }
    LOG.info("Creating new tmp flink-conf file in path {}", tmpFlinkConf.toPath());
    Path tmpFlinkConfDir = Paths.get(tmpFlinkConf.getParent());
    Files.createParentDirs(tmpFlinkConf);
    //register delete on exit.
    tmpFlinkConfDir.toFile().deleteOnExit();
    tmpFlinkConf.createNewFile();
    tmpFlinkConf.deleteOnExit();

    try (FileWriter fileWriter = new FileWriter(tmpFlinkConf);
         PrintWriter printWriter = new PrintWriter(fileWriter)) {
      for (String key : flinkConfiguration.keySet()) {
        String value = flinkConfiguration.getString(key, null);
        printWriter.print(key);
        printWriter.print(": ");
        printWriter.println(value);
      }
      LOG.info("Success to write flink conf to file.");
      return tmpFlinkConfDir;
    }
  }

  /**
   * Flink will find log4j/logback configuration in the ENV property `FLINK_CONF_DIR`.
   * So if we want to change the flink conf dir to temporary dir, the log configuration file also need.
   */
  private static void symbolicLinkFlinkLog4j(Path flinkDir, Path tmpFlinkConfDir) throws IOException {
    Path flinkConfDir = FlinkPackageResolver.getFlinkConfDir(flinkDir);
    try (Stream<Path> flinkLogConfPath = java.nio.file.Files.list(flinkConfDir)) {
      List<Path> flinkLogConfPaths = flinkLogConfPath.filter(file -> file.getFileName().toString()
          .startsWith(FlinkPackageResolver.FLINK_LOG_FILE_PREFIX))
          .collect(Collectors.toList());
      for (Path flinkLogConf : flinkLogConfPaths) {
        Path resolve = tmpFlinkConfDir.resolve(flinkLogConf.getFileName());
        LOG.info("Create flink log symbolic link from {} to {}.", flinkLogConf, resolve);
        java.nio.file.Files.createSymbolicLink(resolve, flinkLogConf);
        resolve.toFile().deleteOnExit();
      }
    }
  }

  public static void exposeFlinkConfDir(ProcessBuilder procBuilder,
                                        Path tmpFlinkConfDir) {
    if (Objects.nonNull(tmpFlinkConfDir)) {
      Map<String, String> envProps = procBuilder.environment();
      envProps.put(ENV_PROP_FLINK_CONF_DIR, tmpFlinkConfDir.toString());
      LOG.info("Set env prop in procBuilder: {}={}", ENV_PROP_FLINK_CONF_DIR, tmpFlinkConfDir);
    }
  }
}
