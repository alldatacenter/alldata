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

package org.apache.ambari.view.pig.utils;

import com.google.common.base.Optional;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.commons.hdfs.ViewPropertyHelper;
import org.apache.ambari.view.pig.templeton.client.TempletonApi;
import org.apache.ambari.view.pig.templeton.client.TempletonApiFactory;
import org.apache.ambari.view.utils.UserLocal;
import org.apache.ambari.view.utils.hdfs.HdfsApi;
import org.apache.ambari.view.utils.hdfs.HdfsApiException;
import org.apache.ambari.view.utils.hdfs.HdfsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class UserLocalObjects {
  public static final String VIEW_CONF_KEYVALUES = "view.conf.keyvalues";

  private final static Logger LOG =
      LoggerFactory.getLogger(UserLocalObjects.class);

  /**
   * HdfsApi user-local instance
   */
  private static UserLocal<HdfsApi> hdfsApi;

  /**
   * TempletonApi user-local instance
   */
  private static UserLocal<TempletonApi> templetonApi;

  static {
    templetonApi = new UserLocal<TempletonApi>(TempletonApi.class) {
      @Override
      protected synchronized TempletonApi initialValue(ViewContext context) {
        TempletonApiFactory templetonApiFactory = new TempletonApiFactory(context);
        return templetonApiFactory.connectToTempletonApi();
      }
    };

    hdfsApi = new UserLocal<HdfsApi>(HdfsApi.class) {
      @Override
      protected synchronized HdfsApi initialValue(ViewContext context) {
        try {
          Optional<Map<String, String>> props = ViewPropertyHelper.getViewConfigs(context, VIEW_CONF_KEYVALUES);
          HdfsApi api;
          if(props.isPresent()){
            api = HdfsUtil.connectToHDFSApi(context, props.get());
          }else{
            api = HdfsUtil.connectToHDFSApi(context);
          }

          return api;
        } catch (HdfsApiException e) {
          throw new ServiceFormattedException(e);
        }
      }
    };
  }

  public static HdfsApi getHdfsApi(ViewContext context) {
    return hdfsApi.get(context);
  }

  public static void setHdfsApi(HdfsApi api, ViewContext context) {
    hdfsApi.set(api, context);
  }

  public static TempletonApi getTempletonApi(ViewContext context) {
    return templetonApi.get(context);
  }

  public static void setTempletonApi(TempletonApi api, ViewContext context) {
    templetonApi.set(api, context);
  }
}
