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

import datart.core.data.provider.sql.Alias;
import datart.core.data.provider.sql.ColumnOperator;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SelectColumn extends ColumnOperator implements Alias {

    private String alias;

    public static SelectColumn of(String alias, String... names) {
        SelectColumn selectColumn = new SelectColumn();
        selectColumn.setAlias(alias);
        selectColumn.setColumn(names);
        return selectColumn;
    }



}
