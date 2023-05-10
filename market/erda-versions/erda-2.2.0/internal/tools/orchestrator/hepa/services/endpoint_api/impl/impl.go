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

package impl

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"regexp"
	"strconv"
	"strings"
	"sync"

	"github.com/pkg/errors"
	log "github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/apipolicy"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/bundle"
	orgCache "github.com/erda-project/erda/internal/tools/orchestrator/hepa/cache/org"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/util"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/config"
	gw "github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/i18n"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/k8s"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong"
	kongDto "github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
	db "github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/service"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/api_policy"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/domain"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/endpoint_api"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/global"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/micro_api"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/openapi_consumer"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/openapi_rule"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/runtime_service"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/zone"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

type GatewayOpenapiServiceImpl struct {
	packageDb       db.GatewayPackageService
	packageApiDb    db.GatewayPackageApiService
	zoneInPackageDb db.GatewayZoneInPackageService
	apiInPackageDb  db.GatewayApiInPackageService
	packageInDb     db.GatewayPackageInConsumerService
	serviceDb       db.GatewayServiceService
	routeDb         db.GatewayRouteService
	consumerDb      db.GatewayConsumerService
	apiDb           db.GatewayApiService
	upstreamApiDb   db.GatewayUpstreamApiService
	azDb            db.GatewayAzInfoService
	kongDb          db.GatewayKongInfoService
	apiBiz          *micro_api.GatewayApiService
	zoneBiz         *zone.GatewayZoneService
	ruleBiz         *openapi_rule.GatewayOpenapiRuleService
	consumerBiz     *openapi_consumer.GatewayOpenapiConsumerService
	globalBiz       *global.GatewayGlobalService
	policyBiz       *api_policy.GatewayApiPolicyService
	runtimeDb       db.GatewayRuntimeServiceService
	domainBiz       *domain.GatewayDomainService
	ctx             context.Context
	reqCtx          context.Context
}

var once sync.Once

func NewGatewayOpenapiServiceImpl() error {
	once.Do(
		func() {
			packageDb, _ := db.NewGatewayPackageServiceImpl()
			packageApiDb, _ := db.NewGatewayPackageApiServiceImpl()
			zoneInPackageDb, _ := db.NewGatewayZoneInPackageServiceImpl()
			apiInPackageDb, _ := db.NewGatewayApiInPackageServiceImpl()
			packageInDb, _ := db.NewGatewayPackageInConsumerServiceImpl()
			serviceDb, _ := db.NewGatewayServiceServiceImpl()
			routeDb, _ := db.NewGatewayRouteServiceImpl()
			consumerDb, _ := db.NewGatewayConsumerServiceImpl()
			apiDb, _ := db.NewGatewayApiServiceImpl()
			upstreamApiDb, _ := db.NewGatewayUpstreamApiServiceImpl()
			kongDb, _ := db.NewGatewayKongInfoServiceImpl()
			azDb, _ := db.NewGatewayAzInfoServiceImpl()
			runtimeDb, _ := db.NewGatewayRuntimeServiceServiceImpl()
			endpoint_api.Service = &GatewayOpenapiServiceImpl{
				packageDb:       packageDb,
				packageApiDb:    packageApiDb,
				zoneInPackageDb: zoneInPackageDb,
				apiInPackageDb:  apiInPackageDb,
				packageInDb:     packageInDb,
				serviceDb:       serviceDb,
				routeDb:         routeDb,
				consumerDb:      consumerDb,
				apiDb:           apiDb,
				upstreamApiDb:   upstreamApiDb,
				kongDb:          kongDb,
				azDb:            azDb,
				zoneBiz:         &zone.Service,
				apiBiz:          &micro_api.Service,
				consumerBiz:     &openapi_consumer.Service,
				ruleBiz:         &openapi_rule.Service,
				globalBiz:       &global.Service,
				policyBiz:       &api_policy.Service,
				runtimeDb:       runtimeDb,
				domainBiz:       &domain.Service,
			}
		})
	return nil
}

func (impl GatewayOpenapiServiceImpl) Clone(ctx context.Context) endpoint_api.GatewayOpenapiService {
	newService := impl
	newService.reqCtx = ctx
	return &newService
}

func (impl GatewayOpenapiServiceImpl) createApiAclRule(aclType, packageId, apiId string) (*gw.OpenapiRule, error) {
	rule, err := impl.createAclRule(aclType, packageId)
	if err != nil {
		return nil, err
	}
	rule.PackageApiId = apiId
	rule.Region = gw.API_RULE
	return rule, nil
}

func (impl GatewayOpenapiServiceImpl) createAclRule(aclType, packageId string) (*gw.OpenapiRule, error) {
	var consumers []orm.GatewayConsumer
	var aclRule *gw.OpenapiRule
	consumers, err := (*impl.consumerBiz).GetConsumersOfPackage(packageId)
	if err != nil {
		return nil, err
	}
	aclRule = &gw.OpenapiRule{
		PackageId:  packageId,
		PluginName: gw.ACL,
		Category:   gw.ACL_RULE,
	}
	var buffer bytes.Buffer
	for _, consumer := range consumers {
		if buffer.Len() > 0 {
			buffer.WriteString(",")
		}
		buffer.WriteString((*impl.consumerBiz).GetKongConsumerName(&consumer))
	}
	wl := buffer.String()
	config := map[string]interface{}{}
	if wl == "" {
		wl = ","
	}
	config["whitelist"] = wl
	aclRule.Config = config
	switch aclType {
	case gw.ACL_ON:
		aclRule.Enabled = true
	case gw.ACL_OFF:
		aclRule.Enabled = false
	default:
		return nil, errors.Errorf("invalid acl type:%s", aclType)
	}
	return aclRule, nil
}

func (impl GatewayOpenapiServiceImpl) createApiAuthRule(packageId, apiId string, enable ...bool) (*gw.OpenapiRule, error) {
	dao, err := impl.packageDb.Get(packageId)
	if err != nil {
		return nil, err
	}
	if dao == nil {
		return nil, errors.Errorf("endpoint not found, id:%s", packageId)
	}
	rule, err := impl.createAuthRule(dao.AuthType, dao, enable...)
	if err != nil {
		return nil, err
	}
	rule.PackageApiId = apiId
	rule.Region = gw.API_RULE
	return rule, nil
}

func (impl GatewayOpenapiServiceImpl) createAuthRule(authType string, pack *orm.GatewayPackage, enable ...bool) (*gw.OpenapiRule, error) {
	ruleEnabled := true
	if len(enable) > 0 {
		ruleEnabled = enable[0]
	}
	authRule := &gw.OpenapiRule{
		PackageId:  pack.Id,
		PluginName: authType,
		Category:   gw.AUTH_RULE,
		Enabled:    ruleEnabled,
	}
	switch authType {
	case gw.AT_KEY_AUTH:
		authRule.Config = gw.KEYAUTH_CONFIG
	case gw.AT_OAUTH2:
		authRule.Config = gw.OAUTH2_CONFIG
	case gw.AT_SIGN_AUTH:
		authRule.Config = gw.SIGNAUTH_CONFIG
	case gw.AT_HMAC_AUTH:
		kongInfo, err := impl.kongDb.GetKongInfo(&orm.GatewayKongInfo{
			Az:        pack.DiceClusterName,
			ProjectId: pack.DiceProjectId,
			Env:       pack.DiceEnv,
		})
		if err != nil {
			return nil, err
		}
		kongAdapter := kong.NewKongAdapter(kongInfo.KongAddr)
		enabled, err := kongAdapter.CheckPluginEnabled(gw.AT_HMAC_AUTH)
		if err != nil {
			return nil, err
		}
		if !enabled {
			return nil, errors.New("hmac-auth plugin is not enabled")
		}
		authRule.Config = gw.HMACAUTH_CONFIG
	case gw.AT_ALIYUN_APP:
		authRule.Config = nil
		authRule.NotKongPlugin = true
	default:
		return nil, errors.Errorf("invalide auth type:%s", authType)
	}
	return authRule, nil
}

func (impl GatewayOpenapiServiceImpl) CreatePackage(args *gw.DiceArgsDto, dto *gw.PackageDto) (result *gw.PackageInfoDto, existName string, err error) {
	var diceInfo gw.DiceInfo
	var helper *db.SessionHelper
	var pack *orm.GatewayPackage
	var aclRule, authRule *gw.OpenapiRule
	var packSession db.GatewayPackageService
	var z *orm.GatewayZone
	var unique bool
	var domains []string
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
			if helper != nil {
				_ = helper.Rollback()
				helper.Close()
			}
		}
	}()
	if args.ProjectId == "" || args.Env == "" {
		err = errors.New("projectId or env is empty")
		return
	}
	dto.Name = strings.TrimSpace(dto.Name)
	for i := 0; i < len(dto.BindDomain); i++ {
		dto.BindDomain[i] = strings.TrimSpace(dto.BindDomain[i])
	}
	dto.BindDomain = util.UniqStringSlice(dto.BindDomain)
	auditCtx := map[string]interface{}{}
	auditCtx["endpoint"] = dto.Name
	auditCtx["domains"] = strings.Join(dto.BindDomain, ", ")
	defer func() {
		audit := common.MakeAuditInfo(impl.reqCtx, common.ScopeInfo{
			ProjectId: args.ProjectId,
			Workspace: args.Env,
		}, apistructs.CreateEndpointTemplate, err, auditCtx)
		if audit != nil {
			berr := bundle.Bundle.CreateAuditEvent(&apistructs.AuditCreateRequest{Audit: *audit})
			if berr != nil {
				log.Errorf("create audit failed, err:%+v", berr)
			}
		}
	}()
	az, err := impl.azDb.GetAz(&orm.GatewayAzInfo{
		Env:       args.Env,
		ProjectId: args.ProjectId,
	})
	if err != nil {
		return
	}
	err = dto.CheckValid()
	if err != nil {
		return
	}
	diceInfo = gw.DiceInfo{
		ProjectId: args.ProjectId,
		Env:       args.Env,
		Az:        az,
	}
	helper, err = db.NewSessionHelper()
	if err != nil {
		return
	}
	// create self zone
	z, err = (*impl.zoneBiz).CreateZone(zone.ZoneConfig{
		Name:      "package-" + dto.Name,
		ProjectId: args.ProjectId,
		Env:       args.Env,
		Az:        az,
		Type:      db.ZONE_TYPE_PACKAGE_NEW,
	}, helper)
	if err != nil {
		return
	}
	// create package in db
	packSession, err = impl.packageDb.NewSession(helper)
	if err != nil {
		return
	}
	pack = &orm.GatewayPackage{
		DiceProjectId:   args.ProjectId,
		DiceEnv:         args.Env,
		DiceClusterName: az,
		ZoneId:          z.Id,
		PackageName:     dto.Name,
		AuthType:        dto.AuthType,
		AclType:         dto.AclType,
		Scene:           dto.Scene,
		Description:     dto.Description,
	}
	unique, err = packSession.CheckUnique(pack)
	if err != nil {
		return
	}
	if !unique {
		err = errors.Errorf("package %s already exists", pack.PackageName)
		existName = pack.PackageName
		return
	}
	err = packSession.Insert(pack)
	if err != nil {
		return
	}
	domains, err = (*impl.domainBiz).TouchPackageDomain(args.OrgId, pack.Id, az, dto.BindDomain, helper)
	if err != nil {
		return
	}
	if dto.Scene == orm.OPENAPI_SCENE {
		// create auth, acl rule
		authRule, err = impl.createAuthRule(dto.AuthType, pack)
		if err != nil {
			return
		}
		authRule.Region = gw.PACKAGE_RULE
		aclRule, err = impl.createAclRule(dto.AclType, pack.Id)
		if err != nil {
			return
		}
		aclRule.Region = gw.PACKAGE_RULE
		err = (*impl.ruleBiz).CreateRule(diceInfo, authRule, helper)
		if err != nil {
			return
		}
		err = (*impl.ruleBiz).CreateRule(diceInfo, aclRule, helper)
		if err != nil {
			return
		}
		// update zone kong polices
		err = (*impl.ruleBiz).SetPackageKongPolicies(pack, helper)
		if err != nil {
			return
		}
	}
	err = helper.Commit()
	if err != nil {
		return
	}
	helper.Close()
	result = impl.packageDto(pack, domains)
	return
}

