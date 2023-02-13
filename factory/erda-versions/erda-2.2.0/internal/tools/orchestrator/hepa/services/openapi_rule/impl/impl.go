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
	"context"
	"encoding/json"
	"sort"
	"strings"
	"sync"

	"github.com/pkg/errors"
	log "github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common"
	gw "github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/exdto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong"
	kongDto "github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
	db "github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/service"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/domain"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/openapi_rule"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/zone"
)

type GatewayOpenapiRuleServiceImpl struct {
	packageDb    db.GatewayPackageService
	consumerDb   db.GatewayConsumerService
	ruleDb       db.GatewayPackageRuleService
	routeDb      db.GatewayRouteService
	azDb         db.GatewayAzInfoService
	kongDb       db.GatewayKongInfoService
	kongPolicyDb db.GatewayPolicyService
	packageApiDb db.GatewayPackageApiService
	zoneBiz      *zone.GatewayZoneService
	domainBiz    *domain.GatewayDomainService
	reqCtx       context.Context
}

var once sync.Once

func NewGatewayOpenapiRuleServiceImpl() error {
	once.Do(
		func() {
			consumerDb, _ := db.NewGatewayConsumerServiceImpl()
			packageDb, _ := db.NewGatewayPackageServiceImpl()
			kongPolicyDb, _ := db.NewGatewayPolicyServiceImpl()
			ruleDb, _ := db.NewGatewayPackageRuleServiceImpl()
			routeDb, _ := db.NewGatewayRouteServiceImpl()
			azDb, _ := db.NewGatewayAzInfoServiceImpl()
			kongDb, _ := db.NewGatewayKongInfoServiceImpl()
			packageApiDb, _ := db.NewGatewayPackageApiServiceImpl()
			openapi_rule.Service = &GatewayOpenapiRuleServiceImpl{
				consumerDb:   consumerDb,
				packageDb:    packageDb,
				ruleDb:       ruleDb,
				routeDb:      routeDb,
				azDb:         azDb,
				kongDb:       kongDb,
				packageApiDb: packageApiDb,
				kongPolicyDb: kongPolicyDb,
				zoneBiz:      &zone.Service,
				domainBiz:    &domain.Service,
			}
		})
	return nil
}

func (impl GatewayOpenapiRuleServiceImpl) Clone(ctx context.Context) openapi_rule.GatewayOpenapiRuleService {
	newService := impl
	newService.reqCtx = ctx
	return &newService
}

func (impl GatewayOpenapiRuleServiceImpl) checkLimit(dto *gw.OpenLimitRuleDto) error {
	dto.KongConfig = map[string]interface{}{}
	if dto.Limit.Day != nil {
		dto.KongConfig["day"] = dto.Limit.Day
	} else if dto.Limit.Hour != nil {
		dto.KongConfig["hour"] = dto.Limit.Hour
	} else if dto.Limit.Minute != nil {
		dto.KongConfig["minute"] = dto.Limit.Minute
	} else if dto.Limit.Second != nil {
		dto.KongConfig["second"] = dto.Limit.Second
	}
	return nil
}

func (impl GatewayOpenapiRuleServiceImpl) config2Limit(config []byte) exdto.LimitType {
	res := exdto.LimitType{}
	var configMap map[string]interface{}
	if len(config) != 0 {
		err := json.Unmarshal(config, &configMap)
		if err != nil {
			log.Errorf("json unmarshal failed, config:%s, err:%s", config, err)
		}
	}
	for key, value := range configMap {
		limit := int(value.(float64))
		switch key {
		case "day":
			res.Day = &limit
		case "hour":
			res.Hour = &limit
		case "minute":
			res.Minute = &limit
		case "second":
			res.Second = &limit
		}
		break
	}
	return res
}

func (impl GatewayOpenapiRuleServiceImpl) checkApi(dto *gw.OpenLimitRuleDto) error {
	if dto.Method == "" && dto.ApiPath == "" {
		return nil
	}
	cond := &orm.GatewayPackageApi{
		PackageId: dto.PackageId,
		ApiPath:   dto.ApiPath,
		Method:    dto.Method,
	}
	cond.SetMustCondCols("api_path", "method", "package_id")
	apis, err := impl.packageApiDb.SelectByAny(cond)
	if err != nil {
		return nil
	}
	if len(apis) > 1 {
		return errors.Errorf("more than one api match, cond:%+v, apis:%+v", cond, apis)
	}
	if len(apis) == 0 {
		return errors.Errorf("no api match, cond:%+v", cond)
	}
	dto.ApiId = apis[0].Id
	return nil
}

