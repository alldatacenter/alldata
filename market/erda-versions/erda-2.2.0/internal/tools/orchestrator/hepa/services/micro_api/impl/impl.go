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
	"strings"
	"sync"

	"github.com/pkg/errors"
	log "github.com/sirupsen/logrus"

	"github.com/erda-project/erda-proto-go/core/hepa/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/bundle"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common"
	. "github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/config"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/assembler"
	gw "github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong"
	kongDto "github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong/dto"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
	db "github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/service"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/global"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/micro_api"
)

type GatewayApiServiceImpl struct {
	apiInPackageDb  db.GatewayApiInPackageService
	zoneInPackageDb db.GatewayZoneInPackageService
	packageDb       db.GatewayPackageService
	packageApiDb    db.GatewayPackageApiService
	serviceDb       db.GatewayServiceService
	routeDb         db.GatewayRouteService
	policyDb        db.GatewayPolicyService
	consumerDb      db.GatewayConsumerService
	pluginDb        db.GatewayPluginInstanceService
	apiDb           db.GatewayApiService
	consumerApiDb   db.GatewayConsumerApiService
	azDb            db.GatewayAzInfoService
	kongDb          db.GatewayKongInfoService
	runtimeDb       db.GatewayRuntimeServiceService
	globalBiz       *global.GatewayGlobalService
	kongAssembler   assembler.GatewayKongAssembler
	dbAssembler     assembler.GatewayDbAssembler
	reqCtx          context.Context
}

var once sync.Once

func NewGatewayApiServiceImpl() error {
	once.Do(
		func() {
			apiInPackageDb, _ := db.NewGatewayApiInPackageServiceImpl()
			serviceDb, _ := db.NewGatewayServiceServiceImpl()
			routeDb, _ := db.NewGatewayRouteServiceImpl()
			policyDb, _ := db.NewGatewayPolicyServiceImpl()
			consumerDb, _ := db.NewGatewayConsumerServiceImpl()
			pluginDb, _ := db.NewGatewayPluginInstanceServiceImpl()
			apiDb, _ := db.NewGatewayApiServiceImpl()
			kongDb, _ := db.NewGatewayKongInfoServiceImpl()
			azDb, _ := db.NewGatewayAzInfoServiceImpl()
			consumerApiDb, _ := db.NewGatewayConsumerApiServiceImpl()
			packageApiDb, _ := db.NewGatewayPackageApiServiceImpl()
			packageDb, _ := db.NewGatewayPackageServiceImpl()
			zoneInPackageDb, _ := db.NewGatewayZoneInPackageServiceImpl()
			runtimeDb, _ := db.NewGatewayRuntimeServiceServiceImpl()
			micro_api.Service = &GatewayApiServiceImpl{
				apiInPackageDb:  apiInPackageDb,
				serviceDb:       serviceDb,
				routeDb:         routeDb,
				policyDb:        policyDb,
				consumerDb:      consumerDb,
				pluginDb:        pluginDb,
				apiDb:           apiDb,
				consumerApiDb:   consumerApiDb,
				kongDb:          kongDb,
				azDb:            azDb,
				kongAssembler:   assembler.GatewayKongAssemblerImpl{},
				dbAssembler:     assembler.GatewayDbAssemblerImpl{},
				globalBiz:       &global.Service,
				packageApiDb:    packageApiDb,
				packageDb:       packageDb,
				zoneInPackageDb: zoneInPackageDb,
				runtimeDb:       runtimeDb,
			}
		})
	return nil
}

func (impl GatewayApiServiceImpl) Clone(ctx context.Context) micro_api.GatewayApiService {
	newService := impl
	newService.reqCtx = ctx
	return &newService
}

func (impl GatewayApiServiceImpl) verifyApiCreateParams(req *gw.ApiReqDto) *common.StandardResult {
	res := &common.StandardResult{Success: false}
	if req == nil || req.IsEmpty() {
		log.Errorf("invalid req[%+v]", req.ApiDto)
		return res.SetReturnCode(PARAMS_IS_NULL)
	}
	req.RegisterType = gw.RT_MANUAL
	var consumer *orm.GatewayConsumer
	var err error
	if req.RuntimeId == "" {
		if req.ConsumerId == "" {
			log.Error("empty consumerId")
			return res.SetReturnCode(PARAMS_IS_NULL)
		}
		consumer, err = impl.consumerDb.GetById(req.ConsumerId)
		if err != nil {
			log.Errorf("error happend:%+v", err)
			return res.SetReturnCode(UNKNOW_ERROR)
		}
		if consumer == nil {
			return res.SetReturnCode(CONSUMER_NOT_EXIST)
		}
		req.Env = consumer.Env
	}
	if req.RedirectType == gw.RT_URL {
		req.RedirectAddr = req.RedirectAddr + "/" + strings.TrimPrefix(req.RedirectPath, "/")
	} else if req.RuntimeId == "" || req.DiceApp == "" || req.DiceService == "" {
		return res.SetErrorInfo(&common.ErrInfo{Msg: "未选择对应的部署分支"})
	}
	return res.SetSuccessAndData(consumer)
}

func (impl GatewayApiServiceImpl) acquirePolicies(apiPolices []string) map[string]orm.GatewayPolicy {
	res := map[string]orm.GatewayPolicy{}

	for _, policyId := range apiPolices {
		policy, err := impl.policyDb.GetById(policyId)
		if err != nil || policy == nil {
			log.Errorf("get policy by id[%s] failed[%+v]", policyId, err)
			continue
		}
		res[policy.PluginName] = *policy
	}

	return res
}

func (impl GatewayApiServiceImpl) CreateUnityPackageShadowApi(apiId, projectId, env, az string) error {
	api, err := impl.apiDb.GetById(apiId)
	if err != nil {
		return err
	}
	pack, err := impl.packageDb.GetByAny(&orm.GatewayPackage{
		Scene:           orm.UNITY_SCENE,
		DiceProjectId:   projectId,
		DiceEnv:         env,
		DiceClusterName: az,
	})
	if err != nil {
		return err
	}
	if pack == nil {
		log.Warnf("unity package not found, projectId:%s, env:%s, az:%s", projectId, env, az)
		return nil
	}
	err = impl.packageApiDb.Insert(&orm.GatewayPackageApi{
		PackageId:    pack.Id,
		ApiPath:      api.ApiPath,
		Method:       api.Method,
		RedirectAddr: api.RedirectAddr,
		Description:  api.Description,
		DiceApp:      api.DiceApp,
		DiceService:  api.DiceService,
		Origin:       string(gw.FROM_SHADOW),
		DiceApiId:    apiId,
		RedirectType: gw.RT_URL,
	})
	if err != nil {
		return err
	}
	err = impl.apiInPackageDb.Insert(&orm.GatewayApiInPackage{
		DiceApiId: apiId,
		PackageId: pack.Id,
	})
	if err != nil {
		return err
	}
	return nil
}

func (impl GatewayApiServiceImpl) CreateUpstreamBindApi(consumer *orm.GatewayConsumer, appName, srvName, runtimeServiceId string, upstreamApi *orm.GatewayUpstreamApi, aliasPath string) (string, error) {
	if upstreamApi == nil {
		return "", errors.New(ERR_INVALID_ARG)
	}
	var apiId string
	if runtimeServiceId == "" {
		if consumer == nil {
			return "", errors.New("consumer is nil")
		}
		kongInfo, err := impl.kongDb.GetKongInfo(&orm.GatewayKongInfo{
			ProjectId: consumer.ProjectId,
			Az:        consumer.Az,
			Env:       consumer.Env,
		})
		if err != nil {
			return "", err
		}
		path := strings.ToLower("/"+kongInfo.ProjectName) + aliasPath + upstreamApi.GatewayPath
		apiId, retCode, err := impl.createApi(consumer, &gw.ApiDto{
			Path:           path,
			Method:         upstreamApi.Method,
			RedirectAddr:   upstreamApi.Address + upstreamApi.Path,
			RedirectType:   gw.RT_URL,
			DiceApp:        appName,
			DiceService:    srvName,
			Env:            consumer.Env,
			ProjectId:      consumer.ProjectId,
			IsInner:        upstreamApi.IsInner,
			OuterNetEnable: true,
			RegisterType:   gw.RT_AUTO,
		}, nil, upstreamApi.Id)
		if retCode == API_EXIST {
			log.Warnf("api already existed, err:%+v", err)
			return apiId, nil
		}
		if err != nil {
			return apiId, err
		}
		err = impl.CreateUnityPackageShadowApi(apiId, consumer.ProjectId, consumer.Env, consumer.Az)
		if err != nil {
			return apiId, err
		}
		return apiId, nil
	}
	apiId, _, err := impl.CreateRuntimeApi(&gw.ApiDto{
		Path:             upstreamApi.GatewayPath,
		Method:           upstreamApi.Method,
		RedirectAddr:     upstreamApi.Address + upstreamApi.Path,
		RedirectType:     gw.RT_URL,
		DiceApp:          appName,
		DiceService:      srvName,
		RuntimeServiceId: runtimeServiceId,
		IsInner:          upstreamApi.IsInner,
		OuterNetEnable:   true,
		UpstreamApiId:    upstreamApi.Id,
		RegisterType:     gw.RT_AUTO,
	})
	if err != nil {
		return apiId, err
	}
	return apiId, nil
}

