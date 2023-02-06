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

package config

var ServerConf *ServerConfig
var LogConf *LogConfig

type LogConfig struct {
	AccessFile        string
	ErrorFile         string
	AccessLevel       string `default:"info"`
	ErrorLevel        string `default:"info"`
	FileMaxAge        int    `default:"48"`
	FileRotateInteval int    `default:"1"`
	PrettyPrint       bool   `default:"true"`
	ShowSQL           bool   `default:"true"`
}

type ServerConfig struct {
	DbDriver                 string `default:"mysql"`
	DbSources                []string
	TableNamePrefix          string   `default:"tb_"`
	UserId                   string   `default:"1100"`
	ListenAddr               string   `default:":8080"`
	KongDebug                bool     `default:"false"`
	KongDebugAddr            string   `default:"http://localhost:8001"`
	ReqTimeout               int      `default:"60"`
	RegisterSliceSize        int      `default:"10"`
	RegisterInterval         int      `default:"5"`
	TargetActiveOffline      bool     `default:"true"`
	ApiRegisterTimeout       int      `default:"15"`
	StaleTargetCheckInterval int      `default:"15"`
	StaleTargetKeepTime      int      `default:"900"`
	UnexpectDeployInterval   int      `default:"3600"`
	BuiltinPlugins           []string `default:""`
	NextUpstreams            string   `default:"error timeout http_429 non_idempotent"`
	OfflineEnv               []string `default:"dev,test,staging"`
	OfflineQps               int      `default:"2000"`
	SpotSendPort             int      `default:"7082"`
	SpotAddonName            string   `default:"ApiGateway"`
	SpotMetricName           string   `default:"application_http"`
	SpotTagsHeaderPrefix     string   `default:"terminus-request-bg-"`
	SpotHostIpKey            string   `default:"HOST_IP"`
	SpotInstanceKey          string   `default:"DICE_ADDON"`
	SubDomainSplit           string   `default:"-"`
	HasRouteInfo             bool     `default:"true"`
	UseAdminEndpoint         bool     `default:"false"`
	AoneAppName              string   `default:""`
	ClusterName              string   `default:""`
	ClusterUIType            string   `default:""`
	TenantGroupKey           string   `default:"58dcbf490ef3"`
	CenterDomainNameKeepList []string `default:"collector,gittar,hepa,openapi,soldier,uc,dice,uc-adaptor,nexus-sys,sonar-sys"`
	EdgeDomainNameKeepList   []string `default:"soldier,nexus-sys"`
}
