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

import { ConnectionValidationResult, Connector, ConnectorType, FilterValidationResult, PropertiesValidationResult } from "@debezium/ui-models";
import { BaseService } from "../baseService";

/**
 * The connector service.  Used to fetch connectors and other connector operations.
 */
export class ConnectorService extends BaseService {

    /**
     * Get the details of connector for supplied connection type
     * Example usage:
     * 
     * const connectorService = Services.getConnectorService();
     * connectorService.getConnectorInfo('postgres')
     *  .then((cDetails: ConnectorType) => {
     *      alert(cDetails);
     *  })
     * .catch((err: Error) => {
     *      alert(err);
     *  });
     */
    public getConnectorInfo(connectorTypeId: string): Promise<ConnectorType> {
        this.logger?.info("[ConnectorService] Getting the details of Connector:", connectorTypeId);
        const connectorType = connectorTypeId;
        const endpoint: string = this.endpoint("/connector-types/debezium-connector-:connectorTypeId/:connectorType.json", true, { connectorTypeId, connectorType });
        return this.httpGet<ConnectorType>(endpoint);
    }

    /**
     * Validate the connection properties for the supplied connection type
     * Example usage:
     * 
     * const connectorService = Services.getConnectorService();
     * const body = { "oneParm: oneValue", "twoParam: twoValue"}
     * connectorService.validateConnection('postgres', body)
     *  .then((result: ConnectionValidationResult) => {
     *    if (result.status === 'INVALID') {
     *      alert('status is INVALID');
     *    } else {
     *      alert('status is VALID');
     *    }
     *  });
     */
    public validateConnection(connectorTypeId: string, body: any): Promise<ConnectionValidationResult> {
        this.logger?.info("[ConnectorService] Validating connection:", connectorTypeId);

        const endpoint: string = this.endpoint("/connector-types/:connectorTypeId/validation/connection", undefined, { connectorTypeId });
        return this.httpPostWithReturn(endpoint, body);
    }

    /**
     * Validate the filters for the supplied connection type
     * Example usage:
     * 
     * const connectorService = Services.getConnectorService();
     * const body = { "oneParm: oneValue", "twoParam: twoValue"}
     * connectorService.validateFilters('postgres', body)
     *  .then((result: FilterValidationResult) => {
     *    if (result.status === 'INVALID') {
     *      alert('status is INVALID');
     *    } else {
     *      alert('status is VALID');
     *    }
     *  });
     */
    public validateFilters(connectorTypeId: string, body: any): Promise<FilterValidationResult> {
        this.logger?.info("[ConnectorService] Validating filters:", connectorTypeId);

        const endpoint: string = this.endpoint("/connector-types/:connectorTypeId/validation/filters",undefined, { connectorTypeId });
        return this.httpPostWithReturn(endpoint, body);
    }

    /**
     * Validate the properties for the supplied connection type
     * Example usage:
     * 
     * const connectorService = Services.getConnectorService();
     * const body = { "oneParm: oneValue", "twoParam: twoValue"}
     * connectorService.validateProperties('postgres', body)
     *  .then((result: PropertiesValidationResult) => {
     *    if (result.status === 'INVALID') {
     *      alert('status is INVALID');
     *    } else {
     *      alert('status is VALID');
     *    }
     *  });
     */
    public validateProperties(connectorTypeId: string, body: any): Promise<PropertiesValidationResult> {
        this.logger?.info("[ConnectorService] Validating properties:", connectorTypeId);

        const endpoint: string = this.endpoint("/connector-types/:connectorTypeId/validation/properties",undefined, { connectorTypeId });
        return this.httpPostWithReturn(endpoint, body);
    }

    /**
     * Create Connector using the supplied ConnectorConfiguration
     * Example usage:
     * 
     * const connectorService = Services.getConnectorService();
     * const configMap = new Map<string,string>();
     * configMap.set("oneParam","oneValue");
     * configMap.set("twoParam","twoValue");
     * const config = new ConnectorConfiguration("connName", configMap);
     * connectorService.createConnector(config)
     *  .then((result: CreateConnectorResult) => {
     *  });
     */
    public createConnector(clusterId: number, connectorTypeId: string, body: any): Promise<void> {
        this.logger?.info("[ConnectorService] Creating a connector:");

        const endpoint: string = this.endpoint("/connector/:clusterId/:connectorTypeId",undefined, { clusterId, connectorTypeId });
        return this.httpPostWithReturn(endpoint, body);
    }

    /**
     * Get the available connectors for the supplied clusterId
     */
    public getConnectors(clusterId: number): Promise<Connector[]> {
        this.logger?.info("[ConnectorService] Getting the list of connectors.");

        const endpoint: string = this.endpoint("/connectors/:clusterId", undefined,{ clusterId });
        return this.httpGet<Connector[]>(endpoint);
    }

    /**
     * Get the Connector config
     */
     public getConnectorConfig(clusterId: number, connectorName: string): Promise<any> {
        this.logger?.info("[ConnectorService] Fetch the connector");

        const endpoint: string = this.endpoint("/connectors/:clusterId/:connectorName/config", undefined,{ clusterId, connectorName });
        return this.httpGet<any>(endpoint);
    }

    /**
     * Delete the Connector for the supplied clusterId
     */
    public deleteConnector(clusterId: number, connectorName: string): Promise<Connector[]> {
        this.logger?.info("[ConnectorService] Delete the connector");

        const endpoint: string = this.endpoint("/connectors/:clusterId/:connectorName", undefined,{ clusterId, connectorName });
        return this.httpDelete<any>(endpoint);
    }

    /**
     * Pause the Connector for the supplied clusterId
     */
    public pauseConnector(clusterId: number, connectorName: string, body: any): Promise<void> {
        this.logger?.info("[ConnectorService] Pause the connector");

        const endpoint: string = this.endpoint("/connector/:clusterId/:connectorName/pause", undefined,{ clusterId, connectorName });
        return this.httpPut(endpoint, body);
    }
    
    /**
     * Resume the Connector for the supplied clusterId
     */
    public resumeConnector(clusterId: number, connectorName: string, body: any): Promise<void> {
        this.logger?.info("[ConnectorService] Resume the connector");

        const endpoint: string = this.endpoint("/connector/:clusterId/:connectorName/resume", undefined,{ clusterId, connectorName });
        return this.httpPut(endpoint, body);
    }
    
    /**
     * Restart the Connector for the supplied clusterId
     */
    public restartConnector(clusterId: number, connectorName: string, body: any): Promise<void> {
        this.logger?.info("[ConnectorService] Restart the connector");

        const endpoint: string = this.endpoint("/connector/:clusterId/:connectorName/restart", undefined,{ clusterId, connectorName });
        return this.httpPost(endpoint, body);
    }    

    /**
     * Restart the Connector Task for the supplied clusterId and connector
     */
    public restartConnectorTask(clusterId: number, connectorName: string, connectorTaskId: number, body: any): Promise<void> {
        this.logger?.info("[ConnectorService] Restart the connector task");

        const endpoint: string = this.endpoint("/connector/:clusterId/:connectorName/task/:connectorTaskId/restart", 
        undefined,{ clusterId, connectorName, connectorTaskId });
        return this.httpPost(endpoint, body);
    }    

    /**
     * Get the transform list and their properties for supplied clusterId
     */
       public getTransform(clusterId: number): Promise<any[]> {
        this.logger?.info("[ConnectorService] Getting the list of transform.");

        const endpoint: string = this.endpoint("/:clusterId/transforms.json", undefined,{ clusterId });
        return this.httpGet<Connector[]>(endpoint);
    }

}
