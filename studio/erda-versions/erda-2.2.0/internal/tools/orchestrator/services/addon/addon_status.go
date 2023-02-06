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
	"bytes"
	"encoding/base64"
	"encoding/gob"
	"encoding/json"
	"fmt"
	"os"
	"strconv"
	"strings"
	"time"

	"github.com/labstack/gommon/random"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/pkg/kms/kmstypes"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/strutil"
)

// CommonDeployStatus 通用addon状态拉取
func (a *Addon) CommonDeployStatus(addonIns *dbclient.AddonInstance, serviceGroup *apistructs.ServiceGroup,
	addonDice *diceyml.Object, addonSpec *apistructs.AddonExtension) (map[string]string, error) {
	configMap := map[string]string{}
	if len(addonSpec.ConfigVars) == 0 {
		return configMap, nil
	}
	// 获取spec中的configVars，处理成可通用处理的map
	// 如：atum:DS_HOST, atum是service name，:分割后面的真实环境变量key
	// 只需要DS这个值
	configVarsMap := map[string]string{}
	for _, configItem := range addonSpec.ConfigVars {
		if configItem == "" {
			continue
		}
		// 例：configItem为atum:DS_HOST，取出moduleName，"atum"
		moduleName := strings.ToLower(strings.Split(configItem, ":")[0])
		// 例：configItem为atum:DS_HOST，取出环境变量key前缀，"DS"
		configVarsMap[moduleName] = strings.Split(strings.Split(configItem, ":")[1], "_")[0]
	}
	for _, valueItem := range serviceGroup.Dice.Services {
		if v, ok := configVarsMap[valueItem.Name]; ok {
			configMap[strings.Join([]string{v, "_", "HOST"}, "")] = valueItem.Vip
			configMap[strings.Join([]string{v, "_", "PORT"}, "")] = strconv.Itoa(addonDice.Services[valueItem.Name].Ports[0].Port)
		}
		break
	}
	return configMap, nil
}

// MySQLDeployStatus myql状态拉取，以及后续环境变量处理
func (a *Addon) MySQLDeployStatus(addonIns *dbclient.AddonInstance, serviceGroup *apistructs.ServiceGroup,
	clusterInfo *apistructs.ClusterInfoData) (map[string]string, error) {
	configMap := map[string]string{}
	// 获取
	masterName, err := a.db.GetByInstanceIDAndField(addonIns.ID, apistructs.AddonMysqlMasterKey)
	if err != nil {
		logrus.Errorf("获取master报错")
		return nil, err
	}
	password, err := a.db.GetByInstanceIDAndField(addonIns.ID, apistructs.AddonMysqlPasswordKey)
	if err != nil {
		logrus.Errorf("获取mysql password报错, %v", err)
		return nil, err
	}
	decPwd := password.Value
	if addonIns.KmsKey != "" {
		decPwd, err = a.DecryptPassword(&addonIns.KmsKey, password.Value)
		if err != nil {
			logrus.Errorf("mysql password decript err, %v", err)
			return nil, err
		}
	} else {
		decPwd, err = a.DecryptPassword(nil, password.Value)
		if err != nil {
			logrus.Errorf("mysql password decript err, %v", err)
			return nil, err
		}
	}

	// 查询集群operator支持情况
	capacity := a.cap.CapacityInfo(addonIns.Cluster)
	if err != nil {
		return nil, err
	}

	if !capacity.MysqlOperator {
		logrus.Info("mysql operator switch is off")
		// 执行mysql主从初始化
		if err := a.initMsAfterStart(serviceGroup, masterName.Value, decPwd, clusterInfo); err != nil {
			logrus.Errorf("mysql initMsAfterStart 报错, %v", err)
			return nil, err
		}
		logrus.Infof("inited mysql: %s", addonIns.ID)
		// sleep 10秒，继续请求
		time.Sleep(time.Duration(2) * time.Second)
		// check主从状态是否正常
		if err := a.checkMysqlHa(serviceGroup, masterName.Value, decPwd, clusterInfo); err != nil {
			logrus.Errorf("mysql checkMysqlHa 报错, %v", err)
			return nil, err
		}
		logrus.Infof("checked mysql %s ha status", addonIns.ID)
		// sleep 10秒，继续请求
		time.Sleep(time.Duration(1) * time.Second)
	}

	// create_dbs操作
	createDbs, err := a.createDBs(serviceGroup, &apistructs.ExistsMysqlExec{}, addonIns, masterName.Value, decPwd, clusterInfo)
	if err != nil {
		logrus.Errorf("mysql createDbs 报错, %v", err)
		return nil, err
	}
	logrus.Infof("created db for mysql: %s", addonIns.ID)
	if len(createDbs) > 0 {
		// init.sql操作
		if err := a.initSqlFile(serviceGroup, &apistructs.ExistsMysqlExec{}, addonIns, createDbs, masterName.Value, decPwd, clusterInfo); err != nil {
			logrus.Errorf("mysql initSqlFile 报错, %v", err)
			return nil, err
		}
		logrus.Infof("executed init.sql for mysql: %s", addonIns.ID)
	}

	// config环境变量配置
	for _, valueItem := range serviceGroup.Dice.Services {
		if valueItem.Name == masterName.Value {
			configMap[apistructs.AddonMysqlHostName] = valueItem.Vip
			configMap[apistructs.AddonMysqlPortName] = apistructs.AddonMysqlDefaultPort
			continue
		}
		configMap[apistructs.AddonMysqlSlaveHostName] = valueItem.Vip
		configMap[apistructs.AddonMysqlSlavePortName] = apistructs.AddonMysqlDefaultPort
	}
	configMap[apistructs.AddonMysqlUserName] = apistructs.AddonMysqlUser
	configMap[apistructs.AddonMysqlPasswordName] = password.Value
	configMap[apistructs.AddonPasswordHasEncripy] = "YES"

	return configMap, nil
}

// CanalDeployStatus canal状态拉取
func (a *Addon) CanalDeployStatus(addonIns *dbclient.AddonInstance, serviceGroup *apistructs.ServiceGroup) (map[string]string, error) {
	configMap := map[string]string{}
	for _, valueItem := range serviceGroup.Dice.Services {
		configMap[apistructs.AddonCanalHostName] = valueItem.Vip
		configMap[apistructs.AddonCanalPortName] = apistructs.AddonCanalDefaultPort
		break
	}
	return configMap, nil
}

// EsDeployStatus es状态拉取
func (a *Addon) EsDeployStatus(addonIns *dbclient.AddonInstance, serviceGroup *apistructs.ServiceGroup) (map[string]string, error) {
	configMap := map[string]string{}
	password, _ := a.db.GetByInstanceIDAndField(addonIns.ID, apistructs.AddonESPasswordKey)

	// 单节点情况
	if len(serviceGroup.Services) >= 1 {
		configMap[apistructs.AddonEsHostName] = serviceGroup.Services[0].Vip
		configMap[apistructs.AddonEsPortName] = apistructs.AddonEsDefaultPort
		if password != nil {
			configMap[apistructs.AddonEsPasswordName] = password.Value
			configMap[apistructs.AddonEsUserName] = apistructs.AddonESDefaultUser
		}
		configMap[apistructs.AddonEsTCPPortName] = apistructs.AddonEsDefaultTcpPort
	}
	configMap["CLUSTER_NAME"] = addonIns.ID
	return configMap, nil
}

// KafkaDeployStatus kafka状态拉取
func (a *Addon) KafkaDeployStatus(addonIns *dbclient.AddonInstance, serviceGroup *apistructs.ServiceGroup, clusterInfo *apistructs.ClusterInfoData) (map[string]string, error) {
	configMap := map[string]string{}
	var kafkaHost []string
	for index, value := range serviceGroup.Services {
		// 跳过manager节点
		if strings.Contains(value.Name, "manager") {
			continue
		}

		kafkaHost = append(kafkaHost, value.Vip+":"+apistructs.KafakaDefaultPort)
		if index == 0 {
			configMap[apistructs.AddonKafkaHostName] = value.Vip
			configMap[apistructs.AddonKafkaPortName] = apistructs.KafakaDefaultPort
			continue
		}
		configMap[apistructs.AddonKafkaHostName+"_"+strconv.Itoa(index-1)] = value.Vip
		configMap[apistructs.AddonKafkaPortName+"_"+strconv.Itoa(index-1)] = apistructs.KafakaDefaultPort
	}
	configMap["MQ_SERVER_ADDRESS"] = strings.Join(kafkaHost, ",")
	configMap["MQ_CLIENT_TYPE"] = strings.ToUpper(addonIns.AddonName)
	configMap["KAFKA_HOSTS"] = strings.Join(kafkaHost, ",")
	// manager暴露外网
	configMap["PUBLIC_HOST"] = strings.Join([]string{addonIns.AddonName, "-", apistructs.AddonKafkaManager, "-", addonIns.ID, ".", (*clusterInfo)[apistructs.DICE_ROOT_DOMAIN]}, "")

	insExtra, err := a.db.GetByInstanceIDAndField(addonIns.ID, apistructs.AddonKafkaZkHost)
	if err != nil {
		return nil, err
	}
	configMap["ZK_HOSTS"] = insExtra.Value
	return configMap, nil
}

// RocketDeployStatus rocket状态拉取
func (a *Addon) RocketDeployStatus(addonIns *dbclient.AddonInstance, serviceGroup *apistructs.ServiceGroup) (map[string]string, error) {
	configMap := map[string]string{}
	i := 1
	var mqServerAddress []string
	for _, value := range serviceGroup.Services {
		// nameSrv处理
		if strings.Contains(value.Name, apistructs.AddonRocketNameSrvPrefix) {
			mqServerAddress = append(mqServerAddress, value.Vip+":"+apistructs.AddonRocketNameSrvDefaultPort)
			if i == 1 {
				configMap[apistructs.AddonRocketNameSrvHost] = value.Vip
				configMap[apistructs.AddonRocketNameSrvPort] = apistructs.AddonRocketNameSrvDefaultPort
				i++
				continue
			}
			configMap[apistructs.AddonRocketNameSrvHost+strconv.Itoa(i)] = value.Vip
			configMap[apistructs.AddonRocketNameSrvPort+strconv.Itoa(i)] = apistructs.AddonRocketNameSrvDefaultPort
			i++
		}
		// consul处理
		if strings.Contains(value.Name, apistructs.AddonRocketConsulPrefix) {
			configMap["PUBLIC_HOST"] = value.Vip + ":" + apistructs.AddonRocketConsoleDefaultPort
		}
	}
	configMap["MQ_SERVER_ADDRESS"] = strings.Join(mqServerAddress, ";")
	configMap["MQ_CLIENT_TYPE"] = strings.ToUpper(addonIns.AddonName)
	return configMap, nil
}

