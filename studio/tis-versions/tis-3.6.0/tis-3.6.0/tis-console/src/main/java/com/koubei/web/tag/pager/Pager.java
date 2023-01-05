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

import com.koubei.web.tag.pager.util.DefaultStaticTool;
import com.koubei.web.tag.pager.util.StaticTool;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class Pager {

  private StaticTool staticTool = new DefaultStaticTool();

  public void setStaticTool(StaticTool staticTool) {
    this.staticTool = staticTool;
  }

  private final LinkBuilder linkBuilder;

  public LinkBuilder getLinkBuilder() {
    return linkBuilder;
  }

  // 分页方案
  private String schema;

  public String getSchema() {
    return schema;
  }

  public void setSchema(String schema) {
    this.schema = schema;
  }

  // 当前是第几页
  private int curPage = 1;

  // 每页条数
  private int rowsPerPage = 20;

  // 总条数
  private int totalCount;

  public Pager(LinkBuilder builder) {
    this.linkBuilder = builder;
  }

  public int getTotalPage() {
    int totalPage = this.getTotalCount() / this.getRowsPerPage();
    if (this.getTotalCount() % this.getRowsPerPage() != 0) {
      totalPage++;
    }
    return totalPage;
  }

  public String getUrl(int page) {
    // url.append("page=").append(page);
    return staticTool.process(this.linkBuilder.getPageUrl(page).toString());
  }

  /**
   * 取得当前页面linkurl
   *
   * @return
   */
  public String getCurrentPageUrl() {
    return getUrl(this.getCurPage());
  }

  public static Pager register(String name, LinkBuilder builder, HttpServletRequest request) {
    Pager pager = new Pager(builder);
    PagerDTO dto = PagerDTO.get(request);
    dto.add(name, pager);
    return pager;
  }

  /**
   * @return
   */
  public String getLink() {
    // 总页数
    final int totlePage = this.getTotalPage();
    // 总页数只有一页话不显示page控件
    if (totlePage < 2) {
      return StringUtils.EMPTY;
    }
    StringBuffer pageHtml = new StringBuffer(getStyle().getAroundStyle().getStart());
    // 当前页码
    final int curPage = this.getCurPage();
    final Pager.DirectJump directJump = this.getStyleFactory().getDirectJump();
    // 直接跳转外括标签输出，比如form标签
    pageHtml.append(directJump.getAroundTag().getStart());
    // 页面统计信息输出
    getPageStatistics().build(pageHtml, curPage, totlePage);
    final int startIndex = getStartIndex();
    final int endIndex = getEndIndex();
    // 是否是第一页
    if (curPage < 2) {
      pageHtml.append(getStyle().getCurrentPageTag(1));
    } else {
      // (, );
      this.getStyle().popPerPageLink(pageHtml, curPage - 1);
      if (showFirstPage()) {
        popDivHreLink(pageHtml, 1, "1");
      }
    }
    if (startIndex > 1 && this.getCurPage() >= this.getPageNumShowRule().startRangeLength()) {
      pageHtml.append(getPreIgnore());
    }
    for (int i = 2; i <= totlePage; i++) {
      if (startIndex <= i && i <= endIndex) {
        if (curPage == i) {
          pageHtml.append(getStyle().getCurrentPageTag(curPage));
        } else {
          popDivHreLink(pageHtml, i, String.valueOf(i));
        }
      }
      if (i == endIndex && totlePage > endIndex) {
        pageHtml.append(getTailIgnore());
        if (getPageNumShowRule().isShowLastPage()) {
          popDivHreLink(pageHtml, totlePage, String.valueOf(totlePage));
        }
      }
    }
    if (!(curPage == totlePage || totlePage == 0)) {
      // popDivHreLink(pageHtml, curPage + 1, "下一页",
      // "yk-pagination-next");
      // (pageHtml,
      this.getStyle().popNextPageLink(pageHtml, curPage + 1);
      // page);
    }
    directJump.build(pageHtml);
    pageHtml.append(directJump.getAroundTag().getEnd());
    pageHtml.append(getStyle().getAroundStyle().getEnd());
    return pageHtml.toString();
  }

  private boolean showFirstPage() {
    int startIndex = this.getStartIndex();
    if (startIndex != 1)
      return this.getPageNumShowRule().isShowFirstPage();
    return true;
  }

  private String getPreIgnore() {
    return this.getPageNumShowRule().getPreOffset().getIgnor();
  }

  private String getTailIgnore() {
    return this.getPageNumShowRule().getTailOffset().getIgnor();
  }

  /**
   * 前部偏移量
   *
   * @return
   */
  private int getStartIndex() {
    int answer = // x
      this.getCurPage() - this.getPageNumShowRule().getPreOffset().getSetp();
    int tailOffset = this.getCurPage() + this.getPageNumShowRule().getTailOffset().getSetp();
    if (tailOffset > this.getTotalPage()) {
      answer -= (tailOffset - this.getTotalPage());
    }
    return answer < 1 ? 1 : answer;
  }

  /**
   * 后部偏移量
   *
   * @return
   */
  private int getEndIndex() {
    final int answer = this.getStartIndex() + this.getPageNumShowRule().getRangeWidth();
    return (answer > this.getTotalPage()) ? this.getTotalPage() : answer;
  }

  private PageStatistics getPageStatistics() {
    return this.getStyleFactory().getPageStatistics();
  }

  private void popDivHreLink(StringBuffer pageHtml, int page, String name) {
    this.getStyle().popDivHreLink(pageHtml, page, name);
  }

  private void popDivHreLink(StringBuffer pageHtml, int page, String name, String cssClass) {
    this.getStyle().popDivHreLink(pageHtml, page, name, cssClass);
  }

  private PageNumShowRule getPageNumShowRule() {
    return this.getStyleFactory().getPageNumShowRule();
  }

  public int getCurPage() {
    if (this.curPage < 1) {
      return 1;
    }
    if (this.getTotalPage() > 0 && this.curPage > this.getTotalPage()) {
      this.curPage = this.getTotalPage();
    }
    return this.curPage;
  }

  public void setCurPage(int curPage) {
    this.curPage = curPage;
  }

  public int getRowsPerPage() {
    return rowsPerPage;
  }

  public void setRowsPerPage(int rowsPerPage) {
    this.rowsPerPage = rowsPerPage;
  }

  public int getTotalCount() {
    return totalCount;
  }

  public void setTotalCount(int totalCount) {
    this.totalCount = totalCount;
  }

  private NavigationStyle getStyle() {
    return getStyleFactory().getNaviagationStyle();
  }

  private StyleFactory getStyleFactory() {
    return StyleFactory.getInstance(this);
  }

  public interface NavigationStyle {

    AroundTag getAroundStyle();

    String getCurrentPageTag(int page);

    void popDivHreLink(StringBuffer pageHtml, int page, String name);

    /**
     * @param pageHtml
     * @param page
     * @param name
     * @param cssClass
     */
    void popDivHreLink(StringBuffer pageHtml, int page, String name, String cssClass);

    /**
     * 创建前一页链接
     */
    void popPerPageLink(StringBuffer pageHtml, int page);

    /**
     * 创建后一页链接
     */
    void popNextPageLink(StringBuffer pageHtml, int page);
    // public String getIgnoreTag();
  }

  /**
   * 页面统计接口
   */
  public interface PageStatistics {

    void build(StringBuffer builder, int page, int pageCount);
  }

  public interface DirectJump {

    void build(StringBuffer builder);

    AroundTag getAroundTag();
  }

  public interface PageNumShowRule {

    /**
     * 取得前偏移值
     *
     * @return
     */
    Offset getPreOffset();

    /**
     * 取得后偏移值
     *
     * @return
     */
    Offset getTailOffset();

    /**
     * 是否显示最后一页
     *
     * @return
     */
    boolean isShowLastPage();

    boolean isShowFirstPage();

    /**
     * 开始使用分页省略符号的页码
     *
     * @return
     */
    int startRangeLength();

    int getRangeWidth();
  }
}
