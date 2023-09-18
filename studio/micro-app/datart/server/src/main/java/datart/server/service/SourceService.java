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

import datart.core.data.provider.SchemaInfo;
import datart.core.entity.Folder;
import datart.core.entity.Source;
import datart.core.mappers.ext.SourceMapperExt;
import datart.server.base.params.SourceBaseUpdateParam;
import datart.server.base.params.SourceCreateParam;
import datart.server.base.params.SourceUpdateParam;
import datart.server.base.transfer.model.SourceResourceModel;
import datart.server.base.transfer.model.TransferModel;

import java.util.List;

public interface SourceService extends BaseCRUDService<Source, SourceMapperExt>, ResourceTransferService<Source, SourceResourceModel, TransferModel, Folder> {

    boolean checkUnique(String orgId, String parentId, String name);

    List<Source> listSources(String orgId, boolean active);

    SchemaInfo getSourceSchemaInfo(String sourceId);

    SchemaInfo syncSourceSchema(String sourceId) throws Exception;

    Source createSource(SourceCreateParam createParam);

    boolean updateSource(SourceUpdateParam updateParam);

    boolean updateBase(SourceBaseUpdateParam updateParam);

    boolean unarchive(String id, String newName, String parentId, double index);

}