func (impl GatewayOpenapiServiceImpl) GetPackage(id string) (dto *gw.PackageInfoDto, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
		}
	}()
	if id == "" {
		err = errors.New("id is empty")
		return
	}
	dao, err := impl.packageDb.Get(id)
	var domains []string
	if err != nil {
		return
	}
	if dao == nil {
		err = errors.Errorf("package not found, packageId: %s", id)
		return
	}
	domains, err = (*impl.domainBiz).GetPackageDomains(dao.Id)
	if err != nil {
		return
	}
	dto = impl.packageDto(dao, domains)
	return
}

func (impl GatewayOpenapiServiceImpl) GetPackages(args *gw.GetPackagesDto) (res common.NewPageQuery, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened: %+v", err)
		}
	}()
	if args.ProjectId == "" || args.Env == "" {
		err = errors.New("projectId or env is empty")
		return
	}
	var domainInfos []orm.GatewayDomain
	var packageIds []string
	if args.Domain != "" {
		domainInfos, err = (*impl.domainBiz).FindDomains(args.Domain, args.ProjectId, args.Env, orm.PrefixMatch, orm.DT_PACKAGE)
		if err != nil {
			return
		}
	}
	for _, domainInfo := range domainInfos {
		if domainInfo.PackageId != "" {
			packageIds = append(packageIds, domainInfo.PackageId)
		}
	}
	pageInfo := common.NewPage2(args.PageSize, args.PageNo)
	options := args.GenSelectOptions()
	if len(packageIds) > 0 {
		options = append(options, orm.SelectOption{
			Type:   orm.Contains,
			Column: "id",
			Value:  packageIds,
		})
	}
	if args.Domain != "" && len(packageIds) == 0 {
		res = common.NewPages(nil, 0)
		return
	}
	var list []gw.PackageInfoDto
	var daos []orm.GatewayPackage
	var ok bool
	page, err := impl.packageDb.GetPage(options, (*common.Page)(pageInfo))
	if err != nil {
		return
	}
	daos, ok = page.Result.([]orm.GatewayPackage)
	if !ok {
		err = errors.New("type convert failed")
		return
	}

	for _, dao := range daos {
		var dto *gw.PackageInfoDto
		var domains []string
		domains, err = (*impl.domainBiz).GetPackageDomains(dao.Id)
		if err != nil {
			return
		}
		dto = impl.packageDto(&dao, domains)
		list = append(list, *dto)
	}
	res = common.NewPages(list, pageInfo.TotalNum)
	return
}

func (impl GatewayOpenapiServiceImpl) ListAllPackages() ([]orm.GatewayPackage, error) {
	return impl.packageDb.SelectByAny(new(orm.GatewayPackage))
}

func (impl GatewayOpenapiServiceImpl) packageDto(dao *orm.GatewayPackage, domains []string) *gw.PackageInfoDto {
	res := &gw.PackageInfoDto{
		Id:       dao.Id,
		CreateAt: dao.CreateTime.Format("2006-01-02T15:04:05"),
		PackageDto: gw.PackageDto{
			Name:        dao.PackageName,
			BindDomain:  domains,
			AuthType:    dao.AuthType,
			AclType:     dao.AclType,
			Description: dao.Description,
			Scene:       dao.Scene,
		},
	}
	return res
}

func (impl GatewayOpenapiServiceImpl) GetPackagesName(args *gw.GetPackagesDto) (list []gw.PackageInfoDto, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
		}
	}()
	if args.ProjectId == "" || args.Env == "" {
		return
	}
	list = []gw.PackageInfoDto{}
	var daos []orm.GatewayPackage
	az, err := impl.azDb.GetAz(&orm.GatewayAzInfo{
		Env:       args.Env,
		ProjectId: args.ProjectId,
	})
	if err != nil {
		return
	}
	daos, err = impl.packageDb.SelectByAny(&orm.GatewayPackage{
		DiceProjectId:   args.ProjectId,
		DiceEnv:         args.Env,
		DiceClusterName: az,
	})
	if err != nil {
		return
	}
	for _, dao := range daos {
		var dto *gw.PackageInfoDto
		var domains []string
		domains, err = (*impl.domainBiz).GetPackageDomains(dao.Id)
		if err != nil {
			return
		}
		dto = impl.packageDto(&dao, domains)
		list = append(list, *dto)
	}
	return
}

func (impl GatewayOpenapiServiceImpl) updatePackageApiHost(pack *orm.GatewayPackage, hosts []string) error {
	kongInfo, err := impl.kongDb.GetKongInfo(&orm.GatewayKongInfo{
		Az:        pack.DiceClusterName,
		ProjectId: pack.DiceProjectId,
		Env:       pack.DiceEnv,
	})
	if err != nil {
		return err
	}
	kongAdapter := kong.NewKongAdapter(kongInfo.KongAddr)
	apis, err := impl.packageApiDb.SelectByAny(&orm.GatewayPackageApi{
		PackageId:    pack.Id,
		RedirectType: gw.RT_URL,
	})
	if err != nil {
		return err
	}
	for _, api := range apis {
		if api.Origin != string(gw.FROM_CUSTOM) && api.Origin != string(gw.FROM_DICEYML) {
			continue
		}
		route, err := impl.routeDb.GetByApiId(api.Id)
		if err != nil {
			return err
		}
		if route == nil {
			return errors.Errorf("can't find route of api:%s", api.Id)
		}
		req := kongDto.NewKongRouteReqDto()
		req.RouteId = route.RouteId
		req.Hosts = hosts
		req.AddTag("package_api_id", api.Id)
		resp, err := kongAdapter.UpdateRoute(req)
		if err != nil {
			return err
		}
		routeDao := impl.kongRouteDao(resp, route.ServiceId, api.Id)
		routeDao.Id = route.Id
		err = impl.routeDb.Update(routeDao)
		if err != nil {
			return err
		}
	}
	return nil
}

func (impl GatewayOpenapiServiceImpl) UpdatePackage(orgId, id string, dto *gw.PackageDto) (result *gw.PackageInfoDto, err error) {
	var diceInfo gw.DiceInfo
	var dao *orm.GatewayPackage
	var z *orm.GatewayZone
	var apiSession db.GatewayPackageApiService
	var domains []string
	var apis []orm.GatewayPackageApi
	var session *db.SessionHelper
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
			if session != nil {
				_ = session.Rollback()
				session.Close()
			}
		}
	}()
	if id == "" {
		err = errors.New("id is empty")
		return
	}
	needUpdate := false
	auditCtx := map[string]interface{}{}
	auditCtx["endpoint"] = dto.Name
	auditCtx["domains"] = strings.Join(dto.BindDomain, ", ")
	defer func() {
		audit := common.MakeAuditInfo(impl.reqCtx, common.ScopeInfo{
			ProjectId: dao.DiceProjectId,
			Workspace: dao.DiceEnv,
		}, apistructs.UpdateEndpointTemplate, err, auditCtx)
		if audit != nil {
			berr := bundle.Bundle.CreateAuditEvent(&apistructs.AuditCreateRequest{Audit: *audit})
			if berr != nil {
				log.Errorf("create audit failed, err:%+v", berr)
			}
		}
	}()
	domainHasDiff := false
	session, err = db.NewSessionHelper()
	if err != nil {
		return
	}
	err = dto.CheckValid()
	if err != nil {
		return
	}
	for i := 0; i < len(dto.BindDomain); i++ {
		dto.BindDomain[i] = strings.TrimSpace(dto.BindDomain[i])
	}
	dto.BindDomain = util.UniqStringSlice(dto.BindDomain)
	dao, err = impl.packageDb.Get(id)
	if err != nil {
		return
	}
	diceInfo = gw.DiceInfo{
		ProjectId: dao.DiceProjectId,
		Env:       dao.DiceEnv,
		Az:        dao.DiceClusterName,
	}
	domainHasDiff, err = (*impl.domainBiz).IsPackageDomainsDiff(id, dao.DiceClusterName, dto.BindDomain, session)
	if err != nil {
		return
	}
	if domainHasDiff {
		needUpdate = true
		domains, err = (*impl.domainBiz).TouchPackageDomain(orgId, id, dao.DiceClusterName, dto.BindDomain, session)
		if err != nil {
			return
		}

		z, err = (*impl.zoneBiz).GetZone(dao.ZoneId, session)
		if err != nil {
			return
		}
		//老类型兼容
		if z.Type == db.ZONE_TYPE_PACKAGE {
			_, err = (*impl.zoneBiz).UpdateZoneRoute(dao.ZoneId, zone.ZoneRoute{
				zone.RouteConfig{
					Hosts: domains,
					Path:  "/",
				},
			})
			if err != nil {
				return
			}
		}
		apiSession, err = impl.packageApiDb.NewSession(session)
		if err != nil {
			return
		}
		apis, err = apiSession.SelectByAny(&orm.GatewayPackageApi{PackageId: id})
		if err != nil {
			return
		}
		wg := sync.WaitGroup{}
		for _, api := range apis {
			wg.Add(1)
			go func(apiRefer orm.GatewayPackageApi) {
				defer util.DoRecover()
				log.Debugf("begin touch pacakge api zone, api:%+v", apiRefer)
				zoneId, aerr := impl.TouchPackageApiZone(endpoint_api.PackageApiInfo{&apiRefer, domains, dao.DiceProjectId, dao.DiceEnv, dao.DiceClusterName, false})
				if aerr != nil {
					err = aerr
				}
				if apiRefer.ZoneId == "" {
					apiRefer.ZoneId = zoneId
					aerr = apiSession.Update(&apiRefer)
					if aerr != nil {
						err = aerr
					}
				}
				wg.Done()
				log.Debugf("complete touch pacakge api zone, api:%+v", apiRefer)
			}(api)
		}
		wg.Wait()
		if err != nil {
			return
		}
		err = impl.updatePackageApiHost(dao, domains)
		if err != nil {
			return
		}
	}
	if dao.AuthType != dto.AuthType {
		needUpdate = true
		var oldAuthRule []gw.OpenapiRuleInfo
		oldAuthRule, err = (*impl.ruleBiz).GetPackageRules(id, session, gw.AUTH_RULE)
		if err != nil {
			return
		}
		for _, rule := range oldAuthRule {
			err = (*impl.ruleBiz).DeleteRule(rule.Id, session)
			if err != nil {
				return
			}
		}
		if dto.AuthType != "" {
			var authRule *gw.OpenapiRule
			authRule, err = impl.createAuthRule(dto.AuthType, dao)
			if err != nil {
				return
			}
			authRule.Region = gw.PACKAGE_RULE
			err = (*impl.ruleBiz).CreateRule(diceInfo, authRule, session)
			if err != nil {
				return
			}
		}
	}
	if dao.AclType != dto.AclType {
		needUpdate = true
		var oldAclRule []gw.OpenapiRuleInfo
		oldAclRule, err = (*impl.ruleBiz).GetPackageRules(id, nil, gw.ACL_RULE)
		if err != nil {
			return
		}
		for _, rule := range oldAclRule {
			err = (*impl.ruleBiz).DeleteRule(rule.Id, session)
			if err != nil {
				return
			}
		}
		if dto.AclType != "" {
			var aclRule *gw.OpenapiRule
			aclRule, err = impl.createAclRule(dto.AclType, id)
			if err != nil {
				return
			}
			aclRule.Region = gw.PACKAGE_RULE
			err = (*impl.ruleBiz).CreateRule(diceInfo, aclRule, session)
			if err != nil {
				return
			}
		}
	}
	dao.AuthType = dto.AuthType
	dao.AclType = dto.AclType
	dao.Description = dto.Description
	err = impl.packageDb.Update(dao)
	if err != nil {
		return
	}
	if needUpdate {
		// update zone kong polices
		err = (*impl.ruleBiz).SetPackageKongPolicies(dao, session)
		if err != nil {
			return
		}
	}
	err = session.Commit()
	if err != nil {
		return
	}
	session.Close()
	result = impl.packageDto(dao, dto.BindDomain)
	return
}

