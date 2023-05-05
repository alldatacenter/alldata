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

package service

import (
	"github.com/xormplus/xorm"

	. "github.com/erda-project/erda/internal/tools/orchestrator/hepa/common"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
	. "github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
)

const (
	ZONE_TYPE_DICE_APP    = "diceApp" //已废弃
	ZONE_TYPE_PACKAGE     = "package" // 老的入口
	ZONE_TYPE_PACKAGE_NEW = "packageNew"
	ZONE_TYPE_PACKAGE_API = "packageApi"
	ZONE_TYPE_UNITY       = "unity"
)

var GLOBAL_REGIONS = []string{"option", "main", "http", "server"}
var ZONE_REGIONS = []string{"annotation", "location"}

type IngressChanges struct {
	// use to merge configMap
	ConfigmapOptions []map[string]*string
	MainSnippets     *[]string
	HttpSnippets     *[]string
	ServerSnippets   *[]string

	// use to merge ingress
	Annotations      []map[string]*string
	LocationSnippets *[]string
}

type KongBelongTuple struct {
	Az        string
	ProjectId string
	Env       string
}

type GatewayPackageService interface {
	NewSession(...*SessionHelper) (GatewayPackageService, error)
	Insert(*GatewayPackage) error
	GetPage(options []SelectOption, page *Page) (*PageQuery, error)
	Count(options []SelectOption) (int64, error)
	Update(*GatewayPackage, ...string) error
	Delete(string, ...bool) error
	Get(string) (*GatewayPackage, error)
	GetByAny(cond *GatewayPackage) (*GatewayPackage, error)
	SelectByAny(*GatewayPackage) ([]GatewayPackage, error)
	//check id when update
	CheckUnique(*GatewayPackage) (bool, error)
}

type GatewayPackageApiService interface {
	NewSession(...*SessionHelper) (GatewayPackageApiService, error)
	Insert(*GatewayPackageApi) error
	GetPage(options []SelectOption, page *Page) (*PageQuery, error)
	Count(options []SelectOption) (int64, error)
	Update(*GatewayPackageApi, ...string) error
	Delete(string) error
	DeleteByPackageDiceApi(string, string) error
	Get(string) (*GatewayPackageApi, error)
	SelectByAny(*GatewayPackageApi) ([]GatewayPackageApi, error)
	GetByAny(*GatewayPackageApi) (*GatewayPackageApi, error)
	GetRawByAny(*GatewayPackageApi) (*GatewayPackageApi, error)
	DeleteByPackageId(string) error
	CheckUnique(*GatewayPackageApi) (bool, error)
	SelectByOptions(options []orm.SelectOption) ([]orm.GatewayPackageApi, error)
}

type GatewayPackageRuleService interface {
	NewSession(...*SessionHelper) (GatewayPackageRuleService, error)
	Insert(*GatewayPackageRule) error
	GetPage(options []SelectOption, page *Page) (*PageQuery, error)
	Count(options []SelectOption) (int64, error)
	Update(*GatewayPackageRule) error
	Delete(string) error
	Get(string) (*GatewayPackageRule, error)
	GetByAny(cond *GatewayPackageRule) (*GatewayPackageRule, error)
	SelectByAny(*GatewayPackageRule) ([]GatewayPackageRule, error)
	DeleteByPackageId(string) error
}

type GatewayZoneInPackageService interface {
	NewSession(...*SessionHelper) (GatewayZoneInPackageService, error)
	Insert(*GatewayZoneInPackage) error
	Update(*GatewayZoneInPackage) error
	GetByAny(*GatewayZoneInPackage) (*GatewayZoneInPackage, error)
	DeleteByPackageId(string) error
	// use for create/update zone's domain binded ingress
	SelectByZoneId(string) ([]GatewayZoneInPackage, error)
	SelectByPackageId(string) ([]GatewayZoneInPackage, error)
}

type GatewayApiInPackageService interface {
	NewSession(...*SessionHelper) (GatewayApiInPackageService, error)
	Insert(*GatewayApiInPackage) error
	Delete(packageId, apiId string) error
	DeleteByPackageId(string) error
	// use for delete or update api check
	SelectByApi(string) ([]GatewayApiInPackage, error)
}

