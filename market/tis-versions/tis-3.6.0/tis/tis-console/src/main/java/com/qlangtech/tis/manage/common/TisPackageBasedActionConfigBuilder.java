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
package com.qlangtech.tis.manage.common;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.opensymphony.xwork2.ObjectFactory;
import com.opensymphony.xwork2.config.Configuration;
import com.opensymphony.xwork2.config.entities.PackageConfig;
import com.opensymphony.xwork2.config.entities.PackageConfig.Builder;
import com.opensymphony.xwork2.inject.Container;
import com.opensymphony.xwork2.inject.Inject;
import com.opensymphony.xwork2.util.AnnotationUtils;
import com.opensymphony.xwork2.util.finder.ClassFinder;
import com.opensymphony.xwork2.util.finder.ClassFinder.ClassInfo;
import com.opensymphony.xwork2.util.finder.Test;
import com.opensymphony.xwork2.util.logging.Logger;
import com.opensymphony.xwork2.util.logging.LoggerFactory;
import com.qlangtech.tis.web.start.TisAppLaunch;
import junit.framework.Assert;
import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.convention.ConventionConstants;
import org.apache.struts2.convention.PackageBasedActionConfigBuilder;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.DefaultInterceptorRef;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TisPackageBasedActionConfigBuilder extends PackageBasedActionConfigBuilder {

  private final PackageConfig parentPkgConfig;

  private static final Pattern filePattern = Pattern.compile("jar:(.+?)!.+");

  private static final Logger LOG = LoggerFactory.getLogger(TisPackageBasedActionConfigBuilder.class);

  public static void main(String[] args) throws Exception {
    Matcher m = filePattern.matcher("jar:file:/opt/app/spring-boot/tjs/lib/tis.jar!/com/qlangtech/tis/runtime/module/screen");
    if (m.matches()) {
      System.out.println(new URL(m.group(1)));
    }
  }

  @Inject
  public TisPackageBasedActionConfigBuilder(Configuration configuration, Container container, ObjectFactory objectFactory, @Inject("struts.convention.redirect.to.slash") String redirectToSlash, @Inject("struts.convention.default.parent.package") String defaultParentPackage) {
    super(configuration, container, objectFactory, redirectToSlash, defaultParentPackage);
    this.parentPkgConfig = configuration.getPackageConfig("default");
    Assert.assertNotNull(this.parentPkgConfig);
  }

  @Override
  protected Test<ClassInfo> getActionClassTest() {
    return super.getActionClassTest();
  }

  public static final Pattern NAMESPACE_PATTERN = Pattern.compile("com\\.qlangtech\\.tis\\.(\\w+)\\.module\\.(screen|action|control)(.*)(\\..*)$");

  // public static final Pattern NAMESPACE_TIS_PATTERN = Pattern.compile("com\\.qlangtech\\.tis\\.(\\w+)\\.module\\.(screen|action|control)(.*)(\\..*)$");
  private String[] tisActionPackages;

  @Inject(value = ConventionConstants.CONVENTION_ACTION_PACKAGES, required = false)
  public void setActionPackages(String actionPackages) {
    super.setActionPackages(actionPackages);
    if (org.apache.commons.lang3.StringUtils.isNotBlank(actionPackages)) {
      this.tisActionPackages = actionPackages.split("\\s*[,]\\s*");
    }
  }

  @Override
  protected ClassFinder buildClassFinder(Test<String> classPackageTest, final List<URL> urls) {
    // File jar = new File("./");
//        String[] targetJars = jar.list(new FilenameFilter() {
//
//            @Override
//            public boolean accept(File dir, String name) {
//                return org.apache.commons.lang.StringUtils.endsWith(name, ".jar");
//            }
//        });
    // if ( targetJars == null || targetJars.length != 1) {
    if (TisAppLaunch.isTestMock()) {
      // 在开发环境中运行
      return super.buildClassFinder(classPackageTest, urls);
    }
    //
    try {
      // 在生产环境中运行
      Set<URL> url = Sets.newHashSet();
      URL l = null;
      Enumeration<URL> res = null;
      for (String scanPackage : this.tisActionPackages) {
        scanPackage = StringUtils.replace(scanPackage, ".", "/");
        res = this.getClassLoader().getResources(scanPackage);
        while (res.hasMoreElements()) {
          l = res.nextElement();
          Matcher m = filePattern.matcher(l.toString());
          if (m.matches()) {
            url.add(new URL(m.group(1)));
          }
        }
      }
      return super.buildClassFinder(classPackageTest, Lists.newArrayList(url));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected Builder getPackageConfig(Map<String, Builder> packageConfigs, String actionNamespace, String actionPackage, Class<?> actionClass, Action action) {
    Matcher matcher = NAMESPACE_PATTERN.matcher(actionClass.getName());
    // 解析struts2的命名空间
    String name = null;
    if (// || (matcher = NAMESPACE_TIS_PATTERN.matcher(actionClass.getName())).matches()
      matcher.matches()) {
      name = '/' + matcher.group(1) + StringUtils.replace(matcher.group(3), ".", "/") + "#" + matcher.group(2);
    } else {
      throw new IllegalStateException("actionPackage:" + actionPackage + " is not a valid actionPackage");
    }
    PackageConfig.Builder pkgConfig = packageConfigs.get(name);
    if (pkgConfig == null) {
      pkgConfig = new PackageConfig.Builder(name).namespace(name).addParent(this.parentPkgConfig);
      // add by baisui: 2020/7/13
      pkgConfig.strictMethodInvocation(false);
      packageConfigs.put(name, pkgConfig);
      // check for @DefaultInterceptorRef in the package
      DefaultInterceptorRef defaultInterceptorRef = AnnotationUtils.findAnnotation(actionClass, DefaultInterceptorRef.class);
      if (defaultInterceptorRef != null) {
        pkgConfig.defaultInterceptorRef(defaultInterceptorRef.value());
        if (LOG.isTraceEnabled())
          LOG.debug("Setting [#0] as the default interceptor ref for [#1]", defaultInterceptorRef.value(), pkgConfig.getName());
      }
    }
    if (LOG.isTraceEnabled()) {
      LOG.trace("Created package config named [#0] with a namespace [#1]", name, actionNamespace);
    }
    return pkgConfig;
  }
}
