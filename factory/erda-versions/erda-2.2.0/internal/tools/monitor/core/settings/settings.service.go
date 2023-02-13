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

package settings

import (
	"context"
	"encoding/json"
	"fmt"
	"sort"
	"strconv"
	"strings"

	"github.com/jinzhu/gorm"
	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/core/monitor/settings/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/common/errors"
)

type globalSetting struct {
	OrgID     int    `gorm:"column:org_id"`
	OrgName   string `gorm:"column:org_name"`
	Namespace string `gorm:"column:namespace"`
	Group     string `gorm:"column:group"`
	Type      string `gorm:"column:type"`
	Unit      string `gorm:"column:unit"`
	Key       string `gorm:"column:key"`
	Value     string `gorm:"column:value"`
}

const (
	globalSettingTableName    = "sp_monitor_global_settings"
	globalSettingInsertUpdate = "INSERT INTO `sp_monitor_global_settings`" +
		"(`org_id`,`org_name`,`namespace`,`group`,`key`,`type`,`value`,`unit`) " +
		"VALUES(?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE `org_name`=VALUES(`org_name`),`type`=VALUES(`type`),`value`=VALUES(`value`),`unit`=VALUES(`unit`);"
)

type settingsService struct {
	p      *provider
	db     *gorm.DB
	cfgMap map[string]map[string]*configDefine
	bundle *bundle.Bundle
	t      i18n.Translator
}

func (s *settingsService) GetSettings(ctx context.Context, req *pb.GetSettingsRequest) (*pb.GetSettingsResponse, error) {
	var list []*globalSetting
	if len(req.Workspace) > 0 {
		req.Workspace = strings.ToLower(req.Workspace)
		if err := s.db.Table(globalSettingTableName).Where("`org_id`=? AND `namespace`=?", req.OrgID, req.Workspace).
			Find(&list).Error; err != nil {
			s.p.Log.Errorf("fail to load %s: %s", globalSettingTableName, err)
			return nil, errors.NewDatabaseError(err)
		}
	} else {
		if err := s.db.Table(globalSettingTableName).Where("`org_id`=?", req.OrgID).
			Find(&list).Error; err != nil {
			s.p.Log.Errorf("fail to load settings: %s", err)
			return nil, errors.NewDatabaseError(err)
		}
	}

	langs := apis.Language(ctx)
	cfg := s.getDefaultConfig(langs, req.Workspace)
	for _, item := range list {
		ns := cfg[item.Namespace]
		if ns == nil {
			ns := make(map[string]map[string]*pb.ConfigItem)
			cfg[item.Namespace] = ns
		}
		cg := ns[item.Group]
		if cg == nil {
			cg = make(map[string]*pb.ConfigItem)
			ns[item.Group] = cg
		}
		cg[item.Key] = &pb.ConfigItem{
			Key:   item.Key,
			Name:  item.Key,
			Type:  item.Type,
			Value: getValue(item.Type, item.Value),
			Unit:  item.Unit,
		}
	}

	result := make(map[string]*pb.ConfigGroups)
	for ns, groups := range cfg {
		result[ns] = &pb.ConfigGroups{}
		nscfg := s.cfgMap[ns]
		for group, gcfg := range groups {
			cg := &pb.ConfigGroup{
				Key:  group,
				Name: group,
			}
			for _, item := range gcfg {
				cg.Items = append(cg.Items, item)
			}
			sort.Slice(cg.Items, func(i, j int) bool {
				return cg.Items[i].Key < cg.Items[j].Key
			})
			if nscfg != nil {
				cd := nscfg[group]
				if cd != nil && cd.convert != nil {
					cg = cd.convert(langs, ns, cg)
				}
			}
			result[ns].Groups = append(result[ns].Groups, cg)
		}
		sort.Slice(result[ns].Groups, func(i, j int) bool {
			return result[ns].Groups[i].Key < result[ns].Groups[j].Key
		})
	}
	return &pb.GetSettingsResponse{Data: result}, nil
}

