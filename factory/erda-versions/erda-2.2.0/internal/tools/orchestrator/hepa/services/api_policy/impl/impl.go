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
	"fmt"
	"hash/fnv"
	"math"
	"runtime/debug"
	"strings"
	"sync"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/apipolicy"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/bundle"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/config"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/k8s"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/service"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/api_policy"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/domain"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/endpoint_api"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/global"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/openapi_rule"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/zone"
)

const (
	mutexBucketSize = 512
)

var azMutex []*sync.Mutex

func init() {
	for i := 0; i < mutexBucketSize; i++ {
		azMutex = append(azMutex, &sync.Mutex{})
	}
}

func strHash(s string) uint32 {
	h := fnv.New32a()
	_, _ = h.Write([]byte(s))
	return h.Sum32()
}

func getAzMutex(tag string) *sync.Mutex {
	index := strHash(tag) % mutexBucketSize
	return azMutex[index]
}

type GatewayApiPolicyServiceImpl struct {
	azDb            service.GatewayAzInfoService
	kongDb          service.GatewayKongInfoService
	ingressPolicyDb service.GatewayIngressPolicyService
	kongPolicyDb    service.GatewayPolicyService
	packageDb       service.GatewayPackageService
	packageApiDb    service.GatewayPackageApiService
	routeDB         service.GatewayRouteService
	defaultPolicyDb service.GatewayDefaultPolicyService
	openapiRuleBiz  *openapi_rule.GatewayOpenapiRuleService
	zoneBiz         *zone.GatewayZoneService
	zoneDb          service.GatewayZoneService
	globalBiz       *global.GatewayGlobalService
	engine          *orm.OrmEngine
	domainBiz       *domain.GatewayDomainService
	packageBiz      *endpoint_api.GatewayOpenapiService
	reqCtx          context.Context
}

var once sync.Once

func NewGatewayApiPolicyServiceImpl() error {
	once.Do(
		func() {
			azDb, _ := service.NewGatewayAzInfoServiceImpl()
			ingressPolicyDb, _ := service.NewGatewayIngressPolicyServiceImpl()
			kongPolicyDb, _ := service.NewGatewayPolicyServiceImpl()
			defaultPolicyDb, _ := service.NewGatewayDefaultPolicyServiceImpl()
			kongDb, _ := service.NewGatewayKongInfoServiceImpl()
			zoneDb, _ := service.NewGatewayZoneServiceImpl()
			packageDb, _ := service.NewGatewayPackageServiceImpl()
			packageApiDb, _ := service.NewGatewayPackageApiServiceImpl()
			routeDB, _ := service.NewGatewayRouteServiceImpl()
			engine, _ := orm.GetSingleton()
			api_policy.Service = &GatewayApiPolicyServiceImpl{
				azDb:            azDb,
				kongDb:          kongDb,
				ingressPolicyDb: ingressPolicyDb,
				kongPolicyDb:    kongPolicyDb,
				packageDb:       packageDb,
				packageApiDb:    packageApiDb,
				routeDB:         routeDB,
				defaultPolicyDb: defaultPolicyDb,
				openapiRuleBiz:  &openapi_rule.Service,
				zoneBiz:         &zone.Service,
				zoneDb:          zoneDb,
				globalBiz:       &global.Service,
				engine:          engine,
				domainBiz:       &domain.Service,
				packageBiz:      &endpoint_api.Service,
			}
		})
	return nil
}

func (impl GatewayApiPolicyServiceImpl) Clone(ctx context.Context) api_policy.GatewayApiPolicyService {
	newService := impl
	newService.reqCtx = ctx
	return &newService
}

