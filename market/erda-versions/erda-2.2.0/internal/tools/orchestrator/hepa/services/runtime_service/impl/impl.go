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
	"fmt"
	"regexp"
	"sort"
	"strconv"
	"strings"
	"sync"

	"github.com/pkg/errors"
	log "github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/bundle"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/util"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/config"
	gw "github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/i18n"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/k8s"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
	db "github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/service"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/domain"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/endpoint_api"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/micro_api"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/runtime_service"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/strutil"
)

type GatewayRuntimeServiceServiceImpl struct {
	runtimeDb  db.GatewayRuntimeServiceService
	azDb       db.GatewayAzInfoService
	kongDb     db.GatewayKongInfoService
	packageBiz *endpoint_api.GatewayOpenapiService
	domainBiz  *domain.GatewayDomainService
	apiBiz     *micro_api.GatewayApiService
	reqCtx     context.Context
}

var once sync.Once

func NewGatewayRuntimeServiceServiceImpl() (e error) {
	once.Do(
		func() {
			runtimeDb, err := db.NewGatewayRuntimeServiceServiceImpl()
			if err != nil {
				e = err
			}
			azDb, err := db.NewGatewayAzInfoServiceImpl()
			if err != nil {
				e = err
			}
			kongDb, err := db.NewGatewayKongInfoServiceImpl()
			if err != nil {
				e = err
			}
			runtime_service.Service = &GatewayRuntimeServiceServiceImpl{
				runtimeDb:  runtimeDb,
				azDb:       azDb,
				kongDb:     kongDb,
				packageBiz: &endpoint_api.Service,
				domainBiz:  &domain.Service,
				apiBiz:     &micro_api.Service,
			}
		})
	return
}

type runtimeService struct {
	dto gw.ServiceDetailDto
	dao *orm.GatewayRuntimeService
}

func diffServices(reqServices []gw.ServiceDetailDto, existServices []orm.GatewayRuntimeService) (addOrUpdates []runtimeService, dels []orm.GatewayRuntimeService) {
	for _, service := range reqServices {
		exist := false
		for i := len(existServices) - 1; i >= 0; i-- {
			serviceObj := existServices[i]
			if service.ServiceName == serviceObj.ServiceName {
				exist = true
				existServices = append(existServices[:i], existServices[i+1:]...)
				addOrUpdates = append(addOrUpdates, runtimeService{service, &serviceObj})
				break
			}

		}
		if !exist {
			addOrUpdates = append(addOrUpdates, runtimeService{service, nil})
		}
	}
	for _, service := range existServices {
		// skip those already deleted services
		if service.InnerAddress != "" {
			dels = append(dels, service)
		}
	}
	return
}

func (impl GatewayRuntimeServiceServiceImpl) Clone(ctx context.Context) runtime_service.GatewayRuntimeServiceService {
	newService := impl
	newService.reqCtx = ctx
	return &newService
}

