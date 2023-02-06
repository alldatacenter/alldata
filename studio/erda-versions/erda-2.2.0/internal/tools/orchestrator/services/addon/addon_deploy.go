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
	"strings"
	"time"

	"github.com/mcuadros/go-version"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/conf"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/internal/tools/orchestrator/i18n"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/mysqlhelper"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/strutil"
)

// GetAddonResourceStatus 从scheduler获取addon发布状态
func (a *Addon) GetAddonResourceStatus(addonIns *dbclient.AddonInstance,
	addonInsRouting *dbclient.AddonInstanceRouting,
	addonDice *diceyml.Object, addonSpec *apistructs.AddonExtension) error {
	startTime := time.Now().Unix()
	for {
		// sleep 10秒，继续请求
		logrus.Infof("polling addon: %v status until it's healthy", addonIns.ID)
		if time.Now().Unix()-startTime > (conf.RuntimeUpMaxWaitTime() * 60) {
			a.ExportLogInfo(apistructs.ErrorLevel, apistructs.AddonError, addonIns.ID, addonIns.ID+"-deploytimeout", i18n.OrgSprintf(addonIns.OrgID, "WaitingForAddonTimeOut"),
				addonSpec.Name, addonIns.ID, conf.RuntimeUpMaxWaitTime())
			logrus.Errorf("polling addon: %v status timeout(max: %d s)", addonIns.ID, apistructs.RuntimeUpMaxWaitTime)
			break
		}

		// 获取addon状态
		//serviceGroup, err := a.bdl.InspectServiceGroup(addonIns.Namespace, addonIns.ScheduleName)
		serviceGroup, err := a.serviceGroupImpl.Info(context.Background(), addonIns.Namespace, addonIns.ScheduleName)
		if err != nil {
			logrus.Errorf("拉取schedule状态接口失败, error: %v", err)
			time.Sleep(10 * time.Second)
			continue
		}
		// 如果状态是ready或者healthy，说明服务已经发起来了
		if serviceGroup.Status == apistructs.StatusReady || serviceGroup.Status == apistructs.StatusHealthy {
			a.ExportLogInfo(apistructs.SuccessLevel, apistructs.AddonError, addonIns.ID, addonIns.ID, i18n.OrgSprintf(addonIns.OrgID, "AddonIsHealthy"),
				addonIns.AddonName, addonIns.ID)
			logrus.Infof("addon: %v is healthy!", addonIns.ID)
			clusterInfo, err := a.clusterinfoImpl.Info(addonInsRouting.Cluster)
			if err != nil {
				logrus.Errorf("拉取cluster接口失败, error: %v", err)
				return err
			}

			configMap := map[string]string{}
			switch addonIns.AddonName {
			case apistructs.AddonMySQL:
				configMap, err = a.MySQLDeployStatus(addonIns, &serviceGroup, &clusterInfo)
			case apistructs.AddonCanal:
				configMap, err = a.CanalDeployStatus(addonIns, &serviceGroup)
			case apistructs.AddonES:
				configMap, err = a.EsDeployStatus(addonIns, &serviceGroup)
			case apistructs.AddonKafka:
				configMap, err = a.KafkaDeployStatus(addonIns, &serviceGroup, &clusterInfo)
			case apistructs.AddonRocketMQ:
				configMap, err = a.RocketDeployStatus(addonIns, &serviceGroup)
			case apistructs.AddonRedis:
				configMap, err = a.RedisDeployStatus(addonIns, &serviceGroup)
			case apistructs.AddonRabbitMQ:
				configMap, err = a.RabbitmqDeployStatus(addonIns, &serviceGroup)
			case apistructs.AddonZookeeper:
				configMap, err = a.ZookeeperDeployStatus(addonIns, &serviceGroup)
			case apistructs.AddonApacheZookeeper:
				configMap, err = a.ZookeeperDeployStatus(addonIns, &serviceGroup)
			case apistructs.AddonConsul:
				configMap, err = a.ConsulDeployStatus(addonIns, &serviceGroup)
			case apistructs.AddonSourcecov:
				asm := &SourcecovAddonManagement{bdl: a.bdl}
				configMap, err = asm.DeployStatus(addonIns, &serviceGroup)
			default:
				// 非基础addon，走通用的处理逻辑
				configMap, err = a.CommonDeployStatus(addonIns, &serviceGroup, addonDice, addonSpec)
			}
			if err != nil {
				logrus.Errorf("fetch addon %s configMap fail: %v", addonIns.ID, err)
				return err
			}

			optionStr, err := json.Marshal(configMap)
			if err != nil {
				return err
			}
			addonIns.Config = string(optionStr)
			addonIns.Status = string(apistructs.AddonAttached)
			if err := a.db.UpdateAddonInstance(addonIns); err != nil {
				return err
			}

			addonInsRouting.Status = string(apistructs.AddonAttached)
			if err := a.db.UpdateInstanceRouting(addonInsRouting); err != nil {
				return err
			}
			return nil
		} else {
			// 还未健康, 查询 podlist, 以返回具体信息
			podList, err := a.instanceinfoImpl.GetPodInfo(apistructs.PodInfoRequest{AddonID: addonIns.ID})
			//podinfo, err := a.bdl.GetPodInfo(apistructs.PodInfoRequest{AddonID: addonIns.ID})
			if err != nil {
				logrus.Errorf("failed to get podinfo: %v", err)
			} else {
				pendingPods := []apistructs.PodInfoData{}
				for _, pod := range podList {
					if strutil.ToUpper(pod.Phase) == "PENDING" {
						pendingPods = append(pendingPods, pod)
					}
				}
				podnamelist := []string{}
				for _, pod := range pendingPods {
					podnamelist = append(podnamelist, pod.PodName)
				}
				a.ExportLogInfo(apistructs.InfoLevel, apistructs.AddonError, addonIns.ID, addonIns.ID, i18n.OrgSprintf(addonIns.OrgID, "UnhealthyAddonWithPendingPods"),
					addonIns.ID, strutil.Join(podnamelist, ",", true))
			}
		}
		time.Sleep(10 * time.Second)

	}
	return errors.Errorf("wait runtime ready timeout, addon: %s, instance id: %s", addonSpec.Name, addonIns.ID)
}