func (impl GatewayApiServiceImpl) UpdateUpstreamBindApi(consumer *orm.GatewayConsumer, appName, serviceName string, upstreamApi *orm.GatewayUpstreamApi, aliasPath string) error {
	if upstreamApi == nil || len(upstreamApi.ApiId) == 0 {
		return errors.Errorf("invalid arg: upstreamApi:%+v", upstreamApi)
	}
	gatewayApi, err := impl.apiDb.GetById(upstreamApi.ApiId)
	if err != nil {
		return err
	}
	if gatewayApi == nil {
		return errors.Errorf("gatewayApi [%s] not exists", upstreamApi.ApiId)
	}
	// update restriction
	if gatewayApi.Method != upstreamApi.Method {
		return errors.Errorf("can't change api method: from %s to %s", gatewayApi.Method, upstreamApi.Method)
	}
	outerNetEnable := true
	if gatewayApi.NetType == gw.NT_IN {
		outerNetEnable = false
	}
	if gatewayApi.RuntimeServiceId == "" {
		if consumer == nil {
			return errors.New("consumer is nil")
		}
		kongInfo, err := impl.kongDb.GetKongInfo(&orm.GatewayKongInfo{
			ProjectId: consumer.ProjectId,
			Az:        consumer.Az,
			Env:       consumer.Env,
		})
		if err != nil {
			return err
		}
		path := strings.ToLower("/"+kongInfo.ProjectName) + aliasPath + upstreamApi.GatewayPath
		_, _, err = impl.updateApi(gatewayApi, consumer, &gw.ApiDto{
			Path:           path,
			Env:            consumer.Env,
			Method:         gatewayApi.Method,
			RegisterType:   gw.RT_AUTO,
			RedirectAddr:   upstreamApi.Address + upstreamApi.Path,
			RedirectType:   gw.RT_URL,
			OuterNetEnable: outerNetEnable,
			DiceApp:        appName,
			DiceService:    serviceName,
			ProjectId:      consumer.ProjectId,
			IsInner:        upstreamApi.IsInner,
			NeedAuth:       gatewayApi.NeedAuth,
			Description:    gatewayApi.Description,
		}, nil, upstreamApi.Id)
		if err != nil {
			return err
		}
		return nil
	}
	_, _, err = impl.updateRuntimeApi(gatewayApi, &gw.ApiDto{
		Path:           upstreamApi.GatewayPath,
		Method:         gatewayApi.Method,
		RegisterType:   gw.RT_AUTO,
		RedirectAddr:   upstreamApi.Address + upstreamApi.Path,
		RedirectType:   gw.RT_URL,
		OuterNetEnable: outerNetEnable,
		IsInner:        upstreamApi.IsInner,
		DiceApp:        appName,
		DiceService:    serviceName,
		Description:    gatewayApi.Description,
	})
	if err != nil {
		return err
	}
	return nil
}

func (impl GatewayApiServiceImpl) DeleteUpstreamBindApi(upstreamApi *orm.GatewayUpstreamApi) error {
	if upstreamApi == nil || len(upstreamApi.ApiId) == 0 {
		return errors.Errorf("invalid arg: upstreamApi[%+v]", upstreamApi)
	}
	_, err := impl.deleteApi(upstreamApi.ApiId)
	if err != nil {
		return err
	}
	return nil
}

func (impl GatewayApiServiceImpl) apiPathExist(consumer *orm.GatewayConsumer, path string, method string) (bool, string, error) {
	cond := &orm.GatewayApi{
		ApiPath: path,
		Method:  method,
	}
	if consumer != nil {
		cond.ConsumerId = consumer.Id
		cond.SetMustCondCols("consumer_id", "api_path", "method")
	} else {
		cond.SetMustCondCols("api_path", "method")
	}
	exist, err := impl.apiDb.GetByAny(cond)
	if err != nil {
		return false, "", err
	}
	if exist == nil {
		return false, "", nil
	}
	// case sensitive
	if exist.ApiPath == path {
		return true, exist.Id, nil
	}
	return false, "", nil
}

const varSlot = ""

const (
	processStrStatus = iota
	processVarStatus
)

func (impl GatewayApiServiceImpl) pathVariableSplit(path string) ([]string, []string, error) {
	status := processStrStatus
	var rawPaths []string
	var vars []string
	var items []byte
	for i := 0; i < len(path); i++ {
		c := path[i]
		if status == processStrStatus {
			if c == '{' {
				status = processVarStatus
				if len(items) > 0 {
					rawPaths = append(rawPaths, string(items))
					items = nil
				}
			} else if c == '}' {
				return nil, nil, errors.Errorf("invalid path:%s", path)
			} else {
				items = append(items, c)
			}
		} else if status == processVarStatus {
			if c == '{' {
				return nil, nil, errors.Errorf("invalid path:%s", path)
			} else if c == '}' {
				status = processStrStatus
				if len(items) > 0 {
					vars = append(vars, string(items))
					rawPaths = append(rawPaths, varSlot)
					items = nil
				} else {
					return nil, nil, errors.Errorf("invalid path:%s", path)
				}
			} else {
				items = append(items, c)
			}
		}
	}
	if status != processStrStatus {
		return nil, nil, errors.Errorf("invalid path:%s", path)
	}
	if len(items) > 0 {
		rawPaths = append(rawPaths, string(items))
	}
	return rawPaths, vars, nil
}

func (impl GatewayApiServiceImpl) pathVariableReplace(rawPaths []string, vars *[]string) (string, error) {
	if len(*vars) == 0 {
		return "", errors.New("invalid vars")
	}
	varIndex := 0
	varRegex := `[^/]+`
	for i, item := range rawPaths {
		if item == varSlot {
			varValue := (*vars)[varIndex]
			colonIndex := strings.Index(varValue, ":")
			if colonIndex != -1 {
				varRegex = varValue[colonIndex+1:]
				varValue = varValue[:colonIndex]
			}
			(*vars)[varIndex] = varValue
			rawPaths[i] = fmt.Sprintf(`(?<%s>%s)`, varValue, varRegex)
			varIndex++
		}
		if varIndex > len(*vars) {
			return "", errors.Errorf("invalid varIndex[%d] of vars[%+v]", varIndex, vars)
		}
	}
	return strings.Join(rawPaths, ""), nil
}

func (impl GatewayApiServiceImpl) pathAdjust(dto *gw.ApiDto) (bool, gw.ApiDto, string, error) {
	if strings.HasSuffix(dto.Path, "/") {
		validPath := strings.TrimSuffix(dto.Path, "/")
		dto.Path = validPath
	}
	dto.Path = strings.Replace(dto.Path, "//", "/", -1)
	dbDto := *dto
	if dbDto.Path == "" {
		dbDto.Path = "/"
	}
	isRegexPath := false
	findBegin := strings.Index(dto.RedirectAddr, "://")
	if findBegin < 0 {
		return isRegexPath, dbDto, "", errors.Errorf("invalid dto:%+v", dto)
	}
	firstSlash := strings.Index(dto.RedirectAddr[findBegin+3:], "/")
	servicePath := ""
	if firstSlash > -1 {
		servicePath = dto.RedirectAddr[findBegin+3+firstSlash:]
	} else {
		firstSlash = 0
	}
	rawPaths, vars, err := impl.pathVariableSplit(dto.Path)
	if err != nil {
		return isRegexPath, dbDto, "", err
	}
	serviceRawPaths, serviceVars, err := impl.pathVariableSplit(servicePath)
	if err != nil {
		return isRegexPath, dbDto, "", err
	}
	if len(vars) > 0 {
		isRegexPath = true
		adjustPath, err := impl.pathVariableReplace(rawPaths, &vars)
		if err != nil {
			return isRegexPath, dbDto, "", err
		}
		dto.Path = adjustPath
	}
	if len(serviceVars) > 0 {
		varIndex := 0
		for i, item := range serviceRawPaths {
			if item == varSlot {
				varValue := serviceVars[varIndex]
				colonIndex := strings.Index(varValue, ":")
				if colonIndex != -1 {
					varValue = varValue[:colonIndex]
				}
				serviceRawPaths[i] = fmt.Sprintf(`{%s}`, varValue)
				serviceVars[varIndex] = varValue
				varIndex++
			}
			if varIndex > len(serviceVars) {
				return isRegexPath, dbDto, "", errors.Errorf("invalid servicePath[%s]", servicePath)
			}
		}
		servicePath = strings.Join(serviceRawPaths, "")
		for _, serviceVarItem := range serviceVars {
			exist := false
			for _, varItem := range vars {
				if varItem == serviceVarItem {
					exist = true
					break
				}
			}
			if !exist {
				return isRegexPath, dbDto, "", errors.Errorf("service var[%s] not exists in vars[%+v] of path[%s]",
					serviceVarItem, vars, dto.Path)
			}
		}
		dto.RedirectAddr = dto.RedirectAddr[:findBegin+3+firstSlash]
		dbDto.RedirectAddr = dto.RedirectAddr + servicePath
		return isRegexPath, dbDto, servicePath, nil
	}
	return isRegexPath, dbDto, "", nil
}

func (impl GatewayApiServiceImpl) GetRuntimeApis(runtimeServiceId string, registerType ...string) ([]gw.ApiDto, error) {
	cond := &orm.GatewayApi{
		RuntimeServiceId: runtimeServiceId,
	}
	if len(registerType) > 0 {
		cond.RegisterType = registerType[0]
	}
	daos, err := impl.apiDb.SelectByAny(cond)
	if err != nil {
		return nil, err
	}
	var res []gw.ApiDto
	for _, dao := range daos {
		dto := gw.ApiDto{
			Method: dao.Method,
			DaoId:  dao.Id,
		}
		dto.Path = strings.TrimPrefix(dao.ApiPath, "/"+dao.RuntimeServiceId)
		if dto.Path == "" {
			dto.Path = "/"
		}
		res = append(res, dto)
	}
	return res, nil
}

