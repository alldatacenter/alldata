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
	"math"
	"os"
	"strconv"
	"strings"
	"time"

	"github.com/jinzhu/gorm"
	"github.com/recallsong/go-utils/conv"
	"github.com/recallsong/go-utils/encoding/md5x"
	"github.com/recallsong/go-utils/reflectx"
	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/core/monitor/settings/pb"
	"github.com/erda-project/erda/pkg/common/errors"
	"github.com/erda-project/erda/pkg/router"
)

func (s *settingsService) monitorConfigMap(ns string) *configDefine {
	metricDays, logDays := 8, 7
	ttl := os.Getenv("METRIC_INDEX_TTL")
	if len(ttl) > 0 {
		d, err := time.ParseDuration(ttl)
		if err != nil {
			s.p.Log.Errorf("fail to parse metric ttl: %s", err)
		} else {
			metricDays = int(math.Ceil(d.Hours() / 24))
		}
	}
	ttl = os.Getenv("LOG_TTL")
	if len(ttl) > 0 {
		sed, err := strconv.ParseInt(ttl, 10, 64)
		if err != nil {
			s.p.Log.Errorf("fail to parse log ttl: %s", err)
		} else {
			const daySec = float64(24 * 60 * 60)
			logDays = int(math.Ceil(float64(sed) / daySec))
		}
	}
	cd := &configDefine{
		handler: s.updateMonitorConfig,
	}

	if ns == "general" {
		cd.defaults = map[string]func(langs i18n.LanguageCodes) *pb.ConfigItem{
			"metrics_ttl": func(langs i18n.LanguageCodes) *pb.ConfigItem {
				return &pb.ConfigItem{
					Key:   "metrics_ttl",
					Name:  s.t.Text(langs, "base") + " " + s.t.Text(langs, "metrics_ttl"),
					Type:  "number",
					Value: structpb.NewNumberValue(float64(metricDays)),
					Unit:  s.t.Text(langs, "days"),
				}
			},
		}
		cd.convert = func(langs i18n.LanguageCodes, ns string, cg *pb.ConfigGroup) *pb.ConfigGroup {
			cg.Name = s.t.Text(langs, cg.Key)
			for _, item := range cg.Items {
				item.Name = s.t.Text(langs, item.Key)
				if item.Key == "metrics_ttl" {
					item.Name = s.t.Text(langs, "base") + " " + item.Name
				}
				item.Value = getValue(item.Type, item.Value)
			}
			return cg
		}
	} else {
		cd.defaults = map[string]func(lang i18n.LanguageCodes) *pb.ConfigItem{
			"logs_ttl": func(lang i18n.LanguageCodes) *pb.ConfigItem {
				return &pb.ConfigItem{
					Key:   "logs_ttl",
					Name:  s.t.Text(lang, "logs_ttl"),
					Type:  "number",
					Value: structpb.NewNumberValue(float64(logDays)),
					Unit:  s.t.Text(lang, "days"),
				}
			},
			"metrics_ttl": func(lang i18n.LanguageCodes) *pb.ConfigItem {
				return &pb.ConfigItem{
					Key:   "metrics_ttl",
					Name:  s.t.Text(lang, "metrics_ttl"),
					Type:  "number",
					Value: structpb.NewNumberValue(float64(metricDays)),
					Unit:  s.t.Text(lang, "days"),
				}
			},
		}
		cd.convert = func(lang i18n.LanguageCodes, ns string, cg *pb.ConfigGroup) *pb.ConfigGroup {
			cg.Name = s.t.Text(lang, cg.Key)
			for _, item := range cg.Items {
				item.Name = s.t.Text(lang, item.Key)
				if item.Key == "metrics_ttl" {
					item.Name = s.t.Text(lang, "app") + " " + item.Name
				}
				item.Value = getValue(item.Type, item.Value)
			}
			return cg
		}
	}
	return cd
}

