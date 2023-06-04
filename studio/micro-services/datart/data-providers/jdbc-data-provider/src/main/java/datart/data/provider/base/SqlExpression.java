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

package datart.data.provider.base;

import lombok.Data;

import java.util.StringJoiner;

@Data
public class SqlExpression {

    private String arg;

    private String val;

    private SqlOperator operator;

    public SqlExpression(String arg, String val, SqlOperator operator) {
        this.arg = arg;
        this.val = val;
        this.operator = operator;
    }

    public String compile() {
        return new StringJoiner(" ", "( ", " )")
                .add(arg)
                .add(operator.getOperator())
                .add(val).toString();
    }
}