func (impl GatewayApiServiceImpl) CreateRuntimeApi(dto *gw.ApiDto, session ...*db.SessionHelper) (string, StandardErrorCode, error) {
	var err error
	dto.OuterNetEnable = true
	apiDb := impl.apiDb
	routeDb := impl.routeDb
	serviceDb := impl.serviceDb
	pluginDb := impl.pluginDb
	runtimeDb := impl.runtimeDb
	packageAPIDb := impl.packageApiDb
	if len(session) > 0 {
		apiDb, err = impl.apiDb.NewSession(session[0])
		if err != nil {
			return "", PARAMS_IS_NULL, err
		}
		routeDb, err = impl.routeDb.NewSession(session[0])
		if err != nil {
			return "", PARAMS_IS_NULL, err
		}
		serviceDb, err = impl.serviceDb.NewSession(session[0])
		if err != nil {
			return "", PARAMS_IS_NULL, err
		}
		pluginDb, err = impl.pluginDb.NewSession(session[0])
		if err != nil {
			return "", PARAMS_IS_NULL, err
		}
		runtimeDb, err = impl.runtimeDb.NewSession(session[0])
		if err != nil {
			return "", PARAMS_IS_NULL, err
		}
		packageAPIDb, err = impl.packageApiDb.NewSession(session[0])
		if err != nil {
			return "", PARAMS_IS_NULL, err
		}
	}
	var runtimeService *orm.GatewayRuntimeService
	if dto.RuntimeServiceId == "" {
		runtimeService, err = impl.getRuntimeService(dto.RuntimeId, dto.DiceApp, dto.DiceService, runtimeDb)
	} else {
		runtimeService, err = runtimeDb.Get(dto.RuntimeServiceId)
	}
	if err != nil {
		return "", PARAMS_IS_NULL, err
	}
	if runtimeService == nil {
		return "", PARAMS_IS_NULL, errors.Errorf("find runtime service failed, id:%s", dto.RuntimeServiceId)
	}
	auditCtx := map[string]interface{}{}
	defer func() {
		method := dto.Method
		if method == "" {
			method = "all"
		}
		auditCtx["path"] = strings.Join(strings.Split(dto.Path, "/")[2:], "/")
		auditCtx["method"] = method
		audit := common.MakeAuditInfo(impl.reqCtx, common.ScopeInfo{
			ProjectId:   runtimeService.ProjectId,
			Workspace:   runtimeService.Workspace,
			AppId:       runtimeService.AppId,
			ServiceName: runtimeService.ServiceName,
			RuntimeName: runtimeService.RuntimeName,
		}, apistructs.CreateServiceApiTemplate, nil, auditCtx)
		if audit != nil {
			if err == nil {
				audit.Result = apistructs.SuccessfulResult
			} else {
				//				audit.Result = apistructs.FailureResult
				//				audit.ErrorMsg = errors.Cause(err).Error()
				return
			}
			err = bundle.Bundle.CreateAuditEvent(&apistructs.AuditCreateRequest{Audit: *audit})
			if err != nil {
				log.Errorf("create audit failed, err:%+v", err)
			}
		}
	}()
	dto.RuntimeServiceId = runtimeService.Id
	dto.ProjectId = runtimeService.ProjectId
	dto.Env = runtimeService.Workspace
	kongInfo, err := impl.kongDb.GetKongInfo(&orm.GatewayKongInfo{
		Az:        runtimeService.ClusterName,
		ProjectId: runtimeService.ProjectId,
		Env:       runtimeService.Workspace,
	})
	if err != nil {
		return "", PARAMS_IS_NULL, err
	}
	kongAdapter := kong.NewKongAdapter(kongInfo.KongAddr)
	dto.Hosts = append(dto.Hosts, kong.InnerHost)
	if dto.RedirectType == gw.RT_SERVICE {
		dto.RedirectAddr = runtimeService.InnerAddress + dto.RedirectPath
		if !strings.HasPrefix(dto.RedirectAddr, "http://") {
			dto.RedirectAddr = "http://" + dto.RedirectAddr
		}
	}
	pathPrefix, err := (*impl.globalBiz).GetRuntimeServicePrefix(runtimeService)
	if err != nil {
		return "", PARAMS_IS_NULL, err
	}
	dto.Path = pathPrefix + dto.Path
	isRegexPath, dbDto, serviceRewritePath, err := impl.pathAdjust(dto)
	if err != nil {
		return "", INVALID_PATH, err
	}
	exist, existId, err := impl.apiPathExist(nil, dto.Path, dto.Method)
	if err != nil {
		return "", PARAMS_IS_NULL, err
	}
	if exist {
		return existId, API_EXIST, errors.Errorf("api path[%s] method[%s] confilct", dto.Path, dto.Method)
	}
	ret := UNKNOW_ERROR
	var serviceResp *kongDto.KongServiceRespDto
	var routeResp *kongDto.KongRouteRespDto
	var gatewayApi *orm.GatewayApi
	var gatewayService *orm.GatewayService
	var gatewayRoute *orm.GatewayRoute
	gatewayApi, err = impl.dbAssembler.BuildGatewayApi(dbDto, "", nil, "")
	if err != nil {
		goto errorHappened
	}
	err = apiDb.Insert(gatewayApi)
	if err != nil {
		goto errorHappened
	}
	{
		ret = CREATE_API_SERVICE_FAIL
		var serviceReq *kongDto.KongServiceReqDto
		serviceReq, err = impl.kongAssembler.BuildKongServiceReq("", dto)
		if err != nil {
			goto errorHappened
		}
		serviceResp, err = kongAdapter.CreateOrUpdateService(serviceReq)
		if err != nil {
			goto errorHappened
		}
		gatewayService, err = impl.dbAssembler.Resp2GatewayServiceByApi(serviceResp, dbDto, gatewayApi.Id)
		if err != nil {
			goto errorHappened
		}
		err = serviceDb.Insert(gatewayService)
		if err != nil {
			goto errorHappened
		}
	}
	{
		ret = CREATE_API_ROUTE_FAIL
		var routeReq *kongDto.KongRouteReqDto
		routeReq, err = impl.kongAssembler.BuildKongRouteReq("", dto, serviceResp.Id, isRegexPath)
		if err != nil {
			goto errorHappened
		}
		packageApi, err := packageAPIDb.GetByAny(&orm.GatewayPackageApi{DiceApiId: gatewayApi.Id})
		if err != nil {
			log.WithError(err).Warnf("failed to packageAPIDb.GetByAny(&orm.GatewayPackageApi{DiceApiId: %s})", gatewayApi.Id)
		}
		if packageApi == nil {
			log.WithError(errors.New("not found")).Warnf("failed to packageAPIDb.GetByAny(&orm.GatewayPackageApi{DiceApiId: %s})", gatewayApi.Id)
		}
		if packageApi != nil {
			routeReq.AddTag("package_api_id", packageApi.Id)
		}
		routeResp, err = kongAdapter.CreateOrUpdateRoute(routeReq)
		if err != nil {
			goto errorHappened
		}
		gatewayRoute, err = impl.dbAssembler.Resp2GatewayRouteByAPi(routeResp, gatewayService.Id, gatewayApi.Id)
		if err != nil {
			goto errorHappened
		}
		err = routeDb.Insert(gatewayRoute)
		if err != nil {
			goto errorHappened
		}
	}
	{
		ret = CREATE_API_PLUGIN_FAIL
		policies := []orm.GatewayPolicy{}
		if len(serviceRewritePath) > 0 {
			configJson, err := json.Marshal(gw.PathVariableConfig{
				RequestRegex: dto.Path,
				RewritePath:  serviceRewritePath,
				Carrier:      "ROUTE",
			})
			if err != nil {
				goto errorHappened
			}
			policies = append(policies, orm.GatewayPolicy{
				PluginName: "path-variable",
				Config:     configJson,
			})
		}
		if config.ServerConf.HasRouteInfo {
			configJson, err := json.Marshal(gw.RouteInfoConfig{
				ProjectId: dbDto.ProjectId,
				Workspace: strings.ToLower(dbDto.Env),
				App:       strings.ToLower(dbDto.DiceApp),
				Service:   strings.ToLower(dbDto.DiceService),
				ApiPath:   dbDto.Path,
				Carrier:   "ROUTE",
			})
			if err != nil {
				goto errorHappened
			}
			policies = append(policies, orm.GatewayPolicy{
				PluginName: "set-route-info",
				Config:     configJson,
			})
		}
		for _, policy := range policies {
			var pluginReq *kongDto.KongPluginReqDto
			var pluginResp *kongDto.KongPluginRespDto
			var pluginInstance *orm.GatewayPluginInstance
			pluginReq, err = impl.kongAssembler.BuildKongPluginReqDto("", &policy, serviceResp.Id, routeResp.Id, "")
			if err != nil {
				goto errorHappened
			}
			pluginResp, err = kongAdapter.AddPlugin(pluginReq)
			if err != nil {
				goto errorHappened
			}
			if pluginResp == nil {
				continue
			}
			pluginResp.PolicyId = policy.Id
			pluginParams := assembler.PluginParams{
				PolicyId:   pluginResp.PolicyId,
				GroupId:    "",
				ServiceId:  gatewayService.Id,
				RouteId:    gatewayRoute.Id,
				ConsumerId: "",
				ApiId:      gatewayApi.Id,
			}
			pluginInstance, err = impl.dbAssembler.Resp2GatewayPluginInstance(pluginResp, pluginParams)
			if err != nil {
				goto errorHappened
			}
			err = pluginDb.Insert(pluginInstance)
			if err != nil {
				goto errorHappened
			}
		}
	}
	return gatewayApi.Id, ret, nil
errorHappened:
	var kerr error
	if gatewayApi.Id != "" {
		kerr = apiDb.RealDeleteById(gatewayApi.Id)
	}
	if serviceResp != nil {
		kerr = kongAdapter.DeleteService(serviceResp.Id)
	}
	if routeResp != nil {
		kerr = kongAdapter.DeleteRoute(routeResp.Id)
	}
	if kerr != nil {
		log.Errorf("delete failed, err:%+v", kerr)
	}
	log.Errorf("error Happend, %+v:", err)
	return "", ret, err

}

func (impl GatewayApiServiceImpl) getRuntimeService(runtimeId, appName, serviceName string, runtimeSession db.GatewayRuntimeServiceService) (*orm.GatewayRuntimeService, error) {
	dao, err := runtimeSession.GetByAny(&orm.GatewayRuntimeService{
		RuntimeId:   runtimeId,
		AppName:     appName,
		ServiceName: serviceName,
	})
	if err != nil {
		return nil, err
	}
	if dao == nil {
		return nil, errors.New("this runtime's deployment is not yet ready, it may be deploying or cancelled")
	}
	return dao, nil
}

