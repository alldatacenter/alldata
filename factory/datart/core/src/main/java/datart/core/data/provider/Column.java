/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.core.data.provider;

import datart.core.base.consts.ValueType;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class Column implements Serializable {

    private String[] name;

    private ValueType type;

    private String fmt;

    private List<ForeignKey> foreignKeys;

    public Column(String[] name, ValueType type) {
        this.name = name;
        this.type = type;
    }

    public Column() {
    }

    public static Column of(ValueType type, String... names) {
        return new Column(names, type);
    }

    public String columnName() {
        return name[name.length - 1];
    }

    public String tableName() {
        if (name.length == 1) {
            return null;
        } else {
            return name[name.length - 2];
        }
    }

    public String columnKey() {
        return String.join(".", name);
    }

    public void setName(String... name) {
        this.name = name;
    }
}