func (impl GatewayOpenapiServiceImpl) TryClearRuntimePackage(runtimeService *orm.GatewayRuntimeService, session *db.SessionHelper, force ...bool) error {
	packSession, _ := impl.packageDb.NewSession(session)
	packApiSession, _ := impl.packageApiDb.NewSession(session)
	pack, err := packSession.GetByAny(&orm.GatewayPackage{
		RuntimeServiceId: runtimeService.Id,
	})
	if err != nil {
		return err
	}
	if pack != nil {
		apis, err := packApiSession.SelectByAny(&orm.GatewayPackageApi{
			PackageId: pack.Id,
			Origin:    string(gw.FROM_CUSTOM),
		})
		if err != nil {
			return err
		}
		otherServiceApiExist := false
		for _, api := range apis {
			if api.DiceApp != runtimeService.AppName || api.DiceService != runtimeService.ServiceName {
				otherServiceApiExist = true
			}
		}
		if !otherServiceApiExist || (len(force) > 0 && force[0]) {
			err = impl.deletePackage(pack.Id, session)
			if err != nil {
				return err
			}
		}
	}
	return nil
}

type ctxWg struct {
}

func (impl *GatewayOpenapiServiceImpl) deletePackage(id string, session *db.SessionHelper, auditCtx ...map[string]interface{}) error {
	// var consumers []orm.GatewayConsumer
	var apis []orm.GatewayPackageApi
	var dao *orm.GatewayPackage
	var oldDomains []string
	defer func() {
		if len(auditCtx) == 0 {
			return
		}
		if dao == nil {
			return
		}
		auditCtx[0]["projectId"] = dao.DiceProjectId
		auditCtx[0]["workspace"] = dao.DiceEnv
		auditCtx[0]["endpoint"] = dao.PackageName
		auditCtx[0]["domains"] = oldDomains
	}()
	wg := &sync.WaitGroup{}
	impl.ctx = context.WithValue(context.Background(), ctxWg{}, wg)
	packSession, err := impl.packageDb.NewSession(session)
	if err != nil {
		return err
	}
	apiSession, err := impl.packageApiDb.NewSession(session)
	if err != nil {
		return err
	}
	dao, err = packSession.Get(id)
	if err != nil {
		return err
	}
	if dao == nil {
		return nil
	}
	if dao.Scene == orm.UNITY_SCENE {
		return errors.New("unity scene can't delete")
	}
	// remove consumer check
	// consumers, err = (*impl.consumerBiz).GetConsumersOfPackage(id)
	// if err != nil {
	// 	return err
	// }
	// if len(consumers) > 0 {
	// 	return errors.New("this endpoint has granted access to other consumers, please cancel first")
	// }
	apis, _ = apiSession.SelectByAny(&orm.GatewayPackageApi{
		PackageId: id,
	})
	for _, api := range apis {
		wg.Add(1)
		_, err := impl.DeletePackageApi(id, api.Id)
		if err != nil {
			return errors.Errorf("delete package api failed, err:%+v", err)
		}
	}
	err = impl.apiInPackageDb.DeleteByPackageId(id)
	if err != nil {
		return err
	}
	err = impl.packageInDb.DeleteByPackageId(id)
	if err != nil {
		return err
	}
	err = (*impl.ruleBiz).DeleteByPackage(dao)
	if err != nil {
		return err
	}
	if dao.ZoneId != "" {
		err = (*impl.zoneBiz).DeleteZone(dao.ZoneId)
		if err != nil {
			return err
		}
	}
	oldDomains, err = (*impl.domainBiz).GetPackageDomains(id, session)
	if err != nil {
		return err
	}
	_, err = (*impl.domainBiz).TouchPackageDomain("", id, dao.DiceClusterName, nil, session)
	if err != nil {
		return err
	}
	err = packSession.Delete(id, true)
	if err != nil {
		return err
	}
	return nil
}

func (impl GatewayOpenapiServiceImpl) DeletePackage(id string) (result bool, err error) {
	var session *db.SessionHelper
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
			if session != nil {
				_ = session.Rollback()
				session.Close()
			}
		}
	}()
	auditCtx := map[string]interface{}{}
	defer func() {
		if auditCtx["projectId"] == nil || auditCtx["workspace"] == nil {
			return
		}
		audit := common.MakeAuditInfo(impl.reqCtx, common.ScopeInfo{
			ProjectId: auditCtx["projectId"].(string),
			Workspace: auditCtx["workspace"].(string),
		}, apistructs.DeleteEndpointTemplate, err, auditCtx)
		if audit != nil {
			berr := bundle.Bundle.CreateAuditEvent(&apistructs.AuditCreateRequest{Audit: *audit})
			if berr != nil {
				log.Errorf("create audit failed, err:%+v", berr)
			}
		}
	}()
	if id == "" {
		err = errors.New("id is empty")
		return
	}
	session, _ = db.NewSessionHelper()
	err = impl.deletePackage(id, session, auditCtx)
	if err != nil {
		return
	}
	err = session.Commit()
	if err != nil {
		return
	}
	session.Close()
	result = true
	return
}

func (impl GatewayOpenapiServiceImpl) packageApiDao(dto *gw.OpenapiDto) *orm.GatewayPackageApi {
	dao := &orm.GatewayPackageApi{
		ApiPath:      dto.ApiPath,
		Method:       dto.Method,
		DiceApp:      dto.RedirectApp,
		DiceService:  dto.RedirectService,
		RedirectAddr: dto.RedirectAddr,
		RedirectPath: dto.RedirectPath,
		RedirectType: dto.RedirectType,
		Description:  dto.Description,
		Origin:       string(gw.FROM_CUSTOM),
	}
	if dto.AllowPassAuth {
		dao.AclType = gw.ACL_OFF
	}
	if dto.Origin != "" {
		dao.Origin = string(dto.Origin)
	}
	return dao
}

func (impl GatewayOpenapiServiceImpl) createKongServiceReq(dto *gw.OpenapiDto, serviceId ...string) *kongDto.KongServiceReqDto {
	i := 0
	reqDto := &kongDto.KongServiceReqDto{
		Url:            dto.AdjustRedirectAddr,
		ConnectTimeout: 5000,
		ReadTimeout:    60000,
		WriteTimeout:   60000,
		Retries:        &i,
	}
	if len(serviceId) != 0 {
		reqDto.ServiceId = serviceId[0]
	}
	return reqDto
}

func (impl GatewayOpenapiServiceImpl) kongServiceDao(dto *kongDto.KongServiceRespDto, apiId string) *orm.GatewayService {
	return &orm.GatewayService{
		ServiceId:   dto.Id,
		ServiceName: dto.Name,
		Protocol:    dto.Protocol,
		Host:        dto.Host,
		Port:        strconv.Itoa(dto.Port),
		Path:        dto.Path,
		ApiId:       apiId,
	}
}

func (impl GatewayOpenapiServiceImpl) touchKongService(adapter kong.KongAdapter, dto *gw.OpenapiDto, apiId string,
	helper ...*db.SessionHelper) (string, error) {
	var serviceSession db.GatewayServiceService
	var err error
	if len(helper) > 0 {
		serviceSession, err = impl.serviceDb.NewSession(helper[0])
		if err != nil {
			return "", err
		}
	} else {
		serviceSession = impl.serviceDb
	}
	service, err := serviceSession.GetByApiId(apiId)
	if err != nil {
		return "", err
	}
	var req *kongDto.KongServiceReqDto
	if service == nil {
		req = impl.createKongServiceReq(dto)
	} else {
		req = impl.createKongServiceReq(dto, service.ServiceId)
	}
	resp, err := adapter.CreateOrUpdateService(req)
	if err != nil {
		return "", err
	}
	serviceDao := impl.kongServiceDao(resp, apiId)
	if err != nil {
		return "", err
	}
	if service == nil {
		err = serviceSession.Insert(serviceDao)
		if err != nil {
			return "", err
		}
	} else {
		serviceDao.Id = service.Id
		err = serviceSession.Update(serviceDao)
		if err != nil {
			return "", err
		}
	}
	return resp.Id, nil
}

func (impl GatewayOpenapiServiceImpl) deleteKongService(adapter kong.KongAdapter, apiId string) error {
	service, err := impl.serviceDb.GetByApiId(apiId)
	if err != nil {
		return err
	}
	if service == nil {
		return nil
	}
	err = adapter.DeleteService(service.ServiceId)
	if err != nil {
		return err
	}
	err = impl.serviceDb.DeleteById(service.Id)
	if err != nil {
		return err
	}
	return nil
}

