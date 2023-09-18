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
import org.apache.commons.lang.StringUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年8月28日
 */
public class TisUTF8 {

    public static void main(String[] args) {
        Charset big5 = Charset.forName("big5");
        System.out.println(big5);
        List<Option> all = allSupported();
        for (Option o : all) {
            System.out.println(o.getName() + ":" + o.getValue());
        }
    }

    public static List<Option> allSupported() {
        List<Option> all = Lists.newArrayList();
        Option o = null;
        for (Map.Entry<String, Charset> entry : Charset.availableCharsets().entrySet()) {
            if (entry.getKey().startsWith("x-")) {
                continue;
            }
            if (entry.getKey().startsWith("IBM")) {
                continue;
            }
            if (entry.getKey().startsWith("windows-")) {
                continue;
            }
            if (entry.getKey().startsWith("ISO-")) {
                continue;
            }
            o = new Option(entry.getKey(), StringUtils.lowerCase(entry.getKey()));
            all.add(o);
        }
        return all;
    }

    private TisUTF8() {
    }

    public static Charset get() {
        return StandardCharsets.UTF_8;
    }

    public static String getName() {
        return get().name();
    }
}