func (impl GatewayOpenapiRuleServiceImpl) openLimitRule(dto *gw.OpenLimitRuleDto) *gw.OpenapiRule {
	rule := &gw.OpenapiRule{
		PackageApiId: dto.ApiId,
		PackageId:    dto.PackageId,
		PluginName:   "rate-limiting",
		Category:     gw.LIMIT_RULE,
		Config:       dto.KongConfig,
		Enabled:      true,
		ConsumerId:   dto.ConsumerId,
	}
	if rule.PackageApiId == "" {
		rule.Region = gw.PACKAGE_RULE
	} else {
		rule.Region = gw.API_RULE
	}
	return rule
}

func (impl GatewayOpenapiRuleServiceImpl) SetPackageApiKongPolicies(packageApi *orm.GatewayPackageApi, session *db.SessionHelper) error {
	packageSession, err := impl.packageDb.NewSession(session)
	if err != nil {
		return err
	}
	pack, err := packageSession.Get(packageApi.PackageId)
	if err != nil {
		return err
	}
	priority, regexDomains, enables, disables, err := impl.getPackageDomainAndRules(pack, session)
	if err != nil {
		return err
	}
	needUpdate, err := impl.setPackageApiKongPolicies(priority, pack.PackageName, regexDomains, enables, disables, packageApi, nil)
	if err != nil {
		return err
	}
	if !needUpdate {
		return nil
	}
	err = (*impl.zoneBiz).UpdateKongDomainPolicy(pack.DiceClusterName, pack.DiceProjectId, pack.DiceEnv, session)
	if err != nil {
		return err
	}
	return nil
}

func (impl GatewayOpenapiRuleServiceImpl) replaceAclRule(enables []string, aclPluginId string) error {
	for i := 0; i < len(enables); i++ {
		pluginId := enables[i]
		rule, err := impl.ruleDb.GetByAny(&orm.GatewayPackageRule{
			PluginId: pluginId,
		})
		if err != nil {
			return err
		}
		if rule == nil {
			continue
		}
		if rule.Category == string(gw.ACL_RULE) {
			enables[i] = aclPluginId
			return nil
		}
	}
	return nil
}

func (impl GatewayOpenapiRuleServiceImpl) setPackageApiKongPolicies(basePriority int, packageName string, packageRegexDomains, packageEnables, packageDisables []string, packageApi *orm.GatewayPackageApi, helper *db.SessionHelper) (bool, error) {
	if packageApi.ZoneId == "" {
		return false, nil
	}
	policies, err := impl.kongPolicyDb.SelectByAny(&orm.GatewayPolicy{ZoneId: packageApi.ZoneId})
	if err != nil {
		return false, err
	}
	var enables, disables []string
	for _, policy := range policies {
		if policy.Enabled == 1 {
			enables = append(enables, policy.PluginId)
		} else {
			disables = append(disables, policy.PluginId)
		}
	}
	rules, err := impl.getApiRulesWithSession(packageApi.Id, helper)
	if err != nil {
		return false, err
	}
	var aclPluginId string
	for _, rule := range rules {
		if rule.PackageZoneNeed {
			continue
		}
		if rule.Enabled {
			if rule.Category == gw.ACL_RULE {
				aclPluginId = rule.PluginId
				continue
			}
			enables = append(enables, rule.PluginId)
		} else {
			disables = append(disables, rule.PluginId)
		}
	}
	if len(enables) == 0 && len(disables) == 0 && aclPluginId == "" {
		zoneInfo, err := (*impl.zoneBiz).GetZone(packageApi.ZoneId, helper)
		if err != nil {
			return false, err
		}
		if len(zoneInfo.KongPolicies) > 0 {
			// clear
			err = (*impl.zoneBiz).SetZoneKongPoliciesWithoutDomainPolicy(packageApi.ZoneId, nil, helper)
			if err != nil {
				return false, err
			}
			return true, nil
		}
		return false, nil
	}
	var fullEnables, fullDisables []string
	fullEnables = append(fullEnables, packageEnables...)
	fullEnables = append(fullEnables, enables...)
	fullDisables = append(fullDisables, packageDisables...)
	fullDisables = append(fullDisables, disables...)
	if aclPluginId != "" {
		err = impl.replaceAclRule(fullEnables, aclPluginId)
		if err != nil {
			return false, err
		}
	}
	var apiPath string
	if packageApi.RedirectType == gw.RT_SERVICE {
		apiPath = packageApi.RedirectPath
	} else {
		apiPath = packageApi.ApiPath
	}
	if packageApi.RuntimeServiceId != "" && packageApi.RedirectType == gw.RT_SERVICE {
		apiPath = "/" + packageApi.RuntimeServiceId + apiPath
	}
	var pathRegexs []string
	for _, regexDomain := range packageRegexDomains {
		pathRegexs = append(pathRegexs, regexDomain+apiPath)
	}
	domainPolicies := gw.ZoneKongPolicies{}
	domainPolicies.Enables = strings.Join(fullEnables, ",")
	domainPolicies.Disables = strings.Join(fullDisables, ",")
	domainPolicies.Id = packageApi.ZoneId
	domainPolicies.Regex = "^(" + strings.Join(pathRegexs, "|") + ")"
	domainPolicies.Priority = basePriority + len(apiPath)
	domainPolicies.PackageName = packageName
	err = (*impl.zoneBiz).SetZoneKongPoliciesWithoutDomainPolicy(packageApi.ZoneId, &domainPolicies, helper)
	if err != nil {
		return false, err
	}
	return true, nil
}

