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

package dto

import (
	"encoding/json"

	"github.com/pkg/errors"
)

type RegisterDto struct {
	OrgId       string          `json:"orgId"`
	ProjectId   string          `json:"projectId"`
	Workspace   string          `json:"workspace"`
	ClusterName string          `json:"clusterName"`
	AppId       string          `json:"appId"`
	AppName     string          `json:"appName"`
	RuntimeId   string          `json:"runtimeId"`
	RuntimeName string          `json:"runtimeName"`
	ServiceName string          `json:"serviceName"`
	ServiceAddr string          `json:"serviceAddr"`
	Swagger     json.RawMessage `json:"swagger"`
}

func (dto RegisterDto) CheckValid() error {
	if dto.OrgId == "" || dto.ProjectId == "" || dto.Workspace == "" || dto.ClusterName == "" || dto.AppId == "" || dto.ServiceName == "" || dto.Swagger == nil {
		return errors.Errorf("invalid RegisterDto: %+v", dto)
	}
	return nil
}

type RegisterRespDto struct {
	ApiRegisterId string `json:"apiRegisterId"`
}

type RegisterStatusDto struct {
	Completed bool `json:"completed"`
}