// initMsAfterStart 初始化mysql主从
func (a *Addon) initMsAfterStart(serviceGroup *apistructs.ServiceGroup, masterName, password string, clusterInfo *apistructs.ClusterInfoData) error {

	// 先处理master节点信息
	var masterService apistructs.Service
	var mysqlExecList []mysqlhelper.Request
	for _, valueItem := range serviceGroup.Dice.Services {
		if valueItem.Name == masterName {
			// master节点
			var execDto mysqlhelper.Request
			// set cluster name
			execDto.ClusterKey = (*clusterInfo)[apistructs.DICE_CLUSTER_NAME]
			// 设置默认密码
			execDto.User = apistructs.MySQLDefaultUser
			// 密码
			execDto.Password = password
			// 设置jdbc连接
			if len(valueItem.InstanceInfos) <= 0 {
				return errors.New("InstanceInfos为空")
			}
			execDto.Url = strings.Join([]string{apistructs.AddonMysqlJdbcPrefix, valueItem.InstanceInfos[0].Ip, ":", apistructs.AddonMysqlDefaultPort}, "")
			// 设置执行sql
			execDto.Sqls = []string{strings.Replace(apistructs.AddonMysqlMasterGrantBackupSqls, "${MYSQL_ROOT_PASSWORD}", password, -1),
				strings.Replace(apistructs.AddonMysqlCreateMysqlUserSqls, "${MYSQL_ROOT_PASSWORD}", password, -1),
				apistructs.AddonMysqlGrantMysqlUserSqls,
				apistructs.AddonMysqlFlushSqls}
			mysqlExecList = append(mysqlExecList, execDto)

			masterService = valueItem
		}
	}
	// 处理slave节点
	for _, valueItem := range serviceGroup.Dice.Services {
		if valueItem.Name != masterName {
			// master节点
			var execDto mysqlhelper.Request
			// set cluster name
			execDto.ClusterKey = (*clusterInfo)[apistructs.DICE_CLUSTER_NAME]
			// 设置默认密码
			execDto.User = apistructs.MySQLDefaultUser
			// 密码
			execDto.Password = password
			// 设置IP
			if len(valueItem.InstanceInfos) <= 0 {
				return errors.New("InstanceInfos为空")
			}
			execDto.Url = strings.Join([]string{apistructs.AddonMysqlJdbcPrefix, valueItem.InstanceInfos[0].Ip, ":", apistructs.AddonMysqlDefaultPort}, "")
			execDto.Sqls = []string{strings.Replace(strings.Replace(apistructs.AddonMysqlSlaveChangeMasterSqls, "${MYSQL_ROOT_PASSWORD}", password, -1), "${MASTER_HOST}", masterService.ShortVIP, -1),
				apistructs.AddonMysqlSlaveResetSlaveSqls,
				apistructs.AddonMysqlSlaveStartSlaveSqls,
				strings.Replace(apistructs.AddonMysqlCreateMysqlUserSqls, "${MYSQL_ROOT_PASSWORD}", password, -1),
				apistructs.AddonMysqlGrantSelectMysqlUserSqls,
				apistructs.AddonMysqlFlushSqls}
			mysqlExecList = append(mysqlExecList, execDto)
		}
	}

	// 请求init 接口
	for _, request := range mysqlExecList {
		err := request.Exec()
		if err != nil {
			return err
		}
	}
	return nil
}

