/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.datax.plugin.writer.hudi;

import com.qlangtech.tis.extension.impl.PluginManifest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.ResourceBundle;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-01-28 12:35
 **/
public class HudiConfig {
    private static HudiConfig config;

    private final String sparkPackageName;
    private final String sparkDistDirName;
    private final File sparkHome;

    private HudiConfig() {
        ResourceBundle bundle =
                ResourceBundle.getBundle(StringUtils.replace(HudiConfig.class.getPackage().getName(), ".", "/") + "/config");
        sparkPackageName = bundle.getString("sparkPackageName");
        if (StringUtils.isEmpty(this.sparkPackageName)) {
            throw new IllegalStateException("config prop sparkPackageName can not be null");
        }
        sparkDistDirName = bundle.getString("sparkDistDirName");
        if (StringUtils.indexOf(sparkDistDirName, "$") > -1) {
            throw new IllegalStateException("sparkDistDirName is illegal:" + sparkDistDirName);
        }

        File hudiLibDir = getHudiPluginLibDir();
        File hudiPluginDir = new File(hudiLibDir, "../..");
        sparkHome = new File(hudiPluginDir, sparkDistDirName);
        if (!sparkHome.exists()) {
            throw new IllegalStateException("sparkHome is not exist:" + sparkHome.getAbsolutePath());
        }
        // 有执行权限
        FileUtils.listFiles(new File(sparkHome, "bin"), null, true)
                .forEach((file) -> {
                    file.setExecutable(true, false);
                });
    }

    public static File getHudiDependencyDir() {
        File hudiLibDir = getHudiPluginLibDir();
        File hudiDependencyDir = new File(hudiLibDir, "../../tis-datax-hudi-dependency");
        hudiDependencyDir = hudiDependencyDir.toPath().normalize().toFile();
        if (!hudiDependencyDir.exists()) {
            throw new IllegalStateException("hudiDependencyDir is not exist:" + hudiDependencyDir.getAbsolutePath());
        }
        return hudiDependencyDir;
    }

    private static File getHudiPluginLibDir() {
        return PluginManifest.create(HudiConfig.class).getPluginLibDir();
        // return Config.getPluginLibDir();


//        Optional<PluginClassifier> classifier = null;
//        String hudiDataXPlugin = "tis-datax-hudi-plugin";
//        for (PluginWrapper p : TIS.get().getPluginManager().activePlugins) {
//            if (hudiDataXPlugin.equals(p.getShortName())) {
//                classifier = p.getClassifier();
//                if (classifier.isPresent()) {
//                    return Config.getPluginLibDir(classifier.get().getTPIPluginName(hudiDataXPlugin));
//                }
//            }
//        }
//        throw new IllegalStateException("can not find plugin:" + hudiDataXPlugin);
    }

    public static File getSparkHome() {
        return getInstance().sparkHome;
    }

    public static String getSparkPackageName() {
        return getInstance().sparkPackageName;
    }

    public static String getSparkReleaseDir() {
        return getInstance().sparkDistDirName;
    }

    private static HudiConfig getInstance() {
        if (config == null) {
            synchronized (HudiConfig.class) {
                if (config == null) {
                    config = new HudiConfig();
                }
            }
        }
        return config;
    }
}
