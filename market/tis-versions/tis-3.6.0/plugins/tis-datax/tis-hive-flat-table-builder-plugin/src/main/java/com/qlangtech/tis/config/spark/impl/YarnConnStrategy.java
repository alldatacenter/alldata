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

package com.qlangtech.tis.config.spark.impl;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.config.Utils;
import com.qlangtech.tis.config.spark.ISparkConnGetter;
import com.qlangtech.tis.config.spark.SparkConnStrategy;
import com.qlangtech.tis.config.yarn.IYarnConfig;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-06-14 17:40
 **/
public class YarnConnStrategy extends SparkConnStrategy {
    private static final Logger logger = LoggerFactory.getLogger(YarnConnStrategy.class);



    @FormField(ordinal = 1, type = FormFieldType.TEXTAREA, validate = {Validator.require})
    public String yarnSite;

    @Override
    public String getSparkMaster(File cfgDir) {

        Utils.setHadoopConfig2Local(cfgDir, IYarnConfig.FILE_NAME_YARN_SITE, yarnSite);

//        try {
//            File ys = new File(cfgDir, IYarnConfig.FILE_NAME_YARN_SITE);
////           // String yarnConfigDir = System.getenv("YARN_CONF_DIR");
////            if (StringUtils.isEmpty(yarnConfigDir) || !yarnConfigDir.equals(cfgDir.getCanonicalPath())) {
////                throw new IllegalStateException("yarnConfigDir is illegal:" + yarnConfigDir
////                        + ",cfgDir:" + cfgDir.getCanonicalPath());
////            }
//            if (!ys.exists()) {
//                FileUtils.write(ys, yarnSite, TisUTF8.get(), false);
//            } else if (!StringUtils.equals(MD5Utils.md5file(yarnSite.getBytes(TisUTF8.get())), MD5Utils.md5file(ys))) {
//                // 先备份
//                FileUtils.moveFile(ys, new File(cfgDir, IYarnConfig.FILE_NAME_YARN_SITE + "_bak_" + IParamContext.getCurrentTimeStamp()));
//                FileUtils.write(ys, yarnSite, TisUTF8.get(), false);
//            }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

        return IYarnConfig.KEY_DISPLAY_NAME;
    }

    public static String dftYarnSiteContent() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "\n" +
                "<configuration>\n" +
                "    <property>\n" +
                "        <name></name>\n" +
                "        <value></value>\n" +
                "    </property>\n" +
                "</configuration> ";
    }


    @TISExtension
    public static class DefaultDesc extends Descriptor<SparkConnStrategy> {
        public DefaultDesc() {
            super();
        }

        public boolean validateYarnSite(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {

            try {
                Configuration conf = new Configuration();
                try (InputStream input = new ByteArrayInputStream(value.getBytes(TisUTF8.get()))) {
                    conf.addResource(input);
                }
                conf.size();
            } catch (Exception e) {
                logger.warn(e.getMessage(), e);
                msgHandler.addFieldError(context, fieldName, e.getMessage());
                return false;
            }


            return true;
        }


        @Override
        public String getDisplayName() {
            return "Yarn";
        }
    }
}