// RabbitmqDeployStatus rabbitmq状态拉取
func (a *Addon) RabbitmqDeployStatus(addonIns *dbclient.AddonInstance, serviceGroup *apistructs.ServiceGroup) (map[string]string, error) {
	configMap := map[string]string{}
	i := 1

	password, err := a.db.GetByInstanceIDAndField(addonIns.ID, apistructs.AddonRabbitmqPasswordKey)
	if err != nil {
		return nil, err
	}
	var mqServerAddress []string
	for _, value := range serviceGroup.Services {
		// 节点host处理
		mqServerAddress = append(mqServerAddress, value.Vip+":"+apistructs.AddonRabbitmqDefaultPort)
		if i == 1 {
			configMap[apistructs.AddonRabbitmqHostName] = value.Vip
			configMap[apistructs.AddonRabbitmqPortName] = apistructs.AddonRabbitmqDefaultPort
			i++
			continue
		}
		configMap[apistructs.AddonRabbitmqHostName+strconv.Itoa(i)] = value.Vip
		configMap[apistructs.AddonRabbitmqPortName+strconv.Itoa(i)] = apistructs.AddonRabbitmqDefaultPort
		i++
	}
	configMap["RABBIT_ADDRESS"] = strings.Join(mqServerAddress, ",")
	configMap["RABBITMQ_DEFAULT_USER"] = "admin"
	configMap["RABBITMQ_DEFAULT_PASS"] = password.Value
	return configMap, nil
}

// RedisDeployStatus redis状态拉取
func (a *Addon) RedisDeployStatus(addonIns *dbclient.AddonInstance, serviceGroup *apistructs.ServiceGroup) (map[string]string, error) {
	logrus.Info("redis环境变量提取。")
	configMap := map[string]string{}
	password, err := a.db.GetByInstanceIDAndField(addonIns.ID, apistructs.AddonRedisPasswordKey)
	if err != nil {
		return nil, err
	}
	if len(serviceGroup.Services) == 1 {
		logrus.Info("单节点redis环境变量。")
		configMap[apistructs.AddonRedisHostName] = serviceGroup.Services[0].Vip
		configMap[apistructs.AddonRedisPortName] = apistructs.RedisDefaultPort
		configMap[apistructs.AddonRedisPasswordName] = password.Value
	} else {
		serviceStr, _ := json.Marshal(serviceGroup.Services)
		logrus.Info("redis sentinel 环境变量。" + string(serviceStr))
		var sentinels []string
		index := 1
		for _, value := range serviceGroup.Services {
			if strings.Index(value.Name, "sentinel") >= 0 {
				index++
				sentinels = append(sentinels, value.Vip+":"+apistructs.RedisSentinelDefaultPort)
			}
		}
		configMap[apistructs.AddonRedisSentinelsName] = strings.Join(sentinels, ",")
		configMap[apistructs.AddonRedisPasswordName] = password.Value
		configMap[apistructs.AddonRedisMasterName] = apistructs.RedisDefaultMasterName
	}
	return configMap, nil
}

// ZookeeperDeployStatus zk状态拉取
func (a *Addon) ZookeeperDeployStatus(addonIns *dbclient.AddonInstance, serviceGroup *apistructs.ServiceGroup) (map[string]string, error) {
	configMap := map[string]string{}
	var zkHost []string
	for index, value := range serviceGroup.Services {
		zkHost = append(zkHost, value.Vip+":"+apistructs.AddonZKDefaultPort)
		if index == 0 {
			configMap[apistructs.AddonZKHostName] = value.Vip
			configMap[apistructs.AddonZKPortName] = apistructs.AddonZKDefaultPort
			continue
		}
		configMap[apistructs.AddonZKHostName+strconv.Itoa(index)] = value.Vip
		configMap[apistructs.AddonZKPortName+strconv.Itoa(index)] = apistructs.AddonZKDefaultPort
	}

	configMap[apistructs.AddonZKHostListName] = strings.Join(zkHost, ",")
	configMap[apistructs.AddonZKDubboHostListName] = strings.Join(zkHost, "|")
	configMap[apistructs.AddonZKDubboName] = fmt.Sprintf("%s?backup=%s,%s", zkHost[0], zkHost[1], zkHost[2])
	return configMap, nil
}

// ApacheZookeeperDeployStatus zk状态拉取
func (a *Addon) ApacheZookeeperDeployStatus(addonIns *dbclient.AddonInstance, serviceGroup *apistructs.ServiceGroup) (map[string]string, error) {
	configMap := map[string]string{}
	var zkHost []string
	for index, value := range serviceGroup.Services {
		zkHost = append(zkHost, value.Vip+":"+apistructs.AddonZKDefaultPort)
		if index == 0 {
			configMap[apistructs.AddonZKHostName] = value.Vip
			configMap[apistructs.AddonZKPortName] = apistructs.AddonZKDefaultPort
			continue
		}
		configMap[apistructs.AddonZKHostName+strconv.Itoa(index)] = value.Vip
		configMap[apistructs.AddonZKPortName+strconv.Itoa(index)] = apistructs.AddonZKDefaultPort
	}

	configMap[apistructs.AddonZKHostListName] = strings.Join(zkHost, ",")
	configMap[apistructs.AddonZKDubboHostListName] = strings.Join(zkHost, "|")
	configMap[apistructs.AddonZKDubboName] = fmt.Sprintf("%s?backup=%s,%s", zkHost[0], zkHost[1], zkHost[2])
	return configMap, nil
}

// ConsulDeployStatus consul状态拉取
func (a *Addon) ConsulDeployStatus(addonIns *dbclient.AddonInstance, serviceGroup *apistructs.ServiceGroup) (map[string]string, error) {
	configMap := map[string]string{}
	i := 1
	consulServices := make([]string, 0, len(serviceGroup.Services))
	for index, value := range serviceGroup.Services {
		consulServices = append(consulServices, value.Vip+":"+apistructs.AddonConsulDefaultPort)
		if index == 0 {
			configMap[apistructs.AddonConsulHostName] = value.Vip
			configMap[apistructs.AddonConsulPortName] = apistructs.AddonConsulDefaultPort
			continue
		}
		configMap[apistructs.AddonConsulHostName+strconv.Itoa(i)] = value.Vip
		configMap[apistructs.AddonConsulPortName+strconv.Itoa(i)] = apistructs.AddonConsulDefaultPort
		i++
	}
	configMap[apistructs.AddonConsulDNSPortName] = apistructs.AddonConsulDefaulDNStPort
	return configMap, nil
}

// buildZookeeperServiceItem 构造zookeeper的service信息
func (a *Addon) BuildZookeeperServiceItem(params *apistructs.AddonHandlerCreateItem, addonIns *dbclient.AddonInstance,
	addonSpec *apistructs.AddonExtension, addonDice *diceyml.Object, clusterInfo *apistructs.ClusterInfoData) error {

	addonDeployPlan := addonSpec.Plan[params.Plan]
	serviceMap := diceyml.Services{}

	// zookeeper servers列表
	var zooServers = map[string]string{}
	for i := 1; i <= addonDeployPlan.Nodes; i++ {
		zooServers["server."+strconv.Itoa(i)] = strings.ToUpper("${" + strings.Replace(addonSpec.Name, "-", "_", -1) + "_" + strconv.Itoa(i) + "_HOST}:2888:3888")
	}

	for i := 1; i <= addonDeployPlan.Nodes; i++ {
		nodeID := a.getRandomId()
		// 从dice.yml中取出对应addon信息
		serviceItem := *addonDice.Services[addonSpec.Name]
		// Resource资源
		serviceItem.Resources = diceyml.Resources{CPU: addonDeployPlan.CPU, MaxCPU: addonDeployPlan.MaxCPU, Mem: addonDeployPlan.Mem, MaxMem: addonDeployPlan.MaxMem}
		// label
		if len(serviceItem.Labels) == 0 {
			serviceItem.Labels = map[string]string{}
		}
		serviceItem.Labels["ADDON_GROUP_ID"] = addonSpec.Name
		SetlabelsFromOptions(params.Options, serviceItem.Labels)

		// envs
		heapSize := fmt.Sprintf("%.f", float64(addonDeployPlan.Mem)*0.5)

		var zooServerLi = make([]string, 0, addonDeployPlan.Nodes)

		for key, value := range zooServers {
			if ("server." + strconv.Itoa(i)) == key {
				zooServerLi = append(zooServerLi, key+"=0.0.0.0:2888:3888")
				continue
			}
			zooServerLi = append(zooServerLi, key+"="+value)
		}

		serviceItem.Envs = map[string]string{
			"ADDON_ID":             addonIns.ID,
			"ADDON_NODE_ID":        nodeID,
			"ZOO_TICK_TIME":        "6000",
			"ZOO_SYNC_LIMIT":       "5",
			"ZOO_AUTO_INTERVAL":    "2",
			"ZOO_AUTO_RETAINCOUNT": "3",
			"ZOO_MY_ID":            strconv.Itoa(i),
			"ZOO_SERVERS":          strings.Join(zooServerLi, "\n"),
			"JAVA_OPTS":            strings.Join([]string{"-Xms", heapSize, "m -Xmx", heapSize, "m"}, ""),
		}
		// volume信息
		clusterType := (*clusterInfo)[apistructs.DICE_CLUSTER_TYPE]
		switch strutil.ToLower(clusterType) {
		case apistructs.DCOS, apistructs.DCOS_OP:
			serviceItem.Binds = diceyml.Binds{
				(*clusterInfo)[apistructs.DICE_STORAGE_MOUNTPOINT] + "/addon/zookeeper/data/" + addonIns.ID + "/" + nodeID + ":/data:rw",
				(*clusterInfo)[apistructs.DICE_STORAGE_MOUNTPOINT] + "/addon/zookeeper/datalog/" + addonIns.ID + "/" + nodeID + ":/datalog:rw",
			}
		default:
			/*
				serviceItem.Binds = diceyml.Binds{nodeID + "-data:/data:rw", nodeID + "-data:/datalog:rw"}
			*/
			//  /data volume
			vol01 := SetAddonVolumes(params.Options, "/data", false)

			//  /datalog volume
			vol02 := SetAddonVolumes(params.Options, "/datalog", false)

			serviceItem.Volumes = diceyml.Volumes{vol01, vol02}
		}
		// 设置service
		serviceMap[strings.Join([]string{addonSpec.Name, strconv.Itoa(i)}, "-")] = &serviceItem
	}
	addonDice.Services = serviceMap

	return nil
}