func (impl GatewayOpenapiRuleServiceImpl) disableAclRulesOnAliyunApp(rules []gw.OpenapiRuleInfo) []gw.OpenapiRuleInfo {
	authIndex := -1
	aclIndex := -1
	for i := 0; i < len(rules); i++ {
		if rules[i].PluginName == gw.ACL {
			aclIndex = i
			continue
		}
		if rules[i].PluginName == gw.AT_ALIYUN_APP {
			authIndex = i
			continue
		}
	}
	if authIndex != -1 && aclIndex != -1 {
		return append(rules[:aclIndex], rules[aclIndex+1:]...)
	}
	return rules
}

func (impl GatewayOpenapiRuleServiceImpl) getPackageDomainAndRules(pack *orm.GatewayPackage, helper *db.SessionHelper) (priority int, regexDomains, enables, disables []string,
	err error) {
	var rules []gw.OpenapiRuleInfo
	rules, err = impl.GetPackageRules(pack.Id, helper)
	if err != nil {
		return
	}
	// 在这里移除之后，单个API的acl也无法替换生效
	rules = impl.disableAclRulesOnAliyunApp(rules)
	sortRules := gw.SortByRuleList(rules)
	sort.Sort(sortRules)
	for _, rule := range sortRules {
		if rule.NotKongPlugin {
			continue
		}
		if rule.Enabled {
			enables = append(enables, rule.PluginId)
		} else {
			disables = append(disables, rule.PluginId)
		}
	}
	domains, err := (*impl.domainBiz).GetPackageDomains(pack.Id, helper)
	if err != nil {
		return
	}
	for _, domain := range domains {
		priority = gw.BASE_PRIORITY
		if strings.Contains(domain, `*`) {
			priority = gw.WILDCARD_DOMAIN_BASE_PRIORITY
		}
		regexDomain := strings.Replace(domain, `.`, `\.`, -1)
		regexDomain = strings.Replace(regexDomain, `*`, `.+`, -1)
		regexDomains = append(regexDomains, regexDomain)
	}
	return
}

func (impl GatewayOpenapiRuleServiceImpl) SetPackageKongPolicies(pack *orm.GatewayPackage, helper *db.SessionHelper) error {
	domainPolicies := gw.ZoneKongPolicies{}
	priority, regexDomains, enables, disables, err := impl.getPackageDomainAndRules(pack, helper)
	if err != nil {
		return err
	}

	policies, err := impl.kongPolicyDb.SelectByAny(&orm.GatewayPolicy{ZoneId: pack.ZoneId})
	if err != nil {
		return err
	}
	var fullEnables, fullDisables []string
	fullEnables = append(fullEnables, enables...)
	fullDisables = append(fullDisables, disables...)
	for _, policy := range policies {
		if policy.Enabled == 1 {
			fullEnables = append(fullEnables, policy.PluginId)
		} else {
			fullDisables = append(fullDisables, policy.PluginId)
		}
	}
	if len(fullEnables) > 0 || len(fullDisables) > 0 {
		domainPolicies.Enables = strings.Join(enables, ",")
		domainPolicies.Disables = strings.Join(disables, ",")
		domainPolicies.Id = pack.ZoneId
		domainPolicies.Regex = "^(" + strings.Join(regexDomains, "|") + ")"
		domainPolicies.Priority = priority
		domainPolicies.PackageName = pack.PackageName
		err = (*impl.zoneBiz).SetZoneKongPoliciesWithoutDomainPolicy(pack.ZoneId, &domainPolicies, helper)
		if err != nil {
			return err
		}
	}
	var apiSession db.GatewayPackageApiService
	if helper != nil {
		apiSession, err = impl.packageApiDb.NewSession(helper)
		if err != nil {
			return err
		}
	} else {
		apiSession = impl.packageApiDb
	}
	apis, err := apiSession.SelectByAny(&orm.GatewayPackageApi{
		PackageId: pack.Id,
	})
	if err != nil {
		return err
	}
	for _, api := range apis {
		_, err = impl.setPackageApiKongPolicies(priority, pack.PackageName, regexDomains, enables, disables, &api, helper)
		if err != nil {
			return err
		}
	}
	err = (*impl.zoneBiz).UpdateKongDomainPolicy(pack.DiceClusterName, pack.DiceProjectId, pack.DiceEnv, helper)
	if err != nil {
		return err
	}
	return nil
}

