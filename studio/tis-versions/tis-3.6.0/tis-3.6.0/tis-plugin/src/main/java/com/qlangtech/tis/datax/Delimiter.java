package com.qlangtech.tis.datax;

import com.qlangtech.tis.manage.common.Option;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-10-16 17:45
 **/
public enum Delimiter {
    Comma("comma", ',') //
    , Tab("tab", '\t') //
    , Char001("char001", '\001') //
    , Char005("char005", '\005');

    public static List<Option> options() {
        return Arrays.stream(Delimiter.values()).map((e) -> new Option(e.name(), e.token)).collect(Collectors.toList());
    }

    public static Delimiter parse(String token) {
        for (Delimiter d : Delimiter.values()) {
            if (d.token.equalsIgnoreCase(token)) {
                return d;
            }
        }
        throw new IllegalStateException("illegal token:" + token);
    }

    private final String token;
    public final char val;

    private Delimiter(String token, char val) {
        this.token = token;
        this.val = val;
    }
}
