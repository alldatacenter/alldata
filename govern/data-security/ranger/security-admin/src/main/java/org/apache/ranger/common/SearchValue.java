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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *
 */
public class SearchValue {
    static final Logger logger = LoggerFactory.getLogger(SearchValue.class);

    SearchField searchField;
    Object value = null;
    List<?> valueList = null;
    boolean isNull = false;

   /**
     * @return the value
     */
    public Object getValue() {
	if (value != null) {
	    return value;
	}
	if (valueList.size() == 1) {
	    return valueList.get(0);
	}
	logger.error("getValue() called for null.", new Throwable());
	return value;
    }



    /**
     * @return the valueList
     */
    public List<?> getValueList() {
	return valueList;
    }

    /**
     * @return the searchField
     */
    public SearchField getSearchField() {
	return searchField;
    }




    public boolean isList() {
	return valueList != null && valueList.size() > 1;
    }

}