func (impl GatewayApiServiceImpl) createApi(consumer *orm.GatewayConsumer, dto *gw.ApiDto, optionDto *gw.ApiReqOptionDto, upstreamApiId ...string) (string, StandardErrorCode, error) {
	if consumer == nil || dto == nil || len(dto.RedirectAddr) == 0 {
		return "", PARAMS_IS_NULL, errors.Errorf("invalid consumer[%+v] or dto[%+v]",
			consumer, dto)
	}
	ret := UNKNOW_ERROR
	var err error
	var serviceResp *kongDto.KongServiceRespDto
	var routeResp *kongDto.KongRouteRespDto
	gatewayPolicies := []orm.GatewayPolicy{}
	pluginRespList := []kongDto.KongPluginRespDto{}
	// all api OuterNetEnable
	dto.OuterNetEnable = true
	// create zone if not exist
	var zoneId string
	kongInfo, err := impl.kongDb.GetKongInfo(&orm.GatewayKongInfo{
		Az:        consumer.Az,
		ProjectId: consumer.ProjectId,
		Env:       consumer.Env,
	})
	if err != nil {
		return "", PARAMS_IS_NULL, err
	}
	kongAdapter := kong.NewKongAdapter(kongInfo.KongAddr)
	dto.Hosts = append(dto.Hosts, kong.InnerHost)
	if dto.OuterNetEnable {
		dto.Hosts = append(dto.Hosts, kongInfo.Endpoint)
	}
	isRegexPath, dbDto, serviceRewritePath, err := impl.pathAdjust(dto)
	if err != nil {
		return "", INVALID_PATH, err
	}
	exist, existId, err := impl.apiPathExist(consumer, dto.Path, dto.Method)
	if err != nil {
		return "", PARAMS_IS_NULL, err
	}
	if exist {
		return existId, API_EXIST, errors.Errorf("api path[%s] method[%s] conflict", dto.Path,
			dto.Method)
	}
	// 1 创建service
	{
		serviceReq, err := impl.kongAssembler.BuildKongServiceReq("", dto)
		if err != nil {
			ret = CREATE_API_SERVICE_FAIL
			goto errorHappened
		}

		serviceResp, err = kongAdapter.CreateOrUpdateService(serviceReq)
		if err != nil {
			ret = CREATE_API_SERVICE_FAIL
			goto errorHappened
		}
	}
	// 2 创建route
	{
		routeReq, err := impl.kongAssembler.BuildKongRouteReq("", dto, serviceResp.Id, isRegexPath)
		if err != nil {
			ret = CREATE_API_ROUTE_FAIL
			goto errorHappened
		}
		for _, id := range upstreamApiId {
			routeReq.AddTag("upstream_api_id", id)
		}
		routeResp, err = kongAdapter.CreateOrUpdateRoute(routeReq)
		if err != nil {
			ret = CREATE_API_ROUTE_FAIL
			goto errorHappened
		}
	}
	// 3 根据group信息+请求信息增加插件
	{
		needAuth := false
		var policies []orm.GatewayPolicy
		var basicPolicies []orm.GatewayPolicy
		// 3.0 请求信息更新
		if optionDto != nil {
			apiPolices := optionDto.Policies
			policyMap := impl.acquirePolicies(apiPolices)
			for _, policy := range policyMap {
				if policy.Category == "auth" {
					// 只能有一种auth策略
					if needAuth {
						continue
					}
					needAuth = true
				}
				policies = append(policies, policy)
			}
		}
		if needAuth {
			dbDto.NeedAuth = 1
		} else {
			dbDto.NeedAuth = 0
		}
		// 3.1 获取基本策略
		basicPolicies, err = impl.policyDb.SelectByCategory("basic")
		// 3.2 基本策略添加，acl根据是否开启鉴权添加
		for _, policy := range basicPolicies {
			if !needAuth && (policy.PluginName == "acl") {
				continue
			}
			if policy.PluginName == "acl" {
				objMap := map[string]interface{}{}
				err = json.Unmarshal([]byte(policy.Config), &objMap)
				if err != nil {
					goto errorHappened
				}
				objMap["whitelist"] = consumer.Id
				var mapJson []byte
				mapJson, err = json.Marshal(objMap)
				if err != nil {
					goto errorHappened
				}
				policy.Config = mapJson
			}
			policies = append(policies, policy)
		}
		if len(serviceRewritePath) > 0 {
			configJson, err := json.Marshal(gw.PathVariableConfig{
				RequestRegex: dto.Path,
				RewritePath:  serviceRewritePath,
				Carrier:      "ROUTE",
			})
			if err != nil {
				goto errorHappened
			}
			policies = append(policies, orm.GatewayPolicy{
				PluginName: "path-variable",
				Config:     configJson,
			})
		}
		if config.ServerConf.HasRouteInfo {
			configJson, err := json.Marshal(gw.RouteInfoConfig{
				ProjectId: dbDto.ProjectId,
				Workspace: strings.ToLower(dbDto.Env),
				App:       strings.ToLower(dbDto.DiceApp),
				Service:   strings.ToLower(dbDto.DiceService),
				ApiPath:   dbDto.Path,
				Carrier:   "ROUTE",
			})
			if err != nil {
				goto errorHappened
			}
			policies = append(policies, orm.GatewayPolicy{
				PluginName: "set-route-info",
				Config:     configJson,
			})
		}
		// 3.3 增加插件
		for _, policy := range policies {
			if policy.PluginName == "oauth2" {
				_ = kongAdapter.TouchRouteOAuthMethod(routeResp.Id)
			}
			var pluginReq *kongDto.KongPluginReqDto
			var pluginResp *kongDto.KongPluginRespDto
			pluginReq, err = impl.kongAssembler.BuildKongPluginReqDto("", &policy, serviceResp.Id, routeResp.Id, "")
			if err != nil {
				ret = CREATE_API_PLUGIN_FAIL
				goto errorHappened
			}
			pluginResp, err = kongAdapter.AddPlugin(pluginReq)
			if err != nil {
				ret = CREATE_API_PLUGIN_FAIL
				goto errorHappened
			}
			if pluginResp == nil {
				continue
			}
			if optionDto != nil {
				for _, reqPolicyId := range optionDto.Policies {
					if reqPolicyId == policy.Id {
						gatewayPolicies = append(gatewayPolicies, policy)
						break
					}
				}
			}
			pluginResp.PolicyId = policy.Id
			pluginRespList = append(pluginRespList, *pluginResp)
		}
		if dto.IsInner == 1 {
			pluginReq := &kongDto.KongPluginReqDto{
				Name:    "host-check",
				RouteId: routeResp.Id,
				Config: map[string]interface{}{
					"allow_host": gw.INNER_HOSTS,
				},
			}
			_, err = kongAdapter.CreateOrUpdatePlugin(pluginReq)
			if err != nil {
				ret = CREATE_API_PLUGIN_FAIL
				goto errorHappened
			}
		}
	}
	// 4 相关信息入库
	{
		var apiId string
		apiId, err = impl.saveToDb(dbDto, consumer, serviceResp, routeResp, gatewayPolicies, pluginRespList, zoneId, upstreamApiId...)
		if err != nil {
			goto errorHappened
		}
		return apiId, ret, nil
	}
errorHappened:
	return "", ret, err
}

func (impl GatewayApiServiceImpl) CreateApi(req *gw.ApiReqDto) (apiId string, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened:%+v", err)
		}
	}()
	// 参数校验
	verifyRes := impl.verifyApiCreateParams(req)
	if !verifyRes.Success {
		err = errors.New(verifyRes.Err.Msg)
		return
	}
	var consumer *orm.GatewayConsumer
	if req.ConsumerId != "" {
		var ok bool
		consumer, ok = verifyRes.Data.(*orm.GatewayConsumer)
		if !ok {
			err = errors.New("transfer to GatewayConsumer failed")
			return
		}
	}
	if req.RuntimeId == "" {
		apiId, _, err = impl.createApi(consumer, req.ApiDto, req.ApiReqOptionDto)
		if err != nil {
			return
		}
	} else {
		apiId, _, err = impl.CreateRuntimeApi(req.ApiDto)
		if err != nil {
			return
		}
	}
	return
}

func (impl GatewayApiServiceImpl) saveToDb(dto gw.ApiDto, consumer *orm.GatewayConsumer, serviceResp *kongDto.KongServiceRespDto, routeResp *kongDto.KongRouteRespDto, gatewayPolicies []orm.GatewayPolicy, pluginRespList []kongDto.KongPluginRespDto, zoneId string, upstreamApiId ...string) (string, error) {

	var gatewayApi *orm.GatewayApi = nil
	var gatewayService *orm.GatewayService = nil
	var gatewayRoute *orm.GatewayRoute = nil
	gatewayApi, err := impl.dbAssembler.BuildGatewayApi(dto, consumer.Id, gatewayPolicies, zoneId, upstreamApiId...)
	if err != nil {
		return "", errors.WithStack(err)
	}
	err = impl.apiDb.Insert(gatewayApi)
	if err != nil {
		return "", errors.WithStack(err)
	}
	gatewayService, err = impl.dbAssembler.Resp2GatewayServiceByApi(serviceResp, dto, gatewayApi.Id)
	if err != nil {
		return "", errors.WithStack(err)
	}
	err = impl.serviceDb.Insert(gatewayService)
	if err != nil {
		return "", errors.WithStack(err)
	}

	gatewayRoute, err = impl.dbAssembler.Resp2GatewayRouteByAPi(routeResp, gatewayService.Id, gatewayApi.Id)
	if err != nil {
		return "", errors.WithStack(err)
	}
	err = impl.routeDb.Insert(gatewayRoute)
	if err != nil {
		return "", errors.WithStack(err)
	}

	for _, respDto := range pluginRespList {
		pluginParams := assembler.PluginParams{
			PolicyId:   respDto.PolicyId,
			GroupId:    "",
			ServiceId:  gatewayService.Id,
			RouteId:    gatewayRoute.Id,
			ConsumerId: "",
			ApiId:      gatewayApi.Id,
		}
		pluginInstance, err := impl.dbAssembler.Resp2GatewayPluginInstance(&respDto, pluginParams)
		if err != nil {
			return "", errors.WithStack(err)
		}

		err = impl.pluginDb.Insert(pluginInstance)
		if err != nil {
			return "", errors.WithStack(err)
		}
	}
	err = impl.consumerApiDb.Insert(&orm.GatewayConsumerApi{
		ConsumerId: consumer.Id,
		ApiId:      gatewayApi.Id,
	})
	if err != nil {
		return "", errors.WithStack(err)
	}
	return gatewayApi.Id, nil
}