func (impl GatewayRuntimeServiceServiceImpl) TouchRuntimeComplete(ctx map[string]interface{}, reqDto *gw.RuntimeServiceReqDto) *common.StandardResult {
	defer util.DoRecover()
	res := &common.StandardResult{Success: false}
	var err error
	var ok bool
	var sessionI, endpointsI interface{}
	var session *db.SessionHelper
	var package2Endpoint map[string]*orm.GatewayRuntimeService
	sessionI, ok = ctx["dbsession"]
	if !ok {
		err = errors.New("can't find dbsession from context")
		goto failed
	}
	session, ok = sessionI.(*db.SessionHelper)
	if !ok {
		err = errors.New("acquire sesion failed")
		goto failed
	}
	endpointsI, ok = ctx["endpoints"]
	if !ok {
		err = errors.New("can't find endpoints from context")
		goto failed
	}
	package2Endpoint, ok = endpointsI.(map[string]*orm.GatewayRuntimeService)
	if !ok {
		err = errors.New("acquire endpoints failed")
		goto failed
	}
	// refresh endpoint services' relation package (ingress & endpoint api)
	for packageId, endpoint := range package2Endpoint {
		_, err = (*impl.domainBiz).GiveRuntimeDomainToPackage(endpoint, session)
		if err != nil {
			goto failed
		}
		err = (*impl.packageBiz).RefreshRuntimePackage(packageId, endpoint, session)
		if err != nil {
			goto failed
		}
	}
	err = session.Commit()
	if err != nil {
		goto failed
	}
	session.Close()

	func() {
		runtimeEndpointsI, ok := ctx["runtime_endpoints"]
		if !ok {
			log.Errorf("can't find dice endpoints from context")
			return
		}
		runtimeEndpoints, ok := runtimeEndpointsI.([]runtime_service.RuntimeEndpointInfo)
		if !ok {
			log.Errorf("acquire dice endpoints failed")
			return
		}
		for _, runtimeEndpoint := range runtimeEndpoints {
			// render platform placeholder
			runtimeEndpoint.Endpoints, err = renderPlatformInfo(runtimeEndpoint.Endpoints, reqDto.ProjectId)
			if err == nil {
				err = (*impl.packageBiz).SetRuntimeEndpoint(runtimeEndpoint)
			}
			if err != nil {
				log.Errorf("set runtime endpoint failed, err:%+v, runtimeEndpoint:%+v", err, runtimeEndpoint)
				humanLog := i18n.Sprintf(ctx["locale"].(string), "FailedToBindServiceEndpoint", runtimeEndpoint.RuntimeService.ServiceName)
				detailLog := fmt.Sprintf("endpoint and service info: %+v, error:%s", runtimeEndpoint, errors.Cause(err).Error())
				go common.AsyncRuntimeError(runtimeEndpoint.RuntimeService.RuntimeId, humanLog, detailLog)
			}
		}
	}()

	return res.SetSuccessAndData(true)
failed:
	log.Errorf("error happened, err:%+v", err)
	if session != nil {
		_ = session.Rollback()
		session.Close()
	}
	return res.SetErrorInfo(&common.ErrInfo{Msg: errors.Cause(err).Error()})
}

