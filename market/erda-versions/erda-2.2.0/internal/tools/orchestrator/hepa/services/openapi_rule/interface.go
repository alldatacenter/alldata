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

package openapi_rule

import (
	"context"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/exdto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/service"
)

var Service GatewayOpenapiRuleService

type GatewayOpenapiRuleService interface {
	Clone(context.Context) GatewayOpenapiRuleService
	CreateOrUpdateLimitRule(consumerId, packageId string, limits []exdto.LimitType) error
	CreateLimitRule(*dto.DiceArgsDto, *dto.OpenLimitRuleDto) (bool, bool, error)
	UpdateLimitRule(string, *dto.OpenLimitRuleDto) (*dto.OpenLimitRuleInfoDto, error)
	GetLimitRules(*dto.GetOpenLimitRulesDto) (common.NewPageQuery, error)
	DeleteLimitRule(string) (bool, error)
	CreateRule(dto.DiceInfo, *dto.OpenapiRule, *service.SessionHelper) error
	UpdateRule(string, *dto.OpenapiRule) (*orm.GatewayPackageRule, error)
	// use session if helper not nil
	GetPackageRules(string, *service.SessionHelper, ...dto.RuleCategory) ([]dto.OpenapiRuleInfo, error)
	GetApiRules(string, ...dto.RuleCategory) ([]dto.OpenapiRuleInfo, error)
	DeleteRule(string, *service.SessionHelper) error
	// recycle plugins
	DeleteByPackage(*orm.GatewayPackage) error
	DeleteByPackageApi(*orm.GatewayPackage, *orm.GatewayPackageApi) error
	SetPackageKongPolicies(*orm.GatewayPackage, *service.SessionHelper) error
	SetPackageApiKongPolicies(packageApi *orm.GatewayPackageApi, session *service.SessionHelper) error
}