func (impl GatewayApiServiceImpl) verifyGetApiInfosParams(orgId string, projectId string, env string) *common.StandardResult {
	res := &common.StandardResult{Success: false}
	if len(orgId) == 0 || len(projectId) == 0 {
		log.Errorf("invalid arg orgId[%s] projectId[%s]", orgId, projectId)
		return res.SetReturnCode(PARAMS_IS_NULL)
	}
	az, err := impl.azDb.GetAz(&orm.GatewayAzInfo{
		ProjectId: projectId,
		Env:       env,
	})
	if err != nil {
		log.Error(err)
		return res.SetReturnCode(CLUSTER_NOT_EXIST)
	}
	consumer, err := impl.consumerDb.GetDefaultConsumer(&orm.GatewayConsumer{
		OrgId:     orgId,
		ProjectId: projectId,
		Env:       env,
		Az:        az,
	})
	if err != nil {
		log.Errorf("error happened[%+v]", err)
		return res.SetReturnCode(UNKNOW_ERROR)
	}
	if consumer == nil {
		log.Errorf("consumer is nil, orgId[%s], projectId[%s], env[%s]",
			orgId, projectId, env)
		return res.SetReturnCode(CONSUMER_NOT_EXIST)
	}
	return res.SetSuccessAndData(consumer)

}

