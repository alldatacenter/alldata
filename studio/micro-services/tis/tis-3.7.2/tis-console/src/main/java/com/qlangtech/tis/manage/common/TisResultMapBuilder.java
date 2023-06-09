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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import org.apache.struts2.convention.ResultMapBuilder;
import org.apache.struts2.convention.annotation.Action;
import org.springframework.util.StringUtils;
import com.opensymphony.xwork2.ActionChainResult;
import com.opensymphony.xwork2.config.entities.PackageConfig;
import com.opensymphony.xwork2.config.entities.ResultConfig;
import com.qlangtech.tis.manage.common.valve.AjaxValve;
import com.qlangtech.tis.runtime.module.action.BasicModule;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2013-6-25
 */
public class TisResultMapBuilder implements ResultMapBuilder {

    private static final ResultConfig ACTION_RESULT_CONFIG
      = (new ResultConfig.Builder(BasicModule.key_FORWARD, TerminatorForwardResult.class.getName())).build();

    @Override
    public Map<String, ResultConfig> build(Class<?> actionClass, Action annotation, String actionName, PackageConfig packageConfig) {
        ResultConfig.Builder build = null;
        final String resultName = actionClass.getSimpleName();
        Map<String, ResultConfig> resultsConfig = new HashMap<String, ResultConfig>();
        resultsConfig.put(BasicModule.key_FORWARD, ACTION_RESULT_CONFIG);
        Matcher matcher = TisPackageBasedActionConfigBuilder.NAMESPACE_PATTERN.matcher(actionClass.getName());
        if (// || (matcher = TisPackageBasedActionConfigBuilder.NAMESPACE_TIS_PATTERN.matcher(actionClass.getName())).matches()
        matcher.matches()) {
            if ("action".equals(matcher.group(2))) {
                // process ajax
                String resultCode = resultName + "_ajax";
                build = new ResultConfig.Builder(resultCode, AjaxValve.class.getName());
                resultsConfig.put(resultCode, build.build());
                // process action submit
                resultCode = resultName + "_action";
                // final String resultCode = resultName.toString() + "_ajax";
                build = new ResultConfig.Builder(resultCode, ActionChainResult.class.getName());
                build.addParam("actionName", TisActionMapper.addUnderline(resultName).toString());
                build.addParam("namespace", "/" + matcher.group(1)
                  + StringUtils.replace(matcher.group(3), ".", "/") + "#screen");
                resultsConfig.put(resultCode, build.build());
            } else {
//                build = new ResultConfig.Builder(resultName, TerminatorVelocityResult.class.getName());
//                build.addParam("location", "/" + matcher.group(1) + "/templates/"
//                  + matcher.group(2) + StringUtils.replace(matcher.group(3), ".", "/") + '/' + getViewName(resultName) + ".vm");
//                resultsConfig.put(resultName, build.build());
            }
        } else {
            throw new IllegalStateException("class name :" + actionClass.getName() + " is illegal");
        }
        return resultsConfig;
    }

    private String getViewName(String resultName) {
        char[] simpleClassName = resultName.toCharArray();
        StringBuffer resultKey = new StringBuffer(String.valueOf(Character.toLowerCase(simpleClassName[0])));
        resultKey.append(simpleClassName, 1, simpleClassName.length - 1);
        return resultKey.toString();
    }
}
