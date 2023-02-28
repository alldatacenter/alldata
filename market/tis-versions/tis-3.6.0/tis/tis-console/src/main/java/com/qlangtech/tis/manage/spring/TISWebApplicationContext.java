/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.manage.spring;

import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.web.start.TisAppLaunch;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.web.context.support.ServletContextResourcePatternResolver;
import org.springframework.web.context.support.XmlWebApplicationContext;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-03-06 11:46
 */
public class TISWebApplicationContext extends XmlWebApplicationContext {

  private static final String RESOURCE_PREFIX_TIS_CLASSPATH = "tis:";

  @Override
  protected ResourcePatternResolver getResourcePatternResolver() {
    return new TISResourcePatternResolver(this);
  }

  private static class TISResourcePatternResolver extends ServletContextResourcePatternResolver {
    public TISResourcePatternResolver(ResourceLoader resourceLoader) {
      super(resourceLoader);
    }

    public Resource[] getResources(String locationPattern) throws IOException {
      if (StringUtils.startsWith(locationPattern, RESOURCE_PREFIX_TIS_CLASSPATH)) {
        return new Resource[]{new TISClassPathResource(StringUtils.substringAfter(locationPattern, RESOURCE_PREFIX_TIS_CLASSPATH))};
      }
      return super.getResources(locationPattern);
    }
  }

  private static class TISClassPathResource extends ClassPathResource {
    public TISClassPathResource(String path) {
      super(path);
    }

    public InputStream getInputStream() throws IOException {
      try {
        return super.getInputStream();
      } catch (IOException e) {
        Config.TestCfgStream stream = Config.openTestCfgStream();
        stream.validate(e);
//        if (stream.getPropsStream() == null) {
//          throw new RuntimeException("cfg props can not be find,prop file:" + stream.getPropsFile(), e);
//        }
        // 当前是测试模式
        TisAppLaunch.setTest(true);
        return stream.getPropsStream();
      }
    }
  }
}
