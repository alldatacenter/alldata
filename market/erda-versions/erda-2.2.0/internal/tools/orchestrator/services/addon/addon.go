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

package addon

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"reflect"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"google.golang.org/grpc/metadata"

	"github.com/erda-project/erda-infra/pkg/transport"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	tenantpb "github.com/erda-project/erda-proto-go/msp/tenant/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/orchestrator/components/addon/mysql"
	"github.com/erda-project/erda/internal/tools/orchestrator/conf"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	i18n2 "github.com/erda-project/erda/internal/tools/orchestrator/i18n"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/cap"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/clusterinfo"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/instanceinfo"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/servicegroup"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/apierrors"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/resource"
	"github.com/erda-project/erda/internal/tools/orchestrator/utils"
	"github.com/erda-project/erda/pkg/crypto/encryption"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/i18n"
	"github.com/erda-project/erda/pkg/kms/kmstypes"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/sexp"
	"github.com/erda-project/erda/pkg/strutil"
)

// ErdaEncryptedValue encrypted value
const ErdaEncryptedValue string = "ERDA_ENCRYPTED"

var (
	// AddonInfos 市场addon全集 key: addonName, value: apistructs.Extension
	AddonInfos sync.Map
	// ProjectInfos 项目信息缓存  key: projectId, value: apistructs.ProjectDTO
	ProjectInfos sync.Map
	// AppInfos app列表缓存 key: appID, value: apistructs.ApplicationDTO
	AppInfos sync.Map
)

// ExtensionDeployAddon .
var ExtensionDeployAddon = map[string]string{"mysql": "", "redis": "", "consul": "", "canal": "", "kafka": "", "rabbitmq": "", "rocketmq": "", "terminus-elasticsearch": "", "terminus-zookeeper": ""} // key 为 appID, value 为应用详情

// Addon addon 实例对象封装
type Addon struct {
	db               *dbclient.DBClient
	bdl              *bundle.Bundle
	hc               *httpclient.HTTPClient
	encrypt          *encryption.EnvEncrypt
	resource         *resource.Resource
	kms              mysql.KMSWrapper
	cap              cap.Cap
	serviceGroupImpl servicegroup.ServiceGroup
	instanceinfoImpl *instanceinfo.InstanceInfoImpl
	clusterinfoImpl  clusterinfo.ClusterInfo
	clusterSvc       clusterpb.ClusterServiceServer
	tenantSvc        tenantpb.TenantServiceServer
}

// Option addon 实例对象配置选项
type Option func(*Addon)

// New 新建 addon service
func New(options ...Option) *Addon {
	var addon Addon
	for _, op := range options {
		op(&addon)
	}

	return &addon
}

// WithDBClient 配置 db client
func WithDBClient(db *dbclient.DBClient) Option {
	return func(a *Addon) {
		a.db = db
	}
}

// WithBundle 配置 bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(a *Addon) {
		a.bdl = bdl
	}
}

// WithHTTPClient 配置 http 客户端对象.
func WithHTTPClient(hc *httpclient.HTTPClient) Option {
	return func(a *Addon) {
		a.hc = hc
	}
}

// WithEnvEncrypt 设置 encrypt service
func WithEnvEncrypt(encrypt *encryption.EnvEncrypt) Option {
	return func(a *Addon) {
		a.encrypt = encrypt
	}
}

// WithResource 配置 Runtime service
func WithResource(resource *resource.Resource) Option {
	return func(a *Addon) {
		a.resource = resource
	}
}

// WithKMSWrapper .
func WithKMSWrapper(kms mysql.KMSWrapper) Option {
	return func(a *Addon) {
		a.kms = kms
	}
}

func WithCap(cap cap.Cap) Option {
	return func(a *Addon) {
		a.cap = cap
	}
}

// WithServiceGroup 配置 serviceGroupImpl
func WithServiceGroup(serviceGroupImpl servicegroup.ServiceGroup) Option {
	return func(a *Addon) {
		a.serviceGroupImpl = serviceGroupImpl
	}
}

// WithInstanceinfoImpl 配置 instanceinfoImpl
func WithInstanceinfoImpl(instanceinfoImpl *instanceinfo.InstanceInfoImpl) Option {
	return func(a *Addon) {
		a.instanceinfoImpl = instanceinfoImpl
	}
}

func WithClusterInfoImpl(instanceinfoImpl clusterinfo.ClusterInfo) Option {
	return func(a *Addon) {
		a.clusterinfoImpl = instanceinfoImpl
	}
}

func WithClusterSvc(clusterSvc clusterpb.ClusterServiceServer) Option {
	return func(a *Addon) {
		a.clusterSvc = clusterSvc
	}
}

func WithTenantSvc(tenantSvc tenantpb.TenantServiceServer) Option {
	return func(a *Addon) {
		a.tenantSvc = tenantSvc
	}
}

func (a *Addon) ExportLogInfo(level apistructs.ErrorLogLevel, tp apistructs.ErrorResourceType, id, dedupid string, format string, params ...interface{}) {
	s := fmt.Sprintf(format, params...)
	if err := a.bdl.CreateErrorLog(&apistructs.ErrorLogCreateRequest{
		ErrorLog: apistructs.ErrorLog{
			ResourceType:   tp,
			Level:          level,
			ResourceID:     id,
			OccurrenceTime: strconv.FormatInt(time.Now().Unix(), 10),
			HumanLog:       s,
			PrimevalLog:    s,
			DedupID:        fmt.Sprintf("orch-%s", dedupid),
		},
	}); err != nil {
		logrus.Errorf("[ExportLogInfo]: %v", err)
	}
}
func (a *Addon) ExportLogInfoDetail(level apistructs.ErrorLogLevel, tp apistructs.ErrorResourceType, id string, humanlog, detaillog string) {
	a.bdl.CreateErrorLog(&apistructs.ErrorLogCreateRequest{
		ErrorLog: apistructs.ErrorLog{
			ResourceType:   tp,
			Level:          level,
			ResourceID:     id,
			OccurrenceTime: strconv.FormatInt(time.Now().Unix(), 10),
			HumanLog:       humanlog,
			PrimevalLog:    detaillog,
			DedupID:        fmt.Sprintf("orch-%s", id),
		},
	})
}

// BatchCreate 批量创建 addons
func (a *Addon) BatchCreate(req *apistructs.AddonCreateRequest) error {

	// TODO 实现
	// 1. 参数校验
	// 2. 兼容 prebuild
	// 3. 构建 DAG
	// 4. 调用 scheduler API 递归创建 addon
	if err := a.checkCreateParams(req); err != nil {
		logrus.Errorf("参数检查失败，%v", err)
		return err
	}

	// plan转换
	a.transPlan(req.Addons)

	existBuilds, err := a.db.GetPreBuildsByRuntimeID(req.RuntimeID)
	if err != nil {
		return err
	}
	defaultAddons := a.getDefaultPreBuildList(req.ApplicationID, req.RuntimeID, req.RuntimeName, req.Workspace)
	if len(req.Addons) == 0 {
		// 若默认 addon 不存在，注入默认 addons
		req.Addons = []apistructs.AddonCreateItem{}
		for _, defaulPre := range defaultAddons {
			req.Addons = append(req.Addons, apistructs.AddonCreateItem{
				Name: defaulPre.InstanceName,
				Type: defaulPre.AddonName,
				Plan: defaulPre.Plan,
			})
		}
	}
	var exitMonitor bool
	for _, v := range req.Addons {
		if v.Type == apistructs.AddonMonitor {
			exitMonitor = true
		}
	}
	if !exitMonitor {
		req.Addons = append(req.Addons, apistructs.AddonCreateItem{
			Name: apistructs.AddonMonitor,
			Type: apistructs.AddonMonitor,
			Plan: apistructs.AddonProfessional,
		})
	}
	// 给定 projectID+workspace下是否有zk/roost
	zkExist, err := a.db.ExistZK(req.ProjectID, req.ClusterName, req.Workspace)
	if err != nil {
		return err
	}

	roostExist, err := a.db.ExistRoost(req.ProjectID, req.ClusterName, req.Workspace)
	if err != nil {
		return err
	}

	// zookeeper属性判断
	zkCanDeploy := a.zkCanDeploy()

	logrus.Infof("zkCanDeploy value is: %v", zkCanDeploy)

	clusterInfo, err := a.clusterinfoImpl.Info(req.ClusterName)
	if err != nil {
		return err
	}
	if clusterInfo[apistructs.DICE_CLUSTER_TYPE] != apistructs.EDAS {
		for i, v := range req.Addons {
			newAddonName := a.parseAddonName(v.Type)
			// 如果zk具备canDeploy属性，则对外展现,可以对外发布
			// zkCanDeploy属性是针对特定的集群，添加的策略，有些私有化集群，如兖矿、家家悦，烟草，他们是已经上线的集群，不能让他们走多租户注册中心
			// 所以要提供zookeeper的部署。
			// 但是还有一个问题存在，就是他们的服务已经跑了老的注册中心的情况下，不能被下面这个判断所影响，所以需要加一个zkExist(是否存在老的zk或者roost)
			// 如果存在，则不走下面这个判断逻辑。这样做的目的，是为了兼容新老roost和zk的情况
			if newAddonName == apistructs.AddonZookeeper && zkCanDeploy && !roostExist {
				continue
			}
			if !zkExist && !roostExist && newAddonName == apistructs.AddonZookeeper {
				return errors.Errorf("terminus-zookeeper is not recommended, please use the regsitercenter instead")
			}
			if !zkExist && !roostExist && newAddonName == apistructs.AddonRoost {
				req.Addons[i].Type = apistructs.AddonZKProxy
			}
			if (zkExist || roostExist) && newAddonName == apistructs.AddonZookeeper {
				req.Addons[i].Type = apistructs.AddonRoost
			}
		}
	}

	// prebuild 两种情况: 1. dice.yml 里删除后又在 dice.yml 里添加相同 addon 2. dice.yml 里未删除，在 UI 已删除，以 UI 删除为优先
	// 若第一次部署，preBuilds 为空
	existBuildMap := make(map[string]dbclient.AddonPrebuild, len(*existBuilds))
	for _, v := range *existBuilds {
		existBuildMap[strutil.Concat(v.RuntimeID, v.AddonName, v.InstanceName)] = v
	}
	// 找出新增 addons，添加至 prebuild
	addonPrebuildList := make([]dbclient.AddonPrebuild, 0, len(req.Addons)) // 新增 addons
	newPrebuildList := make([]dbclient.AddonPrebuild, 0, len(req.Addons))   // 新 addons 列表
	for _, v := range req.Addons {
		newAddonName := a.parseAddonName(v.Type)
		if old, ok := existBuildMap[fmt.Sprintf("%d%s%s", req.RuntimeID, newAddonName, v.Name)]; ok {
			switch old.DeleteStatus {
			case apistructs.AddonPrebuildDiceYmlDeleted: // 若 addon 在 prebuild 已存在，且历史从 dice.yml 删除
				// 更新删除状态为未删除
				old.DeleteStatus = apistructs.AddonPrebuildNotDeleted
				if err := a.db.UpdatePrebuild(&old); err != nil {
					return err
				}
				newPrebuildList = append(newPrebuildList, old)
			case apistructs.AddonPrebuildNotDeleted:
				if len(v.Options) > 0 {
					options, err := json.Marshal(v.Options)
					if err != nil {
						return err
					}
					if v.Plan != old.Plan || (string(options) != old.Options) {
						old.Plan = v.Plan
						old.Options = string(options)
						if err := a.db.UpdatePrebuild(&old); err != nil {
							return err
						}
					}
				}
				if len(v.Options) == 0 && old.Options != "" {
					old.Plan = v.Plan
					old.Options = ""
					if err := a.db.UpdatePrebuild(&old); err != nil {
						return err
					}
				}

				newPrebuildList = append(newPrebuildList, old)

			}
		} else {
			ap := a.ParsePreBuild(req.ApplicationID, req.RuntimeID, req.RuntimeName, req.Workspace, v)
			if ap.AddonName == apistructs.AddonRoost && !zkExist && !roostExist { // 若 dice.yml 里指定 roost，替换为 registercenter
				//ap.InstanceName = apistructs.AddonZKProxy
				ap.AddonName = apistructs.AddonZKProxy
			}
			addonPrebuildList = append(addonPrebuildList, *ap)
			newPrebuildList = append(newPrebuildList, *ap)
		}
	}
	if len(newPrebuildList) > 0 {
		logrus.Infof("new prebuild list: %+v", newPrebuildList)
	}
	// prebuild 入库
	for _, v := range addonPrebuildList {
		if err := a.db.CreatePrebuild(&v); err != nil {
			return err
		}
	}
	// 找出 prebuild 表存在，但 dice.yml 里已去除的 addon, 删除 prebuild 记录
	if err := a.removeUselessPrebuilds(req.RuntimeID, newPrebuildList); err != nil {
		return err
	}

	// 找出待部署addon & 待删除addon
	deploys, removes, err := a.GetDeployAndRemoveAddons(req.RuntimeID)
	// 删除 addons
	for i := range removes {
		// 软删除，并未真的删除实例(由定时任务负责真正删除)
		if err := a.deleteByRuntimeIDAndInstanceID(req.RuntimeID, removes[i].ID); err != nil {
			logrus.Warnf("failed to delete addon instance, err: %v", err)
		}
	}
	// 递归创建 addons
	if err := a.deployAddons(req, deploys); err != nil {
		return err
	}
	return nil
}

// zkCanDeploy 返回zk的属性参数
func (a *Addon) zkCanDeploy() bool {
	return conf.DeployZookeeper() != ""
}

// transPlan plan格式转换
func (a *Addon) transPlan(addons []apistructs.AddonCreateItem) {
	for i := range addons {
		switch addons[i].Plan {
		case "large", apistructs.AddonUltimate:
			addons[i].Plan = apistructs.AddonUltimate
		case "medium", apistructs.AddonProfessional:
			addons[i].Plan = apistructs.AddonProfessional
		case "small", apistructs.AddonBasic:
			addons[i].Plan = apistructs.AddonBasic
		default:
			addons[i].Plan = apistructs.AddonBasic
		}
	}
}

// RuntimeAddonStatus runtime addo status, ensure that
// an error will only be returned when the status is 0
func (a *Addon) RuntimeAddonStatus(runtimeID string) (uint8, error) {
	if runtimeID == "" {
		return 0, errors.New("runtimeId 不能为空")
	}
	runtimeIDUint, err := strconv.ParseUint(runtimeID, 10, 64)
	if err != nil {
		return 0, errors.New("runtimeId必须为int类型")
	}
	// 查询prebuild信息
	addonPrebuildData, err := a.db.GetPreBuildsByRuntimeID(runtimeIDUint)
	if err != nil {
		return 0, err
	}
	if len(*addonPrebuildData) == 0 {
		return 1, nil
	}
	// 查询实例信息
	addonListData, err := a.db.GetAttachMentsByRuntimeID(runtimeIDUint)
	//addonListData, err := a.ListByRuntime(runtimeIDUint, "", "")
	if addonListData == nil || len(*addonListData) == 0 {
		for _, prebuild := range *addonPrebuildData {
			if prebuild.DeleteStatus == apistructs.AddonPrebuildNotDeleted {
				return 2, nil
			}
		}
		return 1, nil
	}
	var insMap = make(map[string]string, len(*addonListData))
	for _, ins := range *addonListData {
		if ins.RoutingInstanceID == "" {
			continue
		}
		routing, err := a.db.GetInstanceRouting(ins.RoutingInstanceID)
		if err != nil {
			return 0, err
		}
		if routing == nil {
			return 0, errors.Errorf("instance routing is not existed: %s", ins.RoutingInstanceID)
		}
		insMap[routing.ID] = routing.Status
		if routing.Status == string(apistructs.AddonAttaching) {
			return 2, nil
		}
	}

	for _, prebuild := range *addonPrebuildData {
		if prebuild.DeleteStatus == apistructs.AddonPrebuildDiceYmlDeleted {
			continue
		}
		if prebuild.DeleteStatus == apistructs.AddonPrebuildUIDeleted {
			continue
		}
		if _, ok := insMap[prebuild.RoutingInstanceID]; !ok {
			fmt.Printf("RuntimeAddonStatus error, routingId not found: %v\n", prebuild.RoutingInstanceID)
			return 0, fmt.Errorf("addon: %s 部署失败，请查看日志详情获知错误原因，并尝试重新部署，若还无法解决请联系系统管理员",
				prebuild.AddonName)
		}
	}

	return 1, nil
}

// RuntimeAddonRemove runtime addon删除
func (a *Addon) RuntimeAddonRemove(runtimeID, env, operatorId string, projectId uint64) error {
	if runtimeID == "" {
		return errors.New("runtimeId 不能为空")
	}
	if err := a.db.DestroyPrebuildByRuntimeID(runtimeID); err != nil {
		return err
	}
	runtimeIDUint, err := strconv.ParseUint(runtimeID, 10, 64)
	if err != nil {
		return errors.New("runtimeId必须为int类型")
	}

	addons, err := a.db.GetAttachMentsByRuntimeID(runtimeIDUint)
	if err != nil {
		return err
	}
	if len(*addons) == 0 {
		return nil
	}

	for _, att := range *addons {
		ins, err := a.db.GetInstanceRouting(att.RoutingInstanceID)
		if err != nil {
			return err
		}
		if ins == nil {
			continue
		}
		if err := a.deleteByRuntimeIDAndInstanceID(runtimeIDUint, ins.ID); err != nil {
			return err
		}

	}

	return nil
}