func (impl GatewayRuntimeServiceServiceImpl) TouchRuntime(reqDto *gw.RuntimeServiceReqDto) (res bool, err error) {
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
	var runtimeSession db.GatewayRuntimeServiceService
	var endpoints []*orm.GatewayRuntimeService
	package2Endpoint := map[string]*orm.GatewayRuntimeService{}
	var addOrUpdates []runtimeService
	var dels []orm.GatewayRuntimeService
	var daos []orm.GatewayRuntimeService
	var cond *orm.GatewayRuntimeService
	var kongInfo *orm.GatewayKongInfo
	var azInfo *orm.GatewayAzInfo
	var k8sAdapter k8s.K8SAdapter
	var isK8S bool
	var runtimeEndpoints []runtime_service.RuntimeEndpointInfo
	var diceYaml *diceyml.DiceYaml
	var diceObj *diceyml.Object
	ctx := map[string]interface{}{}
	apiGatewayError := false
	err = reqDto.CheckValid()
	if err != nil {
		return
	}
	if org, err := bundle.Bundle.GetOrg(reqDto.OrgId); err == nil {
		ctx["locale"] = org.Locale
	}
	diceYaml, err = bundle.Bundle.GetDiceYAML(reqDto.ReleaseId, reqDto.Env)
	if err != nil {
		return
	}
	if diceYaml == nil {
		err = errors.Errorf("get dice yaml failed, release:%s", reqDto.ReleaseId)
		return
	}
	diceObj = diceYaml.Obj()
	azInfo, err = impl.azDb.GetAzInfoByClusterName(reqDto.ClusterName)
	if err != nil {
		return
	}
	isK8S = (azInfo.Type == orm.AT_K8S || azInfo.Type == orm.AT_EDAS) && !config.ServerConf.UseAdminEndpoint
	kongInfo, err = impl.kongDb.GetByAny(&orm.GatewayKongInfo{
		Az: reqDto.ClusterName,
	})
	if err != nil {
		return
	}
	if isK8S {
		k8sAdapter, err = k8s.NewAdapter(reqDto.ClusterName)
		if err != nil {
			return
		}
	}
	session, err = db.NewSessionHelper()
	if err != nil {
		return
	}
	_, err = session.Session().Exec("set innodb_lock_wait_timeout=600")
	if err != nil {
		return
	}
	runtimeSession, err = impl.runtimeDb.NewSession(session)
	if err != nil {
		return
	}
	// get runtime all service, diff req: adds, updates, dels
	cond = &orm.GatewayRuntimeService{
		ProjectId:   reqDto.ProjectId,
		Workspace:   reqDto.Env,
		ClusterName: reqDto.ClusterName,
		AppId:       reqDto.AppId,
		RuntimeName: reqDto.RuntimeName,
	}
	daos, err = runtimeSession.SelectByAny(cond)
	if err != nil {
		return
	}
	addOrUpdates, dels = diffServices(reqDto.Services, daos)

	// create or update runtime service
	for _, service := range addOrUpdates {
		obj := *cond
		obj.RuntimeId = reqDto.RuntimeId
		obj.AppName = reqDto.AppName
		obj.ReleaseId = reqDto.ReleaseId
		obj.GroupNamespace = reqDto.ServiceGroupNamespace
		obj.GroupName = reqDto.ServiceGroupName
		obj.ProjectNamespace = reqDto.ProjectNamespace
		obj.ServiceName = service.dto.ServiceName
		obj.InnerAddress = service.dto.InnerAddress
		obj.UseApigw = 0
		if reqDto.UseApigw {
			obj.UseApigw = 1
		}
		obj.IsEndpoint = 0
		newCreated := true
		serviceRelease, exist := diceObj.Services[obj.ServiceName]
		if !exist {
			err = errors.Errorf("%s not found in dice.yml",
				obj.ServiceName)
			return
		}
		for _, port := range serviceRelease.Ports {
			if port.Expose {
				obj.IsEndpoint = 1
				obj.BackendProtocol = port.Protocol
				break
			}
		}
		if serviceRelease.TrafficSecurity.Mode != "" {
			obj.IsSecurity = 1
		}
		// create or update
		if service.dao == nil {
			err = runtimeSession.Insert(&obj)
			if err != nil {
				return
			}
		} else {
			obj.Id = service.dao.Id
			if service.dao.InnerAddress != "" {
				newCreated = false
			}
			err = runtimeSession.Update(&obj)
			if err != nil {
				return
			}
			// need clear domains
			if !newCreated && service.dao.IsEndpoint == 1 && obj.IsEndpoint == 0 {
				err = impl.clearDomain(&obj, session)
				if err != nil {
					return
				}
			}
			if obj.IsEndpoint == 1 {
				err = (*impl.domainBiz).UpdateRuntimeServicePort(&obj, diceObj)
				if err != nil {
					return
				}
				err = (*impl.domainBiz).RefreshRuntimeDomain(&obj, session)
				if err != nil {
					return
				}
			}
		}
		_ = session.Commit()
		if len(serviceRelease.Endpoints) > 0 {
			runtimeEndpoints = append(runtimeEndpoints, runtime_service.RuntimeEndpointInfo{
				RuntimeService: &obj,
				Endpoints:      serviceRelease.Endpoints,
			})
		}
		_ = session.Begin()
		if kongInfo != nil {
			var namespace string
			namespace, _, err = impl.kongDb.GenK8SInfo(kongInfo)
			if err != nil {
				return
			}
			if isK8S && serviceRelease.MeshEnable != nil && *serviceRelease.MeshEnable {
				err = k8sAdapter.MakeGatewaySupportMesh(namespace)
				if err != nil {
					return
				}
			}
			if isK8S && serviceRelease.TrafficSecurity.Mode == "https" {
				err = k8sAdapter.MakeGatewaySupportHttps(namespace)
				if err != nil {
					return
				}
			}
			err = (*impl.apiBiz).TouchRuntimeApi(&obj, session, newCreated)
			if err != nil {
				_ = session.Rollback()
				_ = session.Begin()
				log.Errorf("touch runtime api failed, err:%+v", err)
				apiGatewayError = true
			}
		}
		if obj.IsEndpoint == 1 {
			endpoints = append(endpoints, &obj)
		}
	}
	// touch endpoint services' relation package meta
	if !apiGatewayError {
		for _, endpoint := range endpoints {
			var packageId string
			if reqDto.UseApigw {
				packageId, _, err = (*impl.packageBiz).TouchRuntimePackageMeta(endpoint, session)
				if err != nil {
					_ = session.Rollback()
					_ = session.Begin()
					log.Errorf("touch runtime package meta failed, err:%+v", err)
					apiGatewayError = true
					break
				}
				package2Endpoint[packageId] = endpoint
			} else {
				// 尝试清除之前依赖API网关自动创建的流量入口
				err = (*impl.packageBiz).TryClearRuntimePackage(endpoint, session)
				if err != nil {
					_ = session.Rollback()
					_ = session.Begin()
					log.Errorf("try clear runtime package failed, err:%+v", err)
					apiGatewayError = true
					break
				}
			}
		}
	}
	for _, dao := range dels {
		err = impl.clearService(&dao, session)
		if err != nil {
			return
		}
	}
	if reqDto.UseApigw && !apiGatewayError {
		ctx["dbsession"] = session
		ctx["endpoints"] = package2Endpoint
		ctx["runtime_endpoints"] = runtimeEndpoints
		go impl.TouchRuntimeComplete(ctx, reqDto)
	} else {
		err = session.Commit()
		if err != nil {
			return
		}
		session.Close()
	}
	res = true
	return
}