// BuildRealZkServiceItem 构造zookeeper的service信息
func (a *Addon) BuildRealZkServiceItem(params *apistructs.AddonHandlerCreateItem, addonIns *dbclient.AddonInstance,
	addonSpec *apistructs.AddonExtension, addonDice *diceyml.Object, clusterInfo *apistructs.ClusterInfoData) error {

	addonDeployPlan := addonSpec.Plan[params.Plan]
	serviceMap := diceyml.Services{}

	// zookeeper servers列表
	var zooServers = map[string]string{}
	for i := 1; i <= addonDeployPlan.Nodes; i++ {
		zooServers["server."+strconv.Itoa(i)] = strings.ToUpper("${" + strings.Replace(addonSpec.Name, "-", "_", -1) + "_" + strconv.Itoa(i) + "_HOST}:2888:3888")
	}

	for i := 1; i <= addonDeployPlan.Nodes; i++ {
		nodeID := a.getRandomId()
		// 从dice.yml中取出对应addon信息
		serviceItem := *addonDice.Services[addonSpec.Name]
		// Resource资源
		serviceItem.Resources = diceyml.Resources{CPU: addonDeployPlan.CPU, MaxCPU: addonDeployPlan.MaxCPU, Mem: addonDeployPlan.Mem, MaxMem: addonDeployPlan.MaxMem}
		// label
		if len(serviceItem.Labels) == 0 {
			serviceItem.Labels = map[string]string{}
		}
		serviceItem.Labels["ADDON_GROUP_ID"] = addonSpec.Name
		SetlabelsFromOptions(params.Options, serviceItem.Labels)
		// envs
		heapSize := fmt.Sprintf("%.f", float64(addonDeployPlan.Mem)*0.5)

		var zooServerLi = make([]string, 0, addonDeployPlan.Nodes)

		for key, value := range zooServers {
			if ("server." + strconv.Itoa(i)) == key {
				zooServerLi = append(zooServerLi, key+"=0.0.0.0:2888:3888")
				continue
			}
			zooServerLi = append(zooServerLi, key+"="+value)
		}

		serviceItem.Envs = map[string]string{
			"ADDON_ID":             addonIns.ID,
			"ADDON_NODE_ID":        nodeID,
			"ZOO_TICK_TIME":        "6000",
			"ZOO_SYNC_LIMIT":       "5",
			"ZOO_AUTO_INTERVAL":    "2",
			"ZOO_AUTO_RETAINCOUNT": "3",
			"ZOO_MY_ID":            strconv.Itoa(i),
			"ZOO_SERVERS":          strings.Join(zooServerLi, "\n"),
			"ZK_SERVER_HEAP":       heapSize,
		}
		// volume信息
		clusterType := (*clusterInfo)[apistructs.DICE_CLUSTER_TYPE]
		switch strutil.ToLower(clusterType) {
		case apistructs.DCOS, apistructs.DCOS_OP:
			serviceItem.Binds = diceyml.Binds{
				(*clusterInfo)[apistructs.DICE_STORAGE_MOUNTPOINT] + "/addon/zookeeper/data/" + addonIns.ID + "/" + nodeID + ":/data:rw",
				(*clusterInfo)[apistructs.DICE_STORAGE_MOUNTPOINT] + "/addon/zookeeper/datalog/" + addonIns.ID + "/" + nodeID + ":/datalog:rw",
			}
		default:
			//serviceItem.Binds = diceyml.Binds{nodeID + "-data:/data:rw", nodeID + "-data:/datalog:rw"}
			//  /data volume
			vol01 := SetAddonVolumes(params.Options, "/data", false)

			//  /datalog volume
			vol02 := SetAddonVolumes(params.Options, "/datalog", false)

			serviceItem.Volumes = diceyml.Volumes{vol01, vol02}
		}
		// 设置service
		serviceMap[strings.Join([]string{addonSpec.Name, strconv.Itoa(i)}, "-")] = &serviceItem
	}
	addonDice.Services = serviceMap

	return nil
}

// buildConsulServiceItem 构造consul的service信息
func (a *Addon) BuildConsulServiceItem(params *apistructs.AddonHandlerCreateItem, addonIns *dbclient.AddonInstance,
	addonSpec *apistructs.AddonExtension, addonDice *diceyml.Object, clusterInfo *apistructs.ClusterInfoData) error {
	if len(params.Options) < 0 || params.Options["canal.instance.master.address"] != "" || params.Options["canal.instance.dbUsername"] != "" ||
		params.Options["canal.instance.dbPassword"] != "" {

	}

	addonDeployPlan := addonSpec.Plan[params.Plan]
	serviceMap := diceyml.Services{}
	for i := 1; i <= addonDeployPlan.Nodes; i++ {
		// 从dice.yml中取出对应addon信息
		serviceItem := *addonDice.Services[addonSpec.Name]
		// Resource资源
		serviceItem.Resources = diceyml.Resources{CPU: addonDeployPlan.CPU, MaxCPU: addonDeployPlan.MaxCPU, Mem: addonDeployPlan.Mem, MaxMem: addonDeployPlan.MaxMem}
		// label
		if len(serviceItem.Labels) == 0 {
			serviceItem.Labels = map[string]string{}
		}
		serviceItem.Labels["ADDON_GROUP_ID"] = addonSpec.Name
		SetlabelsFromOptions(params.Options, serviceItem.Labels)
		// volume信息
		//serviceItem.Binds = diceyml.Binds{(*clusterInfo)[apistructs.DICE_STORAGE_MOUNTPOINT] + "/addon/consul/" + a.getRandomId() + ":/consul/data:rw"}
		//  /consul/data volume
		vol01 := SetAddonVolumes(params.Options, "/consul/data", false)
		serviceItem.Volumes = diceyml.Volumes{vol01}

		// 设置service
		serviceMap[strings.Join([]string{addonSpec.Name, strconv.Itoa(i)}, "-")] = &serviceItem
	}
	addonDice.Services = serviceMap

	return nil
}

// buildEsServiceItem 构造es的service信息
func (a *Addon) BuildEsServiceItem(params *apistructs.AddonHandlerCreateItem, addonIns *dbclient.AddonInstance,
	addonSpec *apistructs.AddonExtension, addonDice *diceyml.Object, clusterInfo *apistructs.ClusterInfoData) error {

	addonDeployPlan := addonSpec.Plan[params.Plan]
	serviceMap := diceyml.Services{}

	//unicastHosts
	var unicastHosts = make([]string, addonDeployPlan.Nodes)
	for i := 1; i <= addonDeployPlan.Nodes; i++ {
		unicastHosts[i-1] = strings.Join([]string{"${", strings.ToUpper(strings.Replace(addonSpec.Name, "-", "_", 1)) + "_" + strconv.Itoa(i), "_HOST}"}, "")
	}

	for i := 1; i <= addonDeployPlan.Nodes; i++ {
		nodeID := a.getRandomId()
		// 从dice.yml中取出对应addon信息
		serviceItem := *addonDice.Services[addonSpec.Name]
		// Resource资源
		serviceItem.Resources = diceyml.Resources{CPU: addonDeployPlan.CPU, MaxCPU: addonDeployPlan.MaxCPU, Mem: addonDeployPlan.Mem, MaxMem: addonDeployPlan.MaxMem}
		// label
		if len(serviceItem.Labels) == 0 {
			serviceItem.Labels = map[string]string{}
		}
		serviceItem.Labels["ADDON_GROUP_ID"] = addonSpec.Name
		SetlabelsFromOptions(params.Options, serviceItem.Labels)
		// envs
		heapSize := fmt.Sprintf("%.f", float64(addonDeployPlan.Mem)*0.5)
		serviceItem.Envs = map[string]string{
			"ADDON_ID":                           addonIns.ID,
			"ADDON_NODE_ID":                      nodeID,
			"XPACK_SECURITY_ENABLED":             "false",
			"XPACK_ML_ENABLED":                   "false",
			"XPACK_MONITORING_ENABLED":           "false",
			"XPACK_WATCHER_ENABLED":              "false",
			"CLUSTER_NAME":                       addonIns.ID,
			"NODE_NAME":                          nodeID,
			"DISCOVERY_ZEN_PING_UNICAST_HOSTS":   strings.Join(unicastHosts, ","),
			"DISCOVERY_ZEN_MINIMUM_MASTER_NODES": strconv.Itoa(Min(i, 2)),
			"TAKE_FILE_OWNERSHIP":                "1000:1000",
			"ES_JAVA_OPTS":                       strings.Join([]string{"-Xms", heapSize, "m -Xmx", heapSize, "m -XX:NewRatio=4 -XX:+PrintGC -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps"}, ""),
			"UPDATE_DICT_URL":                    "http://esplus." + (*clusterInfo)[apistructs.DICE_ROOT_DOMAIN] + "/api/esplus/dict/esload/" + addonIns.ID,
		}
		// volume信息
		//serviceItem.Binds = diceyml.Binds{"es-data:/usr/share/elasticsearch/data:rw"}
		//  /usr/share/elasticsearch/data volume
		vol01 := SetAddonVolumes(params.Options, "/usr/share/elasticsearch/data", false)
		serviceItem.Volumes = diceyml.Volumes{vol01}

		//health check
		serviceItem.HealthCheck.HTTP = &diceyml.HTTPCheck{Port: 9200, Path: "/_cluster/health", Duration: 180}
		// 设置service
		serviceMap[strings.Join([]string{addonSpec.Name, strconv.Itoa(i)}, "-")] = &serviceItem
	}
	addonDice.Services = serviceMap

	return nil
}