// AddonProvisionCallback addon创建回调
func (a *Addon) AddonProvisionCallback(insId string, response *apistructs.AddonCreateCallBackResponse) error {
	if response == nil {
		return errors.New("AddonProvisionCallback方法接收参数为nil")
	}
	addonIns, err := a.db.GetAddonInstance(insId)
	if err != nil {
		return err
	}
	if addonIns == nil || addonIns.ID == "" {
		return errors.New("找不到对应addon instance信息，instanceID：" + insId)
	}
	// addon终结状态不需要修改
	if addonIns.Status == string(apistructs.AddonAttached) || addonIns.Status == string(apistructs.AddonAttachFail) {
		return nil
	}
	if addonIns.Status != string(apistructs.AddonAttaching) {
		logrus.Errorf("实例运行状态错误, instanceID: %v", insId)
		return errors.New("实例运行状态错误")
	}
	// 更新状态字段
	if response.IsSuccess {
		addonIns.Status = string(apistructs.AddonAttached)
		if err := a.db.UpdateAddonInstance(addonIns); err != nil {
			return err
		}
		routings, err := a.db.GetInstanceRoutingByRealInstance(insId)
		if err != nil {
			return err
		}
		// 更新routing信息
		for _, routingItem := range *routings {
			routingItem.Status = string(apistructs.AddonAttached)
			if err := a.db.UpdateInstanceRouting(&routingItem); err != nil {
				return err
			}
		}
	} else {
		var options map[string]string
		_ = json.Unmarshal([]byte(addonIns.Options), &options)
		a.pushLogCore(fmt.Sprintf("err when deploy addon: %s(%s), err: %s", addonIns.Name, addonIns.AddonName, response.ErrMsg), options)

		attachs, err := a.db.GetAttachmentsByInstanceID(insId)
		if err != nil {
			return err
		}
		// 更新attachments信息
		for _, attItem := range *attachs {
			attItem.Deleted = apistructs.AddonDeleted
			a.db.UpdateAttachment(&attItem)
		}
		addonIns.Status = string(apistructs.AddonAttachFail)
		if err := a.db.UpdateAddonInstance(addonIns); err != nil {
			return err
		}
		routings, err := a.db.GetInstanceRoutingByRealInstance(insId)
		if err != nil {
			return err
		}
		// 更新routing信息
		for _, routingItem := range *routings {
			routingItem.Status = string(apistructs.AddonAttachFail)
			if err := a.db.UpdateInstanceRouting(&routingItem); err != nil {
				return err
			}
		}
	}
	return nil
}

// AddonDeprovisionCallback addon删除回调
func (a *Addon) AddonDeprovisionCallback(insId string) error {
	if insId == "" {
		return errors.New("addon删除回调方法接收参数为nil")
	}
	addonIns, err := a.db.GetAddonInstance(insId)
	if err != nil {
		return err
	}
	if addonIns.ID == "" {
		return errors.New("找不到对应addon instance信息，instanceID：" + insId)
	}
	// addon终结状态不需要修改
	if addonIns.Status == string(apistructs.AddonDetached) {
		return nil
	}
	attachs, err := a.db.GetAttachmentsByInstanceID(insId)
	if err != nil {
		return err
	}
	// 更新attachments信息
	for _, attItem := range *attachs {
		attItem.Deleted = apistructs.AddonDeleted
		a.db.UpdateAttachment(&attItem)
	}
	addonIns.Status = string(apistructs.AddonDetached)
	if err := a.db.UpdateAddonInstance(addonIns); err != nil {
		return err
	}
	routings, err := a.db.GetInstanceRoutingByRealInstance(insId)
	if err != nil {
		return err
	}
	// 更新routing信息
	for _, routingItem := range *routings {
		routingItem.Status = string(apistructs.AddonDetached)
		if err := a.db.UpdateInstanceRouting(&routingItem); err != nil {
			return err
		}
	}

	return nil
}

// AddonConfigCallback addon配置回调
func (a *Addon) AddonConfigCallback(insId string, response *apistructs.AddonConfigCallBackResponse) error {
	if insId == "" {
		return errors.New("addon配置回调方法接收参数addonId为nil")
	}
	if response == nil || len(response.Config) == 0 {
		return errors.New("addon配置回调方法接收参数为nil")
	}
	addonIns, err := a.db.GetAddonInstance(insId)
	if err != nil {
		return err
	}
	if addonIns.ID == "" {
		return errors.New("找不到对应addon instance信息，instanceID：" + insId)
	}

	var configMap = make(map[string]interface{})
	for _, v := range response.Config {
		configMap[v.Name] = v.Value
	}
	configBytes, err := json.Marshal(configMap)
	if err != nil {
		return err
	}
	if len(response.Label) > 0 {
		labelBytes, err := json.Marshal(response.Label)
		if err != nil {
			return err
		}
		addonIns.Label = string(labelBytes)
	}

	// 更新instance
	addonIns.Status = string(apistructs.AddonAttached)
	addonIns.Config = string(configBytes)
	if err := a.db.UpdateAddonInstance(addonIns); err != nil {
		return err
	}
	routings, err := a.db.GetInstanceRoutingByRealInstance(insId)
	if err != nil {
		return err
	}
	// 更新routing信息
	for _, routingItem := range *routings {
		routingItem.Status = string(apistructs.AddonAttached)
		if err := a.db.UpdateInstanceRouting(&routingItem); err != nil {
			return err
		}
	}
	return nil
}

// CreateCustom 创建自定义 addon
func (a *Addon) CreateCustom(req *apistructs.CustomAddonCreateRequest) (*map[string]string, error) {
	// 获取addon extension信息
	addonExtension := apistructs.Extension{}
	if v, ok := AddonInfos.Load(req.AddonName); !ok {
		return nil, errors.Errorf("not found addon: %s", req.AddonName)
	} else {
		addonExtension = v.(apistructs.Extension)
	}

	// 校验 project 是否存在
	project, err := a.bdl.GetProject(req.ProjectID)
	if err != nil {
		return nil, err
	}
	// 增加clusterName过滤
	clusterName := project.ClusterConfig[req.Workspace]
	// 判断项目级共享 addon 是否已存在
	addon, err := a.db.GetRoutingInstanceByProjectAndName(project.ID, req.Workspace, req.AddonName, req.Name, clusterName)
	if err != nil {
		return nil, err
	}

	// 创建自定义 addon
	if addon == nil {

		// 申请kms key id
		kmsKey, err := a.bdl.KMSCreateKey(apistructs.KMSCreateKeyRequest{
			CreateKeyRequest: kmstypes.CreateKeyRequest{
				PluginKind: kmstypes.PluginKind_DICE_KMS,
			},
		})
		if err != nil {
			return nil, err
		}

		instanceID := a.getRandomId()
		// 云addon创建
		if req.CustomAddonType == apistructs.CUSTOM_TYPE_CLOUD {
			newCreate := true
			if v, ok := req.Options["instanceID"]; ok {
				if v != "" {
					newCreate = false
				}
			}
			optionsMap := req.Options
			optionsMap["clientToken"] = instanceID
			optionsMap["clusterName"] = clusterName
			optionsMap["projectID"] = strconv.FormatUint(project.ID, 10)
			optionsMap["source"] = "addon"
			optionsMap["customAddonType"] = req.CustomAddonType
			optionStr, err := json.Marshal(optionsMap)
			if err != nil {
				return nil, err
			}
			// create instance
			instance := &dbclient.AddonInstance{
				ID:         instanceID,
				Name:       req.Name,
				AddonName:  req.AddonName,
				Plan:       apistructs.AddonBasic,
				Version:    "1.0.0",
				OrgID:      strconv.FormatUint(project.OrgID, 10),
				ProjectID:  strconv.FormatUint(project.ID, 10),
				ShareScope: apistructs.ProjectShareScope,
				Options:    string(optionStr),
				Workspace:  req.Workspace,
				Status:     string(apistructs.AddonAttaching),
				Cluster:    project.ClusterConfig[req.Workspace],
				Category:   addonExtension.Category,
				CreatedAt:  time.Now(),
				UpdatedAt:  time.Now(),
				Deleted:    apistructs.AddonNotDeleted,
			}
			if err := a.db.CreateAddonInstance(instance); err != nil {
				return nil, err
			}
			logrus.Infof("cloud addon,  instance:%+v", instance)

			// create routing instance
			routingIntance := &dbclient.AddonInstanceRouting{
				ID:           a.getRandomId(),
				RealInstance: instanceID,
				Name:         req.Name,
				AddonName:    req.AddonName,
				Plan:         apistructs.AddonBasic,
				Version:      "1.0.0",
				OrgID:        strconv.FormatUint(project.OrgID, 10),
				ProjectID:    strconv.FormatUint(project.ID, 10),
				Options:      string(optionStr),
				ShareScope:   apistructs.ProjectShareScope,
				Workspace:    req.Workspace,
				Status:       string(apistructs.AddonAttaching),
				Cluster:      project.ClusterConfig[req.Workspace],
				Category:     addonExtension.Category,
				Tag:          req.Tag,
				CreatedAt:    time.Now(),
				UpdatedAt:    time.Now(),
				InsideAddon:  apistructs.NOT_INSIDE,
				Deleted:      apistructs.AddonNotDeleted,
			}
			if err := a.db.CreateAddonInstanceRouting(routingIntance); err != nil {
				return nil, err
			}

			// create cloud addon & update instance config
			var recordID uint64
			cloudName, resourceName := transCustomName2CloudName(req.AddonName)
			if newCreate {
				resp, err := a.bdl.CreateCloudAddon(strconv.FormatUint(project.OrgID, 10), req.OperatorID, cloudName, &optionsMap)
				if err != nil {
					return nil, err
				}
				recordID = resp.RecordID
			} else {
				resp, err := a.bdl.CreateCloudAddonWithInstance(strconv.FormatUint(project.OrgID, 10), req.OperatorID, cloudName, resourceName, &optionsMap)
				if err != nil {
					return nil, err
				}
				recordID = resp.RecordID
			}
			optionsMap["recordId"] = recordID
			optionStr, err = json.Marshal(optionsMap)
			if err != nil {
				return nil, err
			}

			// update addon instance
			instance, err = a.db.GetAddonInstance(instance.ID)
			if err != nil {
				return nil, err
			}
			instance.Options = string(optionStr)
			if err = a.db.UpdateAddonInstance(instance); err != nil {
				return nil, err
			}
			// update addon routingInstance
			routingIntance, err = a.db.GetInstanceRouting(routingIntance.ID)
			if err != nil {
				return nil, err
			}
			routingIntance.Options = string(optionStr)
			if err = a.db.UpdateInstanceRouting(routingIntance); err != nil {
				return nil, err
			}

			return &map[string]string{"instanceId": routingIntance.RealInstance, "routingInstanceId": routingIntance.ID}, nil
		} else {
			logExporterMap := map[string]string{}
			for k, v := range req.Configs {
				// 环境变量interface转为string操作
				// ============= start ===============
				switch t := v.(type) {
				case string:
					logExporterMap[k] = t
				default:
					logExporterMap[k] = fmt.Sprintf("%v", t)
				}
				// ============= end ===============
				if strings.Contains(strings.ToLower(k), "pass") || strings.Contains(strings.ToLower(k), "secret") {
					encryptData, err := a.bdl.KMSEncrypt(apistructs.KMSEncryptRequest{
						EncryptRequest: kmstypes.EncryptRequest{
							KeyID:           kmsKey.KeyMetadata.KeyID,
							PlaintextBase64: base64.StdEncoding.EncodeToString([]byte(v.(string))),
						},
					})
					if err != nil {
						return nil, err
					}
					req.Configs[k] = encryptData.CiphertextBase64
				}
			}
			configs, err := json.Marshal(req.Configs)
			if err != nil {
				return nil, err
			}

			instance := &dbclient.AddonInstance{
				ID:         instanceID,
				Name:       req.Name,
				AddonName:  req.AddonName,
				Plan:       apistructs.AddonBasic,
				Version:    "1.0.0",
				OrgID:      strconv.FormatUint(project.OrgID, 10),
				ProjectID:  strconv.FormatUint(project.ID, 10),
				ShareScope: apistructs.ProjectShareScope,
				Workspace:  req.Workspace,
				Config:     string(configs),
				Status:     string(apistructs.AddonAttached),
				Cluster:    project.ClusterConfig[req.Workspace],
				Category:   addonExtension.Category,
				CreatedAt:  time.Now(),
				UpdatedAt:  time.Now(),
				KmsKey:     kmsKey.KeyMetadata.KeyID,
				Deleted:    apistructs.AddonNotDeleted,
			}
			if err := a.db.CreateAddonInstance(instance); err != nil {
				return nil, err
			}

			routingIntance := &dbclient.AddonInstanceRouting{
				ID:           a.getRandomId(),
				RealInstance: instanceID,
				Name:         req.Name,
				AddonName:    req.AddonName,
				Plan:         apistructs.AddonBasic,
				Version:      "1.0.0",
				OrgID:        strconv.FormatUint(project.OrgID, 10),
				ProjectID:    strconv.FormatUint(project.ID, 10),
				ShareScope:   apistructs.ProjectShareScope,
				Workspace:    req.Workspace,
				Status:       string(apistructs.AddonAttached),
				Cluster:      project.ClusterConfig[req.Workspace],
				Category:     addonExtension.Category,
				Tag:          req.Tag,
				CreatedAt:    time.Now(),
				UpdatedAt:    time.Now(),
				InsideAddon:  apistructs.NOT_INSIDE,
				Deleted:      apistructs.AddonNotDeleted,
			}
			if err := a.db.CreateAddonInstanceRouting(routingIntance); err != nil {
				return nil, err
			}
			// 增加一步请求，判断是否log-exporter，是的化，请求tmc
			if req.AddonName == apistructs.AddonLogExporter {
				params := apistructs.AddonHandlerCreateItem{
					AddonName:  req.AddonName,
					Plan:       apistructs.AddonBasic,
					Options:    logExporterMap,
					OperatorID: req.OperatorID,
				}
				addonSpec, _, err := a.GetAddonExtention(&params)
				if err != nil {
					return nil, err
				}
				if err := a.providerAddonDeploy(instance, routingIntance, &params, addonSpec); err != nil {
					return nil, err
				}
			}
			return &map[string]string{"instanceId": routingIntance.RealInstance, "routingInstanceId": routingIntance.ID}, nil
		}

	}
	return nil, nil
}

// transCustomName2CloudName 转换为可以发布cloud addon的名称
func transCustomName2CloudName(addonName string) (string, string) {
	switch addonName {
	case apistructs.AddonCloudRedis:
		return "cloud-redis", ""
	case apistructs.AddonCloudRds:
		return "cloud-mysql", "db"
	case apistructs.AddonCloudOns:
		return "cloud-ons", "topic"
	case apistructs.AddonCloudOss:
		return "cloud-oss", ""
	case apistructs.AddonCloudGateway:
		return "cloud-gateway", "vpc-grant"
	}
	return "", ""
}

// GetRuntimeAddonConfig 根据runtimeID获取addon环境变量
func (a *Addon) GetRuntimeAddonConfig(runtimeID uint64) (*[]apistructs.AddonConfigRes, error) {
	// 获取所有attachment信息
	attchAddons, err := a.db.GetAttachMentsByRuntimeID(runtimeID)
	if err != nil {
		return nil, err
	}
	var configResList = make([]apistructs.AddonConfigRes, 0, len(*attchAddons)+1)
	// 获取zk addon信息
	hasZookeeper := false
	var routingIDs []string
	for _, att := range *attchAddons {
		routingIDs = append(routingIDs, att.RoutingInstanceID)
	}
	routings, err := a.db.GetInstanceRoutingsByIDs(routingIDs)
	if err != nil {
		return nil, err
	}
	routingMap := make(map[string]*dbclient.AddonInstanceRouting)
	for _, r := range *routings {
		routingMap[r.ID] = &r
	}
	for _, attchItem := range *attchAddons {
		routingIns := routingMap[attchItem.RoutingInstanceID]
		if routingIns == nil || routingIns.ID == "" {
			continue
		}
		// 内部addon跳过
		if attchItem.InsideAddon == apistructs.INSIDE {
			continue
		}
		// 根据是否配置zookeeper，判断roost是否释放出来
		if routingIns.AddonName == apistructs.AddonRoost && hasZookeeper {
			continue
		}
		// 排除状态不是attached的信息
		if routingIns == nil || routingIns.ID == "" || routingIns.Status != string(apistructs.AddonAttached) {
			continue
		}
		// 获取addon config信息
		configResult, err := a.getAddonConfig(attchItem.InstanceID, &attchItem)
		if err != nil {
			return nil, err
		}
		if configResult != nil {
			configResList = append(configResList, *configResult)
		}
		if routingIns.AddonName == apistructs.AddonMicroService {
			// 获取内部依赖addon信息
			insideRelations, err := a.db.GetByOutSideInstanceID(attchItem.InstanceID)
			if err != nil {
				return nil, err
			}
			if insideRelations != nil {
				for _, item := range *insideRelations {
					// 获取addon config信息
					configResultItem, err := a.getAddonConfig(item.InsideInstanceID, nil)
					if err != nil {
						return nil, err
					}
					if configResultItem != nil {
						configResList = append(configResList, *configResultItem)
					}
				}
			}
		}

	}
	return &configResList, nil
}

