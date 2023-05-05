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
	"encoding/json"
	"fmt"
	"math/rand"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-proto-go/msp/tenant/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/internal/tools/orchestrator/conf"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/internal/tools/orchestrator/i18n"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/log"
	"github.com/erda-project/erda/internal/tools/orchestrator/utils"
	"github.com/erda-project/erda/pkg/crypto/uuid"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/kms/kmstypes"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/strutil"
)

// AttachAndCreate addon创建，runtime建立关系方法
func (a *Addon) AttachAndCreate(params *apistructs.AddonHandlerCreateItem) (*apistructs.AddonInstanceRes, error) {
	// 获取addon extension信息
	addonSpec, addonDice, err := a.GetAddonExtention(params)
	if err != nil {
		logrus.Errorf("AttachAndCreate GetAddonExtention err:  %v", err)
		return nil, err
	}
	return a.addonAttach(addonSpec, addonDice, params)
}

// GetAddonExtention 获取addon的spec，dice.yml信息
func (a *Addon) GetAddonExtention(params *apistructs.AddonHandlerCreateItem) (*apistructs.AddonExtension, *diceyml.Object, error) {
	extentionsList, err := a.bdl.QueryExtensionVersions(apistructs.ExtensionVersionQueryRequest{Name: params.AddonName, All: true})
	if err != nil {
		return nil, nil, errors.New("没有匹配的addon信息")
	}

	// 查看用户是否设置了版本号，如果没有，则选择默认版本
	var addon apistructs.ExtensionVersion
	if len(extentionsList) == 1 {
		addon = extentionsList[0]
	} else {
		if params.Options["version"] != "" {
			for _, extentionItem := range extentionsList {
				if extentionItem.Version == params.Options["version"] {
					addon = extentionItem
					break
				}
			}
		} else {
			// 用户没有指定version，则选择默认版本
			for _, extentionItem := range extentionsList {
				if extentionItem.IsDefault {
					logrus.Info("addon extension name ready")
					addon = extentionItem
				}
			}
		}
	}

	if addon.Version == "" {
		return nil, nil, errors.New("failed to create addon, can't find information about addon " + params.AddonName)
	}
	// 检查是否有plan
	if params.Plan == "" {
		params.Plan = string(apistructs.AddonBasic)
	}
	if len(params.Options) == 0 {
		params.Options = map[string]string{}
	}
	params.Options["version"] = addon.Version

	// spec.yml强制转换为string类型
	addonSpecBytes, err := json.Marshal(addon.Spec)
	if err != nil {
		return nil, nil, errors.Wrap(err, "failed to parse addon spec")
	}
	addonSpec := apistructs.AddonExtension{}
	specErr := json.Unmarshal(addonSpecBytes, &addonSpec)
	if specErr != nil {
		return nil, nil, errors.Wrap(specErr, "failed to parse addon spec")
	}
	if len(params.Options) == 0 {
		params.Options = map[string]string{}
	}
	params.Options["version"] = addonSpec.Version
	// dice.yml强制转换为string类型
	diceYmlBytes, err := json.Marshal(addon.Dice)
	if err != nil {
		logrus.Errorf("ext market %s ExtensionVersion.Dice is not string type", addon.Name)
	}
	addonDice := diceyml.Object{}
	diceErr := json.Unmarshal(diceYmlBytes, &addonDice)
	if diceErr != nil {
		return nil, nil, errors.Wrap(diceErr, "failed to parse addon dice")
	}
	return &addonSpec, &addonDice, nil
}

func (a *Addon) AddonDelete(req apistructs.AddonDirectDeleteRequest) error {
	addonIns, err := a.db.GetAddonInstance(req.ID)
	if err != nil {
		return err
	}
	attachments, err := a.db.GetAttachmentsByInstanceID(addonIns.ID)
	if err != nil {
		return err
	}
	if attachments != nil && len(*attachments) > 0 {
		return fmt.Errorf("addon(%s) exists one or more attachments", addonIns.ID)
	}
	if addonIns.Category == apistructs.AddonCustomCategory {
		return fmt.Errorf("not support remove custom addon yet")
	}
	if addonIns.PlatformServiceType != apistructs.PlatformServiceTypeBasic {
		addonProviderRequest := apistructs.AddonProviderRequest{
			UUID:        addonIns.ID,
			Plan:        addonIns.Plan,
			ClusterName: addonIns.Cluster,
		}
		addonSpec, _, err := a.GetAddonExtention(&apistructs.AddonHandlerCreateItem{
			AddonName: addonIns.AddonName,
			Plan:      addonIns.Plan,
			Options: map[string]string{
				"version": addonIns.Version,
			},
		})
		if err != nil {
			return err
		}
		if _, err := a.DeleteAddonProvider(&addonProviderRequest, addonIns.ID, addonIns.AddonName, addonSpec.Domain); err != nil {
			logrus.Errorf("delete provider addon failed, error is %+v", err)
			return err
		}
		if err := a.UpdateAddonStatus(addonIns, apistructs.AddonDetached); err != nil {
			logrus.Errorf("sync remove provider addon error, %+v", err)
			return err
		}
	} else {
		if err := a.serviceGroupImpl.Delete(addonIns.Namespace, addonIns.ScheduleName, "false"); err != nil {
			logrus.Errorf("failed to delete addon: %s/%s", addonIns.Namespace, addonIns.ScheduleName)
			return err
		}
		if err := a.UpdateAddonStatus(addonIns, apistructs.AddonDetached); err != nil {
			logrus.Errorf("sync remove basic addon error, %+v", err)
			return err
		}
	}
	return nil
}

func (a *Addon) AddonCreate(req apistructs.AddonDirectCreateRequest) (string, error) {
	if len(req.Addons) != 1 {
		return "", fmt.Errorf("len(req.Addons) != 1")
	}
	baseAddons := []apistructs.AddonCreateItem{}
	for name, a := range req.Addons {
		plan := strings.SplitN(a.Plan, ":", 2)
		if len(plan) != 2 {
			return "", errors.Errorf("addon plan information is not compliant")
		}
		baseAddons = append(baseAddons, apistructs.AddonCreateItem{
			Name:    name,
			Type:    plan[0],
			Plan:    plan[1],
			Options: a.Options,
		})
	}
	addonItem := apistructs.AddonHandlerCreateItem{
		InstanceName:  baseAddons[0].Name,
		AddonName:     a.parseAddonName(baseAddons[0].Type),
		Plan:          baseAddons[0].Plan,
		ClusterName:   req.ClusterName,
		Workspace:     strutil.ToUpper(req.Workspace),
		OrgID:         strconv.FormatUint(req.OrgID, 10),
		ProjectID:     strconv.FormatUint(req.ProjectID, 10),
		ApplicationID: strconv.FormatUint(req.ApplicationID, 10),
		OperatorID:    req.Operator,
		InsideAddon:   "N",
		ShareScope:    req.ShareScope,
		Options:       baseAddons[0].Options,
	}
	addonSpec, addonDice, err := a.GetAddonExtention(&addonItem)
	if err != nil {
		logrus.Errorf("failed to GetAddonExtention err: %v", err)
		return "", err
	}

	if err := a.checkAddonDeployable(addonItem, addonSpec); err != nil {
		return "", err
	}
	return a.addonCreateAux(addonSpec, addonDice, &addonItem)
}