// formatSoldierUrl 拼接soldier地址
func formatSoldierUrl(clusterInfo *apistructs.ClusterInfoData) string {
	if (*clusterInfo)[apistructs.DICE_IS_EDGE] == "false" {
		return "http://" + discover.Soldier()
	}
	port := "80"
	protocol := "https"
	if strutil.Contains((*clusterInfo)[apistructs.DICE_PROTOCOL], "https") {
		port = (*clusterInfo)[apistructs.DICE_HTTPS_PORT]
	} else {
		protocol = "http"
		port = (*clusterInfo)[apistructs.DICE_HTTP_PORT]
	}
	return protocol + "://soldier." + (*clusterInfo)[apistructs.DICE_ROOT_DOMAIN] + ":" + port

}

// checkMysqlHa 判断mysql主从同步状态
func (a *Addon) checkMysqlHa(serviceGroup *apistructs.ServiceGroup, masterName, password string, clusterInfo *apistructs.ClusterInfoData) error {

	// slave节点信息
	var mysqlExec mysqlhelper.Request
	// Tips: 对于基础版本，不需要 check slave status
	for _, valueItem := range serviceGroup.Dice.Services {
		if valueItem.Name != masterName {
			mysqlExec.ClusterKey = (*clusterInfo)[apistructs.DICE_CLUSTER_NAME]
			// 设置默认密码
			mysqlExec.User = apistructs.MySQLDefaultUser
			// 密码
			mysqlExec.Password = password
			// 设置jdbc连接
			if len(valueItem.InstanceInfos) <= 0 {
				return errors.New("InstanceInfos为空")
			}
			mysqlExec.Url = strings.Join([]string{apistructs.AddonMysqlJdbcPrefix, valueItem.Vip, ":", apistructs.AddonMysqlDefaultPort}, "")

			logrus.Infof("start checkMysqlHa, request: %+v", mysqlExec)

			// 请求init 接口
			status, err := mysqlExec.GetSlaveState()
			if err != nil {
				return err
			}

			if !strings.EqualFold(status.IORunning, "connecting") &&
				!strings.EqualFold(status.IORunning, "yes") ||
				!strings.EqualFold(status.SQLRunning, "connecting") &&
					!strings.EqualFold(status.SQLRunning, "yes") {
				return fmt.Errorf("slave status error")
			}
		}
	}

	return nil
}