// buildKafkaServiceItem 构造kafka的service信息
func (a *Addon) BuildKafkaServiceItem(params *apistructs.AddonHandlerCreateItem, addonIns *dbclient.AddonInstance,
	addonSpec *apistructs.AddonExtension, addonDice *diceyml.Object, clusterInfo *apistructs.ClusterInfoData) error {

	addonDeployPlan := addonSpec.Plan[params.Plan]
	serviceMap := diceyml.Services{}

	var kafkaPlan apistructs.AddonPlanItem
	var kafkaManagerPlan apistructs.AddonPlanItem
	if len(addonDeployPlan.InsideMoudle) > 0 {
		kafkaPlan = addonDeployPlan.InsideMoudle[addonSpec.Name]
		kafkaManagerPlan = addonDeployPlan.InsideMoudle[addonSpec.Name+"-manager"]
	} else {
		kafkaPlan = addonDeployPlan
		kafkaManagerPlan = addonDeployPlan
	}
	var kafkaFactor int = 1
	if kafkaPlan.Nodes > 2 {
		kafkaFactor = 3
	}
	//获取zk地址
	var zkHosts []string
	if addonIns.Options != "" {
		var insOptions map[string]string
		if err := json.Unmarshal([]byte(addonIns.Options), &insOptions); err != nil {
			return err
		}
		if insID, ok := insOptions[apistructs.AddonZookeeper]; ok {
			zkIns, err := a.db.GetAddonInstance(insID)
			if err != nil {
				return err
			}
			var zkConfigMap map[string]string
			if err := json.Unmarshal([]byte(zkIns.Config), &zkConfigMap); err != nil {
				return err
			}
			zkHosts = strings.Split(zkConfigMap[apistructs.AddonZKHostListName], ",")
		}
	}
	if len(zkHosts) == 0 {
		return errors.New("没有找到zk信息")
	}
	kafkaZkConn := strings.Join(zkHosts, "/"+addonIns.ID+",") + "/" + addonIns.ID
	if err := a.db.CreateAddonInstanceExtra(&dbclient.AddonInstanceExtra{
		ID:         a.getRandomId(),
		CreatedAt:  time.Now(),
		UpdatedAt:  time.Now(),
		Deleted:    apistructs.AddonNotDeleted,
		InstanceID: addonIns.ID,
		Field:      apistructs.AddonKafkaZkHost,
		Value:      kafkaZkConn,
	}); err != nil {
		return err
	}

	var depends = make([]string, 0, kafkaPlan.Nodes)
	for i := 1; i <= kafkaPlan.Nodes; i++ {
		nodeID := a.getRandomId()
		// 从dice.yml中取出对应addon信息
		serviceItem := *addonDice.Services[addonSpec.Name]
		// Resource资源
		serviceItem.Resources = diceyml.Resources{CPU: kafkaPlan.CPU, MaxCPU: kafkaPlan.MaxCPU, Mem: kafkaPlan.Mem, MaxMem: kafkaPlan.MaxMem}
		// label
		if len(serviceItem.Labels) == 0 {
			serviceItem.Labels = map[string]string{}
		}
		serviceItem.Labels["ADDON_GROUP_ID"] = addonSpec.Name + "-cluster"
		SetlabelsFromOptions(params.Options, serviceItem.Labels)
		// envs
		heapSize := getHeapSize(kafkaPlan.Mem)

		serviceItem.Envs = map[string]string{
			"ADDON_ID":                               addonIns.ID,
			"ADDON_NODE_ID":                          nodeID,
			"KAFKA_ADVERTISED_PORT":                  "${" + strings.ToUpper(strings.Replace(addonSpec.Name, "-", "_", 1)) + "_" + strconv.Itoa(i) + "_PORT}",
			"KAFKA_ADVERTISED_HOST_NAME":             "${" + strings.ToUpper(strings.Replace(addonSpec.Name, "-", "_", 1)) + "_" + strconv.Itoa(i) + "_HOST}",
			"KAFKA_LOG_DIRS":                         "/kafka/data",
			"KAFKA_ZOOKEEPER_CONNECT":                kafkaZkConn,
			"KAFKA_NUM_PARTITIONS":                   "16",
			"KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR": strconv.Itoa(kafkaFactor),
			"KAFKA_DEFAULT_REPLICATION_FACTOR":       strconv.Itoa(kafkaFactor),
			"KAFKA_HEAP_OPTS":                        strings.Join([]string{"-Xms", heapSize, "m -Xmx", heapSize, "m"}, ""),
		}
		// volume信息
		clusterType := (*clusterInfo)[apistructs.DICE_CLUSTER_TYPE]
		switch strutil.ToLower(clusterType) {
		case apistructs.DCOS, apistructs.DCOS_OP:
			serviceItem.Binds = diceyml.Binds{(*clusterInfo)[apistructs.DICE_STORAGE_MOUNTPOINT] + "/addon/kafka/" + nodeID + ":/kafka/data:rw"}
		default:
			//serviceItem.Binds = diceyml.Binds{nodeID + ":/kafka/data:rw"}
			//  /kafka/data volume
			vol01 := SetAddonVolumes(params.Options, "/kafka/data", false)
			serviceItem.Volumes = diceyml.Volumes{vol01}
		}

		// 设置service
		serviceMap[strings.Join([]string{addonSpec.Name, strconv.Itoa(i)}, "-")] = &serviceItem

		depends = append(depends, strings.Join([]string{addonSpec.Name, strconv.Itoa(i)}, "-"))
	}

	// kafka-manager信息构建
	// 从dice.yml中取出对应addon信息
	managerServiceItem := addonDice.Services[addonSpec.Name+"-manager"]
	// manager资源
	managerServiceItem.Resources = diceyml.Resources{CPU: kafkaManagerPlan.CPU, MaxCPU: kafkaManagerPlan.MaxCPU, Mem: kafkaManagerPlan.Mem, MaxMem: kafkaManagerPlan.MaxMem}
	// envs
	heapSize := getHeapSize(apistructs.KafkaManagerMem)
	managerServiceItem.Envs = map[string]string{
		"ADDON_ID":      addonIns.ID,
		"ADDON_NODE_ID": a.getRandomId(),
		"ZK_HOSTS":      kafkaZkConn,
		"JAVA_OPTS":     strings.Join([]string{"-Xms", heapSize, "m -Xmx", heapSize, "m"}, ""),
	}
	// depends
	managerServiceItem.DependsOn = depends
	// labels
	if len(managerServiceItem.Labels) == 0 {
		managerServiceItem.Labels = map[string]string{}
	}
	managerServiceItem.Labels["ADDON_GROUP_ID"] = addonSpec.Name + "-manager"
	managerServiceItem.Labels["HAPROXY_GROUP"] = "external"
	managerServiceItem.Labels["HAPROXY_0_VHOST"] = strings.Join([]string{addonSpec.Name + "-manager", "-", addonIns.ID, ".", (*clusterInfo)[apistructs.DICE_ROOT_DOMAIN]}, "")

	SetlabelsFromOptions(params.Options, managerServiceItem.Labels)
	// 设置service
	serviceMap[strings.Join([]string{addonSpec.Name, "manager"}, "-")] = managerServiceItem

	addonDice.Services = serviceMap

	return nil
}