// checkAddonDeployable 检查 addon 是否能部署
func (a *Addon) checkAddonDeployable(addon apistructs.AddonHandlerCreateItem, spec *apistructs.AddonExtension) error {
	if spec.SubCategory == "microservice" && strutil.ToUpper(spec.ShareScopes[0]) == "PROJECT" {
		instances, err := a.db.GetAddonInstanceRoutingByProjectAndAddonName(addon.ProjectID, addon.ClusterName, addon.AddonName, addon.Workspace)
		if err != nil {
			return err
		}
		if instances != nil && len(*instances) > 0 {
			return fmt.Errorf("[project(%s)/workspace(%s)] 已存在 microservice(%s), 无法再新建",
				addon.ProjectID, addon.Workspace, addon.AddonName)
		}
	}

	switch strutil.ToLower(addon.AddonName) {
	case "canal":
		if addon.Options["mysql"] == "" && (addon.Options["canal.instance.master.address"] == "" ||
			addon.Options["canal.instance.dbUsername"] == "" ||
			addon.Options["canal.instance.dbPassword"] == "") {
			return fmt.Errorf("创建 canal 参数不足: canal.instance.master.address, canal.instance.dbUsername, canal.instance.dbPassword")

		}
	}

	return nil

}

func (a *Addon) addonCreateAux(addonSpec *apistructs.AddonExtension, addonDice *diceyml.Object,
	params *apistructs.AddonHandlerCreateItem) (string, error) {

	if params.Options == nil {
		params.Options = map[string]string{}
	}
	params.Options["orgId"] = params.OrgID
	params.Options["projectId"] = params.ProjectID
	projectid, err := strconv.ParseUint(params.ProjectID, 10, 64)
	if err != nil {
		return "", fmt.Errorf("failed to parse projectid(%s): %v", params.ProjectID, err)
	}
	project, err := a.bdl.GetProject(projectid)
	if err != nil {
		return "", err
	}
	params.Options["projectName"] = project.Name
	params.Options["env"] = strutil.ToUpper(params.Workspace)
	params.Options["clusterName"] = params.ClusterName

	if err := a.buildRealCreate(addonSpec, params); err != nil {
		return "", err
	}

	addonIns, err := a.buildAddonInstance(addonSpec, params)
	if err != nil {
		return "", err
	}
	addonInsRouting := a.buildAddonInstanceRouting(addonSpec, params, addonIns, 0)
	logrus.Infof("add addonInstance info to db, addon: %s, runtime: %s", addonIns.AddonName, addonIns.AppID)
	if err := a.db.CreateAddonInstance(addonIns); err != nil {
		return "", err
	}
	logrus.Infof("add instanceRouting info to db, addon: %s, runtime: %s", addonInsRouting.AddonName, addonInsRouting.AppID)
	if err := a.db.CreateAddonInstanceRouting(addonInsRouting); err != nil {
		return "", err
	}
	go func() {
		logrus.Infof("sending addon creating request, addon: %s, runtime: %s", params.AddonName, params.RuntimeID)
		if err := a.createAddonResource(addonIns, addonInsRouting, addonSpec, addonDice, params); err != nil {
			logrus.Errorf("failed to create addon: %s, err: %+v", addonIns.ID, err)
			a.FailAndDelete(addonIns)
		}
		nodes := 0
		for _, s := range addonDice.Services {
			nodes += s.Deployments.Replicas
		}

		cpu := 0.0
		mem := 0
		config := ""
		if len(addonDice.Services) > 0 {
			for _, s := range addonDice.Services {
				cpu = s.Resources.CPU
				mem = s.Resources.Mem
				config = s.Envs["config"]
			}
		}
		if err := a.db.Create(&dbclient.AddonManagement{
			AddonID:     addonIns.ID,
			Name:        addonIns.Name,
			ProjectID:   addonIns.ProjectID,
			OrgID:       addonIns.OrgID,
			AddonConfig: config,
			CPU:         cpu,
			Mem:         uint64(mem),
			Nodes:       nodes,
			CreateTime:  time.Now(),
			UpdateTime:  time.Now(),
		}).Error; err != nil {
			logrus.Errorf("failed to create opsdb.AddonManagement: %v", err)
		}

	}()
	return addonInsRouting.ID, nil
}