type GatewayPackageInConsumerService interface {
	NewSession(...*SessionHelper) (GatewayPackageInConsumerService, error)
	Insert(*GatewayPackageInConsumer) error
	Delete(packageId, consumerId string) error
	DeleteByConsumerId(string) error
	DeleteByPackageId(string) error
	// use for show
	SelectByConsumer(string) ([]GatewayPackageInConsumer, error)
	// also used for delete package check
	SelectByPackage(string) ([]GatewayPackageInConsumer, error)
}

type GatewayPackageApiInConsumerService interface {
	NewSession(...*SessionHelper) (GatewayPackageApiInConsumerService, error)
	Insert(*GatewayPackageApiInConsumer) error
	Delete(packageId, packageApiId, consumerId string) error
	DeleteByConsumerId(string) error
	// use for show
	SelectByConsumer(string) ([]GatewayPackageApiInConsumer, error)
	SelectByPackageApi(string, string) ([]GatewayPackageApiInConsumer, error)
}

type GatewayIngressPolicyService interface {
	NewSession(...*SessionHelper) (GatewayIngressPolicyService, error)
	Insert(*GatewayIngressPolicy) error
	GetByAny(*GatewayIngressPolicy) (*GatewayIngressPolicy, error)
	SelectByAny(*GatewayIngressPolicy) ([]GatewayIngressPolicy, error)
	Update(*GatewayIngressPolicy) error
	UpdatePartial(*GatewayIngressPolicy, ...string) error
	CreateOrUpdate(*GatewayIngressPolicy) error
	GetChangesByRegions(az, regions string, zoneId ...string) (*IngressChanges, error)
	GetChangesByRegionsImpl(az, regions string, zoneId ...string) (*IngressChanges, error)
}

type GatewayDefaultPolicyService interface {
	NewSession(...*SessionHelper) (GatewayDefaultPolicyService, error)
	CreateOrUpdate(*GatewayDefaultPolicy) error
	Insert(*GatewayDefaultPolicy) error
	GetByAny(*GatewayDefaultPolicy) (*GatewayDefaultPolicy, error)
	SelectByAny(*GatewayDefaultPolicy) ([]GatewayDefaultPolicy, error)
	Update(*GatewayDefaultPolicy) error
}

type GatewayZoneService interface {
	NewSession(...*SessionHelper) (GatewayZoneService, error)
	Insert(*GatewayZone) error
	GetByAny(*GatewayZone) (*GatewayZone, error)
	GetById(string) (*GatewayZone, error)
	Update(*GatewayZone) error
	DeleteById(string) error
	SelectByAny(*GatewayZone) ([]GatewayZone, error)
	SelectPolicyZones(clusterName string) ([]GatewayZone, error)
}

type GatewayApiService interface {
	NewSession(...*SessionHelper) (GatewayApiService, error)
	Insert(*GatewayApi) error
	GetPage(options []SelectOption, page *Page) (*PageQuery, error)
	Count(options []SelectOption) (int64, error)
	GetPageByConsumerId(consumerId string, page *Page) (*PageQuery, error)
	CountByConsumerId(consumerId string) (int64, error)
	GetById(string) (*GatewayApi, error)
	GetByAny(*GatewayApi) (*GatewayApi, error)
	GetRawByAny(*GatewayApi) (*GatewayApi, error)
	SelectByGroupId(string) ([]GatewayApi, error)
	RealDeleteById(string) error
	DeleteById(string) error
	Update(*GatewayApi) error
	SelectByOptions(options []SelectOption) ([]GatewayApi, error)
	SelectByAny(*GatewayApi) ([]GatewayApi, error)
	RealDeleteByRuntimeServiceId(string) error
}

type GatewayMockService interface {
	Update(*GatewayMock) error
	Insert(*GatewayMock) error
	GetMockByAny(*GatewayMock) (*GatewayMock, error)
}

type GatewayExtraService interface {
	GetByKeyAndField(key string, field string) (*GatewayExtra, error)
}

type GatewayPluginInstanceService interface {
	NewSession(...*SessionHelper) (GatewayPluginInstanceService, error)
	Insert(*GatewayPluginInstance) error
	DeleteByRouteId(routeId string) error
	DeleteByApiId(routeId string) error
	DeleteByServiceId(serviceId string) error
	DeleteByConsumerId(consumerId string) error
	Update(*GatewayPluginInstance) error
	GetByPluginNameAndApiId(pluginName string, apiId string) (*GatewayPluginInstance, error)
	GetByAny(*GatewayPluginInstance) (*GatewayPluginInstance, error)
	DeleteById(string) error
	SelectByOnlyApiId(apiId string) ([]GatewayPluginInstance, error)
	SelectByPolicyId(policyId string) ([]GatewayPluginInstance, error)
}

