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

package com.dorisdb.connector.datax.plugin.writer.doriswriter.row;

import com.alibaba.datax.common.element.Column;
import com.alibaba.datax.common.element.Column.Type;

public class DorisBaseSerializer {

    protected String fieldConvertion(Column col) {
        if (null == col.getRawData()) {
            return null;
        }
        if (Type.BOOL == col.getType()) {
            return String.valueOf(col.asLong());
        }
        return col.asString();
    }

}