func (impl GatewayApiPolicyServiceImpl) GetPolicyConfig(category, packageId, packageApiId string) (result interface{}, err error) {
	defer func() {
		if err != nil {
			logrus.Errorf("error happened, err:%+v", err)
		}
	}()
	if category == "" || packageId == "" {
		// keep compatible
		result = map[string]interface{}{
			"category":    "",
			"description": "",
			"policyList":  nil,
		}
		return
	}
	pack, err := impl.packageDb.Get(packageId)
	if err != nil {
		return
	}
	policyEngine, err := apipolicy.GetPolicyEngine(category)
	if err != nil {
		return
	}
	info := dto.DiceInfo{
		ProjectId: pack.DiceProjectId,
	}
	projectName, err := (*impl.globalBiz).GetProjectName(info)
	if err != nil || projectName == "" {
		err = errors.Errorf("get projectName failed, info:%+v, err:%+v", info, err)
		return
	}
	serviceInfo := apipolicy.ServiceInfo{
		ProjectName: projectName,
		Env:         pack.DiceProjectId,
	}
	ctx := map[string]interface{}{}
	ctx[apipolicy.CTX_SERVICE_INFO] = serviceInfo
	if packageApiId == "" {
		var dto interface{}
		dto, err = policyEngine.GetConfig(category, packageId, nil, ctx)
		if err != nil {
			return
		}
		result = dto
		return
	}
	api, err := impl.packageApiDb.Get(packageApiId)
	if err != nil {
		return
	}
	var zone *orm.GatewayZone
	if api.ZoneId != "" {
		zone, err = (*impl.zoneBiz).GetZone(api.ZoneId)
		if err != nil || zone == nil {
			err = errors.Errorf("get zone failed, info:%+v, err:%+v", info, err)
			return
		}
	}
	dto, err := policyEngine.GetConfig(category, packageId, zone, ctx)
	if err != nil {
		return
	}
	result = dto
	return
}

func (impl GatewayApiPolicyServiceImpl) RefreshZoneIngress(zone orm.GatewayZone, az orm.GatewayAzInfo) error {
	exist, err := (*impl.zoneBiz).GetZone(zone.Id)
	if err != nil {
		return err
	}
	if exist == nil {
		logrus.Infof("zone not exist, maybe session rollback, id:%s", zone.Id)
		return nil
	}
	var zoneRegions []string
	zoneRegions = append(zoneRegions, service.ZONE_REGIONS...)
	zoneRegions = append(zoneRegions, service.GLOBAL_REGIONS...)

	changes, err := impl.ingressPolicyDb.GetChangesByRegions(az.Az, strings.Join(zoneRegions, "|"), zone.Id)
	if err != nil {
		return err
	}
	k8sAdapter, err := k8s.NewAdapter(az.Az)
	if err != nil {
		return err
	}
	namespace, _, err := impl.kongDb.GetK8SInfo(&orm.GatewayKongInfo{
		ProjectId: zone.DiceProjectId,
		Az:        zone.DiceClusterName,
		Env:       zone.DiceEnv,
	})
	if err != nil {
		return err
	}
	err = impl.deployIngressChanges(k8sAdapter, namespace, zone, *changes)
	if err != nil {
		return err
	}
	return nil
}

func (impl GatewayApiPolicyServiceImpl) patchGlobalHttpSnippet(k8sAdapter k8s.K8SAdapter, httpSnippet *[]string) error {
	count, err := k8sAdapter.CountIngressController()
	if err != nil {
		count = 1
		logrus.Errorf("get ingress controller count failed, err:%+v", err)
	}
	qps := int64(math.Ceil(float64(config.ServerConf.OfflineQps) / float64(count)))
	limitReqZone := fmt.Sprintf(`limit_req_zone 1 zone=offline-limit:1m rate=%dr/s;
map $http_referer                   $cors_use_referer {
    ~(^https?://[^/]*)/.*           $1;
}
map $http_origin-$cors_use_referer $from_request_origin_or_referer {
    "-"                             "*";
    "~^-"                           $cors_use_referer;
    default                         $http_origin;
}
`, qps)
	*httpSnippet = append(*httpSnippet, limitReqZone)
	return nil
}