func (a *Addon) GetAddonTenantConfig(ins *dbclient.AddonInstanceTenant) (map[string]interface{}, error) {
	if ins != nil && ins.Config != "" {
		var configMap map[string]interface{}
		if err := json.Unmarshal([]byte(ins.Config), &configMap); err != nil {
			return nil, err
		}
		if ins.KmsKey == "" {
			if _, ok := configMap["ADDON_HAS_ENCRIPY"]; ok {
				a.encrypt.DecryptAddonConfigMap(&configMap)
			}

		} else {
			for k, v := range configMap {
				if strings.Contains(strings.ToLower(k), "pass") || strings.Contains(strings.ToLower(k), "secret") {
					password := v.(string)
					decPwd, err := a.DecryptPassword(&ins.KmsKey, password)
					if err != nil {
						logrus.Errorf("mysql password decript err, %v", err)
						return nil, err
					}
					configMap[k] = decPwd
				}
			}
		}
		logrus.Infof("decrypt config map info: %v", configMap)
		return configMap, nil

	}
	return nil, nil
}

func (a *Addon) GetAddonConfig(ins *dbclient.AddonInstance) (*apistructs.AddonConfigRes, error) {
	if ins != nil && ins.Config != "" {
		var configMap map[string]interface{}
		if err := json.Unmarshal([]byte(ins.Config), &configMap); err != nil {
			return nil, err
		}
		// password解密
		if ins.KmsKey == "" {
			if _, ok := configMap["ADDON_HAS_ENCRIPY"]; ok {
				a.encrypt.DecryptAddonConfigMap(&configMap)
			}
		} else {
			for k, v := range configMap {
				if strings.Contains(strings.ToLower(k), "pass") || strings.Contains(strings.ToLower(k), "secret") {
					password := v.(string)
					decPwd, err := a.DecryptPassword(&ins.KmsKey, password)
					if err != nil {
						logrus.Errorf("mysql password decript err, %v", err)
						return nil, err
					}
					configMap[k] = decPwd
				}
			}
		}
		logrus.Infof("decrypt config map info: %v", configMap)
		result := apistructs.AddonConfigRes{
			Name:   ins.Name,
			Engine: ins.AddonName,
			Config: configMap,
		}
		if ins.Label != "" {
			labelMap := make(map[string]string)
			logrus.Infof("ins label info: %s", ins.Label)
			if err := json.Unmarshal([]byte(ins.Label), &labelMap); err != nil {
				logrus.Errorf("json Unmarshal ins label map error: %v", err)
				return nil, err
			}
			result.Label = labelMap
		}
		return &result, nil
	}
	return nil, nil
}

func (a *Addon) getAddonConfig(instanceID string, attachment *dbclient.AddonAttachment) (*apistructs.AddonConfigRes, error) {
	ins, err := a.db.GetAddonInstance(instanceID)
	if err != nil {
		return nil, err
	}
	res, err := a.GetAddonConfig(ins)
	if err != nil {
		return nil, err
	}
	if ins.AddonName == apistructs.AddonMySQL && attachment != nil && attachment.MySQLAccountID != "" {
		// TODO: too hack
		err := a.toOverrideConfigFromMySQLAccount(res.Config, attachment.MySQLAccountID)
		if err != nil {
			return nil, err
		}
	}
	return res, nil
}

// UpdateCustom 更新自定义 addon
func (a *Addon) UpdateCustom(userID, addonID string, orgID uint64, configReq *map[string]interface{}) error {
	addonInfo, err := a.db.GetInstanceRouting(addonID)
	if err != nil {
		return err
	}
	if addonInfo == nil {
		return errors.Errorf("not found")
	}
	if addonInfo.Category != apistructs.AddonCustomCategory {
		return errors.Errorf("not custom addon")
	}
	projectID, err := strconv.ParseUint(addonInfo.ProjectID, 10, 64)
	if err != nil {
		return err
	}
	// 校验用户是否有权限创建自定义addon(企业管理员或项目管理员)
	manager, err := a.CheckCustomAddonPermission(userID, orgID, projectID)
	if err != nil {
		return err
	}
	if !manager {
		return errors.Errorf("no permission")
	}

	routingInstance, err := a.db.GetInstanceRouting(addonID)
	if err != nil {
		return err
	}
	instance, err := a.db.GetAddonInstance(routingInstance.RealInstance)
	if err != nil {
		return err
	}

	oldConfig, err := GetAddonConfig(instance.Config)
	if err != nil {
		return err
	}

	logExporterMap := map[string]string{}
	for k, v := range *configReq {
		// 环境变量interface转为string操作
		// ============= start ===============
		switch t := v.(type) {
		case string:
			logExporterMap[k] = t
		default:
			logExporterMap[k] = fmt.Sprintf("%v", t)
		}
		// ============= end ===============
		if strings.Contains(strings.ToLower(k), "pass") || strings.Contains(strings.ToLower(k), "secret") {
			oldV, ok := (oldConfig)[k]
			// the encrypted value has not changed
			if ok && IsEncryptedValueByValue(v.(string)) {
				(*configReq)[k] = oldV
				continue
			}

			encryptPassword := v.(string)
			if instance.KmsKey == "" {
				encryptPassword, err = a.encrypt.EncryptPassword(v.(string))
				if err != nil {
					return err
				}
			} else {
				encryptData, err := a.bdl.KMSEncrypt(apistructs.KMSEncryptRequest{
					EncryptRequest: kmstypes.EncryptRequest{
						KeyID:           instance.KmsKey,
						PlaintextBase64: base64.StdEncoding.EncodeToString([]byte(v.(string))),
					},
				})
				if err != nil {
					return err
				}
				encryptPassword = encryptData.CiphertextBase64
			}

			(*configReq)[k] = encryptPassword
			(*configReq)[apistructs.AddonPasswordHasEncripy] = "YES"
		}
	}

	config, err := json.Marshal(configReq)
	if err != nil {
		return err
	}

	instance.Config = string(config)
	if err := a.db.UpdateAddonInstance(instance); err != nil {
		return err
	}
	// 增加一步请求，判断是否log-exporter，是的化，请求tmc
	if addonInfo.AddonName == apistructs.AddonLogExporter {
		params := apistructs.AddonHandlerCreateItem{
			AddonName:  addonInfo.AddonName,
			Plan:       apistructs.AddonBasic,
			Options:    logExporterMap,
			OperatorID: userID,
		}
		addonSpec, _, err := a.GetAddonExtention(&params)
		if err != nil {
			return err
		}
		if err := a.providerAddonDeploy(instance, addonInfo, &params, addonSpec); err != nil {
			return err
		}
	}
	return nil
}

func (a *Addon) GetTenant(userID, orgID, id string, internalcall bool) (*apistructs.AddonFetchResponseData, error) {
	tenantInstance, err := a.db.GetAddonInstanceTenant(id)
	if err != nil {
		return nil, err
	}
	if tenantInstance == nil {
		return nil, errors.Errorf("tenantaddon: %s not found", id)
	}
	routingInstance, err := a.db.GetInstanceRouting(tenantInstance.AddonInstanceRoutingID)
	if err != nil {
		return nil, err
	}
	instance, err := a.db.GetAddonInstance(routingInstance.RealInstance)
	if err != nil {
		return nil, err
	}
	if instance == nil {
		return nil, errors.Errorf("addon real instance: %s not found", routingInstance.RealInstance)
	}
	addonResp := a.convertTenantInstance(tenantInstance, routingInstance)

	manager := false
	if !internalcall {
		orgIDLocal, err := strconv.ParseUint(routingInstance.OrgID, 10, 64)
		permissionResult, err := a.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   userID,
			Scope:    apistructs.OrgScope,
			ScopeID:  orgIDLocal,
			Resource: "addon_env",
			Action:   apistructs.GetAction,
		})
		if err != nil {
			return nil, err
		}
		if permissionResult.Access {
			manager = true
		}
	}
	if !internalcall && !manager && routingInstance.ProjectID != "" {
		projectID, err := strconv.ParseUint(routingInstance.ProjectID, 10, 64)
		permissionResult, err := a.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   userID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  projectID,
			Resource: "addon_env",
			Action:   apistructs.GetAction,
		})
		if err != nil {
			return nil, err
		}
		if permissionResult.Access {
			manager = true
		}
	}
	var config map[string]interface{}
	if err = json.Unmarshal([]byte(tenantInstance.Config), &config); err != nil {
		return nil, err
	}

	for k, v := range config {
		if strings.Contains(strings.ToLower(k), "pass") || strings.Contains(strings.ToLower(k), "secret") {
			if routingInstance.Category == apistructs.AddonCustomCategory || !manager {
				config[k] = ""
			}
			if internalcall || (manager && instance.Category != apistructs.AddonCustomCategory) {
				password := v.(string)
				if tenantInstance.KmsKey != "" {
					decPwd, err := a.DecryptPassword(&tenantInstance.KmsKey, password)
					if err != nil {
						logrus.Errorf("mysql password decript err, %v", err)
						return nil, err
					}
					config[k] = decPwd
				} else {
					if _, ok := config[apistructs.AddonPasswordHasEncripy]; ok {
						decPwd, err := a.DecryptPassword(nil, password)
						if err != nil {
							logrus.Errorf("mysql password decript err, %v", err)
							return nil, err
						}
						config[k] = decPwd
					}
				}
			}
		}
	}
	// 非管理员，隐匿 addon 值信息
	if manager || internalcall {
		if routingInstance.PlatformServiceType == 0 {
			addonResp.CanDel = true
		}
	} else {
		addonResp.CanDel = false
		for k := range config {
			config[k] = ""
		}
	}
	addonResp.Config = config
	return &addonResp, nil
}

// Get 根据 routingInstanceID 获取 addon 实例详情
func (a *Addon) Get(userID, orgID, routingInstanceID string, internalcall bool) (*apistructs.AddonFetchResponseData, error) {
	routingInstance, err := a.db.GetInstanceRouting(routingInstanceID)
	if err != nil {
		return nil, err
	}
	if routingInstance == nil {
		return a.GetTenant(userID, orgID, routingInstanceID, internalcall)
	}

	addonResp := a.convert(routingInstance)

	// 填充 config 信息
	instance, err := a.db.GetAddonInstance(routingInstance.RealInstance)
	if err != nil {
		return nil, err
	}
	if instance == nil {
		return nil, errors.Errorf("addon real instance: %s not found", routingInstance.RealInstance)
	}
	var config map[string]interface{}
	if instance.Config == "" {
		instance.Config = "{}"
	}
	if err = json.Unmarshal([]byte(instance.Config), &config); err != nil {
		return nil, err
	}

	manager := false
	if !internalcall {
		orgIDLocal, err := strconv.ParseUint(routingInstance.OrgID, 10, 64)
		permissionResult, err := a.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   userID,
			Scope:    apistructs.OrgScope,
			ScopeID:  orgIDLocal,
			Resource: "addon_env",
			Action:   apistructs.GetAction,
		})
		if err != nil {
			return nil, err
		}
		if permissionResult.Access {
			manager = true
		}
	}
	if !internalcall && !manager && routingInstance.ProjectID != "" {
		projectID, err := strconv.ParseUint(routingInstance.ProjectID, 10, 64)
		permissionResult, err := a.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   userID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  projectID,
			Resource: "addon_env",
			Action:   apistructs.GetAction,
		})
		if err != nil {
			return nil, err
		}
		if permissionResult.Access {
			manager = true
		}
	}

	// config中的password需要过滤
	for k, v := range config {
		if IsEncryptedValueByKey(k) {
			if routingInstance.Category == apistructs.AddonCustomCategory || !manager {
				config[k] = "***" + ErdaEncryptedValue + "***"
			}
			if internalcall || (manager && instance.Category != apistructs.AddonCustomCategory) {
				password := v.(string)
				if instance.KmsKey != "" {
					decPwd, err := a.DecryptPassword(&instance.KmsKey, password)
					if err != nil {
						logrus.Errorf("mysql password decript err, %v", err)
						return nil, err
					}
					config[k] = decPwd
				} else {
					if _, ok := config[apistructs.AddonPasswordHasEncripy]; ok {
						decPwd, err := a.DecryptPassword(nil, password)
						if err != nil {
							logrus.Errorf("mysql password decript err, %v", err)
							return nil, err
						}
						config[k] = decPwd
					}
				}
			}
		}
	}

	// 非管理员，隐匿 addon 值信息
	if manager || internalcall {
		if routingInstance.PlatformServiceType == 0 {
			addonResp.CanDel = true
		}
	} else {
		addonResp.CanDel = false
		for k := range config {
			config[k] = ""
		}
	}
	addonResp.Config = config
	return &addonResp, nil
}

func (a *Addon) DeleteTenant(userID, addonTenantID string) error {
	tenant, err := a.db.GetAddonInstanceTenant(addonTenantID)
	if err != nil {
		return err
	}
	if tenant == nil {
		return nil
	}
	if tenant.ProjectID != "" {
		projectIDInt, err := strconv.Atoi(tenant.ProjectID)
		permissionResult, err := a.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   userID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  uint64(projectIDInt),
			Resource: "addon",
			Action:   apistructs.DeleteAction,
		})
		if err != nil {
			logrus.Errorf("check permission error when delete addon, %+v", err)
			return err
		}
		if !permissionResult.Access {
			return errors.New("access denied")
		}
	}

	tenant.Deleted = apistructs.AddonDeleted
	if err := a.db.UpdateAddonInstanceTenant(tenant); err != nil {
		return err
	}
	return nil
}

