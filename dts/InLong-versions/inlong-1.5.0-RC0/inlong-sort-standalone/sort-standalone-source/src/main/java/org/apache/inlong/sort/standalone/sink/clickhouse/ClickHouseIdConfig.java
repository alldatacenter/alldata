/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.standalone.sink.clickhouse;

import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 
 * ClickHouseIdConfig
 */
public class ClickHouseIdConfig {

    public static final String FIELD_NAME_SEPARATOR = ",";

    private String inlongGroupId;
    private String inlongStreamId;
    private String separator = "|";
    private String contentFieldNames;
    private int contentOffset = 0;// except for boss + tab(1)
    private String tableName;
    private String dbFieldNames;
    // parse
    private List<String> contentFieldList;
    private List<Pair<String, Integer>> dbFieldList;
    private String insertSql;

    /**
     * parseFieldList
     */
    public static List<String> parseFieldNames(String fieldNames) {
        List<String> fieldList = new ArrayList<>();
        if (fieldNames != null) {
            String[] fieldNameArray = fieldNames.split(FIELD_NAME_SEPARATOR);
            fieldList.addAll(Arrays.asList(fieldNameArray));
        }
        return fieldList;
    }

    /**
     * get inlongGroupId
     * @return the inlongGroupId
     */
    public String getInlongGroupId() {
        return inlongGroupId;
    }

    /**
     * set inlongGroupId
     * @param inlongGroupId the inlongGroupId to set
     */
    public void setInlongGroupId(String inlongGroupId) {
        this.inlongGroupId = inlongGroupId;
    }

    /**
     * get inlongStreamId
     * @return the inlongStreamId
     */
    public String getInlongStreamId() {
        return inlongStreamId;
    }

    /**
     * set inlongStreamId
     * @param inlongStreamId the inlongStreamId to set
     */
    public void setInlongStreamId(String inlongStreamId) {
        this.inlongStreamId = inlongStreamId;
    }

    /**
     * get separator
     * @return the separator
     */
    public String getSeparator() {
        return separator;
    }

    /**
     * set separator
     * @param separator the separator to set
     */
    public void setSeparator(String separator) {
        this.separator = separator;
    }

    /**
     * get contentFieldNames
     * @return the contentFieldNames
     */
    public String getContentFieldNames() {
        return contentFieldNames;
    }

    /**
     * set contentFieldNames
     * @param contentFieldNames the contentFieldNames to set
     */
    public void setContentFieldNames(String contentFieldNames) {
        this.contentFieldNames = contentFieldNames;
    }

    /**
     * get contentOffset
     * @return the contentOffset
     */
    public int getContentOffset() {
        return contentOffset;
    }

    /**
     * set contentOffset
     * @param contentOffset the contentOffset to set
     */
    public void setContentOffset(int contentOffset) {
        this.contentOffset = contentOffset;
    }

    /**
     * get tableName
     * @return the tableName
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * set tableName
     * @param tableName the tableName to set
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * get dbFieldNames
     * @return the dbFieldNames
     */
    public String getDbFieldNames() {
        return dbFieldNames;
    }

    /**
     * set dbFieldNames
     * @param dbFieldNames the dbFieldNames to set
     */
    public void setDbFieldNames(String dbFieldNames) {
        this.dbFieldNames = dbFieldNames;
    }

    /**
     * get contentFieldList
     * @return the contentFieldList
     */
    public List<String> getContentFieldList() {
        return contentFieldList;
    }

    /**
     * set contentFieldList
     * @param contentFieldList the contentFieldList to set
     */
    public void setContentFieldList(List<String> contentFieldList) {
        this.contentFieldList = contentFieldList;
    }

    /**
     * get dbFieldList
     * @return the dbFieldList
     */
    public List<Pair<String, Integer>> getDbFieldList() {
        return dbFieldList;
    }

    /**
     * set dbFieldList
     * @param dbFieldList the dbFieldList to set
     */
    public void setDbFieldList(List<Pair<String, Integer>> dbFieldList) {
        this.dbFieldList = dbFieldList;
    }

    /**
     * get insertSql
     * @return the insertSql
     */
    public String getInsertSql() {
        return insertSql;
    }

    /**
     * set insertSql
     * @param insertSql the insertSql to set
     */
    public void setInsertSql(String insertSql) {
        this.insertSql = insertSql;
    }

}