func (impl GatewayOpenapiServiceImpl) createKongRouteReq(dto *gw.OpenapiDto, serviceId string, routeId ...string) *kongDto.KongRouteReqDto {
	reqDto := kongDto.NewKongRouteReqDto()
	if dto.Method != "" {
		reqDto.Methods = []string{dto.Method}
	}
	if dto.ApiPath != "" {
		reqDto.Paths = []string{dto.AdjustPath}
	}
	reqDto.Hosts = dto.Hosts
	reqDto.Service = &kongDto.Service{}
	reqDto.Service.Id = serviceId
	if dto.IsRegexPath {
		ignore := strings.Count(dto.AdjustPath, "^/") + strings.Count(dto.AdjustPath, `\/`)
		reqDto.RegexPriority = strings.Count(dto.AdjustPath, "/") - ignore
	}
	if len(routeId) != 0 {
		reqDto.RouteId = routeId[0]
	}
	return reqDto
}

func (impl GatewayOpenapiServiceImpl) kongRouteDao(dto *kongDto.KongRouteRespDto, serviceId, apiId string) *orm.GatewayRoute {
	protocols, _ := json.Marshal(dto.Protocols)
	hosts, _ := json.Marshal(dto.Hosts)
	paths, _ := json.Marshal(dto.Paths)
	methods, _ := json.Marshal(dto.Methods)
	return &orm.GatewayRoute{
		RouteId:   dto.Id,
		Protocols: string(protocols),
		Hosts:     string(hosts),
		Paths:     string(paths),
		Methods:   string(methods),
		ServiceId: serviceId,
		ApiId:     apiId,
	}
}

func (impl GatewayOpenapiServiceImpl) touchKongRoute(adapter kong.KongAdapter, dto *gw.OpenapiDto, apiId string,
	helper ...*db.SessionHelper) (string, error) {
	var routeSession db.GatewayRouteService
	var err error
	if len(helper) > 0 {
		routeSession, err = impl.routeDb.NewSession(helper[0])
		if err != nil {
			return "", err
		}
	} else {
		routeSession = impl.routeDb
	}
	route, err := routeSession.GetByApiId(apiId)
	if err != nil {
		return "", err
	}
	var req *kongDto.KongRouteReqDto
	if route == nil {
		req = impl.createKongRouteReq(dto, dto.ServiceId)
	} else {
		req = impl.createKongRouteReq(dto, dto.ServiceId, route.RouteId)
	}
	req.AddTag("package_api_id", apiId)
	resp, err := adapter.CreateOrUpdateRoute(req)
	if err != nil {
		return "", err
	}
	routeDao := impl.kongRouteDao(resp, dto.ServiceId, apiId)
	if err != nil {
		return "", err
	}
	if route == nil {
		err = routeSession.Insert(routeDao)
		if err != nil {
			return "", err
		}
	} else {
		routeDao.Id = route.Id
		err = routeSession.Update(routeDao)
		if err != nil {
			return "", err
		}
	}
	return resp.Id, nil
}

func (impl GatewayOpenapiServiceImpl) deleteKongRoute(adapter kong.KongAdapter, apiId string) error {
	route, err := impl.routeDb.GetByApiId(apiId)
	if err != nil {
		return err
	}
	if route == nil {
		return nil
	}
	err = adapter.DeleteRoute(route.RouteId)
	if err != nil {
		return err
	}
	err = impl.routeDb.DeleteById(route.Id)
	if err != nil {
		return err
	}
	return nil
}

func (impl GatewayOpenapiServiceImpl) deleteKongApi(adapter kong.KongAdapter, apiId string) error {
	err := impl.deleteKongRoute(adapter, apiId)
	if err != nil {
		return err
	}
	err = impl.deleteKongService(adapter, apiId)
	if err != nil {
		return err
	}
	return nil
}

func (impl GatewayOpenapiServiceImpl) createOrUpdatePlugins(adapter kong.KongAdapter, dto *gw.OpenapiDto) error {
	if dto.ServiceRewritePath != "" {
		reqDto := &kongDto.KongPluginReqDto{
			Name: "path-variable",
			// ServiceId: dto.ServiceId,
			RouteId: dto.RouteId,
			Config: map[string]interface{}{
				"request_regex": dto.AdjustPath,
				"rewrite_path":  dto.ServiceRewritePath,
			},
		}
		_, err := adapter.CreateOrUpdatePlugin(reqDto)
		if err != nil {
			return err
		}
	}
	if config.ServerConf.HasRouteInfo {
		reqDto := &kongDto.KongPluginReqDto{
			Name:    "set-route-info",
			RouteId: dto.RouteId,
			Config: map[string]interface{}{
				"project_id": dto.ProjectId,
				"workspace":  strings.ToLower(dto.Env),
				"api_path":   dto.ApiPath,
			},
		}
		_, err := adapter.CreateOrUpdatePlugin(reqDto)
		if err != nil {
			return err
		}
	}
	return nil
}

func (impl GatewayOpenapiServiceImpl) SessionCreatePackageApi(id string, dto *gw.OpenapiDto, session *db.SessionHelper, injectRuntimeDomain bool) (bool, string, *apistructs.Audit, error) {
	exist := false
	var pack *orm.GatewayPackage
	var kongInfo *orm.GatewayKongInfo
	var kongAdapter kong.KongAdapter
	var dao *orm.GatewayPackageApi
	var unique bool
	var serviceId, routeId string
	var apiSession db.GatewayPackageApiService
	var packSession db.GatewayPackageService
	var err error
	var zoneId string
	var domains []string
	var audit *apistructs.Audit
	needUpdateDomainPolicy := false
	method := dto.Method
	if valid, msg := dto.CheckValid(); !valid {
		return false, "", audit, errors.New(msg)
	}
	packSession, err = impl.packageDb.NewSession(session)
	if err != nil {
		goto failed
	}
	pack, err = packSession.Get(id)
	if err != nil {
		goto failed
	}
	if pack == nil {
		err = errors.New("package not find")
		goto failed
	}
	if method == "" {
		method = "all"
	}
	audit = common.MakeAuditInfo(impl.reqCtx, common.ScopeInfo{
		ProjectId: pack.DiceProjectId,
		Workspace: pack.DiceEnv,
	}, apistructs.CreateRouteTemplate, nil, map[string]interface{}{
		"endpoint": pack.PackageName,
		"method":   method,
		"path":     dto.ApiPath,
	})
	dto.ProjectId = pack.DiceProjectId
	dto.Env = pack.DiceEnv
	if pack.Scene == orm.UNITY_SCENE {
		defaultPath, err := (*impl.globalBiz).GenerateDefaultPath(pack.DiceProjectId)
		if err != nil {
			goto failed
		}
		if !strings.HasPrefix(dto.ApiPath, defaultPath) {
			dto.ApiPath = defaultPath + dto.ApiPath
		}
	}
	// get package domain
	domains, err = (*impl.domainBiz).GetPackageDomains(id, session)
	if err != nil {
		goto failed
	}
	if len(domains) == 0 {
		err = errors.New("you need set the endpoint's domain first")
		goto failed
	}
	dto.Hosts = domains
	log.Debugf("api dto before adjust %+v", dto) // output for debug
	err = dto.Adjust()
	if err != nil {
		goto failed
	}
	log.Debugf("api dto after adjust %+v", dto) // output for debug
	dao = impl.packageApiDao(dto)
	dao.PackageId = id
	apiSession, err = impl.packageApiDb.NewSession(session)
	if err != nil {
		goto failed
	}
	unique, err = apiSession.CheckUnique(dao)
	if err != nil {
		goto failed
	}
	if !unique {
		err = errors.Errorf("package api already exist, dao:%+v", dao)
		exist = true
		goto failed
	}
	if dto.RedirectType == gw.RT_URL {
		var kongSession db.GatewayKongInfoService
		kongSession, err = impl.kongDb.NewSession(session)
		if err != nil {
			goto failed
		}
		err = apiSession.Insert(dao)
		if err != nil {
			goto failed
		}
		kongInfo, err = kongSession.GetKongInfo(&orm.GatewayKongInfo{
			Az:        pack.DiceClusterName,
			ProjectId: pack.DiceProjectId,
			Env:       pack.DiceEnv,
		})
		if err != nil {
			goto failed
		}
		kongAdapter = kong.NewKongAdapter(kongInfo.KongAddr)
		serviceId, err = impl.touchKongService(kongAdapter, dto, dao.Id, session)
		if err != nil {
			goto failed
		}
		dto.ServiceId = serviceId
		routeId, err = impl.touchKongRoute(kongAdapter, dto, dao.Id, session)
		if err != nil {
			goto failed
		}
		dto.RouteId = routeId
		err = impl.createOrUpdatePlugins(kongAdapter, dto)
		if err != nil {
			goto failed
		}
	} else if dto.RedirectType == gw.RT_SERVICE {
		var runtimeSession db.GatewayRuntimeServiceService
		runtimeSession, err = impl.runtimeDb.NewSession(session)
		if err != nil {
			goto failed
		}
		var runtimeService *orm.GatewayRuntimeService
		if dto.RuntimeServiceId == "" {
			runtimeService, err = runtimeSession.GetByAny(&orm.GatewayRuntimeService{
				AppName:     dto.RedirectApp,
				ServiceName: dto.RedirectService,
				RuntimeId:   dto.RedirectRuntimeId,
			})
		} else {
			runtimeService, err = runtimeSession.Get(dto.RuntimeServiceId)
		}
		if err != nil {
			goto failed
		}
		if runtimeService == nil {
			err = errors.New("find runtime service failed")
			goto failed
		}
		dao.RuntimeServiceId = runtimeService.Id
		err = apiSession.Insert(dao)
		if err != nil {
			goto failed
		}
	}
	zoneId, err = impl.TouchPackageApiZone(endpoint_api.PackageApiInfo{dao, domains, pack.DiceProjectId, pack.DiceEnv, pack.DiceClusterName, injectRuntimeDomain}, session)
	if err != nil {
		goto failed
	}
	if dao.ZoneId == "" {
		dao.ZoneId = zoneId
		err = apiSession.Update(dao)
		if err != nil {
			goto failed
		}
	}
	err = apiSession.Update(dao)
	if err != nil {
		goto failed
	}
	if dto.AllowPassAuth {
		var authRule, aclRule *gw.OpenapiRule
		diceInfo := gw.DiceInfo{
			ProjectId: pack.DiceProjectId,
			Env:       pack.DiceEnv,
			Az:        pack.DiceClusterName,
		}
		aclRule, err = impl.createApiAclRule(gw.ACL_OFF, dao.PackageId, dao.Id)
		if err != nil {
			goto failed
		}
		authRule, err = impl.createApiAuthRule(dao.PackageId, dao.Id, false)
		if err != nil {
			goto failed
		}
		err = (*impl.ruleBiz).CreateRule(diceInfo, aclRule, session)
		if err != nil {
			goto failed
		}
		err = (*impl.ruleBiz).CreateRule(diceInfo, authRule, session)
		if err != nil {
			goto failed
		}
		needUpdateDomainPolicy = true
	}
	if needUpdateDomainPolicy {
		err = (*impl.ruleBiz).SetPackageKongPolicies(pack, session)
		if err != nil {
			goto failed
		}
	}
	return false, dao.Id, audit, nil
failed:
	if serviceId != "" {
		_ = kongAdapter.DeleteService(serviceId)
	}
	if routeId != "" {
		_ = kongAdapter.DeleteRoute(routeId)
	}
	if zoneId != "" {
		zerr := (*impl.zoneBiz).DeleteZoneRoute(zoneId, session)
		if zerr != nil {
			log.Errorf("delete zone route failed, err:%+v", zerr)
		}
	}
	return exist, "", audit, err
}

