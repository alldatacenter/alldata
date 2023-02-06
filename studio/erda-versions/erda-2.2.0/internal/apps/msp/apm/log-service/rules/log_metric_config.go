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

package rules

import (
	"context"
	"encoding/json"

	"github.com/recallsong/go-utils/encoding/md5x"
	uuid "github.com/satori/go.uuid"
	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/log-service/analysis/processors"
	"github.com/erda-project/erda/internal/apps/msp/apm/log-service/rules/db"
	"github.com/erda-project/erda/internal/pkg/mysql"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric"
)

// ListLogMetricConfig .
func (p *provider) ListLogMetricConfig(scope, scopeID string) ([]*LogMetricConfigSimple, error) {
	var result []*LogMetricConfigSimple
	list, err := p.db.LogMetricConfig.QueryByScope(scope, scopeID)
	if err != nil {
		return nil, err
	}
	for _, item := range list {
		result = append(result, (&LogMetricConfigSimple{}).FromModel(item))
	}
	return result, nil
}

// GetLogMetricConfig .
func (p *provider) GetLogMetricConfig(scope, scopeID string, id int64) (*LogMetricConfig, error) {
	c, err := p.db.LogMetricConfig.QueryByID(scope, scopeID, id)
	if err != nil {
		return nil, err
	}
	cfg := (&LogMetricConfig{}).FromModel(c)
	return cfg, nil
}

func (p *provider) makeIndex() string {
	return "log_" + md5x.SumString(uuid.NewV4().String()).String16()
}

// CreateLogMetricConfig .
func (p *provider) CreateLogMetricConfig(cfg *LogMetricConfig) (bool, error) {
	m := cfg.ToModel()
	m.Metric = p.makeIndex()
	meta, err := p.convertToMetricMeta(m)
	if err != nil {
		return false, err
	}
	err = p.db.LogMetricConfig.Insert(m)
	if err != nil {
		if mysql.IsUniqueConstraintError(err) {
			return true, err
		}
		return false, err
	}
	_, err = p.MetricMeta.RegisterMetricMeta(context.Background(), &pb.RegisterMetricMetaRequest{
		Scope:   cfg.Scope,
		ScopeID: cfg.ScopeID,
		Group:   "log_metrics",
		Meta:    meta,
	})
	if err != nil {
		return false, err
	}
	return false, nil
}

// EnableLogMetricConfig .
func (p *provider) EnableLogMetricConfig(scope, scopeID string, id int64, enable bool) error {
	return p.db.LogMetricConfig.Enable(scope, scopeID, id, enable)
}

// UpdateLogMetricConfig .
func (p *provider) UpdateLogMetricConfig(cfg *LogMetricConfig) (bool, error) {
	db := p.db.Begin()
	err := db.LogMetricConfig.Update(cfg.ToModel())
	if err != nil {
		db.Rollback()
		if mysql.IsUniqueConstraintError(err) {
			return true, err
		}
		return false, err
	}

	m, err := db.LogMetricConfig.QueryByID(cfg.Scope, cfg.ScopeID, cfg.ID)
	if err != nil {
		db.Rollback()
		return false, err
	}
	meta, err := p.convertToMetricMeta(m)
	if err != nil {
		db.Rollback()
		return false, err
	}
	_, err = p.MetricMeta.RegisterMetricMeta(context.Background(), &pb.RegisterMetricMetaRequest{
		Scope:   cfg.Scope,
		ScopeID: cfg.ScopeID,
		Group:   "log_metrics",
		Meta:    meta,
	})
	if err != nil {
		db.Rollback()
		return false, err
	}
	return false, db.Commit().Error
}

func (p *provider) convertToMetricMeta(cfg *db.LogMetricConfig) (*pb.MetricMeta, error) {
	m := metric.NewMeta()
	m.Name.Key = cfg.Metric
	m.Name.Name = cfg.Name
	for _, key := range []string{"dice_org_id", "dice_org_name",
		"dice_project_id", "dice_project_name",
		"dice_application_id", "dice_application_name",
		"dice_runtime_id", "dice_runtime_name",
		"dice_service_name", "_level",
		"org_id", "org_name",
		"project_id", "project_name",
		"application_id", "application_name",
		"runtime_id", "runtime_name", "service_name"} {
		m.Tags[key] = &pb.TagDefine{Key: key, Name: key}
	}

	m.Tags["dice_workspace"] = &pb.TagDefine{
		Key:  "dice_workspace",
		Name: "Workspace",
		Values: []*pb.ValueDefine{
			{
				Value: structpb.NewStringValue("dev"),
				Name:  "Develop",
			},
			{
				Value: structpb.NewStringValue("test"),
				Name:  "Test",
			},
			{
				Value: structpb.NewStringValue("staging"),
				Name:  "Staging",
			},
			{
				Value: structpb.NewStringValue("prod"),
				Name:  "Production",
			},
		},
	}
	var ps []*ProcessorConfig
	if err := json.Unmarshal([]byte(cfg.Processors), &ps); err == nil {
		for _, p := range ps {
			byts, _ := json.Marshal(p.Config)
			proc, err := processors.NewProcessor(cfg.Metric, p.Type, byts)
			if err == nil {
				keys := proc.Keys()
				for _, k := range keys {
					if len(k.Name) <= 0 {
						k.Name = k.Key
					}
					m.Fields[k.Key] = k
				}
			}
			break
		}
	}
	return m, nil
}

// DeleteLogMetricConfig .
func (p *provider) DeleteLogMetricConfig(scope, scopeID string, id int64) (string, error) {
	db := p.db.Begin()
	c, err := db.LogMetricConfig.QueryByID(scope, scopeID, id)
	if err != nil {
		db.Rollback()
		return "", err
	}
	err = db.LogMetricConfig.Delete(scope, scopeID, id)
	if err == nil && c != nil {
		_, err = p.MetricMeta.UnRegisterMetricMeta(context.Background(), &pb.UnRegisterMetricMetaRequest{
			Scope:      scope,
			ScopeID:    scopeID,
			Group:      "log_metrics",
			MetricName: c.Metric,
		})
		if err != nil {
			db.Rollback()
			return "", err
		}
	}
	if err != nil {
		db.Rollback()
		return "", err
	}
	return c.Name, db.Commit().Error
}
