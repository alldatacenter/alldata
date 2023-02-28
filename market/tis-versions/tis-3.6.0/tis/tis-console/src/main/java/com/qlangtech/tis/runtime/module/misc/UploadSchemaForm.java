/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qlangtech.tis.runtime.module.misc;

import com.alibaba.fastjson.JSONArray;
import com.qlangtech.tis.solrdao.ISchema;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 面向前端使用對象模型
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年5月8日
 */
public class UploadSchemaForm implements ISchema {
  private final List<SchemaField> fields = new ArrayList<SchemaField>();

  @Override
  public List<SchemaField> getSchemaFields() {
    return this.fields;
  }

  public List<SchemaField> getFields() {
    return this.getSchemaFields();
  }

  public boolean containsField(String name) {
    for (SchemaField f : this.fields) {
      if (StringUtils.equals(f.getName(), name)) {
        return true;
      }
    }
    return false;
  }

  public int getFieldCount() {
    return this.fields.size();
  }

  private String memo;

  public String getMemo() {
    return memo;
  }

  public void setMemo(String memo) {
    this.memo = memo;
  }

  public String getUniqueKey() {
    for (SchemaField f : this.fields) {
      if (f.isUniqueKey()) {
        return f.getName();
      }
    }
    return StringUtils.EMPTY;
  }

  public String getShareKey() {
    for (SchemaField f : this.fields) {
      if (f.isSharedKey()) {
        return f.getName();
      }
    }
    return StringUtils.EMPTY;
  }

  @Override
  public String getSharedKey() {
    return this.getShareKey();
  }

  @Override
  public JSONArray serialTypes() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clearFields() {
    this.fields.clear();
  }


  private List<String> errlist = new ArrayList<String>();

  @Override
  public boolean isValid() {
    if (!this.errlist.isEmpty()) {
      return false;
    }
    return CollectionUtils.isEmpty(this.errlist = ISchema.validateSchema(this.fields));
  }

  @Override
  public List<String> getErrors() {
    return Collections.unmodifiableList(this.errlist);
  }


}