// addon attach功能
func (a *Addon) addonAttach(addonSpec *apistructs.AddonExtension, addonDice *diceyml.Object,
	params *apistructs.AddonHandlerCreateItem) (*apistructs.AddonInstanceRes, error) {

	// addon 策略处理
	addonStrategyRouting, err := a.strategyAddon(params, addonSpec)
	if err != nil {
		return nil, err
	}
	logrus.Infof("finished addon strategry processing, addonStrategyRouting: %+v", addonStrategyRouting)
	if addonStrategyRouting != nil && addonStrategyRouting.ID != "" {
		// 返回可共享的 addon 实例
		return a.existAttachAddon(params, addonSpec, addonStrategyRouting)
	}

	// 未找到共享 addon 实例，真实创建
	logrus.Infof("failed to find shared addon, creating addon: %s, runtime: %s", params.AddonName, params.RuntimeID)
	// 不包含tag，addon名称改为addon名称
	if params.Workspace == string(apistructs.DevWorkspace) || params.Workspace == string(apistructs.TestWorkspace) {
		params.InstanceName = strings.Replace(params.AddonName, "terminus-", "", -1)
	}
	// 组装Instance数据
	addonIns, err := a.buildAddonInstance(addonSpec, params)
	if err != nil {
		return nil, err
	}
	// 组装instanceRouting
	addonInsRouting := a.buildAddonInstanceRouting(addonSpec, params, addonIns)
	// 组装attachment
	addonAttach := a.buildAddonAttachments(params, addonInsRouting)

	logrus.Infof("add addonInstance info to db, addon: %s, runtime: %s", addonIns.AddonName, addonIns.AppID)
	// 信息入库
	if err := a.db.CreateAddonInstance(addonIns); err != nil {
		return nil, err
	}
	logrus.Infof("add instanceRouting info to db, addon: %s, runtime: %s", addonInsRouting.AddonName, addonInsRouting.AppID)
	if err := a.db.CreateAddonInstanceRouting(addonInsRouting); err != nil {
		return nil, err
	}
	logrus.Infof("add addonAttachment info to db, instance: %s, runtime: %s", addonAttach.InstanceID, addonAttach.RuntimeID)
	if err := a.db.CreateAttachment(addonAttach); err != nil {
		return nil, err
	}
	// 开始创建addon
	go func() {
		logrus.Infof("sending addon creating request, addon: %s, runtime: %s", params.AddonName, params.RuntimeID)
		if err := a.createAddonResource(addonIns, addonInsRouting, addonSpec, addonDice, params); err != nil {
			logrus.Errorf("failed to create addon: %s, err: %+v", addonIns.ID, err)
			a.FailAndDelete(addonIns)
		}

		// prepare attachment for mysql account
		if a.prepareAttachment(addonInsRouting, addonAttach) && addonAttach.ID != 0 {
			if err := a.db.UpdateAttachment(addonAttach); err != nil {
				logrus.Errorf("update attachment failed: %+v", err)
			}
		}

		nodes := 0
		for _, s := range addonDice.Services {
			nodes += s.Deployments.Replicas
		}

		cpu := 0.0
		mem := 0
		config := ""
		if len(addonDice.Services) > 0 {
			for _, s := range addonDice.Services {
				cpu = s.Resources.CPU
				mem = s.Resources.Mem
				config = s.Envs["config"]
			}
		}
		if err := a.db.Create(&dbclient.AddonManagement{
			AddonID:     addonIns.ID,
			Name:        addonIns.Name,
			ProjectID:   addonIns.ProjectID,
			OrgID:       addonIns.OrgID,
			AddonConfig: config,
			CPU:         cpu,
			Mem:         uint64(mem),
			Nodes:       nodes,
			CreateTime:  time.Now(),
			UpdateTime:  time.Now(),
		}).Error; err != nil {
			logrus.Errorf("failed to create opsdb.AddonManagement: %v", err)
		}

	}()

	// TODO Name这里应该返回的是:老数据返回，新数据不返回？
	return &apistructs.AddonInstanceRes{
		InstanceID:     addonInsRouting.ID,
		AddonName:      addonInsRouting.AddonName,
		Name:           addonInsRouting.Name,
		Plan:           addonInsRouting.Plan,
		PlanCnName:     "",
		Version:        addonInsRouting.Version,
		Category:       addonInsRouting.Category,
		ProjectID:      addonInsRouting.ProjectID,
		OrgID:          addonInsRouting.OrgID,
		Env:            addonInsRouting.Workspace,
		Status:         addonInsRouting.Status,
		ShareScope:     addonInsRouting.ShareScope,
		LogoURL:        addonSpec.LogoUrl,
		Cluster:        addonInsRouting.Cluster,
		RealInstanceID: addonInsRouting.RealInstance,
	}, nil
}

//buildRealCreate 真实例创建判断
func (a *Addon) buildRealCreate(addonSpec *apistructs.AddonExtension, params *apistructs.AddonHandlerCreateItem) error {
	if len(params.Options) == 0 {
		return nil
	}
	if v, ok := params.Options["tag"]; ok {
		params.Tag = v
	}
	if v, ok := params.Options["shareScope"]; ok {
		params.ShareScope = v
	} else {
		params.ShareScope = addonSpec.ShareScopes[0]
	}
	tenantGroup := md5V(params.ProjectID + "_" + params.Workspace + "_" + params.ClusterName + conf.TenantGroupKey())
	resp, err := a.tenantSvc.CreateTenant(context.Background(), &pb.CreateTenantRequest{
		ProjectID:  params.ProjectID,
		TenantType: pb.Type_DOP.String(),
		Workspaces: []string{params.Workspace},
	})
	if err != nil {
		return err
	}
	if len(resp.Data) <= 0 {
		params.Options["tenantGroup"] = tenantGroup
	} else {
		params.Options["tenantGroup"] = resp.Data[0].Id
	}
	return nil
}

// existAttachAddon 已存在addonRouting信息，需要执行attachment操作，mysql还需要进行
func (a *Addon) existAttachAddon(params *apistructs.AddonHandlerCreateItem, addonSpec *apistructs.AddonExtension,
	existInstanceRouting *dbclient.AddonInstanceRouting) (*apistructs.AddonInstanceRes, error) {
	logrus.Infof("addonAttach existAttachAddon start: %+v", existInstanceRouting)
	// 能找到对应共享addon，查询attachments信息，看看是否关联了runtime
	attachments, err := a.db.GetByRuntimeIDAndRoutingInstanceID(params.RuntimeID, existInstanceRouting.ID)
	if err != nil {
		return nil, err
	}

	clusterInfo, err := a.clusterinfoImpl.Info(params.ClusterName)
	if err != nil {
		logrus.Errorf("existAttachAddon 获取cluster信息失败, %v", err)
		return nil, err
	}

	logrus.Info("existAttachAddon 查询attachments中是否已经关联")
	// 判断该routing是否关联了runtime
	var attachExist bool
	for _, v := range *attachments {
		if v.RoutingInstanceID == existInstanceRouting.ID {
			logrus.Info("existAttachAddon attachments中已经关联")
			attachExist = true
			break
		}
	}

	if !attachExist {
		logrus.Info("existAttachAddon 未关联，开始关联")
		addonAttach := a.buildAddonAttachments(params, existInstanceRouting)
		a.prepareAttachment(existInstanceRouting, addonAttach)
		if err := a.db.CreateAttachment(addonAttach); err != nil {
			return nil, err
		}
		// attachCount + 1
		existInstanceRouting.Reference = existInstanceRouting.Reference + 1
		if err := a.db.UpdateInstanceRouting(existInstanceRouting); err != nil {
			return nil, err
		}
	}

	addonIns, err := a.db.GetAddonInstance(existInstanceRouting.RealInstance)
	if err != nil {
		return nil, err
	}
	// needAttachInit mysql的init.sql、createDBs功能
	if existInstanceRouting.Status == string(apistructs.AddonAttached) && addonSpec.Name == string(apistructs.AddonMySQL) {
		logrus.Info("existAttachAddon needAttachInit")
		if addonIns.Config == "" {
			logrus.Errorf("existAttachAddon needAttachInit ：addon环境变量为空: %+v", *addonIns)
			return nil, errors.New("初始化mysql信息失败，环境变量不能为空")
		}
		var configMap map[string]interface{}
		err = json.Unmarshal([]byte(addonIns.Config), &configMap)
		if err != nil {
			logrus.Errorf("existAttachAddon needAttachInit Unmarshal error, addonIns Config: %+v", addonIns.Config)
			return nil, err
		}
		decPwd := configMap["MYSQL_PASSWORD"].(string)
		if addonIns.KmsKey != "" {
			decPwd, err = a.DecryptPassword(&addonIns.KmsKey, configMap["MYSQL_PASSWORD"].(string))
			if err != nil {
				logrus.Errorf("mysql password decript err, %v", err)
				return nil, err
			}
		} else {
			if _, ok := configMap[apistructs.AddonPasswordHasEncripy]; ok {
				decPwd, err = a.DecryptPassword(nil, configMap["MYSQL_PASSWORD"].(string))
				if err != nil {
					logrus.Errorf("mysql password decript err, %v", err)
					return nil, err
				}
			}
		}

		logrus.Infof("configMap port: %+v", configMap)
		existsMysqlExec := apistructs.ExistsMysqlExec{
			MysqlHost: configMap["MYSQL_HOST"].(string),
			MysqlPort: apistructs.AddonMysqlDefaultPort,
			Password:  decPwd,
			User:      configMap["MYSQL_USERNAME"].(string),
			Options:   params.Options,
		}
		createDbs, err := a.createDBs(nil, &existsMysqlExec, addonIns, "", "", &clusterInfo)
		if err != nil {
			logrus.Errorf("existAttachAddon needAttachInit createDbs error, %v", err)
			return nil, err
		}

		if err := a.initSqlFile(nil, &existsMysqlExec, addonIns, createDbs, "", "", &clusterInfo); err != nil {
			logrus.Errorf("existAttachAddon needAttachInit initSqlFile error, %v", err)
			return nil, err
		}
	}

	// provider addon 每次deploy都要重新提交一遍
	//a.providerAttachCreate(addonIns, addonSpec, params)

	return &apistructs.AddonInstanceRes{
		InstanceID:     existInstanceRouting.ID,
		AddonName:      existInstanceRouting.AddonName,
		Name:           existInstanceRouting.Name,
		Plan:           existInstanceRouting.Plan,
		PlanCnName:     "",
		Version:        existInstanceRouting.Version,
		Category:       existInstanceRouting.Category,
		ProjectID:      existInstanceRouting.ProjectID,
		OrgID:          existInstanceRouting.OrgID,
		Env:            existInstanceRouting.Workspace,
		Status:         existInstanceRouting.Status,
		ShareScope:     existInstanceRouting.ShareScope,
		LogoURL:        addonSpec.LogoUrl,
		Cluster:        existInstanceRouting.Cluster,
		RealInstanceID: existInstanceRouting.RealInstance,
	}, nil
}