func (impl GatewayApiServiceImpl) buildApiInfoDto(gatewayApi *orm.GatewayApi) (*gw.ApiInfoDto, error) {
	if gatewayApi == nil {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	res := &gw.ApiInfoDto{
		Path:         gatewayApi.ApiPath,
		DisplayPath:  gatewayApi.ApiPath,
		MonitorPath:  gatewayApi.ApiPath,
		ApiId:        gatewayApi.Id,
		Method:       gatewayApi.Method,
		RedirectAddr: gatewayApi.RedirectAddr,
		RedirectType: gatewayApi.RedirectType,
		Description:  gatewayApi.Description,
		CreateAt:     gatewayApi.CreateTime.Format("2006-01-02T15:04:05"),
	}
	if gatewayApi.RegisterType == gw.RT_AUTO_REGISTER {
		res.RegisterType = gw.RT_AUTO
	} else {
		res.RegisterType = gatewayApi.RegisterType
	}
	if gatewayApi.NeedAuth == 1 {
		res.NeedAuth = true
	}
	if gatewayApi.NetType == gw.NT_OUT {
		res.OuterNetEnable = true
	}
	// TODO get prefix by id
	if gatewayApi.RuntimeServiceId != "" {
		res.DisplayPath = strings.TrimPrefix(res.Path, "/"+gatewayApi.RuntimeServiceId)
	}
	if res.DisplayPath == "" {
		res.DisplayPath = "/"
	}
	res.Path = res.DisplayPath
	res.MonitorPath = res.DisplayPath
	scheme_find := strings.Index(res.RedirectAddr, "://")
	if scheme_find == -1 {
		return nil, errors.Errorf("invalid RedirectAddr %s", res.RedirectAddr)
	}
	res.RedirectPath = "/"
	slash_find := strings.Index(res.RedirectAddr[scheme_find+3:], "/")
	if slash_find != -1 {
		res.RedirectPath = res.RedirectAddr[slash_find+scheme_find+3:]
		res.RedirectAddr = res.RedirectAddr[:slash_find+scheme_find+3]
	}
	res.RedirectAddr = strings.TrimSuffix(res.RedirectAddr, "/")
	if len(gatewayApi.Policies) != 0 {
		policies := []gw.PolicyDto{}
		err := json.Unmarshal([]byte(gatewayApi.Policies), &policies)
		if err != nil {
			return nil, errors.Wrapf(err, "json unmarshal [%s] failed", gatewayApi.Policies)
		}
		res.Policies = policies
	}
	return res, nil
}

func (impl GatewayApiServiceImpl) genSelectOptions(dto *gw.GetApisDto) []orm.SelectOption {
	var result []orm.SelectOption
	if dto.Method != "" {
		result = append(result, orm.SelectOption{
			Type:   orm.ExactMatch,
			Column: "method",
			Value:  dto.Method,
		})
	}
	if dto.ApiPath != "" {
		result = append(result, orm.SelectOption{
			Type:   orm.FuzzyMatch,
			Column: "api_path",
			Value:  dto.ApiPath,
		})
	}
	if dto.RegisterType != "" {
		result = append(result, orm.SelectOption{
			Type:   orm.ExactMatch,
			Column: "register_type",
			Value:  dto.RegisterType,
		})
	}
	if dto.NetType != "" {
		result = append(result, orm.SelectOption{
			Type:   orm.ExactMatch,
			Column: "net_type",
			Value:  dto.NetType,
		})
	}
	if dto.NeedAuth == 1 {
		result = append(result, orm.SelectOption{
			Type:   orm.ExactMatch,
			Column: "need_auth",
			Value:  1,
		})
	}
	if dto.RuntimeId == "" {
		result = append(result, orm.SelectOption{
			Type:   orm.ExactMatch,
			Column: "consumer_id",
			Value:  dto.ConsumerId,
		})
		if dto.DiceApp != "" {
			result = append(result, orm.SelectOption{
				Type:   orm.ExactMatch,
				Column: "dice_app",
				Value:  dto.DiceApp,
			})
			if dto.DiceService != "" {
				result = append(result, orm.SelectOption{
					Type:   orm.ExactMatch,
					Column: "dice_service",
					Value:  dto.DiceService,
				})
			}
		}
	}
	if dto.SortField != "" && dto.SortType != "" {
		if dto.SortField == "apiPath" {
			dto.SortField = "api_path"
		} else if dto.SortField == "createAt" {
			dto.SortField = "create_time"
		}
		var option *orm.SelectOption = nil
		switch dto.SortType {
		case gw.ST_UP:
			option = &orm.SelectOption{
				Type:   orm.AscOrder,
				Column: dto.SortField,
			}
		case gw.ST_DOWN:
			option = &orm.SelectOption{
				Type:   orm.DescOrder,
				Column: dto.SortField,
			}
		default:
			log.Errorf("unknown sort type: %s", dto.SortType)
		}
		if option != nil {
			result = append(result, *option)
		}
	} else {
		// 默认按修改时间排序
		result = append(result, orm.SelectOption{
			Type:   orm.DescOrder,
			Column: "update_time",
		})
	}
	return result

}

func (impl GatewayApiServiceImpl) GetApiInfos(dto *gw.GetApisDto) (result *common.PageQuery, err error) {
	var gatewayApiList *common.PageQuery
	apiInfoList := []interface{}{}
	var pageInfo *pb.Page
	verifyRes := impl.verifyGetApiInfosParams(dto.OrgId, dto.ProjectId, dto.Env)
	var options []orm.SelectOption
	defer func() {
		if err != nil {
			log.Errorf("error happened:%+v", err)
		}
	}()
	if !verifyRes.Success {
		if CONSUMER_NOT_EXIST.Code == verifyRes.Err.Code {
			if err != nil {
				return
			}
			result = &common.PageQuery{
				Result: apiInfoList,
				Page:   common.NewPage2(dto.Size, dto.Page),
			}
			return
		}
		err = errors.New(verifyRes.Err.Msg)
		return
	}
	if dto.RuntimeId == "" {
		consumer, ok := verifyRes.Data.(*orm.GatewayConsumer)
		if !ok {
			err = errors.New("transfer to GatewayConsumer failed")
			return
		}
		dto.ConsumerId = consumer.Id
	}
	options = impl.genSelectOptions(dto)
	if dto.RuntimeId != "" {
		var runtimeService *orm.GatewayRuntimeService
		runtimeService, err = impl.getRuntimeService(
			dto.RuntimeId,
			dto.DiceApp,
			dto.DiceService,
			impl.runtimeDb,
		)
		if err != nil {
			return
		}
		options = append(options, orm.SelectOption{
			Type:   orm.ExactMatch,
			Column: "runtime_service_id",
			Value:  runtimeService.Id,
		})
		dto.RuntimeServiceId = runtimeService.Id
	}
	{
		if dto.Page < 0 {
			dto.Page = 1
		}
		if dto.Size > 1000 {
			dto.Size = 1000
		}
		pageInfo = common.NewPage2(dto.Size, dto.Page)
		gatewayApiList, err = impl.apiDb.GetPage(options, (*common.Page)(pageInfo))
		if err != nil {
			return
		}
	}
	{
		apiOrmList, ok := gatewayApiList.Result.([]orm.GatewayApi)
		if !ok {
			err = errors.New("transfer to []orm.GatewayApi failed")
			return
		}
		for _, gatewayApi := range apiOrmList {
			var apiInfo *gw.ApiInfoDto
			apiInfo, err = impl.buildApiInfoDto(&gatewayApi)
			if err != nil {
				return
			}
			apiInfoList = append(apiInfoList, *apiInfo)
		}
	}
	result = &common.PageQuery{
		Result: apiInfoList,
		Page:   pageInfo,
	}
	return
}

func (impl GatewayApiServiceImpl) deleteApi(apiId string) (StandardErrorCode, error) {
	var gatewayRoute *orm.GatewayRoute
	var gatewayService *orm.GatewayService
	var gatewayApi *orm.GatewayApi
	var kongAdapter kong.KongAdapter
	var kongInfo *orm.GatewayKongInfo
	var runtimeService *orm.GatewayRuntimeService
	ret := UNKNOW_ERROR
	var err error = nil
	var inPackage []orm.GatewayApiInPackage
	auditCtx := map[string]interface{}{}
	if len(apiId) == 0 {
		err = errors.New("empty apiId")
		ret = PARAMS_IS_NULL
		goto errorHappened
	}
	gatewayApi, err = impl.apiDb.GetById(apiId)
	if err != nil {
		goto errorHappened
	}
	if gatewayApi == nil {
		ret = API_NOT_EXIST
		return ret, nil
	}
	inPackage, err = impl.apiInPackageDb.SelectByApi(apiId)
	if err != nil {
		goto errorHappened
	}
	if len(inPackage) != 0 {
		if gatewayApi.RuntimeServiceId != "" {
			err = errors.Errorf("api in packages, packages:%+v", inPackage)
			ret = API_IN_PACKAGE
			goto errorHappened
		}
		for _, dao := range inPackage {
			_ = impl.packageApiDb.DeleteByPackageDiceApi(dao.PackageId, apiId)
			_ = impl.apiInPackageDb.Delete(dao.PackageId, apiId)
		}
	}

	if gatewayApi.ConsumerId != "" {
		kongAdapter = kong.NewKongAdapterByConsumerId(impl.consumerDb, gatewayApi.ConsumerId)
	} else if gatewayApi.RuntimeServiceId != "" {
		runtimeService, err = impl.runtimeDb.Get(gatewayApi.RuntimeServiceId)
		if err != nil {
			goto errorHappened
		}
		if runtimeService == nil {
			goto errorHappened
		}
		defer func() {
			method := gatewayApi.Method
			if method == "" {
				method = "all"
			}
			auditCtx["path"] = strings.Join(strings.Split(gatewayApi.ApiPath, "/")[2:], "/")
			auditCtx["method"] = method
			audit := common.MakeAuditInfo(impl.reqCtx, common.ScopeInfo{
				ProjectId:   runtimeService.ProjectId,
				Workspace:   runtimeService.Workspace,
				AppId:       runtimeService.AppId,
				ServiceName: runtimeService.ServiceName,
				RuntimeName: runtimeService.RuntimeName,
			}, apistructs.DeleteServiceApiTemplate, nil, auditCtx)
			if audit != nil {
				if err == nil {
					audit.Result = apistructs.SuccessfulResult
				} else {
					//audit.Result = apistructs.FailureResult
					//audit.ErrorMsg = errors.Cause(err).Error()
					return
				}
				err = bundle.Bundle.CreateAuditEvent(&apistructs.AuditCreateRequest{Audit: *audit})
				if err != nil {
					log.Errorf("create audit failed, err:%+v", err)
				}
			}
		}()

		kongInfo, err = impl.kongDb.GetKongInfo(&orm.GatewayKongInfo{
			Az:        runtimeService.ClusterName,
			ProjectId: runtimeService.ProjectId,
			Env:       runtimeService.Workspace,
		})
		if err != nil {
			goto errorHappened
		}
		kongAdapter = kong.NewKongAdapter(kongInfo.KongAddr)
	} else {
		err = errors.Errorf("invalid api: %+v", gatewayApi)
		goto errorHappened
	}
	if gatewayRoute, err = impl.routeDb.GetByApiId(apiId); err == nil && gatewayRoute != nil {
		err = kongAdapter.DeleteRoute(gatewayRoute.RouteId)
		err = impl.routeDb.DeleteById(gatewayRoute.Id)
	}
	if err != nil {
		goto errorHappened
	}
	if gatewayService, err = impl.serviceDb.GetByApiId(apiId); err == nil && gatewayService != nil {
		err = kongAdapter.DeleteService(gatewayService.ServiceId)
		err = impl.serviceDb.DeleteById(gatewayService.Id)
	}
	if err != nil {
		goto errorHappened
	}
	_ = impl.pluginDb.DeleteByApiId(apiId)
	err = impl.apiDb.DeleteById(apiId)
	if err != nil {
		goto errorHappened
	}
	return ret, nil
errorHappened:
	return ret, err
}

func (impl GatewayApiServiceImpl) DeleteApi(apiId string) error {
	_, err := impl.deleteApi(apiId)
	if err != nil {
		log.Errorf("error happened: %+v", err)
		return err
	}
	return nil
}

func (impl GatewayApiServiceImpl) verifyApiUpdateParams(apiId string, req *gw.ApiReqDto) *common.StandardResult {
	res := &common.StandardResult{Success: false}
	if len(apiId) == 0 {
		log.Error("empty apiId")
		return res.SetReturnCode(PARAMS_IS_NULL)
	}
	gatewayApi, err := impl.apiDb.GetById(apiId)
	if err != nil {
		log.Errorf("err happened[%+v]", err)
		return res.SetReturnCode(UNKNOW_ERROR)
	}
	if gatewayApi == nil {
		log.Error("gatewayApi is nil")
		return res.SetReturnCode(API_NOT_EXIST)
	}
	if req == nil || req.IsEmpty() {
		log.Errorf("invalid req[%+v]", req)
		return res.SetReturnCode(PARAMS_IS_NULL)
	}
	req.DiceApp = gatewayApi.DiceApp
	req.DiceService = gatewayApi.DiceService
	if req.RedirectType == gw.RT_URL {
		if req.RedirectAddr == "" {
			return res.SetReturnCode(PARAMS_IS_NULL)
		}
		req.RedirectAddr = req.RedirectAddr + "/" + strings.TrimPrefix(req.RedirectPath, "/")
	} else if req.RuntimeId == "" || req.DiceApp == "" || req.DiceService == "" {
		return res.SetErrorInfo(&common.ErrInfo{Msg: "未选择对应的部署分支"})
	}

	return res.SetSuccessAndData(gatewayApi)
}

func (impl GatewayApiServiceImpl) updateService(kongAdapter kong.KongAdapter, req *gw.ApiDto, gatewayApi *orm.GatewayApi) (*orm.GatewayService, error) {
	service, err := impl.serviceDb.GetByApiId(gatewayApi.Id)
	if err != nil {
		return nil, errors.WithStack(err)
	}
	if service == nil {
		return nil, errors.New("service is nil")
	}
	// 转发地址变化
	if gatewayApi.RedirectAddr != req.RedirectAddr {
		serviceReq, err := impl.kongAssembler.BuildKongServiceReq(service.ServiceId, req)
		if err != nil {
			return service, errors.WithStack(err)
		}
		serviceResp, err := kongAdapter.CreateOrUpdateService(serviceReq)
		if err != nil {
			return service, errors.WithStack(err)
		}
		err = impl.dbAssembler.Resp2GatewayService(serviceResp, service)
		if err != nil {
			return service, errors.WithStack(err)
		}
		err = impl.serviceDb.Update(service)
		if err != nil {
			return service, errors.WithStack(err)
		}
	}
	return service, nil
}

func (impl GatewayApiServiceImpl) updateRoute(kongAdapter kong.KongAdapter, req *gw.ApiDto, gatewayApi *orm.GatewayApi, service *orm.GatewayService, isRegexPath bool, normalPath string) (*orm.GatewayRoute, error) {
	route, err := impl.routeDb.GetByApiId(gatewayApi.Id)
	if err != nil {
		return nil, errors.WithStack(err)
	}
	if service == nil {
		return nil, errors.New("service is nil")
	}
	// 若已与后端API绑定，无法修改Method
	if len(gatewayApi.UpstreamApiId) > 0 {
		log.Warnf("upstreamApiId[%s] method[%s] not change: req method[%s]", gatewayApi.UpstreamApiId, gatewayApi.Method, req.Method)
		req.Method = gatewayApi.Method
	}
	oldOuterEnable := false
	if gatewayApi.NetType == gw.NT_OUT {
		oldOuterEnable = true
	}
	// apiPath变化 或 method变化 或 网络模式变化
	if (gatewayApi.ApiPath != normalPath) || gatewayApi.Method != req.Method || oldOuterEnable != req.OuterNetEnable {
		routeReq, err := impl.kongAssembler.BuildKongRouteReq(route.RouteId, req, service.ServiceId, isRegexPath)
		if err != nil {
			return route, errors.WithStack(err)
		}
		routeResp, err := kongAdapter.CreateOrUpdateRoute(routeReq)
		if err != nil {
			return route, err
		}
		err = impl.dbAssembler.Resp2GatewayRoute(routeResp, route)
		if err != nil {
			return route, errors.WithStack(err)
		}
		err = impl.routeDb.Update(route)
		if err != nil {
			return route, errors.WithStack(err)
		}
	}
	return route, nil
}

func (impl GatewayApiServiceImpl) updatePolicy(kongAdapter kong.KongAdapter, req *gw.ApiDto, reqOption *gw.ApiReqOptionDto, gatewayApi *orm.GatewayApi, service *orm.GatewayService, route *orm.GatewayRoute, serviceRewritePath string, dbDto *gw.ApiDto) error {
	adds := []orm.GatewayPolicy{}
	updates := []orm.GatewayPolicy{}
	dels := []orm.GatewayPolicy{}
	reqMap := map[string]orm.GatewayPolicy{}
	if reqOption != nil {
		reqPolicies := reqOption.Policies
		reqMap = impl.acquirePolicies(reqPolicies)
	}
	if len(serviceRewritePath) > 0 {
		configJson, err := json.Marshal(gw.PathVariableConfig{
			RequestRegex: req.Path,
			RewritePath:  serviceRewritePath,
			Carrier:      "ROUTE",
		})
		if err != nil {
			return errors.WithStack(err)
		}
		reqMap["path-variable"] = orm.GatewayPolicy{
			PluginName: "path-variable",
			Config:     configJson,
		}
	}
	if config.ServerConf.HasRouteInfo {
		configJson, err := json.Marshal(gw.RouteInfoConfig{
			ProjectId: dbDto.ProjectId,
			Workspace: strings.ToLower(dbDto.Env),
			App:       strings.ToLower(dbDto.DiceApp),
			Service:   strings.ToLower(dbDto.DiceService),
			ApiPath:   dbDto.Path,
			Carrier:   "ROUTE",
		})
		if err != nil {
			return errors.WithStack(err)
		}
		reqMap["set-route-info"] = orm.GatewayPolicy{
			PluginName: "set-route-info",
			Config:     configJson,
		}
	}
	plugins, err := impl.pluginDb.SelectByOnlyApiId(gatewayApi.Id)
	if err != nil {
		return errors.WithStack(err)
	}
	var aclExistPolicy *orm.GatewayPolicy = nil
	policyMap := map[string]orm.GatewayPolicy{}
	for _, plugin := range plugins {
		if len(plugin.PolicyId) == 0 {
			policyMap[plugin.PluginName] = orm.GatewayPolicy{
				PluginName: plugin.PluginName,
			}
			continue
		}
		gatewayPolicy, err := impl.policyDb.GetById(plugin.PolicyId)
		if err != nil {
			return errors.WithStack(err)
		}
		if gatewayPolicy == nil {
			log.Error("gatewayPolicy is nil")
			continue
		}
		if gatewayPolicy.Category != "basic" {
			policyMap[plugin.PluginName] = *gatewayPolicy
		}
		if gatewayPolicy.PluginName == "acl" {
			aclExistPolicy = gatewayPolicy
		}
	}
	needAuth := false
	for name, policy := range reqMap {
		if policy.Category == "auth" {
			needAuth = true
		}
		if policy.PluginName == "oauth2" {
			_ = kongAdapter.TouchRouteOAuthMethod(route.RouteId)
		}
		if _, exist := policyMap[name]; !exist {
			adds = append(adds, policy)
			continue
		}
		if len(policy.Id) != 0 && policy.Id == policyMap[name].Id {
			delete(policyMap, name)
			continue
		}
		updates = append(updates, policy)
		delete(policyMap, name)
	}
	for _, policy := range policyMap {
		dels = append(dels, policy)
	}
	if needAuth {
		req.NeedAuth = 1
	} else {
		req.NeedAuth = 0
	}
	// remove acl plugin if no auth
	if aclExistPolicy != nil && !needAuth {
		dels = append(dels, *aclExistPolicy)
	}
	// update basic plugin
	basicPolicies, _ := impl.policyDb.SelectByCategory("basic")
	for _, policy := range basicPolicies {
		if policy.PluginName == "acl" && aclExistPolicy == nil && needAuth {
			objMap := map[string]interface{}{}
			err = json.Unmarshal([]byte(policy.Config), &objMap)
			if err != nil {
				return errors.WithStack(err)
			}
			objMap["whitelist"] = reqOption.ConsumerId
			var mapJson []byte
			mapJson, err = json.Marshal(objMap)
			if err != nil {
				return errors.WithStack(err)
			}
			policy.Config = mapJson
			adds = append(adds, policy)
		}
	}
	for _, policy := range adds {
		pluginReq, err := impl.kongAssembler.BuildKongPluginReqDto("", &policy, service.ServiceId, route.RouteId, "")
		if err != nil {
			return errors.WithStack(err)
		}
		pluginResp, err := kongAdapter.AddPlugin(pluginReq)
		if err != nil {
			return errors.WithStack(err)
		}
		if pluginResp == nil {
			continue
		}
		pluginParams := assembler.PluginParams{
			PolicyId:  policy.Id,
			ServiceId: service.Id,
			RouteId:   route.Id,
			ApiId:     gatewayApi.Id,
		}
		pluginDao, err := impl.dbAssembler.Resp2GatewayPluginInstance(pluginResp, pluginParams)
		if err != nil {
			return errors.WithStack(err)
		}
		err = impl.pluginDb.Insert(pluginDao)
		if err != nil {
			return errors.WithStack(err)
		}
	}
	for _, policy := range updates {
		oldPlugin, err := impl.pluginDb.GetByPluginNameAndApiId(policy.PluginName, gatewayApi.Id)
		if err != nil {
			return errors.WithStack(err)
		}
		if oldPlugin == nil {
			log.Error("oldPlugin is nil")
			continue
		}
		pluginReq, err := impl.kongAssembler.BuildKongPluginReqDto(oldPlugin.PluginId,
			&policy, service.ServiceId, route.RouteId, "")
		if err != nil {
			return errors.WithStack(err)
		}
		pluginResp, err := kongAdapter.PutPlugin(pluginReq)
		if err != nil {
			return errors.WithStack(err)
		}
		if pluginResp == nil {
			continue
		}
		pluginParams := assembler.PluginParams{
			PolicyId:  policy.Id,
			ServiceId: service.Id,
			RouteId:   route.Id,
			ApiId:     gatewayApi.Id,
		}
		newPlugin, err := impl.dbAssembler.Resp2GatewayPluginInstance(pluginResp, pluginParams)
		if err != nil {
			return errors.WithStack(err)
		}
		newPlugin.Id = oldPlugin.Id
		err = impl.pluginDb.Update(newPlugin)
		if err != nil {
			return errors.WithStack(err)
		}
	}
	for _, policy := range dels {
		oldPlugin, err := impl.pluginDb.GetByPluginNameAndApiId(policy.PluginName, gatewayApi.Id)
		if err != nil {
			return errors.WithStack(err)
		}
		if oldPlugin == nil {
			log.Error("oldPlugin is nil")
			continue
		}
		err = kongAdapter.RemovePlugin(oldPlugin.PluginId)
		if err != nil {
			return errors.WithStack(err)
		}
		err = impl.pluginDb.DeleteById(oldPlugin.Id)
		if err != nil {
			return errors.WithStack(err)
		}
	}
	pluginReq := &kongDto.KongPluginReqDto{
		Name:    "host-check",
		RouteId: route.RouteId,
		Config: map[string]interface{}{
			"allow_host": gw.INNER_HOSTS,
		},
	}
	if req.IsInner == 1 {
		_, err = kongAdapter.CreateOrUpdatePlugin(pluginReq)
		if err != nil {
			return err
		}
	} else {
		err = kongAdapter.DeletePluginIfExist(pluginReq)
		if err != nil {
			return err
		}
	}
	return nil
}

func (impl GatewayApiServiceImpl) updateRuntimeApi(gatewayApi *orm.GatewayApi, dto *gw.ApiDto, session ...*db.SessionHelper) (*orm.GatewayApi, StandardErrorCode, error) {
	ret := UNKNOW_ERROR
	var err error
	var service *orm.GatewayService
	var route *orm.GatewayRoute
	var newGatewayApi *orm.GatewayApi
	var kongAdapter kong.KongAdapter
	var isRegexPath bool
	var dbDto gw.ApiDto
	var inPackages []orm.GatewayApiInPackage
	var pathPrefix string
	var kongInfo *orm.GatewayKongInfo
	var runtimeService *orm.GatewayRuntimeService
	auditCtx := map[string]interface{}{}
	dto.OuterNetEnable = true
	serviceRewritePath := ""
	runtimeDb := impl.runtimeDb
	if len(session) > 0 {
		ret = PARAMS_IS_NULL
		runtimeDb, err = impl.runtimeDb.NewSession(session[0])
		if err != nil {
			goto errorHappened
		}
	}
	runtimeService, err = runtimeDb.Get(gatewayApi.RuntimeServiceId)
	if err != nil {
		return nil, PARAMS_IS_NULL, err
	}
	if runtimeService == nil {
		return nil, PARAMS_IS_NULL, errors.Errorf("find runtime service failed, id:%s", dto.RuntimeServiceId)
	}
	defer func() {
		method := dto.Method
		if method == "" {
			method = "all"
		}
		auditCtx["path"] = strings.Join(strings.Split(dto.Path, "/")[2:], "/")
		auditCtx["method"] = method
		audit := common.MakeAuditInfo(impl.reqCtx, common.ScopeInfo{
			ProjectId:   runtimeService.ProjectId,
			Workspace:   runtimeService.Workspace,
			AppId:       runtimeService.AppId,
			ServiceName: runtimeService.ServiceName,
			RuntimeName: runtimeService.RuntimeName,
		}, apistructs.UpdateServiceApiTemplate, nil, auditCtx)
		if audit != nil {
			if err == nil {
				audit.Result = apistructs.SuccessfulResult
			} else {
				//audit.Result = apistructs.FailureResult
				//audit.ErrorMsg = errors.Cause(err).Error()
				return
			}
			err = bundle.Bundle.CreateAuditEvent(&apistructs.AuditCreateRequest{Audit: *audit})
			if err != nil {
				log.Errorf("create audit failed, err:%+v", err)
			}
		}
	}()
	dto.Swagger = gatewayApi.Swagger
	dto.RuntimeServiceId = runtimeService.Id
	dto.ProjectId = runtimeService.ProjectId
	dto.Env = runtimeService.Workspace
	kongInfo, err = impl.kongDb.GetKongInfo(&orm.GatewayKongInfo{
		Az:        runtimeService.ClusterName,
		ProjectId: runtimeService.ProjectId,
		Env:       runtimeService.Workspace,
	})
	if err != nil {
		return nil, PARAMS_IS_NULL, err
	}
	kongAdapter = kong.NewKongAdapter(kongInfo.KongAddr)
	dto.Hosts = append(dto.Hosts, kong.InnerHost)
	if dto.RedirectType == gw.RT_SERVICE {
		dto.RedirectAddr = runtimeService.InnerAddress + dto.RedirectPath
		if !strings.HasPrefix(dto.RedirectAddr, "http://") {
			dto.RedirectAddr = "http://" + dto.RedirectAddr
		}
	}
	pathPrefix, err = (*impl.globalBiz).GetRuntimeServicePrefix(runtimeService)
	if err != nil {
		return nil, PARAMS_IS_NULL, err
	}
	dto.Path = pathPrefix + dto.Path

	isRegexPath, dbDto, serviceRewritePath, err = impl.pathAdjust(dto)
	if err != nil {
		return nil, INVALID_PATH, err
	}
	if dto.Path != gatewayApi.ApiPath || dto.Method != gatewayApi.Method {
		exist, existId, err := impl.apiPathExist(nil, dto.Path, dto.Method)
		if err != nil {
			return nil, PARAMS_IS_NULL, err
		}
		if exist {
			existDao, err := impl.apiDb.GetById(existId)
			if err != nil {
				return nil, PARAMS_IS_NULL, err
			}
			return existDao, API_EXIST, errors.Errorf("api path[%s] method[%s] conflict", dto.Path, dto.Method)
		}

	}
	service, err = impl.updateService(kongAdapter, dto, gatewayApi)
	if err != nil {
		ret = UPDATE_API_SERVICE_FAIL
		goto errorHappened
	}
	route, err = impl.updateRoute(kongAdapter, dto, gatewayApi, service, isRegexPath, dbDto.Path)
	if err != nil {
		ret = UPDATE_API_ROUTE_FAIL
		goto errorHappened
	}
	err = impl.updatePolicy(kongAdapter, dto, nil, gatewayApi, service, route, serviceRewritePath, &dbDto)
	if err != nil {
		ret = UPDATE_API_PLUGIN_FAIL
		goto errorHappened
	}
	newGatewayApi, err = impl.dbAssembler.BuildGatewayApi(dbDto, "", nil, "")
	if err != nil {
		goto errorHappened
	}
	newGatewayApi.Id = gatewayApi.Id
	err = impl.apiDb.Update(newGatewayApi)
	if err != nil {
		goto errorHappened
	}
	newGatewayApi.CreateTime = gatewayApi.CreateTime
	inPackages, err = impl.apiInPackageDb.SelectByApi(newGatewayApi.Id)
	if err != nil {
		goto errorHappened
	}
	for _, pack := range inPackages {
		var packageApi *orm.GatewayPackageApi
		packageApi, err = impl.packageApiDb.GetByAny(&orm.GatewayPackageApi{
			PackageId: pack.PackageId,
			DiceApiId: gatewayApi.Id,
		})
		if err != nil {
			goto errorHappened
		}
		if packageApi != nil {
			packageApi.ApiPath = newGatewayApi.ApiPath
			packageApi.Method = newGatewayApi.Method
			packageApi.RedirectAddr = newGatewayApi.RedirectAddr
			err = impl.packageApiDb.Update(packageApi)
			if err != nil {
				goto errorHappened
			}
		}
	}

	return newGatewayApi, ret, nil
errorHappened:
	log.Errorf("error Happend, %+v:", err)
	return nil, ret, err
}

func (impl GatewayApiServiceImpl) updateApi(gatewayApi *orm.GatewayApi, consumer *orm.GatewayConsumer, dto *gw.ApiDto, optionDto *gw.ApiReqOptionDto, upstreamApiId ...string) (*orm.GatewayApi, StandardErrorCode, error) {
	if consumer == nil || dto == nil || len(dto.RedirectAddr) == 0 {
		return nil, PARAMS_IS_NULL, errors.Errorf("invalid consumer[%+v] or dto[%+v]",
			consumer, dto)
	}
	ret := UNKNOW_ERROR
	var err error
	var service *orm.GatewayService
	var route *orm.GatewayRoute
	var newGatewayApi = new(orm.GatewayApi)
	var kongAdapter kong.KongAdapter
	var gatewayPolicies []orm.GatewayPolicy
	var isRegexPath bool
	var dbDto gw.ApiDto
	var inPackages []orm.GatewayApiInPackage
	serviceRewritePath := ""
	kongInfo, err := impl.kongDb.GetKongInfo(&orm.GatewayKongInfo{
		Az:        consumer.Az,
		ProjectId: consumer.ProjectId,
		Env:       consumer.Env,
	})
	if err != nil {
		return nil, PARAMS_IS_NULL, err
	}
	kongAdapter = kong.NewKongAdapter(kongInfo.KongAddr)
	dto.Hosts = append(dto.Hosts, kong.InnerHost)
	dto.Hosts = append(dto.Hosts, kongInfo.Endpoint)

	isRegexPath, dbDto, serviceRewritePath, err = impl.pathAdjust(dto)
	if err != nil {
		ret = INVALID_PATH
		goto errorHappened
	}
	if dto.Path != gatewayApi.ApiPath || dto.Method != gatewayApi.Method {
		exist, _, err := impl.apiPathExist(consumer, dto.Path, dto.Method)
		if err != nil {
			return nil, PARAMS_IS_NULL, err
		}
		if exist {
			return nil, API_EXIST, errors.Errorf("api path[%s] method[%s] conflict", dto.Path, dto.Method)
		}
	}
	service, err = impl.updateService(kongAdapter, dto, gatewayApi)
	if err != nil {
		ret = UPDATE_API_SERVICE_FAIL
		goto errorHappened
	}
	route, err = impl.updateRoute(kongAdapter, dto, gatewayApi, service, isRegexPath, dbDto.Path)
	if err != nil {
		ret = UPDATE_API_ROUTE_FAIL
		goto errorHappened
	}
	if optionDto != nil {
		err = impl.updatePolicy(kongAdapter, dto, optionDto, gatewayApi, service, route, serviceRewritePath, &dbDto)
		if err != nil {
			ret = UPDATE_API_PLUGIN_FAIL
			goto errorHappened
		}
		dbDto.NeedAuth = dto.NeedAuth
	}
	if optionDto != nil {
		for _, policyId := range optionDto.Policies {
			policy, err := impl.policyDb.GetById(policyId)
			if err != nil {
				goto errorHappened
			}
			if policy == nil {
				log.Error("policy is nil")
				continue
			}
			gatewayPolicies = append(gatewayPolicies, *policy)
		}
	}
	newGatewayApi, err = impl.dbAssembler.BuildGatewayApi(dbDto, gatewayApi.ConsumerId, gatewayPolicies, "", upstreamApiId...)
	if err != nil {
		goto errorHappened
	}
	newGatewayApi.Id = gatewayApi.Id
	err = impl.apiDb.Update(newGatewayApi)
	if err != nil {
		goto errorHappened
	}
	newGatewayApi.CreateTime = gatewayApi.CreateTime
	inPackages, err = impl.apiInPackageDb.SelectByApi(newGatewayApi.Id)
	if err != nil {
		goto errorHappened
	}
	for _, pack := range inPackages {
		var packageApi *orm.GatewayPackageApi
		packageApi, err = impl.packageApiDb.GetByAny(&orm.GatewayPackageApi{
			PackageId: pack.PackageId,
			DiceApiId: gatewayApi.Id,
		})
		if err != nil {
			goto errorHappened
		}
		if packageApi != nil {
			packageApi.ApiPath = newGatewayApi.ApiPath
			packageApi.Method = newGatewayApi.Method
			packageApi.RedirectAddr = newGatewayApi.RedirectAddr
			err = impl.packageApiDb.Update(packageApi)
			if err != nil {
				goto errorHappened
			}
		}
	}

	return newGatewayApi, ret, nil

errorHappened:
	log.Errorf("error Happend, %+v:", err)
	return nil, ret, err
}

func (impl GatewayApiServiceImpl) UpdateApi(apiId string, req *gw.ApiReqDto) (apiInfo *gw.ApiInfoDto, err error) {
	defer func() {
		if err != nil {
			log.Errorf("error happened:%+v", err)
		}
	}()
	var newGatewayApi *orm.GatewayApi
	verifyRes := impl.verifyApiUpdateParams(apiId, req)
	if !verifyRes.Success {
		err = errors.New(verifyRes.Err.Msg)
		return
	}
	var consumer *orm.GatewayConsumer
	gatewayApi, ok := verifyRes.Data.(*orm.GatewayApi)
	if !ok {
		err = errors.New("transfer to GatewayApi failed")
		return
	}
	if gatewayApi.ConsumerId != "" {
		consumer, err = impl.consumerDb.GetById(gatewayApi.ConsumerId)
		if err != nil {
			return
		}
		if consumer == nil {
			err = errors.New("consumer is nil")
			return
		}
	}
	// 继承老的注册类型
	req.RegisterType = gatewayApi.RegisterType
	req.ApiDto.UpstreamApiId = gatewayApi.UpstreamApiId
	req.OuterNetEnable = true
	if gatewayApi.RuntimeServiceId == "" {
		newGatewayApi, _, err = impl.updateApi(gatewayApi, consumer, req.ApiDto, req.ApiReqOptionDto)
	} else {
		newGatewayApi, _, err = impl.updateRuntimeApi(gatewayApi, req.ApiDto)
	}
	if err != nil {
		return
	}
	apiInfo, err = impl.buildApiInfoDto(newGatewayApi)
	if err != nil {
		return
	}
	return
}

func (impl GatewayApiServiceImpl) ClearRuntimeApi(dao *orm.GatewayRuntimeService) error {
	apis, err := impl.apiDb.SelectByAny(&orm.GatewayApi{
		RuntimeServiceId: dao.Id,
		RedirectType:     gw.RT_SERVICE,
	})
	if err != nil {
		return err
	}
	for _, api := range apis {
		impl.deleteApi(api.Id)
	}
	impl.apiDb.RealDeleteByRuntimeServiceId(dao.Id)
	return nil
}

func (impl GatewayApiServiceImpl) TouchRuntimeApi(dao *orm.GatewayRuntimeService, session *db.SessionHelper, newCreated bool) error {
	apiSession, err := impl.apiDb.NewSession(session)
	if err != nil {
		return err
	}
	apis, err := apiSession.SelectByAny(&orm.GatewayApi{
		RuntimeServiceId: dao.Id,
		RedirectType:     gw.RT_SERVICE,
	})
	if err != nil {
		return err
	}
	redirectAddr := dao.InnerAddress
	if strings.EqualFold(dao.BackendProtocol, "https") {
		redirectAddr = "https://" + redirectAddr
	}
	for _, api := range apis {
		redirectPath := "/"
		scheme_find := strings.Index(api.RedirectAddr, "://")
		if scheme_find == -1 {
			return errors.Errorf("invalid RedirctAddr:%s", api.RedirectAddr)
		}
		slash_find := strings.Index(api.RedirectAddr[scheme_find+3:], "/")
		if slash_find != -1 {
			redirectPath = api.RedirectAddr[slash_find+scheme_find+3:]
		}
		pathPrefix, err := (*impl.globalBiz).GetRuntimeServicePrefix(dao)
		if err != nil {
			return err
		}
		path := strings.TrimPrefix(api.ApiPath, pathPrefix)
		_, _, err = impl.updateRuntimeApi(&api, &gw.ApiDto{
			Path:           path,
			Method:         api.Method,
			RedirectAddr:   redirectAddr,
			RedirectPath:   redirectPath,
			RegisterType:   api.RegisterType,
			RedirectType:   gw.RT_SERVICE,
			OuterNetEnable: true,
			DiceApp:        api.DiceApp,
			DiceService:    api.DiceService,
			Description:    api.Description,
		}, session)
		if err != nil {
			return err
		}
	}
	//	if newCreated {
	pathPrefix, err := (*impl.globalBiz).GetRuntimeServicePrefix(dao)
	if err != nil {
		return err
	}
	api, err := impl.apiDb.GetRawByAny(&orm.GatewayApi{
		ApiPath: pathPrefix,
	})
	if err != nil {
		return err
	}
	if api == nil {
		_, _, err = impl.CreateRuntimeApi(&gw.ApiDto{
			Path:             "/",
			RedirectAddr:     redirectAddr + "/",
			RedirectType:     gw.RT_SERVICE,
			DiceApp:          dao.AppName,
			DiceService:      dao.ServiceName,
			RuntimeServiceId: dao.Id,
			OuterNetEnable:   true,
			RegisterType:     gw.RT_AUTO,
		}, session)
		if err != nil {
			return err
		}
	}
	//	}
	return nil
}