// createDBs mysql 创建数据库
func (a *Addon) createDBs(serviceGroup *apistructs.ServiceGroup, existsMysqlExec *apistructs.ExistsMysqlExec,
	addonIns *dbclient.AddonInstance, masterName, password string, clusterInfo *apistructs.ClusterInfoData) ([]string, error) {

	// create_dbs从options里面取出来
	var dbNamesStr string

	var execSqlDto mysqlhelper.Request
	if serviceGroup != nil && serviceGroup.ID != "" {
		if addonIns.Options == "" {
			return nil, nil
		}
		var optionsMap map[string]string
		if err := json.Unmarshal([]byte(addonIns.Options), &optionsMap); err != nil {
			return nil, errors.Wrapf(err, "instance optiosn Unmarshal error, body %s", addonIns.Options)
		}
		dbNamesStr = optionsMap["create_dbs"]

		for _, valueItem := range serviceGroup.Dice.Services {
			if valueItem.Name == masterName {
				execSqlDto.ClusterKey = (*clusterInfo)[apistructs.DICE_CLUSTER_NAME]
				// 设置默认密码
				execSqlDto.User = apistructs.MySQLDefaultUser
				// 密码
				execSqlDto.Password = password
				// 设置jdbc连接
				if len(valueItem.InstanceInfos) <= 0 {
					return nil, errors.New("InstanceInfos为空")
				}
				execSqlDto.Url = strings.Join([]string{apistructs.AddonMysqlJdbcPrefix, valueItem.InstanceInfos[0].Ip, ":", apistructs.AddonMysqlDefaultPort}, "")
			}
		}
	}
	if existsMysqlExec != nil && existsMysqlExec.MysqlHost != "" {
		if len(existsMysqlExec.Options) <= 0 {
			return nil, nil
		}
		dbNamesStr = existsMysqlExec.Options["create_dbs"]
		execSqlDto.ClusterKey = (*clusterInfo)[apistructs.DICE_CLUSTER_NAME]
		// 设置默认密码
		execSqlDto.User = existsMysqlExec.User
		// 密码
		execSqlDto.Password = existsMysqlExec.Password
		// 设置jdbc连接
		execSqlDto.Url = strings.Join([]string{apistructs.AddonMysqlJdbcPrefix, existsMysqlExec.MysqlHost, ":", existsMysqlExec.MysqlPort}, "")
	}
	if dbNamesStr == "" {
		return nil, nil
	}
	if execSqlDto.Url == "" {
		return nil, nil
	}

	// 构建执行sql
	var sqls []string
	createDbs := strings.Split(dbNamesStr, ",")
	if len(createDbs) <= 0 {
		return nil, nil
	}
	for _, value := range createDbs {
		sqls = append(sqls, "create database if not exists `"+strings.Trim(value, " ")+"`;")
	}
	execSqlDto.Sqls = sqls

	// 请求create_dbs接口 接口
	err := execSqlDto.Exec()
	if err != nil {
		return nil, err
	}

	return createDbs, nil
}

// initSqlFile 初始化init.sql
// Deprecated
func (a *Addon) initSqlFile(serviceGroup *apistructs.ServiceGroup, existsMysqlExec *apistructs.ExistsMysqlExec,
	addonIns *dbclient.AddonInstance, createDbs []string, masterName, password string, clusterInfo *apistructs.ClusterInfoData) error {
	if createDbs == nil || len(createDbs) <= 0 {
		return nil
	}
	// 取出options信息，里面有init.sql
	var optionsMap map[string]string
	// 初始化定义执行参数
	var execSqlDto apistructs.MysqlExec
	if serviceGroup != nil && serviceGroup.ID != "" {
		if addonIns.Options == "" {
			return nil
		}
		if err := json.Unmarshal([]byte(addonIns.Options), &optionsMap); err != nil {
			return errors.Wrapf(err, "instance optiosn Unmarshal error, body %s", addonIns.Options)
		}

		for _, valueItem := range serviceGroup.Dice.Services {
			if valueItem.Name == masterName {
				// 设置默认密码
				execSqlDto.User = apistructs.MySQLDefaultUser
				// 密码
				execSqlDto.Password = password
				// 设置jdbc连接
				if len(valueItem.InstanceInfos) <= 0 {
					return errors.New("InstanceInfos为空")
				}
				execSqlDto.URL = strings.Join([]string{apistructs.AddonMysqlJdbcPrefix, valueItem.InstanceInfos[0].Ip, ":", apistructs.AddonMysqlDefaultPort}, "")
			}
		}
	}
	if existsMysqlExec != nil && existsMysqlExec.MysqlHost != "" {
		if len(existsMysqlExec.Options) <= 0 {
			return nil
		}
		optionsMap = existsMysqlExec.Options
		// 设置默认密码
		execSqlDto.User = existsMysqlExec.User
		// 密码
		execSqlDto.Password = existsMysqlExec.Password
		// 设置jdbc连接
		execSqlDto.URL = strings.Join([]string{apistructs.AddonMysqlJdbcPrefix, existsMysqlExec.MysqlHost, ":", existsMysqlExec.MysqlPort}, "")
	}
	if len(optionsMap) <= 0 {
		return nil
	}
	initSql := optionsMap["init_sql"]
	if initSql == "" {
		return nil
	}
	execSqlDto.CreateDbs = createDbs
	execSqlDto.OssURL = initSql

	// 请求create_dbs接口 接口
	err := a.bdl.MySQLExecFile(&execSqlDto, formatSoldierUrl(clusterInfo))
	return err
}