func (impl GatewayOpenapiServiceImpl) TouchPackageApiZone(info endpoint_api.PackageApiInfo, session ...*db.SessionHelper) (string, error) {
	var runtimeSession db.GatewayRuntimeServiceService
	var err error
	if len(session) > 0 {
		runtimeSession, err = impl.runtimeDb.NewSession(session[0])
		if err != nil {
			return "", err
		}
	} else {
		runtimeSession = impl.runtimeDb
	}
	az, err := impl.azDb.GetAzInfoByClusterName(info.Az)
	if err != nil {
		return "", err
	}
	// set route config
	routeConfig := zone.RouteConfig{
		Hosts: info.Hosts,
		Path:  info.ApiPath,
	}
	routeConfig.InjectRuntimeDomain = info.InjectRuntimeDomain
	if info.RedirectType == gw.RT_SERVICE {
		runtimeService, err := runtimeSession.Get(info.RuntimeServiceId)
		if err != nil {
			return "", err
		}
		if runtimeService == nil {
			return "", errors.Errorf("runtime service not found, err:%+v", err)
		}
		if runtimeService.IsSecurity == 1 {
			routeConfig.BackendProtocol = &[]k8s.BackendProtocl{k8s.HTTPS}[0]
		}
		_, innerHost, err := (*impl.globalBiz).GenerateEndpoint(gw.DiceInfo{
			Az:        info.Az,
			ProjectId: info.ProjectId,
			Env:       info.Env,
		})
		if err != nil {
			return "", err
		}
		routeConfig.UseRegex = true
		routeConfig.RewriteHost = &innerHost
		rewritePath, err := (*impl.globalBiz).GetRuntimeServicePrefix(runtimeService)
		if err != nil {
			return "", err
		}
		// avoid dup slash in redirect path
		if strings.HasSuffix(info.RedirectPath, "/") {
			if routeConfig.Path != "/" {
				routeConfig.Path = strings.TrimSuffix(routeConfig.Path, "/")
				routeConfig.Path += "(/|$)(.*)"
				rewritePath += info.RedirectPath + "$2"
			} else {
				routeConfig.Path += "(.*)"
				rewritePath += info.RedirectPath + "$1"
			}
		} else {
			routeConfig.Path += "(.*)"
			rewritePath += info.RedirectPath + "$1"
		}
		routeConfig.RewritePath = &rewritePath
	} else {
		routeConfig.BackendProtocol = &[]k8s.BackendProtocl{k8s.HTTPS}[0]
		routeConfig.UseRegex = true
		routeConfig.Path += ".*"
		routeConfig.Annotations = map[string]*string{}
		routeConfig.Annotations[k8s.REWRITE_HOST_KEY] = nil
		routeConfig.Annotations[k8s.REWRITE_PATH_KEY] = nil
	}
	if info.ZoneId == "" {
		//create zone
		z, err := (*impl.zoneBiz).CreateZoneWithoutIngress(zone.ZoneConfig{
			ZoneRoute: &zone.ZoneRoute{routeConfig},
			Name:      "api-" + info.Id,
			ProjectId: info.ProjectId,
			Env:       info.Env,
			Az:        info.Az,
			Type:      db.ZONE_TYPE_PACKAGE_API,
		}, session...)
		if err != nil {
			return "", err
		}
		transSucc := false
		annotations, locationSnippet, retSession, err := (*impl.policyBiz).SetZoneDefaultPolicyConfig(info.PackageId, z, az, session...)
		if len(session) == 0 {
			defer func() {
				if transSucc {
					_ = retSession.Commit()
				} else {
					_ = retSession.Rollback()
				}
				retSession.Close()
			}()
		}
		if err != nil {
			return "", err
		}
		routeConfig.Annotations = annotations
		routeConfig.LocationSnippet = locationSnippet
		_, err = (*impl.zoneBiz).UpdateZoneRoute(z.Id, zone.ZoneRoute{routeConfig}, session...)
		if err != nil {
			return "", err
		}
		transSucc = true
		return z.Id, nil
	}
	//update zone route
	exist, err := (*impl.zoneBiz).UpdateZoneRoute(info.ZoneId, zone.ZoneRoute{routeConfig})
	if err != nil {
		return "", err
	}
	if !exist {
		z, err := (*impl.zoneBiz).GetZone(info.ZoneId, session...)
		if err != nil {
			return "", err
		}
		err = (*impl.policyBiz).RefreshZoneIngress(*z, *az)
		if err != nil {
			return "", err
		}
	}
	return info.ZoneId, nil
}

func (impl GatewayOpenapiServiceImpl) CreatePackageApi(id string, dto *gw.OpenapiDto) (apiId string, exist bool, err error) {
	var helper *db.SessionHelper
	defer func() {
		if err != nil {
			if helper != nil {
				_ = helper.Rollback()
				helper.Close()
			}
			log.Errorf("errorHappened, err:%+v", err)
		}
	}()
	if id == "" || (dto.ApiPath == "" && dto.Method == "") {
		err = errors.New("invalid request")
		return
	}
	var audit *apistructs.Audit
	defer func() {
		if audit != nil && err == nil {
			berr := bundle.Bundle.CreateAuditEvent(&apistructs.AuditCreateRequest{Audit: *audit})
			if berr != nil {
				log.Errorf("create audit failed, err:%+v", err)
			}
		}
	}()
	helper, err = db.NewSessionHelper()
	if err != nil {
		return
	}
	if exist, apiId, audit, err = impl.SessionCreatePackageApi(id, dto, helper, false); err != nil {
		return
	}
	_ = helper.Commit()
	helper.Close()
	return
}

func (impl GatewayOpenapiServiceImpl) openapiDto(dao *orm.GatewayPackageApi) *gw.OpenapiInfoDto {
	dto := &gw.OpenapiInfoDto{
		ApiId:       dao.Id,
		CreateAt:    dao.CreateTime.Format("2006-01-02T15:04:05"),
		DiceApp:     dao.DiceApp,
		DiceService: dao.DiceService,
		Origin:      gw.Origin(dao.Origin),
		OpenapiDto: gw.OpenapiDto{
			ApiPath:      dao.ApiPath,
			RedirectType: dao.RedirectType,
			Method:       dao.Method,
			Description:  dao.Description,
		},
	}
	if dao.AclType == gw.ACL_OFF {
		dto.AllowPassAuth = true
	}
	if dto.Origin == gw.FROM_DICE || dto.Origin == gw.FROM_SHADOW {
		dto.Mutable = false
	} else {
		dto.Mutable = true
	}
	if dao.RuntimeServiceId != "" {
		dto.RedirectPath = dao.RedirectPath
		runtimeService, err := impl.runtimeDb.Get(dao.RuntimeServiceId)
		if err != nil || runtimeService == nil {
			log.Errorf("get runtimeservice failed, err:%+v", err)
			return dto
		}
		dto.RedirectApp = runtimeService.AppName
		dto.RedirectService = runtimeService.ServiceName
		dto.RedirectRuntimeId = runtimeService.RuntimeId
		dto.RedirectRuntimeName = runtimeService.RuntimeName
		return dto
	}
	if dao.RedirectAddr != "" {
		dto.RedirectAddr = dao.RedirectAddr
		scheme_find := strings.Index(dto.RedirectAddr, "://")
		if scheme_find == -1 {
			log.Errorf("invalide RedirectAddr %s", dto.RedirectAddr)
			return dto
		}
		dto.RedirectPath = "/"
		slash_find := strings.Index(dto.RedirectAddr[scheme_find+3:], "/")
		if slash_find != -1 {
			dto.RedirectPath = dto.RedirectAddr[slash_find+scheme_find+3:]
			dto.RedirectAddr = dto.RedirectAddr[:slash_find+scheme_find+3]
		}
	}
	return dto
}

func (impl GatewayOpenapiServiceImpl) deleteApiPassAuthRules(apiId string) error {
	var rules []gw.OpenapiRuleInfo
	rules, err := (*impl.ruleBiz).GetApiRules(apiId, gw.AUTH_RULE)
	if err != nil {
		return err
	}
	if len(rules) > 0 {
		err = (*impl.ruleBiz).DeleteRule(rules[0].Id, nil)
		if err != nil {

			return err
		}
	}
	rules, err = (*impl.ruleBiz).GetApiRules(apiId, gw.ACL_RULE)
	if err != nil {
		return err
	}
	if len(rules) > 0 {
		err = (*impl.ruleBiz).DeleteRule(rules[0].Id, nil)
		if err != nil {
			return err
		}
	}
	return nil
}

func (impl GatewayOpenapiServiceImpl) createApiPassAuthRules(pack *orm.GatewayPackage, apiId string) error {
	var authRule, aclRule *gw.OpenapiRule
	diceInfo := gw.DiceInfo{
		ProjectId: pack.DiceProjectId,
		Env:       pack.DiceEnv,
		Az:        pack.DiceClusterName,
	}
	aclRule, err := impl.createApiAclRule(gw.ACL_OFF, pack.Id, apiId)
	if err != nil {
		return err
	}
	authRule, err = impl.createApiAuthRule(pack.Id, apiId, false)
	if err != nil {
		return err
	}
	err = (*impl.ruleBiz).CreateRule(diceInfo, aclRule, nil)
	if err != nil {
		return err
	}
	err = (*impl.ruleBiz).CreateRule(diceInfo, authRule, nil)
	if err != nil {
		return err
	}
	return nil
}

func (impl GatewayOpenapiServiceImpl) GetPackageApis(id string, args *gw.GetOpenapiDto) (result common.NewPageQuery, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
		}
	}()
	pageInfo := common.NewPage2(args.PageSize, args.PageNo)
	options := args.GenSelectOptions()
	options = append(options, orm.SelectOption{
		Type:   orm.ExactMatch,
		Column: "package_id",
		Value:  id,
	})
	var list []gw.OpenapiInfoDto
	var daos []orm.GatewayPackageApi
	var ok bool
	page, err := impl.packageApiDb.GetPage(options, (*common.Page)(pageInfo))
	if err != nil {
		return
	}
	daos, ok = page.Result.([]orm.GatewayPackageApi)
	if !ok {
		err = errors.New("type convert failed")
		return
	}
	for _, dao := range daos {
		dto := impl.openapiDto(&dao)
		list = append(list, *dto)
	}
	result = common.NewPages(list, pageInfo.TotalNum)
	return
}

func (impl GatewayOpenapiServiceImpl) ListPackageAllApis(id string) ([]orm.GatewayPackageApi, error) {
	return impl.packageApiDb.SelectByAny(&orm.GatewayPackageApi{PackageId: id})
}

