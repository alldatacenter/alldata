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
package com.koubei.web.tag.pager;

import org.apache.commons.lang.StringUtils;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public abstract class AroundTag {

    public abstract String getStart();

    public abstract String getEnd();

    public static AroundTag AroundTag1 = new AroundTag() {

        public String getStart() {
            return "<div class=\"pagination\">\n<ul>\n";
        }

        public String getEnd() {
            return "\n</ul></div>\n";
        }
    };

    public static AroundTag GlobalTag1 = new AroundTag() {

        public String getStart() {
            return "<div class='yk-pagination'>\n";
        }

        public String getEnd() {
            return "\n</div>\n";
        }
    };

    public static AroundTag NULL = new AroundTag() {

        public String getStart() {
            return StringUtils.EMPTY;
        }

        public String getEnd() {
            return StringUtils.EMPTY;
        }
    };
}