func (impl GatewayOpenapiRuleServiceImpl) CreateOrUpdateLimitRule(consumerId, packageId string, limits []exdto.LimitType) (err error) {
	pack, err := impl.packageDb.Get(packageId)
	if err != nil {
		return
	}
	if pack == nil {
		err = errors.New("invalid packageId")
		return
	}
	session, err := db.NewSessionHelper()
	if err != nil {
		return
	}
	ruleSession, err := impl.ruleDb.NewSession(session)
	if err != nil {
		return
	}
	defer func() {
		if err != nil {
			_ = session.Rollback()
		} else {
			_ = session.Commit()
		}
		session.Close()
	}()
	rules, err := ruleSession.SelectByAny(&orm.GatewayPackageRule{
		ConsumerId: consumerId,
		PackageId:  packageId,
		Category:   string(gw.LIMIT_RULE),
	})
	if err != nil {
		return
	}
	for _, rule := range rules {
		err = ruleSession.Delete(rule.Id)
		if err != nil {
			return
		}
	}
	for _, limit := range limits {
		limitDto := &gw.OpenLimitRuleDto{
			ConsumerId: consumerId,
			PackageId:  packageId,
			Limit:      limit,
		}
		err = impl.checkLimit(limitDto)
		if err != nil {
			return
		}
		err = impl.CreateRule(gw.DiceInfo{
			ProjectId: pack.DiceProjectId,
			Env:       pack.DiceEnv,
		}, impl.openLimitRule(limitDto), session)
		if err != nil {
			return
		}
	}
	err = impl.SetPackageKongPolicies(pack, session)
	if err != nil {
		return
	}
	return nil
}

func (impl GatewayOpenapiRuleServiceImpl) CreateLimitRule(args *gw.DiceArgsDto, dto *gw.OpenLimitRuleDto) (res bool, existed bool, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
		}
	}()
	if args.ProjectId == "" || args.Env == "" || dto.ConsumerId == "" || dto.PackageId == "" {
		err = errors.New("id is empty")
		return
	}
	var diceInfo gw.DiceInfo
	var az string
	var exist *orm.GatewayPackageRule
	var cond *orm.GatewayPackageRule
	pack, err := impl.packageDb.Get(dto.PackageId)
	if err != nil {
		return
	}
	if pack == nil {
		err = errors.New("package not find")
		return
	}
	az, err = impl.azDb.GetAz(&orm.GatewayAzInfo{
		Env:       args.Env,
		ProjectId: args.ProjectId,
	})
	if err != nil {
		return
	}
	diceInfo = gw.DiceInfo{
		Env:       args.Env,
		ProjectId: args.ProjectId,
		Az:        az,
	}
	err = impl.checkLimit(dto)
	if err != nil {
		return
	}
	err = impl.checkApi(dto)
	if err != nil {
		return
	}
	cond = &orm.GatewayPackageRule{
		ConsumerId: dto.ConsumerId,
		PackageId:  dto.PackageId,
		Category:   string(gw.LIMIT_RULE),
		ApiId:      dto.ApiId,
	}
	cond.SetMustCondCols("consumer_id", "package_id", "category", "api_id")
	exist, err = impl.ruleDb.GetByAny(cond)
	if err != nil {
		return
	}
	if exist != nil {
		err = errors.Errorf("rule already exist, cond:%+v, exist:%+v", cond, exist)
		existed = true
		return
	}
	err = impl.CreateRule(diceInfo, impl.openLimitRule(dto), nil)
	if err != nil {
		return
	}
	err = impl.SetPackageKongPolicies(pack, nil)
	if err != nil {
		return
	}
	res = true
	return
}

