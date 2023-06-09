package com.qlangtech.tis.plugin.ds;

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

import com.qlangtech.tis.plugin.IdentityName;
import org.apache.commons.lang.StringUtils;

import java.util.Objects;
import java.util.Optional;

/**
 * 数据库实例
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-01-31 11:19
 **/
public interface DBIdentity extends IdentityName {

    public static DBIdentity parseId(final String idVal) {
        Optional<DBIdentity> id = parse(idVal);
        if (!id.isPresent()) {
            throw new IllegalStateException("id must be present:" + idVal);
        }
        return id.get();
    }

    public static Optional<DBIdentity> parse(final String idVal) {
        if (StringUtils.isEmpty(idVal)) {
            return Optional.empty();
        }
        return Optional.of(new DBIdentity() {
            @Override
            public String identityValue() {
                return idVal;
            }
        });
    }

    default boolean isEquals(DBIdentity queryDBSourceId) {
        return StringUtils.equals(Objects.requireNonNull(queryDBSourceId.identityValue(), "dbFactoryId can not be null")
                , this.identityValue());
    }

}