func (s *settingsService) PutSettings(ctx context.Context, req *pb.PutSettingsRequest) (*pb.PutSettingsResponse, error) {
	orgName, err := s.getOrgName(req.OrgID)
	if err != nil {
		return nil, errors.NewServiceInvokingError("org", err)
	}
	defCfg := s.getDefaultConfig(apis.Language(ctx), "")
	tx := s.db.Begin()
	for ns, groups := range req.Data {
		if groups == nil {
			continue
		}
		ns = strings.ToLower(ns)
		nscfg := s.cfgMap[ns]
		nsdef := defCfg[ns]
		if nscfg == nil || nsdef == nil {
			continue
		}
		for _, group := range groups.Groups {
			if group == nil {
				continue
			}
			gcfg := nscfg[group.Key]
			gdef := nsdef[group.Key]
			if gcfg == nil || gcfg.handler == nil || gdef == nil {
				continue
			}
			cfg := map[string]interface{}{}
			for _, item := range group.Items {
				cdef := gdef[item.Key]
				if cdef == nil || item.Value == nil {
					continue
				}
				val := item.Value.AsInterface()
				cfg[item.Key] = val
				byts, _ := json.Marshal(val)
				err := tx.Exec(globalSettingInsertUpdate, req.OrgID, orgName, ns, group.Key, item.Key, cdef.Type, string(byts), cdef.Unit).Error
				if err != nil {
					tx.Rollback()
					return nil, errors.NewDatabaseError(err)
				}
			}
			err := gcfg.handler(tx, req.OrgID, orgName, ns, group.Key, cfg)
			if err != nil {
				tx.Rollback()
				return nil, errors.NewInternalServerError(err)
			}
		}
	}
	if err := tx.Commit().Error; err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	return &pb.PutSettingsResponse{Data: "OK"}, nil
}

type configDefine struct {
	handler  func(tx *gorm.DB, orgID int64, orgName, ns, group string, keys map[string]interface{}) error
	defaults map[string]func(lang i18n.LanguageCodes) *pb.ConfigItem
	convert  func(lang i18n.LanguageCodes, ns string, gs *pb.ConfigGroup) *pb.ConfigGroup
}

func (s *settingsService) initConfigMap() {
	s.cfgMap = map[string]map[string]*configDefine{
		"dev": {
			"monitor": s.monitorConfigMap("dev"),
		},
		"test": {
			"monitor": s.monitorConfigMap("test"),
		},
		"staging": {
			"monitor": s.monitorConfigMap("staging"),
		},
		"prod": {
			"monitor": s.monitorConfigMap("prod"),
		},
		"general": {
			"monitor": s.monitorConfigMap("general"),
		},
	}
}

func (s *settingsService) getDefaultConfig(lang i18n.LanguageCodes, ns string) map[string]map[string]map[string]*pb.ConfigItem {
	result := map[string]map[string]map[string]*pb.ConfigItem{}
	if len(ns) > 0 {
		cfg := s.cfgMap[ns]
		if cfg == nil {
			return nil
		}
		nscfg := map[string]map[string]*pb.ConfigItem{}
		for group, cfg := range cfg {
			gcfg := map[string]*pb.ConfigItem{}
			for key, fn := range cfg.defaults {
				gcfg[key] = fn(lang)
			}
			nscfg[group] = gcfg
		}
		result[ns] = nscfg
	} else {
		for ns, cfg := range s.cfgMap {
			nscfg := map[string]map[string]*pb.ConfigItem{}
			for group, cfg := range cfg {
				gcfg := map[string]*pb.ConfigItem{}
				for key, fn := range cfg.defaults {
					gcfg[key] = fn(lang)
				}
				nscfg[group] = gcfg
			}
			result[ns] = nscfg
		}
	}
	return result
}

func getValue(typ string, value interface{}) *structpb.Value {
	switch typ {
	case "number":
		switch val := value.(type) {
		case string:
			v, err := strconv.Atoi(val)
			if err == nil {
				return structpb.NewNumberValue(float64(v))
			}
		case *structpb.Value:
			return getValue(typ, val.AsInterface())
		}
	}
	v, _ := structpb.NewValue(value)
	return v
}

func (s *settingsService) getOrgName(id int64) (string, error) {
	resp, err := s.bundle.GetOrg(int(id))
	if err != nil {
		return "", fmt.Errorf("fail to get orgName: %s", err)
	}
	if resp == nil {
		return "", fmt.Errorf("org %d not exist", id)
	}
	return resp.Name, nil
}