// Delete 根据 routingInstanceID 删除 addon
func (a *Addon) Delete(userID, routingInstanceID string) error {
	//鉴权
	routingIns, err := a.db.GetInstanceRouting(routingInstanceID)
	if err != nil {
		logrus.Errorf("get routing instance error when delete addon, %+v", err)
		return err
	}
	if routingIns == nil {
		return a.DeleteTenant(userID, routingInstanceID)
	}
	if routingIns.ProjectID != "" {
		projectIDInt, err := strconv.Atoi(routingIns.ProjectID)
		permissionResult, err := a.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   userID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  uint64(projectIDInt),
			Resource: "addon",
			Action:   apistructs.DeleteAction,
		})
		if err != nil {
			logrus.Errorf("check permission error when delete addon, %+v", err)
			return err
		}
		if !permissionResult.Access {
			return errors.New("access denied")
		}
	}
	// 若引用关系存在，不能删除
	count, err := a.db.GetAttachmentCountByRoutingInstanceID(routingInstanceID)
	if err != nil {
		logrus.Errorf("get routing attachments error when delete addon, %+v", err)
		return err
	}
	if count > 0 {
		return errors.New("addon is being referenced, can't delete")
	}

	routingInstance, err := a.db.GetInstanceRouting(routingInstanceID)
	if err != nil {
		logrus.Errorf("get routing instance error when delete addon, %+v", err)
		return err
	}
	if routingInstance == nil || routingInstance.ID == "" {
		return nil
	}
	addonInstance, err := a.db.GetAddonInstance(routingInstance.RealInstance)
	if err != nil {
		logrus.Errorf("get addon instance error when delete addon, %+v", err)
		return err
	}
	if addonInstance == nil || addonInstance.ID == "" {
		return nil
	}
	// 判断 instanceID 对应引用关系是否存在，若不存在，可物理删除 addon
	count, err = a.db.GetAttachmentCountByInstanceID(routingInstance.RealInstance)
	if err != nil {
		return err
	}
	// 调用 scheduler 物理删除 addon
	if count == 0 {
		// addon 分为两大类: 1. 基础 addon  2. 业务 addon
		// 基础 addon 有两个子类: 1. custom addon 2. 其他基础 addon(kafka 是特例，基础 addon 嵌套 另一个基础 addon: zookeeper)
		// custom addon: 仅存储环境变量，真实实例在公有云，非dice管理，无须删除
		// 基础 addon: 一般 addon, 通过 scheduler 发起，删除时调用 scheduler API 删除
		// 业务 addon: 微服务 addon, 删除时调用 tmc API 删除: <spec.domain>/<spec.addonName>/dice/resources/<addonID>

		addonSpec, err := a.getAddonExtension(routingInstance.AddonName, routingInstance.Version)
		if err != nil {
			logrus.Errorf("get extension err when delete addon, %+v", err)
			return err
		}
		if addonSpec.SubCategory != apistructs.BasicAddon { // 业务 addon
			// 调用 pandora/tmc API 删除
			optionsMap := map[string]string{}
			if routingInstance.Options != "" {
				if err := json.Unmarshal([]byte(routingInstance.Options), &optionsMap); err != nil {
					logrus.Errorf("unmarshal error when delete addon, %+v", err)
					return err
				}
			}
			addonProviderRequest := apistructs.AddonProviderRequest{
				UUID:        routingInstance.RealInstance,
				Plan:        routingInstance.Plan,
				ClusterName: routingInstance.Cluster,
				Options:     optionsMap,
			}
			_, err := a.DeleteAddonProvider(&addonProviderRequest, routingInstance.RealInstance, addonSpec.Name, addonSpec.Domain)
			if err != nil {
				logrus.Errorf("provider addon delete fail: %v", err)
			}
		} else { // 基础 addon
			if addonSpec.Category != apistructs.AddonCustomCategory {
				// 调用 scheduler API 删除
				relationAddons, err := a.db.GetByOutSideInstanceID(routingInstance.RealInstance)
				if err != nil {
					logrus.Errorf("failed to GetByOutSideInstanceID, %+v", err)
					return err
				}
				ctx := transport.WithHeader(context.Background(), metadata.New(map[string]string{httputil.InternalHeader: "cmp"}))
				resp, err := a.clusterSvc.GetCluster(ctx, &clusterpb.GetClusterRequest{IdOrName: routingInstance.Cluster})
				if err != nil {
					logrus.Errorf("get cluster info failed, cluster name: %s, error: %v", routingInstance.Cluster, err)
					//The addon can also be forcibly deleted, when the cluster is not exists
					if !errorresp.IsNotFound(err) {
						return err
					}
				}
				cInfo := resp.Data
				var force bool
				if cInfo != nil && cInfo.OpsConfig != nil && cInfo.OpsConfig.Status == apistructs.ClusterStatusOffline {
					force = true
				}

				if relationAddons != nil && len(*relationAddons) > 0 {
					for _, relationItem := range *relationAddons {
						count, err = a.db.GetAttachmentCountByInstanceID(relationItem.InsideInstanceID)
						if err != nil {
							return err
						}
						if count > 0 {
							continue
						}
						relationRoutings, err := a.db.GetInstanceRoutingByRealInstance(relationItem.InsideInstanceID)
						if err != nil {
							return err
						}
						relationIns, err := a.db.GetAddonInstance(relationItem.InsideInstanceID)
						if err != nil {
							return err
						}
						if relationIns == nil || relationIns.ID == "" {
							continue
						}

						req := apistructs.ServiceGroupDeleteRequest{Namespace: relationIns.Namespace,
							Name: relationIns.ScheduleName, Force: force}
						forceDelete := "false"
						if force {
							forceDelete = "true"
						}
						if err := a.serviceGroupImpl.Delete(req.Namespace, req.Name, forceDelete); err != nil {
							logrus.Errorf("delete service group failed, request: %v, error: %v", req, err)
							return err
						}

						// 删除 routingInstance
						for _, v := range *relationRoutings {
							v.Deleted = apistructs.AddonDeleted
							v.Status = string(apistructs.AddonDetached)
							if err = a.db.UpdateInstanceRouting(&v); err != nil {
								return err
							}
						}

						// 删除 addon instance
						relationIns.Deleted = apistructs.AddonDeleted
						relationIns.Status = string(apistructs.AddonDetached)
						if err = a.db.UpdateAddonInstance(relationIns); err != nil {
							return err
						}
					}

				}

				req := apistructs.ServiceGroupDeleteRequest{Namespace: addonInstance.Namespace,
					Name: addonInstance.ScheduleName, Force: force}
				forceDelete := "false"
				if force {
					forceDelete = "true"
				}
				if err := a.serviceGroupImpl.Delete(req.Namespace, req.Name, forceDelete); err != nil {
					logrus.Errorf("delete service group failed, request: %+v, error: %+v", req, err)
					return err
				}

			} else {
				// 调用 pandora/tmc API 删除
				if routingInstance.AddonName == apistructs.AddonLogExporter {
					optionsMap := map[string]string{}
					if routingInstance.Options != "" {
						if err := json.Unmarshal([]byte(routingInstance.Options), optionsMap); err != nil {
							logrus.Errorf("delete exporter fail, err: %+v", err)
							return err
						}
					}
					addonProviderRequest := apistructs.AddonProviderRequest{
						UUID:        routingInstance.RealInstance,
						Plan:        routingInstance.Plan,
						ClusterName: routingInstance.Cluster,
						Options:     optionsMap,
					}
					_, err := a.DeleteAddonProvider(&addonProviderRequest, routingInstance.RealInstance, addonSpec.Name, addonSpec.Domain)
					if err != nil {
						logrus.Errorf("provider addon delete fail: %v", err)
					}
				}
				// 云服务删除
				if routingInstance.Options != "" {
					options := make(map[string]interface{})
					err := json.Unmarshal([]byte(routingInstance.Options), &options)
					if err != nil {
						logrus.Errorf("fail to json unmarshal instance: %s options: %s", routingInstance.ID, routingInstance.Options)
					}
					recordID := 0
					if v, ok := options["recordId"]; ok {
						recordID = int(v.(float64))
					}
					customAddonType := "custom"
					if v, ok := options["customAddonType"]; ok {
						customAddonType = v.(string)
					}
					if customAddonType == apistructs.CUSTOM_TYPE_CLOUD {
						CloudAddonDelBody := map[string]interface{}{
							"source":    "addon",
							"recordID":  strconv.Itoa(recordID),
							"projectID": routingInstance.ProjectID,
							"addonID":   routingInstance.RealInstance,
						}
						pathValue, resourceName := transCustomName2CloudName(routingInstance.AddonName)
						if resourceName == "" {
							if err := a.bdl.DeleteCloudAddon(routingInstance.OrgID, userID, pathValue, &CloudAddonDelBody); err != nil {
								return err
							}
						} else {
							if err := a.bdl.DeleteCloudAddonResource(routingInstance.OrgID, userID, pathValue, resourceName, &CloudAddonDelBody); err != nil {
								return err
							}
						}
					}
				}
			}
		}
	}
	// 删除 routingInstance
	routingInstance.Deleted = apistructs.AddonDeleted
	routingInstance.Status = string(apistructs.AddonDetached)
	if err = a.db.UpdateInstanceRouting(routingInstance); err != nil {
		return err
	}

	if count == 0 {
		// 删除 addon instance
		addonInstance.Deleted = apistructs.AddonDeleted
		addonInstance.Status = string(apistructs.AddonDetached)
		if err = a.db.UpdateAddonInstance(addonInstance); err != nil {
			return err
		}
	}

	return nil
}

// Scale 根据 routingInstanceID 调整 addon 实例副本数实现:
// 停止: 副本数设置为 0
// 启动: 副本数调整为期望值
// 平台级 addon 只支持扩，不支持缩
func (a *Addon) Scale(userID, routingInstanceID, action string) error {
	//鉴权
	errMsg := ""
	routingIns, err := a.db.GetInstanceRouting(routingInstanceID)
	if err != nil {
		logrus.Errorf("scale addon failed: get routing instance error, %+v", err)
		return err
	}

	if routingIns == nil || routingIns.ID == "" {
		errMsg = fmt.Sprintf("scale addon failed: not found addon with routing id %s", routingInstanceID)
		logrus.Errorf(errMsg)
		return errors.New(errMsg)
	}

	// 不允许对 inside addon 执行 scale 操作，需要对上层 outside addon 进行 scale 操作以触发 inside addon 的 scale 操作
	// 例如不能对 kafka addon 中的 zookeeper addon 执行 scale 操作，只能对 kafka addon 执行 scale 操作
	if routingIns.InsideAddon == apistructs.INSIDE {
		return a.insideAddonCanNotScale(routingIns)
	}

	projectIDInt, _ := strconv.Atoi(routingIns.ProjectID)
	pcr := &apistructs.PermissionCheckRequest{
		UserID:   userID,
		Scope:    apistructs.ProjectScope,
		ScopeID:  uint64(projectIDInt),
		Resource: "addon",
		Action:   apistructs.UpdateAction,
	}
	if routingIns.ProjectID != "" {
		//projectIDInt, err := strconv.Atoi(routingIns.ProjectID)
		permissionResult, err := a.bdl.CheckPermission(pcr)
		if err != nil {
			logrus.Errorf("scale addon failed: check permission error, %+v", err)
			return err
		}
		if !permissionResult.Access {
			return errors.New("scale addon failed: access denied")
		}
	}

	// 根据 addon 状态，判断当前是否支持对应 action 的操作
	err = addonCanScale(routingIns.AddonName, routingIns.RealInstance, routingIns.Plan, routingIns.Version, routingIns.Status, action)
	if err != nil {
		logrus.Errorf("%v", err)
		return err
	}

	// 若引用关系存在，不能停止
	count, err := a.db.GetAttachmentCountByRoutingInstanceID(routingInstanceID)
	if err != nil {
		logrus.Errorf("scale addon failed: get routing attachments error, %+v", err)
		return err
	}
	if count > 0 && action == apistructs.ScaleActionDown {
		return errors.New("scale addon failed: addon is being referenced, can't scale to 0")
	}

	addonInstance, err := a.db.GetAddonInstance(routingIns.RealInstance)
	if err != nil {
		logrus.Errorf("scale addon failed: get addon instance error, %+v", err)
		return err
	}
	if addonInstance == nil || addonInstance.ID == "" {
		errMsg = fmt.Sprintf("scale addon failed: not found addon with id %s", routingIns.RealInstance)
		logrus.Errorf(errMsg)
		return errors.New(errMsg)
	}

	// 根据 addon 状态，判断当前是否支持对应 action 的操作
	err = addonCanScale(addonInstance.AddonName, addonInstance.ID, addonInstance.Plan, addonInstance.Version, addonInstance.Status, action)
	if err != nil {
		logrus.Errorf("scale inside addon %s wint id %s failed, error: %v", addonInstance.AddonName, addonInstance.ID, err)
		return err
	}

	addonSpec, err := a.getAddonExtension(routingIns.AddonName, routingIns.Version)
	if err != nil {
		logrus.Errorf("scale addon failed: get extension error, %+v", err)
		return err
	}
	if addonSpec.SubCategory != apistructs.BasicAddon { // 业务 addon
		// TODO: 业务 addon 是否需要支持启动/停止？
		errMsg = fmt.Sprintf("scale addon failed: can not support scale addon %s with subCategory %s", addonSpec.Name, addonSpec.SubCategory)
		logrus.Errorf(errMsg)
		return errors.New(errMsg)
	} else { // 基础 addon
		if addonSpec.Category != apistructs.AddonCustomCategory {
			// 调用 scheduler API 进行 addon scale 操作
			relationAddons, err := a.db.GetByOutSideInstanceID(routingIns.RealInstance)
			if err != nil {
				logrus.Errorf("scale addon failed: get addon addonInstanceRelations info for instance %s with id %s , error: %v", routingIns.AddonName, routingIns.RealInstance, err)
				return err
			}

			// 1. addon 对应有 inside addon，则先对 inside addon 进行 scale 操作
			if relationAddons != nil && len(*relationAddons) > 0 {
				for _, relationItem := range *relationAddons {
					count, err = a.db.GetAttachmentCountByInstanceID(relationItem.InsideInstanceID)
					if err != nil {
						return err
					}
					// 如果引用计数 > 0,则不能停止 addon
					if count > 0 && action == apistructs.ScaleActionDown {
						continue
					}

					// 查看 inside addon 对应的 routing 表中对应的状态。确保状态满足执行 scale
					routingInstances, err := a.db.GetInstanceRoutingByRealInstance(relationItem.InsideInstanceID)
					if err != nil {
						logrus.Errorf("scale addon failed: get routing instance error, %+v", err)
						return err
					}

					if routingInstances == nil || len(*routingInstances) == 0 {
						errMsg = fmt.Sprintf("scale addon failed: not found addon routings with real_instance %s", relationItem.InsideInstanceID)
						logrus.Errorf(errMsg)
						return errors.New(errMsg)
					}
					for _, rInstance := range *routingInstances {
						err = addonCanScale(rInstance.AddonName, rInstance.RealInstance, rInstance.Plan, rInstance.Version, rInstance.Status, action)
						if err != nil {
							logrus.Errorf("%v", err)
							return err
						}
					}

					relationIns, err := a.db.GetAddonInstance(relationItem.InsideInstanceID)
					if err != nil {
						return err
					}
					if relationIns == nil || relationIns.ID == "" {
						continue
					}

					err = addonCanScale(relationIns.AddonName, relationIns.ID, relationIns.Plan, relationIns.Version, relationIns.Status, action)
					if err != nil {
						logrus.Errorf("scale inside addon %s wint id %s failed, error: %v", relationIns.AddonName, relationIns.ID, err)
						return err
					}

					err = a.doAddonScale(relationIns, routingInstances, action)
					if err != nil {
						return err
					}
				}
			}

			// 2. addon 无 inside addon，直接对 addon 进行 scale 操作
			routingInstances := make([]dbclient.AddonInstanceRouting, 0)
			routingInstances = append(routingInstances, *routingIns)
			return a.doAddonScale(addonInstance, &routingInstances, action)
		} else {
			// custom addon, 不支持启动/停止
			errMsg = fmt.Sprintf("scale addon failed: can not support scale addon %s with subCategory %s", addonSpec.Name, addonSpec.SubCategory)
			logrus.Errorf(errMsg)
			return errors.New(errMsg)
		}
	}
}

func (a *Addon) insideAddonCanNotScale(routingIns *dbclient.AddonInstanceRouting) error {
	errMsg := ""
	addonRelation, err := a.db.GetByInSideInstanceID(routingIns.RealInstance)
	if err != nil {
		errMsg = fmt.Sprintf("scale addon failed: addon %s with id %s is a inside addon which can not do scale. can not found the outside addon for it, error: %v", routingIns.AddonName, routingIns.RealInstance, err)
		logrus.Errorf(errMsg)
		return errors.New(errMsg)
	}

	if addonRelation == nil || addonRelation.OutsideInstanceID == "" {
		errMsg = fmt.Sprintf("scale addon failed: addon %s with id %s is a inside addon which can not do scale. can not found the outside addon for it, error: %v", routingIns.AddonName, routingIns.RealInstance, err)
		logrus.Errorf(errMsg)
		return errors.New(errMsg)
	}

	relationAddonRoutings, err := a.db.GetInstanceRoutingByRealInstance(addonRelation.OutsideInstanceID)
	if err != nil {
		errMsg = fmt.Sprintf("scale addon failed: addon %s with id %s is a inside addon which can not do scale. can not found outside addon %s routings info. error: %v", routingIns.AddonName, routingIns.RealInstance, addonRelation.OutsideInstanceID, err)
		logrus.Errorf(errMsg)
		return errors.New(errMsg)
	}

	if relationAddonRoutings == nil || len(*relationAddonRoutings) == 0 {
		errMsg = fmt.Sprintf("scale addon failed: addon %s with id %s is a inside addon which can not do scale. no routings info for outside addon %s", routingIns.AddonName, routingIns.RealInstance, addonRelation.OutsideInstanceID)
		logrus.Errorf(errMsg)
		return errors.New(errMsg)
	}

	type addonAndRotingIds struct {
		Name string
		Id   string
	}
	addonAndIds := make([]addonAndRotingIds, 0)
	for _, rr := range *relationAddonRoutings {
		addonAndIds = append(addonAndIds, addonAndRotingIds{
			Name: rr.Name,
			Id:   rr.ID,
		})
	}

	errMsg = fmt.Sprintf("scale addon failed: addon %s with id %s is a inside addon which can not do scale. please scale outside addon use routing ID in %+v instead.", routingIns.AddonName, routingIns.RealInstance, addonAndIds)
	logrus.Errorf(errMsg)
	return errors.New(errMsg)
}

func (a *Addon) doAddonScale(addonInstance *dbclient.AddonInstance, addonInstanceRoutings *[]dbclient.AddonInstanceRouting, action string) error {
	var optionMap map[string]string
	json.Unmarshal([]byte(addonInstance.Options), &optionMap)
	createItem := &apistructs.AddonHandlerCreateItem{
		InstanceName: addonInstance.Name,
		AddonName:    addonInstance.AddonName,
		//TODO: Plan
		Plan:          addonInstance.Plan,
		ClusterName:   addonInstance.Cluster,
		Workspace:     addonInstance.Workspace,
		OrgID:         addonInstance.OrgID,
		ProjectID:     addonInstance.ProjectID,
		ApplicationID: addonInstance.ApplicationID,
	}
	var optionsMap map[string]string
	if err := json.Unmarshal([]byte(addonInstance.Options), &optionsMap); err != nil {
		return errors.Wrapf(err, "Unmarshal instance optiosn for addon %s with id %s failed, error: %v", addonInstance.Name, addonInstance.ID, err)
	}

	createItem.Options = optionsMap

	if runtimeName, ok := optionMap["runtimeName"]; ok {
		createItem.RuntimeName = runtimeName
	}

	if runtimeID, ok := optionMap["runtimeId"]; ok {
		createItem.RuntimeID = runtimeID
	}

	req, err := a.createAddonScaleRequest(createItem, addonInstance, action)
	if err != nil {
		logrus.Errorf("scale addon failed: create addon scale request error: %v", err)
		return err
	}

	sgb, _ := json.Marshal(req)
	logrus.Infof("scale service group body is %s", string(sgb))
	if _, err := a.serviceGroupImpl.Scale(req); err != nil {
		logrus.Errorf("scale service group failed, request: %v, error: %v", req, err)
		return err
	}

	// 更新 inside addon status
	newStatus := ""
	switch action {
	case apistructs.ScaleActionDown:
		newStatus = string(apistructs.AddonOffline)
	case apistructs.ScaleActionUp:
		newStatus = string(apistructs.AddonAttached)
	}

	if err := a.db.UpdateAddonInstanceStatus(addonInstance.ID, newStatus); err != nil {
		return err
	}

	for _, rInstance := range *addonInstanceRoutings {
		rInstance.Status = newStatus
		if err := a.db.UpdateInstanceRouting(&rInstance); err != nil {
			return err
		}
	}
	return nil
}