func (impl GatewayOpenapiRuleServiceImpl) limitRuleInfoDto(dao *orm.GatewayPackageRule) *gw.OpenLimitRuleInfoDto {
	var apiPath, method string
	if dao.ApiId != "" {
		api, _ := impl.packageApiDb.Get(dao.ApiId)
		if api != nil {
			apiPath = api.ApiPath
			method = api.Method
		}
	}
	return &gw.OpenLimitRuleInfoDto{
		OpenLimitRuleDto: gw.OpenLimitRuleDto{
			ConsumerId: dao.ConsumerId,
			PackageId:  dao.PackageId,
			Method:     method,
			ApiPath:    apiPath,
			Limit:      impl.config2Limit(dao.Config),
		},
		Id:           dao.Id,
		CreateAt:     dao.CreateTime.Format("2006-01-02T15:04:05"),
		ConsumerName: dao.ConsumerName,
		PackageName:  dao.PackageName,
	}
}

func (impl GatewayOpenapiRuleServiceImpl) UpdateLimitRule(ruleId string, dto *gw.OpenLimitRuleDto) (res *gw.OpenLimitRuleInfoDto, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
		}
	}()
	if dto.ConsumerId == "" || dto.PackageId == "" {
		err = errors.New("id is empty")
		return
	}
	var dao *orm.GatewayPackageRule
	err = impl.checkLimit(dto)
	if err != nil {
		return
	}
	err = impl.checkApi(dto)
	if err != nil {
		return
	}
	dao, err = impl.UpdateRule(ruleId, impl.openLimitRule(dto))
	if err != nil {
		return
	}
	res = impl.limitRuleInfoDto(dao)
	return
}
func (impl GatewayOpenapiRuleServiceImpl) GetLimitRules(args *gw.GetOpenLimitRulesDto) (res common.NewPageQuery, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
		}
	}()
	options := args.GenSelectOptions()
	options = append(options, orm.SelectOption{
		Type:   orm.ExactMatch,
		Column: "category",
		Value:  gw.LIMIT_RULE,
	})
	pageInfo := common.NewPage2(args.PageSize, args.PageNo)
	var list []gw.OpenLimitRuleInfoDto
	var daos []orm.GatewayPackageRule
	var ok bool
	page, err := impl.ruleDb.GetPage(options, (*common.Page)(pageInfo))
	if err != nil {
		return
	}
	daos, ok = page.Result.([]orm.GatewayPackageRule)
	if !ok {
		err = errors.New("type convert failed")
		return
	}
	for _, dao := range daos {
		dto := impl.limitRuleInfoDto(&dao)
		list = append(list, *dto)
	}
	res = common.NewPages(list, pageInfo.TotalNum)
	return
}

func (impl GatewayOpenapiRuleServiceImpl) DeleteLimitRule(id string) (res bool, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
		}
	}()
	if id == "" {
		err = errors.New("id is empty")
		return
	}
	var pack *orm.GatewayPackage
	rule, err := impl.ruleDb.Get(id)
	if err != nil {
		return
	}
	if rule == nil {
		err = errors.New("rule not find")
		return
	}
	pack, err = impl.packageDb.Get(rule.PackageId)
	if err != nil {
		return
	}
	if pack == nil {
		err = errors.New("package not find")
		return
	}
	err = impl.DeleteRule(id, nil)
	if err != nil {
		return
	}
	err = impl.SetPackageKongPolicies(pack, nil)
	if err != nil {
		return
	}
	res = true
	return
}

