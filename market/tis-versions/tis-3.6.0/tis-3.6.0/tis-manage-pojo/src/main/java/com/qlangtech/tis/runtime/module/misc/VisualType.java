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

package com.qlangtech.tis.runtime.module.misc;

import com.qlangtech.tis.plugin.ds.ReflectSchemaFieldType;

/**
 * @author: baisui 百岁
 * @create: 2020-10-03 17:45
 **/
public class VisualType {

  public static final VisualType STRING_TYPE
    = new VisualType(ReflectSchemaFieldType.STRING.literia, true);

  public final String type;

  // private final boolean ranageQueryAware;

  // 是否可分词
  private final boolean split;


  /**
   * @param type
   * @param split
   */
  public VisualType(String type, boolean split) {
    super();
    this.type = type;
    //this.ranageQueryAware = ranageQueryAware;
    this.split = split;
  }

  public ISearchEngineTokenizerType[] getTokenerTypes() {
    if (this.split) {
      return TokenizerType.values();
    } else {
      return new ISearchEngineTokenizerType[0];
    }
  }

  /**
   * 是否是可分词，庖丁等等
   *
   * @return
   */
  public boolean isSplit() {
    return split;
  }


  public String getType() {
    return type;
  }

  @Override
  public int hashCode() {
    return type.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (type.equals(((VisualType) obj).type)) {
      return true;
    }
    return false;
  }

}
