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
	"strconv"
	"strings"
	"time"

	"github.com/mitchellh/mapstructure"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	"github.com/erda-project/erda/internal/tools/monitor/core/alert/alert-apis/db"
	"github.com/erda-project/erda/internal/tools/monitor/utils"
)

// FromModel .
func (a *CustomizeAlertDetail) FromModel(m *db.CustomizeAlert) *CustomizeAlertDetail {
	if m == nil {
		return nil
	}
	a.ID = m.ID
	a.Name = m.Name
	a.AlertType = m.AlertType
	a.AlertScope = m.AlertScope
	a.AlertScopeID = m.AlertScopeID
	a.Enable = m.Enable
	a.Attributes = m.Attributes
	return a
}

// FromModelWithDetail .
func (a *CustomizeAlertDetail) FromModelWithDetail(m *db.CustomizeAlert,
	rules []*CustomizeAlertRule, notifys []*CustomizeAlertNotifyTemplate) *CustomizeAlertDetail {
	if len(rules) == 0 || len(notifys) == 0 {
		return nil
	}
	var templates *CustomizeAlertNotifyTemplates
	for _, notify := range notifys {
		if templates == nil {
			templates = &CustomizeAlertNotifyTemplates{
				ID:         notify.ID,
				Name:       notify.Name,
				Title:      notify.Title,
				Content:    notify.Content,
				CreateTime: notify.CreateTime,
				UpdateTime: notify.UpdateTime,
			}
		}
		templates.Targets = append(templates.Targets, notify.Target)
	}
	a.ID = m.ID
	a.Name = m.Name
	a.AlertType = m.AlertType
	a.AlertScope = m.AlertScope
	a.AlertScopeID = m.AlertScopeID
	a.Enable = m.Enable
	a.Rules = rules
	a.Notifies = []*CustomizeAlertNotifyTemplates{templates}
	a.CreateTime = utils.ConvertTimeToMS(m.CreateTime)
	a.UpdateTime = utils.ConvertTimeToMS(m.UpdateTime)

	return a
}

// ToModel .
func (a *CustomizeAlertDetail) ToModel() *db.CustomizeAlert {
	if a == nil {
		return nil
	}
	attributes := a.Attributes
	return &db.CustomizeAlert{
		ID:           a.ID,
		Name:         a.Name,
		AlertType:    a.AlertType,
		AlertScope:   a.AlertScope,
		AlertScopeID: a.AlertScopeID,
		Enable:       a.Enable,
		Attributes:   attributes,
	}
}

// FromModel .
func (r *CustomizeAlertRule) FromModel(m *db.CustomizeAlertRule) (*CustomizeAlertRule, error) {
	if err := mapstructure.Decode(m.Template, &r); err != nil {
		return nil, err
	}
	r.ID = m.ID
	r.Name = m.Name
	r.CreateTime = m.CreateTime.UnixNano() / int64(time.Millisecond)
	r.UpdateTime = m.UpdateTime.UnixNano() / int64(time.Millisecond)
	for _, function := range r.Functions {
		function.DataType = TypeOf(function.Value)
	}
	for _, filter := range r.Filters {
		filter.DataType = TypeOf(filter.Value)
	}
	r.Attributes = m.Attributes

	if len(m.Attributes) != 0 {
		if v, ok := m.Attributes["active_metric_groups"]; ok {
			if slice, ok := utils.ConvertStringArr(v); !ok {
				logrus.Error("fail to convert active_metric_groups to string slice")
			} else {
				r.ActivedMetricGroups = slice
			}
		}
	}
	return r, nil
}