func (a *Addon) BuildRocketMqServiceItem(params *apistructs.AddonHandlerCreateItem, addonIns *dbclient.AddonInstance,
	addonSpec *apistructs.AddonExtension, addonDice *diceyml.Object, clusterInfo *apistructs.ClusterInfoData) error {

	addonDeployPlan := addonSpec.Plan[params.Plan]
	serviceMap := diceyml.Services{}

	var nameSrvUrlNodes = []string{}
	var nameSrvNameNodes = []string{}

	var nameSrvPlan apistructs.AddonPlanItem
	var brokerPlan apistructs.AddonPlanItem
	var consolePlan apistructs.AddonPlanItem
	if len(addonDeployPlan.InsideMoudle) > 0 {
		nameSrvPlan = addonDeployPlan.InsideMoudle[addonSpec.Name+"-namesrv"]
		brokerPlan = addonDeployPlan.InsideMoudle[addonSpec.Name+"-broker"]
		consolePlan = addonDeployPlan.InsideMoudle[addonSpec.Name+"-console"]
	} else {
		nameSrvPlan = addonDeployPlan
		brokerPlan = addonDeployPlan
		consolePlan = addonDeployPlan
	}

	for i := 1; i <= nameSrvPlan.Nodes; i++ {
		// 从dice.yml中取出对应addon信息
		nameSrvNodeId := a.getRandomId()
		// nameSrv构建
		nameServiceItem := *addonDice.Services[addonSpec.Name+"-namesrv"]
		// Resource资源
		nameServiceItem.Resources = diceyml.Resources{CPU: nameSrvPlan.CPU, MaxCPU: nameSrvPlan.MaxCPU, Mem: nameSrvPlan.Mem, MaxMem: nameSrvPlan.MaxMem}
		// label
		if len(nameServiceItem.Labels) == 0 {
			nameServiceItem.Labels = map[string]string{}
		}
		nameServiceItem.Labels["ADDON_GROUP_ID"] = addonSpec.Name + "-namesrv"
		SetlabelsFromOptions(params.Options, nameServiceItem.Labels)
		// envs
		heapSize := getHeapSize(nameServiceItem.Resources.Mem)
		heapSizeInt, err := strconv.Atoi(heapSize)
		// TODO 异常处理
		if err != nil {

		}
		nameServiceItem.Envs = map[string]string{
			"ADDON_ID":      addonIns.ID,
			"ADDON_NODE_ID": nameSrvNodeId,
			"Xms":           heapSize + "m",
			"Xmx":           heapSize + "m",
			"Xmn":           strconv.Itoa(int(heapSizeInt/2)) + "m",
			"JAVA_OPTS":     strings.Join([]string{"-Xms", heapSize, "m -Xmx", heapSize, "m"}, ""),
		}
		// binds
		clusterType := (*clusterInfo)[apistructs.DICE_CLUSTER_TYPE]
		switch strutil.ToLower(clusterType) {
		case apistructs.DCOS, apistructs.DCOS_OP:
			nameServiceItem.Binds = diceyml.Binds{(*clusterInfo)[apistructs.DICE_STORAGE_MOUNTPOINT] + "/addon/rocketmq/" + addonIns.ID + "/" + nameSrvNodeId + "/namesrv/logs:/opt/store:rw",
				(*clusterInfo)[apistructs.DICE_STORAGE_MOUNTPOINT] + "/addon/rocketmq/" + addonIns.ID + "/" + nameSrvNodeId + "/namesrv/logs:/opt/logs:rw"}
		default:
			//nameServiceItem.Binds = diceyml.Binds{nameSrvNodeId + "-namesrv-store:/opt/store:rw", nameSrvNodeId + "-namesrv-logs:/opt/logs:rw"}
			//  /opt/store volume
			vol01 := SetAddonVolumes(params.Options, "/opt/store", false)
			//  /opt/logs volume
			vol02 := SetAddonVolumes(params.Options, "/opt/logs", false)
			nameServiceItem.Volumes = diceyml.Volumes{vol01, vol02}
		}

		// 设置service
		serviceMap[strings.Join([]string{addonSpec.Name, "namesrv", strconv.Itoa(i)}, "-")] = &nameServiceItem
		nameSrvNameNodes = append(nameSrvNameNodes, strings.Join([]string{addonSpec.Name, "namesrv", strconv.Itoa(i)}, "-"))
		key := strings.ToUpper(strings.Join([]string{addonSpec.Name, "namesrv", strconv.Itoa(i)}, "_"))
		nameSrvUrlNodes = append(nameSrvUrlNodes, "${"+key+"_HOST}:${"+key+"_PORT}")

	}

	for i := 1; i <= brokerPlan.Nodes; i++ {
		// 从dice.yml中取出对应addon信息
		brokerNodeId := a.getRandomId()
		nodeName := strings.Join([]string{addonSpec.Name, "broker", strconv.Itoa(i)}, "-")
		brokerServiceItem := *addonDice.Services[addonSpec.Name+"-broker"]
		// Resource资源
		brokerServiceItem.Resources = diceyml.Resources{CPU: brokerPlan.CPU, MaxCPU: brokerPlan.MaxCPU, Mem: brokerPlan.Mem, MaxMem: brokerPlan.MaxMem}
		// label
		if len(brokerServiceItem.Labels) == 0 {
			brokerServiceItem.Labels = map[string]string{}
		}
		brokerServiceItem.Labels["ADDON_GROUP_ID"] = addonSpec.Name + "-broker"
		SetlabelsFromOptions(params.Options, brokerServiceItem.Labels)
		// envs
		heapSize := getHeapSize(brokerPlan.Mem)
		heapSizeInt, err := strconv.Atoi(heapSize)
		// TODO 异常处理
		if err != nil {

		}
		brokerServiceItem.Envs = map[string]string{
			"ADDON_ID":      addonIns.ID,
			"ADDON_NODE_ID": brokerNodeId,
			"Xms":           heapSize + "m",
			"Xmx":           heapSize + "m",
			"Xmn":           strconv.Itoa(int(heapSizeInt/2)) + "m",
			"JAVA_OPTS":     strings.Join([]string{"-Xms", heapSize, "m -Xmx", heapSize, "m"}, ""),
			"NAMESRV_ADDR":  strings.Join(nameSrvUrlNodes, ";"),
			"BROKER_NAME":   nodeName,
		}
		// binds
		clusterType := (*clusterInfo)[apistructs.DICE_CLUSTER_TYPE]
		switch strutil.ToLower(clusterType) {
		case apistructs.DCOS, apistructs.DCOS_OP:
			brokerServiceItem.Binds = diceyml.Binds{(*clusterInfo)[apistructs.DICE_STORAGE_MOUNTPOINT] + "/addon/rocketmq/" + addonIns.ID + "/" + brokerNodeId + "/broker/logs:/opt/store:rw",
				(*clusterInfo)[apistructs.DICE_STORAGE_MOUNTPOINT] + "/addon/rocketmq/" + addonIns.ID + "/" + brokerNodeId + "/broker/logs:/opt/logs:rw"}
		default:
			//brokerServiceItem.Binds = diceyml.Binds{brokerNodeId + "-broker-store:/opt/store:rw", brokerNodeId + "-broker-logs:/opt/logs:rw"}
			//  /opt/store volume
			vol01 := SetAddonVolumes(params.Options, "/opt/store", false)
			//  /opt/logs volume
			vol02 := SetAddonVolumes(params.Options, "/opt/logs", false)
			brokerServiceItem.Volumes = diceyml.Volumes{vol01, vol02}
		}
		// depends
		brokerServiceItem.DependsOn = nameSrvNameNodes
		// 设置service
		serviceMap[nodeName] = &brokerServiceItem
	}

	// 从dice.yml中取出对应addon信息
	consoleServiceItem := addonDice.Services[addonSpec.Name+"-console"]
	// Resource资源
	consoleServiceItem.Resources = diceyml.Resources{CPU: consolePlan.CPU, MaxCPU: consolePlan.MaxCPU, Mem: consolePlan.Mem, MaxMem: consolePlan.MaxMem}
	// label
	if len(consoleServiceItem.Labels) == 0 {
		consoleServiceItem.Labels = map[string]string{}
	}
	consoleServiceItem.Labels["ADDON_GROUP_ID"] = addonSpec.Name + "-console"
	SetlabelsFromOptions(params.Options, consoleServiceItem.Labels)
	// envs
	heapSize := getHeapSize(consolePlan.Mem)
	heapSizeInt, err := strconv.Atoi(heapSize)
	// TODO 异常处理
	if err != nil {

	}
	consoleServiceItem.Envs = map[string]string{
		"ADDON_ID":      addonIns.ID,
		"ADDON_NODE_ID": a.getRandomId(),
		"Xms":           heapSize + "m",
		"Xmx":           heapSize + "m",
		"Xmn":           strconv.Itoa(int(heapSizeInt/2)) + "m",
		"JAVA_OPTS":     strings.Join([]string{"-Xms", heapSize, "m -Xmx", heapSize, "m -Drocketmq.namesrv.addr=", strings.Join(nameSrvUrlNodes, ";"), " -Dcom.rocketmq.sendMessageWithVIPChannel=false"}, ""),
		"NAMESRV_ADDR":  strings.Join(nameSrvUrlNodes, ";"),
	}
	// depends
	consoleServiceItem.DependsOn = nameSrvNameNodes
	// 设置service
	serviceMap[strings.Join([]string{addonSpec.Name, "console", "1"}, "-")] = consoleServiceItem

	addonDice.Services = serviceMap

	return nil
}

// buildMysqlServiceItem 构造mysql的service信息
func (a *Addon) BuildMysqlServiceItem(params *apistructs.AddonHandlerCreateItem, addonIns *dbclient.AddonInstance,
	addonSpec *apistructs.AddonExtension, addonDice *diceyml.Object, clusterInfo *apistructs.ClusterInfoData) error {
	addonDeployPlan := addonSpec.Plan[params.Plan]
	serviceMap := diceyml.Services{}
	// 保存密码
	password, err := a.savePassword(addonIns, apistructs.AddonMysqlPasswordKey)
	if err != nil {
		return err
	}
	serviceBase := *addonDice.Services[addonSpec.Name]
	for i := 1; i <= addonDeployPlan.Nodes; i++ {
		addonNodeId := a.getRandomId()

		// TODO deepCopy，序列化反序列化，不优雅
		var serviceItem diceyml.Service
		if err := a.deepCopy(&serviceItem, &serviceBase); err != nil {
			logrus.Errorf("deep copy error, %v", err)
		}

		// Resource资源
		serviceItem.Resources = diceyml.Resources{CPU: addonDeployPlan.CPU, MaxCPU: addonDeployPlan.MaxCPU, Mem: addonDeployPlan.Mem, MaxMem: addonDeployPlan.MaxMem}
		// label
		if len(serviceItem.Labels) == 0 {
			serviceItem.Labels = map[string]string{}
		}

		SetlabelsFromOptions(params.Options, serviceItem.Labels)

		/*
			// volume信息
			serviceItem.Binds = diceyml.Binds{strings.Join([]string{strings.Join([]string{(*clusterInfo)[apistructs.DICE_STORAGE_MOUNTPOINT], "/addon/mysql/backup/", addonIns.ID, "_", strconv.Itoa(i)}, ""), "/var/backup/mysql", "rw"}, ":"),
				strings.Join([]string{strings.Join([]string{addonIns.ID, strconv.Itoa(i)}, "_"), "/var/lib/mysql", "rw"}, ":")}
		*/

		// Use volumes 代替 Binds
		//  /var/backup/mysql volume
		vol01 := SetAddonVolumes(params.Options, "/var/backup/mysql", false)

		//  /var/lib/mysql volume
		vol02 := SetAddonVolumes(params.Options, "/var/lib/mysql", false)

		serviceItem.Volumes = diceyml.Volumes{vol01, vol02}

		// health check
		execHealth := diceyml.ExecCheck{Cmd: fmt.Sprintf("mysql -uroot -p%s  -e 'select 1'", password)}
		health := diceyml.HealthCheck{Exec: &execHealth}
		serviceItem.HealthCheck = health
		// envs
		serviceItem.Envs = map[string]string{
			"ADDON_ID":            addonIns.ID,
			"ADDON_NODE_ID":       addonNodeId,
			"MYSQL_ROOT_PASSWORD": password,
			"SERVER_ID":           strconv.Itoa(i),
		}

		// 保存mysql的master节点信息
		serviceNameItem := strings.Join([]string{addonSpec.Name, strconv.Itoa(i)}, "-")
		if i == 1 {
			addonInstanceExtra := dbclient.AddonInstanceExtra{
				ID:         a.getRandomId(),
				InstanceID: addonIns.ID,
				Field:      apistructs.AddonMysqlMasterKey,
				Value:      serviceNameItem,
				Deleted:    apistructs.AddonNotDeleted,
			}

			serviceItem.Labels["ADDON_GROUP_ID"] = addonSpec.Name + "-" + apistructs.AddonMysqlMasterKey
			err := a.db.CreateAddonInstanceExtra(&addonInstanceExtra)
			if err != nil {
				return err
			}
		} else {
			serviceItem.Labels["ADDON_GROUP_ID"] = addonSpec.Name + "-" + apistructs.AddonMysqlSlaveKey
		}
		// 设置service
		serviceMap[serviceNameItem] = &serviceItem
	}
	addonDice.Services = serviceMap

	return nil
}