const (
	monitorConfigRegisterTableName    = "sp_monitor_config_register"
	monitorConfigRegisterInsertUpdate = "INSERT INTO `sp_monitor_config_register`" +
		"(`scope`,`scope_id`,`namespace`,`type`,`names`,`filters`,`enable`,`update_time`,`desc`,`hash`) " +
		"VALUES(?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE `update_time`=VALUES(`update_time`),`enable`=VALUES(`enable`)"
	monitorConfigInsertUpdate = "INSERT INTO `sp_monitor_config`" +
		"(`org_id`,`org_name`,`type`,`names`,`filters`,`config`,`create_time`,`update_time`,`enable`,`key`,`hash`) " +
		"VALUES(?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE `names`=VALUES(`names`),`filters`=VALUES(`filters`),`config`=VALUES(`config`),`update_time`=VALUES(`update_time`),`enable`=VALUES(`enable`),`key`=VALUES(`key`);"
)

// monitorConfigRegister .
type monitorConfigRegister struct {
	Scope      string    `json:"scope" gorm:"column:scope"`
	ScopeID    string    `json:"scope_id" gorm:"column:scope_id"`
	Namespace  string    `json:"namespace" gorm:"column:namespace"`
	Type       string    `json:"type" gorm:"column:type"`
	Names      string    `json:"names" gorm:"column:names"`
	Filters    string    `json:"filters" gorm:"column:filters"`
	Enable     bool      `json:"enable" gorm:"column:enable"`
	UpdateTime time.Time `json:"update_time" gorm:"column:update_time"`
	Desc       string    `json:"desc" gorm:"column:desc"`
	Hash       string    `json:"hash" gorm:"column:hash"`
}

func (s *settingsService) updateMonitorConfig(tx *gorm.DB, orgid int64, orgName, ns, group string, keys map[string]interface{}) error {
	if ns == "general" {
		ns = ""
	}
	orgID := strconv.FormatInt(orgid, 10)
	key := md5x.SumString(orgID + "/" + ns).String16()
	for k, v := range keys {
		var typ string
		if k == "metrics_ttl" {
			typ = "metric"
		} else if k == "logs_ttl" {
			typ = "log"
		} else {
			continue
		}
		days := conv.ToInt64(v, -1)
		if days < 0 {
			return fmt.Errorf("invalid value %v for key %s", v, k)
		}
		var list []*monitorConfigRegister
		err := tx.Table(monitorConfigRegisterTableName).
			Where("`scope`='org' AND (`scope_id`=? OR `scope_id`='') AND `namespace`=? AND `type`=?", orgID, ns, typ).
			Find(&list).Error
		if err != nil {
			return fmt.Errorf("fail to get register monitor config: %s", err)
		}
		err = s.syncMonitorConfig(tx, orgid, orgID, orgName, ns, typ, key, list, days)
		if err != nil {
			return fmt.Errorf("fail to get sync monitor config: %s", err)
		}
	}
	return nil
}

func (s *settingsService) syncMonitorConfig(tx *gorm.DB, orgid int64, orgID, orgName, env, typ, key string, list []*monitorConfigRegister, days int64) (err error) {
	cfg := getConfigFromDays(days)
	now := time.Now()
	for _, item := range list {
		if len(item.ScopeID) <= 0 {
			item.Filters, err = insertOrgFilter(typ, orgID, orgName, item.Filters)
			if err != nil {
				s.p.Log.Error(err)
				return err
			}
		}
		hash := md5x.SumString(orgID + "," + env + "," + typ + "," + item.Names + item.Filters).String()
		// Update the actual configuration table
		err = tx.Exec(monitorConfigInsertUpdate, orgid, orgName, typ, item.Names, item.Filters, cfg, now, now, 1, key, hash).Error
		if err != nil {
			s.p.Log.Error(err)
			return err
		}
	}
	return nil
}

func insertOrgFilter(typ, orgID, orgName, filters string) (string, error) {
	var fs []*router.KeyValue
	err := json.Unmarshal(reflectx.StringToBytes(filters), &fs)
	if err != nil {
		return filters, fmt.Errorf("invalid monitor filters: %s, %s", filters, err)
	}
	if typ == "metric" {
		fs = append([]*router.KeyValue{
			{
				Key:   "org_name",
				Value: orgName,
			},
		}, fs...)
	} else if typ == "log" {
		fs = append([]*router.KeyValue{
			{
				Key:   "dice_org_name",
				Value: orgName,
			},
		}, fs...)
	}
	byts, _ := json.Marshal(fs)
	return string(byts), nil
}

