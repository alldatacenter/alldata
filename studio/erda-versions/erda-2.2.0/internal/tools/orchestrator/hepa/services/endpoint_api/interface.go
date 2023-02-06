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

package endpoint_api

import (
	"context"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/service"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/runtime_service"
)

var Service GatewayOpenapiService

type PackageApiInfo struct {
	*orm.GatewayPackageApi
	Hosts               []string
	ProjectId           string
	Env                 string
	Az                  string
	InjectRuntimeDomain bool
}

type GatewayOpenapiService interface {
	Clone(context.Context) GatewayOpenapiService
	ClearRuntimeRoute(id string) error
	SetRuntimeEndpoint(runtime_service.RuntimeEndpointInfo) error
	TouchPackageRootApi(packageId string, reqDto *dto.OpenapiDto) (bool, error)
	TryClearRuntimePackage(*orm.GatewayRuntimeService, *service.SessionHelper, ...bool) error
	TouchRuntimePackageMeta(*orm.GatewayRuntimeService, *service.SessionHelper) (string, bool, error)
	RefreshRuntimePackage(string, *orm.GatewayRuntimeService, *service.SessionHelper) error
	CreateUnityPackageZone(string, *service.SessionHelper) (*orm.GatewayZone, error)
	CreateTenantPackage(string, *service.SessionHelper) error
	CreatePackage(*dto.DiceArgsDto, *dto.PackageDto) (*dto.PackageInfoDto, string, error)
	GetPackages(*dto.GetPackagesDto) (common.NewPageQuery, error)
	ListAllPackages() ([]orm.GatewayPackage, error)
	GetPackage(string) (*dto.PackageInfoDto, error)
	GetPackagesName(*dto.GetPackagesDto) ([]dto.PackageInfoDto, error)
	UpdatePackage(string, string, *dto.PackageDto) (*dto.PackageInfoDto, error)
	DeletePackage(string) (bool, error)
	CreatePackageApi(string, *dto.OpenapiDto) (string, bool, error)
	GetPackageApis(string, *dto.GetOpenapiDto) (common.NewPageQuery, error)
	ListPackageAllApis(id string) ([]orm.GatewayPackageApi, error)
	UpdatePackageApi(string, string, *dto.OpenapiDto) (*dto.OpenapiInfoDto, bool, error)
	DeletePackageApi(string, string) (bool, error)
	TouchPackageApiZone(info PackageApiInfo, session ...*service.SessionHelper) (string, error)
}