// strategyAddon addon策略部署
func (a *Addon) strategyAddon(params *apistructs.AddonHandlerCreateItem,
	addonSpec *apistructs.AddonExtension) (*dbclient.AddonInstanceRouting, error) {
	// 针对真实创建策略处理
	if err := a.buildRealCreate(addonSpec, params); err != nil {
		return nil, err
	}
	logrus.Infof("after buildRealCreate, param:  %+v", *params)
	// 根据tag查询addon实例信息
	shareRoutingIns, err := a.getTagInstance(addonSpec, params)
	if err != nil {
		return nil, err
	}
	if shareRoutingIns != nil && shareRoutingIns.ID != "" {
		return shareRoutingIns, nil
	}
	if len(params.Options) > 0 {
		if v, ok := params.Options["use_default"]; ok {
			if v == "false" {
				return nil, nil
			}
		}
	}
	// 根据name查询addon实例信息
	shareRoutingIns, err = a.getShareInstance(addonSpec, params)
	if err != nil {
		return nil, err
	}
	logrus.Infof("addonAttach getShareInstance finish ,shareRoutingIns :  %+v", *shareRoutingIns)
	// 判断如果是微服务或者通用能力，不为nil，直接返回
	if shareRoutingIns != nil && shareRoutingIns.ID != "" && shareRoutingIns.PlatformServiceType != apistructs.PlatformServiceTypeBasic {
		return shareRoutingIns, nil
	}
	// 判断基础addon，如果版本相同，则返回
	if shareRoutingIns != nil && shareRoutingIns.ID != "" && shareRoutingIns.Version == addonSpec.Version {
		return shareRoutingIns, nil
	}
	// addon策略筛选
	// canal不需要走策略，canal就是要一个给一个
	if params.AddonName == apistructs.AddonCanal {
		return nil, nil
	}
	// custom addon不需要走策略
	if addonSpec.Category == apistructs.AddonCustomCategory {
		return nil, nil
	}
	var (
		exitAddonInsRouting *[]dbclient.AddonInstanceRouting
	)
	// 策略，除了redis，其他的plan在dev、test环境都变为basic
	a.strategyPlan(params)
	// 根据 sharescope查询出共享信息
	exitAddonInsRouting, err = a.db.GetRoutingInstancesBySimilar([]string{addonSpec.Name}, params)
	if err != nil {
		return nil, err
	}
	// 查询结果如果为null，则返回nil
	if len(*exitAddonInsRouting) == 0 {
		return nil, nil
	}
	var resultInsRouting dbclient.AddonInstanceRouting
	for _, value := range *exitAddonInsRouting {
		if value.Version == addonSpec.Version {
			// redis的basic和professional返回参数不一样，所以需要单独判断
			if params.AddonName == apistructs.AddonRedis && value.Plan != params.Plan {
				continue
			}
			resultInsRouting = value
			break
		}
	}
	return &resultInsRouting, nil
}

// getTagInstance 根据tag获取addon实例信息
func (a *Addon) getTagInstance(addonSpec *apistructs.AddonExtension, params *apistructs.AddonHandlerCreateItem) (*dbclient.AddonInstanceRouting, error) {
	l := logrus.WithField("func", "*Addon.getTagInstance")
	if params.ShareScope == "" {
		params.ShareScope = addonSpec.ShareScopes[0]
	}
	if len(addonSpec.Similar) == 0 {
		addonSpec.Similar = []string{addonSpec.Name}
	}
	l.Debugf("addonSpec.Similar: %v", addonSpec.Similar)
	routingList, err := a.db.GetRoutingInstancesBySimilar(addonSpec.Similar, params)
	if err != nil {
		return nil, err
	}
	list := addonInstanceRoutingList(*routingList)
	if item, ok := list.GetByName(params.InstanceName); ok {
		l.Debugf("find routingInstance by name, name: %s, routingInstance: %+v", item.Name, item)
		return item, nil
	}
	if item, ok := list.GetByTag(params.Tag); ok {
		l.Debugf("find routingInstance by tag, tag: %s, routingInstance: %+v", item.Tag, item)
		return item, nil
	}

	l.WithField("name", params.InstanceName).WithField("tag", params.Tag).Debugln("routingInstance not found")
	return nil, nil
}

// strategyPlan 除了redis, es，其余的addon，在开发、测试环境，都指定为basic规格
func (a *Addon) strategyPlan(params *apistructs.AddonHandlerCreateItem) {}

