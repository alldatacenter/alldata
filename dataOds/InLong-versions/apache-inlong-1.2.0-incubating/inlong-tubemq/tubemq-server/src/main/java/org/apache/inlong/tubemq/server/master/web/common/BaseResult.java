/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.master.web.common;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;

/**
 * Paging algorithm package.
 *
 * Pagination must be set: TotalItem (the total number of bars), the default is 0,
 * should be set in dao PageSize (number of pages per page), should be set in the web
 * layer QueryBase defaults to 20, subclasses can be overwritten getDefaultPageSize()
 * Modify CurrentPage (current page), default is 1, home page, should be set in the
 * web layer. After paging, you can get: TotalPage (total number of pages) FirstItem
 * (the current page starts recording position, counting from 1) PageLastItem
 * (current page last recording position) On the page, the number of pages displayed
 * per page should be: lines , the current page name should be: page
 *
 * Add the render link function at the same time,
 * the subclass overrides the getParameters method and returns valid parameters.
 */
public class BaseResult implements Serializable {
    private static final long serialVersionUID = 8807356835558347735L;
    private static final Integer defaultPageSize = 20;
    private static final Integer defaultFirstPage = 1;
    private static final Integer defaultTotalItem = 0;
    /**
     * max page size
     */
    private static final int MAX_PAGE_SIZE = 501;
    private Integer totalItem;
    private Integer pageSize;
    private Integer currentPage;
    // for paging
    private int startRow;
    private int endRow;
    private Map tempParam;
    private Object removeObject;
    // for ajax
    private String ajaxPrefix;
    private String ajaxSuffix;
    private String charset;
    private String from;
    private boolean escape = true;
    private boolean jsEscape = false;

    /**
     * parse date
     *
     * @param dateTime   the string date time
     * @param format     the date format
     * @param def        the defalut date value
     */
    public static Date parseDate(String dateTime, String format, Date def) {
        Date date = def;
        try {
            DateFormat formatter = new SimpleDateFormat(format);
            date = formatter.parse(dateTime);
        } catch (Exception e) {
            DateFormat f = DateFormat.getDateInstance();
            try {
                date = f.parse(dateTime);
            } catch (Exception ee) {
                // ignore
            }
        }
        return date;
    }