func (impl GatewayApiPolicyServiceImpl) deployControllerChanges(k8sAdapter k8s.K8SAdapter, changes service.IngressChanges) error {
	configmap := map[string]*string{}
	for _, options := range changes.ConfigmapOptions {
		for key, value := range options {
			if old, exist := configmap[key]; exist {
				logrus.Debugf("invalid changes:%+v", changes)
				return errors.Errorf("config map key duplicated, key:%s, value:%+v, new value:%+v",
					key, old, value)
			}
			configmap[key] = value
		}
	}
	luaDictConfig := "configuration_data: 100"
	configmap["lua-shared-dicts"] = &luaDictConfig
	emptyStr := ""
	var mainSnippet *string
	if changes.MainSnippets != nil {
		if len(*changes.MainSnippets) == 0 {
			mainSnippet = &emptyStr
		} else {
			mergeStr := strings.Join(*changes.MainSnippets, "\n###HEPA-AUTO-CONFIG###\n")
			mainSnippet = &mergeStr
		}
	}
	var httpSnippet *string
	if changes.HttpSnippets != nil {
		err := impl.patchGlobalHttpSnippet(k8sAdapter, changes.HttpSnippets)
		if err != nil {
			return err
		}
		mergeStr := strings.Join(*changes.HttpSnippets, "\n###HEPA-AUTO-CONFIG###\n")
		httpSnippet = &mergeStr
	}
	var serverSnippet *string
	if changes.ServerSnippets != nil {
		if len(*changes.ServerSnippets) == 0 {
			serverSnippet = &emptyStr
		} else {
			mergeStr := strings.Join(*changes.ServerSnippets, "\n###HEPA-AUTO-CONFIG###\n")
			serverSnippet = &mergeStr
		}
	}
	if len(configmap) > 0 || mainSnippet != nil || httpSnippet != nil || serverSnippet != nil {
		err := k8sAdapter.UpdateIngressConroller(configmap, mainSnippet, httpSnippet, serverSnippet)
		if err != nil {
			return err
		}
	}
	return nil
}

func (impl GatewayApiPolicyServiceImpl) getAnnotationChanges(changes service.IngressChanges) (map[string]*string, *string, error) {
	emptyStr := ""
	annotation := map[string]*string{}
	for _, anno := range changes.Annotations {
		for key, value := range anno {
			if old, exist := annotation[key]; exist {
				logrus.Debugf("invalid changes:%+v", changes)
				return nil, nil, errors.Errorf("annotation key duplicated, key:%s, value:%+v, new value:%+v",
					key, old, value)
			}
			annotation[key] = value
		}
	}
	var locationSnippet *string
	if changes.LocationSnippets != nil {
		if len(*changes.LocationSnippets) == 0 {
			locationSnippet = &emptyStr
		} else {
			mergeStr := strings.Join(*changes.LocationSnippets, "\n###HEPA-AUTO-CONFIG###\n")
			locationSnippet = &mergeStr
		}

	}
	return annotation, locationSnippet, nil
}

func (impl GatewayApiPolicyServiceImpl) deployAnnotationChanges(k8sAdapter k8s.K8SAdapter, zone orm.GatewayZone, changes service.IngressChanges, namespace string) error {
	annotation, locationSnippet, err := impl.getAnnotationChanges(changes)
	if err != nil {
		return err
	}
	if len(annotation) > 0 || locationSnippet != nil {
		err = k8sAdapter.UpdateIngressAnnotaion(namespace, zone.Name, annotation, locationSnippet)
		if err != nil {
			cause := err.Error()
			lines := strings.Split(cause, "\n")
			for i := len(lines) - 1; i >= 0; i-- {
				// move the useless warn info of nginx
				if strings.Contains(lines[i], "[warn]") {
					lines = append(lines[:i], lines[i+1:]...)
				}
			}
			return errors.New(strings.Join(lines, "\n"))
		}
	}
	return nil
}
func (impl GatewayApiPolicyServiceImpl) deployIngressChanges(k8sAdapter k8s.K8SAdapter, namespace string, zone orm.GatewayZone, changes service.IngressChanges, annotationReset ...bool) error {
	logrus.Debugf("deploy ingress zone:%+v, changes:%+v", zone, changes)
	var err error
	if len(annotationReset) > 0 && annotationReset[0] {
		err = impl.deployAnnotationChanges(k8sAdapter, zone, changes, namespace)
		if err != nil {
			return err
		}
		err = impl.deployControllerChanges(k8sAdapter, changes)
		if err != nil {
			return err
		}
		return nil
	}
	err = impl.deployControllerChanges(k8sAdapter, changes)
	if err != nil {
		return err
	}
	err = impl.deployAnnotationChanges(k8sAdapter, zone, changes, namespace)
	if err != nil {
		return err
	}
	return nil
}

