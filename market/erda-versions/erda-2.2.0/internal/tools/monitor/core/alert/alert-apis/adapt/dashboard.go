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

package adapt

import (
	"encoding/json"
	"fmt"
	"strings"
	"time"

	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	block "github.com/erda-project/erda/internal/tools/monitor/core/dataview/v1-chart-block"
	"github.com/erda-project/erda/internal/tools/monitor/utils"
	"github.com/erda-project/erda/pkg/common/errors"
)

func NewDashboard(a *Adapt) *dashgen {
	return &dashgen{a: a}
}

type dashgen struct {
	a              *Adapt
	preview        bool
	lang           i18n.LanguageCodes
	scope, scopeID string
}

func (d *dashgen) CreateChartDashboard(alertDetail *pb.CustomizeAlertDetail) (string, error) {
	d.init(alertDetail)
	return d.createChartDashboard(alertDetail)
}

func (d *dashgen) GenerateDashboardPreView(alertDetail *pb.CustomizeAlertDetail) (res *pb.View, err error) {
	if len(alertDetail.Rules) != 1 {
		return nil, fmt.Errorf("must be only one view")
	}
	d.preview = true
	d.init(alertDetail)
	vitems, err := d.generateViewConfigItems(alertDetail.Rules[0])
	if err != nil {
		return nil, err
	}
	staticData, err := structpb.NewValue(vitems[0].View.StaticData)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	v := vitems[0].View
	result, err := d.ToPBView(v)
	if err != nil {
		return nil, err
	}
	result.StaticData = staticData
	return result, nil
}

func (d *dashgen) ToPBView(v *block.View) (*pb.View, error) {
	controls, err := structpb.NewValue(v.Controls)
	if err != nil {
		return nil, err
	}
	result := &pb.View{
		Title:          v.Title,
		Description:    v.Description,
		ChartType:      v.ChartType,
		DataSourceType: v.DataSourceType,
		Controls:       controls,
	}
	dataSourceConfig, err := structpb.NewValue(v.Config.DataSourceConfig)
	if err != nil {
		return nil, err
	}
	option, err := structpb.NewValue(v.Config.Option)
	if err != nil {
		return nil, err
	}
	optionProps := make(map[string]*structpb.Value)
	if v.Config.OptionProps != nil {
		optionProps, err = (&Adapt{}).InterfaceMapToValueMap(*(v.Config.OptionProps))
		if err != nil {
			return nil, err
		}
	}
	config := &pb.Config{
		OptionProps:      optionProps,
		DataSourceConfig: dataSourceConfig,
		Option:           option,
	}
	result.Config = config
	query, err := (&Adapt{}).InterfaceMapToValueMap(v.API.Query)
	if err != nil {
		return nil, err
	}
	body, err := (&Adapt{}).InterfaceMapToValueMap(v.API.Body)
	if err != nil {
		return nil, err
	}
	header, err := (&Adapt{}).InterfaceMapToValueMap(v.API.Header)
	if err != nil {
		return nil, err
	}
	extraData, err := (&Adapt{}).InterfaceMapToValueMap(v.API.ExtraData)
	if err != nil {
		return nil, err
	}
	api := &pb.API{
		Url:       v.API.URL,
		Query:     query,
		Body:      body,
		Header:    header,
		ExtraData: extraData,
		Method:    v.API.Method,
	}
	result.Api = api
	return result, nil
}

func (d *dashgen) createChartDashboard(alertDetail *pb.CustomizeAlertDetail) (string, error) {
	block, err := d.generateDashboard(alertDetail)
	if err != nil {
		return "", fmt.Errorf("generate dashboard fialed. err=%s", err)
	}
	dash, err := d.a.dashboardAPI.CreateDashboard(block)
	if err != nil {
		return "", err
	}
	return dash.ID, nil
}

func (d *dashgen) init(alertDetail *pb.CustomizeAlertDetail) {
	d.scopeID = alertDetail.AlertScopeId
	d.scope = alertDetail.AlertScope

	if d.scope == "org" {
		org, err := d.a.bdl.GetOrg(d.scopeID)
		if err != nil {
			d.a.l.Infof("failed to get org by orgId=%s", d.scopeID)
		}
		d.scopeID = org.Name
	}
}

func (d *dashgen) generateDashboard(alertDetail *pb.CustomizeAlertDetail) (res *block.UserBlock, err error) {
	block := &block.UserBlock{
		Name:    "",
		Scope:   fmt.Sprintf("%s:alert", d.scope),
		ScopeID: d.scopeID,
	}
	viewConfig, err := d.createViewConfig(alertDetail.Rules)
	if err != nil {
		return nil, err
	}
	block.ViewConfig = &viewConfig

	return block, nil
}

func (d *dashgen) createViewConfig(rules []*pb.CustomizeAlertRule) (vc block.ViewConfigDTO, err error) {
	res := []*block.ViewConfigItem{}
	for _, rule := range rules {
		vci, err := d.generateViewConfigItems(rule)
		if err != nil {
			return nil, err
		}
		res = append(res, vci...)
	}
	return res, nil
}