func (impl GatewayOpenapiRuleServiceImpl) packageRuleDao(diceInfo gw.DiceInfo, dto *gw.OpenapiRule, helper *db.SessionHelper) *orm.GatewayPackageRule {
	var packDbService db.GatewayPackageService
	var err error
	if helper != nil {
		packDbService, err = impl.packageDb.NewSession(helper)
		if err != nil {
			log.Errorf("get session failed, err:%+v", err)
			return nil
		}
	} else {
		packDbService = impl.packageDb
	}
	configStr, err := json.Marshal(dto.Config)
	if err != nil {
		log.Errorf("json marsharl failed, config:%s, err:%s", dto.Config, err)
		return nil
	}
	enabled := 0
	if dto.Enabled {
		enabled = 1
	}
	res := &orm.GatewayPackageRule{
		ApiId:           dto.PackageApiId,
		Category:        string(dto.Category),
		Config:          configStr,
		ConsumerId:      dto.ConsumerId,
		DiceEnv:         diceInfo.Env,
		DiceOrgId:       diceInfo.OrgId,
		DiceProjectId:   diceInfo.ProjectId,
		DiceClusterName: diceInfo.Az,
		Enabled:         enabled,
		PackageId:       dto.PackageId,
		PluginName:      dto.PluginName,
		PluginId:        dto.PluginId,
	}
	pack, err := packDbService.Get(dto.PackageId)
	if err != nil {
		log.Errorf("package not find, id:%s, err:%+v", dto.PackageId, err)
		return nil
	}
	if pack == nil {
		log.Errorf("package not find, id:%s, err:%+v", dto.PackageId, errors.New("not find"))
		return nil
	}
	res.PackageName = pack.PackageName
	if dto.ConsumerId != "" {
		consumer, err := impl.consumerDb.GetById(dto.ConsumerId)
		if err != nil {
			log.Errorf("consumer not find, id:%s, err:%+v", dto.ConsumerId, err)
			return nil
		}
		if consumer == nil {
			log.Errorf("consumer not find, id:%s, err:%+v", dto.ConsumerId, errors.New("not find"))
			return nil
		}
		res.ConsumerName = consumer.ConsumerName
	}
	if dto.PackageApiId != "" {
		res.PackageZoneNeed = 0
	} else {
		res.PackageZoneNeed = 1
	}
	return res
}

func (impl GatewayOpenapiRuleServiceImpl) kongPluginReq(dto *gw.OpenapiRule, helper *db.SessionHelper) (*kongDto.KongPluginReqDto, error) {
	var routeDb db.GatewayRouteService
	var apiDb db.GatewayPackageApiService
	var err error
	if helper != nil {
		routeDb, err = impl.routeDb.NewSession(helper)
		if err != nil {
			return nil, err
		}
		apiDb, err = impl.packageApiDb.NewSession(helper)
		if err != nil {
			return nil, err
		}
	} else {
		routeDb = impl.routeDb
		apiDb = impl.packageApiDb
	}
	disable := false
	reqDto := &kongDto.KongPluginReqDto{
		Name:    dto.PluginName,
		Enabled: &disable,
		Config:  dto.Config,
		Id:      dto.PluginId,
	}
	if dto.ConsumerId != "" {
		consumer, err := impl.consumerDb.GetById(dto.ConsumerId)
		if err != nil {
			log.Errorf("consumer not find, id:%s, err:%s", dto.ConsumerId, err)
			return nil, err
		}
		reqDto.ConsumerId = consumer.ConsumerId
	}
	if dto.Region == gw.API_RULE {
		apiId := dto.PackageApiId
		api, err := apiDb.Get(dto.PackageApiId)
		if err != nil {
			return nil, err
		}
		if api.DiceApiId != "" {
			apiId = api.DiceApiId
		}
		route, err := routeDb.GetByApiId(apiId)
		if err != nil {
			return nil, err
		}
		if route != nil {
			reqDto.RouteId = route.RouteId
		}

	}
	return reqDto, nil
}

func (impl GatewayOpenapiRuleServiceImpl) createOrUpdateKongPlugin(adapter kong.KongAdapter, dto *gw.OpenapiRule, helper *db.SessionHelper) (string, error) {
	req, err := impl.kongPluginReq(dto, helper)
	if err != nil {
		return "", err
	}
	resp, err := adapter.CreateOrUpdatePluginById(req)
	if err != nil {
		return "", err
	}
	return resp.Id, nil
}

func (impl GatewayOpenapiRuleServiceImpl) deleteKongPlugin(adapter kong.KongAdapter, pluginId string) error {
	return adapter.RemovePlugin(pluginId)
}