func (impl GatewayApiPolicyServiceImpl) setIngressPolicyDaoConfig(policyDao *orm.GatewayIngressPolicy, policyConfig apipolicy.PolicyConfig) error {
	annotation := policyConfig.IngressAnnotation
	controller := policyConfig.IngressController
	var regions []string
	if annotation != nil {
		if annotation.Annotation != nil {
			regions = append(regions, "annotation")
			annoByte, err := json.Marshal(annotation.Annotation)
			if err != nil {
				return errors.WithStack(err)
			}
			policyDao.Annotations = annoByte
		}
		if annotation.LocationSnippet != nil {
			regions = append(regions, "location")
			policyDao.LocationSnippet = []byte(*annotation.LocationSnippet)
		}
	}
	if controller != nil {
		if controller.ConfigOption != nil {
			regions = append(regions, "option")
			optionByte, err := json.Marshal(controller.ConfigOption)
			if err != nil {
				return errors.WithStack(err)
			}
			policyDao.ConfigmapOption = optionByte
		}
		if controller.MainSnippet != nil {
			regions = append(regions, "main")
			policyDao.MainSnippet = []byte(*controller.MainSnippet)
		}
		if controller.HttpSnippet != nil {
			regions = append(regions, "http")
			policyDao.HttpSnippet = []byte(*controller.HttpSnippet)
		}
		if controller.ServerSnippet != nil {
			regions = append(regions, "server")
			policyDao.ServerSnippet = []byte(*controller.ServerSnippet)
		}
	}
	policyDao.Regions = strings.Join(regions, "|")
	return nil
}

func (impl GatewayApiPolicyServiceImpl) executePolicyEngine(zone *orm.GatewayZone, category string, engine apipolicy.PolicyEngine, config []byte, dto apipolicy.PolicyDto, ctx map[string]interface{}, policyService service.GatewayIngressPolicyService, k8sAdapter k8s.K8SAdapter, helper *service.SessionHelper, needDeployTag ...bool) error {
	var apiService service.GatewayPackageApiService
	var packService service.GatewayPackageService
	var kongService service.GatewayKongInfoService
	var err error
	if helper != nil {
		apiService, err = impl.packageApiDb.NewSession(helper)
		if err != nil {
			return err
		}
		packService, err = impl.packageDb.NewSession(helper)
		if err != nil {
			return err
		}
		kongService, err = impl.kongDb.NewSession(helper)
		if err != nil {
			return err
		}
	} else {
		apiService = impl.packageApiDb
		packService = impl.packageDb
		kongService = impl.kongDb
	}
	needDeployIngress := true
	if len(needDeployTag) > 0 {
		needDeployIngress = needDeployTag[0]
	}
	policyConfig, err := engine.ParseConfig(dto, ctx)
	if err != nil {
		return err
	}
	policyDao := &orm.GatewayIngressPolicy{
		Name:   category,
		Az:     zone.DiceClusterName,
		ZoneId: zone.Id,
	}
	err = impl.setIngressPolicyDaoConfig(policyDao, policyConfig)
	if err != nil {
		return err
	}
	err = policyService.CreateOrUpdate(policyDao)
	if err != nil {
		return err
	}
	namespace, _, err := kongService.GetK8SInfo(&orm.GatewayKongInfo{
		ProjectId: zone.DiceProjectId,
		Az:        zone.DiceClusterName,
		Env:       zone.DiceEnv,
	})
	if err != nil {
		return err
	}
	if needDeployIngress {
		changes, err := policyService.GetChangesByRegionsImpl(zone.DiceClusterName, policyDao.Regions, zone.Id)
		if err != nil {
			return err
		}
		err = impl.deployIngressChanges(k8sAdapter, namespace, *zone, *changes, policyConfig.AnnotationReset)
		if err != nil {
			return err
		}
	}
	if policyConfig.KongPolicyChange {
		if zone.Type == service.ZONE_TYPE_PACKAGE_API {
			api, err := apiService.GetByAny(&orm.GatewayPackageApi{
				ZoneId: zone.Id,
			})
			if err != nil {
				return err
			}
			if api != nil {
				err = (*impl.openapiRuleBiz).SetPackageApiKongPolicies(api, helper)
				if err != nil {
					return err
				}
			}
		} else if zone.Type == service.ZONE_TYPE_UNITY {
			pack, err := packService.GetByAny(&orm.GatewayPackage{
				ZoneId: zone.Id,
			})
			if err != nil {
				return err
			}
			if pack != nil {
				err = (*impl.openapiRuleBiz).SetPackageKongPolicies(pack, helper)
				if err != nil {
					return err
				}
			}
		}
	}
	return nil
}

