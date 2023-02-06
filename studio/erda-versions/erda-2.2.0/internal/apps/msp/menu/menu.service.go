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
	"encoding/json"
	"fmt"
	"os"
	"strings"

	"github.com/ahmetb/go-linq/v3"

	"github.com/erda-project/erda-proto-go/msp/menu/pb"
	tenantpb "github.com/erda-project/erda-proto-go/msp/tenant/pb"
	"github.com/erda-project/erda/bundle"
	instancedb "github.com/erda-project/erda/internal/apps/msp/instance/db"
	"github.com/erda-project/erda/internal/apps/msp/menu/db"
	"github.com/erda-project/erda/pkg/common/errors"
)

type menuService struct {
	p                *provider
	db               *db.MenuConfigDB
	instanceTenantDB *instancedb.InstanceTenantDB
	instanceDB       *instancedb.InstanceDB
	bdl              *bundle.Bundle
	version          string
}

var NotExist = map[string]bool{
	//"LogAnalyze":      true,
	//"APIGateway":      true,
	//"RegisterCenter":  true,
	//"ConfigCenter":    true,
	//"AlarmManagement": true,
	//"AlertCenter":   true,
	"ServiceManage": true,
}

var DopMenu = map[string]bool{
	"MonitorCenter":  true,
	"ServiceManage":  true,
	"EnvironmentSet": true,
}

type componentInfo struct {
	cnName string
	enName string
}

var ComponentInfo = map[string]*componentInfo{
	"MonitorCenter": {
		enName: "MonitorCenter",
		cnName: "应用监控",
	},
	"LogAnalyze": {
		enName: "LogAnalyze",
		cnName: "日志分析",
	},
	"APIGateway": {
		enName: "APIGateway",
		cnName: "API网关",
	},
	"RegisterCenter": {
		enName: "RegisterCenter",
		cnName: "注册中心",
	},
	"ConfigCenter": {
		enName: "ConfigCenter",
		cnName: "配置中心",
	},
}

var splitEDAS = strings.ToLower(os.Getenv("SPLIT_EDAS_CLUSTER_TYPE")) == "true"