func (impl GatewayOpenapiRuleServiceImpl) CreateRule(diceInfo gw.DiceInfo, rule *gw.OpenapiRule, helper *db.SessionHelper) error {
	var ruleDbService db.GatewayPackageRuleService
	var err error
	if helper != nil {
		ruleDbService, err = impl.ruleDb.NewSession(helper)
		if err != nil {
			return err
		}
	} else {
		ruleDbService = impl.ruleDb
	}
	dao := impl.packageRuleDao(diceInfo, rule, helper)
	if dao == nil {
		return errors.Errorf("convert to dao failed, rule:%+v", rule)
	}
	if !rule.NotKongPlugin {
		az, err := impl.azDb.GetAz(&orm.GatewayAzInfo{
			Env:       diceInfo.Env,
			ProjectId: diceInfo.ProjectId,
		})
		if err != nil {
			return err
		}
		kongInfo, err := impl.kongDb.GetKongInfo(&orm.GatewayKongInfo{
			Az:        az,
			ProjectId: diceInfo.ProjectId,
			Env:       diceInfo.Env,
		})
		if err != nil {
			return err
		}
		kongAdapter := kong.NewKongAdapter(kongInfo.KongAddr)
		pluginId, err := impl.createOrUpdateKongPlugin(kongAdapter, rule, helper)
		if err != nil {
			return err
		}
		dao.PluginId = pluginId
		err = ruleDbService.Insert(dao)
		if err != nil {
			_ = impl.deleteKongPlugin(kongAdapter, pluginId)
			return err
		}
	} else {
		err = ruleDbService.Insert(dao)
		if err != nil {
			return err
		}
	}
	return nil
}

func (impl GatewayOpenapiRuleServiceImpl) UpdateRule(ruleId string, rule *gw.OpenapiRule) (*orm.GatewayPackageRule, error) {
	if ruleId == "" {
		return nil, errors.New("empty ruleId")
	}
	dao, err := impl.ruleDb.Get(ruleId)
	if err != nil {
		return nil, err
	}
	if dao == nil {
		return nil, errors.New("rule not find")
	}
	if rule.PackageId != dao.PackageId || rule.ConsumerId != dao.ConsumerId ||
		rule.PluginName != dao.PluginName {
		return nil, errors.New("field can't change when update rule")
	}
	rule.PluginId = dao.PluginId
	kongInfo, err := impl.kongDb.GetKongInfo(&orm.GatewayKongInfo{
		Az:        dao.DiceClusterName,
		ProjectId: dao.DiceProjectId,
		Env:       dao.DiceEnv,
	})
	if err != nil {
		return nil, err
	}
	kongAdapter := kong.NewKongAdapter(kongInfo.KongAddr)
	_, err = impl.createOrUpdateKongPlugin(kongAdapter, rule, nil)
	if err != nil {
		return nil, err
	}
	updateDao := impl.packageRuleDao(gw.DiceInfo{
		Az:        dao.DiceClusterName,
		Env:       dao.DiceEnv,
		ProjectId: dao.DiceProjectId,
		OrgId:     dao.DiceOrgId,
	}, rule, nil)
	updateDao.Id = dao.Id
	err = impl.ruleDb.Update(updateDao)
	if err != nil {
		return nil, err
	}
	return dao, nil
}

func (impl GatewayOpenapiRuleServiceImpl) DeleteRule(ruleId string, helper *db.SessionHelper) error {
	if ruleId == "" {
		return errors.New("empty ruleId")
	}
	var ruleDbService db.GatewayPackageRuleService
	var err error
	if helper != nil {
		ruleDbService, err = impl.ruleDb.NewSession(helper)
		if err != nil {
			return err
		}
	} else {
		ruleDbService = impl.ruleDb
	}
	dao, err := ruleDbService.Get(ruleId)
	if err != nil {
		return err
	}
	if dao == nil {
		return nil
	}
	kongInfo, err := impl.kongDb.GetKongInfo(&orm.GatewayKongInfo{
		Az:        dao.DiceClusterName,
		ProjectId: dao.DiceProjectId,
		Env:       dao.DiceEnv,
	})
	if err != nil {
		return err
	}
	if dao.PluginId != "" {
		kongAdapter := kong.NewKongAdapter(kongInfo.KongAddr)
		err = impl.deleteKongPlugin(kongAdapter, dao.PluginId)
		if err != nil {
			return err
		}
	}
	return ruleDbService.Delete(ruleId)
}