// BuildAddonRequestGroup build请求serviceGroup的body信息
func (a *Addon) BuildAddonRequestGroup(params *apistructs.AddonHandlerCreateItem, addonIns *dbclient.AddonInstance, addonSpec *apistructs.AddonExtension, addonDice *diceyml.Object) (*apistructs.ServiceGroupCreateV2Request, error) {
	addonDeployGroup := apistructs.ServiceGroupCreateV2Request{
		ClusterName: params.ClusterName,
		ID:          addonIns.ID,
		Type:        strings.Join([]string{"addon-", strings.Replace(strings.Replace(params.AddonName, "terminus-", "", 1), "-operator", "", 1)}, ""),
		GroupLabels: map[string]string{},
	}

	// group labels setting
	if len(params.Options) > 0 {
		groupLabels := map[string]string{
			"DICE_ADDON":      addonIns.ID,
			"SERVICE_TYPE":    "ADDONS",
			"DICE_ADDON_TYPE": strings.Replace(addonSpec.Name, "-operator", "", 1),
			"ADDON_TYPE":      addonSpec.Name,
		}
		// 从options中添加标签
		if v, ok := params.Options["projectId"]; ok {
			groupLabels["DICE_PROJECT_ID"] = v
		}
		if v, ok := params.Options["env"]; ok {
			groupLabels["DICE_WORKSPACE"] = strings.ToLower(v)
		}
		if v, ok := params.Options["instanceName"]; ok {
			groupLabels["DICE_ADDON_NAME"] = v
		}
		if v, ok := params.Options["shareScope"]; ok {
			groupLabels["DICE_SHARED_LEVEL"] = v
		}
		if v, ok := params.Options["projectName"]; ok {
			groupLabels["DICE_PROJECT_NAME"] = v
		}
		if v, ok := params.Options["orgId"]; ok {
			groupLabels["DICE_ORG_ID"] = v
		}
		if v, ok := params.Options["orgName"]; ok {
			groupLabels["DICE_ORG_NAME"] = v
		}
		groupLabels["DICE_CLUSTER_NAME"] = params.ClusterName
		addonDeployGroup.GroupLabels = groupLabels
	}
	// 根据plan信息中，规定的节点数量，build出对应数量的service信息

	//查询cluster信息
	clusterInfo, err := a.clusterinfoImpl.Info(params.ClusterName)
	if err != nil {
		return nil, err
	}
	// 查询集群operator支持情况
	capacity := a.cap.CapacityInfo(params.ClusterName)
	//capacity, err := a.bdl.CapacityInfo(params.ClusterName)
	if err != nil {
		return nil, err
	}
	var buildErr error
	switch params.AddonName {
	case apistructs.AddonZookeeper:
		addonDeployGroup.GroupLabels["ADDON_GROUPS"] = "1"
		buildErr = a.BuildZookeeperServiceItem(params, addonIns, addonSpec, addonDice, &clusterInfo)
	case apistructs.AddonApacheZookeeper:
		addonDeployGroup.GroupLabels["ADDON_GROUPS"] = "1"
		buildErr = a.BuildRealZkServiceItem(params, addonIns, addonSpec, addonDice, &clusterInfo)
	case apistructs.AddonConsul:
		addonDeployGroup.GroupLabels["ADDON_GROUPS"] = "1"
		buildErr = a.BuildConsulServiceItem(params, addonIns, addonSpec, addonDice, &clusterInfo)
	case apistructs.AddonCanal:
		addonDeployGroup.GroupLabels["ADDON_GROUPS"] = "1"
		buildErr = a.BuildCanalServiceItem(params, addonIns, addonSpec, addonDice)
	case apistructs.AddonRedis:
		if capacity.RedisOperator && params.Plan == apistructs.AddonProfessional {
			_, addonOperatorDice, err := a.GetAddonExtention(&apistructs.AddonHandlerCreateItem{
				AddonName: apistructs.AddonRedis + "-operator",
				Plan:      apistructs.AddonBasic,
			})
			if err != nil {
				return nil, err
			}
			buildErr = a.BuildRedisOperatorServiceItem(params.Options, addonIns, addonOperatorDice)
			addonDice = addonOperatorDice
		} else {
			if params.Plan == apistructs.AddonBasic {
				addonDeployGroup.GroupLabels["ADDON_GROUPS"] = "1"
			}
			if params.Plan == apistructs.AddonProfessional {
				addonDeployGroup.GroupLabels["ADDON_GROUPS"] = "2"
			}
			buildErr = a.BuildRedisServiceItem(params, addonIns, addonSpec, addonDice)
		}
	case apistructs.AddonMySQL:
		if !capacity.MysqlOperator {
			addonDeployGroup.GroupLabels["ADDON_GROUPS"] = "2"

			mysqlPreProcess(params, addonSpec, &addonDeployGroup)

			buildErr = a.BuildMysqlServiceItem(params, addonIns, addonSpec, addonDice, &clusterInfo)
		} else {
			_, addonOperatorDice, err := a.GetAddonExtention(&apistructs.AddonHandlerCreateItem{
				AddonName: apistructs.AddonMySQL + "-operator",
				Plan:      apistructs.AddonBasic,
			})
			if err != nil {
				return nil, err
			}
			buildErr = a.BuildMysqlOperatorServiceItem(params.Options, addonIns, addonOperatorDice, &clusterInfo)
		}
	case apistructs.AddonES:
		// 6.8.9 or later version use operator.
		if capacity.ElasticsearchOperator && version.Compare(addonSpec.Version, "6.8.9", ">=") {
			buildErr = a.BuildESOperatorServiceItem(params.Options, addonIns, addonDice, addonSpec.Version)
		} else {
			addonDeployGroup.GroupLabels["ADDON_GROUPS"] = "1"
			buildErr = a.BuildEsServiceItem(params, addonIns, addonSpec, addonDice, &clusterInfo)
		}
	case apistructs.AddonRocketMQ:
		addonDeployGroup.GroupLabels["ADDON_GROUPS"] = "3"
		buildErr = a.BuildRocketMqServiceItem(params, addonIns, addonSpec, addonDice, &clusterInfo)
	case apistructs.AddonKafka:
		addonDeployGroup.GroupLabels["ADDON_GROUPS"] = "2"
		buildErr = a.BuildKafkaServiceItem(params, addonIns, addonSpec, addonDice, &clusterInfo)
	case apistructs.AddonRabbitMQ:
		if params.Plan == apistructs.AddonBasic {
			addonDeployGroup.GroupLabels["ADDON_GROUPS"] = "1"
		}
		if params.Plan == apistructs.AddonProfessional {
			addonDeployGroup.GroupLabels["ADDON_GROUPS"] = "2"
		}
		buildErr = a.BuildRabbitmqServiceItem(params, addonIns, addonSpec, addonDice)
	case apistructs.AddonSourcecov:
		if !capacity.SourcecovOperator {
			return nil, errors.New("sourcecov operator not installed")
		}
		sam := &SourcecovAddonManagement{bdl: a.bdl}
		buildErr = sam.BuildSourceCovServiceItem(params, addonIns, addonSpec, addonDice, &clusterInfo)
	default: //default case
		buildErr = a.BuildCommonServiceItem(params, addonIns, addonSpec, addonDice, &clusterInfo)
	}
	if buildErr != nil {
		return nil, buildErr
	}

	if len((addonDice.Services)) > 0 {
		for k, v := range addonDice.Services {
			replica := 1
			if v.Deployments.Replicas != 0 {
				replica = v.Deployments.Replicas
			}
			for i := 0; i < replica; i++ {
				a.db.CreateAddonNode(&dbclient.AddonNode{
					ID:         a.getRandomId(),
					InstanceID: addonIns.ID,
					Namespace:  addonIns.Namespace,
					NodeName:   fmt.Sprintf("%s-%d", k, i),
					CreatedAt:  time.Now(),
					UpdatedAt:  time.Now(),
					CPU:        v.Resources.CPU,
					Mem:        uint64(v.Resources.Mem),
					Deleted:    apistructs.AddonNotDeleted,
				})
			}
		}
	}
	// labels加载到对应env中
	for _, ser := range addonDice.Services {
		ser.Envs["ADDON_ID"] = addonIns.ID
		for k, v := range addonDeployGroup.GroupLabels {
			ser.Envs[k] = v
		}
	}
	addonDeployGroup.DiceYml = *addonDice
	return &addonDeployGroup, nil
}