func (impl GatewayOpenapiServiceImpl) UpdatePackageApi(packageId, apiId string, dto *gw.OpenapiDto) (result *gw.OpenapiInfoDto, exist bool, err error) {
	var dao, updateDao *orm.GatewayPackageApi
	var pack *orm.GatewayPackage
	var kongInfo *orm.GatewayKongInfo
	var kongAdapter kong.KongAdapter
	var unique bool
	var serviceId, routeId string
	var domains []string
	needUpdateDomainPolicy := false
	auditCtx := map[string]interface{}{}
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
		}
		if pack == nil {
			return
		}
		method := dto.Method
		if method == "" {
			method = "all"
		}
		auditCtx["endpoint"] = pack.PackageName
		auditCtx["path"] = dto.ApiPath
		auditCtx["method"] = method
		audit := common.MakeAuditInfo(impl.reqCtx, common.ScopeInfo{
			ProjectId: pack.DiceProjectId,
			Workspace: pack.DiceEnv,
		}, apistructs.UpdateRouteTemplate, err, auditCtx)
		if audit != nil {
			err = bundle.Bundle.CreateAuditEvent(&apistructs.AuditCreateRequest{Audit: *audit})
			if err != nil {
				log.Errorf("create audit failed, err:%+v", err)
			}
		}
	}()
	if packageId == "" || apiId == "" {
		err = errors.New("packageId or apiId is empty")
		return
	}
	if valid, msg := dto.CheckValid(); !valid {
		err = errors.New(msg)
		return
	}
	// get package domain
	domains, err = (*impl.domainBiz).GetPackageDomains(packageId)
	if err != nil {
		return
	}
	dto.Hosts = domains
	err = dto.Adjust()
	if err != nil {
		return
	}
	dao, err = impl.packageApiDb.Get(apiId)
	if err != nil {
		return
	}
	if dao == nil {
		err = errors.New("api not exist")
		return
	}
	pack, err = impl.packageDb.Get(packageId)
	if err != nil {
		return
	}
	if pack == nil {
		err = errors.New("can't find endpoint")
		return
	}
	auditCtx["endpoint"] = pack.PackageName
	if pack.Scene == orm.UNITY_SCENE {
		var defaultPath string
		defaultPath, err = (*impl.globalBiz).GenerateDefaultPath(pack.DiceProjectId)
		if err != nil {
			return
		}
		if !strings.HasPrefix(dto.ApiPath, defaultPath) {
			dto.ApiPath = defaultPath + dto.ApiPath
		}
	}
	kongInfo, err = impl.kongDb.GetKongInfo(&orm.GatewayKongInfo{
		Az:        pack.DiceClusterName,
		ProjectId: pack.DiceProjectId,
		Env:       pack.DiceEnv,
	})
	if err != nil {
		return
	}
	kongAdapter = kong.NewKongAdapter(kongInfo.KongAddr)
	updateDao = impl.packageApiDao(dto)
	updateDao.ZoneId = dao.ZoneId
	//mutable check
	if dao.Origin == string(gw.FROM_DICE) || dao.Origin == string(gw.FROM_SHADOW) {
		if dto.RedirectAddr != dao.RedirectAddr {
			err = errors.Errorf("redirectaddr not same, old:%s, new:%s", dao.RedirectAddr,
				dto.RedirectAddr)
			return
		}
		if dto.Method != dao.Method {
			err = errors.Errorf("method not same, old:%s, new:%s", dao.Method,
				dto.Method)
			return
		}
	} else if dao.Origin == string(gw.FROM_CUSTOM) || dao.Origin == string(gw.FROM_DICEYML) {
		var zoneId string
		updateDao.PackageId = packageId
		updateDao.Id = apiId
		unique, err = impl.packageApiDb.CheckUnique(updateDao)
		if err != nil {
			return
		}
		if !unique {
			err = errors.Errorf("package api already exist, dao:%+v", updateDao)
			exist = true
			return
		}
		dto.ProjectId = pack.DiceProjectId
		dto.Env = pack.DiceEnv
		if updateDao.RedirectType == gw.RT_URL {
			updateDao.RuntimeServiceId = ""
			updateDao.DiceApp = ""
			updateDao.DiceService = ""
			if dto.RedirectAddr != dao.RedirectAddr || dto.ApiPath != dao.ApiPath || dto.Method != dao.Method {
				serviceId, err = impl.touchKongService(kongAdapter, dto, apiId)
				if err != nil {
					return
				}
				dto.ServiceId = serviceId
				routeId, err = impl.touchKongRoute(kongAdapter, dto, apiId)
				if err != nil {
					return
				}
				dto.RouteId = routeId
			} else {
				var route *orm.GatewayRoute
				route, err = impl.routeDb.GetByApiId(apiId)
				if err != nil {
					return
				}
				if route == nil {
					err = errors.Errorf("route of api:%s not exist", apiId)
					return
				}
				dto.RouteId = route.RouteId
			}
			err = impl.createOrUpdatePlugins(kongAdapter, dto)
			if err != nil {
				return
			}
		} else if updateDao.RedirectType == gw.RT_SERVICE {
			var runtimeService *orm.GatewayRuntimeService
			if dao.RedirectType == gw.RT_URL {
				err = impl.deleteKongApi(kongAdapter, apiId)
				if err != nil {
					return
				}
			}
			updateDao.RedirectAddr = ""
			runtimeSession := impl.runtimeDb
			if dto.RuntimeServiceId == "" {
				runtimeService, err = runtimeSession.GetByAny(&orm.GatewayRuntimeService{
					AppName:     dto.RedirectApp,
					ServiceName: dto.RedirectService,
					RuntimeId:   dto.RedirectRuntimeId,
				})
			} else {
				runtimeService, err = runtimeSession.Get(dto.RuntimeServiceId)
			}
			if err != nil {
				return
			}
			if runtimeService == nil {
				err = errors.New("find runtime service failed")
				return
			}
			updateDao.RuntimeServiceId = runtimeService.Id
		}
		zoneId, err = impl.TouchPackageApiZone(endpoint_api.PackageApiInfo{updateDao, domains, pack.DiceProjectId, pack.DiceEnv, pack.DiceClusterName, false})
		if err != nil {
			return
		}
		updateDao.ZoneId = zoneId
	}
	err = impl.packageApiDb.Update(updateDao)
	if err != nil {
		return
	}
	if dao.AclType != updateDao.AclType {
		needUpdateDomainPolicy = true
		if updateDao.AclType == gw.ACL_NONE {
			err = impl.deleteApiPassAuthRules(apiId)
			if err != nil {
				return
			}
		} else if updateDao.AclType == gw.ACL_OFF {
			err = impl.createApiPassAuthRules(pack, apiId)
			if err != nil {
				return
			}
		}
	} else if dao.RedirectType != updateDao.RedirectType {
		needUpdateDomainPolicy = true
		if updateDao.AclType == gw.ACL_OFF {
			err = impl.deleteApiPassAuthRules(apiId)
			if err != nil {
				return
			}
			err = impl.createApiPassAuthRules(pack, apiId)
			if err != nil {
				return
			}
		}
	}
	if needUpdateDomainPolicy {
		err = (*impl.ruleBiz).SetPackageKongPolicies(pack, nil)
		if err != nil {
			return
		}
	}
	result = impl.openapiDto(dao)
	return
}

func (impl *GatewayOpenapiServiceImpl) DeletePackageApi(packageId, apiId string) (result bool, err error) {
	var pack *orm.GatewayPackage
	var dao *orm.GatewayPackageApi
	var kongInfo *orm.GatewayKongInfo
	var kongAdapter kong.KongAdapter
	auditCtx := map[string]interface{}{}
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
		}
		if pack == nil || dao == nil {
			return
		}
		method := dao.Method
		if method == "" {
			method = "all"
		}
		auditCtx["endpoint"] = pack.PackageName
		auditCtx["path"] = dao.ApiPath
		auditCtx["method"] = method
		audit := common.MakeAuditInfo(impl.reqCtx, common.ScopeInfo{
			ProjectId: pack.DiceProjectId,
			Workspace: pack.DiceEnv,
		}, apistructs.DeleteRouteTemplate, err, auditCtx)
		if audit != nil {
			err = bundle.Bundle.CreateAuditEvent(&apistructs.AuditCreateRequest{Audit: *audit})
			if err != nil {
				log.Errorf("create audit failed, err:%+v", err)
			}
		}
	}()
	dao, err = impl.packageApiDb.Get(apiId)
	if err != nil {
		return
	}
	if dao == nil {
		result = true
		return
	}
	pack, err = impl.packageDb.Get(packageId)
	if err != nil {
		return
	}
	kongInfo, err = impl.kongDb.GetKongInfo(&orm.GatewayKongInfo{
		Az:        pack.DiceClusterName,
		ProjectId: pack.DiceProjectId,
		Env:       pack.DiceEnv,
	})
	if err != nil {
		return
	}
	kongAdapter = kong.NewKongAdapter(kongInfo.KongAddr)
	err = (*impl.ruleBiz).DeleteByPackageApi(pack, dao)
	if err != nil {
		return
	}
	if dao.Origin == string(gw.FROM_DICE) || dao.Origin == string(gw.FROM_SHADOW) {
		diceApiId := dao.DiceApiId
		var api *orm.GatewayApi
		var upApi *orm.GatewayUpstreamApi
		if diceApiId == "" {
			err = errors.Errorf("dice api id is empty, package api id:%s", apiId)
			return
		}
		err = impl.apiInPackageDb.Delete(packageId, diceApiId)
		if err != nil {
			return
		}
		api, err = impl.apiDb.GetById(diceApiId)
		if err != nil {
			return
		}
		if api != nil {
			if api.UpstreamApiId != "" {
				upApi, err = impl.upstreamApiDb.GetById(api.UpstreamApiId)
				if err != nil {
					return
				}
				if upApi != nil {
					err = (*impl.apiBiz).DeleteUpstreamBindApi(upApi)
					if err != nil {
						return
					}
					err = impl.upstreamApiDb.DeleteById(api.UpstreamApiId)
					if err != nil {
						return
					}
				}
			} else {
				(*impl.apiBiz).DeleteApi(api.Id)
			}
		}

	} else if dao.Origin == string(gw.FROM_CUSTOM) || dao.Origin == string(gw.FROM_DICEYML) {
		err = impl.deleteKongApi(kongAdapter, apiId)
		if err != nil {
			return
		}
	}
	if dao.ZoneId != "" {
		err = (*impl.zoneBiz).DeleteZone(dao.ZoneId)
		if err != nil {
			return
		}
	}
	err = impl.packageApiDb.Delete(apiId)
	if err != nil {
		return
	}
	result = true
	return
}

