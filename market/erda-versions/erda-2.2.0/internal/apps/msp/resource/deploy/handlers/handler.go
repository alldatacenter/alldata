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

package handlers

import (
	"bytes"
	"fmt"
	"os"
	"time"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/msp/instance/db"
	"github.com/erda-project/erda/internal/apps/msp/resource/utils"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

const RuntimeMaxUpTimeoutSeconds = 15 * 60
const (
	TmcInstanceStatusInit        = "INIT"
	TmcInstanceStatusRunning     = "RUNNING"
	TmcInstanceStatusError       = "ERROR"
	TmcInstanceStatusDeleted     = "DELETED"
	TmcInstanceStatusDeleteError = "DELETE_ERROR"
)

const (
	ResourceConfigCenter     = "config-center"
	ResourceNacos            = "nacos"
	ResourceMysql            = "mysql"
	ResourceTerminusZKProxy  = "terminus-zkproxy"
	ResourceRegisterCenter   = "registercenter"
	ResourceZookeeper        = "zookeeper"
	ResourceZKProxy          = "zkproxy"
	ResourceEtcd             = "etcd"
	ResourceApiGateway       = "api-gateway"
	ResourcePostgresql       = "postgresql"
	ResourceMonitor          = "monitor"
	ResourceJvmProfiler      = "jvm-profiler"
	ResourceLogAnalytics     = "log-analytics"
	ResourceLogEs            = "log-es"
	ResourceLogExporter      = "log-exporter"
	ResourceMonitorCollector = "monitor-collector"
	ResourceMonitorKafka     = "monitor-kafka"
	ResourceMonitorZk        = "monitor-zk"
	ResourceServiceMesh      = "service-mesh"
	ResourceLogService       = "log-service"
)

const (
	DeployModeSAAS = "SAAS"
	DeployModePAAS = "PAAS"
)

type ResourceDeployRequest struct {
	Engine      string
	Uuid        string
	Plan        string
	Az          string
	Options     map[string]string
	TenantGroup string
	Callback    string
}

type ResourceDeployResult struct {
	ID          string
	Config      map[string]string
	Options     map[string]string
	Status      string
	CreatedTime time.Time
	UpdatedTime time.Time
}

type ResourceInfo struct {
	Tmc        *db.Tmc
	TmcVersion *db.TmcVersion
	Spec       *apistructs.AddonExtension
	Dice       *diceyml.Object
}

type ResourceDeployHandler interface {
	IsMatch(tmc *db.Tmc) bool
	GetResourceInfo(req *ResourceDeployRequest) (*ResourceInfo, error)
	GetClusterConfig(az string) (map[string]string, error)
	CheckIfHasCustomConfig(clusterConfig map[string]string) (map[string]string, bool)
	UpdateTmcInstanceOnCustom(tmcInstance *db.Instance, sgConfig map[string]string) error
	CheckIfNeedTmcInstance(req *ResourceDeployRequest, resourceInfo *ResourceInfo) (*db.Instance, bool, error)
	DoPreDeployJob(resourceInfo *ResourceInfo, tmcInstance *db.Instance) error
	BuildServiceGroupRequest(resourceInfo *ResourceInfo, tmcInstance *db.Instance, clusterConfig map[string]string) interface{}
	DoDeploy(serviceGroupDeployRequest interface{}, resourceInfo *ResourceInfo, tmcInstance *db.Instance, clusterConfig map[string]string) (interface{}, error)
	BuildTmcInstanceConfig(tmcInstance *db.Instance, serviceGroupDeployResult interface{}, clusterConfig map[string]string, additionalConfig map[string]string) map[string]string
	UpdateTmcInstanceOnFinish(tmcInstance *db.Instance, sgConfig map[string]string, sgStatus string) error
	DoPostDeployJob(tmcInstance *db.Instance, serviceGroupDeployResult interface{}, clusterConfig map[string]string) (map[string]string, error)
	CheckIfNeedTmcInstanceTenant(req *ResourceDeployRequest, resourceInfo *ResourceInfo) (*db.InstanceTenant, bool, error)
	DoApplyTmcInstanceTenant(req *ResourceDeployRequest, resourceInfo *ResourceInfo, tmcInstance *db.Instance, tenant *db.InstanceTenant, clusterConfig map[string]string) (map[string]string, error)
	InitializeTmcInstance(req *ResourceDeployRequest, resourceInfo *ResourceInfo, subResults []*ResourceDeployResult) (*db.Instance, error)
	InitializeTmcInstanceTenant(req *ResourceDeployRequest, tmcInstance *db.Instance, subResults []*ResourceDeployResult) (*db.InstanceTenant, error)
	UpdateTmcInstanceTenantOnFinish(tenant *db.InstanceTenant, config map[string]string) (*db.InstanceTenant, error)
	BuildSubResourceDeployRequest(name string, addon *diceyml.AddOn, req *ResourceDeployRequest) *ResourceDeployRequest
	BuildRequestRelation(parentRequest string, childRequest string) error
	BuildDeployResult(tmcInstance *db.Instance, tenant *db.InstanceTenant) ResourceDeployResult
	DeleteTmcInstance(tmcInstance *db.Instance, status string) error
	DeleteTenant(tenant *db.InstanceTenant, tmcInstance *db.Instance, clusterConfig map[string]string) error
	UnDeploy(tmcInstance *db.Instance) error
	GetRelationResourceIds(parentId string) []string
	DeleteRequestRelation(parentId string, childId string) error
}

type DefaultDeployHandler struct {
	TenantDb             *db.InstanceTenantDB
	InstanceDb           *db.InstanceDB
	TmcRequestRelationDb *db.TmcRequestRelationDB
	TmcDb                *db.TmcDB
	TmcVersionDb         *db.TmcVersionDB
	TmcIniDb             *db.TmcIniDB
	Bdl                  *bundle.Bundle
	Log                  logs.Logger
}

func (h *DefaultDeployHandler) DeleteRequestRelation(parentId string, childId string) error {
	err := h.TmcRequestRelationDb.DeleteRequestRelation(parentId, childId)
	return err
}

func (h *DefaultDeployHandler) GetRelationResourceIds(parentId string) []string {
	ids, _ := h.TmcRequestRelationDb.GetChildRequestIdsByParentId(parentId)
	return ids
}

func (h *DefaultDeployHandler) UnDeploy(tmcInstance *db.Instance) error {
	err := h.Bdl.DeleteServiceGroup("addon-"+tmcInstance.Engine, tmcInstance.ID)
	return err
}

func (h *DefaultDeployHandler) DeleteTenant(tenant *db.InstanceTenant, tmcInstance *db.Instance, clusterConfig map[string]string) error {
	return h.TenantDb.Model(tenant).Update("is_deleted", "Y").Error
}

func (h *DefaultDeployHandler) DeleteTmcInstance(tmcInstance *db.Instance, status string) error {
	return h.InstanceDb.Model(tmcInstance).
		Update(map[string]interface{}{
			"status":     status,
			"is_deleted": "Y",
		}).Error
}

func (h *DefaultDeployHandler) BuildDeployResult(tmcInstance *db.Instance, tenant *db.InstanceTenant) ResourceDeployResult {
	result := ResourceDeployResult{ID: tmcInstance.ID,
		Config:      map[string]string{},
		Options:     map[string]string{},
		Status:      tmcInstance.Status,
		CreatedTime: tmcInstance.CreateTime,
		UpdatedTime: tmcInstance.UpdateTime,
	}

	instanceConfig := map[string]string{}
	utils.JsonConvertObjToType(tmcInstance.Config, &instanceConfig)
	utils.AppendMap(result.Config, instanceConfig)
	if tenant != nil {
		result.ID = tenant.ID
		tenantConfig := map[string]string{}
		utils.JsonConvertObjToType(tenant.Config, &tenantConfig)
		utils.AppendMap(result.Config, tenantConfig)

		tenantOptions := map[string]string{}
		utils.JsonConvertObjToType(tenant.Options, &tenantOptions)
		utils.AppendMap(result.Options, tenantOptions)

		result.CreatedTime = tenant.CreateTime
		result.UpdatedTime = tenant.UpdateTime
	}

	return result
}

func (h *DefaultDeployHandler) BuildRequestRelation(parentRequest string, childRequest string) error {
	relation := db.TmcRequestRelation{ParentRequestId: parentRequest, ChildRequestId: childRequest}
	if err := h.TmcRequestRelationDb.Save(&relation).Error; err != nil {
		return err
	}
	return nil
}

func (h *DefaultDeployHandler) BuildSubResourceDeployRequest(name string, addon *diceyml.AddOn, req *ResourceDeployRequest) *ResourceDeployRequest {

	options := map[string]string{}
	utils.AppendMap(options, addon.Options)

	subReq := ResourceDeployRequest{
		Engine:      name,
		Uuid:        utils.GetRandomId(),
		Plan:        addon.Plan,
		Az:          req.Az,
		Options:     options,
		TenantGroup: req.TenantGroup,
		//Callback:    req.Callback, //  sub addon do not need perform callback action
	}

	return &subReq
}

func (h *DefaultDeployHandler) UpdateTmcInstanceOnCustom(tmcInstance *db.Instance, sgConfig map[string]string) error {
	// update config is_custom version status
	oldConfig := map[string]string{}
	_ = utils.JsonConvertObjToType(tmcInstance.Config, &oldConfig)
	utils.AppendMap(oldConfig, sgConfig)
	config, _ := utils.JsonConvertObjToString(oldConfig)

	return h.InstanceDb.Model(&tmcInstance).
		Update("config", config).
		Update("is_custom", "Y").
		Update("version", "custom").
		Update("status", TmcInstanceStatusRunning).Error
}

func (h *DefaultDeployHandler) CheckIfHasCustomConfig(clusterConfig map[string]string) (map[string]string, bool) {
	return nil, false
}

func NewDefaultHandler(dbClient *gorm.DB, logger logs.Logger) *DefaultDeployHandler {
	return &DefaultDeployHandler{
		TenantDb:             &db.InstanceTenantDB{DB: dbClient},
		InstanceDb:           &db.InstanceDB{DB: dbClient},
		TmcDb:                &db.TmcDB{DB: dbClient},
		TmcVersionDb:         &db.TmcVersionDB{DB: dbClient},
		TmcRequestRelationDb: &db.TmcRequestRelationDB{DB: dbClient},
		TmcIniDb:             &db.TmcIniDB{DB: dbClient},
		Bdl: bundle.New(bundle.WithScheduler(),
			bundle.WithOrchestrator(),
			bundle.WithDiceHub(),
			bundle.WithHepa(),
			bundle.WithCMDB(),
			bundle.WithCoreServices(),
			bundle.WithPipeline(),
			bundle.WithMonitor(),
			bundle.WithCollector(),
			bundle.WithHTTPClient(httpclient.New(httpclient.WithTimeout(time.Second*10, time.Second*60))),
		),
		Log: logger,
	}
}

func (h *DefaultDeployHandler) CheckIfNeedTmcInstance(req *ResourceDeployRequest, resourceInfo *ResourceInfo) (*db.Instance, bool, error) {
	instance, err := h.InstanceDb.GetByEngineAndVersionAndAz(resourceInfo.TmcVersion.Engine, "custom", req.Az)
	if err != nil {
		return nil, false, err
	}

	// we only care about the RUNNING one
	isValid := func(ins *db.Instance) bool {
		return instance != nil && instance.Status == TmcInstanceStatusRunning
	}

	if !isValid(instance) {
		instance, err = h.InstanceDb.GetByEngineAndVersionAndAz(resourceInfo.TmcVersion.Engine, resourceInfo.TmcVersion.Version, req.Az)
		if err != nil {
			return nil, false, err
		}
	}

	return instance, !isValid(instance), nil
}

func (h *DefaultDeployHandler) GetClusterConfig(az string) (map[string]string, error) {
	configmap, err := h.Bdl.QueryClusterInfo(az)
	if err != nil {
		return nil, err
	}

	config := map[string]string{}
	utils.JsonConvertObjToType(configmap, &config)
	return config, nil
}

func (h *DefaultDeployHandler) InitializeTmcInstance(req *ResourceDeployRequest, resourceInfo *ResourceInfo,
	subResults []*ResourceDeployResult) (*db.Instance, error) {

	options := map[string]string{}
	utils.AppendMap(options, req.Options)
	for _, subInstance := range subResults {
		var subConfig map[string]string
		if utils.JsonConvertObjToType(subInstance.Config, &subConfig) == nil {
			utils.AppendMap(options, subConfig)
		}
	}

	// for paas, no need to create tenant, use the req.uuid as instance id for identity
	var id string
	if resourceInfo.Tmc.DeployMode == DeployModePAAS {
		id = req.Uuid
	} else {
		id = utils.GetRandomId()
	}

	optionsStr, _ := utils.JsonConvertObjToString(options)
	instance := db.Instance{
		ID:         id,
		Engine:     resourceInfo.TmcVersion.Engine,
		Version:    resourceInfo.TmcVersion.Version,
		Az:         req.Az,
		ReleaseID:  resourceInfo.TmcVersion.ReleaseId,
		Options:    optionsStr,
		IsCustom:   "N",
		IsDeleted:  "N",
		CreateTime: time.Now(),
		UpdateTime: time.Now(),
		Status:     TmcInstanceStatusInit,
	}

	if err := h.InstanceDb.Save(&instance).Error; err != nil {
		return nil, err
	}

	return &instance, nil
}

func (h *DefaultDeployHandler) InitializeTmcInstanceTenant(req *ResourceDeployRequest,
	tmcInstance *db.Instance, subResults []*ResourceDeployResult) (*db.InstanceTenant, error) {

	// actually, for tenant, the options only need `tenantGroup` key,
	// append the req.Options which is addon.options seems something unnecessary, but for now, just leave it

	options := map[string]string{}
	_ = utils.JsonConvertObjToType(req.Options, &options)
	options["tenantGroup"] = req.TenantGroup // this line is necessary for sub addons

	for _, subInstance := range subResults {
		var subConfig map[string]string
		if utils.JsonConvertObjToType(subInstance.Config, &subConfig) == nil {
			utils.AppendMap(options, subConfig)
		}
	}

	// create tenant record
	optionStr, _ := utils.JsonConvertObjToString(options)
	tenant := db.InstanceTenant{
		ID:          req.Uuid,
		InstanceID:  tmcInstance.ID,
		TenantGroup: req.TenantGroup,
		Engine:      tmcInstance.Engine,
		Az:          req.Az,
		Options:     optionStr,
		Config:      "{}",
		CreateTime:  time.Now(),
		UpdateTime:  time.Now(),
		IsDeleted:   "N",
	}

	if err := h.TenantDb.Save(&tenant).Error; err != nil {
		return nil, err
	}

	return &tenant, nil
}

func (h *DefaultDeployHandler) UpdateTmcInstanceTenantOnFinish(tenant *db.InstanceTenant, config map[string]string) (*db.InstanceTenant, error) {
	// update tenant record
	oldConfig := map[string]string{}
	_ = utils.JsonConvertObjToType(tenant.Config, &oldConfig)
	utils.AppendMap(oldConfig, config)
	configStr, _ := utils.JsonConvertObjToString(oldConfig)

	if err := h.TenantDb.Model(&tenant).Update("config", configStr).Error; err != nil {
		return nil, err
	}

	return tenant, nil
}

func (h *DefaultDeployHandler) IsMatch(tmc *db.Tmc) bool {
	return true
}

func (h *DefaultDeployHandler) GetResourceInfo(req *ResourceDeployRequest) (*ResourceInfo, error) {
	info := ResourceInfo{}

	const VersionKey = "version"

	// old resource compatible
	if req.Engine == ResourceTerminusZKProxy {
		req.Engine = ResourceRegisterCenter
		if _, ok := req.Options[VersionKey]; ok {
			req.Options[VersionKey] = "1.0.0"
		}
	}
	if tmc, err := h.TmcDb.GetByEngine(req.Engine); err != nil {
		return nil, err
	} else if tmc == nil {
		return nil, fmt.Errorf("tmc not found")
	} else {
		info.Tmc = tmc
	}

	requestVersion := req.Options[VersionKey]
	if len(requestVersion) > 0 {
		if tmcVersion, err := h.TmcVersionDb.GetByEngine(req.Engine, requestVersion); err != nil {
			return nil, err
		} else if tmcVersion == nil {
			return nil, fmt.Errorf("tmcversion not found")
		} else {
			info.TmcVersion = tmcVersion
		}
	} else {
		if tmcVersion, err := h.TmcVersionDb.GetLatestVersionByEngine(req.Engine); err != nil {
			return nil, err
		} else if tmcVersion == nil {
			return nil, fmt.Errorf("tmcversion not found")
		} else {
			info.TmcVersion = tmcVersion
		}
	}

	resp, err := h.Bdl.GetExtensionVersion(apistructs.ExtensionVersionGetRequest{
		Version: info.TmcVersion.Version, Name: info.Tmc.Engine})
	if err != nil {
		return nil, err
	}

	var spec apistructs.AddonExtension
	if err := utils.JsonConvertObjToType(resp.Spec, &spec); err != nil {
		return nil, err
	}
	info.Spec = &spec

	var dice diceyml.Object
	if err := utils.JsonConvertObjToType(resp.Dice, &dice); err != nil {
		return nil, err
	}
	info.Dice = &dice

	// 继承 req 中的 addonDiskType、addonVolumeSize、addonSnapMaxHistory、alibabacloud.com/eci 等 options
	for _, addon := range info.Dice.AddOns {
		if addon.Options == nil {
			addon.Options = make(map[string]string)
		}

		// addonDiskType
		if _, ok := req.Options[diceyml.AddonDiskType]; ok {
			addon.Options[diceyml.AddonDiskType] = req.Options[diceyml.AddonDiskType]
		}

		// addonVolumeSize
		if _, ok := req.Options[diceyml.AddonVolumeSize]; ok {
			addon.Options[diceyml.AddonVolumeSize] = req.Options[diceyml.AddonVolumeSize]
		}

		// alibabacloud.com/eci
		if _, ok := req.Options[apistructs.AlibabaECILabel]; ok {
			addon.Options[apistructs.AlibabaECILabel] = req.Options[apistructs.AlibabaECILabel]
		}

		// addonSnapMaxHistory
		if _, ok := req.Options[diceyml.AddonSnapMaxHistory]; ok {
			addon.Options[diceyml.AddonSnapMaxHistory] = req.Options[diceyml.AddonSnapMaxHistory]
		}
	}

	return &info, nil
}

func (h *DefaultDeployHandler) DoPreDeployJob(resourceInfo *ResourceInfo, tmcInstance *db.Instance) error {
	return nil
}

// DoDeploy submit to scheduler and wait status ready
func (h *DefaultDeployHandler) DoDeploy(serviceGroupDeployRequest interface{}, resourceInfo *ResourceInfo, tmcInstance *db.Instance, clusterConfig map[string]string) (
	interface{}, error) {

	serviceGroup := serviceGroupDeployRequest.(*apistructs.ServiceGroupCreateV2Request)
	// request scheduler
	reqData, _ := utils.JsonConvertObjToString(serviceGroup)
	h.Log.Infof("about to call scheduler, request: %s", reqData)
	err := h.Bdl.CreateServiceGroup(*serviceGroup)
	if err != nil {
		h.Log.Infof("scheduler resp err: %s", err.Error())
		return nil, err
	}

	// wait ready status
	sgWithStatus, err := h.waitServiceGroupReady(serviceGroup)
	if err != nil {
		h.Bdl.DeleteServiceGroup(serviceGroup.Type, serviceGroup.ID)
		return nil, err
	}

	return sgWithStatus, nil
}

func (h *DefaultDeployHandler) BuildTmcInstanceConfig(tmcInstance *db.Instance, serviceGroupDeployResult interface{}, clusterConfig map[string]string, additionalConfig map[string]string) map[string]string {
	return map[string]string{}
}

func (h *DefaultDeployHandler) UpdateTmcInstanceOnFinish(tmcInstance *db.Instance, sgConfig map[string]string, sgStatus string) error {
	oldConfig := map[string]string{}
	utils.JsonConvertObjToType(tmcInstance.Config, &oldConfig)
	utils.AppendMap(oldConfig, sgConfig)
	config, _ := utils.JsonConvertObjToString(oldConfig)

	if err := h.InstanceDb.Model(tmcInstance).
		Update(map[string]interface{}{"config": config, "status": sgStatus}).Error; err != nil {
		return err
	}

	return nil
}

func (h *DefaultDeployHandler) DoPostDeployJob(tmcInstance *db.Instance, serviceGroupDeployResult interface{}, clusterConfig map[string]string) (map[string]string, error) {
	return nil, nil
}

func (h *DefaultDeployHandler) CheckIfNeedTmcInstanceTenant(req *ResourceDeployRequest, resourceInfo *ResourceInfo) (*db.InstanceTenant, bool, error) {
	need := resourceInfo.Tmc.DeployMode == DeployModeSAAS

	tenant, err := h.TenantDb.GetByID(req.Uuid)
	if err != nil {
		return nil, need, err
	}

	// if tenant already marked deleted, the caller(orchestrator) should use new uuid for next request
	// we return error here if the same failed id came again
	if tenant != nil && tenant.IsDeleted == "Y" {
		return tenant, need, fmt.Errorf("tenant id not valid")
	}

	return tenant, need && tenant == nil, nil
}

func (h *DefaultDeployHandler) DoApplyTmcInstanceTenant(req *ResourceDeployRequest, resourceInfo *ResourceInfo,
	tmcInstance *db.Instance, tenant *db.InstanceTenant, clusterConfig map[string]string) (map[string]string, error) {
	return map[string]string{}, nil
}

func (h *DefaultDeployHandler) BuildServiceGroupRequest(resourceInfo *ResourceInfo, tmcInstance *db.Instance, clusterConfig map[string]string) interface{} {
	// create common labels
	labels := map[string]string{
		"SERVICE_TYPE":             "ADDONS",
		"DICE_ADDON":               tmcInstance.ID,
		"DICE_ADDON_TYPE":          tmcInstance.Engine,
		"LOCATION-CLUSTER-SERVICE": "",
		"ADDON_TYPE":               tmcInstance.ID,
		"ADDON_ID":                 tmcInstance.ID,
	}

	options := map[string]string{}
	utils.JsonConvertObjToType(tmcInstance.Options, &options)
	if v, ok := options["instanceName"]; ok {
		labels["DICE_ADDON_NAME"] = v
	}
	labels["ADDON_GROUPS"] = "1"

	az := tmcInstance.Az

	// build scheduler request
	req := apistructs.ServiceGroupCreateV2Request{
		ClusterName: az,
		ID:          tmcInstance.ID,
		Type:        "addon-" + tmcInstance.Engine,
		DiceYml:     *resourceInfo.Dice, // todo should deep copy here?
		GroupLabels: labels,
	}

	for _, service := range req.DiceYml.Services {
		utils.AppendMap(service.Envs, labels)
		if service.Labels == nil {
			service.Labels = make(map[string]string)
		}
		utils.SetlabelsFromOptions(options, service.Labels)
	}

	return &req
}

// wait servicegroup ready and return the latest servicegroup obj with status
func (h *DefaultDeployHandler) waitServiceGroupReady(req *apistructs.ServiceGroupCreateV2Request) (
	*apistructs.ServiceGroup, error) {

	var lastStatus apistructs.StatusDesc
	startTime := time.Now().Unix()

	for time.Now().Unix()-startTime < RuntimeMaxUpTimeoutSeconds {
		time.Sleep(10 * time.Second)

		serviceGroup, err := h.Bdl.InspectServiceGroup(req.Type, req.ID)
		if serviceGroup != nil {
			lastStatus = serviceGroup.StatusDesc
		}
		if err != nil {
			continue
		}

		if serviceGroup.Status == apistructs.StatusReady || serviceGroup.Status == apistructs.StatusHealthy {
			return serviceGroup, nil
		}
	}

	return nil, fmt.Errorf("wait servicegroup up timeout, last status: %+v", lastStatus)
}

func (h *DefaultDeployHandler) Callback(url string, id string, success bool, config map[string]string, options map[string]string, errMsg string) error {

	userId := h.GetDiceOperatorId()

	req := struct {
		IsSuccess bool   `json:"isSuccess"`
		ErrMsg    string `json:"errMsg"`
	}{IsSuccess: success, ErrMsg: errMsg}

	h.Log.Infof("about to callback, request:%+v", req)

	var body bytes.Buffer
	resp, err := httpclient.New().
		Post(url+"/api/addon-platform/addons/"+id+"/action/provision").
		Header("User-ID", userId).
		JSONBody(req).
		Do().
		Body(&body)

	if err != nil {
		h.Log.Errorf("callback orchestrator err:%s, resp body:%s", err.Error(), body.String())
		return err
	}

	if !resp.IsOK() {
		h.Log.Errorf("callback orchestrator status code error[%d], resp body:%s", resp.StatusCode(), body.String())
		return nil
	}

	var configList []apistructs.AddonConfigCallBackItemResponse
	for k, v := range config {
		configList = append(configList, apistructs.AddonConfigCallBackItemResponse{Name: k, Value: v})
	}

	version := config["version"]
	configReq := apistructs.AddonConfigCallBackResponse{
		Label:   map[string]string{},
		Version: version,
		Config:  configList,
	}

	_, _ = httpclient.New().
		Post(url + "/api/addon-platform/addons/" + id + "/config").
		JSONBody(configReq).
		Do().
		DiscardBody()

	return nil
}

func (h *DefaultDeployHandler) GetInstanceById(id string) (instance *db.Instance, tenant *db.InstanceTenant, tmc *db.Tmc, dice *diceyml.Object, err error) {
	tenant, err = h.TenantDb.GetByID(id)
	if err != nil {
		return
	}

	if tenant != nil {
		instance, err = h.InstanceDb.GetByID(tenant.InstanceID)
		if err != nil {
			return
		}
	} else {
		instance, err = h.InstanceDb.GetByID(id)
		if err != nil {
			return
		}
	}
	if instance == nil && tenant == nil {
		err = fmt.Errorf("resource not found")
		return
	}

	engine := ""
	if instance != nil {
		engine = instance.Engine
	} else {
		engine = tenant.Engine
	}

	tmc, err = h.TmcDb.GetByEngine(engine)
	if err != nil {
		return
	}
	addon, err := h.Bdl.GetExtensionVersion(apistructs.ExtensionVersionGetRequest{
		Version: instance.Version, Name: instance.Engine})
	if err == nil {
		var diceYaml diceyml.Object
		utils.JsonConvertObjToType(addon.Dice, &diceYaml)
		dice = &diceYaml
	}
	return
}

func (h *DefaultDeployHandler) GetDiceOperatorId() string {
	userID := "1100"
	if oid := os.Getenv("DICE_OPERATOR_ID"); len(oid) > 0 {
		userID = oid
	}
	return userID
}

func (h *DefaultDeployHandler) IsNotDCOSCluster(clusterType string) bool {
	return clusterType != "dcos"
}