// BuildAddonScaleRequestGroup build请求serviceGroup的body信息
func (a *Addon) BuildAddonScaleRequestGroup(params *apistructs.AddonHandlerCreateItem, addonIns *dbclient.AddonInstance, scaleAction string, addonSpec *apistructs.AddonExtension, addonDice *diceyml.Object) (*apistructs.ServiceGroup, error) {
	addonDeploysg := apistructs.ServiceGroup{
		ClusterName: params.ClusterName,
		Dice: apistructs.Dice{
			ID:       addonIns.ScheduleName,
			Type:     addonIns.Namespace,
			Services: make([]apistructs.Service, 0),
		},
	}

	addonCreateReq, err := a.BuildAddonRequestGroup(params, addonIns, addonSpec, addonDice)
	if err != nil || addonCreateReq == nil {
		logrus.Errorf("failed to build addon creating request body, addon: %v, err: %v", addonIns.ID, err)
		return nil, err
	}

	addonDeploysg.Labels = addonCreateReq.GroupLabels

	for k, v := range addonCreateReq.DiceYml.Meta {
		addonDeploysg.Labels[k] = v
	}

	for _, s := range addonCreateReq.DiceYml.Services {
		for k, v := range s.Labels {
			addonDeploysg.Labels[k] = v
		}
	}

	for svcName, svc := range addonCreateReq.DiceYml.Services {
		scale := 0
		if scaleAction == apistructs.ScaleActionDown {
			scale = 0
		}
		if scaleAction == apistructs.ScaleActionUp {
			// TODO: 从数据库表获取前一次 scaleUp 成功之后的 replicas
			scale = svc.Deployments.Replicas
		}

		addonDeploysg.Services = append(addonDeploysg.Services, apistructs.Service{
			Name:  svcName,
			Scale: scale,
			Resources: apistructs.Resources{
				Cpu:  svc.Resources.CPU,
				Mem:  float64(svc.Resources.Mem),
				Disk: float64(svc.Resources.Disk),
			},
		})
	}

	return &addonDeploysg, nil
}