func (impl GatewayRuntimeServiceServiceImpl) clearDomain(dao *orm.GatewayRuntimeService, session *db.SessionHelper) error {
	material, err := runtime_service.MakeEndpointMaterial(dao)
	if err != nil {
		return err
	}
	if material.ServiceGroupNamespace == "" || material.ServiceGroupName == "" {
		log.Errorf("invalid material:%+v maybe old, ignored", material)
		return nil
	}
	_, err = (*impl.domainBiz).TouchRuntimeDomain("", dao, material, nil, nil, session)
	if err != nil {
		return err
	}
	return nil
}

func (impl GatewayRuntimeServiceServiceImpl) clearService(dao *orm.GatewayRuntimeService, session *db.SessionHelper) error {
	runtimeSession, err := impl.runtimeDb.NewSession(session)
	if err != nil {
		return err
	}
	dao.InnerAddress = ""
	err = runtimeSession.Update(dao)
	if err != nil {
		return err
	}
	err = impl.clearDomain(dao, session)
	if err != nil {
		return err
	}
	err = (*impl.packageBiz).ClearRuntimeRoute(dao.Id)
	if err != nil {
		return err
	}
	err = (*impl.packageBiz).TryClearRuntimePackage(dao, session)
	if err != nil {
		return err
	}
	err = (*impl.apiBiz).ClearRuntimeApi(dao)
	if err != nil {
		return err
	}
	return nil
}

func (impl GatewayRuntimeServiceServiceImpl) DeleteRuntime(runtimeId string) (err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
		}
	}()
	if runtimeId == "" {
		return errors.New("runtimeId is empty")
	}
	var daos []orm.GatewayRuntimeService
	// get runtime service id
	daos, err = impl.runtimeDb.SelectByAny(&orm.GatewayRuntimeService{
		RuntimeId: runtimeId,
	})
	if err != nil {
		return
	}
	// clear all inner_addr and runtime domain
	for _, dao := range daos {
		err = impl.clearService(&dao, nil)
		if err != nil {
			return
		}
	}
	return nil
}

func (impl GatewayRuntimeServiceServiceImpl) GetRegisterAppInfo(projectId, env string) (resDto gw.RegisterAppsDto, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
		}
	}()
	if projectId == "" || env == "" {
		err = errors.New("projectId or env is empty")
		return
	}
	appMap := map[string]map[string]bool{}
	var runtimeServices []orm.GatewayRuntimeService
	az, err := impl.azDb.GetAz(&orm.GatewayAzInfo{
		ProjectId: projectId,
		Env:       env,
	})
	if err != nil {
		return
	}
	if az == "" {
		err = errors.New("find cluster failed")
		return
	}
	runtimeServices, err = impl.runtimeDb.SelectByAny(&orm.GatewayRuntimeService{
		ProjectId:   projectId,
		Workspace:   env,
		ClusterName: az,
	})
	if err != nil {
		return
	}
	for _, serviceDao := range runtimeServices {
		if serviceDao.InnerAddress == "" || serviceDao.AppName == "" ||
			serviceDao.ServiceName == "" {
			continue
		}
		appName := serviceDao.AppName
		svcName := serviceDao.ServiceName
		if appName == "" || svcName == "" {
			continue
		}
		serviceMap, exist := appMap[appName]
		if !exist {
			appMap[appName] = map[string]bool{}
			appMap[appName][svcName] = true
			continue
		}
		serviceMap[svcName] = true
	}
	resDto = gw.RegisterAppsDto{
		Apps: []gw.RegisterApp{},
	}
	for appName, services := range appMap {
		app := gw.RegisterApp{
			Name: appName,
		}
		for svcName := range services {
			app.Services = append(app.Services, svcName)
		}
		resDto.Apps = append(resDto.Apps, app)
	}
	return
}