func (impl GatewayApiPolicyServiceImpl) SetZonePolicyConfig(zone *orm.GatewayZone, category string, config []byte, helper *service.SessionHelper, needDeployTag ...bool) (apipolicy.PolicyDto, string, error) {
	needDeployIngress := true
	if len(needDeployTag) > 0 {
		needDeployIngress = needDeployTag[0]
	}
	var policyService service.GatewayIngressPolicyService
	var kongService service.GatewayKongInfoService
	var err error
	if helper != nil {
		policyService, err = impl.ingressPolicyDb.NewSession(helper)
		if err != nil {
			return nil, "", err
		}
		kongService, err = impl.kongDb.NewSession(helper)
		if err != nil {
			return nil, "", err
		}
	} else {
		policyService = impl.ingressPolicyDb
		kongService = impl.kongDb
	}

	// 部署ingress需要加锁
	if needDeployIngress {
		mutex := getAzMutex(zone.DiceClusterName)
		mutex.Lock()
		defer mutex.Unlock()
	}

	policyEngine, err := apipolicy.GetPolicyEngine(category)
	if err != nil {
		return nil, "", err
	}
	dto, err, msg := policyEngine.UnmarshalConfig(config)
	if err != nil {
		return nil, msg, err
	}
	k8sAdapter, err := k8s.NewAdapter(zone.DiceClusterName)
	if err != nil {
		return nil, "", err
	}
	kongInfo, err := kongService.GetKongInfo(&orm.GatewayKongInfo{
		Az:        zone.DiceClusterName,
		ProjectId: zone.DiceProjectId,
		Env:       zone.DiceEnv,
	})
	if err != nil {
		return nil, "", err
	}
	kongAdapter := kong.NewKongAdapter(kongInfo.KongAddr)
	ctx := map[string]interface{}{
		apipolicy.CTX_K8S_CLIENT:   k8sAdapter,
		apipolicy.CTX_IDENTIFY:     zone.Name,
		apipolicy.CTX_KONG_ADAPTER: kongAdapter,
		apipolicy.CTX_ZONE:         zone,
	}

	err = impl.executePolicyEngine(zone, category, policyEngine, config, dto, ctx, policyService, k8sAdapter, helper, needDeployTag...)
	if err != nil {
		return nil, fmt.Sprintf("执行策略失败, 失败原因:\n%s", errors.Cause(err)), err
	}
	if category != "built-in" {
		builtinEngine, err := apipolicy.GetPolicyEngine("built-in")
		if err != nil {
			return nil, "", err
		}
		err = impl.executePolicyEngine(zone, "built-in", builtinEngine, nil, nil, ctx, policyService, k8sAdapter, helper, needDeployTag...)
		if err != nil {
			return nil, fmt.Sprintf("更新内置策略失败, 失败原因:\n%s", errors.Cause(err)), err
		}
	}
	return dto, "", nil
}

