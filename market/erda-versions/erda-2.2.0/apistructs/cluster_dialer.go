// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package apistructs

import (
	"encoding/json"
	"fmt"
)

type ClusterManagerHeaderKey string

var (
	ClusterManagerHeaderKeyClusterKey    ClusterManagerHeaderKey = "X-Erda-Cluster-Key"
	ClusterManagerHeaderKeyClientType    ClusterManagerHeaderKey = "X-Erda-Client-Type"
	ClusterManagerHeaderKeyClusterInfo   ClusterManagerHeaderKey = "X-Erda-Cluster-Info"
	ClusterManagerHeaderKeyAuthorization ClusterManagerHeaderKey = "Authorization"
	ClusterManagerHeaderKeyClientDetail  ClusterManagerHeaderKey = "X-Erda-Client-Detail"
)

func (c ClusterManagerHeaderKey) String() string {
	return string(c)
}

type ClusterManagerClientType string

var (
	ClusterManagerClientTypeDefault  ClusterManagerClientType = ""         // cluster
	ClusterManagerClientTypeCluster  ClusterManagerClientType = "cluster"  // cluster
	ClusterManagerClientTypePipeline ClusterManagerClientType = "pipeline" // pipeline
)

func (c ClusterManagerClientType) String() string {
	return string(c)
}

type ClusterManagerClientEventType string

var (
	ClusterManagerClientEventRegister ClusterManagerClientEventType = "register"
)

func (c ClusterManagerClientType) GenEventName(eventType ClusterManagerClientEventType) string {
	return fmt.Sprintf("client-%s-event-%s", c, eventType)
}

func (c ClusterManagerClientType) MakeClientKey(clusterKey string) string {
	if c == "" {
		return clusterKey
	}
	return fmt.Sprintf("%s-client-type-%s", clusterKey, c)
}

type ClusterManagerClientDetailKey string

var (
	ClusterManagerDataKeyClusterKey ClusterManagerClientDetailKey = "clusterKey"

	ClusterManagerDataKeyPipelineHost ClusterManagerClientDetailKey = "pipelineHost"
	ClusterManagerDataKeyPipelineAddr ClusterManagerClientDetailKey = "pipelineAddr"
)

type ClusterManagerClientDetail map[ClusterManagerClientDetailKey]string

type ClusterManagerClientMap map[string]ClusterManagerClientDetail

func (detail ClusterManagerClientDetail) Get(key ClusterManagerClientDetailKey) string {
	return detail[key]
}

func (detail ClusterManagerClientDetail) Marshal() ([]byte, error) {
	return json.Marshal(detail)
}

func (m ClusterManagerClientMap) GetClientDetail(clientKey string) ClusterManagerClientDetail {
	return m[clientKey]
}
