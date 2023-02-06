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
	"encoding/json"
	"strings"
	"time"

	"github.com/recallsong/go-utils/reflectx"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/msp/apm/log-service/rules/db"
)

// LogMetricConfig .
type LogMetricConfig struct {
	ID         int64              `json:"id"`
	OrgID      int64              `json:"org_id"`
	Scope      string             `json:"scope"`
	ScopeID    string             `json:"Scope_id"`
	Name       string             `json:"name"`
	Metric     string             `json:"metric"`
	Filters    []*Tag             `json:"filters"`
	Processors []*ProcessorConfig `json:"processors"`
	Enable     bool               `json:"enable"`
	CreatorID  string             `json:"creator_id"`
	CreateTime int64              `json:"create_time"`
	UpdateTime int64              `json:"update_time"`
}

// Tag .
type Tag struct {
	Key   string `json:"key"`
	Value string `json:"value"`
}

// ProcessorConfig .
type ProcessorConfig struct {
	Type   string                 `json:"type"`
	Config map[string]interface{} `json:"config"`
}

type ListRulesResponse struct {
	LogData  []*LogMetricConfigSimple         `json:"logData"`
	UserInfo *apistructs.UserListResponseData `json:"userInfo"`
}

// FromModel .
func (c *LogMetricConfig) FromModel(m *db.LogMetricConfig) *LogMetricConfig {
	if len(m.Filters) > 0 {
		var tags []*Tag
		err := json.Unmarshal([]byte(m.Filters), &tags)
		if err == nil {
			c.Filters = tags
		}
	}
	if len(m.Processors) > 0 {
		var ps []*ProcessorConfig
		err := json.Unmarshal([]byte(m.Processors), &ps)
		if err == nil {
			c.Processors = ps
		}
	}
	c.ID = m.ID
	c.OrgID = m.OrgID
	c.Scope = m.Scope
	c.ScopeID = m.ScopeID
	c.Name = m.Name
	c.Metric = m.Metric
	c.Enable = m.Enable
	c.CreateTime = m.CreateTime.UnixNano() / int64(time.Millisecond)
	c.UpdateTime = m.UpdateTime.UnixNano() / int64(time.Millisecond)
	return c
}

// ToModel .
func (c *LogMetricConfig) ToModel() *db.LogMetricConfig {
	filters, _ := json.Marshal(c.Filters)
	processors, _ := json.Marshal(c.Processors)
	return &db.LogMetricConfig{
		ID:         c.ID,
		OrgID:      c.OrgID,
		Scope:      c.Scope,
		ScopeID:    c.ScopeID,
		Name:       c.Name,
		Metric:     c.Metric,
		Filters:    string(filters),
		Processors: string(processors),
		Enable:     c.Enable,
		CreatorID:  c.CreatorID,
		CreateTime: time.Unix(c.CreateTime/1000, (c.CreateTime%1000)*int64(time.Millisecond)),
		UpdateTime: time.Unix(c.UpdateTime/1000, (c.UpdateTime%1000)*int64(time.Millisecond)),
	}
}

// LogMetricConfigSimple .
type LogMetricConfigSimple struct {
	ID         int64  `json:"id"`
	OrgID      int64  `json:"org_id"`
	Scope      string `json:"scope"`
	ScopeID    string `json:"scope_id"`
	Name       string `json:"name"`
	Types      string `json:"types"`
	Metric     string `json:"metric"`
	Enable     bool   `json:"enable"`
	Creator    string `json:"creator"`
	CreateTime int64  `json:"create_time"`
	UpdateTime int64  `json:"update_time"`
}

// FromModel .
func (c *LogMetricConfigSimple) FromModel(m *db.LogMetricConfig) *LogMetricConfigSimple {
	c.ID = m.ID
	c.OrgID = m.OrgID
	c.Scope = m.Scope
	c.ScopeID = m.ScopeID
	c.Name = m.Name
	c.Metric = m.Metric
	c.Enable = m.Enable
	c.CreateTime = m.CreateTime.UnixNano() / int64(time.Millisecond)
	c.UpdateTime = m.UpdateTime.UnixNano() / int64(time.Millisecond)
	c.Creator = m.CreatorID
	// ProcessorConfig .
	type ProcessorConfig struct {
		Type string `json:"type"`
	}
	var processors []*ProcessorConfig
	err := json.Unmarshal(reflectx.StringToBytes(m.Processors), &processors)
	if err == nil {
		var typs []string
		for _, p := range processors {
			typs = append(typs, p.Type)
		}
		c.Types = strings.Join(typs, ",")
	}
	return c
}
