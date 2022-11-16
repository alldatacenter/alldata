/**
 * @license
 * Copyright 2020 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {ConnectorType} from "@debezium/ui-models";
import {BaseService} from "../baseService";


/**
 * The globals service.  Used to get global/settings information from the back-end.
 */
export class GlobalsService extends BaseService {

    public getConnectorTypes(): Promise<ConnectorType[]> {
        this.logger?.info("[GlobalsService] Getting the list of connector types.");
        const endpoint: string = this.endpoint("/connector-types");
        return this.httpGet<ConnectorType[]>(endpoint);
    }

    public getConnectCluster(): Promise<string[]> {
        this.logger?.info("[GlobalsService] Getting the list of connector cluster.");
        const endpoint: string = this.endpoint("/connect-clusters");
        return this.httpGet<string[]>(endpoint);
    }

    /**
     * Get the enabled state for topic creation for the supplied clusterId
     */
    public getTopicCreationEnabled(clusterId: number): Promise<boolean> {
        this.logger?.info("[GlobalsService] Getting the enabled state for topic creation.");

        const endpoint: string = this.endpoint("/:clusterId/topic-creation-enabled", undefined,{ clusterId });
        return this.httpGet<boolean>(endpoint);
    }

}