// TODO 放到prebuild逻辑
// checkCreatePrecondition 创建addon时，判断addon的requires属性是否支持创建
func (a *Addon) checkCreatePrecondition(params *apistructs.AddonHandlerCreateItem, addonSpec *apistructs.AddonExtension) (bool, error) {
	// 如果非基础addon，直接返回(微服务、通用能力等)
	if addonSpec.SubCategory != apistructs.BasicAddon {
		return true, nil
	}
	minShareScope := addonSpec.ShareScopes[0]
	var minId string
	switch minShareScope {
	case apistructs.ProjectShareScope:
		minId = params.ProjectID
	case apistructs.OrgShareScope:
		minId = params.OrgID
	}
	if minId == "" {
		return false, errors.New("找不到ProjectID或者OrgID")
	}

	var hasManyPerApp = false
	// 判断是否有many_per_app属性
	for _, requ := range addonSpec.Requires {
		if requ == "many_per_app" {
			hasManyPerApp = true
			break
		}
	}
	if !hasManyPerApp {
		runtime, err := strconv.ParseUint(params.RuntimeID, 10, 64)
		if err != nil {
			return false, err
		}
		addonPrebuilds, err := a.db.GetPreBuildsByRuntimeID(runtime)
		if err != nil {
			return false, err
		}
		for _, value := range *addonPrebuilds {
			if value.DeleteStatus != apistructs.AddonPrebuildNotDeleted {
				continue
			}
			if value.AddonName == params.AddonName {
				return false, nil
			}
		}
	}

	return true, nil

}

// 获取对应共享级别的Id
func (a *Addon) getMinShareId(params *apistructs.AddonHandlerCreateItem, addonSpec *apistructs.AddonExtension) (string, error) {
	minShareScope := addonSpec.ShareScopes[0]
	var minId string
	switch minShareScope {
	case apistructs.ProjectShareScope:
		minId = params.ProjectID
	case apistructs.OrgShareScope:
		minId = params.OrgID
	}
	if minId == "" {
		return "", errors.New("找不到ProjectID或者OrgID")
	}

	return minId, nil
}

// getShareInstance 根据创建参数，查询是否有匹配的addon实例信息
func (a *Addon) getShareInstance(addonSpec *apistructs.AddonExtension, params *apistructs.AddonHandlerCreateItem) (*dbclient.AddonInstanceRouting, error) {
	// 判断该addon支持的shareScope中，有没有匹配的addonInstance信息，有即可返回
	var existRoutingIns dbclient.AddonInstanceRouting
	for _, scope := range addonSpec.ShareScopes {
		switch scope {
		case apistructs.ClusterShareScope:
			routingList, err := a.db.GetRoutingInstancesBySimilar([]string{addonSpec.Name}, params)
			if err != nil {
				return nil, errors.New("查询Add-on信息失败")
			}
			if len(*routingList) > 0 {
				existRoutingIns = (*routingList)[0]
			}
		case apistructs.ApplicationShareScope:
			routingList, err := a.db.GetRoutingInstancesBySimilar([]string{addonSpec.Name}, params)
			if err != nil {
				return nil, errors.New("查询Add-on信息失败")
			}
			if len(*routingList) > 0 {
				existRoutingIns = (*routingList)[0]
			}
		case apistructs.ProjectShareScope:
			routingList, err := a.db.GetAddonInstanceRoutingByOrgAndAddonName(params.OrgID, params.ClusterName, params.AddonName, params.Workspace, scope)
			if err != nil {
				return nil, errors.New("查询Add-on信息失败")
			}
			for _, routingIns := range *routingList {
				// addonSpec.SubCategory不为空，表示的是基础addon；为空，表示的是provider addon
				// 判断是否基础addon && 参数名称是否匹配
				if addonSpec.SubCategory == apistructs.BasicAddon && routingIns.Name != params.InstanceName {
					continue
				}
				//if routingIns.InsideAddon == apistructs.INSIDE {
				//	continue
				//}
				if routingIns.ProjectID != params.ProjectID {
					continue
				}
				// redis的basic和professional返回参数不一样，所以需要单独判断
				if params.AddonName == apistructs.AddonRedis && routingIns.Plan != params.Plan {
					continue
				}
				existRoutingIns = routingIns
				break
			}
		case apistructs.OrgShareScope:
			routingList, err := a.db.GetAddonInstanceRoutingByOrgAndAddonName(params.OrgID, params.ClusterName, params.AddonName, params.Workspace, scope)
			if err != nil {
				return nil, errors.New("查询Add-on信息失败")
			}
			for _, routingIns := range *routingList {
				// addonSpec.SubCategory不为空，表示的是基础addon；为空，表示的是provider addon
				// 判断是否基础addon && 参数名称是否匹配
				if addonSpec.SubCategory == apistructs.BasicAddon && routingIns.Name != params.InstanceName {
					continue
				}
				//if routingIns.InsideAddon == apistructs.INSIDE {
				//	continue
				//}

				// redis的basic和professional返回参数不一样，所以需要单独判断
				if params.AddonName == apistructs.AddonRedis && routingIns.Plan != params.Plan {
					continue
				}
				existRoutingIns = routingIns
				break
			}
		}
	}
	return &existRoutingIns, nil
}

// CreateAddonResource 创建addon资源
func (a *Addon) createAddonResource(addonIns *dbclient.AddonInstance, addonInsRouting *dbclient.AddonInstanceRouting,
	addonSpec *apistructs.AddonExtension, addonDice *diceyml.Object, params *apistructs.AddonHandlerCreateItem) error {
	// 自定义addon直接save数据，返回
	switch addonSpec.Category {
	case apistructs.AddonCustomCategory:
		if err := a.customDeploy(addonIns, addonInsRouting, params); err != nil {
			a.pushLog(fmt.Sprintf("error when addon is released, %v", err), params)
			return err
		}
		return nil
	default:
		// 内部addon发布
		if err := a.insideAddonAttach(addonIns, addonSpec, addonDice, params); err != nil {
			logrus.Errorf("createAddonResource insideAddonAttach err: %v", err)
			if err := a.FailAndDelete(addonIns); err != nil {
				return err
			}
			return err
		}
		if addonSpec.SubCategory == apistructs.BasicAddon {
			// TODO: get vendor from cluster or config
			if err := a.basicAddonDeploy(addonIns, addonInsRouting, params, addonSpec, addonDice, apistructs.ECIVendorAlibaba); err != nil {
				a.pushLog(fmt.Sprintf("error when addon is released, %v", err), params)
				logrus.Errorf("error when addon is released, %v", err)
				if err := a.FailAndDelete(addonIns); err != nil {
					return err
				}
			}
		} else {
			if err := a.providerAddonDeploy(addonIns, addonInsRouting, params, addonSpec); err != nil {
				a.ExportLogInfo(apistructs.ErrorLevel, apistructs.AddonError, addonIns.ID, addonIns.ID+"-callprovider", i18n.OrgSprintf(addonIns.OrgID, "FailedToCreateAddonByProvider"),
					params.AddonName, params.InstanceName, err)
				a.pushLog(fmt.Sprintf("(%v)", err), params)
				return err
			}
		}
	}
	return nil
}

