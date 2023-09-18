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
import com.koubei.web.tag.pager.Pager.DirectJump;
import com.koubei.web.tag.pager.Pager.NavigationStyle;
import com.koubei.web.tag.pager.Pager.PageNumShowRule;
import com.koubei.web.tag.pager.Pager.PageStatistics;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public abstract class GlobalStyleFactory extends StyleFactory {

    public GlobalStyleFactory(Pager pager) {
        super(pager);
    }

    /*
	 * (non-Javadoc)
	 *
	 * @see com.koubei.ring.util.StyleFactory#getDirectJump()
	 */
    @Override
    public DirectJump getDirectJump() {
        return new FormtDirectJump(this.getPager(), getDirectJumpCss());
    }

    protected FormtDirectJump.DirectJumpCss getDirectJumpCss() {
        return new FormtDirectJump.DirectJumpCss("yk-pagination-quick-jump", "yk-pagination-jump-inp", "yk-pagination-submit", null);
    }

    private final NavigationStyle naviagationStyle = new NavigationStyle20101203();

    @Override
    public NavigationStyle getNaviagationStyle() {
        return naviagationStyle;
    }

    protected class NavigationStyle20101203 implements NavigationStyle {

        @Override
        public AroundTag getAroundStyle() {
            return AroundTag.GlobalTag1;
        }

        @Override
        public String getCurrentPageTag(int page) {
            return "<strong class='yk-pagination-current'>" + page + "</strong>";
        }

        @Override
        public final void popDivHreLink(StringBuffer pageHtml, int page, String name) {
            popDivHreLink(pageHtml, page, name, StringUtils.EMPTY);
        }

        @Override
        public final void popDivHreLink(StringBuffer pageHtml, int page, String name, String cssClass) {
            pageHtml.append("<a href='").append(getPager().getUrl(page)).append("'");
            if (StringUtils.isNotEmpty(cssClass)) {
                pageHtml.append(" class='").append(cssClass).append("'");
            }
            pageHtml.append(" >");
            pageHtml.append(name).append("</a>");
        }

        @Override
        public void popNextPageLink(StringBuffer pageHtml, int page) {
            popDivHreLink(pageHtml, page, "下一页", "yk-pagination-next");
        }

        @Override
        public void popPerPageLink(StringBuffer pageHtml, int page) {
            popDivHreLink(pageHtml, page, "上一页", "yk-pagination-pre");
        }
    }

    @Override
    public PageNumShowRule getPageNumShowRule() {
        return new AbstractPageNumShowRule.PageNumShowRule20101203(AbstractPageNumShowRule.YK_IGNORE_TAG);
    }

    @Override
    public PageStatistics getPageStatistics() {
        return new PageStatistics() {

            @Override
            public void build(StringBuffer builder, int page, int pageCount) {
                builder.append("<span class='yk-pagination-sum'>共<b>").append(pageCount).append("</b>页</span>");
            }
        };
    }
}