//GetMenu api
func (s *menuService) GetMenu(ctx context.Context, req *pb.GetMenuRequest) (*pb.GetMenuResponse, error) {
	//监控中心保留服务监控，诊断分析保留链路追踪、错误分析
	// get menu items
	items, err := s.getMenuItems()
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	if req.Type == tenantpb.Type_MSP.String() {
		var mspItems []*pb.MenuItem
		for _, item := range items {
			if NotExist[item.Key] {
				continue
			}
			params := s.composeMSPMenuParams(req)
			item.Params = params
			for _, child := range item.Children {
				child.Params = params
			}
			mspItems = append(mspItems, item)
		}
		items = mspItems
	}

	// get cluster info
	if req.Type != tenantpb.Type_MSP.String() {
		clusterName, err := s.instanceTenantDB.GetClusterNameByTenantGroup(req.TenantId)
		if err != nil {
			return nil, errors.NewDatabaseError(err)
		}
		if len(clusterName) <= 0 {
			return nil, errors.NewNotFoundError("TenantGroup.ClusterName")
		}
		clusterInfo, err := s.bdl.QueryClusterInfo(clusterName)
		if err != nil {
			return nil, errors.NewServiceInvokingError("QueryClusterInfo", err)
		}
		clusterType := clusterInfo.Get("DICE_CLUSTER_TYPE")

		menuMap := make(map[string]*pb.MenuItem)
		for _, item := range items {
			isK8s := clusterInfo.IsK8S() || (!splitEDAS && clusterInfo.IsEDAS())
			if DopMenu[item.Key] {
				for _, child := range item.Children {
					child.Params = item.Params
					// 反转exists字段，隐藏引导页，显示功能子菜单
					child.Exists = !child.Exists
					if child.OnlyK8S && !isK8s {
						child.Exists = false
					}
					if child.OnlyNotK8S && isK8s {
						child.Exists = false
					}
					if child.MustExists {
						child.Exists = true
					}
					child.IsK8S = isK8s
					child.IsEdas = clusterInfo.IsEDAS()
				}
			}
			item.ClusterName = clusterName
			item.ClusterType = clusterType
			item.IsK8S = isK8s
			item.IsEdas = clusterInfo.IsEDAS()
			for _, child := range item.Children {
				if len(child.Href) > 0 {
					child.Href = s.version + child.Href
				}
				menuMap[child.Key] = child
			}
			menuMap[item.Key] = item
		}

		configs, err := s.getEngineConfigs(req.TenantId, req.TenantId)
		if err != nil {
			return nil, err
		}
		for engine, config := range configs {
			menuKey, err := s.db.GetMicroServiceEngineKey(engine)
			if err != nil {
				return nil, errors.NewDatabaseError(err)
			}
			if len(menuKey) <= 0 {
				continue
			}
			item := menuMap[menuKey]
			if item == nil {
				return nil, errors.NewDatabaseError(fmt.Errorf("not found menu by key %q", menuKey))
			}

			// setup params
			item.Params = make(map[string]string)
			paramsStr := config["PUBLIC_HOST"]
			if len(paramsStr) > 0 {
				params := make(map[string]interface{})
				err := json.Unmarshal([]byte(paramsStr), &params)
				if err != nil {
					return nil, errors.NewDatabaseError(fmt.Errorf("PUBLIC_HOST format error"))
				}
				for k, v := range params {
					item.Params[k] = fmt.Sprint(v)
				}
			}
			if engine != "monitor" {
				item.Params["_enabled"] = "true"
			}

			// setup exists
			isK8s := clusterInfo.IsK8S() || (!splitEDAS && clusterInfo.IsEDAS())
			for _, child := range item.Children {
				child.Params = item.Params
				// 反转exists字段，隐藏引导页，显示功能子菜单
				child.Exists = !child.Exists
				if child.OnlyK8S && !isK8s {
					child.Exists = false
				}
				if child.OnlyNotK8S && isK8s {
					child.Exists = false
				}
				if child.MustExists {
					child.Exists = true
				}
				child.IsK8S = isK8s
				child.IsEdas = clusterInfo.IsEDAS()
			}
		}
	}

	if items == nil {
		items = make([]*pb.MenuItem, 0)
	}

	return &pb.GetMenuResponse{Data: s.adjustMenuParams(items)}, nil
}

func (s *menuService) composeMSPMenuParams(req *pb.GetMenuRequest) map[string]string {
	params := map[string]string{}
	params["key"] = "Overview"
	params["tenantGroup"] = req.TenantId
	params["tenantId"] = req.TenantId
	params["terminusKey"] = req.TenantId
	return params
}

// GetSetting api
func (s *menuService) GetSetting(ctx context.Context, req *pb.GetSettingRequest) (*pb.GetSettingResponse, error) {
	configs, err := s.getEngineConfigs(req.TenantGroup, req.TenantId)
	if err != nil {
		return nil, err
	}
	settings := make([]*pb.EngineSetting, 0)
	for engine, config := range configs {
		menuKey, err := s.db.GetMicroServiceEngineKey(engine)
		if err != nil {
			return nil, errors.NewDatabaseError(err)
		}
		if len(menuKey) <= 0 {
			continue
		}
		item := ComponentInfo[menuKey]
		if item == nil {
			return nil, errors.NewDatabaseError(fmt.Errorf("not found menu by key %q", menuKey))
		}
		if _, ok := config["PUBLIC_HOST"]; ok {
			delete(config, "PUBLIC_HOST")
		}
		settings = append(settings, &pb.EngineSetting{
			AddonName: engine,
			Config:    config,
			CnName:    item.cnName,
			EnName:    item.enName,
		})
	}
	return &pb.GetSettingResponse{Data: settings}, nil
}