// insideAddonAttach 内部addon发布流程
func (a *Addon) insideAddonAttach(addonIns *dbclient.AddonInstance, addonSpec *apistructs.AddonExtension, addonDice *diceyml.Object,
	params *apistructs.AddonHandlerCreateItem) error {
	// 发布内部addon
	insideIds, err := a.insideAddonDeploy(addonIns, addonSpec, addonDice, params)
	if err != nil {
		logrus.Errorf("insideAddonAttach insideAddonDeploy err : %v", err)
		a.retrieveInsideAddon(addonIns)
		return err
	}
	if insideIds == nil || len(*insideIds) == 0 { // 无内部 addon 依赖
		return nil
	}

	// 内部addon状态同步
	insideAddonIns, err := a.waitInsideAddonDeploy(insideIds)
	if err != nil {
		logrus.Errorf("insideAddonAttach waitInsideAddonDeploy err : %v", err)
		if err := a.retrieveInsideAddon(addonIns); err != nil {
			logrus.Error("waitInsideAddonDeploy deploy fail")
		}
		return err
	}

	// 发布成功后，在外部addon options中，把内部addon的instanceId填充进去
	var insOptions map[string]string
	if addonIns.Options != "" {
		if insideAddonIns == nil {
			return nil
		}
		if err := json.Unmarshal([]byte(addonIns.Options), &insOptions); err != nil {
			return err
		}
	}
	if len(insOptions) == 0 {
		insOptions = map[string]string{}
	}
	for k, v := range *insideAddonIns {
		insOptions[v] = k
	}
	options, _ := json.Marshal(insOptions)
	addonIns.Options = string(options)

	return nil
}

// providerAddonDeploy provider addon发布
func (a *Addon) providerAddonDeploy(addonIns *dbclient.AddonInstance, addonInsRouting *dbclient.AddonInstanceRouting,
	params *apistructs.AddonHandlerCreateItem, addonSpec *apistructs.AddonExtension) error {
	// 走http domain的方式
	addonProviderRequest := apistructs.AddonProviderRequest{
		UUID:        addonIns.ID,
		Plan:        addonIns.Plan,
		ClusterName: addonIns.Cluster,
		Options:     params.Options,
	}
	statusCode, providerResponse, err := a.CreateAddonProvider(&addonProviderRequest, addonSpec.Name, addonSpec.Domain, params.OperatorID)
	if err != nil || (statusCode != 200 && statusCode != 202) {
		deleteProviderResp, delErr := a.DeleteAddonProvider(&addonProviderRequest, addonIns.ID, addonSpec.Name, addonSpec.Domain)
		if delErr != nil {
			logrus.Errorf("provider addon delete fail: %v", delErr)
		}
		if deleteProviderResp == nil {
			logrus.Error("provider addon , delete action, response fail")
		}

		if err := a.UpdateAddonStatus(addonIns, apistructs.AddonAttachFail); err != nil {
			return err
		}

		return err
	}
	// leave it to the callback if deploying
	if statusCode == 202 || providerResponse != nil && providerResponse.Data.Status == "INIT" {
		return nil
	}
	if len(providerResponse.Data.Config) > 0 {
		configBytes, err := json.Marshal(providerResponse.Data.Config)
		if err != nil {
			// 失败后更新状态
			if err := a.UpdateAddonStatus(addonIns, apistructs.AddonAttachFail); err != nil {
				return err
			}
			return err
		}
		addonIns.Config = string(configBytes)
	}
	if len(providerResponse.Data.Label) > 0 {
		labelBytes, err := json.Marshal(providerResponse.Data.Label)
		if err != nil {
			// 失败后更新状态
			if err := a.UpdateAddonStatus(addonIns, apistructs.AddonAttachFail); err != nil {
				return err
			}
			return err
		}
		addonIns.Label = string(labelBytes)
	}
	// 成功后更新状态
	if err := a.UpdateAddonStatus(addonIns, apistructs.AddonAttached); err != nil {
		return err
	}

	return nil
}

// basicAddonDeploy 基础addon发布
func (a *Addon) basicAddonDeploy(addonIns *dbclient.AddonInstance, addonInsRouting *dbclient.AddonInstanceRouting,
	params *apistructs.AddonHandlerCreateItem, addonSpec *apistructs.AddonExtension, addonDice *diceyml.Object, vendor string) error {
	if err := a.preCheck(params); err != nil {
		return err
	}

	// 构建 addon 创建请求
	projID, _ := strconv.ParseUint(addonIns.ProjectID, 10, 64)
	orgID, _ := strconv.ParseUint(addonIns.OrgID, 10, 64)
	logrus.Infof("params is %+v", *params)
	logrus.Infof("params is %#v", *params)
	projectECI := utils.IsProjectECIEnable(a.bdl, projID, addonIns.Workspace, orgID, params.OperatorID)
	if projectECI {
		if params.Options == nil {
			params.Options = make(map[string]string)
		}
		switch vendor {
		case apistructs.ECIVendorAlibaba:
			// Alicloud ECI
			if _, ok := params.Options[apistructs.AlibabaECILabel]; !ok {
				params.Options[apistructs.AlibabaECILabel] = "true"
			}

		case apistructs.ECIVendorHuawei:
			// Huawei CCI
			if _, ok := params.Options[apistructs.HuaweiCCILabel]; !ok {
				params.Options[apistructs.HuaweiCCILabel] = "force"
			}

		case apistructs.ECIVendorTecent:
			// Tencent EKSCI
			if _, ok := params.Options[apistructs.TecentEKSNodeSelectorKey]; !ok {
				params.Options[apistructs.TecentEKSNodeSelectorKey] = apistructs.TecentEKSNodeSelectorValue
			}

		default:
			// Alicloud ECI
			if _, ok := params.Options[apistructs.AlibabaECILabel]; !ok {
				params.Options[apistructs.AlibabaECILabel] = "true"
			}
		}

		if _, ok := params.Options[diceyml.AddonDiskType]; !ok {
			params.Options[diceyml.AddonDiskType] = apistructs.VolumeTypeSSD
		}

		if _, ok := params.Options[diceyml.AddonVolumeSize]; !ok {
			params.Options[diceyml.AddonVolumeSize] = strconv.Itoa(int(diceyml.ProjectECIVolumeDefaultSize))
		}

		if _, ok := params.Options[diceyml.AddonSnapMaxHistory]; !ok {
			params.Options[diceyml.AddonSnapMaxHistory] = "0"
		}
	}

	addonCreateReq, err := a.BuildAddonRequestGroup(params, addonIns, addonSpec, addonDice)
	if err != nil || addonCreateReq == nil {
		logrus.Errorf("failed to build addon creating request body, addon: %v, err: %v", addonIns.ID, err)
		a.FailAndDelete(addonIns)
		return err
	}
	logrus.Infof("sending addon creating request, request body: %+v", *addonCreateReq)

	// 请求调度器
	_, err = a.serviceGroupImpl.Create(*addonCreateReq)
	if err != nil {
		a.ExportLogInfo(apistructs.ErrorLevel, apistructs.AddonError, addonIns.ID, addonIns.ID+"-internal", i18n.OrgSprintf(addonIns.OrgID, "FailedToCreateAddonByScheduler"), err)
		logrus.Errorf("failed to create addon %s, instance id %v from scheduler, %v", addonSpec.Name, addonIns.ID, err)
		return err
	}

	// 循环拉去addon状态
	if err := a.GetAddonResourceStatus(addonIns, addonInsRouting, addonDice, addonSpec); err != nil {
		// 如果失败了，及时删除addon信息
		if err := a.FailAndDelete(addonIns); err != nil {
			return err
		}
		return err
	}

	// after deployed
	// init first mysql account
	if err := a.InitMySQLAccount(addonIns, addonInsRouting, params.OperatorID); err != nil {
		return err
	}

	return nil
}

