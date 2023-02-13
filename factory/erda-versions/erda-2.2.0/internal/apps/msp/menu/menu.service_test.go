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

package menu

import (
	"context"
	"fmt"
	"reflect"
	"testing"
	"time"

	"bou.ke/monkey"
	"github.com/golang/mock/gomock"
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-proto-go/msp/menu/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/msp/instance/db"
	mdb "github.com/erda-project/erda/internal/apps/msp/menu/db"
)

////go:generate mockgen -destination=./menu_register_test.go -package exporter github.com/erda-project/erda-infra/pkg/transport Register
////go:generate mockgen -destination=./menu_logs_test.go -package exporter github.com/erda-project/erda-infra/base/logs Logger
func Test_menuService_GetMenu(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	logger := NewMockLogger(ctrl)
	register := NewMockRegister(ctrl)
	defer monkey.UnpatchAll()
	monkey.Patch((*menuService).composeMSPMenuParams, func(_ *menuService, _ *pb.GetMenuRequest) map[string]string {
		return map[string]string{
			"key":         "Overview",
			"tenantGroup": "010cf648c9f13887cf8d729a9e2453c9",
			"tenantId":    "010cf648c9f13887cf8d729a9e2453c9",
			"terminusKey": "terminusKey",
		}
	})
	var indb *db.InstanceTenantDB
	monkey.PatchInstanceMethod(reflect.TypeOf(indb), "GetClusterNameByTenantGroup",
		func(_ *db.InstanceTenantDB, _ string) (string, error) {
			return "terminus-dev", nil
		})
	var bdl *bundle.Bundle
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "QueryClusterInfo",
		func(_ *bundle.Bundle, _ string) (apistructs.ClusterInfoData, error) {
			return apistructs.ClusterInfoData{
				"DICE_CLUSTER_TYPE": "kubernetes",
			}, nil
		})
	clusterInfoData := apistructs.ClusterInfoData{}
	monkey.Patch(clusterInfoData.IsK8S, func() bool {
		return true
	})
	monkey.Patch((*menuService).getEngineConfigs, func(_ *menuService, _ string, _ string) (map[string]map[string]string, error) {
		return map[string]map[string]string{
			"monitor-collector": {
				"BOOTSTRAP_SERVERS":     "kafka-1.group-addon-monitor-kafka--sb7a6bfb737be49689fc782c2acd1db51.svc.cluster.local:9092,kafka-2.group-addon-monitor-kafka--sb7a6bfb737be49689fc782c2acd1db51.svc.cluster.local:9092,kafka-3.group-addon-monitor-kafka--sb7a6bfb737be49689fc782c2acd1db51.svc.cluster.local:9092",
				"MONITOR_LOG_COLLECTOR": "http://zbca863a6cd664fa19f18fc53f35d0275.addon-monitor-collector--zbca863a6cd664fa19f18fc53f35d0275.svc.cluster.local:7076/collect/logs/container",
			},
		}, nil
	})
	var menudb *mdb.MenuConfigDB
	monkey.PatchInstanceMethod(reflect.TypeOf(menudb), "GetMicroServiceEngineKey",
		func(_ *mdb.MenuConfigDB, _ string) (string, error) {
			return "RegisterCenter", nil
		})
	monkey.PatchInstanceMethod(reflect.TypeOf(menudb), "GetMicroServiceMenu",
		func(_ *mdb.MenuConfigDB) (*mdb.TmcIni, error) {
			return &mdb.TmcIni{
				ID:         13,
				IniName:    "MS_MENU",
				IniDesc:    "微服务治理菜单列表",
				IniValue:   "[{\"key\":\"Overview\",\"cnName\":\"全局拓扑\",\"enName\":\"Overview\",\"exists\":true,\"mustExists\":true},{\"key\":\"MonitorCenter\",\"cnName\":\"监控中心\",\"enName\":\"MonitorCenter\",\"children\":[{\"key\":\"ServiceMonitor\",\"cnName\":\"服务监控\",\"enName\":\"ServiceMonitor\",\"mustExists\":true,\"exists\":true},{\"key\":\"FrontMonitor\",\"cnName\":\"前端监控\",\"enName\":\"FrontMonitor\",\"onlyK8S\":true,\"onlyNotK8S\":false,\"exists\":false},{\"key\":\"ActiveMonitor\",\"cnName\":\"主动监控\",\"enName\":\"ActiveMonitor\",\"onlyK8S\":true,\"onlyNotK8S\":false,\"exists\":false}],\"exists\":true,\"mustExists\":true},{\"key\":\"AlertCenter\",\"cnName\":\"告警中心\",\"enName\":\"AlertCenter\",\"children\":[{\"key\":\"AlertStrategy\",\"cnName\":\"告警策略\",\"enName\":\"AlertStrategy\",\"exists\":true,\"mustExists\":true},{\"key\":\"AlarmHistory\",\"cnName\":\"告警历史\",\"enName\":\"AlarmHistory\",\"exists\":true,\"mustExists\":true},{\"key\":\"RuleManagement\",\"cnName\":\"规则管理\",\"enName\":\"RuleManagement\",\"exists\":true,\"mustExists\":true},{\"key\":\"NotifyGroupManagement\",\"cnName\":\"通知组管理\",\"enName\":\"NotifyGroupManagement\",\"exists\":true,\"mustExists\":true}],\"exists\":true,\"mustExists\":true},{\"key\":\"DiagnoseAnalyzer\",\"cnName\":\"诊断分析\",\"enName\":\"DiagnoseAnalyzer\",\"children\":[{\"key\":\"Tracing\",\"cnName\":\"链路追踪\",\"enName\":\"Tracing\",\"exists\":true,\"mustExists\":true},{\"key\":\"LogAnalyze\",\"cnName\":\"日志分析\",\"enName\":\"LogAnalyze\",\"exists\":true,\"mustExists\":true,\"onlyK8S\":true,\"onlyNotK8S\":false},{\"key\":\"ErrorInsight\",\"cnName\":\"错误分析\",\"enName\":\"ErrorInsight\",\"exists\":true,\"mustExists\":true},{\"key\":\"Dashboard\",\"cnName\":\"自定义大盘\",\"enName\":\"Dashboard\",\"exists\":true,\"mustExists\":true}],\"exists\":true,\"mustExists\":true},{\"key\":\"ServiceManage\",\"cnName\":\"服务治理\",\"enName\":\"ServiceManage\",\"children\":[{\"key\":\"APIGateway\",\"cnName\":\"API网关\",\"enName\":\"APIGateway\",\"exists\":true,\"mustExists\":true},{\"key\":\"RegisterCenter\",\"cnName\":\"注册中心\",\"enName\":\"RegisterCenter\",\"exists\":true,\"mustExists\":true},{\"key\":\"ConfigCenter\",\"cnName\":\"配置中心\",\"enName\":\"ConfigCenter\",\"exists\":true,\"mustExists\":true}],\"exists\":true,\"mustExists\":true},{\"key\":\"EnvironmentSet\",\"cnName\":\"环境设置\",\"enName\":\"EnvironmentSet\",\"children\":[{\"key\":\"AccessConfig\",\"cnName\":\"接入配置\",\"enName\":\"AccessConfig\",\"onlyK8S\":true,\"onlyNotK8S\":true,\"exists\":true},{\"key\":\"MemberManagement\",\"cnName\":\"成员管理\",\"enName\":\"MemberManagement\",\"onlyK8S\":true,\"onlyNotK8S\":true,\"exists\":true},{\"key\":\"ComponentInfo\",\"cnName\":\"组件信息\",\"enName\":\"ComponentInfo\",\"onlyK8S\":true,\"onlyNotK8S\":false,\"exists\":false}],\"exists\":true,\"mustExists\":true}]",
				CreateTime: time.Now(),
				UpdateTime: time.Now(),
				IsDeleted:  "N",
			}, nil
		})
	pro := &provider{
		Cfg:         &config{},
		Log:         logger,
		Register:    register,
		DB:          &gorm.DB{},
		Perm:        nil,
		MPerm:       nil,
		menuService: &menuService{},
		bdl:         &bundle.Bundle{},
	}
	pro.menuService.p = pro
	_, err := pro.menuService.GetMenu(context.Background(), &pb.GetMenuRequest{
		TenantId: "010cf648c9f13887cf8d729a9e2453c9",
		Type:     "MSP",
	})
	if err != nil {
		fmt.Println("should not err 1")
	}
	_, err = pro.menuService.GetMenu(context.Background(), &pb.GetMenuRequest{
		TenantId: "3bff0d6c179334ff186b0dbdb775174e",
		Type:     "DOP",
	})
	if err != nil {
		fmt.Println("should not err 2")
	}
}