func addonCanScale(addonName, addonId, plan, version, status, action string) error {
	if addonName == apistructs.AddonES && (action == apistructs.ScaleActionDown || action == apistructs.ScaleActionUp) && (version == "6.8.9" || version == "6.8.22") {
		errMsg := fmt.Sprintf("scale addon failed: addon %s is an operator which can not do %s", addonName, action)
		return errors.New(errMsg)
	}

	// 如果 addon status 不是 运行中(ATTACHED)，则不能执行 scaleDown 或变更 非0 副本数之类的 scale 操作
	if action == apistructs.ScaleActionDown && status != string(apistructs.AddonAttached) {
		errMsg := fmt.Sprintf("scale addon failed: addon %s with id %s is not in running state, can not do %s", addonName, addonId, apistructs.ScaleActionDown)
		return errors.New(errMsg)
	}

	// 如果 addon status 不是未启动(OFFLINE)状态，则不能执行 scaleUp操作
	if action == apistructs.ScaleActionUp && status != string(apistructs.AddonOffline) {
		errMsg := fmt.Sprintf("scale addon failed: addon %s with id %s is not in offline state, can not do %s", addonName, addonId, apistructs.ScaleActionUp)
		return errors.New(errMsg)
	}
	return nil
}

// createAddonScaleRequest 构建 addon scale 操作对应的请求
func (a *Addon) createAddonScaleRequest(params *apistructs.AddonHandlerCreateItem, addonIns *dbclient.AddonInstance, scaleAction string) (*apistructs.ServiceGroup, error) {
	// 获取addon extension信息
	addonSpec, addonDice, err := a.GetAddonExtention(params)
	if err != nil {
		logrus.Errorf("AttachAndCreate GetAddonExtention err:  %v", err)
		return nil, err
	}
	return a.BuildAddonScaleRequestGroup(params, addonIns, scaleAction, addonSpec, addonDice)
}

// getAddonExtension 获取extension信息
func (a *Addon) getAddonExtension(addonName, version string) (*apistructs.AddonExtension, error) {

	if _, ok := AddonInfos.Load(addonName); !ok {
		logrus.Errorf("[alert] can't find addon: %s,version: %s ", addonName, version)
		return nil, errors.Errorf("can't find addon: %s", addonName)
	}
	extensionVersionReq := apistructs.ExtensionVersionGetRequest{
		Name:    addonName,
		Version: version,
	}
	addonVersion, err := a.bdl.GetExtensionVersion(extensionVersionReq)
	if err != nil {
		return nil, err
	}

	// spec.yml强制转换为string类型
	addonSpecBytes, err := json.Marshal(addonVersion.Spec)
	if err != nil {
		return nil, errors.Wrap(err, "failed to parse addon spec")
	}
	addonSpec := apistructs.AddonExtension{}
	specErr := json.Unmarshal(addonSpecBytes, &addonSpec)
	if specErr != nil {
		return nil, errors.Wrap(specErr, "failed to parse addon spec")
	}
	return &addonSpec, nil
}

// ListByAddonName 根据 addonName 获取 指定企业下 addon 列表
func (a *Addon) ListByAddonName(orgID uint64, addonName string) (*[]apistructs.AddonFetchResponseData, error) {
	addons, err := a.db.GetRoutingInstancesByAddonName(orgID, addonName)
	if err != nil {
		return nil, err
	}
	addonRespList := make([]apistructs.AddonFetchResponseData, 0, len(*addons))
	for _, v := range *addons {
		addonRespList = append(addonRespList, a.convert(&v))
	}

	return &addonRespList, nil
}

// ListByCategory 根据 category 获取 指定企业下 addon 列表
func (a *Addon) ListByCategory(orgID uint64, category string) (*[]apistructs.AddonFetchResponseData, error) {
	addons, err := a.db.GetRoutingInstancesByCategory(orgID, category)
	if err != nil {
		return nil, err
	}
	addonRespList := make([]apistructs.AddonFetchResponseData, 0, len(*addons))
	for _, v := range *addons {
		addonRespList = append(addonRespList, a.convert(&v))
	}

	return &addonRespList, nil
}

// ListByWorkbench 获取给定企业下用户有权限的 addon 列表
func (a *Addon) ListByWorkbench(orgID uint64, userID, category string) (*[]apistructs.AddonFetchResponseData, error) {
	// 获取用户有权限的项目列表
	permissionResp, err := a.bdl.ListScopeRole(userID, strconv.FormatUint(orgID, 10))
	if err != nil {
		return nil, err
	}
	projectIDs := make([]string, 0, len(permissionResp.List))
	for _, v := range permissionResp.List {
		if v.Scope.Type == apistructs.ProjectScope && v.Access {
			projectIDs = append(projectIDs, v.Scope.ID)
		}
	}

	addons, err := a.db.GetRoutingInstancesByWorkbench(orgID, projectIDs, category)
	if err != nil {
		return nil, err
	}

	addonRespList := make([]apistructs.AddonFetchResponseData, 0, len(*addons))
	for _, v := range *addons {
		projectInfo, err := a.getProject(v.ProjectID)
		if err != nil {
			return nil, err
		}
		if projectInfo.Name == "" {
			continue
		}
		if az, ok := projectInfo.ClusterConfig[v.Workspace]; !ok || az != v.Cluster {
			continue
		}
		addonRespList = append(addonRespList, a.convert(&v))
	}

	microRoutingResponse, err := a.listMicroAttach(strconv.FormatUint(orgID, 10), projectIDs)
	if err != nil {
		return nil, err
	}
	if microRoutingResponse != nil && len(*microRoutingResponse) > 0 {
		for _, v := range *microRoutingResponse {
			addonRespList = append(addonRespList, v)
		}
	}

	return &addonRespList, nil
}

// ListByOrg 根据 orgID 获取 addon 列表
func (a *Addon) ListByOrg(orgID uint64) (*[]apistructs.AddonFetchResponseData, error) {
	addons, err := a.db.GetRoutingInstancesByOrg(orgID)
	if err != nil {
		return nil, err
	}
	addonRespList := make([]apistructs.AddonFetchResponseData, 0, len(*addons))
	for _, v := range *addons {
		addonRespList = append(addonRespList, a.convert(&v))
	}

	microRoutingResponse, err := a.listMicroAttach(strconv.FormatUint(orgID, 10), []string{})
	if err != nil {
		return nil, err
	}
	if microRoutingResponse != nil && len(*microRoutingResponse) > 0 {
		for _, v := range *microRoutingResponse {
			addonRespList = append(addonRespList, v)
		}
	}

	return &addonRespList, nil
}

// ListByProject 根据 projectID 获取 addon 列表
func (a *Addon) ListByProject(orgID, projectID uint64, category string) (*[]apistructs.AddonFetchResponseData, error) {
	addons, err := a.db.GetRoutingInstancesByProject(orgID, projectID, category)
	if err != nil {
		return nil, err
	}
	addonRespList := make([]apistructs.AddonFetchResponseData, 0, len(*addons))
	for _, v := range *addons {
		projectInfo, err := a.getProject(v.ProjectID)
		if err != nil {
			return nil, err
		}
		if projectInfo.Name == "" {
			continue
		}
		if az, ok := projectInfo.ClusterConfig[v.Workspace]; !ok || az != v.Cluster {
			continue
		}
		if v.Category != apistructs.AddonCustomCategory && v.Status != string(apistructs.AddonAttached) && v.Status != string(apistructs.AddonOffline) {
			continue
		}
		addonRespList = append(addonRespList, a.convert(&v))
	}
	microRoutingResponse, err := a.listMicroAttach(strconv.FormatUint(orgID, 10), []string{strconv.FormatUint(projectID, 10)})
	if err != nil {
		return nil, err
	}
	if microRoutingResponse != nil && len(*microRoutingResponse) > 0 {
		for _, v := range *microRoutingResponse {
			addonRespList = append(addonRespList, v)
		}
	}

	tenants, err := a.db.ListAddonInstanceTenantByProjectIDs([]uint64{projectID})
	if err != nil {
		return nil, err
	}
	for _, t := range tenants {
		routings, err := a.db.GetInstanceRoutingsByIDs([]string{t.AddonInstanceRoutingID})
		if err != nil {
			logrus.Errorf("failed to GetInstanceRoutingsByIDs: %s", t.AddonInstanceRoutingID)
			continue
		}
		if routings == nil || len(*routings) != 1 {
			continue
		}
		routing := (*routings)[0]
		addonRespList = append(addonRespList, a.convertTenantInstance(&t, &routing))
	}

	return &addonRespList, nil
}

// listMicroAttach 查询微服务与project的对应关系，目前只针对配置中心
func (a *Addon) listMicroAttach(orgID string, projectIDs []string) (*[]apistructs.AddonFetchResponseData, error) {
	// 查询microAttach表，查询配置中心对应关系
	microAttachs, err := a.db.GetMicroAttachesByAddonName(apistructs.AddonConfigCenter, orgID, projectIDs)
	if err != nil {
		return nil, err
	}
	if len(*microAttachs) == 0 {
		return nil, nil
	}
	// 获取配置中心routing信息
	configCenterRouting, err := a.db.GetInstanceRouting((*microAttachs)[0].RoutingInstanceID)
	if err != nil {
		return nil, err
	}
	if configCenterRouting == nil || configCenterRouting.ID == "" {
		return nil, nil
	}
	addonRespList := make([]apistructs.AddonFetchResponseData, 0, len(*microAttachs))
	for _, v := range *microAttachs {
		tmpRouting := *configCenterRouting
		tmpRouting.Workspace = v.Env
		tmpRouting.ProjectID = v.ProjectID
		tmpRouting.OrgID = v.OrgID
		tmpRouting.Reference = int(v.Count)
		addonRespList = append(addonRespList, a.convert(&tmpRouting))
	}
	return &addonRespList, nil
}

func (a *Addon) BuildAddonAndTenantMap(projectAddons []dbclient.AddonInstanceRouting, projectAddonTenants []dbclient.AddonInstanceTenant) (
	addonnameMap map[string][]dbclient.AddonInstanceRouting, addonIDMap map[string]dbclient.AddonInstanceRouting,
	addonTenantNameMap map[string][]dbclient.AddonInstanceTenant, addonTenantIDMap map[string]dbclient.AddonInstanceTenant) {

	addonnameMap = map[string][]dbclient.AddonInstanceRouting{}
	addonIDMap = map[string]dbclient.AddonInstanceRouting{}
	addonTenantNameMap = map[string][]dbclient.AddonInstanceTenant{}
	addonTenantIDMap = map[string]dbclient.AddonInstanceTenant{}
	for _, addon := range projectAddons {
		if v, ok := addonnameMap[addon.Name]; ok {
			addonnameMap[addon.Name] = append(v, addon)
		} else {
			addonnameMap[addon.Name] = []dbclient.AddonInstanceRouting{addon}
		}
		addonIDMap[addon.ID] = addon
	}
	for _, tenant := range projectAddonTenants {
		if v, ok := addonTenantNameMap[tenant.Name]; ok {
			addonTenantNameMap[tenant.Name] = append(v, tenant)
		} else {
			addonTenantNameMap[tenant.Name] = []dbclient.AddonInstanceTenant{tenant}
		}
		addonTenantIDMap[tenant.ID] = tenant
	}
	return
}

func (a *Addon) ListByDiceymlEnvs(diceyml_s string, projectid uint64, workspace string, clustername string) ([]apistructs.AddonFetchResponseData, error) {
	projectAddons, err := a.db.GetAliveProjectAddons(strconv.FormatUint(projectid, 10), clustername, workspace)
	if err != nil {
		return nil, err
	}
	projectAddonTenants, err := a.db.ListAddonInstanceTenantByProjectIDs([]uint64{projectid}, workspace)
	if err != nil {
		return nil, err
	}
	addonnameMap, addonIDMap, addonTenantNameMap, addonTenantIDMap := a.BuildAddonAndTenantMap(*projectAddons, projectAddonTenants)
	d, err := diceyml.New([]byte(diceyml_s), false)
	if err != nil {
		return nil, err
	}
	r := []apistructs.AddonFetchResponseData{}
	vars := []string{}
	for _, v := range d.Obj().Envs {
		v_ := strutil.Trim(v)
		if !strutil.HasPrefixes(v_, "${{") || !strutil.HasSuffixes(v_, "}}") {
			continue
		}
		exp, err := sexp.Parse(strutil.TrimSuffixes(strutil.TrimPrefixes(v_, "${{"), "}}"))
		if err != nil {
			logrus.Errorf("failed to parse env-sexp: %v", err)
			continue
		}
		vars = append(vars, sexp.ReferencedVars(exp)...)
	}
	for name, s := range d.Obj().Services {
		logrus.Infof("[DEBUG] service(%s) env: %+v", name, s.Envs)
		for _, v := range s.Envs {
			v_ := strutil.Trim(v)
			if !strutil.HasPrefixes(v_, "${{") || !strutil.HasSuffixes(v_, "}}") {
				continue
			}
			exp, err := sexp.Parse(strutil.TrimSuffixes(strutil.TrimPrefixes(v_, "${{"), "}}"))
			if err != nil {
				logrus.Errorf("failed to parse env-sexp: %v", err)
				continue
			}
			vars = append(vars, sexp.ReferencedVars(exp)...)
		}
	}
	for _, v := range vars {
		vs := strutil.Split(v, ".", true)
		if len(vs) != 3 {
			continue
		}
		if vs[0] != "addons" {
			continue
		}
		nameorid := vs[1]
		if routings, ok := addonnameMap[nameorid]; ok {
			if len(routings) == 1 {
				r = append(r, a.convert(&routings[0]))
			}
		} else if routing, ok := addonIDMap[nameorid]; ok {
			r = append(r, a.convert(&routing))
		} else if tenants, ok := addonTenantNameMap[nameorid]; ok {
			if len(tenants) == 1 {
				routing, err := a.db.GetInstanceRouting(tenants[0].AddonInstanceRoutingID)
				if err != nil {
					logrus.Errorf("failed to GetInstanceRouting: %v", err)
					continue
				}
				r = append(r, a.convertTenantInstance(&tenants[0], routing))
			}
		} else if tenant, ok := addonTenantIDMap[nameorid]; ok {
			routing, err := a.db.GetInstanceRouting(tenant.AddonInstanceRoutingID)
			if err != nil {
				logrus.Errorf("failed to GetInstanceRouting: %v", err)
				continue
			}
			r = append(r, a.convertTenantInstance(&tenant, routing))
		}
	}
	return r, nil
}

