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

package global

import (
	"context"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/service"
)

var Service GatewayGlobalService

type GatewayGlobalService interface {
	Clone(context.Context) GatewayGlobalService
	GetDiceHealth() dto.DiceHealthDto
	GenTenantGroup(projectId, env, clusterName string) (string, error)
	GetGatewayFeatures(clusterName string) map[string]string
	GetTenantGroup(projectId, env string) (string, error)
	GetOrgId(string) (string, error)
	GetClusterUIType(string, string, string) *common.StandardResult
	GenerateEndpoint(dto.DiceInfo, ...*service.SessionHelper) (string, string, error)
	GenerateDefaultPath(string, ...*service.SessionHelper) (string, error)
	GetProjectName(dto.DiceInfo, ...*service.SessionHelper) (string, error)
	GetProjectNameFromCmdb(string) (string, error)
	GetClustersByOrg(string) ([]string, error)
	GetServiceAddr(string) string
	GetRuntimeServicePrefix(*orm.GatewayRuntimeService) (string, error)
	CreateTenant(*dto.TenantDto) (bool, error)
}