func (a *Addon) deepCopy(dst, src interface{}) error {
	var buf bytes.Buffer
	if err := gob.NewEncoder(&buf).Encode(src); err != nil {
		return err
	}
	return gob.NewDecoder(bytes.NewBuffer(buf.Bytes())).Decode(dst)
}

func (a *Addon) BuildESOperatorServiceItem(options map[string]string, addonIns *dbclient.AddonInstance, addonDice *diceyml.Object, version string) error {
	// 设置密码
	password, err := a.savePassword(addonIns, apistructs.AddonESPasswordKey)
	if err != nil {
		return err
	}
	addonDice.Meta = map[string]string{
		"USE_OPERATOR": "elasticsearch",
		"VERSION":      version,
	}
	//设置环境变量
	for _, v := range addonDice.Services {
		if len(v.Envs) == 0 {
			v.Envs = map[string]string{
				"ADDON_ID":    addonIns.ID,
				"requirepass": password,
			}
		} else {
			v.Envs["ADDON_ID"] = addonIns.ID
			v.Envs["requirepass"] = password
		}
		// set options for label
		if len(v.Labels) == 0 {
			v.Labels = make(map[string]string)
		}
		SetlabelsFromOptions(options, v.Labels)

		//  主要目的是传递 PVC 相关信息
		vol01 := SetAddonVolumes(options, "/for-operator", false)
		v.Volumes = diceyml.Volumes{vol01}
	}
	return nil
}

// buildMysqlOperatorServiceItem 生成operator发布的格式
func (a *Addon) BuildMysqlOperatorServiceItem(options map[string]string, addonIns *dbclient.AddonInstance, addonDice *diceyml.Object, clusterInfo *apistructs.ClusterInfoData) error {
	// 设置密码
	password, err := a.savePassword(addonIns, apistructs.AddonMysqlPasswordKey)
	if err != nil {
		return err
	}
	// 设置meta
	addonDice.Meta = map[string]string{
		"USE_OPERATOR": apistructs.AddonMySQL,
	}
	//设置环境变量
	for k, v := range addonDice.Services {
		v.Envs = map[string]string{
			"ADDON_ID":            addonIns.ID,
			"ADDON_NODE_ID":       a.getRandomId(),
			"MYSQL_ROOT_PASSWORD": password,
		}

		if len(v.Labels) == 0 {
			v.Labels = make(map[string]string)
		}
		SetlabelsFromOptions(options, v.Labels)

		//  主要目的是传递 PVC 相关信息
		vol01 := SetAddonVolumes(options, "/for-operator", false)
		v.Volumes = diceyml.Volumes{vol01}

		addonInstanceExtra := dbclient.AddonInstanceExtra{
			ID:         a.getRandomId(),
			InstanceID: addonIns.ID,
			Field:      apistructs.AddonMysqlMasterKey,
			Value:      k,
			Deleted:    apistructs.AddonNotDeleted,
		}
		err := a.db.CreateAddonInstanceExtra(&addonInstanceExtra)
		if err != nil {
			return err
		}
	}
	return nil
}

// buildRedisServiceItem 构造redis的service信息
func (a *Addon) BuildRedisServiceItem(params *apistructs.AddonHandlerCreateItem, addonIns *dbclient.AddonInstance,
	addonSpec *apistructs.AddonExtension, addonDice *diceyml.Object) error {
	addonDeployPlan := addonSpec.Plan[params.Plan]
	serviceMap := diceyml.Services{}
	// 生成password
	// 设置密码
	password, err := a.savePassword(addonIns, apistructs.AddonRedisPasswordKey)
	if err != nil {
		return err
	}

	// 初始化amster节点
	masterServiceItem := addonDice.Services[apistructs.RedisMasterNamePrefix]
	// resource信息
	masterServiceItem.Resources = diceyml.Resources{CPU: addonDeployPlan.CPU, MaxCPU: addonDeployPlan.MaxCPU, Mem: addonDeployPlan.Mem, MaxMem: addonDeployPlan.MaxMem}
	// envs
	masterServiceItem.Envs = map[string]string{
		"ADDON_ID":       addonIns.ID,
		"ADDON_NODE_ID":  a.getRandomId(),
		"REDIS_ROLE":     "master",
		"REDIS_PORT":     apistructs.RedisDefaultPort,
		"REDIS_PASSWORD": password,
		"REDIS_LOG_DIR":  "/datalog",
		"REDIS_LOG_FILE": "/datalog/redis.log",
	}
	// label
	if len(masterServiceItem.Labels) == 0 {
		masterServiceItem.Labels = map[string]string{}
	}
	masterServiceItem.Labels["ADDON_GROUP_ID"] = addonSpec.Name
	// set options for label
	SetlabelsFromOptions(params.Options, masterServiceItem.Labels)

	serviceMap[apistructs.RedisMasterNamePrefix] = masterServiceItem

	if params.Plan == apistructs.AddonProfessional {
		// 初始化amster节点
		slaveServiceItem := *serviceMap[apistructs.RedisMasterNamePrefix]
		slaveServiceItem.Envs = map[string]string{
			"REDIS_ROLE":     "slave",
			"ADDON_NODE_ID":  a.getRandomId(),
			"ADDON_ID":       addonIns.ID,
			"MASTER_HOST":    strings.Join([]string{"${", strings.ToUpper(strings.Replace(apistructs.RedisMasterNamePrefix, "-", "_", 1)), "_HOST}"}, ""),
			"MASTER_PORT":    apistructs.RedisDefaultPort,
			"REDIS_PORT":     apistructs.RedisDefaultPort,
			"REDIS_PASSWORD": password,
			"REDIS_LOG_DIR":  "/datalog",
			"REDIS_LOG_FILE": "/datalog/redis.log",
		}
		// depends_on
		slaveServiceItem.DependsOn = []string{apistructs.RedisMasterNamePrefix}
		// put到dice.Services中
		serviceMap[apistructs.RedisSlaveNamePrefix] = &slaveServiceItem

		for i := 1; i <= 3; i++ {
			sentinelServiceItem := *addonDice.Services[apistructs.RedisSentinelNamePrefix]
			sentinelServiceItem.Envs = map[string]string{
				"ADDON_NODE_ID":       a.getRandomId(),
				"ADDON_ID":            addonIns.ID,
				"REDIS_ROLE":          "sentinel",
				"SENTINEL_PORT":       apistructs.RedisSentinelDefaultPort,
				"SENTINEL_QUORUM":     apistructs.RedisSentinelQuorum,
				"SENTINEL_DOWN_AFTER": apistructs.RedisSentinelDownAfter,
				"SENTINEL_FAILOVER":   apistructs.RedisSentinelFailover,
				"PARALLEL_SYNCS":      apistructs.RedisSentinelSyncs,
				"MASTER_NAME":         apistructs.RedisDefaultMasterName,
				"MASTER_HOST":         strings.Join([]string{"${", strings.ToUpper(strings.Replace(apistructs.RedisMasterNamePrefix, "-", "_", 1)), "_HOST}"}, ""),
				"MASTER_PORT":         apistructs.RedisDefaultPort,
				"SLAVES_HOST":         strings.Join([]string{"${", strings.ToUpper(strings.Replace(apistructs.RedisSlaveNamePrefix, "-", "_", 1)), "_HOST}"}, ""),
				"SLAVES_PORT":         apistructs.RedisDefaultPort,
				"REDIS_LOG_DIR":       "/datalog",
				"REDIS_LOG_FILE":      "/datalog/redis.log",
				"REDIS_PASSWORD":      password,
			}
			// label 设定
			if len(sentinelServiceItem.Labels) == 0 {
				sentinelServiceItem.Labels = map[string]string{}
			}
			sentinelServiceItem.Labels["ADDON_GROUP_ID"] = apistructs.RedisSentinelNamePrefix
			// set options for label
			SetlabelsFromOptions(params.Options, sentinelServiceItem.Labels)

			// depends_on
			sentinelServiceItem.DependsOn = []string{apistructs.RedisMasterNamePrefix, apistructs.RedisSlaveNamePrefix}
			// put到dice.Services中
			serviceMap[apistructs.RedisSentinelNamePrefix+"-"+strconv.Itoa(i)] = &sentinelServiceItem
		}
	}
	addonDice.Services = serviceMap

	return nil
}

