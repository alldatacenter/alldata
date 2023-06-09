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

package com.qlangtech.tis.plugin.ds;

import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-02-26 17:25
 **/
public interface IDBReservedKeys {
    default Optional<String> getEscapeChar() {
        return Optional.empty();
    }

    default String getEscapedEntity(String name) {
        Optional<String> escapeChar = this.getEscapeChar();
        if (escapeChar.isPresent()) {
            String ec = escapeChar.get();
            return ec + name + ec;
        } else {
            return name;
        }
    }

    default String removeEscapeChar(String entityName) {
        if (entityName == null) {
            throw new IllegalArgumentException("param entityName can not be null");
        }
        Optional<String> escape = getEscapeChar();
        if (escape.isPresent()) {
            return entityName.replace(escape.get(), "");
            // return StringUtils.remove(entityName, escape.get());
        } else {
            return entityName;
        }

    }
}
