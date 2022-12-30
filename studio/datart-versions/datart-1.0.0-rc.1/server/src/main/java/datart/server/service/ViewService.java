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

package datart.server.service;

import datart.core.entity.Folder;
import datart.core.entity.View;
import datart.core.mappers.ext.ViewMapperExt;
import datart.server.base.dto.ViewDetailDTO;
import datart.server.base.params.BaseUpdateParam;
import datart.server.base.transfer.model.TransferModel;
import datart.server.base.transfer.model.ViewResourceModel;
import datart.server.base.params.ViewBaseUpdateParam;

import java.util.List;

public interface ViewService extends VizCRUDService<View, ViewMapperExt>, ResourceTransferService<View, ViewResourceModel, TransferModel, Folder> {

    ViewDetailDTO getViewDetail(String viewId);

    List<View> getViews(String orgId);

    View updateView(BaseUpdateParam updateParam);

    boolean unarchive(String id, String newName, String parentId, double index);

    boolean updateBase(ViewBaseUpdateParam updateParam);

    boolean checkUnique(String orgId, String parentId, String name);

}