func (s *menuService) getMenuItems() ([]*pb.MenuItem, error) {
	menuIni, err := s.db.GetMicroServiceMenu()
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	if menuIni == nil {
		return nil, nil
	}
	var list []*pb.MenuItem
	err = json.Unmarshal([]byte(menuIni.IniValue), &list)
	if err != nil {
		return nil, fmt.Errorf("microservice menu config format error")
	}
	return list, nil
}

func (s *menuService) getEngineConfigs(group, tenantID string) (map[string]map[string]string, error) {
	tenants, err := s.instanceTenantDB.GetByTenantGroup(group)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	if len(tenants) <= 0 {
		return nil, nil
	}
	configs := make(map[string]map[string]string)
	for _, tenant := range tenants {
		// 针对配置中心一个tenantGroup下有多个tenantId的情况，tenantId与请求一致时，允许覆盖
		if configs[tenant.Engine] == nil || tenant.ID == tenantID {
			instance, err := s.instanceDB.GetByID(tenant.InstanceID)
			if err != nil {
				return nil, errors.NewDatabaseError(err)
			}
			if instance == nil {
				return nil, errors.NewDatabaseError(fmt.Errorf("fail to find instance by id=%s", tenant.InstanceID))
			}
			config := make(map[string]string)
			if len(instance.Config) > 0 {
				instanceConfig := make(map[string]interface{})
				err = json.Unmarshal([]byte(instance.Config), &instanceConfig)
				if err != nil {
					return nil, errors.NewDatabaseError(fmt.Errorf("fail to unmarshal instance config: %w", err))
				}
				for k, v := range instanceConfig {
					config[k] = fmt.Sprint(v)
				}
			}
			if len(tenant.Config) > 0 {
				tenantConfig := make(map[string]interface{})
				err = json.Unmarshal([]byte(tenant.Config), &tenantConfig)
				if err != nil {
					return nil, errors.NewDatabaseError(fmt.Errorf("fail to unmarshal tenant config: %w", err))
				}
				for k, v := range tenantConfig {
					config[k] = fmt.Sprint(v)
				}
			}

			// 兼容已经创建的日志分析租户，新创建的日志分析会有 PUBLIC_HOST
			if tenant.Engine == "log-analytics" {
				byts, _ := json.Marshal(map[string]interface{}{
					"tenantId":    tenant.ID,
					"tenantGroup": tenant.TenantGroup,
					"logKey":      tenant.ID,
					"key":         "LogQuery",
				})
				config["PUBLIC_HOST"] = string(byts)
			}
			configs[tenant.Engine] = config
		}
	}

	// if both log-analytics and log-service addon exists, use log-service config only
	if _, ok := configs["log-service"]; ok {
		delete(configs, "log-analytics")
	}

	return configs, nil
}

func (s *menuService) adjustMenuParams(items []*pb.MenuItem) []*pb.MenuItem {
	//var overview, monitor, loghub *pb.MenuItem
	var monitor, loghub *pb.MenuItem
	setParams := make([]*pb.MenuItem, 0)
	for _, item := range items {
		if item.Params == nil {
			setParams = append(setParams, item)
		}
		switch item.Key {
		case "MonitorCenter":
			monitor = item
		case "DiagnoseAnalyzer":
			loghub, _ = linq.From(item.Children).
				FirstWith(func(i interface{}) bool { return i.(*pb.MenuItem).Key == "LogAnalyze" }).(*pb.MenuItem)
		}
	}
	if monitor != nil {
		for _, item := range setParams {
			item.Params = monitor.Params
			for _, child := range item.Children {
				if child.Params != nil {
					continue
				}
				child.Params = monitor.Params
			}
		}
		if loghub != nil {
			if loghub.Params == nil {
				loghub.Params = make(map[string]string)
			}
			loghub.Params["terminusKey"] = monitor.Params["terminusKey"]
		}
	}
	return items
}