type GatewayPolicyService interface {
	GetById(policyId string) (*GatewayPolicy, error)
	SelectByCategory(category string) ([]GatewayPolicy, error)
	Insert(*GatewayPolicy) error
	Update(*GatewayPolicy) error
	GetByPolicyName(policyName string, consumerId string) (*GatewayPolicy, error)
	DeleteById(string) error
	SelectByCategoryAndConsumer(category string, consumerId string) ([]GatewayPolicy, error)
	SelectInIds(ids ...string) ([]GatewayPolicy, error)
	SelectByAny(*GatewayPolicy) ([]GatewayPolicy, error)
	GetByAny(*GatewayPolicy) (*GatewayPolicy, error)
}

type GatewayRouteService interface {
	NewSession(...*SessionHelper) (GatewayRouteService, error)
	GetById(string) (*GatewayRoute, error)
	Insert(*GatewayRoute) error
	GetByApiId(apiId string) (*GatewayRoute, error)
	DeleteById(string) error
	Update(*GatewayRoute) error
}

type GatewayServiceService interface {
	NewSession(...*SessionHelper) (GatewayServiceService, error)
	GetById(string) (*GatewayService, error)
	Insert(*GatewayService) error
	GetByApiId(apiId string) (*GatewayService, error)
	DeleteById(string) error
	Update(*GatewayService) error
}

type GatewayOrgClientService interface {
	NewSession(...*SessionHelper) (GatewayOrgClientService, error)
	Insert(*GatewayOrgClient) error
	GetByAny(*GatewayOrgClient) (*GatewayOrgClient, error)
	GetById(string) (*GatewayOrgClient, error)
	Update(*GatewayOrgClient) error
	DeleteById(string) error
	SelectByAny(*GatewayOrgClient) ([]GatewayOrgClient, error)
	CheckUnique(*GatewayOrgClient) (bool, error)
}

type GatewayConsumerService interface {
	Get(*GatewayConsumer) (*GatewayConsumer, error)
	GetDefaultConsumerName(*GatewayConsumer) string
	GetDefaultConsumer(*GatewayConsumer) (*GatewayConsumer, error)
	GetByName(name string) (*GatewayConsumer, error)
	Insert(*GatewayConsumer) error
	Update(*GatewayConsumer) error
	GetById(string) (*GatewayConsumer, error)
	SelectByAny(*GatewayConsumer) ([]GatewayConsumer, error)
	GetByAny(*GatewayConsumer) (*GatewayConsumer, error)
	DeleteById(string) error
	CheckUnique(*GatewayConsumer) (bool, error)
	GetPage(options []SelectOption, page *Page) (*PageQuery, error)
	Count(options []SelectOption) (int64, error)
	SelectByOptions(options []SelectOption) ([]GatewayConsumer, error)
}

type GatewayConsumerApiService interface {
	Insert(*GatewayConsumerApi) error
	Update(*GatewayConsumerApi) error
	DeleteById(id string) error
	GetByConsumerAndApi(string, string) (*GatewayConsumerApi, error)
	GetById(string) (*GatewayConsumerApi, error)
	SelectByConsumer(string) ([]GatewayConsumerApi, error)
	SelectByApi(string) ([]GatewayConsumerApi, error)
}

type GatewayGroupService interface {
	Insert(*GatewayGroup) error
	GetPageByConsumerId(consumerId string, page *Page) (*PageQuery, error)
	CountByConsumerId(consumerId string) (int64, error)
	DeleteById(groupId string) error
	Update(*GatewayGroup) error
	GetById(groupId string) (*GatewayGroup, error)
	GetByNameAndConsumerId(groupName string, consumerId string) (*GatewayGroup, error)
}

type IniService interface {
	GetValueByName(name string) (string, error)
}

type GatewayUpstreamService interface {
	UpdateRegister(*xorm.Session, *GatewayUpstream) (bool, bool, string, error)
	GetValidIdForUpdate(string, *xorm.Session) (string, error)
	UpdateValidId(*GatewayUpstream, ...*xorm.Session) error
	UpdateAutoBind(*GatewayUpstream) error
	SelectByAny(*GatewayUpstream) ([]GatewayUpstream, error)
}

