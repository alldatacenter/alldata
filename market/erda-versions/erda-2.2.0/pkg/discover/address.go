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

package discover

import (
	"sort"
)

// 定义各个服务地址的环境变量配置名字.
const (
	EnvEventBox       = "EVENTBOX_ADDR"
	EnvCMDB           = "CMDB_ADDR"
	EnvScheduler      = "SCHEDULER_ADDR"
	EnvDiceHub        = "DICEHUB_ADDR"
	EnvSoldier        = "SOLDIER_ADDR"
	EnvOrchestrator   = "ORCHESTRATOR_ADDR"
	EnvAddOnPlatform  = "ADDON_PLATFORM_ADDR"
	EnvGittar         = "GITTAR_ADDR"
	EnvGittarAdaptor  = "GITTAR_ADAPTOR_ADDR"
	EnvCollector      = "COLLECTOR_ADDR"
	EnvMonitor        = "MONITOR_ADDR"
	EnvPipeline       = "PIPELINE_ADDR"
	EnvHepa           = "HEPA_ADDR"
	EnvCMP            = "CMP_ADDR"
	EnvOpenapi        = "OPENAPI_ADDR"
	EnvKMS            = "KMS_ADDR"
	EnvQA             = "QA_ADDR"
	EnvAPIM           = "APIM_ADDR"
	EnvTMC            = "TMC_ADDR" // TODO REMOVE
	EnvMSP            = "MSP_ADDR"
	EnvUC             = "UC_ADDR"
	EnvDOP            = "DOP_ADDR"
	EnvECP            = "ECP_ADDR"
	EnvClusterManager = "CLUSTER_MANAGER_ADDR"
	EnvClusterDialer  = "CLUSTER_DIALER_ADDR"
	EnvCoreServices   = "CORE_SERVICES_ADDR"
	EnvFDPMaster      = "FDP_MASTER_ADDR"
	EnvErdaServer     = "ERDA_SERVER_ADDR"
)

// 定义各个服务的 k8s svc 名称
const (
	SvcEventBox       = "eventbox"
	SvcCMDB           = "cmdb"
	SvcScheduler      = "scheduler"
	SvcDiceHub        = "dicehub"
	SvcSoldier        = "soldier"
	SvcOrchestrator   = "orchestrator"
	SvcAddOnPlatform  = "addon-platform"
	SvcGittar         = "gittar"
	SvcGittarAdaptor  = "gittar-adaptor"
	SvcCollector      = "collector"
	SvcMonitor        = "monitor"
	SvcPipeline       = "pipeline"
	SvcHepa           = "hepa"
	SvcCMP            = "cmp"
	SvcOpenapi        = "openapi"
	SvcKMS            = "addon-kms"
	SvcQA             = "qa"
	SvcAPIM           = "apim"
	SvcTMC            = "tmc"
	SvcMSP            = "msp"
	SvcUC             = "uc"
	SvcDOP            = "dop"
	SvcECP            = "ecp"
	SvcClusterManager = "cluster-manager"
	SvcClusterDialer  = "cluster-dialer"
	SvcCoreServices   = "core-services"
	SvcFDPMaster      = "fdp-master"
	SvcErdaServer     = "erda-server"
)

var ServicesEnvKeys = map[string]string{
	SvcCMDB:           EnvCMDB,
	SvcScheduler:      EnvScheduler,
	SvcDiceHub:        EnvDiceHub,
	SvcSoldier:        EnvSoldier,
	SvcOrchestrator:   EnvOrchestrator,
	SvcAddOnPlatform:  EnvAddOnPlatform,
	SvcGittar:         EnvGittar,
	SvcGittarAdaptor:  EnvGittarAdaptor,
	SvcCollector:      EnvCollector,
	SvcMonitor:        EnvMonitor,
	SvcPipeline:       EnvPipeline,
	SvcHepa:           EnvHepa,
	SvcCMP:            EnvCMP,
	SvcOpenapi:        EnvOpenapi,
	SvcKMS:            EnvKMS,
	SvcQA:             EnvQA,
	SvcAPIM:           EnvAPIM,
	SvcTMC:            EnvTMC,
	SvcMSP:            EnvMSP,
	SvcUC:             EnvUC,
	SvcDOP:            EnvDOP,
	SvcECP:            EnvECP,
	SvcClusterManager: EnvClusterManager,
	SvcClusterDialer:  EnvClusterDialer,
	SvcCoreServices:   EnvCoreServices,
	SvcFDPMaster:      EnvFDPMaster,
	SvcErdaServer:     EnvErdaServer,
}

func Services() []string {
	list := make([]string, 0, len(ServicesEnvKeys))
	for key := range ServicesEnvKeys {
		list = append(list, key)
	}
	sort.Strings(list)
	return list
}

func EventBox() string       { return getURL(SvcEventBox) }
func CMDB() string           { return getURL(SvcCMDB) }
func Scheduler() string      { return getURL(SvcScheduler) }
func DiceHub() string        { return getURL(SvcDiceHub) }
func Soldier() string        { return getURL(SvcSoldier) }
func Orchestrator() string   { return getURL(SvcOrchestrator) }
func AddOnPlatform() string  { return getURL(SvcAddOnPlatform) }
func Gittar() string         { return getURL(SvcGittar) }
func GittarAdaptor() string  { return getURL(SvcGittarAdaptor) }
func Collector() string      { return getURL(SvcCollector) }
func Monitor() string        { return getURL(SvcMonitor) }
func Pipeline() string       { return getURL(SvcPipeline) }
func Hepa() string           { return getURL(SvcHepa) }
func TMC() string            { return getURL(SvcTMC) }
func MSP() string            { return getURL(SvcMSP) }
func CMP() string            { return getURL(SvcCMP) }
func Openapi() string        { return getURL(SvcOpenapi) }
func KMS() string            { return getURL(SvcKMS) }
func QA() string             { return getURL(SvcQA) }
func APIM() string           { return getURL(SvcAPIM) }
func UC() string             { return getURL(SvcUC) }
func DOP() string            { return getURL(SvcDOP) }
func CoreServices() string   { return getURL(SvcCoreServices) }
func ClusterManager() string { return getURL(SvcClusterManager) }
func ClusterDialer() string  { return getURL(SvcClusterDialer) }

func getURL(srvName string) string {
	url, _ := GetEndpoint(srvName)
	return url
}