func mysqlPreProcess(params *apistructs.AddonHandlerCreateItem, addonSpec *apistructs.AddonExtension, addonDeployGroup *apistructs.ServiceGroupCreateV2Request) {
	if params.Plan == apistructs.AddonBasic {
		addonDeployGroup.GroupLabels["ADDON_GROUPS"] = "1"
		if p, ok := addonSpec.Plan[params.Plan]; ok {
			p.Nodes = 1
			addonSpec.Plan[params.Plan] = p
		}
	}
}

// FailAndDelete addon发布失败后，更新表状态
func (a *Addon) FailAndDelete(addonIns *dbclient.AddonInstance) error {
	// 如果失败了，及时删除addon信息
	if err := a.UpdateAddonStatus(addonIns, apistructs.AddonAttachFail); err != nil {
		return err
	}
	// attachments表更新
	attachments, err := a.db.GetAttachmentsByInstanceID(addonIns.ID)
	if err != nil {
		return err
	}
	if len(*attachments) > 0 {
		attItem := (*attachments)[0]
		attItem.Deleted = apistructs.AddonDeleted
		if err := a.db.UpdateAttachment(&attItem); err != nil {
			return err
		}
	}
	// schedule删除
	if err := a.serviceGroupImpl.Delete(addonIns.Namespace, addonIns.ScheduleName, "false"); err != nil {
		logrus.Errorf("failed to delete addon: %s/%s", addonIns.Namespace, addonIns.ScheduleName)
		return err
	}

	return nil
}

// updateAddonStatus addon发布失败后，更新表状态
// TODO 状态更新不应混杂删除逻辑
func (a *Addon) UpdateAddonStatus(addonIns *dbclient.AddonInstance, addonStatus apistructs.AddonStatus) error {
	// 如果失败了，及时删除addon信息
	addonInsResult, err := a.db.GetAddonInstance(addonIns.ID)
	if err != nil {
		return err
	}
	if addonInsResult == nil {
		return nil
	}
	addonInsResult.Status = string(addonStatus)

	routings, err := a.db.GetByRealInstance(addonInsResult.ID)
	if err != nil {
		return err
	}
	if routings != nil && len(*routings) > 0 {
		for _, routingItem := range *routings {
			routingItem.Status = string(addonStatus)
			if addonStatus != apistructs.AddonAttached && addonStatus != apistructs.AddonAttaching {
				routingItem.Deleted = apistructs.AddonDeleted
				addonInsResult.Deleted = apistructs.AddonDeleted
			}
			a.db.UpdateInstanceRouting(&routingItem)
		}
	}

	// 更新instance表
	if err := a.db.UpdateAddonInstance(addonInsResult); err != nil {
		return err
	}

	return nil
}

