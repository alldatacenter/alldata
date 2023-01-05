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

import com.google.common.collect.Lists;
import com.qlangtech.tis.manage.common.TisUTF8;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-03-10 10:51
 **/
public class TypedPropertiesBuilder implements IPropertiesBuilder {
    private List<String[]> props = Lists.newArrayList();

    public void setProperty(String key, String value) {
        props.add(new String[]{key, value});
    }

    public void store(OutputStream write) throws IOException {
        if (props.isEmpty()) {
            throw new IllegalStateException("props can not be null");
        }
        IOUtils.write(props.stream().map((prop) -> prop[0] + "=" + StringUtils.trimToEmpty(prop[1])).collect(Collectors.joining("\n")), write, TisUTF8.get());
    }
}
