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

package datart.server.base.params;

import datart.core.data.provider.ScriptType;
import datart.core.data.provider.ScriptVariable;
import datart.core.data.provider.SelectColumn;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class TestExecuteParam {

    private String sourceId;

    private String script;

    private List<SelectColumn> columns;

    @NotNull
    private ScriptType scriptType;

    private List<ScriptVariable> variables;

    private int size = 100;

}