type GatewayUpstreamApiService interface {
	GetById(string) (*GatewayUpstreamApi, error)
	DeleteById(string) error
	Recover(string) error
	Insert(*xorm.Session, *GatewayUpstreamApi) (string, error)
	GetLastApiId(*GatewayUpstreamApi) string
	UpdateApiId(*GatewayUpstreamApi) error
	GetPage(ids []string, page *Page) (*PageQuery, error)
	SelectInIdsAndDeleted(ids []string) ([]GatewayUpstreamApi, error)
	SelectInIds(ids []string) ([]GatewayUpstreamApi, error)
}

type GatewayUpstreamRegisterRecordService interface {
	Insert(*xorm.Session, *GatewayUpstreamRegisterRecord) error
	GetPage(upstreamId string, page *Page) (*PageQuery, error)
	Get(upstreamId string, registerId string) (*GatewayUpstreamRegisterRecord, error)
}

type GatewayAzInfoService interface {
	GetAz(*GatewayAzInfo) (string, error)
	GetAzInfoByClusterName(name string) (*orm.GatewayAzInfo, error)
	GetAzInfo(*GatewayAzInfo) (*GatewayAzInfo, error)
	SelectByAny(*GatewayAzInfo) ([]GatewayAzInfo, error)
	SelectValidAz() ([]GatewayAzInfo, error)
}

type GatewayKongInfoService interface {
	NewSession(...*SessionHelper) (GatewayKongInfoService, error)
	Update(*GatewayKongInfo) error
	Insert(*GatewayKongInfo) error
	GetTenantId(projectId, env, az string) (string, error)
	GetByAny(*GatewayKongInfo) (*GatewayKongInfo, error)
	GetForUpdate(projectId, env, az string) (*GatewayKongInfo, error)
	GenK8SInfo(kongInfo *orm.GatewayKongInfo) (string, string, error)
	GetKongInfo(*GatewayKongInfo) (*GatewayKongInfo, error)
	GetK8SInfo(*GatewayKongInfo) (string, string, error)
	GetBelongTuples(instanceId string) ([]KongBelongTuple, error)
}

type GatewayUpstreamLbService interface {
	Get(*GatewayUpstreamLb) (*GatewayUpstreamLb, error)
	GetForUpdate(*xorm.Session, *GatewayUpstreamLb) (*GatewayUpstreamLb, error)
	Insert(*xorm.Session, *GatewayUpstreamLb) error
	UpdateDeploymentId(string, int) error
	GetById(string) (*GatewayUpstreamLb, error)
	GetByKongId(string) (*GatewayUpstreamLb, error)
}

type GatewayUpstreamLbTargetService interface {
	Insert(*GatewayUpstreamLbTarget) error
	SelectByDeploymentId(int) ([]GatewayUpstreamLbTarget, error)
	Select(lbId, target string) ([]GatewayUpstreamLbTarget, error)
	Delete(string) error
}

type GatewayRuntimeServiceService interface {
	NewSession(...*SessionHelper) (GatewayRuntimeServiceService, error)
	CreateIfNotExist(session *xorm.Session, dao *GatewayRuntimeService) (*GatewayRuntimeService, error)
	Insert(*GatewayRuntimeService) error
	Update(*GatewayRuntimeService) error
	Delete(string) error
	Get(string) (*GatewayRuntimeService, error)
	SelectByAny(*GatewayRuntimeService) ([]GatewayRuntimeService, error)
	GetByAny(*GatewayRuntimeService) (*GatewayRuntimeService, error)
}

type GatewayDomainService interface {
	NewSession(...*SessionHelper) (GatewayDomainService, error)
	Insert(*GatewayDomain) error
	Update(*GatewayDomain) error
	Delete(string) error
	Get(string) (*GatewayDomain, error)
	DeleteByAny(*GatewayDomain) (int64, error)
	SelectByAny(*GatewayDomain) ([]GatewayDomain, error)
	GetByAny(*GatewayDomain) (*GatewayDomain, error)
	GetPage(options []SelectOption, page *Page) (*PageQuery, error)
	SelectByOptions(options []SelectOption) ([]GatewayDomain, error)
}
