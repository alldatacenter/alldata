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

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class FormtDirectJump implements Pager.DirectJump {

    private final Pager pager;

    private final DirectJumpCss css;

    public FormtDirectJump(Pager pager, DirectJumpCss css) {
        this.pager = pager;
        this.css = css;
    }

    public void build(StringBuffer b) {
        final String randomKey = RandomStringUtils.randomAlphabetic(3);
        int size = 1;
        int page = pager.getTotalPage();
        while ((page = page / 10) > 0) {
            size++;
        }
        b.append("<span class='");
        b.append(css.quickJumpCss).append("'>快速至第<input maxlength=\"").append(size).append("\" size=\"").append(size).append("\" type='text' name=\"page\" class='").append(css.quickJumpPagerCss).append("' />页</span>");
        // b.append("<button type='submit' onclick=\"return ").append(randomKey)
        // .append("shortcutjump(form);\" class='").append(css.buttonCss)
        // .append("'>跳转</button>\n");
        b.append("<button type='submit' class='").append(css.buttonCss).append("'>跳转</button>\n");
    // b.append("<script language=\"javascript\">\n");
    // b.append("function ").append(randomKey).append("shortcutjump(form){\n");
    // b.append("var pageVal = parseInt(form.elements[\"page\"].value);\n");
    // b.append("if(isNaN(pageVal)||pageVal>").append(pager.getTotalPage())
    // .append("||pageVal== ").append(pager.getCurPage()).append(
    // "){\n");
    // b.append("alert(\"请输入正确的页码\");\n");
    // b.append("form.elements[\"page\"].select();\n");
    // b.append("return false;\n");
    // b.append("}\n");
    // b.append(" return true;\n");
    // b.append("}\n");
    // b.append("</script>\n");
    }

    public AroundTag getAroundTag() {
        return new AroundTag() {

            @Override
            public String getEnd() {
                return "</form>";
            }

            @Override
            public String getStart() {
                return "<form method=\"post\" action='" + (pager.getLinkBuilder().getPagerUrl()) + "' " + (StringUtils.isNotEmpty(css.formClass) ? ("class='" + css.formClass + "' ") : StringUtils.EMPTY) + ">";
            }
        };
    }

    /**
     * 定义控件上的各种css样式
     */
    public static class DirectJumpCss {

        // <span class="jump">快速至第<input type="text" class="jump-inp"/>页</span>
        private final String quickJumpCss;

        private final String quickJumpPagerCss;

        // <button type="submit" class="submit">跳转</button>
        private final String buttonCss;

        private final String formClass;

        public DirectJumpCss(String quickJumpCss, String quickJumpPagerCss, String buttonCss, String formClass) {
            super();
            this.quickJumpCss = quickJumpCss;
            this.quickJumpPagerCss = quickJumpPagerCss;
            this.buttonCss = buttonCss;
            this.formClass = formClass;
        }
    }
}