// buildRedisOperatorServiceItem redis operator build service item
func (a *Addon) BuildRedisOperatorServiceItem(options map[string]string, addonIns *dbclient.AddonInstance, addonDice *diceyml.Object) error {
	// 设置密码
	password, err := a.savePassword(addonIns, apistructs.AddonRedisPasswordKey)
	if err != nil {
		return err
	}
	// 设置meta
	addonDice.Meta = map[string]string{
		"USE_OPERATOR": apistructs.AddonRedis,
	}
	//设置环境变量
	for _, v := range addonDice.Services {
		v.Envs = map[string]string{
			"ADDON_ID":      addonIns.ID,
			"ADDON_NODE_ID": a.getRandomId(),
			"requirepass":   password,
		}
		// set options for label
		if len(v.Labels) == 0 {
			v.Labels = make(map[string]string)
		}
		SetlabelsFromOptions(options, v.Labels)
	}
	return nil
}

func (a *Addon) guessCanalAddr(instanceroutings []dbclient.AddonInstanceRouting, instances []dbclient.AddonInstance) map[string]string {
	for _, routing := range instanceroutings {
		var (
			address    string
			dbusername string
			dbpassword string
		)

		if routing.AddonName != "mysql" && routing.AddonName != "alicloud-rds" {
			continue
		}
		optionmap := map[string]string{}
		if err := json.Unmarshal([]byte(routing.Options), &optionmap); err != nil {
			continue
		}
		if optionmap["MYSQL_PASSWORD"] != "" && optionmap["MYSQL_USERNAME"] != "" {
			dbusername = optionmap["MYSQL_USERNAME"]
			dbpassword = optionmap["MYSQL_PASSWORD"]
			var err error
			if v, ok := optionmap["KMS_KEY"]; ok {
				dbpassword, err = a.DecryptPassword(&v, dbpassword)
			} else if _, ok := optionmap[apistructs.AddonPasswordHasEncripy]; ok {
				dbpassword, err = a.DecryptPassword(nil, dbpassword)
			}
			if err != nil {
				continue
			}
		}
		ins, err := a.db.GetAddonInstance(routing.RealInstance)
		if err != nil || ins == nil {
			continue
		}
		configmap := map[string]interface{}{}
		if err := json.Unmarshal([]byte(ins.Config), &configmap); err != nil {
			continue
		}
		if configmap["MYSQL_HOST"] != nil && configmap["MYSQL_PORT"] != nil {
			host, ok1 := configmap["MYSQL_HOST"].(string)
			port, ok2 := configmap["MYSQL_PORT"].(string)
			if !ok1 || !ok2 {
				continue
			}
			address = fmt.Sprintf("%s:%s", host, port)
		}
		if address != "" && dbusername != "" && dbpassword != "" {
			return map[string]string{
				"canal.instance.master.address": address,
				"canal.instance.dbUsername":     dbusername,
				"canal.instance.dbPassword":     dbpassword,
			}
		}
	}
	for _, ins := range instances {
		var (
			address    string
			dbusername string
			dbpassword string
		)
		if ins.AddonName != "mysql" && ins.AddonName != "alicloud-rds" {
			continue
		}
		configmap := map[string]interface{}{}
		if err := json.Unmarshal([]byte(ins.Config), &configmap); err != nil {
			continue
		}
		if configmap["MYSQL_HOST"] != nil &&
			configmap["MYSQL_PORT"] != nil &&
			configmap["MYSQL_PASSWORD"] != nil &&
			configmap["MYSQL_USERNAME"] != nil {
			host, ok1 := configmap["MYSQL_HOST"].(string)
			port, ok2 := configmap["MYSQL_PORT"].(string)
			if !ok1 || !ok2 {
				logrus.Errorf("guesscanaloption: MYSQL_HOST: %v, MYSQL_PORT: %v", ok1, ok2)
				continue
			}
			address = fmt.Sprintf("%s:%s", host, port)
			dbusername = configmap["MYSQL_USERNAME"].(string)
			dbpassword = configmap["MYSQL_PASSWORD"].(string)
			var err error
			if ins.KmsKey != "" {
				dbpassword, err = a.DecryptPassword(&ins.KmsKey, dbpassword)
			} else if _, ok := configmap[apistructs.AddonPasswordHasEncripy]; ok {
				dbpassword, err = a.DecryptPassword(nil, dbpassword)
			}
			if err != nil {
				logrus.Errorf("DecryptPassword: %v", err)
				continue
			}
			if address != "" && dbusername != "" && dbpassword != "" {
				return map[string]string{
					"canal.instance.master.address": address,
					"canal.instance.dbUsername":     dbusername,
					"canal.instance.dbPassword":     dbpassword,
				}
			}
		}
	}

	return nil
}

// buildCanalServiceItem 构造canal的service信息
func (a *Addon) BuildCanalServiceItem(params *apistructs.AddonHandlerCreateItem, addonIns *dbclient.AddonInstance,
	addonSpec *apistructs.AddonExtension, addonDice *diceyml.Object) error {
	if params.Options != nil && params.Options["mysql"] != "" {
		idorname := params.Options["mysql"]
		projectid, err := strconv.ParseUint(addonIns.ProjectID, 10, 64)
		if err != nil {
			return err
		}
		foundmysql := false
		routing, err := a.db.GetInstanceRouting(idorname)
		if err != nil {
			return err
		}
		if routing != nil {
			ins, err := a.db.GetAddonInstance(routing.RealInstance)
			if err != nil {
				return err
			}
			options := a.guessCanalAddr([]dbclient.AddonInstanceRouting{*routing}, []dbclient.AddonInstance{*ins})
			if len(options) == 0 {
				return fmt.Errorf("未找到配置的 mysql 信息")
			}
			for k, v := range options {
				params.Options[k] = v
			}
			foundmysql = true
		} else {
			addoninsList, err := a.db.ListAddonInstancesByProjectIDs([]uint64{projectid})
			if err != nil {
				return err
			}
			for _, addon := range *addoninsList {
				if addon.Name == idorname {
					routings, err := a.db.GetInstanceRoutingByRealInstance(addon.ID)
					if err != nil {
						return err
					}
					if len(*routings) != 1 {
						continue
					}
					routing := (*routings)[0]
					options := a.guessCanalAddr([]dbclient.AddonInstanceRouting{routing}, []dbclient.AddonInstance{addon})
					if len(options) == 0 {
						continue
					}
					for k, v := range options {
						params.Options[k] = v
					}
					foundmysql = true
					break
				}
			}
		}
		if !foundmysql {
			return fmt.Errorf("未找到匹配的mysql")
		}
	} else if params.Options == nil || params.Options["canal.instance.master.address"] == "" || params.Options["canal.instance.dbUsername"] == "" ||
		params.Options["canal.instance.dbPassword"] == "" {
		// guess mysql info here
		runtimeid, err := strconv.ParseUint(params.RuntimeID, 10, 64)
		if err != nil {
			return fmt.Errorf("failed to parse runtimeid(%s):%v", params.RuntimeID, err)
		}
		instanceroutings, err := a.ListInstanceRoutingByRuntime(runtimeid)
		if err != nil {
			return err
		}
		instances, err := a.ListInstanceByRuntime(runtimeid)
		if err != nil {
			return err
		}
		addroptions := a.guessCanalAddr(*instanceroutings, instances)
		if len(addroptions) == 0 {
			return fmt.Errorf("未设置 canal 参数")
		} else {
			a.pushLog(fmt.Sprintf("自动获取 canal 参数: %+v", addroptions), params)
		}
		if params.Options == nil {
			params.Options = map[string]string{}
		}
		for k, v := range addroptions {
			params.Options[k] = v
		}
	}

	addonDeployPlan := addonSpec.Plan[params.Plan]
	serviceMap := diceyml.Services{}
	for i := 1; i <= addonDeployPlan.Nodes; i++ {
		// 从dice.yml中取出对应addon信息
		serviceItem := *addonDice.Services[addonSpec.Name]
		// Resource资源
		serviceItem.Resources = diceyml.Resources{CPU: addonDeployPlan.CPU, MaxCPU: addonDeployPlan.MaxCPU, Mem: addonDeployPlan.Mem, MaxMem: addonDeployPlan.MaxMem}
		// label
		if len(serviceItem.Labels) == 0 {
			serviceItem.Labels = map[string]string{}
		}
		serviceItem.Labels["ADDON_GROUP_ID"] = addonSpec.Name
		SetlabelsFromOptions(params.Options, serviceItem.Labels)
		// volume信息
		//serviceItem.Binds = diceyml.Binds{"canal-data:/usr/share/canal/logs:rw"}

		//  /usr/share/canal/logs volume
		vol01 := SetAddonVolumes(params.Options, "/usr/share/canal/logs", false)
		serviceItem.Volumes = diceyml.Volumes{vol01}
		// envs
		heapSize := fmt.Sprintf("%.f", float64(addonDeployPlan.Mem)*0.7)
		serviceItem.Envs = map[string]string{
			"ADDON_ID":                      addonIns.ID,
			"ADDON_NODE_ID":                 a.getRandomId(),
			"JAVA_OPTS":                     strings.Join([]string{"-Xms", heapSize, "m -Xmx", heapSize, "m"}, ""),
			"canal.instance.mysql.slaveId":  "123" + strconv.Itoa(i),
			"canal.destinations":            "example",
			"canal.instance.master.address": params.Options["canal.instance.master.address"],
			"canal.instance.dbUsername":     params.Options["canal.instance.dbUsername"],
			"canal.instance.dbPassword":     params.Options["canal.instance.dbPassword"],
			"canal.auto.scan":               "false",
			"instance.connectionCharset":    "UTF-8",
		}

		// 设置service
		serviceMap[strings.Join([]string{addonSpec.Name, strconv.Itoa(i)}, "-")] = &serviceItem
	}
	addonDice.Services = serviceMap

	return nil
}

