/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

 /**
 *
 */
package org.apache.ranger.common;

/**
 *
 *
 */
public class SortField {
    public enum SORT_ORDER {
	ASC, DESC
    };

    String paramName;
    String fieldName;
    boolean isDefault = false;
    SORT_ORDER defaultOrder = SORT_ORDER.ASC;

    /**
     * @param string
     * @param string2
     */
    public SortField(String paramName, String fieldName) {
	this.paramName = paramName;
	this.fieldName = fieldName;
	isDefault = false;
    }

    /**
     * @param paramName
     * @param fieldName
     * @param isDefault
     */
    public SortField(String paramName, String fieldName, boolean isDefault,
	    SORT_ORDER defaultOrder) {
	this.paramName = paramName;
	this.fieldName = fieldName;
	this.isDefault = isDefault;
	this.defaultOrder = defaultOrder;
    }

    /**
     * @return the paramName
     */
    public String getParamName() {
	return paramName;
    }



    /**
     * @return the fieldName
     */
    public String getFieldName() {
	return fieldName;
    }



    /**
     * @return the isDefault
     */
    public boolean isDefault() {
	return isDefault;
    }



    /**
     * @return the defaultOrder
     */
    public SORT_ORDER getDefaultOrder() {
        return defaultOrder;
    }




}
