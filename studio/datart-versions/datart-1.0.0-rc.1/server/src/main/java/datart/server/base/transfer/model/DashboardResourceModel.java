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

import datart.core.entity.Dashboard;
import datart.core.entity.Folder;
import datart.server.base.dto.WidgetDetail;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class DashboardResourceModel extends TransferModel {

    private List<MainModel> mainModels;

    private Set<String> datacharts;

    private Set<String> views;

    private Set<String> parents;

    @Override
    public String getVizName() {
        return mainModels.get(0).getDashboard().getName();
    }

    @Data
    public static class MainModel implements Serializable {

        private Dashboard dashboard;

        private Folder folder;

        private List<WidgetDetail> widgets;

        private Map<String, byte[]> files;

    }

}