// ToModel .
func (a *Adapt) ToModel(r *pb.CustomizeAlertRule, alert *pb.CustomizeAlertDetail, index string) *db.CustomizeAlertRule {
	attributeData := make(map[string]interface{})
	for k, v := range r.Attributes {
		attributeData[k] = v.AsInterface()
	}
	attributeData["level"] = "WARNING"
	attributeData["recover"] = strconv.FormatBool(false)
	attributeData["alert_dashboard_id"] = alert.Attributes["alert_dashboard_id"]

	var groups []string
	for _, group := range r.Group {
		groups = append(groups, "{{"+group+"}}")
	}
	groupKey := strings.Join(groups, "-")
	attributeData["tickets_metric_key"] = groupKey
	var alertGroup string
	if attributeData["alert_group"] == nil {
		alertGroup = ""
	} else if v, ok := attributeData["alert_group"].(string); ok {
		alertGroup = v
	} else {
		alertGroup = ""
	}
	if alertGroup != "" && groupKey != "" {
		alertGroup += "-"
	}
	alertGroup += groupKey
	attributeData["alert_group"] = alertGroup

	attributeData["active_metric_groups"] = r.ActivedMetricGroups

	var (
		functions []*CustomizeAlertRuleFunction
		filters   []*CustomizeAlertRuleFilter
	)
	for _, function := range r.Functions {
		functions = append(functions, &CustomizeAlertRuleFunction{
			Field:      function.Field,
			Alias:      function.Alias,
			Aggregator: function.Aggregator,
			Operator:   function.Operator,
			Value:      function.Value,
		})
	}
	for _, filter := range r.Filters {
		filters = append(filters, &CustomizeAlertRuleFilter{
			Tag:      filter.Tag,
			Operator: filter.Operator,
			Value:    filter.Value,
		})
	}
	template := CustomizeAlertRuleTemplate{
		Metric:    r.Metric,
		Window:    r.Window,
		Functions: functions,
		Filters:   filters,
		Group:     r.Group,
		Outputs:   r.Outputs,
		Select:    r.Select,
	}
	return &db.CustomizeAlertRule{
		ID:               r.Id,
		Name:             r.Name,
		CustomizeAlertID: alert.Id,
		AlertType:        alert.AlertType,
		AlertIndex:       index,
		AlertScope:       alert.AlertScope,
		AlertScopeID:     alert.AlertScopeId,
		Template:         utils.ConvertStructToMap(template),
		Attributes:       attributeData,
		Enable:           alert.Enable,
	}
}

// FromModel .
func (t *CustomizeAlertNotifyTemplate) FromModel(m *db.CustomizeAlertNotifyTemplate) *CustomizeAlertNotifyTemplate {
	t.ID = m.ID
	t.Name = m.Name
	t.Target = m.Target
	t.Title = m.Title
	t.Content = m.Template
	t.CreateTime = m.CreateTime.UnixNano() / int64(time.Millisecond)
	t.UpdateTime = m.UpdateTime.UnixNano() / int64(time.Millisecond)
	return t
}

// ToModel .
func (t *CustomizeAlertNotifyTemplate) ToModel(alert *pb.CustomizeAlertDetail, index string) (*db.CustomizeAlertNotifyTemplate, error) {
	if ok := notifyTargetSet[t.Target]; !ok {
		return nil, invalidParameter("not support notify target %s", t.Target)
	}
	return &db.CustomizeAlertNotifyTemplate{
		ID:               t.ID,
		Name:             t.Name,
		CustomizeAlertID: alert.Id,
		AlertType:        alert.AlertType,
		AlertIndex:       index,
		Target:           t.Target,
		Trigger:          "alert",
		Title:            t.Title,
		Template:         t.Content,
		Version:          "1.0",
		Enable:           alert.Enable,
	}, nil
}

func (a *Adapt) newCustomizeAlertOverview(
	code i18n.LanguageCodes,
	alert *db.CustomizeAlert,
	rules []*pb.CustomizeAlertRule,
	notifies []*CustomizeAlertNotifyTemplate) *pb.CustomizeAlertOverview {
	if len(rules) == 0 || len(notifies) == 0 {
		return nil
	}

	var dashboardID string
	if alert.Attributes != nil {
		v := alert.Attributes["alert_dashboard_id"]
		if v != nil {
			dashboardID = v.(string)
		}
	}

	var targets []string
	for _, template := range notifies {
		targets = append(targets, a.t.Text(code, template.Target))
	}

	metricName := rules[0].Metric
	if rules[0].Attributes != nil {
		if v, ok := rules[0].Attributes["metric_name"]; ok {
			metricName = v.GetStringValue()
		}
	}
	return &pb.CustomizeAlertOverview{
		Id:            alert.ID,
		Name:          alert.Name,
		Metric:        a.t.Text(code, metricName),
		Window:        rules[0].Window,
		NotifyTargets: targets,
		DashboardId:   dashboardID,
		Enable:        alert.Enable,
		CreateTime:    alert.CreateTime.UnixNano() / int64(time.Millisecond),
		UpdateTime:    alert.UpdateTime.UnixNano() / int64(time.Millisecond),
		Creator:       alert.CreatorID,
	}
}