func (impl GatewayOpenapiRuleServiceImpl) openapiRuleInfoDto(dao *orm.GatewayPackageRule) (*gw.OpenapiRuleInfo, error) {
	config := map[string]interface{}{}
	if len(dao.Config) > 0 {
		err := json.Unmarshal(dao.Config, &config)
		if err != nil {
			return nil, errors.Errorf("json unmarshal failed, config:%s", dao.Config)
		}
	}
	enabled := false
	if dao.Enabled == 1 {
		enabled = true
	}
	packageZoneNeed := false
	if dao.PackageZoneNeed == 1 {
		packageZoneNeed = true
	}
	var region gw.RuleRegion
	if dao.ApiId != "" {
		region = gw.API_RULE
	} else {
		region = gw.PACKAGE_RULE
	}
	return &gw.OpenapiRuleInfo{
		Id: dao.Id,
		OpenapiRule: gw.OpenapiRule{
			Region:          region,
			PackageApiId:    dao.ApiId,
			PackageId:       dao.PackageId,
			PluginId:        dao.PluginId,
			PluginName:      dao.PluginName,
			Category:        gw.RuleCategory(dao.Category),
			Config:          config,
			Enabled:         enabled,
			PackageZoneNeed: packageZoneNeed,
			ConsumerId:      dao.ConsumerId,
		},
	}, nil
}

func (impl GatewayOpenapiRuleServiceImpl) GetPackageRules(packageId string, helper *db.SessionHelper, category ...gw.RuleCategory) ([]gw.OpenapiRuleInfo, error) {
	var ruleDbService db.GatewayPackageRuleService
	var err error
	if helper != nil {
		ruleDbService, err = impl.ruleDb.NewSession(helper)
		if err != nil {
			return nil, err
		}
	} else {
		ruleDbService = impl.ruleDb
	}
	cond := &orm.GatewayPackageRule{
		PackageId:       packageId,
		PackageZoneNeed: 1,
	}
	if len(category) > 0 {
		cond.Category = string(category[0])
	}
	daos, err := ruleDbService.SelectByAny(cond)
	if err != nil {
		return nil, err
	}
	var res []gw.OpenapiRuleInfo
	for _, dao := range daos {
		dto, err := impl.openapiRuleInfoDto(&dao)
		if err != nil {
			return nil, err
		}
		res = append(res, *dto)
	}
	return res, nil
}

func (impl GatewayOpenapiRuleServiceImpl) getApiRulesWithSession(apiId string, helper *db.SessionHelper, category ...gw.RuleCategory) ([]gw.OpenapiRuleInfo, error) {
	var ruleDbService db.GatewayPackageRuleService
	var err error
	if helper != nil {
		ruleDbService, err = impl.ruleDb.NewSession(helper)
		if err != nil {
			return nil, err
		}
	} else {
		ruleDbService = impl.ruleDb
	}
	cond := &orm.GatewayPackageRule{
		ApiId: apiId,
	}
	if len(category) > 0 {
		cond.Category = string(category[0])
	}
	daos, err := ruleDbService.SelectByAny(cond)
	if err != nil {
		return nil, err
	}
	var res []gw.OpenapiRuleInfo
	for _, dao := range daos {
		dto, err := impl.openapiRuleInfoDto(&dao)
		if err != nil {
			return nil, err
		}
		res = append(res, *dto)
	}
	return res, nil
}

func (impl GatewayOpenapiRuleServiceImpl) GetApiRules(apiId string, category ...gw.RuleCategory) ([]gw.OpenapiRuleInfo, error) {
	cond := &orm.GatewayPackageRule{
		ApiId: apiId,
	}
	if len(category) > 0 {
		cond.Category = string(category[0])
	}
	daos, err := impl.ruleDb.SelectByAny(cond)
	if err != nil {
		return nil, err
	}
	var res []gw.OpenapiRuleInfo
	for _, dao := range daos {
		dto, err := impl.openapiRuleInfoDto(&dao)
		if err != nil {
			return nil, err
		}
		res = append(res, *dto)
	}
	return res, nil
}

func (impl GatewayOpenapiRuleServiceImpl) DeleteByPackage(pack *orm.GatewayPackage) error {
	rules, err := impl.GetPackageRules(pack.Id, nil)
	if err != nil {
		return err
	}
	for _, rule := range rules {
		err = impl.DeleteRule(rule.Id, nil)
		if err != nil {
			return err
		}
	}
	// err = impl.SetPackageKongPolicies(pack, nil)
	// if err != nil {
	// 	return err
	// }
	return nil
}

func (impl GatewayOpenapiRuleServiceImpl) DeleteByPackageApi(pack *orm.GatewayPackage, api *orm.GatewayPackageApi) error {
	rules, err := impl.GetApiRules(api.Id)
	if err != nil {
		return err
	}
	for _, rule := range rules {
		err = impl.DeleteRule(rule.Id, nil)
		if err != nil {
			return err
		}
	}
	err = impl.SetPackageKongPolicies(pack, nil)
	if err != nil {
		return err
	}
	return nil
}