func (impl GatewayApiPolicyServiceImpl) SetZoneDefaultPolicyConfig(packageId string, zone *orm.GatewayZone, az *orm.GatewayAzInfo, helper ...*service.SessionHelper) (map[string]*string, *string, *service.SessionHelper, error) {
	mutex := getAzMutex(az.Az)
	mutex.Lock()
	defer mutex.Unlock()
	var session *service.SessionHelper
	var err error
	if len(helper) > 0 {
		session = helper[0]
	} else {
		session, err = service.NewSessionHelper()
		if err != nil {
			return nil, nil, nil, err
		}
	}
	policies, err := impl.defaultPolicyDb.SelectByAny(&orm.GatewayDefaultPolicy{
		Level:     orm.POLICY_PACKAGE_LEVEL,
		PackageId: packageId,
	})
	if err != nil {
		return nil, nil, nil, err
	}
	for _, policy := range policies {
		_, _, err = impl.SetZonePolicyConfig(zone, policy.Name, policy.Config, session, false)
		if err != nil {
			return nil, nil, nil, err
		}
	}
	if len(policies) == 0 {
		_, _, err = impl.SetZonePolicyConfig(zone, "built-in", nil, session, false)
		if err != nil {
			return nil, nil, nil, err
		}
	}
	policyService, err := impl.ingressPolicyDb.NewSession(session)
	if err != nil {
		return nil, nil, nil, err
	}
	controllerChange, err := policyService.GetChangesByRegions(az.Az, strings.Join(service.GLOBAL_REGIONS, "|"))
	if err != nil {
		return nil, nil, nil, err
	}
	zoneChange, err := policyService.GetChangesByRegions(az.Az, strings.Join(service.ZONE_REGIONS, "|"), zone.Id)
	if err != nil {
		return nil, nil, nil, err
	}
	k8sAdapter, err := k8s.NewAdapter(az.Az)
	if err != nil {
		return nil, nil, nil, err
	}
	err = impl.deployControllerChanges(k8sAdapter, *controllerChange)
	if err != nil {
		return nil, nil, nil, err
	}
	annotations, locationSnippet, err := impl.getAnnotationChanges(*zoneChange)
	if err != nil {
		return nil, nil, nil, err
	}
	return annotations, locationSnippet, session, nil
}

func doRecover() {
	if r := recover(); r != nil {
		logrus.Errorf("recovered from: %+v ", r)
		debug.PrintStack()
	}
}