func (impl GatewayRuntimeServiceServiceImpl) GetServiceRuntimes(projectId, env, app, service string) (result []orm.GatewayRuntimeService, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened, err:%+v", err)
		}
	}()
	if projectId == "" || env == "" || app == "" || service == "" {
		err = errors.New("params is empty")
		return
	}
	var daos []orm.GatewayRuntimeService
	az, err := impl.azDb.GetAz(&orm.GatewayAzInfo{
		ProjectId: projectId,
		Env:       env,
	})
	if err != nil {
		return
	}
	if az == "" {
		err = errors.New("find cluster failed")
		return
	}
	daos, err = impl.runtimeDb.SelectByAny(&orm.GatewayRuntimeService{
		ProjectId:   projectId,
		Workspace:   env,
		AppName:     app,
		ServiceName: service,
		ClusterName: az,
	})
	if err != nil {
		return
	}
	for _, dao := range daos {
		if dao.InnerAddress != "" {
			result = append(result, dao)
		}
	}
	return
}

// 获取指定服务的API前缀
func (impl GatewayRuntimeServiceServiceImpl) GetServiceApiPrefix(req *gw.ApiPrefixReqDto) (prefixs []string, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened: %+v", err)
		}
	}()
	dao, err := impl.runtimeDb.GetByAny(&orm.GatewayRuntimeService{
		ProjectId:   req.ProjectId,
		Workspace:   req.Env,
		ServiceName: req.Service,
		AppName:     req.App,
		RuntimeId:   req.RuntimeId,
	})
	if err != nil {
		return
	}
	if dao == nil {
		log.Errorf("runtime service not found, req:%+v", req)
		prefixs = []string{}
		return
	}
	apis, err := (*impl.apiBiz).GetRuntimeApis(dao.Id)
	if err != nil {
		return
	}
	pathPrefixMap := map[string]bool{}
	if len(apis) > 0 {
		pathPrefixMap["/"] = true
	}
	for _, api := range apis {
		path := api.Path
		if !strings.Contains(path, "{") {
			pathPrefixMap[path] = true
		}
		splitSlice := strings.SplitN(path, "/", 4)
		switch len(splitSlice) {
		case 0, 1, 2:
		default:
			path = strings.Join(splitSlice[:2], "/")
			if !strings.Contains(path, "{") {
				pathPrefixMap[path] = true
			}
			path = strings.Join(splitSlice[:3], "/")
			if !strings.Contains(path, "{") {
				pathPrefixMap[path] = true
			}
		}
	}
	for pathPrefix := range pathPrefixMap {
		prefixs = append(prefixs, pathPrefix)
	}
	sort.Strings(prefixs)
	return
}

func renderPlatformInfo(endpoints []diceyml.Endpoint, projectIdStr string) ([]diceyml.Endpoint, error) {
	var (
		projectName              *string
		left, right, platformTag = "${", "}", "platform."
		rePlaceholder            = regexp.MustCompile("\\$\\{(.+?)\\}")
	)

	for i, endpoint := range endpoints {
		if !rePlaceholder.MatchString(endpoint.Domain) {
			continue
		}
		res := rePlaceholder.FindAllString(endpoint.Domain, -1)
		for _, r := range res {
			placeholder, start, end, err := strutil.FirstCustomPlaceholder(r, left, right)
			if err != nil || start == end || !strings.HasPrefix(placeholder, platformTag) {
				return nil, fmt.Errorf("placeholder %s format error, %v", placeholder, err)
			}

			switch strings.Trim(placeholder, platformTag) {
			case "DICE_PROJECT_NAME":
				if projectName == nil {
					projectId, err := strconv.ParseUint(projectIdStr, 10, 64)
					if err != nil {
						return nil, fmt.Errorf("failed to parse project id %s, err: %v", projectIdStr, err)
					}
					project, err := bundle.Bundle.GetProject(projectId)
					if err != nil {
						return nil, fmt.Errorf("faield to get project, id: %d, err: %v", projectId, err)
					}
					projectName = &project.Name
				}
				endpoints[i].Domain = strings.ReplaceAll(endpoints[i].Domain, r, *projectName)
			default:
				return nil, fmt.Errorf("placeholder %s doesn't support", placeholder)
			}
		}
	}

	return endpoints, nil
}
