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

package api_policy

import (
	"context"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/apipolicy"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/service"
)

var Service GatewayApiPolicyService

type GatewayApiPolicyService interface {
	Clone(context.Context) GatewayApiPolicyService
	SetPackageDefaultPolicyConfig(category, packageId string, az *orm.GatewayAzInfo, config []byte, helper ...*service.SessionHelper) (string, error)
	GetPolicyConfig(category, packageId, packageApiId string) (interface{}, error)
	SetPolicyConfig(category, packageId, packageApiId string, config []byte) (interface{}, error)
	RefreshZoneIngress(zone orm.GatewayZone, az orm.GatewayAzInfo) error
	SetZonePolicyConfig(zone *orm.GatewayZone, category string, config []byte, helper *service.SessionHelper, needDeployTag ...bool) (apipolicy.PolicyDto, string, error)
	SetZoneDefaultPolicyConfig(packageId string, zone *orm.GatewayZone, az *orm.GatewayAzInfo, session ...*service.SessionHelper) (map[string]*string, *string, *service.SessionHelper, error)
}