func (impl GatewayOpenapiServiceImpl) TouchRuntimePackageMeta(endpoint *orm.GatewayRuntimeService, session *db.SessionHelper) (string, bool, error) {
	packSession, err := impl.packageDb.NewSession(session)
	if err != nil {
		return "", false, err
	}
	dao, err := packSession.GetByAny(&orm.GatewayPackage{
		RuntimeServiceId: endpoint.Id,
	})
	if err != nil {
		return "", false, err
	}
	if dao != nil {
		return dao.Id, false, nil
	}
	name := endpoint.AppName + "/" + endpoint.ServiceName + "/" + endpoint.RuntimeName
	z, err := (*impl.zoneBiz).CreateZone(zone.ZoneConfig{
		Name:      regexp.MustCompile(`([^0-9a-zA-z]+|_+|\\+)`).ReplaceAllString("package-"+name, "-"),
		ProjectId: endpoint.ProjectId,
		Env:       endpoint.Workspace,
		Az:        endpoint.ClusterName,
		Type:      db.ZONE_TYPE_PACKAGE_NEW,
	}, session)
	if err != nil {
		return "", false, err
	}
	pack := &orm.GatewayPackage{
		DiceProjectId:    endpoint.ProjectId,
		DiceEnv:          endpoint.Workspace,
		DiceClusterName:  endpoint.ClusterName,
		PackageName:      name,
		Scene:            orm.WEBAPI_SCENE,
		ZoneId:           z.Id,
		RuntimeServiceId: endpoint.Id,
	}
	unique, err := packSession.CheckUnique(pack)
	if err != nil {
		return "", false, err
	}
	if !unique {
		return "", false, errors.Errorf("package %s already exists", pack.PackageName)
	}
	err = packSession.Insert(pack)
	if err != nil {
		return "", false, err
	}
	return pack.Id, true, nil
}

func (impl GatewayOpenapiServiceImpl) RefreshRuntimePackage(packageId string, endpoint *orm.GatewayRuntimeService, session *db.SessionHelper) error {
	packSession, err := impl.packageDb.NewSession(session)
	if err != nil {
		return err
	}
	dao, err := packSession.Get(packageId)
	if err != nil {
		return err
	}
	if dao == nil {
		return nil
	}
	apiSession, err := impl.packageApiDb.NewSession(session)
	if err != nil {
		return err
	}
	apis, err := apiSession.SelectByAny(&orm.GatewayPackageApi{PackageId: packageId})
	if err != nil {
		return err
	}
	domains, err := (*impl.domainBiz).GetPackageDomains(packageId, session)
	if err != nil {
		return err
	}
	if len(domains) == 0 {
		return impl.TryClearRuntimePackage(endpoint, session, true)
	}
	wg := sync.WaitGroup{}
	for _, api := range apis {
		wg.Add(1)
		go func(apiRefer orm.GatewayPackageApi) {
			defer util.DoRecover()
			zoneId, aerr := impl.TouchPackageApiZone(endpoint_api.PackageApiInfo{&apiRefer, domains, dao.DiceProjectId, dao.DiceEnv, dao.DiceClusterName, false})
			if aerr != nil {
				err = aerr
			}
			if apiRefer.ZoneId == "" {
				apiRefer.ZoneId = zoneId
				aerr = apiSession.Update(&apiRefer)
				if aerr != nil {
					err = aerr
				}
			}
			wg.Done()
		}(api)
	}
	wg.Wait()
	if err != nil {
		return err
	}
	err = impl.updatePackageApiHost(dao, domains)
	if err != nil {
		return err
	}
	api, err := impl.packageApiDb.GetRawByAny(&orm.GatewayPackageApi{
		ApiPath:   "/",
		PackageId: packageId,
	})
	if err != nil {
		return err
	}
	if api == nil {
		_, _, _, err = impl.SessionCreatePackageApi(packageId, &gw.OpenapiDto{
			ApiPath:          "/",
			RedirectType:     gw.RT_SERVICE,
			RedirectPath:     "/",
			RedirectApp:      endpoint.AppName,
			RedirectService:  endpoint.ServiceName,
			RuntimeServiceId: endpoint.Id,
		}, session, true)
		if err != nil {
			return err
		}
	}

	err = (*impl.ruleBiz).SetPackageKongPolicies(dao, session)
	if err != nil {
		return err
	}
	return nil
}

func (impl GatewayOpenapiServiceImpl) CreateUnityPackageZone(packageId string, session *db.SessionHelper) (*orm.GatewayZone, error) {
	packSession, err := impl.packageDb.NewSession(session)
	if err != nil {
		return nil, err
	}
	pack, err := packSession.Get(packageId)
	if err != nil {
		return nil, err
	}
	diceInfo := gw.DiceInfo{
		ProjectId: pack.DiceProjectId,
		Az:        pack.DiceClusterName,
		Env:       pack.DiceEnv,
	}
	outerHost, rewriteHost, err := (*impl.globalBiz).GenerateEndpoint(diceInfo)
	if err != nil {
		return nil, err
	}
	innerHost := (*impl.globalBiz).GetServiceAddr(diceInfo.Env)
	path, err := (*impl.globalBiz).GenerateDefaultPath(diceInfo.ProjectId)
	if err != nil {
		return nil, err
	}
	route := zone.RouteConfig{
		Hosts: []string{outerHost, innerHost},
		Path:  path + "/.*",
		RouteOptions: k8s.RouteOptions{
			BackendProtocol: &[]k8s.BackendProtocl{k8s.HTTPS}[0],
		},
	}
	route.RewriteHost = &rewriteHost
	route.UseRegex = true
	z, err := (*impl.zoneBiz).CreateZone(zone.ZoneConfig{
		ZoneRoute: &zone.ZoneRoute{Route: route},
		Name:      "unity",
		ProjectId: diceInfo.ProjectId,
		Env:       diceInfo.Env,
		Az:        diceInfo.Az,
		Type:      db.ZONE_TYPE_UNITY,
	}, session)
	if err != nil {
		return nil, err
	}
	pack.ZoneId = z.Id
	err = packSession.Update(pack)
	if err != nil {
		return nil, err
	}
	return z, nil
}

func (impl GatewayOpenapiServiceImpl) CreateTenantPackage(tenantId string, session *db.SessionHelper) error {
	kongSession, err := impl.kongDb.NewSession(session)
	if err != nil {
		return err
	}
	kongInfo, err := kongSession.GetByAny(&orm.GatewayKongInfo{
		TenantId: tenantId,
	})
	if err != nil {
		return err
	}
	diceInfo := gw.DiceInfo{
		ProjectId: kongInfo.ProjectId,
		Az:        kongInfo.Az,
		Env:       kongInfo.Env,
	}
	outerHost, rewriteHost, err := (*impl.globalBiz).GenerateEndpoint(diceInfo, session)
	if err != nil {
		return err
	}
	innerHost := (*impl.globalBiz).GetServiceAddr(kongInfo.Env)
	path, err := (*impl.globalBiz).GenerateDefaultPath(kongInfo.ProjectId, session)
	if err != nil {
		return err
	}
	route := zone.RouteConfig{
		Hosts: []string{outerHost, innerHost},
		Path:  path + "/.*",
		RouteOptions: k8s.RouteOptions{
			BackendProtocol: &[]k8s.BackendProtocl{k8s.HTTPS}[0],
		},
	}
	route.RewriteHost = &rewriteHost
	route.UseRegex = true
	z, err := (*impl.zoneBiz).CreateZone(zone.ZoneConfig{
		ZoneRoute: &zone.ZoneRoute{Route: route},
		Name:      "unity",
		ProjectId: kongInfo.ProjectId,
		Env:       kongInfo.Env,
		Az:        kongInfo.Az,
		Type:      db.ZONE_TYPE_UNITY,
	}, session)
	if err != nil {
		return err
	}
	var orgId string
	var pack *orm.GatewayPackage
	packageSession, err := impl.packageDb.NewSession(session)
	if err != nil {
		goto clear_route
	}
	pack = &orm.GatewayPackage{
		DiceProjectId:   kongInfo.ProjectId,
		DiceEnv:         kongInfo.Env,
		DiceClusterName: kongInfo.Az,
		PackageName:     "unity",
		Scene:           orm.UNITY_SCENE,
		ZoneId:          z.Id,
	}
	err = packageSession.Insert(pack)
	if err != nil {
		goto clear_route
	}
	orgId, err = (*impl.globalBiz).GetOrgId(pack.DiceProjectId)
	if err != nil {
		goto clear_route
	}
	_, err = (*impl.domainBiz).TouchPackageDomain(orgId, pack.Id, kongInfo.Az, []string{outerHost}, session)
	if err != nil {
		goto clear_route
	}
	_, _, err = (*impl.policyBiz).SetZonePolicyConfig(z, "built-in", nil, session)
	if err != nil {
		goto clear_route
	}
	{
		var policyEngine apipolicy.PolicyEngine
		var corsConfig apipolicy.PolicyDto
		var configByte []byte
		var azInfo *orm.GatewayAzInfo
		azInfo, err = impl.azDb.GetAzInfoByClusterName(kongInfo.Az)
		if err != nil {
			goto clear_route
		}
		if azInfo == nil {
			err = errors.New("az not found")
			goto clear_route
		}
		policyEngine, err = apipolicy.GetPolicyEngine("cors")
		if err != nil {
			goto clear_route
		}
		corsConfig = policyEngine.CreateDefaultConfig(nil)
		corsConfig.SetEnable(true)
		configByte, err = json.Marshal(corsConfig)
		if err != nil {
			err = errors.WithStack(err)
			goto clear_route
		}
		//TODO
		_ = session.Commit()
		_, err = (*impl.policyBiz).SetPackageDefaultPolicyConfig("cors", pack.Id, azInfo, configByte)
		if err != nil {
			goto clear_route
		}
	}
	return nil
clear_route:
	delErr := (*impl.zoneBiz).DeleteZoneRoute(z.Id, session)
	if delErr != nil {
		log.Errorf("delete zone failed, err:%+v", delErr)
	}
	return err
}

func (impl GatewayOpenapiServiceImpl) TouchPackageRootApi(packageId string, reqDto *gw.OpenapiDto) (result bool, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened: %+v", err)
		}
	}()
	api, err := impl.packageApiDb.GetByAny(&orm.GatewayPackageApi{
		ApiPath:   "/",
		PackageId: packageId,
	})
	if err != nil {
		return
	}
	if !strings.HasPrefix(reqDto.RedirectAddr, "http://") && !strings.HasPrefix(reqDto.RedirectAddr, "https://") {
		reqDto.RedirectAddr = "http://" + reqDto.RedirectAddr
	}
	apiDto := &gw.OpenapiDto{
		ApiPath:      "/",
		RedirectType: gw.RT_URL,
		RedirectAddr: reqDto.RedirectAddr,
		RedirectPath: reqDto.RedirectPath,
	}
	if api == nil {
		_, _, err = impl.CreatePackageApi(packageId, apiDto)
		if err != nil {
			return
		}
		result = true
		return
	}
	_, _, err = impl.UpdatePackageApi(packageId, api.Id, apiDto)
	if err != nil {
		return
	}
	result = true
	return
}

func (impl GatewayOpenapiServiceImpl) endpointPadding(endpoint *diceyml.Endpoint, service *orm.GatewayRuntimeService) error {
	if strings.HasSuffix(endpoint.Domain, ".*") {
		azInfo, err := impl.azDb.GetAzInfoByClusterName(service.ClusterName)
		if err != nil {
			return err
		}
		if azInfo == nil {
			return errors.New("az not found")
		}
		endpoint.Domain = strings.Replace(endpoint.Domain, "*", azInfo.WildcardDomain, 1)
	}
	if endpoint.Path == "" {
		endpoint.Path = "/"
	}
	if endpoint.BackendPath == "" {
		endpoint.BackendPath = endpoint.Path
	}
	return nil
}

