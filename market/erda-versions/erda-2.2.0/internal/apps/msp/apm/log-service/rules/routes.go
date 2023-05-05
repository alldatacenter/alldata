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
	"fmt"
	"net/http"
	"strconv"
	"time"

	"github.com/erda-project/erda-infra/pkg/strutil"
	"github.com/erda-project/erda-infra/providers/httpserver"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/msp/apm/log-service/analysis/processors"
	_ "github.com/erda-project/erda/internal/apps/msp/apm/log-service/analysis/processors/regex" //
	metrics "github.com/erda-project/erda/internal/tools/monitor/core/metric"
	api "github.com/erda-project/erda/pkg/common/httpapi"
)

func (p *provider) intRoutes(routes httpserver.Router) error {
	routes.GET("/api/logs/metric/:scope/rules/templates", p.listTemplates)
	routes.GET("/api/logs/metric/:scope/rules/templates/:name", p.getTemplates)
	routes.GET("/api/logs/metric/:scope/rules", p.listRules)
	routes.GET("/api/logs/metric/:scope/rules/:id", p.getRule)
	routes.POST("/api/logs/metric/:scope/rules", p.createRule)
	routes.PUT("/api/logs/metric/:scope/rules/:id", p.updateRule)
	routes.PUT("/api/logs/metric/:scope/rules/:id/state", p.enableRule)
	routes.DELETE("/api/logs/metric/:scope/rules/:id", p.deleteRule)
	routes.POST("/api/logs/metric/:scope/rules/test", p.testRule)
	return nil
}

func (p *provider) listTemplates(r *http.Request, params struct {
	Scope string `param:"scope"`
}) interface{} {
	return api.Success(p.ListConfigTemplate(params.Scope, api.Language(r)))
}

func (p *provider) getTemplates(r *http.Request, params struct {
	Name  string `param:"name" validate:"required"`
	Scope string `param:"scope"`
}) interface{} {
	return api.Success(p.GetConfigTemplate(params.Scope, params.Name, api.Language(r)))
}

func (p *provider) getOrgName(id int64) (string, error) {
	info, err := p.bdl.GetOrg(id)
	if err != nil {
		return "", err
	}
	return info.Name, nil
}

func (p *provider) getOrgScopeID(r *http.Request) (string, interface{}) {
	orgID := api.OrgID(r)
	orgid, err := strconv.ParseInt(orgID, 10, 64)
	if err != nil {
		return "", api.Errors.InvalidParameter("invalid Org-ID")
	}
	name, err := p.getOrgName(orgid)
	if err != nil {
		return "", api.Errors.Internal(err)
	}
	return name, nil
}

func (p *provider) listRules(r *http.Request, params struct {
	Scope   string `param:"scope"`
	ScopeID string `query:"scopeID"`
}) interface{} {
	if len(params.ScopeID) <= 0 {
		name, err := p.getOrgScopeID(r)
		if err != nil {
			return err
		}
		params.ScopeID = name
	}
	var userIDs []string
	list, err := p.ListLogMetricConfig(params.Scope, params.ScopeID)
	if err != nil {
		return api.Errors.Internal(err)
	}
	for _, v := range list {
		if v.Creator != "" {
			userIDs = append(userIDs, v.Creator)
		}
	}
	userIDs = strutil.DedupSlice(userIDs, true)
	userInfo, err := p.bdl.ListUsers(apistructs.UserListRequest{
		UserIDs: userIDs,
	})
	if err != nil {
		return api.Errors.Internal(err)
	}
	resp := ListRulesResponse{
		LogData:  list,
		UserInfo: userInfo,
	}
	return api.Success(resp)
}

func (p *provider) getRule(r *http.Request, params struct {
	Scope   string `param:"scope"`
	ScopeID string `query:"scopeID"`
	ID      int    `param:"id" validate:"gte=1"`
}) interface{} {
	if len(params.ScopeID) <= 0 {
		name, err := p.getOrgScopeID(r)
		if err != nil {
			return err
		}
		params.ScopeID = name
	}
	c, err := p.GetLogMetricConfig(params.Scope, params.ScopeID, int64(params.ID))
	if err != nil {
		return api.Errors.Internal(err)
	}
	return api.Success(c)
}

func (p *provider) createRule(r *http.Request, c LogMetricConfig, params struct {
	Scope   string `param:"scope"`
	ScopeID string `query:"scopeID"`
}) interface{} {
	orgID := api.OrgID(r)
	userID := api.UserID(r)
	orgid, err := strconv.ParseInt(orgID, 10, 64)
	if err != nil {
		return api.Errors.InvalidParameter("invalid Org-ID")
	}
	c.OrgID = orgid
	c.CreatorID = userID
	c.Scope = params.Scope
	if len(params.ScopeID) > 0 {
		c.ScopeID = params.ScopeID
	}
	if len(c.ScopeID) <= 0 {
		name, err := p.getOrgName(orgid)
		if err != nil {
			return api.Errors.Internal(err)
		}
		c.ScopeID = name
	}
	if err := p.checkLogConfig(&c); err != nil {
		return err
	}
	exist, err := p.CreateLogMetricConfig(&c)
	if err != nil {
		if exist {
			return api.Errors.AlreadyExists("name 已存在")
		}
		return api.Errors.Internal(err)
	}
	return api.Success("OK")
}