// ListByRuntime 根据 runtimeID 获取 addon 列表
func (a *Addon) ListByRuntime(runtimeID uint64, projectID, workspace string) (*[]apistructs.AddonFetchResponseData, error) {
	addonRespListFilter := []apistructs.AddonFetchResponseData{}
	addons, err := a.db.GetUnDeletableAttachMentsByRuntimeID(runtimeID)
	if err != nil {
		return nil, err
	}
	if len(*addons) == 0 {
		return &addonRespListFilter, nil
	}
	addonRespList := make([]apistructs.AddonFetchResponseData, 0, len(*addons))
	var roostZkMap = make(map[string]string, len(*addons))
	for _, att := range *addons {
		ins, err := a.db.GetAddonInstance(att.InstanceID)
		if err != nil {
			return nil, err
		}
		if ins == nil || ins.ID == "" {
			continue
		}
		if ins.AddonName == apistructs.AddonTerminusRoost {
			var optionMap map[string]string
			json.Unmarshal([]byte(ins.Options), &optionMap)
			if optionMap["roost_zk"] != "" {
				roostZkMap[optionMap["roost_zk"]] = ""
			}
		}
	}

	var instanceIdsMap = make(map[string]string)
	var insideInstanceIdsMap = make(map[string]string)
	exitMicro := false
	for _, att := range *addons {
		if att.InsideAddon == apistructs.INSIDE {
			insideInstanceIdsMap[att.RoutingInstanceID] = ""
			continue
		}
		routing, err := a.db.GetInstanceRouting(att.RoutingInstanceID)
		if err != nil {
			return nil, err
		}
		if routing == nil || routing.ID == "" {
			continue
		}
		instanceIdsMap[routing.ID] = routing.Status
		if routing.AddonName == apistructs.AddonConfigCenter {
			routing.Workspace = workspace
			routing.ProjectID = projectID
		}
		if routing.AddonName == apistructs.AddonMicroService {
			exitMicro = true
		}
		if _, ok := roostZkMap[routing.RealInstance]; ok {
			continue
		}
		addonRespList = append(addonRespList, a.convert(routing))
	}
	for _, att := range *addons {
		if att.TenantInstanceID == "" {
			continue
		}
		tenant, err := a.db.GetAddonInstanceTenant(att.TenantInstanceID)
		if err != nil {
			return nil, err
		}
		if tenant == nil || tenant.ID == "" {
			continue
		}
		routing, err := a.db.GetInstanceRouting(tenant.AddonInstanceRoutingID)
		if err != nil {
			return nil, err
		}
		if routing == nil || routing.ID == "" {
			continue
		}
		addonRespList = append(addonRespList, a.convertTenantInstance(tenant, routing))
	}

	// 针对发布失败的addon，进行runtime界面填充，让它也显示出来
	prebuilds, err := a.db.GetPreBuildsByRuntimeID(runtimeID)
	if err != nil {
		return nil, err
	}
	if len(*prebuilds) > 0 {
		for _, pre := range *prebuilds {
			if pre.DeleteStatus != apistructs.AddonPrebuildNotDeleted {
				continue
			}
			if pre.RoutingInstanceID == "" {
				continue
			}
			if _, ok := insideInstanceIdsMap[pre.RoutingInstanceID]; ok {
				routing, err := a.db.GetInstanceRouting(pre.RoutingInstanceID)
				if err != nil {
					return nil, err
				}
				if routing == nil || routing.ID == "" {
					continue
				}
				addonRespList = append(addonRespList, a.convert(routing))
			}
			if _, ok := instanceIdsMap[pre.RoutingInstanceID]; !ok {
				addonInsRout := dbclient.AddonInstanceRouting{
					ID:                  pre.RoutingInstanceID,
					Name:                pre.InstanceName,
					AddonName:           pre.AddonName,
					Plan:                pre.Plan,
					ProjectID:           projectID,
					Workspace:           workspace,
					Status:              string(apistructs.AddonAttachFail),
					RealInstance:        pre.InstanceID,
					Reference:           0,
					IsPlatform:          false,
					PlatformServiceType: 0,
					Deleted:             apistructs.AddonNotDeleted,
					InsideAddon:         apistructs.NOT_INSIDE,
					CreatedAt:           time.Now(),
					UpdatedAt:           time.Now(),
				}
				addonRespList = append(addonRespList, a.convert(&addonInsRout))
			}
		}
	}
	if len(addonRespList) > 0 {
		for _, ins := range addonRespList {
			if ins.PlatformServiceType == apistructs.PlatformServiceTypeMicro &&
				ins.AddonName != apistructs.AddonMicroService &&
				exitMicro {
				continue
			}
			addonRespListFilter = append(addonRespListFilter, ins)
		}
	}
	return &addonRespListFilter, nil
}

// ListAvailableByProject 根据 orgID & projectID 获取可用的 addon 实例列表
func (a *Addon) ListAvailable(orgID, projectID, workspace string) (*[]apistructs.AddonFetchResponseData, error) {
	projectInfo, err := a.getProject(projectID)
	if err != nil {
		return nil, err
	}
	if projectInfo.Name == "" {
		return nil, errors.New("找不到project信息。")
	}

	orgAddons, err := a.db.GetOrgRoutingInstances(orgID, workspace, projectInfo.ClusterConfig[workspace])
	if err != nil {
		return nil, err
	}

	projectAddons, err := a.db.GetProjectRoutingInstances(orgID, projectID, workspace, projectInfo.ClusterConfig[workspace])
	if err != nil {
		return nil, err
	}

	addonRespList := make([]apistructs.AddonFetchResponseData, 0, len(*orgAddons)+len(*projectAddons))
	for _, v := range *orgAddons {
		if v.PlatformServiceType != 0 {
			continue
		}
		fetchRes := a.convert(&v)
		fetchRes.AddonDisplayName = fetchRes.Name
		addonRespList = append(addonRespList, fetchRes)
	}
	for _, v := range *projectAddons {
		if v.Category == "discovery" || v.PlatformServiceType != 0 {
			continue
		}
		fetchRes := a.convert(&v)
		fetchRes.AddonDisplayName = fetchRes.Name
		addonRespList = append(addonRespList, fetchRes)
	}

	return &addonRespList, nil
}

// ListByAddonNameAndProject 根据addonName查询数据
func (a *Addon) ListByAddonNameAndProject(projectID, workspace, addonName string) ([]apistructs.AddonNameResultItem, error) {
	ins, err := a.db.ListAddonInstanceByAddonName(projectID, workspace, addonName)
	if err != nil {
		return nil, err
	}
	if ins == nil {
		return []apistructs.AddonNameResultItem{}, nil
	}
	result := make([]apistructs.AddonNameResultItem, 0, len(*ins))
	for _, item := range *ins {
		var config map[string]interface{}
		if item.Config != "" {
			if err := json.Unmarshal([]byte(item.Config), &config); err != nil {
				return nil, err
			}
		}
		result = append(result, apistructs.AddonNameResultItem{
			InstanceID: item.ID,
			Config:     config,
			Status:     item.Status,
		})
	}
	return result, nil
}

// ListExtension 包装api/extensions接口
func (a *Addon) ListExtension(extensionType string) (*[]map[string]interface{}, error) {
	// 不展示的addon列表
	extensionFilterMap := map[string]string{"addresscenter": "", "terminus-haproxy": "", "terminus-roost": "", "terminus-zkproxy": "",
		"objectstorage": "", "permissioncenter": "", "searchcenter": "", "usercenter": "",
		"alicloud-ons": "", "alicloud-rds": "", "alicloud-redis": "", "custom": "", "atlas": "", "rabbitmq": "",
		"microservice-set": "", "mysql-operator": "", "redis-operator": "", "mall-inventory": "", "mall-item": "",
		"mall-price": "", "mall-settle": "", "mall-trade": "", "escp": "", "srm-alhasa": "", "srm-bahariya": "", "srm-contract": "",
		"srm-material": "", "srm-partner": "", "aliyun-image": "", "aliyun-imagesearch": "", "aliyun-lvwang": "", "aliyun-nlp": "",
		"aliyun-ocr": "", "aliyun-trans": "", "aliyun-face": ""}

	// zookeeper属性判断，如果canDeploy，则开放terminus-zookeeper
	if !a.zkCanDeploy() {
		extensionFilterMap["terminus-zookeeper"] = ""
	}
	// 构建请求参数，请求extension
	req := apistructs.ExtensionQueryRequest{
		All:  true,
		Type: extensionType,
	}
	extensions, err := a.bdl.QueryExtensions(req)
	if err != nil {
		return nil, err
	}
	extensionResult := make([]map[string]interface{}, 0, len(extensions))
	for _, item := range extensions {
		if _, ok := extensionFilterMap[item.Name]; ok {
			continue
		}
		addonMap := a.StructToMap(item, 0, "json")
		addonMap["addonName"] = item.Name
		extensionResult = append(extensionResult, addonMap)
	}
	return &extensionResult, nil
}

// ListCustomAddon 包装api/extensions接口，返回第三方addon
func (a *Addon) ListCustomAddon() (*[]map[string]interface{}, error) {
	locale := a.bdl.GetLocale(i18n.GetGoroutineBindLang())
	basic := locale.Get("basicPlan")
	professional := locale.Get("professionalPlan")

	createableAddons := []string{"api-gateway", "mysql", "canal", "monitor"}
	createableAddonVersion := map[string]string{"api-gateway": "3.0.0", "mysql": "5.7.29", "canal": "1.1.0", "monitor": "3.6"}
	createableAddonPlan := map[string][]map[string]string{
		"api-gateway": {{"label": basic, "value": "api-gateway:basic"}},
		"mysql":       {{"label": basic, "value": "mysql:basic"}},
		"canal":       {{"label": basic, "value": "canal:basic"}},
		"monitor":     {{"label": professional, "value": "monitor:professional"}},
	}

	// 构建请求参数，请求extension
	req := apistructs.ExtensionQueryRequest{
		All:  true,
		Type: "addon",
	}
	extensions, err := a.bdl.QueryExtensions(req)
	if err != nil {
		return nil, err
	}
	extensionResult := make([]map[string]interface{}, 0, len(extensions))
	for _, item := range extensions {
		if item.Category != apistructs.AddonCustomCategory && !strutil.Exist(createableAddons, item.Name) {
			continue
		}
		version := createableAddonVersion[item.Name]
		if version == "" {
			version = "1.0.0"
		}
		itemSpecResult, err := a.bdl.GetExtensionVersion(apistructs.ExtensionVersionGetRequest{
			Name:    item.Name,
			Version: version,
		})
		if err != nil {
			logrus.Errorf("custom request fail, addon name: %v", item.Name)
			continue
		}
		// spec.yml强制转换为string类型
		addonSpecBytes, err := json.Marshal(itemSpecResult.Spec)
		if err != nil {
			logrus.Error("failed to parse addon spec")
			continue
		}
		// spec转换为strcut
		addonSpec := apistructs.AddonExtension{}
		specErr := json.Unmarshal(addonSpecBytes, &addonSpec)
		if specErr != nil {
			logrus.Error("failed to parse addon spec")
			continue
		}
		// vars填充入map中
		addonMap := a.StructToMap(item, 0, "json")
		addonMap["addonName"] = item.Name
		addonMap["vars"] = addonSpec.ConfigVars
		switch item.Name {
		case "mysql", "api-gateway", "monitor":
			addonMap["vars"] = nil
		case "canal":
			addonMap["vars"] = []string{
				"canal.instance.master.address",
				"canal.instance.dbUsername",
				"canal.instance.dbPassword",
				"mysql"}
		case "custom":
			addonMap["vars"] = []string{}
		}
		addonMap["version"] = version
		addonMap["plan"] = createableAddonPlan[item.Name]
		// TODO: disable tenant support, we shall refactor later
		//switch item.Name {
		//case "mysql":
		//	addonMap["supportTenant"] = true
		//	addonMap["tenantVars"] = []string{"database"}
		//default:
		//	addonMap["supportTenant"] = false
		//}
		extensionResult = append(extensionResult, addonMap)
	}
	return &extensionResult, nil
}

// ListReferencesByInstanceID 根据 instanceID 获取引用列表
func (a *Addon) ListReferencesByInstanceID(orgID uint64, userID, instanceID string) (*[]apistructs.AddonReferenceInfo, error) {
	attachments, err := a.db.GetAttachmentsByInstanceID(instanceID)
	if err != nil {
		return nil, err
	}
	referenceInfos := make([]apistructs.AddonReferenceInfo, 0, len(*attachments))
	// 批量查询 projectName & appName 等信息
	for _, v := range *attachments {
		app := apistructs.ApplicationDTO{}
		if application, ok := AppInfos.Load(v.ApplicationID); !ok {
			// 若找不到应用信息，根据 projectID 实时获取并缓存
			projectID, _ := strconv.ParseUint(v.ProjectID, 10, 64)
			appResp, err := a.bdl.GetAppsByProject(projectID, orgID, userID)
			if err != nil {
				return nil, err
			}
			for _, item := range appResp.List {
				AppInfos.Store(strconv.FormatUint(item.ID, 10), item)
			}
			if application, ok = AppInfos.Load(v.ApplicationID); ok {
				app = application.(apistructs.ApplicationDTO)
			}
		} else {
			app = application.(apistructs.ApplicationDTO)
		}
		runtimeID, _ := strconv.ParseUint(v.RuntimeID, 10, 64)
		reference := apistructs.AddonReferenceInfo{
			OrgID:       app.OrgID,
			ProjectID:   app.ProjectID,
			ProjectName: app.ProjectName,
			AppID:       app.ID,
			AppName:     app.Name,
			RuntimeID:   runtimeID,
			RuntimeName: v.RuntimeName,
		}
		referenceInfos = append(referenceInfos, reference)
	}

	return &referenceInfos, nil
}

// ListReferencesByRoutingInstanceID 根据 routingInstanceID 获取引用列表
func (a *Addon) ListReferencesByRoutingInstanceID(orgID uint64, userID, routingInstanceID string, internal ...bool) (*[]apistructs.AddonReferenceInfo, error) {
	//鉴权
	routingIns, err := a.db.GetInstanceRouting(routingInstanceID)
	if err != nil {
		return nil, err
	}
	if routingIns == nil {
		tenant, err := a.db.GetAddonInstanceTenant(routingInstanceID)
		if err != nil {
			return nil, err
		}
		if tenant == nil {
			return nil, nil
		}
		routingIns = &dbclient.AddonInstanceRouting{
			OrgID:     tenant.OrgID,
			ProjectID: tenant.ProjectID,
			Name:      tenant.Name,
			ID:        tenant.ID,
		}
	}
	// 先检查是否企业管理元
	permissionOK := false
	if len(internal) > 0 && internal[0] == true {
		permissionOK = true
	} else {

		if routingIns.OrgID != "" {
			orgIDInt, err := strconv.Atoi(routingIns.OrgID)
			permissionResult, err := a.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
				UserID:   userID,
				Scope:    apistructs.OrgScope,
				ScopeID:  uint64(orgIDInt),
				Resource: "addon",
				Action:   apistructs.GetAction,
			})
			if err != nil {
				return nil, err
			}
			if permissionResult.Access {
				permissionOK = true
			}
		}
		// 再检查是否项目管理元
		if !permissionOK && routingIns.ProjectID != "" {
			projectIDInt, err := strconv.Atoi(routingIns.ProjectID)
			permissionResult, err := a.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
				UserID:   userID,
				Scope:    apistructs.ProjectScope,
				ScopeID:  uint64(projectIDInt),
				Resource: "addon",
				Action:   apistructs.GetAction,
			})
			if err != nil {
				return nil, err
			}
			if !permissionResult.Access {
				return nil, errors.New("权限不足")
			}
		}
	}
	// ============ attachment 中分析 reference ================
	addon_attachments, err := a.db.GetAttachmentsByRoutingInstanceID(routingInstanceID)
	if err != nil {
		return nil, err
	}
	tenant_attachments, err := a.db.GetAttachmentsByTenantInstanceID(routingInstanceID)
	if err != nil {
		return nil, err
	}
	attachments := []dbclient.AddonAttachment{}
	if addon_attachments != nil {
		attachments = append(attachments, (*addon_attachments)...)
	}
	if tenant_attachments != nil {
		attachments = append(attachments, (*tenant_attachments)...)
	}

	referenceInfos := make([]apistructs.AddonReferenceInfo, 0, len(attachments))
	// 批量查询 projectName & appName 等信息
	for _, v := range attachments {
		app := apistructs.ApplicationDTO{}

		if application, ok := AppInfos.Load(v.ApplicationID); !ok {
			// 若找不到应用信息，根据 projectID 实时获取并缓存
			projectID, _ := strconv.ParseUint(v.ProjectID, 10, 64)
			appResp, err := a.bdl.GetAppsByProject(projectID, orgID, userID)
			if err != nil {
				return nil, err
			}
			for _, item := range appResp.List {
				AppInfos.Store(strconv.FormatUint(item.ID, 10), item)
			}
			if application, ok := AppInfos.Load(v.ApplicationID); ok {
				app = application.(apistructs.ApplicationDTO)
			}
		} else {
			app = application.(apistructs.ApplicationDTO)
		}
		if app.ID == 0 {
			applictionId, err := strconv.ParseUint(v.ApplicationID, 10, 64)
			if err != nil {
				return nil, err
			}
			projectId, err := strconv.ParseUint(v.ProjectID, 10, 64)
			if err != nil {
				return nil, err
			}
			orgId, err := strconv.ParseUint(v.ApplicationID, 10, 64)
			if err != nil {
				return nil, err
			}
			app.ID = applictionId
			app.Name = v.ApplicationID
			app.ProjectID = projectId
			app.ProjectName = v.ProjectID
			app.OrgID = orgId
		}
		runtimeID, _ := strconv.ParseUint(v.RuntimeID, 10, 64)
		reference := apistructs.AddonReferenceInfo{
			OrgID:       app.OrgID,
			ProjectID:   app.ProjectID,
			ProjectName: app.ProjectName,
			AppID:       app.ID,
			AppName:     app.Name,
			RuntimeID:   runtimeID,
			RuntimeName: v.RuntimeName,
		}
		referenceInfos = append(referenceInfos, reference)
	}
	return &referenceInfos, nil
}

// deleteBusinessAddon 删除业务 addon
func (a *Addon) deleteBusinessAddon(routingInstanceID string, addonSpec *apistructs.AddonExtension) error {
	resp, err := httpclient.New().Delete(addonSpec.Domain).
		Path(fmt.Sprintf("/%s/dice/resources/%s", addonSpec.Name, routingInstanceID)).
		Do().DiscardBody()
	if err != nil {
		return err
	}
	if !resp.IsOK() {
		return errors.Errorf("failed to delete addon: %s, status code: %d", routingInstanceID, resp.StatusCode())
	}
	return nil
}