// 内部addon发布
func (a *Addon) insideAddonDeploy(addonIns *dbclient.AddonInstance, addonSpec *apistructs.AddonExtension, addonDice *diceyml.Object,
	params *apistructs.AddonHandlerCreateItem) (*[]string, error) {
	// 无内部 addon 依赖
	if len(addonDice.AddOns) == 0 {
		return nil, nil
	}

	// 判断策略中是否支持解析addons
	if len(addonSpec.Strategy) == 0 {
		addonSpec.Strategy = map[string]interface{}{apistructs.AddonStrategyParsingAddonsKey: true}
	}

	parsingSwitch, ok := addonSpec.Strategy[apistructs.AddonStrategyParsingAddonsKey]
	if ok {
		if !(parsingSwitch.(bool)) {
			logrus.Infof("no need to deploy inside addon: %+v", addonSpec.Name)
			return nil, nil
		}
	}

	// 循环处理内部addon
	insideInstanceIDs := make([]string, 0, len(addonDice.AddOns))
	for insName, addon := range addonDice.AddOns {
		// 还是先按照老的格式来解析
		plan := strings.SplitN(addon.Plan, ":", 2)
		if len(plan) != 2 {
			return nil, errors.Errorf("invalid plan: %s in addon: %s", addon.Plan, addonSpec.Name)
		}
		// build body
		paramItem := *params
		paramItem.InstanceName = insName
		paramItem.AddonName = plan[0]
		paramItem.Plan = plan[1]
		paramItem.InsideAddon = "Y"
		if len(paramItem.Options) == 0 {
			paramItem.Options = map[string]string{}
		}
		if _, ok := paramItem.Options["version"]; ok {
			paramItem.Options["version"] = ""
		}
		if len(addon.Options) > 0 {
			for key, value := range addon.Options {
				paramItem.Options[key] = value
			}
		}
		logrus.Infof("insideAddonDeploy paramItem: %+v", paramItem)
		insResp, err := a.AttachAndCreate(&paramItem)
		if err != nil {
			return nil, err
		}
		if err := a.db.CreateAddonInstanceRelation(&dbclient.AddonInstanceRelation{
			ID:                a.getRandomId(),
			OutsideInstanceID: addonIns.ID,
			InsideInstanceID:  insResp.RealInstanceID,
			CreatedAt:         time.Now(),
			UpdatedAt:         time.Now(),
			Deleted:           apistructs.AddonNotDeleted,
		}); err != nil {
			return nil, err
		}
		insideInstanceIDs = append(insideInstanceIDs, insResp.RealInstanceID)
	}
	return &insideInstanceIDs, nil
}

// 等待内部addon发布成功
func (a *Addon) waitInsideAddonDeploy(insideInstanceIds *[]string) (*map[string]string, error) {
	if insideInstanceIds == nil || len(*insideInstanceIds) == 0 {
		return nil, nil
	}
	readyInsMap := make(map[string]string, len(*insideInstanceIds))
	startTime := time.Now().Unix()
	for {
		if time.Now().Unix()-startTime > apistructs.RuntimeUpMaxWaitTime {
			return nil, errors.Errorf("wait inside addon ready timeout")
		}
		for _, id := range *insideInstanceIds {
			if _, ok := readyInsMap[id]; !ok {
				ins, err := a.db.GetAddonInstance(id)
				if err != nil {
					return nil, err
				}
				if ins == nil {
					return nil, errors.Errorf("inside addon deploy error")
				}
				if ins.Status == string(apistructs.AddonAttached) {
					readyInsMap[id] = ins.AddonName
				}
			}
		}
		// 如果所有的都发布成功了，则跳出循环
		if len(readyInsMap) == len(*insideInstanceIds) {
			break
		}
		// sleep 10秒，继续请求
		time.Sleep(10 * time.Second)
	}

	return &readyInsMap, nil
}

// retrieveInsideAddon addon发布失败后，更新表状态
func (a *Addon) retrieveInsideAddon(addonIns *dbclient.AddonInstance) error {
	insideList, err := a.db.GetByOutSideInstanceID(addonIns.ID)
	if err != nil {
		logrus.Errorf("retrieveInsideAddon GetByOutSideInstanceID err: %v ", err)
		return err
	}

	for _, id := range *insideList {
		instance, err := a.db.GetAddonInstance(id.InsideInstanceID)
		if err != nil {
			return err
		}
		if instance == nil {
			continue
		}
		if instance.PlatformServiceType != apistructs.PlatformServiceTypeBasic {
			continue
		}
		// 查询出attachmen信息数量
		count, err := a.db.GetAttachmentCountByInstanceID(instance.ID)
		if err != nil {
			return err
		}
		//如果引用数量为0，则执行删除
		if count == 0 {
			if err := a.serviceGroupImpl.Delete(instance.Namespace, instance.ScheduleName, "false"); err != nil {
				logrus.Errorf("[alert] failed to delete addon: %v from scheduler, :%v", addonIns.ID, err)
				continue
			}
		}
	}

	return nil
}