func getConfigFromDays(days int64) string {
	c := struct {
		TTL string `json:"ttl"`
	}{
		TTL: time.Duration(days * 24 * int64(time.Hour)).String(),
	}
	byts, _ := json.Marshal(&c)
	return string(byts)
}

func (s *settingsService) RegisterMonitorConfig(ctx context.Context, req *pb.RegisterMonitorConfigRequest) (*pb.RegisterMonitorConfigResponse, error) {
	now := time.Now()
	tx := s.db.Begin()
	orgIDs := make(map[string]map[string]map[string][]*monitorConfigRegister)
	for _, item := range req.Data {
		if item == nil {
			continue
		}
		if len(item.Scope) <= 0 || len(item.Type) <= 0 {
			tx.Rollback()
			return nil, errors.NewMissingParameterError("scope or type")
		}
		mc := &monitorConfigRegister{
			Scope:     item.Scope,
			ScopeID:   item.ScopeID,
			Namespace: item.Namespace,
			Type:      item.Type,
			Names:     item.Names,
			Filters:   item.Filters,
			Enable:    item.Enable,
			Desc:      item.Desc,
		}
		if len(mc.Desc) <= 0 {
			mc.Desc = req.Desc
		}
		mc.Namespace = strings.ToLower(mc.Namespace)
		mc.Hash = md5x.SumString(mc.Scope + "," + mc.ScopeID + "," + mc.Type + "," + mc.Namespace + "," + mc.Names + mc.Filters).String()
		mc.UpdateTime = now
		err := tx.Exec(monitorConfigRegisterInsertUpdate, mc.Scope, mc.ScopeID, mc.Namespace, mc.Type, mc.Names, mc.Filters,
			mc.Enable, mc.UpdateTime, mc.Desc, mc.Hash).Error
		if err != nil {
			tx.Rollback()
			return nil, errors.NewDatabaseError(err)
		}
		if mc.Scope == "org" {
			envs, ok := orgIDs[mc.ScopeID]
			if !ok {
				envs = make(map[string]map[string][]*monitorConfigRegister)
				orgIDs[mc.ScopeID] = envs
			}
			typs, ok := envs[item.Namespace]
			if !ok {
				typs = make(map[string][]*monitorConfigRegister)
				envs[mc.Namespace] = typs
			}
			typs[mc.Type] = append(typs[mc.Type], mc)
		}
	}
	for orgID, envs := range orgIDs {
		orgid, err := strconv.ParseInt(orgID, 10, 64)
		if err != nil {
			continue
		}
		for env, typs := range envs {
			if env == "general" {
				env = ""
			}
			key := md5x.SumString(orgID + "/" + env).String16()
			for typ, list := range typs {
				var ck string
				if typ == "metric" {
					ck = "metrics_ttl"
				} else if typ == "log" {
					ck = "logs_ttl"
				} else {
					continue
				}
				var gs globalSetting
				err := tx.Table(globalSettingTableName).
					Where("`org_id`=? AND `namespace`=? AND `group`='monitor' AND `key`=?", orgid, env, ck).
					Find(&gs).Error
				if err != nil {
					if gorm.IsRecordNotFoundError(err) {
						continue
					}
					tx.Rollback()
					return nil, errors.NewDatabaseError(err)
				}
				days, err := strconv.ParseInt(gs.Value, 10, 64)
				if err != nil {
					s.p.Log.Errorf("fail to parse metric ttl to int: %s", err)
					continue
				}
				err = s.syncMonitorConfig(tx, orgid, orgID, gs.OrgName, env, typ, key, list, days)
				if err != nil {
					tx.Rollback()
					return nil, errors.NewDatabaseError(fmt.Errorf("fail to get sync monitor config: %s", err))
				}
			}
		}
	}
	if err := tx.Commit().Error; err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	return &pb.RegisterMonitorConfigResponse{Data: "OK"}, nil
}