// deleteByRuntimeIDAndInstanceID 根据 runtimeID & routingInstanceID 删除 addon
func (a *Addon) deleteByRuntimeIDAndInstanceID(runtimeID uint64, routingInstanceID string) error {
	routing, err := a.db.GetInstanceRouting(routingInstanceID)
	if err != nil {
		return err
	}
	if routing == nil {
		return nil
	}
	total, err := a.db.GetAttachmentCountByRoutingInstanceID(routingInstanceID)
	if err != nil {
		return err
	}
	if total == 0 && routing.PlatformServiceType != apistructs.PlatformServiceTypeBasic {
		// 调用 pandora/tmc API 删除
		optionsMap := map[string]string{}
		if routing.Options != "" {
			if err := json.Unmarshal([]byte(routing.Options), &optionsMap); err != nil {
				logrus.Errorf("unmarshal error when delete addon, %+v", err)
				return err
			}
		}
		addonProviderRequest := apistructs.AddonProviderRequest{
			UUID:        routing.RealInstance,
			Plan:        routing.Plan,
			ClusterName: routing.Cluster,
			Options:     optionsMap,
		}
		addonSpec, err := a.getAddonExtension(routing.AddonName, routing.Version)
		if err != nil {
			logrus.Errorf("get extension err when delete addon, %+v", err)
			return err
		}
		if _, err := a.DeleteAddonProvider(&addonProviderRequest, routing.RealInstance, routing.AddonName, addonSpec.Domain); err != nil {
			logrus.Errorf("delete provider addon failed, error is %+v", err)
		}

		routing.Deleted = apistructs.AddonDeleted
		routing.Status = string(apistructs.AddonDetached)
		if err := a.db.UpdateInstanceRouting(routing); err != nil {
			logrus.Errorf("delete addon update error, %+v", err)
			return err
		}

		// 判断instance实例是否存在引用
		totalInsAtt, err := a.db.GetAttachmentCountByInstanceID(routing.RealInstance)
		if err != nil {
			return err
		}
		addonInsResult, err := a.db.GetAddonInstance(routing.RealInstance)
		if err != nil {
			return err
		}
		if totalInsAtt == 0 {
			addonInsResult.Deleted = apistructs.AddonDeleted
			addonInsResult.Status = string(apistructs.AddonDetached)
			if err := a.db.UpdateAddonInstance(addonInsResult); err != nil {
				logrus.Errorf("delete addon update error, %+v", err)
			}
		}
	}

	if err := a.db.DeleteAttachmentByRuntimeAndRoutingInstanceID(
		strconv.FormatUint(runtimeID, 10),
		routingInstanceID); err != nil {
		return err
	}
	if err := a.updateReference(routingInstanceID, false); err != nil {
		return err
	}

	relations, err := a.db.GetByOutSideInstanceID(routing.RealInstance)
	if err != nil {
		return err
	}
	if len(*relations) == 0 {
		return nil
	}
	for _, v := range *relations {
		if err := a.db.DeleteAttachmentByRuntimeAndInstanceID(
			strconv.FormatUint(runtimeID, 10),
			v.InsideInstanceID); err != nil {
			return err
		}
	}
	return nil
}

// updateReference 根据 routingInstanceID 更新引用计数
func (a *Addon) updateReference(routingInstanceID string, increase bool) error {
	// 更新 instanceRouting 引用计数
	instanceRouting, err := a.db.GetInstanceRouting(routingInstanceID)
	if err != nil || instanceRouting == nil {
		return err
	}
	if increase {
		instanceRouting.Reference++
	} else {
		if instanceRouting.Reference > 0 {
			instanceRouting.Reference--
		}

	}
	if err := a.db.UpdateInstanceRouting(instanceRouting); err != nil {
		return err
	}
	return nil
}

func (a *Addon) checkCreateParams(req *apistructs.AddonCreateRequest) error {
	// TODO cluster 取值范围校验
	if req.ClusterName == "" {
		return apierrors.ErrCreateAddon.MissingParameter("cluster")
	}
	if req.ProjectID == 0 {
		return apierrors.ErrCreateAddon.MissingParameter("projectId")
	}
	if req.ApplicationID == 0 {
		return apierrors.ErrCreateAddon.MissingParameter("applicatonId")
	}
	if req.RuntimeName == "" {
		return apierrors.ErrCreateAddon.MissingParameter("runtime name")
	}
	if req.Workspace == "" {
		return apierrors.ErrCreateAddon.MissingParameter("workspace")
	}

	// 校验 dicde.yml 里指定 addon & version 是否合法
	addonKeys := make([]string, 0, len(req.Addons))
	for _, v := range req.Addons {
		key := a.parseAddonName(v.Type)
		if version, ok := v.Options["version"]; ok {
			key = strutil.Concat(key, "@", version)
		}
		addonKeys = append(addonKeys, key)
	}
	// 从应用市场获取 addon
	extensionReq := apistructs.ExtensionSearchRequest{
		Extensions: addonKeys,
	}
	extensions, err := a.bdl.SearchExtensions(extensionReq)
	if err != nil {
		return err
	}
	for _, key := range addonKeys {
		if _, ok := extensions[key]; !ok {
			return errors.Errorf("invalid addon && version: %s", strutil.Split(key, "@"))
		}
	}

	return nil
}

// transAddonName 名称转换，用户dice.yml中可能会写zookeeper，但是市场中是terminus-zookeeper，需要做兼容
func (a *Addon) transAddonName(addonName string) string {
	if addonName == "zookeeper" {
		return apistructs.AddonZookeeper
	}
	if addonName == "elasticsearch" {
		return apistructs.AddonES
	}
	return addonName
}

// deployAddons addons 部署
func (a *Addon) deployAddons(req *apistructs.AddonCreateRequest, deploys []dbclient.AddonPrebuild) error {
	needDeployAddons := []apistructs.AddonHandlerCreateItem{}
	for _, v := range deploys {
		if _, ok := AddonInfos.Load(v.AddonName); !ok {
			a.ExportLogInfoDetail(apistructs.ErrorLevel, apistructs.RuntimeError, fmt.Sprintf("%d", req.RuntimeID),
				i18n2.OrgSprintf(strconv.FormatUint(req.OrgID, 10), "AddonTypeDoseNoExist", v.AddonName),
				fmt.Sprintf("not found addon: %s", v.AddonName))
			return errors.Errorf("not found addon: %s", v.AddonName)
		}

		createItem := &apistructs.AddonHandlerCreateItem{
			InstanceName:  v.InstanceName,
			AddonName:     v.AddonName,
			Plan:          v.Plan,
			ClusterName:   req.ClusterName,
			Workspace:     v.Env,
			OrgID:         strconv.FormatUint(req.OrgID, 10),
			ProjectID:     strconv.FormatUint(req.ProjectID, 10),
			ApplicationID: strconv.FormatUint(req.ApplicationID, 10),
			RuntimeID:     strconv.FormatUint(req.RuntimeID, 10),
			RuntimeName:   req.RuntimeName,
			OperatorID:    req.Operator,
		}
		if len(createItem.Options) == 0 {
			createItem.Options = map[string]string{}
		}
		if len(createItem.Config) == 0 {
			createItem.Config = map[string]string{}
		}

		requestOptions := a.StructToMap(req.Options, 0, "json")
		if len(requestOptions) > 0 {
			for k, v := range requestOptions {
				createItem.Options[k] = v.(string)
			}
		}
		if v.Config != "" {
			var config map[string]string
			if err := json.Unmarshal([]byte(v.Config), &config); err != nil {
				return err
			}
			createItem.Config = config
		}
		if v.Options != "" {
			var options map[string]string
			if err := json.Unmarshal([]byte(v.Options), &options); err != nil {
				return err
			}
			if len(options) > 0 {
				for k, v := range options {
					createItem.Options[k] = v
				}
			}
		}
		needDeployAddons = append(needDeployAddons, *createItem)
	}

	for index, v := range deploys {
		createItem := needDeployAddons[index]
		instanceRes, err := a.AttachAndCreate(&createItem)
		if err != nil {
			return errors.Wrapf(err, "failed to create addon: %s", v.AddonName)
		}
		v.InstanceID = instanceRes.RealInstanceID
		v.RoutingInstanceID = instanceRes.InstanceID

		if err := a.db.UpdatePrebuild(&v); err != nil {
			return err
		}
	}

	return nil
}

// PrepareCheckProjectLastResource 计算项目预留资源，是否满足发布徐局
func (a *Addon) PrepareCheckProjectLastResource(projectID uint64, req *[]apistructs.AddonHandlerCreateItem) error {
	// 获取项目资源信息
	if len(*req) == 0 {
		return nil
	}
	projectInfo, err := a.bdl.GetProject(projectID)
	if err != nil {
		return errors.Errorf("Failed to get project info, err: %v", err)
	}
	if projectInfo == nil {
		return errors.Errorf("No project information found, err: %v", err)
	}
	deployNeedMem := 0.0
	deployNeedCpu := 0.0
	for _, v := range *req {
		if _, ok := ExtensionDeployAddon[v.AddonName]; ok {
			// 查看此reques是否是真的不存在addon需要发布
			routing, err := a.FindNeedCreateAddon(&v)
			// 如果存在，则不计算在deploy所需资源中
			if routing != nil && routing.ID != "" {
				continue
			}
			addonSpec, _, err := a.GetAddonExtention(&apistructs.AddonHandlerCreateItem{
				AddonName: v.AddonName,
				Plan:      v.Plan,
				Options:   v.Options,
			})
			if err != nil {
				return err
			}
			if len((*addonSpec).Plan[v.Plan].InsideMoudle) == 0 {
				deployNeedMem += float64((*addonSpec).Plan[v.Plan].Nodes * (*addonSpec).Plan[v.Plan].Mem)
				deployNeedCpu += float64(float64((*addonSpec).Plan[v.Plan].Nodes) * (*addonSpec).Plan[v.Plan].CPU)
				continue
			}
			for _, item := range (*addonSpec).Plan[v.Plan].InsideMoudle {
				deployNeedMem += float64(item.Nodes * item.Mem)
				deployNeedCpu += float64(item.Nodes) * item.CPU
			}
		}
	}
	logrus.Infof("PrepareCheckProjectLastResource deployNeedMem: %f, deployNeedCpu: %f", deployNeedMem, deployNeedCpu)
	// 获取项目所使用service信息
	serviceResource, err := a.resource.GetProjectServiceResource([]uint64{projectID})
	if err != nil {
		return errors.Errorf("Failed to get project service resources, err: %v", err)
	}
	cc, _ := json.Marshal(serviceResource)
	logrus.Infof("PrepareCheckProjectLastResource serviceResource: %s", string(cc))
	// 获取项目所使用addon信息
	addonResource, err := a.resource.GetProjectAddonResource([]uint64{projectID})
	if err != nil {
		return errors.Errorf("Failed to get project addon resources, err: %v", err)
	}
	bb, _ := json.Marshal(addonResource)
	logrus.Infof("PrepareCheckProjectLastResource addonResource: %s", string(bb))

	// 对service和addon的资源，进行累加
	usedMem := 0.0
	usedCpu := 0.0
	if len(*serviceResource) > 0 {
		usedMem += (*serviceResource)[projectID].MemServiceUsed * 1024
		usedCpu += (*serviceResource)[projectID].CpuServiceUsed
	}
	if addonResource != nil && len(*addonResource) > 0 {
		logrus.Infof("addon resource cpu: %f", (*addonResource)[projectID].CpuServiceUsed)
		logrus.Infof("addon resource mem: %f", (*addonResource)[projectID].MemServiceUsed)
		usedMem += (*addonResource)[projectID].MemAddonUsed * 1024
		usedCpu += (*addonResource)[projectID].CpuAddonUsed
	}

	// 获取最近一次deployment的数据
	runtimeID, err := strconv.ParseUint((*req)[0].RuntimeID, 10, 64)
	if err != nil {
		return err
	}
	runtimeInfo, err := a.db.GetRuntime(runtimeID)
	if err != nil {
		return err
	}
	if runtimeInfo.CPU != 0.0 {
		deployment, err := a.db.FindLastDeployment(runtimeID)
		if err != nil {
			return err
		}
		var dice diceyml.Object
		if err := json.Unmarshal([]byte(deployment.Dice), &dice); err != nil {
			return apierrors.ErrGetRuntime.InvalidState(strutil.Concat("dice.json invalid: ", err.Error()))
		}
		localMem := 0.0
		localCpu := 0.0
		for _, v := range dice.Jobs {
			localMem += float64(v.Resources.Mem)
			localCpu += v.Resources.CPU
		}
		for _, v := range dice.Services {
			localMem += float64(v.Deployments.Replicas) * float64(v.Resources.Mem)
			localCpu += float64(v.Deployments.Replicas) * v.Resources.CPU
		}
		if utils.Smaller(localMem, runtimeInfo.Mem) {
			usedMem -= runtimeInfo.Mem - localMem
		}
		if utils.Smaller(localCpu, runtimeInfo.CPU) {
			usedCpu -= runtimeInfo.CPU - localCpu
		}
	}

	logrus.Infof("PrepareCheckProjectLastResource check used cpu: %f, used mem: %f", usedCpu, usedMem)

	// 比较项目quota预留资源是不是够
	if utils.Smaller(projectInfo.CpuQuota-usedCpu, deployNeedCpu) {
		s := fmt.Sprintf("The CPU reserved for the project is %.2f cores, %.2f cores have been occupied, %.2f CPUs are required for deploy, and the resources for addon are insufficient", projectInfo.CpuQuota, usedCpu, deployNeedCpu)
		a.ExportLogInfoDetail(apistructs.ErrorLevel, apistructs.RuntimeError, (*req)[0].RuntimeID, i18n2.OrgSprintf(strconv.FormatUint(runtimeInfo.OrgID, 10), "NotEnoughQuotaToDeploy"), s)
		return errors.Errorf(s)
	}
	useMem2, err := strconv.ParseFloat(fmt.Sprintf("%.2f", usedMem), 64)
	if err != nil {
		return err
	}
	if utils.Smaller(projectInfo.MemQuota*1024.0-float64(usedMem), deployNeedMem) {
		s := fmt.Sprintf("The memory reserved for the project is %.2f G, %.2f G have been occupied, %.2f G are required for deploy, and the resources for addon are insufficient", projectInfo.MemQuota, useMem2/1024, deployNeedMem/1024.0)
		a.ExportLogInfoDetail(apistructs.ErrorLevel, apistructs.RuntimeError, (*req)[0].RuntimeID, i18n2.OrgSprintf(strconv.FormatUint(runtimeInfo.OrgID, 10), "NotEnoughQuotaToDeploy"), s)
		return errors.Errorf(s)
	}

	return nil
}

// StructToMap
func (a *Addon) StructToMap(data interface{}, depth int, tag ...string) map[string]interface{} {
	m := make(map[string]interface{})
	values := reflect.ValueOf(data)
	types := reflect.TypeOf(data)
	for types.Kind() == reflect.Ptr {
		values = values.Elem()
		types = types.Elem()
	}
	num := types.NumField()
	depth = depth - 1
	if len(tag) <= 0 || tag[0] == "" {
		if depth == -1 {
			for i := 0; i < num; i++ {
				t := types.Field(i)
				v := values.Field(i)
				if v.CanInterface() {
					m[t.Name] = v.Interface()
				}
			}
		} else {
			for i := 0; i < num; i++ {
				t := types.Field(i)
				v := values.Field(i)
				v_struct := v
				v_struct_ptr := v
				for v_struct.Kind() == reflect.Ptr {
					v_struct_ptr = v_struct
					v_struct = v_struct.Elem()
				}
				if v_struct.Kind() == reflect.Struct && v_struct_ptr.CanInterface() {
					m[t.Name] = a.StructToMap(v_struct_ptr.Interface(), depth, tag[0])
				} else {
					if v.CanInterface() {
						m[t.Name] = v.Interface()
					}
				}
			}
		}
	} else {
		tagName := tag[0]
		if depth == -1 {
			for i := 0; i < num; i++ {
				t := types.Field(i)
				v := values.Field(i)
				tagVal := t.Tag.Get(tagName)
				if v.CanInterface() && tagVal != "" && tagVal != "-" {
					m[tagVal] = v.Interface()
				}
			}
		} else {
			for i := 0; i < num; i++ {
				t := types.Field(i)
				v := values.Field(i)
				tagVal := t.Tag.Get(tagName)
				if tagVal != "" && tagVal != "-" {
					v_struct := v
					v_struct_ptr := v
					for v_struct.Kind() == reflect.Ptr {
						v_struct_ptr = v_struct
						v_struct = v_struct.Elem()
					}
					if v_struct.Kind() == reflect.Struct && v_struct_ptr.CanInterface() {
						m[tagVal] = a.StructToMap(v_struct_ptr.Interface(), depth, tag[0])
						continue
					}
					if v.CanInterface() {
						m[tagVal] = v.Interface()
					}
				}
			}
		}
	}
	return m
}

// removeUselessPrebuilds 删除无用的 addonPrebuild 记录
func (a *Addon) removeUselessPrebuilds(runtimeID uint64, newPrebuildList []dbclient.AddonPrebuild) error {
	newPrebuildMap := make(map[string]dbclient.AddonPrebuild, len(newPrebuildList))
	for _, v := range newPrebuildList {
		newPrebuildMap[strutil.Concat(v.RuntimeID, v.AddonName, v.InstanceName)] = v
	}
	existBuilds, err := a.db.GetPreBuildsByRuntimeID(runtimeID)
	if err != nil {
		return err
	}
	for _, v := range *existBuilds {
		if v.BuildFrom == apistructs.AddonBuildFromUI {
			continue
		}
		if v.DeleteStatus != apistructs.AddonPrebuildNotDeleted {
			continue
		}
		if _, ok := newPrebuildMap[strutil.Concat(v.RuntimeID, v.AddonName, v.InstanceName)]; !ok {
			v.DeleteStatus = apistructs.AddonPrebuildDiceYmlDeleted
			if err := a.db.UpdatePrebuild(&v); err != nil {
				return err
			}
		}
	}
	return nil
}

