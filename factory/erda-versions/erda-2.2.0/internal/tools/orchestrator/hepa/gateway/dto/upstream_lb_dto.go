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

import "strings"

type UpstreamLbDto struct {
	Az              string   `json:"az"`
	LbName          string   `json:"lbName"`
	OrgId           string   `json:"orgId"`
	ProjectId       string   `json:"projectId"`
	Env             string   `json:"env"`
	DeploymentId    int      `json:"deploymentId"`
	HealthcheckPath string   `json:"healthcheckPath"`
	Targets         []string `json:"targets"`
}

func (dto UpstreamLbDto) IsInvalid() bool {
	return dto.Az == "" || dto.LbName == "" || dto.OrgId == "" || dto.ProjectId == "" ||
		dto.Env == "" || dto.DeploymentId == 0 || !strings.HasPrefix(dto.HealthcheckPath, "/") || len(dto.Targets) == 0
}