// preCheck the production environment does not allow the deployment of basic addon
func (a *Addon) preCheck(params *apistructs.AddonHandlerCreateItem) error {
	if params.Plan == "basic" && params.Workspace == "PROD" {
		return fmt.Errorf("failed to create addon, the production environment does not allow the deployment of basic addon")
	}
	return nil
}

// custom addon 发布
func (a *Addon) customDeploy(addonIns *dbclient.AddonInstance, addonInsRouting *dbclient.AddonInstanceRouting,
	params *apistructs.AddonHandlerCreateItem) error {
	if len(addonIns.Config) == 0 {
		a.ExportLogInfo(apistructs.ErrorLevel, apistructs.RuntimeError, params.RuntimeID, params.RuntimeID, i18n.OrgSprintf(addonIns.OrgID, "CustomAddonDoseNotExist"),
			addonInsRouting.AddonName, addonInsRouting.Name)
		return errors.Errorf("custom addon should be created before being referenced, addon name: %s, instance name: %s", addonInsRouting.AddonName, addonInsRouting.Name)
	}
	addonAudit := dbclient.AddonAudit{
		OrgID:     addonIns.OrgID,
		ProjectID: addonIns.ProjectID,
		Workspace: addonIns.Workspace,
		Operator:  params.OperatorID,
		AddonName: params.AddonName,
		OpName:    apistructs.AddCustomAddon,
		InsID:     addonIns.ID,
		InsName:   addonIns.Name,
		Params:    addonIns.Config,
	}
	if err := a.db.CreateAddonAudit(addonAudit); err != nil {
		return err
	}
	// 更新addon表状态
	if err := a.UpdateAddonStatus(addonIns, apistructs.AddonAttached); err != nil {
		return err
	}
	return nil
}

// buildAddonInstance addonInstance数据装填
func (a *Addon) buildAddonInstance(addonSpec *apistructs.AddonExtension, params *apistructs.AddonHandlerCreateItem) (*dbclient.AddonInstance, error) {
	addonIns := dbclient.AddonInstance{
		ID:                  a.getRandomId(),
		Name:                params.InstanceName,
		AddonName:           params.AddonName,
		Category:            addonSpec.Category,
		Plan:                params.Plan,
		Version:             addonSpec.Version,
		OrgID:               params.OrgID,
		ProjectID:           params.ProjectID,
		Workspace:           params.Workspace,
		Status:              string(apistructs.AddonAttaching),
		ShareScope:          params.ShareScope,
		Cluster:             params.ClusterName,
		ApplicationID:       params.ApplicationID,
		AppID:               params.ProjectID,
		PlatformServiceType: 0,
		Deleted:             apistructs.AddonNotDeleted,
		CreatedAt:           time.Now(),
		UpdatedAt:           time.Now(),
	}
	if addonIns.Name == "" {
		addonIns.Name = addonSpec.Name + "-" + addonIns.ID
	}
	if addonSpec.SubCategory == apistructs.MicroAddon {
		addonIns.PlatformServiceType = 1
	}
	if addonSpec.SubCategory == apistructs.AbilityAddon {
		addonIns.PlatformServiceType = 2
	}
	addonIns.Namespace = strings.Join([]string{"addon", strings.Replace(addonSpec.Name, "terminus-", "", 1)}, "-")
	addonIns.ScheduleName = addonIns.ID
	if len(params.Options) > 0 {
		options, _ := json.Marshal(params.Options)
		addonIns.Options = string(options)
	}
	if len(params.Config) > 0 {
		configs, _ := json.Marshal(params.Config)
		addonIns.Config = string(configs)
	}

	kmsKey, err := a.bdl.KMSCreateKey(apistructs.KMSCreateKeyRequest{
		CreateKeyRequest: kmstypes.CreateKeyRequest{
			PluginKind: kmstypes.PluginKind_DICE_KMS,
		},
	})
	if err != nil {
		return nil, err
	}
	addonIns.KmsKey = kmsKey.KeyMetadata.KeyID

	return &addonIns, nil
}

// buildAddonInstance addonInstanceRouting数据装填
func (a *Addon) buildAddonInstanceRouting(addonSpec *apistructs.AddonExtension,
	params *apistructs.AddonHandlerCreateItem,
	addonIns *dbclient.AddonInstance, reference ...int) *dbclient.AddonInstanceRouting {

	if params.InsideAddon == "" {
		params.InsideAddon = apistructs.NOT_INSIDE
	}
	ref := 1
	if len(reference) > 0 {
		ref = reference[0]
	}

	addonInsRout := dbclient.AddonInstanceRouting{
		ID:                  a.getRandomId(),
		Name:                params.InstanceName,
		AddonName:           params.AddonName,
		Category:            addonSpec.Category,
		Plan:                params.Plan,
		Version:             addonSpec.Version,
		OrgID:               params.OrgID,
		ProjectID:           params.ProjectID,
		Workspace:           params.Workspace,
		Status:              string(apistructs.AddonAttaching),
		ShareScope:          params.ShareScope,
		Cluster:             params.ClusterName,
		ApplicationID:       params.ApplicationID,
		AppID:               params.ProjectID,
		RealInstance:        addonIns.ID,
		Reference:           ref,
		IsPlatform:          false,
		PlatformServiceType: 0,
		Deleted:             apistructs.AddonNotDeleted,
		InsideAddon:         params.InsideAddon,
		Tag:                 params.Tag,
		CreatedAt:           time.Now(),
		UpdatedAt:           time.Now(),
	}
	if addonInsRout.Name == "" {
		addonInsRout.Name = addonSpec.Name
	}
	//if params.InsideAddon == apistructs.INSIDE {
	//	addonInsRout.InsideAddon = params.InsideAddon
	//}
	if addonSpec.SubCategory != apistructs.BasicAddon {
		addonInsRout.IsPlatform = true
	}
	if addonSpec.SubCategory == apistructs.MicroAddon {
		addonInsRout.PlatformServiceType = 1
	}
	if addonSpec.SubCategory == apistructs.AbilityAddon {
		addonInsRout.PlatformServiceType = 2
	}
	if len(params.Options) > 0 {
		options, _ := json.Marshal(params.Options)
		addonInsRout.Options = string(options)
	}
	return &addonInsRout
}