func (impl GatewayApiPolicyServiceImpl) SetPackageDefaultPolicyConfig(category, packageId string, az *orm.GatewayAzInfo, config []byte, helperOption ...*service.SessionHelper) (string, error) {
	mutex := getAzMutex(az.Az)
	mutex.Lock()
	defer mutex.Unlock()
	var err error
	transSucc := false
	var helper *service.SessionHelper
	if len(helperOption) == 0 {
		helper, err = service.NewSessionHelper()
		if err != nil {
			return "", err
		}
		defer func() {
			if transSucc {
				_ = helper.Commit()
			} else {
				_ = helper.Rollback()
			}
			helper.Close()
		}()
	} else {
		helper = helperOption[0]
	}
	var zones []orm.GatewayZone
	defer func() {
		if !transSucc {
			_ = helper.Rollback()
			wg := sync.WaitGroup{}
			for _, zone := range zones {
				wg.Add(1)
				go func(z orm.GatewayZone) {
					defer doRecover()
					err = impl.RefreshZoneIngress(z, *az)
					if err != nil {
						logrus.Errorf("refresh zone ingress failed, %+v", err)
					}
					wg.Done()
				}(zone)
			}
			wg.Wait()
			logrus.Info("zone ingress rollback done")
		}
	}()
	packageDb, err := impl.packageDb.NewSession(helper)
	if err != nil {
		return "", err
	}
	pack, err := packageDb.Get(packageId)
	if err != nil {
		return "", err
	}
	if pack.Scene == orm.UNITY_SCENE {
		zone, err := (*impl.zoneBiz).GetZone(pack.ZoneId, helper)
		if err != nil {
			return "", err
		}
		if zone == nil {
			zone, err = (*impl.packageBiz).CreateUnityPackageZone(packageId, helper)
			if err != nil {
				return "", err
			}
		}
		zones = append(zones, *zone)
	}
	packageApiDb, err := impl.packageApiDb.NewSession(helper)
	if err != nil {
		return "", err
	}
	apis, err := packageApiDb.SelectByAny(&orm.GatewayPackageApi{
		PackageId: packageId,
	})
	if err != nil {
		return "", err
	}
	for _, api := range apis {
		if api.ZoneId != "" {
			zone, err := (*impl.zoneBiz).GetZone(api.ZoneId, helper)
			if err != nil {
				return "", err
			}
			zones = append(zones, *zone)
		}
	}
	policyEngine, err := apipolicy.GetPolicyEngine(category)
	if err != nil {
		return "", err
	}
	dto, err, msg := policyEngine.UnmarshalConfig(config)
	if err != nil {
		return msg, err
	}
	needResetAnnotaion := policyEngine.NeedResetAnnotation(dto)
	policyService, err := impl.ingressPolicyDb.NewSession(helper)
	if err != nil {
		return "", err
	}
	for _, zone := range zones {
		policy, err := policyService.GetByAny(&orm.GatewayIngressPolicy{
			Name:   category,
			ZoneId: zone.Id,
		})
		if err != nil {
			return "", err
		}
		if policy == nil || len(policy.Config) == 0 {
			_, msg, err = impl.SetZonePolicyConfig(&zone, category, config, helper, false)
			if err != nil {
				return msg, err
			}
		}
	}
	controllerChanges, err := policyService.GetChangesByRegions(az.Az, strings.Join(service.GLOBAL_REGIONS, "|"))
	if err != nil {
		return "", err
	}
	var zoneChanges []service.IngressChanges
	for _, zone := range zones {
		zoneChange, err := policyService.GetChangesByRegions(az.Az, strings.Join(service.ZONE_REGIONS, "|"), zone.Id)
		if err != nil {
			return "", err
		}
		zoneChanges = append(zoneChanges, *zoneChange)
	}
	k8sAdapter, err := k8s.NewAdapter(az.Az)
	if err != nil {
		return "", err
	}
	kongDb, err := impl.kongDb.NewSession(helper)
	if err != nil {
		return "", err
	}
	namespace, _, err := kongDb.GetK8SInfo(&orm.GatewayKongInfo{
		ProjectId: pack.DiceProjectId,
		Env:       pack.DiceEnv,
		Az:        pack.DiceClusterName,
	})
	if err != nil {
		return "", err
	}
	deployZoneFunc := func() error {
		wg := sync.WaitGroup{}
		var err error
		for i := 0; i < len(zones); i++ {
			wg.Add(1)
			go func(index int) {
				defer doRecover()
				gerr := impl.deployAnnotationChanges(k8sAdapter, zones[index], zoneChanges[index], namespace)
				if gerr != nil {
					err = gerr
				}
				wg.Done()
			}(i)
		}
		wg.Wait()
		return err
	}
	if needResetAnnotaion {
		err = deployZoneFunc()
		if err != nil {
			return "", err
		}
		err = impl.deployControllerChanges(k8sAdapter, *controllerChanges)
		if err != nil {
			return "", err
		}
	} else {
		err = impl.deployControllerChanges(k8sAdapter, *controllerChanges)
		if err != nil {
			return "", err
		}
		err = deployZoneFunc()
		if err != nil {
			return "", err
		}
	}
	err = impl.defaultPolicyDb.CreateOrUpdate(&orm.GatewayDefaultPolicy{
		Config:    config,
		Level:     orm.POLICY_PACKAGE_LEVEL,
		Name:      category,
		PackageId: packageId,
	})
	if err != nil {
		return "", err
	}
	transSucc = true
	return "", nil
}

