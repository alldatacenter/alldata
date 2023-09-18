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
package com.qlangtech.tis.manage.common.ibatis;

import java.sql.SQLException;
import com.ibatis.sqlmap.client.extensions.ParameterSetter;
import com.ibatis.sqlmap.client.extensions.ResultGetter;
import com.ibatis.sqlmap.client.extensions.TypeHandlerCallback;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2012-8-2
 */
public class BooleanYorNConvertCallback implements TypeHandlerCallback {

    public static final String YES = "Y";

    @Override
    public Object getResult(ResultGetter getter) throws SQLException {
        if ("N".equalsIgnoreCase(getter.getString())) {
            return Boolean.FALSE;
        } else if (YES.equalsIgnoreCase(getter.getString())) {
            return Boolean.TRUE;
        }
        return Boolean.TRUE;
    }

    @Override
    public void setParameter(ParameterSetter setter, Object parameter) throws SQLException {
        setter.setString(((Boolean) parameter) ? YES : "N");
    }

    @Override
    public Object valueOf(String s) {
        return "N".equalsIgnoreCase(s);
    }
}
