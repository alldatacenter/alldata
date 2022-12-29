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

package datart.server.base.transfer.model;

import datart.core.entity.Variable;
import datart.core.entity.View;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

@Data
public class ViewResourceModel extends TransferModel {

    private List<MainModel> mainModels;

    private Set<String> sources;

    private List<View> parents;

    @Override
    public String getVizName() {
        return mainModels.get(0).view.getName();
    }

    @Data
    public static class MainModel implements Serializable {

        private View view;

        private List<Variable> variables;

    }
}
