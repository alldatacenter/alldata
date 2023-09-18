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

import com.koubei.web.tag.pager.Pager.DirectJump;
import com.koubei.web.tag.pager.Pager.PageStatistics;
import com.koubei.web.tag.pager.k2.K2AroundTag;
import com.koubei.web.tag.pager.k2.K2StyleFactory;

/**
 * 风格抽象工厂
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public abstract class StyleFactory {

    public abstract Pager.NavigationStyle getNaviagationStyle();

    public abstract Pager.PageStatistics getPageStatistics();

    public abstract Pager.DirectJump getDirectJump();

    public abstract Pager.PageNumShowRule getPageNumShowRule();

    public static final DirectJump NULL_DIRECT_JUMP = new DirectJump() {

        public void build(StringBuffer builder) {
            return;
        }

        public AroundTag getAroundTag() {
            return AroundTag.NULL;
        }
    };

    public static final PageStatistics NULL_PAGE_STSTISTICS = new PageStatistics() {

        @Override
        public void build(StringBuffer builder, int page, int pageCount) {
            return;
        }
    };

    // public static final PageNumShowRule COMMON_PAGE_NUM_SHOW_RULE =
    private final Pager pager;

    public StyleFactory(Pager pager) {
        this.pager = pager;
    }

    protected Pager getPager() {
        return this.pager;
    }

    private static final boolean IS_RIGHT_ALIGN_TRUE = true;

    private static final boolean IS_RIGHT_ALIGN_FALSE = !IS_RIGHT_ALIGN_TRUE;

    private static final boolean HAS_DIRECT_JUMP = true;

    private static final boolean HAS_NOT_DIRECT_JUMP = !HAS_DIRECT_JUMP;

    public static StyleFactory getInstance(final Pager pager) {
        final String schema = pager.getSchema();
        // }
        if ("k1".equalsIgnoreCase(schema)) {
            return new K2StyleFactory(pager, new K2AroundTag(IS_RIGHT_ALIGN_TRUE, HAS_NOT_DIRECT_JUMP), true);
        }
        // }
        throw new IllegalArgumentException("invalid schema value has not match[" + schema + "]");
    }
}