func (p *provider) checkLogConfig(c *LogMetricConfig) interface{} {
	if len(c.Name) <= 0 {
		return api.Errors.MissingParameter("name must not be empty")
	}
	if len(c.Processors) <= 0 {
		return api.Errors.MissingParameter("processors must not be empty")
	}
	for i, p := range c.Processors {
		if p == nil || len(p.Type) <= 0 || len(p.Config) <= 0 {
			return api.Errors.MissingParameter(fmt.Sprintf("invalid processors[%d]", i))
		}
		keys := p.Config["keys"]
		if keys == nil {
			return api.Errors.MissingParameter(fmt.Sprintf("keys must not empty in processors[%d]", i))
		}
		ks, ok := keys.([]interface{})
		if !ok {
			return api.Errors.InvalidParameter(fmt.Sprintf("invalid keys in processors[%d]", i))
		}
		keyset := make(map[string]bool)
		for _, item := range ks {
			kv, ok := item.(map[string]interface{})
			if !ok {
				return api.Errors.InvalidParameter(fmt.Sprintf("invalid keys in processors[%d]", i))
			}
			k := kv["key"]
			key, ok := k.(string)
			if !ok {
				return api.Errors.InvalidParameter(fmt.Sprintf("invalid keys in processors[%d]", i))
			}
			if keyset[key] {
				return api.Errors.AlreadyExists(fmt.Sprintf("key '%s'", key))
			}
			keyset[key] = true
		}
		p.Config["replaceKey"] = map[string]string{
			"level": "_level",
		}
	}
	return nil
}

func (p *provider) updateRule(r *http.Request, params struct {
	Scope   string `param:"scope"`
	ScopeID string `query:"scopeID"`
	ID      int    `param:"id" validate:"gte=1"`
}, c LogMetricConfig) interface{} {
	orgID := api.OrgID(r)
	orgid, err := strconv.ParseInt(orgID, 10, 64)
	if err != nil {
		return api.Errors.InvalidParameter("invalid Org-ID")
	}
	c.OrgID = orgid
	c.ID = int64(params.ID)
	c.Scope = params.Scope
	if len(params.ScopeID) > 0 {
		c.ScopeID = params.ScopeID
	}
	if len(c.ScopeID) <= 0 {
		name, err := p.getOrgName(orgid)
		if err != nil {
			return api.Errors.Internal(err)
		}
		c.ScopeID = name
	}
	if err := p.checkLogConfig(&c); err != nil {
		return err
	}
	exist, err := p.UpdateLogMetricConfig(&c)
	if err != nil {
		if exist {
			return api.Errors.AlreadyExists("name 已存在")
		}
		return api.Errors.Internal(err)
	}
	return api.Success("OK")
}

func (p *provider) enableRule(r *http.Request, params struct {
	Scope   string `param:"scope"`
	ScopeID string `query:"scopeID"`
	ID      int    `param:"id" validate:"gte=1"`
	Enable  bool   `query:"enable" json:"enable"`
}) interface{} {
	scope, scopeID := params.Scope, params.ScopeID
	if len(scopeID) <= 0 {
		name, err := p.getOrgScopeID(r)
		if err != nil {
			return err
		}
		scopeID = name
	}
	err := p.EnableLogMetricConfig(scope, scopeID, int64(params.ID), params.Enable)
	if err != nil {
		return api.Errors.Internal(err)
	}
	return api.Success("OK")
}

func (p *provider) deleteRule(r *http.Request, params struct {
	Scope   string `param:"scope"`
	ScopeID string `query:"scopeID"`
	ID      int    `param:"id" validate:"gte=1"`
}) interface{} {
	scope, scopeID := params.Scope, params.ScopeID
	if len(scopeID) <= 0 {
		name, err := p.getOrgScopeID(r)
		if err != nil {
			return err
		}
		scopeID = name
	}
	name, err := p.DeleteLogMetricConfig(scope, scopeID, int64(params.ID))
	if err != nil {
		return api.Errors.Internal(err)
	}
	return api.Success(name)
}

func (p *provider) testRule(params struct {
	Content    string             `json:"content"`
	MetricName string             `json:"metric_name"`
	Processors []*ProcessorConfig `json:"processors"`
}) interface{} {
	for _, p := range params.Processors {
		byts, err := json.Marshal(p.Config)
		if err != nil {
			return api.Errors.InvalidParameter("invalid processor", err.Error())
		}
		proc, err := processors.NewProcessor(params.MetricName, p.Type, byts)
		if err != nil {
			return api.Errors.InvalidParameter("fail to create processor", err.Error())
		}
		name, fields, _, _, err := proc.Process(params.Content)
		if err != nil {
			return api.Success(nil)
		}
		return api.Success(&metrics.Metric{
			Name:      name,
			Tags:      map[string]string{},
			Fields:    fields,
			Timestamp: time.Now().UnixNano(),
		})
	}
	return api.Success(nil)
}