// GetDeployAndRemoveAddons 获取待部署 addons & 待删除 addons
func (a *Addon) GetDeployAndRemoveAddons(runtimeID uint64) ([]dbclient.AddonPrebuild, []dbclient.AddonInstanceRouting, error) {
	createdInstanceRoutings, err := a.ListInstanceRoutingByRuntime(runtimeID)
	if err != nil {
		return nil, nil, err
	}
	// 获取 runtime 下的全量 prebuild
	allPreBuilds, err := a.db.GetPreBuildsByRuntimeID(runtimeID)
	if err != nil {
		return nil, nil, err
	}
	deployList := make([]dbclient.AddonPrebuild, 0, len(*allPreBuilds))
	// 数据库中没有发布信息，是首次发布
	if createdInstanceRoutings == nil || len(*createdInstanceRoutings) == 0 {
		// 遍历prebuild列表，将没有删除的数据全部放入deployList中，准备发布
		for _, v := range *allPreBuilds {
			if v.DeleteStatus == apistructs.AddonPrebuildNotDeleted {
				deployList = append(deployList, v)
			}
		}
		// 直接返回数据
		return sortPrebuild(deployList), nil, nil
	}
	// 遍历出所有未删除的prebuild信息
	notDeletedPrebuildMap := make(map[string]string)
	canalDeploying := false
	for _, v := range *allPreBuilds {
		if v.DeleteStatus == apistructs.AddonPrebuildNotDeleted && v.Deleted == apistructs.AddonNotDeleted && v.RoutingInstanceID != "" {
			notDeletedPrebuildMap[v.RoutingInstanceID] = v.InstanceID
		}
		if v.AddonName == apistructs.AddonCanal && v.DeleteStatus == apistructs.AddonPrebuildNotDeleted && v.RoutingInstanceID != "" {
			canalDeploying = true
		}
	}

	// 实例列表转map
	instanceRoutingMap := make(map[string]dbclient.AddonInstanceRouting, len(*createdInstanceRoutings))
	for _, v := range *createdInstanceRoutings {
		instanceRoutingMap[v.ID] = v
	}

	removeList := make([]dbclient.AddonInstanceRouting, 0, len(*allPreBuilds))
	for _, v := range *allPreBuilds {
		if v.RoutingInstanceID != "" {
			// canal特殊处理，如果能查询到结果，说明已经发布成功，不需要再走deploy流程
			if v.AddonName == apistructs.AddonCanal {
				canalRouting, err := a.db.GetInstanceRouting(v.RoutingInstanceID)
				if err != nil {
					return nil, nil, err
				}
				if canalRouting != nil {
					if canalRouting.Status == string(apistructs.AddonAttached) || canalRouting.Status == string(apistructs.AddonAttaching) {
						if v.DeleteStatus != apistructs.AddonPrebuildNotDeleted && !canalDeploying {
							removeList = append(removeList, *canalRouting)
							continue
						}
						continue
					}
				}
			}
			instanceRouting, ok := instanceRoutingMap[v.RoutingInstanceID]
			if !ok {
				// addon 创建为异步过程，若上次 addon 创建失败, prebuild 表记录 instanceRoutingID 不为空，但 instanceRouting 记录为 ATTACH_FAIL
				if v.DeleteStatus == apistructs.AddonPrebuildNotDeleted {
					deployList = append(deployList, v)
				}
				continue
			}

			if (instanceRouting.Status == string(apistructs.AddonAttached) || instanceRouting.Status == string(apistructs.AddonAttaching)) &&
				v.DeleteStatus != apistructs.AddonPrebuildNotDeleted {
				// 如果不在已经发布的列表map中，才能remove
				if _, ok := notDeletedPrebuildMap[v.RoutingInstanceID]; !ok {
					removeList = append(removeList, instanceRouting)
				}
			}
			if instanceRouting.Status == string(apistructs.AddonAttachFail) &&
				v.DeleteStatus == apistructs.AddonPrebuildNotDeleted {
				deployList = append(deployList, v)
			}
		} else if v.DeleteStatus == apistructs.AddonPrebuildNotDeleted {
			deployList = append(deployList, v)
		}
	}
	return sortPrebuild(deployList), removeList, nil
}

func sortPrebuild(prebuilds []dbclient.AddonPrebuild) []dbclient.AddonPrebuild {
	canalPrebuilds := []dbclient.AddonPrebuild{}
	others := []dbclient.AddonPrebuild{}
	for _, i := range prebuilds {
		if i.AddonName == "canal" {
			canalPrebuilds = append(canalPrebuilds, i)
		} else {
			others = append(others, i)
		}
	}
	return append(others, canalPrebuilds...)
}

// ListInstanceRoutingByRuntime 根据 runtimeID 获取已创建 addon 实例列表
func (a *Addon) ListInstanceRoutingByRuntime(runtimeID uint64) (*[]dbclient.AddonInstanceRouting, error) {
	attachments, err := a.db.GetAttachMentsByRuntimeID(runtimeID)
	if err != nil {
		return nil, err
	}
	if len(*attachments) == 0 {
		return nil, nil
	}

	instanceRoutings := make([]dbclient.AddonInstanceRouting, 0, len(*attachments))
	for i := range *attachments {
		instanceRouting, err := a.db.GetInstanceRouting((*attachments)[i].RoutingInstanceID)
		if err != nil {
			return nil, err
		}
		if instanceRouting == nil {
			continue
		}
		instanceRoutings = append(instanceRoutings, *instanceRouting)
	}

	return &instanceRoutings, nil
}

func (a *Addon) ListInstanceByRuntime(runtimeID uint64) ([]dbclient.AddonInstance, error) {
	attachments, err := a.db.GetAttachMentsByRuntimeID(runtimeID)
	if err != nil {
		return nil, err
	}
	if attachments == nil || len(*attachments) == 0 {
		return nil, nil
	}
	instances := []dbclient.AddonInstance{}
	for _, attach := range *attachments {
		instance, err := a.db.GetAddonInstance(attach.InstanceID)
		if err != nil {
			return nil, err
		}
		if instance == nil {
			continue
		}
		instances = append(instances, *instance)
	}
	return instances, nil
}

// ParsePreBuild 根据 addon 信息生成 AddonPreBuild 对象
func (a *Addon) ParsePreBuild(appID, runtimeID uint64, runtimeName, workspace string, addon apistructs.AddonCreateItem) *dbclient.AddonPrebuild {
	ap := &dbclient.AddonPrebuild{
		ApplicationID: strconv.FormatUint(appID, 10),
		RuntimeID:     strconv.FormatUint(runtimeID, 10),
		GitBranch:     runtimeName,
		Env:           workspace,
		InstanceName:  addon.Name,
		AddonName:     a.parseAddonName(addon.Type),
		Plan:          addon.Plan,
		Deleted:       apistructs.AddonNotDeleted,
	}
	if len(addon.Options) > 0 {
		options, _ := json.Marshal(addon.Options)
		ap.Options = string(options)
	}
	if len(addon.Configs) > 0 {
		configs, _ := json.Marshal(addon.Configs)
		ap.Config = string(configs)
	}

	return ap
}

// CheckCustomAddonPermission 校验 custom addon 权限
func (a *Addon) CheckCustomAddonPermission(userID string, orgID, projecID uint64) (bool, error) {
	if orgID != 0 {
		resp, err := a.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   userID,
			Scope:    apistructs.OrgScope,
			ScopeID:  orgID,
			Resource: apistructs.CustomAddonResource,
			Action:   apistructs.CreateAction,
		})
		if err != nil {
			return false, err
		}
		if resp.Access {
			return true, nil
		}
	}

	if projecID != 0 {
		resp, err := a.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   userID,
			Scope:    apistructs.ProjectScope,
			ScopeID:  projecID,
			Resource: apistructs.CustomAddonResource,
			Action:   apistructs.CreateAction,
		})
		if err != nil {
			return false, err
		}
		if resp.Access {
			return true, nil
		}
	}

	return false, nil
}

// 获取默认 addon 列表(若用户在 dice.yml 里未制定，默认为用户创建)
func (a *Addon) getDefaultPreBuildList(appID, runtimeID uint64, runtimeName, workspace string) []dbclient.AddonPrebuild {
	// 添加默认 addon(目前只有monitor)
	defaults := make([]dbclient.AddonPrebuild, 0)
	addon := apistructs.AddonCreateItem{
		Name: "monitor",
		Type: "monitor",
		Plan: "professional",
	}
	item := a.ParsePreBuild(appID, runtimeID, runtimeName, workspace, addon)
	defaults = append(defaults, *item)

	return defaults
}

// 兼容老版
func (a *Addon) parseAddonName(addonType string) string {
	switch addonType {
	case "zookeeper":
		return apistructs.AddonZookeeper
	case "elasticsearch":
		return apistructs.AddonES
	case "configcenter":
		return apistructs.AddonConfigCenter
	default:
		return addonType
	}
}

func (a *Addon) convertTenantInstance(t *dbclient.AddonInstanceTenant, routingInstance *dbclient.AddonInstanceRouting) apistructs.AddonFetchResponseData {
	addonResp := a.convert(routingInstance)

	config := make(map[string]interface{})
	if err := json.Unmarshal([]byte(t.Config), &config); err != nil {
		logrus.Errorf("fail to json unmarshal instance: %s config: %s", t.ID, t.Config)
	}
	addonResp.Config = config
	addonResp.TenantOwner = routingInstance.ID
	addonResp.ID = t.ID
	addonResp.Name = t.Name
	return addonResp
}

// convert AddonInstanceRouting转换为AddonFetchResponseData
func (a *Addon) convert(routingInstance *dbclient.AddonInstanceRouting) apistructs.AddonFetchResponseData {
	orgID, _ := strconv.ParseUint(routingInstance.OrgID, 10, 64)
	projectID, _ := strconv.ParseUint(routingInstance.ProjectID, 10, 64)

	addonResp := apistructs.AddonFetchResponseData{
		ID:                  routingInstance.ID,
		Name:                routingInstance.Name,
		AddonName:           routingInstance.AddonName,
		AddonDisplayName:    routingInstance.Name,
		Plan:                routingInstance.Plan,
		Version:             routingInstance.Version,
		Category:            routingInstance.Category,
		ShareScope:          routingInstance.ShareScope,
		Cluster:             routingInstance.Cluster,
		OrgID:               orgID,
		ProjectID:           projectID,
		AttachCount:         routingInstance.Reference,
		Tag:                 routingInstance.Tag,
		PlatformServiceType: routingInstance.PlatformServiceType,
		Reference:           routingInstance.Reference,
		Workspace:           routingInstance.Workspace,
		Status:              routingInstance.Status,
		RealInstanceID:      routingInstance.RealInstance,
		Platform:            routingInstance.IsPlatform,
		CreatedAt:           routingInstance.CreatedAt,
		UpdatedAt:           routingInstance.UpdatedAt,
		IsInsideAddon:       routingInstance.InsideAddon,
	}

	if routingInstance.Options != "" {
		options := make(map[string]interface{})
		err := json.Unmarshal([]byte(routingInstance.Options), &options)
		if err != nil {
			logrus.Errorf("fail to json unmarshal instance: %s options: %s", routingInstance.ID, routingInstance.Options)
		}
		if v, ok := options["recordId"]; ok {
			addonResp.RecordID = int(v.(float64))
		}
		if routingInstance.Category == apistructs.AddonCustomCategory {
			addonResp.CustomAddonType = apistructs.CUSTOM_TYPE_CUSTOM
		}
		if v, ok := options["customAddonType"]; ok {
			addonResp.CustomAddonType = v.(string)
		}
	}

	if v, ok := AddonInfos.Load(routingInstance.AddonName); ok {
		ext := v.(apistructs.Extension)
		addonResp.AddonDisplayName = ext.DisplayName
		addonResp.LogoURL = ext.LogoUrl
		addonResp.Desc = ext.Desc
	} else {
		logrus.Warnf("failed to fetch addon info: %s", routingInstance.AddonName)
	}
	ins, err := a.db.GetAddonInstance(routingInstance.RealInstance)
	if err != nil || ins == nil {
		logrus.Warnf("failed to fetch addon instance: %v", err)
	}

	if ins != nil && ins.Config != "" {
		config := make(map[string]interface{})
		if err := json.Unmarshal([]byte(ins.Config), &config); err != nil {
			logrus.Errorf("fail to json unmarshal instance: %s config: %s", ins.ID, ins.Config)
		}
		for i := range config { // remove all env values
			config[i] = ""
		}
		addonResp.Config = config
	}

	if routingInstance.PlatformServiceType != 0 {
		addonResp.Name = addonResp.AddonDisplayName
		// 获取public_host
		if ins != nil && ins.Config != "" {
			config := make(map[string]interface{})
			err := json.Unmarshal([]byte(ins.Config), &config)
			if err != nil {
				logrus.Errorf("fail to json unmarshal instance: %s config: %s", ins.ID, ins.Config)
			} else {
				if publicHost, ok := config["PUBLIC_HOST"]; ok {
					addonResp.ConsoleUrl = publicHost.(string)
				}
				// 当 addon 为 monitor 时，须返回 TERMINUS KEY
				if routingInstance.AddonName == "monitor" {
					// 填充 config 信息
					if terminusKey, ok := config["TERMINUS_KEY"]; ok {
						addonResp.TerminusKey = terminusKey.(string)
					}
				}
			}
		}

	}
	// 填充 projectName
	if routingInstance.ProjectID != "" {
		project := apistructs.ProjectDTO{}

		if v, ok := ProjectInfos.Load(routingInstance.ProjectID); !ok {
			// 若找不到项目信息，根据 projectID 实时获取并缓存
			projResp, err := a.bdl.GetProject(projectID)
			if err != nil {
				logrus.Warnf("failed to convert addon instance, %v", err)
				return addonResp
			}
			project = *projResp
			ProjectInfos.Store(routingInstance.ProjectID, *projResp)
		} else {
			project = v.(apistructs.ProjectDTO)
		}
		addonResp.ProjectName = project.Name
	}

	return addonResp
}

// getProject 根据 projectID 获取项目信息
func (a *Addon) getProject(projectID string) (*apistructs.ProjectDTO, error) {
	project := apistructs.ProjectDTO{}

	if v, ok := ProjectInfos.Load(projectID); !ok {
		id, err := strconv.ParseUint(projectID, 10, 64)
		if err != nil {
			return nil, errors.Errorf("failed to convert project id: %s, %v", projectID, err)
		}

		// 若找不到项目信息，根据 projectID 实时获取并缓存
		projectResp, err := a.bdl.GetProject(id)
		if err != nil {
			return nil, errors.Errorf("failed to get project, %v", err)
		}
		project = *projectResp
		ProjectInfos.Store(projectID, project)
	} else {
		project = v.(apistructs.ProjectDTO)
	}
	return &project, nil
}

func (a *Addon) AddonYmlExport(projectID string) (*diceyml.Object, error) {
	projectID_int, err := strconv.ParseUint(projectID, 10, 64)
	if err != nil {
		return nil, err
	}
	addons, err := a.db.ListAddonInstancesByProjectIDs([]uint64{projectID_int})
	if err != nil {
		return nil, err
	}
	if addons == nil {
		return nil, nil
	}
	res := diceyml.AddOns{}
	for _, addon := range *addons {
		if addon.AddonName != "custom" { // 只导出 custom addon
			continue
		}
		res[addon.Name] = &diceyml.AddOn{
			Plan:    "custom:basic",
			Options: map[string]string{},
		}
		res[addon.Name].Options["config"] = addon.Config
		res[addon.Name].Options["workspace"] = addon.Workspace
	}
	r := diceyml.Object{}
	r.AddOns = res
	return &r, nil
}

func (a *Addon) AddonYmlImport(projectID uint64, yml diceyml.Object, userid string) error {
	addons := yml.AddOns
	for name, addon := range addons {
		plan := strutil.Split(addon.Plan, ":", true)
		if len(plan) < 1 || plan[0] != "custom" {
			continue
		}
		workspace, ok := addon.Options["workspace"]
		if !ok {
			continue
		}
		config, ok := addon.Options["config"]
		if !ok {
			continue
		}
		configmap := map[string]interface{}{}
		if err := json.Unmarshal([]byte(config), &configmap); err != nil {
			continue
		}

		if _, err := a.CreateCustom(&apistructs.CustomAddonCreateRequest{
			Name:       name,
			AddonName:  "custom",
			Configs:    configmap,
			ProjectID:  projectID,
			Workspace:  workspace,
			OperatorID: userid,
		}); err != nil {
			logrus.Errorf("failed to createcustom: %v", err)
			continue
		}
	}
	return nil
}

func (a *Addon) ListAddonInstanceByOrg(orgid uint64) ([]dbclient.AddonInstance, error) {
	addons, err := a.db.ListAddonInstanceByOrg(orgid)
	if err != nil {
		return nil, err
	}
	return *addons, nil
}