func (d *dashgen) generateViewConfigItems(r *pb.CustomizeAlertRule) (res []*block.ViewConfigItem, err error) {
	res = []*block.ViewConfigItem{}
	heigth := 10
	for idx, f := range r.Functions {
		ct, err := d.getChartType(f)
		if err != nil {
			return nil, err
		}
		_url, err := d.getURL(ct, r.Metric)
		query, err := d.generateQuery(r)
		if err != nil {
			return nil, err
		}
		data, err := json.Marshal(f)
		if err != nil {
			return nil, errors.NewInternalServerError(err)
		}
		alertRuleFunction := &CustomizeAlertRuleFunction{}
		err = json.Unmarshal(data, alertRuleFunction)
		if err != nil {
			return nil, errors.NewInternalServerError(err)
		}
		view := &block.View{
			Title:     "",
			ChartType: ct,
			API: &block.API{
				URL:       _url,
				Query:     query,
				Body:      nil,
				Header:    nil,
				ExtraData: d.generateExtraData(query, alertRuleFunction, r.ActivedMetricGroups, r.Metric),
			},
		}
		vci := &block.ViewConfigItem{
			W:    24,
			H:    int64(heigth * (idx + 1)),
			X:    0,
			Y:    int64(heigth * idx),
			I:    fmt.Sprintf("view-%s", utils.RandomString(8)),
			View: view,
		}
		res = append(res, vci)
	}
	return res, nil
}

func (d *dashgen) getURL(ct string, field string) (string, error) {
	path := ""
	switch d.scope {
	case "org":
		path = "/api/orgCenter/metrics"
	case "micro_service":
		path = "/api/tmc/metrics"
	default:
		return "", fmt.Errorf("invalid scope %s", d.scope)
	}
	res := fmt.Sprintf("%s/%s/histogram", path, field)
	if ct == "table" {
		res = fmt.Sprintf("/api/orgCenter/metrics/%s", field)
	}
	return res, nil
}

func (d *dashgen) getChartType(f *pb.CustomizeAlertRuleFunction) (string, error) {
	ct := "chart:line"
	langCode := i18n.LanguageCodes{
		{
			Code: "en",
		},
	}
	agg, err := d.a.metricq.GetSingleAggregationMeta(langCode, "analysis", f.Aggregator)
	if err != nil {
		d.a.l.Errorf("cant find agg %s", f.Aggregator)
	}
	if agg.ResultType != "number" {
		ct = "table"
	}
	return ct, nil
}

func (d *dashgen) generateExtraData(query map[string]interface{}, f *CustomizeAlertRuleFunction, groups []string, metric string) map[string]interface{} {
	res := make(map[string]interface{})
	res["aggregation"] = f.Aggregator
	res["metric"] = strings.Join([]string{metric, f.Field}, "-")

	group := []string{}
	if val, ok := query["group"]; ok {
		val = strings.TrimLeft(val.(string), "(")
		val = strings.TrimRight(val.(string), ")")
		for _, vv := range strings.Split(val.(string), ",") {
			group = append(group, vv)
		}
	}
	res["group"] = group

	filters := []map[string]string{}
	for k, v := range query {
		idx := strings.Index(k, "_")
		if idx == -1 {
			continue
		}
		if _, ok := v.(string); !ok {
			continue
		}
		method, tag := k[:idx], k[idx+1:]
		if strings.HasPrefix(tag, "tags.") {
			filters = append(filters, map[string]string{
				"key":    utils.RandomString(1),
				"method": method,
				"tag":    tag,
				"value":  v.(string),
			})
		}
	}
	res["filters"] = filters

	res["activedMetricGroups"] = groups
	return res
}

func (d *dashgen) generateQuery(rule *pb.CustomizeAlertRule) (map[string]interface{}, error) {
	metrics, err := d.a.metricq.MetricMeta(d.lang, d.scope, d.scopeID, rule.Metric)
	if err != nil {
		return nil, err
	} else if metrics == nil {
		return nil, fmt.Errorf("%s has no metric meta", rule.Metric)
	}
	metric := metrics[0]
	res := make(map[string]interface{})
	res["start"] = utils.ConvertTimeToMS(time.Now().Add(-1 * time.Hour))
	res["end"] = utils.ConvertTimeToMS(time.Now())

	// transform group
	if len(rule.Group) > 0 {
		group := "("
		for i, item := range rule.Group {
			if i > 0 {
				group += ","
			}
			name := "tags." + item
			group += name
			if !d.preview {
				res["filter_"+name] = "{{" + item + "}}"
			}
		}
		group += ")"
		res["group"] = group
	}

	// transform filter
	for _, item := range rule.Filters {
		res[item.Operator+"_tags."+item.Tag] = item.Value
	}
	// microservices, warning previews need to be strengthened and filtered
	if d.scope == "micro_service" && d.preview {
		res["filter_terminus_key"] = d.scopeID
	}

	// transform function
	if rule.Functions != nil {
		function := rule.Functions[0]
		name := function.Field
		if strings.HasPrefix("tags.", function.Field) {
			if t, ok := metric.Tags[function.Field[len("tags."):]]; ok {
				name = t.Name
			}
		} else if strings.HasPrefix("fields.", function.Field) {
			if f, ok := metric.Fields[function.Field[len("fields."):]]; ok {
				name = f.Name
			}
		} else {
			if f, ok := metric.Fields[function.Field]; ok {
				name = f.Name
			}
		}
		res[function.Aggregator] = function.Field
		res["alias_"+function.Aggregator+"."+function.Field] = name
	}
	return res, nil
}
