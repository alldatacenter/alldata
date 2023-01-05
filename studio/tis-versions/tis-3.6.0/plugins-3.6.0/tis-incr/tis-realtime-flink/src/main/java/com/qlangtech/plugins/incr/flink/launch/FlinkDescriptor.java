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

package com.qlangtech.plugins.incr.flink.launch;

import com.google.common.collect.Lists;
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.util.OverwriteProps;
import com.qlangtech.tis.manage.common.Option;
import org.apache.commons.lang3.EnumUtils;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.configuration.description.Description;
import org.apache.flink.configuration.description.HtmlFormatter;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-03-04 12:02
 **/
public class FlinkDescriptor<T extends Describable> extends Descriptor<T> {

    protected void addFieldDescriptor(String fieldName, ConfigOption<?> configOption) {
        addFieldDescriptor(fieldName, configOption, new OverwriteProps());
    }

    protected void addFieldDescriptor(String fieldName, ConfigOption<?> configOption, OverwriteProps overwriteProps) {
        Description desc = configOption.description();
        HtmlFormatter htmlFormatter = new HtmlFormatter();

        Object dftVal = overwriteProps.processDftVal(configOption.defaultValue());


        StringBuffer helperContent = new StringBuffer(htmlFormatter.format(desc));
        if (overwriteProps.appendHelper.isPresent()) {
            helperContent.append("\n\n").append(overwriteProps.appendHelper.get());
        }

        Class<?> targetClazz = getTargetClass(configOption);

        List<Option> opts = null;
        if (targetClazz == Duration.class) {
            if (dftVal != null) {
                dftVal = ((Duration) dftVal).getSeconds();
            }
            helperContent.append("\n\n 单位：`秒`");
        } else if (targetClazz == MemorySize.class) {
            if (dftVal != null) {
                dftVal = ((MemorySize) dftVal).getKibiBytes();
            }
            helperContent.append("\n\n 单位：`kb`");
        } else if (targetClazz.isEnum()) {
            List<Enum> enums = EnumUtils.getEnumList((Class<Enum>) targetClazz);
            opts = enums.stream().map((e) -> new Option(e.name())).collect(Collectors.toList());
        } else if (targetClazz == Boolean.class) {
            opts = Lists.newArrayList(new Option("是", true), new Option("否", false));
        }

        this.addFieldDescriptor(fieldName, dftVal, helperContent.toString()
                , overwriteProps.opts.isPresent() ? overwriteProps.opts : Optional.ofNullable(opts));
    }

    private static Method getClazzMethod;

    private static Class<?> getTargetClass(ConfigOption<?> configOption) {
        try {
            if (getClazzMethod == null) {
                getClazzMethod = ConfigOption.class.getDeclaredMethod("getClazz");
                getClazzMethod.setAccessible(true);
            }
            return (Class<?>) getClazzMethod.invoke(configOption);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