// buildAddonAttachments AddonAttachment数据装填
func (a *Addon) buildAddonAttachments(params *apistructs.AddonHandlerCreateItem, addonInsRouting *dbclient.AddonInstanceRouting) *dbclient.AddonAttachment {
	addonAttachment := dbclient.AddonAttachment{
		InstanceID:        addonInsRouting.RealInstance,
		RoutingInstanceID: addonInsRouting.ID,
		OrgID:             params.OrgID,
		ProjectID:         params.ProjectID,
		ApplicationID:     params.ApplicationID,
		RuntimeID:         params.RuntimeID,
		InsideAddon:       apistructs.NOT_INSIDE,
		RuntimeName:       params.RuntimeName,
		Deleted:           apistructs.AddonNotDeleted,
		CreatedAt:         time.Now(),
		UpdatedAt:         time.Now(),
	}
	if params.InsideAddon == apistructs.INSIDE {
		addonAttachment.InsideAddon = params.InsideAddon
	}

	return &addonAttachment
}

// GetRandomId 生成随机33位uuid，并且，（首字母开头 + 32位uuid）构成Id
func (a *Addon) getRandomId() string {
	str := "abcdefghijklmnopqrstuvwxyz"
	bytes := []byte(str)
	result := []byte{}
	r := rand.New(rand.NewSource(time.Now().UnixNano()))
	for i := 0; i < 1; i++ {
		result = append(result, bytes[r.Intn(len(bytes))])
	}
	return strings.Join([]string{string(result), uuid.UUID()}, "")
}

// CreateAddonProvider 请求addon provider，获取新的addon实例
func (a *Addon) CreateAddonProvider(req *apistructs.AddonProviderRequest, addonName, providerDomain, userId string) (int, *apistructs.AddonProviderResponse, error) {
	if strings.Contains(providerDomain, "tmc.") {
		providerDomain = discover.MSP()
	}
	req.Callback = "http://" + discover.Orchestrator()
	logrus.Infof("start creating addon provider, url: %v, body: %+v", providerDomain+"/"+addonName+apistructs.AddonGetResourcePath, req)

	var resp apistructs.AddonProviderResponse
	hc := a.hc
	r, err := hc.Post(providerDomain).
		Path("/"+addonName+apistructs.AddonGetResourcePath).
		Header("USER-ID", userId).
		JSONBody(req).
		Do().
		JSON(&resp)
	if err != nil {
		return 0, nil, apierrors.ErrInvoke.InternalError(err)
	}

	logrus.Infof("provider response http : %+v", r)
	logrus.Infof("provider response info : %+v", resp)

	if !r.IsOK() || !resp.Success {
		logrus.Errorf("provider response statuscode : %v", r.StatusCode())
		logrus.Errorf("provider response err : %+v", r)
		logrus.Errorf("provider response : %+v", resp)
		a.pushLogCore(fmt.Sprintf("err when deploy addon: %s, err: %s", addonName, resp.Error.Msg), req.Options)
		return 0, nil, apierrors.ErrInvoke.InternalError(
			fmt.Errorf("create provider addon, response fail, code:%v, msg:%v",
				resp.Error.Code, resp.Error.Msg))
	}
	return r.StatusCode(), &resp, nil
}

// DeleteAddonProvider 删除addon provideraddon实例
func (a *Addon) DeleteAddonProvider(req *apistructs.AddonProviderRequest, uuid, addonName, providerDomain string) (*apistructs.AddonProviderDeleteResponse, error) {
	logrus.Infof("deleting addon provider request: %+v", req)

	if strings.Contains(providerDomain, "pandora.") {
		providerDomain = strings.Replace(providerDomain, "pandora.marathon.l4lb.thisdcos.directory:8050", "pandora.default.svc.cluster.local:8050", -1)
	} else if strings.Contains(providerDomain, "tmc.") {
		providerDomain = discover.MSP()
		//providerDomain = strings.Replace(providerDomain, "tmc.marathon.l4lb.thisdcos.directory:8050", "tmc.default.svc.cluster.local:8050", -1)
	}
	logrus.Infof("start delete addon provider, url: %v", providerDomain+"/"+addonName+apistructs.AddonGetResourcePath+"/"+uuid)

	var resp apistructs.AddonProviderDeleteResponse
	hc := a.hc
	r, err := hc.Delete(providerDomain).
		Path("/" + addonName + apistructs.AddonGetResourcePath + "/" + uuid).
		JSONBody(req).
		Do().
		JSON(&resp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	logrus.Infof("delete addon provider, resp: %+v", resp)
	if !r.IsOK() || !resp.Success {
		return nil, apierrors.ErrInvoke.InternalError(errors.New("delete provider addon, response fail"))
	}
	return &resp, nil
}

// FindNeedCreateAddon 判断是否需要真实创建addon
func (a *Addon) FindNeedCreateAddon(params *apistructs.AddonHandlerCreateItem) (*dbclient.AddonInstanceRouting, error) {
	addonSpec, _, err := a.GetAddonExtention(params)
	if err != nil {
		logrus.Errorf("AttachAndCreate GetAddonExtention err:  %v", err)
		return nil, err
	}
	// addon 策略处理
	addonStrategyRouting, err := a.strategyAddon(params, addonSpec)
	if err != nil {
		return nil, err
	}
	return addonStrategyRouting, nil
}

func (a *Addon) pushLog(content string, params *apistructs.AddonHandlerCreateItem) {
	a.pushLogCore(content, params.Options)
}

func (a *Addon) pushLogCore(content string, params map[string]string) {
	deploymentId, ok := params["deploymentId"]
	if !ok {
		return
	}
	logHelper := &log.DeployLogHelper{
		DeploymentID: deploymentId,
		Bdl:          a.bdl,
	}
	tags := map[string]string{}
	if orgName, ok := params["orgName"]; ok {
		tags[log.TAG_ORG_NAME] = orgName
	}
	logHelper.Log(content, tags)
}

type addonInstanceRoutingList []dbclient.AddonInstanceRouting

func (l addonInstanceRoutingList) sort() {
	f := func(i, j int) bool {
		if strings.EqualFold(l[i].Category, apistructs.CUSTOM_TYPE_CUSTOM) && !strings.EqualFold(l[j].Category, apistructs.CUSTOM_TYPE_CUSTOM) {
			return true
		}
		if !strings.EqualFold(l[i].Category, apistructs.CUSTOM_TYPE_CUSTOM) && strings.EqualFold(l[j].Category, apistructs.CUSTOM_TYPE_CUSTOM) {
			return false
		}
		return l[i].Name < l[j].Name
	}
	if ok := sort.SliceIsSorted(l, f); !ok {
		sort.Slice(l, f)
	}
}

func (l addonInstanceRoutingList) GetByName(name string) (*dbclient.AddonInstanceRouting, bool) {
	l.sort()
	for i := range l {
		if l[i].Name == name {
			return &l[i], true
		}
	}
	return nil, false
}

func (l addonInstanceRoutingList) GetByTag(tag string) (*dbclient.AddonInstanceRouting, bool) {
	if tag == "" {
		return nil, false
	}
	l.sort()
	for i := range l {
		if l[i].Tag == tag {
			return &l[i], true
		}
	}
	return nil, false
}