func (impl GatewayOpenapiServiceImpl) getPackageIdByDomain(domain, projectId, workspace string) (allowCreate bool, id string, err error) {
	var domains, allDomains []orm.GatewayDomain
	domains, err = (*impl.domainBiz).FindDomains(domain, projectId, workspace, orm.ExactMatch, orm.DT_PACKAGE)
	if err != nil {
		return
	}
	if len(domains) == 0 {
		allDomains, err = (*impl.domainBiz).FindDomains(domain, "", "", orm.ExactMatch)
		if err != nil {
			return
		}
		if len(allDomains) > 0 {
			return
		}
		allowCreate = true
		return
	}
	id = domains[0].PackageId
	return
}

func (impl GatewayOpenapiServiceImpl) createRuntimeEndpointPackage(domain string, service *orm.GatewayRuntimeService) (id string, err error) {
	packageName := domain
	dto, _, err := impl.CreatePackage(&gw.DiceArgsDto{
		ProjectId: service.ProjectId,
		Env:       service.Workspace,
	}, &gw.PackageDto{
		Name:       packageName,
		BindDomain: []string{domain},
		Scene:      gw.WEBAPI_SCENE,
	})
	if err != nil {
		err = errors.Errorf("create package failed, err:%v", err)
		return
	}
	id = dto.Id
	return
}

func (impl GatewayOpenapiServiceImpl) createRuntimeEndpointRoute(packageId string, endpoint diceyml.Endpoint, service *orm.GatewayRuntimeService) (id string, err error) {
	id, _, err = impl.CreatePackageApi(packageId, &gw.OpenapiDto{
		ApiPath:          endpoint.Path,
		RedirectPath:     endpoint.BackendPath,
		RedirectType:     gw.RT_SERVICE,
		RedirectApp:      service.AppName,
		RedirectService:  service.ServiceName,
		RuntimeServiceId: service.Id,
		Origin:           gw.FROM_DICEYML,
	})
	if err != nil {
		err = errors.Errorf("create package api failed, err:%v", err)
		return
	}
	return
}

func (impl GatewayOpenapiServiceImpl) updateRuntimeEndpointRoute(packageId, packageApiId string, endpoint diceyml.Endpoint, service *orm.GatewayRuntimeService) error {
	_, _, err := impl.UpdatePackageApi(packageId, packageApiId, &gw.OpenapiDto{
		ApiPath:          endpoint.Path,
		RedirectPath:     endpoint.BackendPath,
		RedirectType:     gw.RT_SERVICE,
		RedirectApp:      service.AppName,
		RedirectService:  service.ServiceName,
		RuntimeServiceId: service.Id,
		Origin:           gw.FROM_DICEYML,
	})
	if err != nil {
		return errors.Errorf("update package api failed, err:%v", err)
	}
	return nil
}

func (impl GatewayOpenapiServiceImpl) getPackageApiIdByRoute(packageId, path string, service *orm.GatewayRuntimeService) (allowChange bool, id string, err error) {
	api, err := impl.packageApiDb.GetByAny(&orm.GatewayPackageApi{
		ApiPath:   path,
		PackageId: packageId,
	})
	if err != nil {
		return
	}
	if api == nil {
		allowChange = true
		return
	}
	if api.RuntimeServiceId != service.Id {
		return
	}
	id = api.Id
	allowChange = true
	return
}

func (impl GatewayOpenapiServiceImpl) setRoutePolicy(category, packageId, packageApiId string, cf map[string]interface{}) error {
	engine, err := apipolicy.GetPolicyEngine(category)
	if err != nil {
		return err
	}
	config, err := engine.MergeDiceConfig(cf)
	if err != nil {
		return err
	}
	configByte, err := json.Marshal(config)
	if err != nil {
		return errors.Errorf("mashal failed, config:%v, err:%v",
			config, err)
	}
	_, err = (*impl.policyBiz).SetPolicyConfig(category, packageId, packageApiId, configByte)
	if err != nil {
		return err
	}
	return nil
}

func (impl GatewayOpenapiServiceImpl) clearRoutePolicy(category, packageId, packageApiId string) error {
	engine, err := apipolicy.GetPolicyEngine(category)
	if err != nil {
		return err
	}
	config := engine.CreateDefaultConfig(map[string]interface{}{})
	configByte, err := json.Marshal(config)
	if err != nil {
		return errors.Errorf("mashal failed, config:%v, err:%v",
			config, err)
	}
	_, err = (*impl.policyBiz).SetPolicyConfig(category, packageId, packageApiId, configByte)
	if err != nil {
		return err
	}
	return nil
}

func (impl GatewayOpenapiServiceImpl) setRuntimeEndpointRoutePolicy(packageId, packageApiId string, policies diceyml.EndpointPolicies) error {
	if policies.Cors != nil {
		err := impl.setRoutePolicy("cors", packageId, packageApiId, *policies.Cors)
		if err != nil {
			return err
		}
	} else {
		// clear
		err := impl.clearRoutePolicy("cors", packageId, packageApiId)
		if err != nil {
			return err
		}
	}
	if policies.RateLimit != nil {
		err := impl.setRoutePolicy("safety-server-guard", packageId, packageApiId, *policies.RateLimit)
		if err != nil {
			return err
		}
	} else {
		// clear
		err := impl.clearRoutePolicy("safety-server-guard", packageId, packageApiId)
		if err != nil {
			return err
		}
	}
	return nil
}

func (impl GatewayOpenapiServiceImpl) clearRuntimeEndpointRoute(endpoints []endpointInfo, service *orm.GatewayRuntimeService) error {
	apis, err := impl.packageApiDb.SelectByAny(&orm.GatewayPackageApi{
		Origin:           string(gw.FROM_DICEYML),
		RuntimeServiceId: service.Id,
	})
	if err != nil {
		return nil
	}
	checkPackageEmpty := map[string]bool{}
	for _, api := range apis {
		exist := false
		for _, endpoint := range endpoints {
			if api.Id == endpoint.apiId {
				exist = true
				break
			}
		}
		if !exist {
			_, err := impl.DeletePackageApi(api.PackageId, api.Id)
			if err != nil {
				return err
			}
			checkPackageEmpty[api.PackageId] = true
		}
	}
	for pack := range checkPackageEmpty {
		apis, err := impl.packageApiDb.SelectByAny(&orm.GatewayPackageApi{
			PackageId: pack,
		})
		if err != nil {
			return err
		}
		// empty package, clear it
		if len(apis) == 0 {
			err = impl.deletePackage(pack, nil)
			if err != nil {
				return err
			}
		}
	}
	return nil
}

type endpointInfo struct {
	diceyml.Endpoint
	apiId string
}

func (impl GatewayOpenapiServiceImpl) SetRuntimeEndpoint(info runtime_service.RuntimeEndpointInfo) error {
	// get org locale
	var locale string
	if orgDTO, ok := orgCache.GetOrgByProjectID(info.RuntimeService.ProjectId); ok {
		locale = orgDTO.Locale
	}

	var endpoints []endpointInfo
	for _, endpoint := range info.Endpoints {
		err := impl.endpointPadding(&endpoint, info.RuntimeService)
		if err != nil {
			return err
		}
		allowCreate, packageId, err := impl.getPackageIdByDomain(endpoint.Domain, info.RuntimeService.ProjectId, info.RuntimeService.Workspace)
		if err != nil {
			return err
		}
		if !allowCreate && packageId == "" {
			rawLog := fmt.Sprintf("app:%s service:%s endpoint create failed,  domain:%s already used by other one",
				info.RuntimeService.AppName, info.RuntimeService.ServiceName, endpoint.Domain)
			humanLog := i18n.Sprintf(locale, "FailedToBindServiceDomain.Occupied", info.RuntimeService.ServiceName, endpoint.Domain)
			log.Error(rawLog)
			go common.AsyncRuntimeError(info.RuntimeService.RuntimeId, humanLog, rawLog)
			continue
		}
		if allowCreate {
			packageId, err := impl.createRuntimeEndpointPackage(endpoint.Domain, info.RuntimeService)
			if err != nil {
				return err
			}
			packageApiId, err := impl.createRuntimeEndpointRoute(packageId, endpoint, info.RuntimeService)
			if err != nil {
				return err
			}
			err = impl.setRuntimeEndpointRoutePolicy(packageId, packageApiId, endpoint.Policies)
			if err != nil {
				return err
			}
			endpoints = append(endpoints, endpointInfo{Endpoint: endpoint, apiId: packageApiId})
			continue
		}
		if packageId != "" {
			allowChange, packageApiId, err := impl.getPackageApiIdByRoute(packageId, endpoint.Path, info.RuntimeService)
			if err != nil {
				return err
			}
			if !allowChange {
				rawLog := fmt.Sprintf("app:%s service:%s endpoint route create failed,  domain:%s path:%s already used by other one",
					info.RuntimeService.AppName, info.RuntimeService.ServiceName, endpoint.Domain, endpoint.Path)
				humanLog := i18n.Sprintf(locale, "FailedToBindServiceDomainRoute.Occupied", info.RuntimeService.ServiceName,
					endpoint.Domain, endpoint.Path)
				go common.AsyncRuntimeError(info.RuntimeService.RuntimeId, humanLog, rawLog)
				continue
			}
			if packageApiId != "" {
				err = impl.updateRuntimeEndpointRoute(packageId, packageApiId, endpoint, info.RuntimeService)
			} else {
				packageApiId, err = impl.createRuntimeEndpointRoute(packageId, endpoint, info.RuntimeService)
			}
			if err != nil {
				return err
			}
			err = impl.setRuntimeEndpointRoutePolicy(packageId, packageApiId, endpoint.Policies)
			if err != nil {
				return err
			}
			endpoints = append(endpoints, endpointInfo{Endpoint: endpoint, apiId: packageApiId})
		}
	}
	err := impl.clearRuntimeEndpointRoute(endpoints, info.RuntimeService)
	if err != nil {
		return err
	}
	return nil
}

func (impl GatewayOpenapiServiceImpl) ClearRuntimeRoute(id string) error {
	apis, err := impl.packageApiDb.SelectByAny(&orm.GatewayPackageApi{
		RuntimeServiceId: id,
	})
	if err != nil {
		return err
	}
	checkPackageEmpty := map[string]bool{}
	for _, api := range apis {
		_, err = impl.DeletePackageApi(api.PackageId, api.Id)
		if err != nil {
			return err
		}
		checkPackageEmpty[api.PackageId] = true
	}
	for pack := range checkPackageEmpty {
		apis, err := impl.packageApiDb.SelectByAny(&orm.GatewayPackageApi{
			PackageId: pack,
		})
		if err != nil {
			return err
		}
		// empty package, clear it
		if len(apis) == 0 {
			err = impl.deletePackage(pack, nil)
			if err != nil {
				return err
			}
		}
	}
	return nil
}
