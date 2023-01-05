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
package com.qlangtech.tis.solrdao;

import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.qlangtech.tis.lang.TisException;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年5月8日
 */
public interface ISchema {
    public static final Set<String> reserved_words = Sets.newHashSet("_val_", "fq", "docId", "score", "q", "boost");
    public static final StringBuffer reservedWordsBuffer = new StringBuffer(reserved_words.stream().map(w -> "'" + w + "'").collect(Collectors.joining(",")));

    //    static {
//        reserved_words.add("_val_");
//        reserved_words.add("fq");
//        reserved_words.add("docId");
//        reserved_words.add("score");
//        reserved_words.add("q");
//        reserved_words.add("boost");
//        for (String s : reserved_words) {
//            reservedWordsBuffer.append("'").append(s).append("' ");
//        }
//    }
    static String getFieldPropRequiredErr(String fieldName) {
        return "字段:‘" + fieldName + "’的属性'stored'或'indexed'或'docvalue'至少有一项为true";
    }

    public static <T extends ISchemaField> List<String> validateSchema(List<T> fields) {
        List<String> errlist = new ArrayList<>();
        List<String> pks = Lists.newArrayList();
        for (ISchemaField field : fields) {
            if (field.isUniqueKey()) {
                pks.add(field.getName());
            }
            if (!field.isIndexed() && !field.isStored() && !field.isDocValue()) {
                errlist.add(getFieldPropRequiredErr(field.getName()));
            }
            String fieldName = StringUtils.lowerCase(field.getName());
            if (reserved_words.contains(fieldName)) {
                errlist.add("字段名称:" + field.getName() + "不能命名成系统保留字符" + reservedWordsBuffer);
            }
        }
        if (pks.size() < 1) {
            errlist.add("请为当前schema定义主键（PK）");
        } else if (pks.size() > 1) {
            throw new TisException("can not define more than 1 pks:" + pks.stream().collect(Collectors.joining(",")));
        }
        return errlist;
    }

    <TT extends ISchemaField> List<TT> getSchemaFields();

    String getUniqueKey();

    String getSharedKey();

    JSONArray serialTypes();

    void clearFields();

    /**
     * 校验是否有错误
     *
     * @return
     */
    public boolean isValid();

    /**
     * 取得错误
     *
     * @return
     */
    public List<String> getErrors();
}
