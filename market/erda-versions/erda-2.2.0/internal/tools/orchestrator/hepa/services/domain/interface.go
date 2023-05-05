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

package domain

import (
	"context"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/endpoint"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/service"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

var Service GatewayDomainService

type GatewayDomainService interface {
	GetOrgDomainInfo(*dto.ManageDomainReq) (common.NewPageQuery, error)
	GetTenantDomains(projectId, env string) ([]string, error)
	GetRuntimeDomains(runtimeId string) (dto.RuntimeDomainsDto, error)
	UpdateRuntimeServiceDomain(orgId, runtimeId, serviceName string, reqDto *dto.ServiceDomainReqDto) (bool, string, error)
	CreateOrUpdateComponentIngress(apistructs.ComponentIngressUpdateRequest) (bool, error)
	Clone(context.Context) GatewayDomainService
	FindDomains(domain, projectId, workspace string, matchType orm.OptionType, domainType ...string) ([]orm.GatewayDomain, error)
	UpdateRuntimeServicePort(runtimeService *orm.GatewayRuntimeService, releaseInfo *diceyml.Object) error
	RefreshRuntimeDomain(runtimeService *orm.GatewayRuntimeService, session *service.SessionHelper) error
	GiveRuntimeDomainToPackage(runtimeService *orm.GatewayRuntimeService, session *service.SessionHelper) (bool, error)
	TouchRuntimeDomain(orgId string, runtimeService *orm.GatewayRuntimeService, material endpoint.EndpointMaterial, domains []dto.EndpointDomainDto, audits *[]apistructs.Audit, session *service.SessionHelper) (string, error)
	TouchPackageDomain(orgId, packageId, clusterName string, domains []string, session *service.SessionHelper) ([]string, error)
	GetPackageDomains(packageId string, session ...*service.SessionHelper) ([]string, error)
	IsPackageDomainsDiff(packageId, clusterName string, domains []string, session *service.SessionHelper) (bool, error)
}
