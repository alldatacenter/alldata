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

package org.apache.ambari.server.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmbariPath {
  private static final Logger LOG = LoggerFactory.getLogger(AmbariPath.class);
  
  public static final String AMBARI_SERVER_ROOT_ENV_VARIABLE = "ROOT";
  public static final String rootDirectory = System.getenv(AMBARI_SERVER_ROOT_ENV_VARIABLE);
  
  public static String getPath(String path) {
    if(rootDirectory == null) {  
      LOG.warn("Cannot get $ROOT enviroment variable. Installed to custom root directory Ambari might not work correctly.");
      return path;
    }
    String result = (rootDirectory + path).replaceAll("/+","/");
    return result;
  }
}