func Test_menuService_GetSetting(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	logger := NewMockLogger(ctrl)
	register := NewMockRegister(ctrl)
	defer monkey.UnpatchAll()
	monkey.Patch((*menuService).getEngineConfigs, func(_ *menuService, _ string, _ string) (map[string]map[string]string, error) {
		return map[string]map[string]string{
			"monitor-collector": {
				"BOOTSTRAP_SERVERS":     "kafka-1.group-addon-monitor-kafka--sb7a6bfb737be49689fc782c2acd1db51.svc.cluster.local:9092,kafka-2.group-addon-monitor-kafka--sb7a6bfb737be49689fc782c2acd1db51.svc.cluster.local:9092,kafka-3.group-addon-monitor-kafka--sb7a6bfb737be49689fc782c2acd1db51.svc.cluster.local:9092",
				"MONITOR_LOG_COLLECTOR": "http://zbca863a6cd664fa19f18fc53f35d0275.addon-monitor-collector--zbca863a6cd664fa19f18fc53f35d0275.svc.cluster.local:7076/collect/logs/container",
			},
			"registercenter": {
				"ELASTICJOB_HOST":   "zookeeper.addon-zookeeper--f981d03b3b72e4386830a5b9c826b4014.svc.cluster.local:2181",
				"NACOS_ADDRESS":     "nacos.addon-nacos--r823ba8ce60a94db68dab45557547ada9.svc.cluster.local:8848",
				"ZOOKEEPER_ADDRESS": "zookeeper.addon-zookeeper--f981d03b3b72e4386830a5b9c826b4014.svc.cluster.local:2181",
			},
		}, nil
	})
	var menudb *mdb.MenuConfigDB
	monkey.PatchInstanceMethod(reflect.TypeOf(menudb), "GetMicroServiceEngineKey",
		func(_ *mdb.MenuConfigDB, _ string) (string, error) {
			return "RegisterCenter", nil
		})
	pro := &provider{
		Cfg:         &config{},
		Log:         logger,
		Register:    register,
		DB:          &gorm.DB{},
		Perm:        nil,
		MPerm:       nil,
		menuService: &menuService{},
		bdl:         &bundle.Bundle{},
	}
	pro.menuService.p = pro
	_, err := pro.menuService.GetSetting(context.Background(), &pb.GetSettingRequest{
		TenantId: "3bff0d6c179334ff186b0dbdb775174e",
	})
	if err != nil {
		fmt.Println("should not err")
	}
}
