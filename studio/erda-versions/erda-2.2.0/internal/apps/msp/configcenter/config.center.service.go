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

package configcenter

import (
	"context"
	"encoding/json"
	"fmt"
	"strconv"

	"gopkg.in/yaml.v2"

	"github.com/erda-project/erda-proto-go/msp/configcenter/pb"
	"github.com/erda-project/erda/internal/apps/msp/configcenter/nacos"
	instancedb "github.com/erda-project/erda/internal/apps/msp/instance/db"
	"github.com/erda-project/erda/pkg/common/errors"
)

type PropertySource string

const (
	PropertySourceDeploy PropertySource = "DEPLOY"
	PropertySourceDice   PropertySource = "DICE"
)

type configCenterService struct {
	p                *provider
	instanceTenantDB *instancedb.InstanceTenantDB
	instanceDB       *instancedb.InstanceDB
}

func (s *configCenterService) GetGroups(ctx context.Context, req *pb.GetGroupRequest) (*pb.GetGroupResponse, error) {
	cfg, err := s.extractConfig(req.TenantID)
	if err != nil {
		return nil, err
	}
	keyword := "*"
	if len(req.Keyword) > 0 {
		keyword = "*" + req.Keyword + "*"
	}
	adp := newNacosAdapter(cfg)
	resp, err := adp.SearchConfig(nacos.SearchModeBlur, cfg.TenantName, keyword,
		"application.yml", int(req.PageNum), int(req.PageSize))
	if err != nil {
		return nil, errors.NewServiceInvokingError("nacos.SearchConfig", err)
	}
	if resp == nil {
		return &pb.GetGroupResponse{Data: &pb.Groups{}}, nil
	}
	return &pb.GetGroupResponse{
		Data: resp.ToConfigCenterGroups(),
	}, nil
}

func (s *configCenterService) GetGroupProperties(ctx context.Context, req *pb.GetGroupPropertiesRequest) (*pb.GetGroupPropertiesResponse, error) {
	cfg, err := s.extractConfig(req.TenantID)
	if err != nil {
		return nil, err
	}
	adp := newNacosAdapter(cfg)

	// get config
	var list []*nacos.ConfigItem
	for i, pages := 1, 1; i <= pages; i++ {
		resp, err := adp.SearchConfig(nacos.SearchModeAccurate, cfg.TenantName, req.GroupID, "", i, 100)
		if err != nil {
			return nil, errors.NewServiceInvokingError("nacos.SearchConfig", err)
		}
		if resp == nil {
			return nil, errors.NewNotFoundError("NacosConfig")
		}
		list = append(list, resp.ConfigItems...)
		if i == 1 {
			pages = int(resp.Pages)
		}
	}

	// convert config
	var props []*pb.Property
	for _, item := range list {
		if item.DataID == "application.yml" {
			prop := make(map[string]interface{})
			if len(item.Content) > 0 {
				if err = yaml.Unmarshal([]byte(item.Content), &prop); err != nil {
					return nil, errors.NewInternalServerError(fmt.Errorf("config format error: %w", err))
				}
			}
			for k, v := range prop {
				val, ok := v.(string)
				if ok {
					val = strconv.Quote(val)
				} else {
					val = fmt.Sprint(v)
				}
				props = append(props, &pb.Property{
					Key:    k,
					Value:  val,
					Source: string(PropertySourceDice),
				})
			}
		} else {
			props = append(props, &pb.Property{
				Key:    item.DataID,
				Value:  item.Content,
				Source: string(PropertySourceDeploy),
			})
		}
	}
	return &pb.GetGroupPropertiesResponse{
		Data: []*pb.GroupProperties{{
			Group:      req.GroupID,
			Properties: props,
		}},
	}, nil
}

func (s *configCenterService) SaveGroupProperties(ctx context.Context, req *pb.SaveGroupPropertiesRequest) (*pb.SaveGroupPropertiesResponse, error) {
	cfg, err := s.extractConfig(req.TenantID)
	if err != nil {
		return nil, err
	}
	adp := newNacosAdapter(cfg)
	data := make(map[string]string)
	for _, prop := range req.Properties {
		val, err := strconv.Unquote(prop.Value)
		if err == nil {
			prop.Value = val
		}
		if prop.Source == string(PropertySourceDice) {
			data[prop.Key] = prop.Value
		} else {
			err := adp.SaveConfig(cfg.TenantName, req.GroupID, prop.Key, prop.Value)
			if err != nil {
				return nil, errors.NewServiceInvokingError("nacos.SaveConfig", err)
			}
		}
	}
	byts, _ := yaml.Marshal(data)
	err = adp.SaveConfig(cfg.TenantName, req.GroupID, "application.yml", string(byts))
	if err != nil {
		return nil, errors.NewServiceInvokingError("nacos.SaveConfig", err)
	}
	return &pb.SaveGroupPropertiesResponse{Data: true}, nil
}

func (s *configCenterService) extractConfig(tenantID string) (*ConfigInfo, error) {
	tenant, err := s.instanceTenantDB.GetByID(tenantID)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	if tenant == nil {
		return nil, errors.NewNotFoundError(fmt.Sprintf("tenant/%s", tenantID))
	}
	instance, err := s.instanceDB.GetByID(tenant.InstanceID)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	if instance == nil {
		return nil, errors.NewNotFoundError(fmt.Sprintf("instance/%s", tenant.InstanceID))
	}
	return parseConfigInfo(instance, tenant), nil
}

// ConfigInfo .
type ConfigInfo struct {
	ClusterName string
	Host        string
	User        string
	Password    string
	TenantName  string
	GroupName   string
}

func parseConfigInfo(instance *instancedb.Instance, tenant *instancedb.InstanceTenant) *ConfigInfo {
	c := &ConfigInfo{
		ClusterName: instance.Az,
	}
	cfg, opts := make(map[string]interface{}), make(map[string]interface{})
	tcfg := make(map[string]interface{})
	json.Unmarshal([]byte(instance.Config), &cfg)
	json.Unmarshal([]byte(instance.Options), &opts)
	json.Unmarshal([]byte(tenant.Config), &tcfg)
	if host, ok := cfg["CONFIGCENTER_ADDRESS"].(string); ok {
		c.Host = host
	}
	if user, ok := opts["NACOS_USER"].(string); ok {
		c.User = user
	}
	if pwd, ok := opts["NACOS_PASSWORD"].(string); ok {
		c.Password = pwd
	}
	if name, ok := tcfg["CONFIGCENTER_TENANT_NAME"].(string); ok {
		c.TenantName = name
	}
	if name, ok := tcfg["CONFIGCENTER_GROUP_NAME"].(string); ok {
		c.GroupName = name
	}
	return c
}

func newNacosAdapter(cfg *ConfigInfo) *nacos.Adapter {
	return nacos.NewAdapter(cfg.ClusterName, cfg.Host, cfg.User, cfg.Password)
}
