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

import com.koubei.web.tag.pager.Pager.PageNumShowRule;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public abstract class AbstractPageNumShowRule implements PageNumShowRule {

    public static final String YK_IGNORE_TAG = "<span class=\"yk-pagination-lot-of-page\">...</span>";

    public static final String K2_IGNORE_TAG = "<span class=\"more\">...</span>";

    @Override
    public final int getRangeWidth() {
        return this.getPreOffset().getSetp() + this.getTailOffset().getSetp();
    }

    @Override
    public boolean isShowLastPage() {
        return false;
    }

    @Override
    public boolean isShowFirstPage() {
        return true;
    }

    public static class PageNumShowRule20101203 extends AbstractPageNumShowRule {

        private final String ignorTag;

        public PageNumShowRule20101203(String ignorTag) {
            this.ignorTag = ignorTag;
        }

        @Override
        public Offset getPreOffset() {
            return new Offset(3, ignorTag);
        }

        @Override
        public Offset getTailOffset() {
            return new Offset(3, ignorTag);
        }

        @Override
        public int startRangeLength() {
            return 7;
        }
    }
}
