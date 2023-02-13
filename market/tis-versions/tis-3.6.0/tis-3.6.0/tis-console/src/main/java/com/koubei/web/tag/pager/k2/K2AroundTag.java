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
package com.koubei.web.tag.pager.k2;

import java.text.MessageFormat;
import org.apache.commons.lang.StringUtils;
import com.koubei.web.tag.pager.AroundTag;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class K2AroundTag extends AroundTag {

    private boolean isRightAlign;

    private boolean containForm;

    // k2-pagination-right <div class=\"option\">
    private static final MessageFormat START_TAGE_FORMAT = new MessageFormat("<div class=\"k2-pagination{0}\">{1}");

    public K2AroundTag(boolean isRightAlign, boolean containForm) {
        super();
        this.isRightAlign = isRightAlign;
        this.containForm = containForm;
    }

    public boolean isContainForm() {
        return containForm;
    }

    /**
     * 是否右对齐
     *
     * @param isRightAlign
     */
    public K2AroundTag(boolean isRightAlign) {
        this(isRightAlign, false);
    }

    @Override
    public String getEnd() {
        return (isRightAlign && !containForm) ? "</div></div>" : "</div>";
    }

    @Override
    public String getStart() {
        Object[] args = isRightAlign ? new Object[] { " k2-pagination-right", !this.containForm ? "<div class=\"option\">" : StringUtils.EMPTY } : new Object[] { StringUtils.EMPTY, StringUtils.EMPTY };
        // "<div class=\"k2-pagination k2-pagination-right\"><div class=\"option\">";
        return START_TAGE_FORMAT.format(args);
    }

    public static void main(String[] arg) {
        K2AroundTag tag = new K2AroundTag(false, true);
        System.out.println(tag.getStart());
        System.out.println("=======");
        System.out.println(tag.getEnd());
    }
}