// BuildRabbitmqServiceItem 构造rabbitmq的service信息
func (a *Addon) BuildRabbitmqServiceItem(params *apistructs.AddonHandlerCreateItem, addonIns *dbclient.AddonInstance,
	addonSpec *apistructs.AddonExtension, addonDice *diceyml.Object) error {
	cc, err := json.Marshal(*addonSpec)
	if err != nil {
		return err
	}
	logrus.Infof("rabbitmq addon spec: " + string(cc))

	addonDeployPlan := addonSpec.Plan[params.Plan]

	// rabbitmq servers列表
	rabbitServers := make([]string, 0, addonDeployPlan.Nodes)
	for i := 1; i <= addonDeployPlan.Nodes; i++ {
		rabbitServers = append(rabbitServers, strings.Join([]string{addonSpec.Name, strings.ToUpper("@${" + addonSpec.Name + "_" + strconv.Itoa(i) + "_HOST}")}, ""))
	}
	password, err := a.savePassword(addonIns, apistructs.AddonRabbitmqPasswordKey)
	if err != nil {
		return err
	}
	userName := "admin"
	erlangCookie := random.String(16)
	serviceMap := diceyml.Services{}

	logrus.Infof("rabbitmq addonDeployPlan.Nodes: " + strconv.Itoa(addonDeployPlan.Nodes))

	for i := 1; i <= addonDeployPlan.Nodes; i++ {
		// 从dice.yml中取出对应addon信息
		serviceItem := *addonDice.Services[addonSpec.Name]
		// Resource资源
		serviceItem.Resources = diceyml.Resources{CPU: addonDeployPlan.CPU, MaxCPU: addonDeployPlan.MaxCPU, Mem: addonDeployPlan.Mem, MaxMem: addonDeployPlan.MaxMem}
		// label
		if len(serviceItem.Labels) == 0 {
			serviceItem.Labels = map[string]string{}
		}
		serviceItem.Labels["ADDON_GROUP_ID"] = addonSpec.Name + "-" + strconv.Itoa(i)
		SetlabelsFromOptions(params.Options, serviceItem.Labels)

		// volume信息
		nodeId := a.getRandomId()
		//serviceItem.Binds = diceyml.Binds{nodeId + "-mq-data:/var/lib/rabbitmq:rw"}

		//  /var/lib/rabbitmq volume
		vol01 := SetAddonVolumes(params.Options, "/var/lib/rabbitmq", false)
		serviceItem.Volumes = diceyml.Volumes{vol01}

		// envs
		heapSize := getHeapSize(addonDeployPlan.Mem)
		serviceItem.Envs = map[string]string{
			"ADDON_ID":               addonIns.ID,
			"ADDON_NODE_ID":          nodeId,
			"JAVA_OPTS":              strings.Join([]string{"-Xms", heapSize, "m -Xmx", heapSize, "m"}, ""),
			"RABBITMQ_ERLANG_COOKIE": erlangCookie,
			"RABBITMQ_NODENAME":      strings.Join([]string{addonSpec.Name, strings.ToUpper("@${" + addonSpec.Name + "_" + strconv.Itoa(i) + "_HOST}")}, ""),
			"RABBITMQ_DEFAULT_USER":  userName,
			"RABBITMQ_DEFAULT_PASS":  password,
		}

		if addonDeployPlan.Nodes > 1 {
			serviceItem.Envs["CLUSTER_NODE"] = strings.Join(rabbitServers, ";")
		}

		// 设置service
		serviceMap[strings.Join([]string{addonSpec.Name, strconv.Itoa(i)}, "-")] = &serviceItem
	}
	addonDice.Services = serviceMap
	bb, err := json.Marshal(*addonDice)
	if err != nil {
		return err
	}
	logrus.Infof("rabbitmq request body: " + string(bb))
	return nil
}

// BuildCommonServiceItem 构造标准化的addon发布service body
func (a *Addon) BuildCommonServiceItem(params *apistructs.AddonHandlerCreateItem, addonIns *dbclient.AddonInstance,
	addonSpec *apistructs.AddonExtension, addonDice *diceyml.Object, clusterInfo *apistructs.ClusterInfoData) error {

	// 获取inside addon信息
	insideAddonIns, err := a.db.GetByOutSideInstanceID(addonIns.ID)
	if err != nil {
		return err
	}
	insideConfigMap := make(map[string]string)
	// 若inside addon不为空，则取inside addon的环境变量
	if len(*insideAddonIns) > 0 {
		insideIds := make([]string, 0, len(*insideAddonIns))
		for _, insideId := range *insideAddonIns {
			insideIds = append(insideIds, insideId.InsideInstanceID)
		}
		instances, err := a.db.GetInstancesByIDs(insideIds)
		if err != nil {
			return err
		}
		for _, insideIns := range *instances {
			if insideIns.Config != "" {
				var configMapItem map[string]interface{}
				if err := json.Unmarshal([]byte(insideIns.Config), &configMapItem); err != nil {
					logrus.Errorf("BuildCommonServiceItem inside addon config unmarshal failed, %v", insideIns.Config)
					return err
				}
				for k, v := range configMapItem {
					switch t := v.(type) {
					case string:
						insideConfigMap[k] = t
					default:
						insideConfigMap[k] = fmt.Sprintf("%v", t)
					}
				}
			}
		}
	}
	//设置环境变量
	if len(insideConfigMap) > 0 {
		for _, v := range addonDice.Services {
			v.Envs = insideConfigMap
		}
	}

	return nil
}

// getHeapSize 计算addon中jvm大小
func getHeapSize(mem int) string {
	if mem <= 1024 {
		return fmt.Sprintf("%.f", float64(mem)*0.5)
	}
	if mem > 1024 && mem <= 4096 {
		return fmt.Sprintf("%.f", float64(mem)*0.7)
	}
	if mem > 4096 {
		return fmt.Sprintf("%.f", float64(mem)*0.8)
	}
	return "0"
}

// 比较大小int
func Min(x, y int) int {
	if x < y {
		return x
	}
	return y
}

// optional: kmskey
func (a *Addon) DecryptPassword(kmskey *string, password string) (string, error) {
	if kmskey == nil {
		return a.encrypt.DecryptPassword(password)
	}
	decryptData, err := a.bdl.KMSDecrypt(apistructs.KMSDecryptRequest{
		DecryptRequest: kmstypes.DecryptRequest{
			KeyID:            *kmskey,
			CiphertextBase64: password,
		},
	})
	if err != nil {
		return "", err
	}
	decodePasswordStr, err := base64.StdEncoding.DecodeString(decryptData.PlaintextBase64)
	if err != nil {
		return "", err
	}
	return string(decodePasswordStr), nil
}

// savePassword 保存密码
func (a *Addon) savePassword(addonIns *dbclient.AddonInstance, key string) (string, error) {
	password := random.String(16)

	// encrypt
	encryptData, err := a.bdl.KMSEncrypt(apistructs.KMSEncryptRequest{
		EncryptRequest: kmstypes.EncryptRequest{
			KeyID:           addonIns.KmsKey,
			PlaintextBase64: base64.StdEncoding.EncodeToString([]byte(password)),
		},
	})
	if err != nil {
		return "", err
	}
	// 保存password信息
	addonInstanceExtra := dbclient.AddonInstanceExtra{
		ID:         a.getRandomId(),
		InstanceID: addonIns.ID,
		Field:      key,
		Value:      encryptData.CiphertextBase64,
		Deleted:    apistructs.AddonNotDeleted,
	}
	err = a.db.CreateAddonInstanceExtra(&addonInstanceExtra)
	if err != nil {
		return "", err
	}
	return password, nil
}

// setAddonVolumes set Volumes for diceyml.Service for addon service
// service： addon service
// options:  主要获取如下三种 options
//     "addonDiskType": "SSD"
//     "addonVolumeSize": "10"
//     "addonSnapMaxHistory": "3"
// targetPath：容器中卷映射目录
// readOnly： 表明卷是否以只读方式挂载
func SetAddonVolumes(options map[string]string, targetPath string, readOnly bool) diceyml.Volume {
	snap := diceyml.VolumeSnapshot{
		MaxHistory: 0,
	}

	volume := diceyml.Volume{
		//SourcePath: hostPath,
		TargetPath: targetPath,
		ReadOnly:   readOnly,
		Snapshot:   &snap,
	}

	// Type
	if dt, ok := options[diceyml.AddonDiskType]; ok {
		volume.Type = dt
	} else {
		volume.Type = ""
	}

	// Capacity
	if ds, ok := options[diceyml.AddonVolumeSize]; ok {
		dsize, _ := strconv.Atoi(ds)
		// 卷容量最大限制 32768Gi
		if int32(dsize) >= diceyml.AddonVolumeSizeMin && int32(dsize) < diceyml.AddonVolumeSizeMax {
			volume.Capacity = int32(dsize)
		}

		// 小于 0，修正为默认值
		if int32(dsize) < diceyml.AddonVolumeSizeMin {
			volume.Capacity = diceyml.AddonVolumeSizeMin
		}
		// 大于 32768，修正为 32768
		if int32(dsize) >= diceyml.AddonVolumeSizeMax {
			volume.Capacity = diceyml.AddonVolumeSizeMax
		}
	}

	// Snapshot
	if sc, ok := options[diceyml.AddonSnapMaxHistory]; ok {
		snapshotCount, _ := strconv.Atoi(sc)
		// 卷快照数量最大限制 100
		if snapshotCount > 0 && int32(snapshotCount) < diceyml.AddonVolumeSnapshotMax {
			volume.Snapshot.MaxHistory = int32(snapshotCount)
		}
	}

	return volume
}

// SetlabelsFromOptions set service labels from addon.Optons
func SetlabelsFromOptions(options, labels map[string]string) {
	// addons[xxx].options 中的部分 Option 创建为 labels
	for k, v := range options {
		switch k {
		// some options may be is not a valid value for labels, so only set some predefined options
		case diceyml.AddonDiskType, diceyml.AddonVolumeSize, diceyml.AddonSnapMaxHistory, apistructs.AlibabaECILabel:
			labels[k] = v
			if k == apistructs.AlibabaECILabel && v == "true" {
				registry := os.Getenv(diceyml.ErdaImageRegistry)
				if registry != "" {
					// 从 orchestraor 的环境变量获取 addon 组件的镜像仓库
					labels[diceyml.AddonImageRegistry] = registry
				} else {
					labels[diceyml.AddonImageRegistry] = diceyml.AddonPublicRegistry
				}
			}
		}
	}
}