    /**
     * Encoding according to the escape method in javascript
     *
     * @return Encoded string
     */
    public static String jsEncode(String str) {
        if (null == str) {
            return null;
        }

        char[] cs = str.toCharArray();
        StringBuilder sBuilder = new StringBuilder(str.length());

        for (int i = 0; i < cs.length; ++i) {
            int c = cs[i] & 0xFFFF;

            if (((c >= '0') && (c <= '9')) || (((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z')))) {
                sBuilder.append(cs[i]);
            } else {
                sBuilder.append('%');

                if (c > 255) {
                    sBuilder.append('u');
                }

                sBuilder.append(Integer.toHexString(c));
            }
        }

        return sBuilder.toString();
    }

    /**
     * Parsing a string encoded with javascript's escape,
     * equivalent to javascript's unescape
     *
     * @return Encoded string
     */
    public static String jsDecode(String str) {
        if (null == str) {
            return null;
        }

        StringBuilder sBuilder = new StringBuilder(str.length());
        char[] cs = str.toCharArray();

        for (int i = 0; i < cs.length; ++i) {
            int c = cs[i] & 0xFFFF;

            if (c == '%') {
                if (cs[i + 1] == 'u') {
                    if ((i + 6) > cs.length) {
                        sBuilder.append(cs[i]);
                    } else {
                        try {
                            char cc = (char) Integer.parseInt(new String(cs, i + 2, 4), 16);
                            sBuilder.append(cc);
                            i += 5;
                        } catch (Exception e) {
                            sBuilder.append(cs[i]);
                        }
                    }
                } else {
                    if ((i + 3) > cs.length) {
                        sBuilder.append(cs[i]);
                    } else {
                        try {
                            char cc = (char) Integer.parseInt(new String(cs, i + 1, 2), 16);
                            sBuilder.append(cc);
                            i += 2;
                        } catch (Exception e) {
                            sBuilder.append(cs[i]);
                        }
                    }
                }
            } else {
                sBuilder.append(cs[i]);
            }
        }

        return sBuilder.toString();
    }

    /**
     * Get default page size.
     *
     * @return Returns the defaultPageSize.
     */
    protected Integer getDefaultPageSize() {
        return defaultPageSize;
    }

    public boolean isFirstPage() {
        return this.getCurrentPage().intValue() == 1;
    }

    public int getPreviousPage() {
        int back = this.getCurrentPage().intValue() - 1;

        if (back <= 0) {
            back = 1;
        }

        return back;
    }

    public boolean isLastPage() {
        return this.getTotalPage() == this.getCurrentPage().intValue();
    }

    public int getNextPage() {
        int back = this.getCurrentPage().intValue() + 1;

        if (back > this.getTotalPage()) {
            back = this.getTotalPage();
        }

        return back;
    }

    /**
     * Get current page value
     *
     * @return Returns the currentPage.
     */
    public Integer getCurrentPage() {
        if (currentPage == null) {
            return defaultFirstPage;
        }

        return currentPage;
    }

    /**
     * Set current page value
     *
     * @param cPage The currentPage to set.
     */
    public void setCurrentPage(Integer cPage) {
        if ((cPage == null) || (cPage <= 0)) {
            this.currentPage = null;
        } else {
            this.currentPage = cPage;
        }
        setStartEndRow();
    }

    public void setCurrentPageString(String pageString) {
        if (isBlankString(pageString)) {
            this.setCurrentPage(defaultFirstPage);
        }

        try {
            Integer integer = new Integer(pageString);
            this.setCurrentPage(integer);
        } catch (NumberFormatException ignore) {
            this.setCurrentPage(defaultFirstPage);
        }
    }

    private void setStartEndRow() {
        this.startRow = this.getPageSize().intValue() * (this.getCurrentPage().intValue() - 1);
        this.endRow = this.startRow + this.getPageSize().intValue();
    }

    /**
     * Get page size value
     * @return Returns the pageSize.
     */
    public Integer getPageSize() {
        if (pageSize == null) {
            return getDefaultPageSize();
        }

        return pageSize;
    }

    /**
     * Set page size value
     *
     * @param pSize The pageSize to set.
     */
    public void setPageSize(Integer pSize) {

        if ((pSize == null) || (pSize < 0)) {
            this.pageSize = null;
        } else if (pSize > MAX_PAGE_SIZE || pSize < 1) {
            throw new IllegalArgumentException("The number of displayed pages per page ranges from 1~" + MAX_PAGE_SIZE);
        } else {
            this.pageSize = pSize;
        }
        setStartEndRow();
    }

    public boolean hasSetPageSize() {
        return pageSize != null;
    }

    public void setPageSizeString(String pageSizeString) {
        if (isBlankString(pageSizeString)) {
            return;
        }

        try {
            Integer integer = new Integer(pageSizeString);
            if (integer > MAX_PAGE_SIZE || integer < 1) {
                throw new IllegalArgumentException("The number of displayed pages per page ranges from 1~"
                        + MAX_PAGE_SIZE);
            }
            this.setPageSize(integer);
        } catch (NumberFormatException ignore) {
            //
        }
    }

    /**
     * Determine if the string is blank
     *
     * @param pageSizeString   pagesize value
     * @return result
     */
    private boolean isBlankString(String pageSizeString) {
        if (pageSizeString == null) {
            return true;
        }

        return pageSizeString.trim().length() == 0;

    }

    /**
     * Get total item value
     * @return Returns the totalItem.
     */
    public Integer getTotalItem() {
        if (totalItem == null) {
            // throw new IllegalStateException("Please set the TotalItem first.");
            return defaultTotalItem;
        }

        return totalItem;
    }

    /**
     * Set total item value
     *
     * @param tItem The totalItem to set.
     */
    public void setTotalItem(Integer tItem) {
        if (tItem == null) {
            throw new IllegalArgumentException("TotalItem can't be null.");
        }

        this.totalItem = tItem;

        int current = this.getCurrentPage().intValue();
        int lastPage = this.getTotalPage();

        if (current > lastPage) {
            this.setCurrentPage(new Integer(lastPage));
        }
    }

    public int getTotalPage() {
        int pgSize = this.getPageSize().intValue();
        int total = this.getTotalItem().intValue();
        int result = total / pgSize;

        if ((total == 0) || ((total % pgSize) != 0)) {
            result++;
        }

        return result;
    }

    public int getPageFirstItem() {
        int cPage = this.getCurrentPage().intValue();

        if (cPage == 1) {
            return 1;
        }

        cPage--;

        int pgSize = this.getPageSize().intValue();

        return (pgSize * cPage) + 1;
    }

    public int getPageLastItem() {
        int cPage = this.getCurrentPage().intValue();
        int pgSize = this.getPageSize().intValue();
        int assumeLast = pgSize * cPage;
        int totalItem = getTotalItem().intValue();

        return Math.min(assumeLast, totalItem);
    }

    /**
     * Get end row value
     *
     * @return Returns the endRow.
     */
    public int getEndRow() {
        return endRow;
    }

    /**
     * Set end row value
     *
     * @param endRow The endRow to set.
     */
    public void setEndRow(int endRow) {
        this.endRow = endRow;
    }

    /**
     * Get start row value
     *
     * @return Returns the startRow.
     */
    public int getStartRow() {
        return startRow;
    }

    /**
     * Set start row value
     *
     * @param startRow The startRow to set.
     */
    public void setStartRow(int startRow) {
        this.startRow = startRow;
    }

    protected String getSQLBlurValue(String value) {
        if (value == null) {
            return null;
        }

        return value + '%';
    }

    protected String formatDate(String dateString) {
        if (TStringUtils.isBlank(dateString)) {
            return null;
        } else {
            return (dateString + " 00:00:00");
        }
    }

    /**
     * When the time is queried, the end time is 23:59:59
     *
     * @param dateString    the sting date value
     */
    protected String addDateEndPostfix(String dateString) {
        if (TStringUtils.isBlank(dateString)) {
            return null;
        }

        return dateString + " 23:59:59";
    }

    /**
     * When the time is queried, the start time is 00:00:00
     *
     * @param dateString    the string date value
     */
    protected String addDateStartPostfix(String dateString) {
        if (TStringUtils.isBlank(dateString)) {
            return null;
        }

        return dateString + " 00:00:00";
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public String[] getParameters() {
        return new String[0];
    }

    /**
     * Get ajax prefix value
     *
     * @return Returns the ajaxPrefix.
     */
    public String getAjaxPrefix() {
        return ajaxPrefix;
    }

    /**
     * Set ajax prefix
     *
     * @param ajaxPrefix The ajaxPrefix to set.
     */
    public BaseResult setAjaxPrefix(String ajaxPrefix) {
        this.ajaxPrefix = ajaxPrefix;
        return this;
    }

    /**
     * Get ajaxSuffix value
     *
     * @return Returns the ajaxSuffix.
     */
    public String getAjaxSuffix() {
        return ajaxSuffix;
    }

    /**
     * Set ajax suffix
     *
     * @param ajaxSuffix The ajaxSuffix to set.
     */
    public BaseResult setAjaxSuffix(String ajaxSuffix) {
        this.ajaxSuffix = ajaxSuffix;
        return this;
    }

    /**
     * Get charset
     *
     * @return Returns the charset.
     */
    public String getCharset() {
        return charset;
    }

    /**
     * Set charset
     *
     * @param charset The charset to set.
     */
    public BaseResult setCharset(String charset) {
        this.charset = charset;
        return this;
    }

    /**
     * Remove a parameter
     *
     * @param key    the Key that need to be removed
     */
    public BaseResult remove(Object key) {
        if (null == this.removeObject) {
            this.removeObject = new Object();
        }
        replace(key, this.removeObject);
        return this;
    }

    /**
     * Temporarily modify the value of a parameter
     *
     * @param key   the Key that need to be modified
     * @param val   the new value
     */
    public BaseResult replace(Object key, Object val) {
        if (null != key && null != val) {
            if (null == this.tempParam) {
                this.tempParam = new HashMap(5);
            }
            this.tempParam.put(String.valueOf(key), val);
        }
        return this;
    }

    /**
     * Get source
     *
     * @return Returns the from.
     */
    public String getFrom() {
        return from;
    }

    /**
     * set source
     *
     * @param from The from to set.
     */
    public void setFrom(String from) {
        this.from = from;
    }

    /**
     * Get whether to escape
     *
     * @return Returns the escape.
     */
    public boolean isEscape() {
        return escape;
    }

    /**
     * Set escape
     *
     * @param escape The escape to set.
     */
    public BaseResult setEscape(boolean escape) {
        this.escape = escape;
        return this;
    }

    /**
     * Get whether to escape js
     *
     * @return Returns the jsEscape.
     */
    public final boolean isJsEscape() {
        return this.jsEscape;
    }

    /**
     * Set escape js status
     *
     * @param jsEscape The jsEscape to set.
     */
    public final BaseResult setJsEscape(boolean jsEscape) {
        this.jsEscape = jsEscape;
        return this;
    }
}
