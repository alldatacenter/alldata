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

package org.apache.ambari.view.commons.hdfs;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.utils.hdfs.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ViewPropertyHelper {
  private static final Logger LOG = LoggerFactory.getLogger(ConfigurationBuilder.class);

  public static Optional<Map<String, String>> getViewConfigs(ViewContext context, String viewConfigPropertyName) {
    Map<String, String> viewConfigs = new HashMap<>();
    String keyValues = context.getProperties().get(viewConfigPropertyName);
    LOG.debug("{} : {}", viewConfigPropertyName, keyValues);
    if (Strings.isNullOrEmpty(keyValues)) {
      LOG.info("No values found in {} property.", viewConfigPropertyName);
      return Optional.absent();
    }

    for (String entry : keyValues.split(";")) {
      String[] kv = entry.split("=");
      if (kv.length != 2) {
        LOG.error("Ignoring entry {}, because it is not formatted like key=value");
        continue;
      }

      viewConfigs.put(kv[0], kv[1]);
    }

    return Optional.of(viewConfigs);
  }
}
