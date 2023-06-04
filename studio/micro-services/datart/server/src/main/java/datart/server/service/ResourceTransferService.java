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

import datart.server.base.transfer.ImportStrategy;
import datart.server.base.transfer.TransferConfig;
import datart.server.base.transfer.model.TransferModel;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ResourceTransferService<T, R extends TransferModel, RT extends TransferModel, F> {

    List<T> getAllParents(String id);

    R exportResource(TransferConfig transferConfig, Set<String> ids);

    boolean importResource(R model, ImportStrategy strategy, String orgId);

    void replaceId(R model
            , Map<String, String> sourceIdMapping
            , Map<String, String> viewIdMapping
            , Map<String, String> chartIdMapping
            , Map<String, String> boardIdMapping
            , Map<String, String> folderIdMapping);

    default F importTemplate(RT model, String orgId, String name, F folder) {
        return null;
    }

}