func (impl GatewayApiPolicyServiceImpl) SetPolicyConfig(category, packageId, packageApiId string, config []byte) (result interface{}, rerr error) {
	auditCtx := map[string]interface{}{}
	var pack *orm.GatewayPackage
	defer func() {
		if rerr != nil {
			logrus.Errorf("error happened, err:%+v", rerr)
		}
		if pack == nil {
			return
		}
		engine, err := apipolicy.GetPolicyEngine(category)
		if err != nil {
			return
		}
		dto, err, _ := engine.UnmarshalConfig(config)
		if err != nil {
			return
		}
		if dto.Enable() {
			auditCtx["switch"] = "on"
		} else {
			auditCtx["switch"] = "off"
		}
		auditCtx["endpoint"] = pack.PackageName
		auditKey := apistructs.UpdateGlobalRoutePolicyTemplate
		if packageApiId != "" {
			auditKey = apistructs.UpdateRoutePolicyTemplate
		}
		auditCtx["policy"] = category
		audit := common.MakeAuditInfo(impl.reqCtx, common.ScopeInfo{
			ProjectId: pack.DiceProjectId,
			Workspace: pack.DiceEnv,
		}, auditKey, err, auditCtx)
		if audit != nil {
			err = bundle.Bundle.CreateAuditEvent(&apistructs.AuditCreateRequest{Audit: *audit})
			if err != nil {
				logrus.Errorf("create audit failed, err:%+v", err)
			}
		}
	}()
	if category == "" || packageId == "" {
		rerr = errors.New("invalid argument")
		return
	}
	pack, rerr = impl.packageDb.Get(packageId)
	if rerr != nil {
		return
	}
	if pack == nil {
		rerr = errors.New("endpoint not found")
		return
	}
	az, rerr := impl.azDb.GetAzInfoByClusterName(pack.DiceClusterName)
	if rerr != nil {
		return
	}
	if packageApiId == "" {
		var msg string
		msg, rerr = impl.SetPackageDefaultPolicyConfig(category, packageId, az, config)
		if rerr != nil {
			logrus.Errorf("set default policy config failed, err:%+v", rerr)
			if msg != "" {
				rerr = errors.Errorf("update config failed: %s", msg)
			}
			return
		}
		result = true
		return
	}
	api, rerr := impl.packageApiDb.Get(packageApiId)
	if rerr != nil {
		return
	}
	if api == nil {
		rerr = errors.New("api not found")
		return
	}
	auditCtx["path"] = api.ApiPath
	auditCtx["method"] = api.Method
	var zone *orm.GatewayZone
	if api.ZoneId == "" {
		var domains []string
		domains, rerr = (*impl.domainBiz).GetPackageDomains(packageId)
		if rerr != nil {
			return
		}
		var zoneId string
		zoneId, rerr = (*impl.packageBiz).TouchPackageApiZone(endpoint_api.PackageApiInfo{api, domains, pack.DiceProjectId, pack.DiceEnv, pack.DiceClusterName, false})
		if rerr != nil {
			return
		}
		zone, rerr = (*impl.zoneBiz).GetZone(zoneId)
		if rerr != nil || zone == nil {
			rerr = errors.Errorf("get zone failed, err:%+v", rerr)
			return
		}
		api.ZoneId = zoneId
		rerr = impl.packageApiDb.Update(api)
		if rerr != nil {
			return
		}
	} else {
		zone, rerr = (*impl.zoneBiz).GetZone(api.ZoneId)
		if rerr != nil || zone == nil {
			rerr = errors.Errorf("get zone failed, api:%+v, err:%+v", api, rerr)
			return
		}
	}
	helper, rerr := service.NewSessionHelper()
	if rerr != nil {
		return
	}
	transSucc := false
	defer func() {
		if transSucc {
			_ = helper.Commit()
		} else {
			_ = helper.Rollback()
			err := impl.RefreshZoneIngress(*zone, *az)
			if err != nil {
				logrus.Errorf("refresh zone ingress failed, %+v", err)
			}
			logrus.Info("zone ingress rollback done")
		}
		helper.Close()
	}()
	dto, msg, rerr := impl.SetZonePolicyConfig(zone, category, config, helper)
	if rerr != nil {
		logrus.Errorf("set zone policy config failed, err:%+v", rerr)
		if msg != "" {
			rerr = errors.Errorf("update config failed: %s", msg)
		}
		return
	}
	ingressPolicyService, rerr := impl.ingressPolicyDb.NewSession(helper)
	if rerr != nil {
		return
	}
	if dto.IsGlobal() {
		config = []byte{}
	}
	rerr = ingressPolicyService.UpdatePartial(&orm.GatewayIngressPolicy{
		Az:     az.Az,
		ZoneId: zone.Id,
		Name:   category,
		Config: config,
	}, "config")
	if rerr != nil {
		return
	}
	transSucc = true
	result = dto
	return
}
