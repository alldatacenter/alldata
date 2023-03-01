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

package org.apache.uniffle.common.filesystem;

import java.util.concurrent.Callable;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.SecurityUtil;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.security.SecurityContextFactory;

/**
 * This HadoopFilesystemProvider will provide the only entrypoint to get the hadoop filesystem whether
 * the dfs cluster is kerberos enabled or not.
 */
public class HadoopFilesystemProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(HadoopFilesystemProvider.class);

  public static FileSystem getFilesystem(Path path, Configuration configuration) throws Exception {
    return getFilesystem(
        SecurityContextFactory.get().getSecurityContext().getContextLoginUser(),
        path,
        configuration
    );
  }

  public static FileSystem getFilesystem(String user, Path path, Configuration configuration) throws Exception {
    UserGroupInformation.AuthenticationMethod authenticationMethod =
        SecurityUtil.getAuthenticationMethod(configuration);
    boolean needSecurity = authenticationMethod != UserGroupInformation.AuthenticationMethod.SIMPLE;

    Callable<FileSystem> callable = () -> FileSystem.get(path.toUri(), configuration);

    FileSystem fileSystem;
    if (needSecurity) {
      fileSystem = SecurityContextFactory
          .get()
          .getSecurityContext()
          .runSecured(user, callable);
    } else {
      fileSystem = callable.call();
    }

    if (fileSystem instanceof LocalFileSystem) {
      LOGGER.debug("{} is local file system", path);
      return ((LocalFileSystem) fileSystem).getRawFileSystem();
    }

    return fileSystem;
  }
}
