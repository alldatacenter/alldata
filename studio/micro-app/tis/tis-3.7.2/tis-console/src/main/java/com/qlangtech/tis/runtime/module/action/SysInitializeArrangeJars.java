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
package com.qlangtech.tis.runtime.module.action;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.qlangtech.tis.manage.common.Option;
import com.qlangtech.tis.web.start.TisSubModule;
import com.qlangtech.tis.util.Memoizer;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * 重新整理 项目中的jar包，可以使得整个Uber包可以做到最小
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-03-22 14:59
 */
public class SysInitializeArrangeJars {
  // private static final List<Option> subDirs = Lists.newArrayList("tis-assemble", "solr", "tjs", "tis-collect");

  private static final String tis_builder_api = "tis-builder-api(.*)\\.jar";
  private static final String tis_manage_pojo = "tis-manage-pojo(.*)\\.jar";
  private static final String spring_web = "spring-web-(.*)\\.jar";

  private static final String commons_ = "commons-(.*)\\.jar";

//  private static final List<SubProj> subDirs
//    = Lists.newArrayList( //
//    new SubProj("tis-assemble", "tis-assemble\\.jar", tis_builder_api, tis_manage_pojo, spring_web) //
//    , new SubProj("solr", "solr\\.jar", tis_builder_api, tis_manage_pojo, spring_web)
//    , new SubProj("tjs", "tis\\.jar", tis_builder_api, tis_manage_pojo, spring_web)
//    , new SubProj("tis-collect", "tis-collect\\.jar", tis_builder_api, tis_manage_pojo, spring_web));


  private static final List<SubProj> subDirs
    = Lists.newArrayList( //
    new SubProj(TisSubModule.TIS_ASSEMBLE, commons_) //
    , new SubProj(TisSubModule.ZEPPELIN, commons_)
    , new SubProj(TisSubModule.TIS_CONSOLE, commons_)
    //  , new SubProj("tis-collect", commons_)
  );

  static final Memoizer<String, List<File>> jars = new Memoizer<String, List<File>>() {
    @Override
    public List<File> compute(String key) {
      return Lists.newArrayList();
    }
  };

  public static void main(String[] args) throws Exception {
    if (args.length < 1) {
      throw new IllegalStateException("please set uberDir ");
    }
    File uberDir = new File(args[0]);
    if (!uberDir.exists()) {
      throw new IllegalStateException("uberDir is not exist:" + uberDir.getAbsolutePath());
    }
    File subModuleLibDir = null;

    final File webStartDir = new File(uberDir, TisSubModule.WEB_START.moduleName + "/lib");
    Set<String> existJarFiles = Sets.newHashSet(webStartDir.list());
    if (existJarFiles.size() < 1) {
      throw new IllegalStateException("webStartDir:" + webStartDir.getAbsolutePath() + " has any jar file");
    }

    // 将web-start中已有jar在子工程中去掉
    for (SubProj sbDir : subDirs) {
      subModuleLibDir = new File(uberDir, sbDir.getName() + "/lib");
      if (!subModuleLibDir.getParentFile().exists()) {
        // throw new IllegalStateException("sub lib dir:" + subModuleLibDir.getAbsolutePath() + " is not exist");
        continue;
      }
      for (String jarFileName : subModuleLibDir.list()) {
        if (existJarFiles.contains(jarFileName)) {
          FileUtils.deleteQuietly(new File(subModuleLibDir, jarFileName));
        }
      }
    }

    for (SubProj sbDir : subDirs) {
      subModuleLibDir = new File(uberDir, sbDir.getName() + "/lib");
      if (!subModuleLibDir.getParentFile().exists()) {
        // throw new IllegalStateException("sub lib dir:" + subModuleLibDir.getAbsolutePath() + " is not exist");
        continue;
      }
      for (String jarFileName : subModuleLibDir.list()) {
        if (sbDir.isMatch(jarFileName)) {
          jars.get(jarFileName).add(new File(subModuleLibDir, jarFileName));
        }
      }
    }


    for (Map.Entry<String, List<File>> subModuleJar : jars.getEntries()) {
      System.out.println("process file:" + subModuleJar.getKey());
      boolean first = true;
      for (File f : subModuleJar.getValue()) {
        if (first) {
          File dest = new File(webStartDir, subModuleJar.getKey());
          if (dest.exists()) {
            FileUtils.deleteQuietly(f);
          } else {
            FileUtils.moveFile(f, dest);
          }
          first = false;
        } else {
          FileUtils.deleteQuietly(f);
        }
      }
    }
  }


//  private static void forceDeleteOnExit(File f) {
//    try {
//      FileUtils.forceDeleteOnExit(f);
//    } catch (IOException e) {
//      throw new IllegalStateException("path:" + f.getAbsolutePath(), e);
//    }
//  }

  private static class SubProj extends Option {
    // 需要保留的jar包
    private final List<Pattern> retainJars = Lists.newArrayList();

    public SubProj(TisSubModule name, String value, String... retainJar) {
      super(name.moduleName, value);
      retainJars.add(Pattern.compile(value));
      for (String jar : retainJar) {
        retainJars.add(Pattern.compile(jar));
      }
    }

    public boolean isMatch(String jarFileName) {
      for (Pattern p : retainJars) {
        if (p.matcher(jarFileName).matches()) {
          return true;
        }
      }
      return false;
    }
  }

}
