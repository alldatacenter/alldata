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
	"sort"
	"time"

	"github.com/mitchellh/mapstructure"
	uuid "github.com/satori/go.uuid"
	"github.com/sirupsen/logrus"
	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	"github.com/erda-project/erda/internal/tools/monitor/core/alert/alert-apis/db"
	"github.com/erda-project/erda/internal/tools/monitor/utils"
	"github.com/erda-project/erda/pkg/encoding/jsonmap"
)

// FieldMeta .
type FieldMeta struct {
	Field    *DisplayKey `json:"field"`
	DataType string      `json:"dataType"`
}

func (fm *FieldMeta) String() string {
	return fmt.Sprintf("FieldMeta. key: %s, dataType: %s", fm.Field.Key, fm.DataType)
}

// FieldMetaSlice .
type FieldMetaSlice []*pb.FieldMeta

func (s FieldMetaSlice) Len() int {
	return len(s)
}
func (s FieldMetaSlice) Swap(i, j int) {
	s[i], s[j] = s[j], s[i]
}
func (s FieldMetaSlice) Less(i, j int) bool {
	if s[i].Field.Key == s[i].Field.Display {
		if s[j].Field.Key == s[j].Field.Display {
			return s[i].Field.Key < s[j].Field.Key
		} else {
			return false
		}
	} else {
		if s[j].Field.Key == s[j].Field.Display {
			return true
		} else {
			return s[i].Field.Key < s[j].Field.Key
		}
	}
}

// TagMeta .
type TagMeta struct {
	Tag      *DisplayKey `json:"tag"`
	DataType string      `json:"dataType"`
}

func (tm *TagMeta) String() string {
	return fmt.Sprintf("FieldMeta. key: %s, dataType: %s", tm.Tag.Key, tm.DataType)
}

// TagMetaSlice .
type TagMetaSlice []*pb.TagMeta

func (s TagMetaSlice) Len() int {
	return len(s)
}
func (s TagMetaSlice) Swap(i, j int) {
	s[i], s[j] = s[j], s[i]
}
func (s TagMetaSlice) Less(i, j int) bool {
	if s[i].Tag.Key == s[i].Tag.Display {
		if s[j].Tag.Key == s[j].Tag.Display {
			return s[i].Tag.Key < s[j].Tag.Key
		} else {
			return false
		}
	} else {
		if s[j].Tag.Key == s[j].Tag.Display {
			return true
		} else {
			return s[i].Tag.Key < s[j].Tag.Key
		}
	}
}

// MetricMeta .
type MetricMeta struct {
	Name   *DisplayKey  `json:"name"`
	Fields []*FieldMeta `json:"fields"`
	Tags   []*TagMeta   `json:"tags"`
}

// MetricMetaSlice .
type MetricMetaSlice []*pb.MetricMeta

func (s MetricMetaSlice) Len() int {
	return len(s)
}
func (s MetricMetaSlice) Swap(i, j int) {
	s[i], s[j] = s[j], s[i]
}
func (s MetricMetaSlice) Less(i, j int) bool {
	if s[i].Name.Key == s[i].Name.Display {
		if s[j].Name.Key == s[j].Name.Display {
			return s[i].Name.Key < s[j].Name.Key
		}
		return false
	} else {
		if s[j].Name.Key == s[j].Name.Display {
			return true
		}
		return s[i].Name.Key < s[j].Name.Key
	}
}

// CustomizeMetrics .
type CustomizeMetrics struct {
	Metrics           []*MetricMeta `json:"metrics"`
	FunctionOperators []*Operator   `json:"functionOperators"`
	FilterOperators   []*Operator   `json:"filterOperators"`
	Aggregator        []*DisplayKey `json:"aggregator"`
	NotifySample      string        `json:"notifySample"`
}

// CustomizeMetrics .
func (a *Adapt) CustomizeMetrics(lang i18n.LanguageCodes, scope, scopeID string, names []string) (*pb.CustomizeMetrics, error) {
	meta, err := a.metricq.MetricMeta(lang, scope, scopeID, names...)
	if err != nil {
		return nil, err
	}
	var metrics []*pb.MetricMeta
	for _, m := range meta {
		metric := &pb.MetricMeta{
			Name: &pb.DisplayKey{Key: m.Name.Key, Display: m.Name.Name},
		}
		for _, field := range m.Fields {
			metric.Fields = append(metric.Fields, &pb.FieldMeta{
				Field:    &pb.DisplayKey{Key: field.Key, Display: field.Name},
				DataType: field.Type,
			})
		}
		sort.Sort(FieldMetaSlice(metric.Fields))
		for _, tag := range m.Tags {
			metric.Tags = append(metric.Tags, &pb.TagMeta{
				Tag:      &pb.DisplayKey{Key: tag.Key, Display: tag.Name},
				DataType: "string",
			})
		}
		sort.Sort(TagMetaSlice(metric.Tags))
		metrics = append(metrics, metric)
	}
	sort.Sort(MetricMetaSlice(metrics))

	return &pb.CustomizeMetrics{
		Metrics:           metrics,
		FunctionOperators: a.FunctionOperatorKeys(lang),
		FilterOperators:   a.FilterOperatorKeys(lang),
		Aggregator:        a.AggregatorKeys(lang),
	}, nil
}

type (
	// CustomizeAlertRule .
	CustomizeAlertRule struct {
		ID                  uint64                        `json:"id"`
		Name                string                        `json:"name"`
		Metric              string                        `json:"metric"`
		Window              uint64                        `json:"window"`
		Functions           []*CustomizeAlertRuleFunction `json:"functions"`
		Filters             []*CustomizeAlertRuleFilter   `json:"filters"`
		Group               []string                      `json:"group"`
		Outputs             []string                      `json:"outputs"`
		Select              map[string]string             `json:"select"`
		Attributes          map[string]interface{}        `json:"attributes"`
		ActivedMetricGroups []string                      `json:"activedMetricGroups,omitempty"` // for frontend
		CreateTime          int64                         `json:"createTime"`
		UpdateTime          int64                         `json:"updateTime"`
	}
	// CustomizeAlertRuleFunction .
	CustomizeAlertRuleFunction struct {
		Field      string      `json:"field"`
		Alias      string      `json:"alias"`
		Aggregator string      `json:"aggregator"`
		Operator   string      `json:"operator"`
		Value      interface{} `json:"value"`
		DataType   string      `json:"dataType"`
		Unit       string      `json:"unit" translate:"true"`
	}
	// CustomizeAlertRuleFilter .
	CustomizeAlertRuleFilter struct {
		Tag      string      `json:"tag"`
		Operator string      `json:"operator"`
		Value    interface{} `json:"value"`
		DataType string      `json:"dataType"`
	}
	// CustomizeAlertRuleTemplate .
	CustomizeAlertRuleTemplate struct {
		Metric    string                        `json:"metric"`
		Window    uint64                        `json:"window"`
		Functions []*CustomizeAlertRuleFunction `json:"functions"`
		Filters   []*CustomizeAlertRuleFilter   `json:"filters"`
		Group     []string                      `json:"group"`
		Outputs   []string                      `json:"outputs"`
		Select    map[string]string             `json:"select"`
	}
	// CustomizeAlertNotifyTemplate .
	CustomizeAlertNotifyTemplate struct {
		ID         uint64                 `json:"id"`
		Name       string                 `json:"name"`
		Target     string                 `json:"target"`
		Title      string                 `json:"title"`
		Content    string                 `json:"content"`
		Attributes map[string]interface{} `json:"attributes"`
		CreateTime int64                  `json:"createTime"`
		UpdateTime int64                  `json:"updateTime"`
	}
	// CustomizeAlertOverview .
	CustomizeAlertOverview struct {
		ID            uint64   `json:"id"`
		Name          string   `json:"name"`
		Metric        string   `json:"metric"`
		Window        uint64   `json:"window"`
		NotifyTargets []string `json:"notifyTargets"`
		DashboardID   string   `json:"dashboardId,omitempty"`
		Enable        bool     `json:"enable"`
		CreateTime    int64    `json:"createTime"`
		UpdateTime    int64    `json:"updateTime"`
	}
	// CustomizeAlertNotifyTemplates .
	CustomizeAlertNotifyTemplates struct {
		ID         uint64                 `json:"id"`
		Name       string                 `json:"name"`
		Targets    []string               `json:"targets"`
		Title      string                 `json:"title"`
		Content    string                 `json:"content"`
		Attributes map[string]interface{} `json:"attributes"`
		CreateTime int64                  `json:"createTime"`
		UpdateTime int64                  `json:"updateTime"`
	}
	// CustomizeAlertDetail .
	CustomizeAlertDetail struct {
		ID           uint64                           `json:"id"`
		ClusterName  string                           `json:"clusterName"`
		Name         string                           `json:"name"`
		AlertType    string                           `json:"alertType"`
		AlertScope   string                           `json:"alertScope"`
		AlertScopeID string                           `json:"alertScopeId"`
		Enable       bool                             `json:"enable"`
		Attributes   map[string]interface{}           `json:"attributes"`
		Rules        []*CustomizeAlertRule            `json:"rules"`
		Notifies     []*CustomizeAlertNotifyTemplates `json:"notifies"`
		CreateTime   int64                            `json:"createTime"`
		UpdateTime   int64                            `json:"updateTime"`

		// just for frontend
		Lang i18n.LanguageCodes `json:"-"`
	}
)

// customize alert type
const (
	customizeAlertTypeOrg          = "org_customize"
	customizeAlertTypeMicroService = "micro_service_customize"
	applicationIdTag               = "application_id"
	applicationIdValue             = "$application_id"
	clusterNameTag                 = "cluster_name"
	clusterNameValue               = "$cluster_name"

	any = "any"
	all = "all"

	targetTerminusKey = "target_terminus_key"
	terminusKey       = "terminus_key"
)

// CustomizeAlerts .
func (a *Adapt) CustomizeAlerts(lang i18n.LanguageCodes, scope, scopeID string, pageNo, pageSize int, name string) ([]*pb.CustomizeAlertOverview, []string, int, error) {
	alerts, err := a.db.CustomizeAlert.QueryByScopeAndScopeID(scope, scopeID, pageNo, pageSize, name)
	if err != nil {
		return nil, nil, 0, err
	}
	var alertIDs []uint64
	var userIDs []string
	userIDMap := make(map[string]bool)

	for _, alert := range alerts {
		alertIDs = append(alertIDs, alert.ID)
		userId := alert.CreatorID
		if userId != "" && !userIDMap[userId] {
			userIDs = append(userIDs, userId)
			userIDMap[userId] = true
		}
	}
	// get alert rule
	rulesMap, err := a.getCustomizeAlertRulesByAlertIDs(alertIDs)
	if err != nil {
		return nil, nil, 0, err
	}
	// get alert notify template
	notifyTemplatesMap, err := a.getCustomizeAlertNotifyTemplatesByAlertIDs(alertIDs)
	if err != nil {
		return nil, nil, 0, err
	}
	var list []*pb.CustomizeAlertOverview
	for _, item := range alerts {
		alert := a.newCustomizeAlertOverview(lang, item, rulesMap[item.ID], notifyTemplatesMap[item.ID])
		if alert == nil {
			continue
		}
		list = append(list, alert)
	}
	total, err := a.db.CustomizeAlert.CountByScopeAndScopeID(scope, scopeID, name)
	if err != nil {
		return nil, nil, 0, err
	}
	return list, userIDs, total, nil
}

// return alertID to rules Map
func (a *Adapt) getCustomizeAlertRulesByAlertIDs(id []uint64) (map[uint64][]*pb.CustomizeAlertRule, error) {
	rules, err := a.db.CustomizeAlertRule.QueryByAlertIDs(id)
	if err != nil {
		return nil, err
	}
	alertRules := make(map[uint64][]*pb.CustomizeAlertRule)
	for _, item := range rules {
		rule, err := CustomizeAlertRuleFromModel(item)
		if err != nil {
			a.l.Errorf("fail to wrap customize rule: %s", err)
			continue
		} else if rule == nil {
			continue
		}
		alertRules[item.CustomizeAlertID] = append(alertRules[item.CustomizeAlertID], rule)
	}
	return alertRules, nil
}

// return alertID to notify template Map
func (a *Adapt) getCustomizeAlertNotifyTemplatesByAlertIDs(id []uint64) (map[uint64][]*CustomizeAlertNotifyTemplate, error) {
	notifyTemplates, err := a.db.CustomizeAlertNotifyTemplate.QueryByAlertIDs(id)
	if err != nil {
		return nil, err
	}
	notifyTemplatesMap := make(map[uint64][]*CustomizeAlertNotifyTemplate)
	for _, item := range notifyTemplates {
		notify := (&CustomizeAlertNotifyTemplate{}).FromModel(item)
		if notify == nil {
			continue
		}
		notifyTemplatesMap[item.CustomizeAlertID] = append(notifyTemplatesMap[item.CustomizeAlertID], notify)
	}
	return notifyTemplatesMap, nil
}

// CustomizeAlert .
func (a *Adapt) CustomizeAlert(id uint64) (*pb.CustomizeAlertDetail, error) {
	alert, err := a.db.CustomizeAlert.GetByID(id)
	if err != nil {
		return nil, err
	}
	return a.FromModel(alert), nil
}

func (a *Adapt) FromModel(m *db.CustomizeAlert) *pb.CustomizeAlertDetail {
	if m == nil {
		return nil
	}
	customizeAlertDetail := &pb.CustomizeAlertDetail{
		Attributes: make(map[string]*structpb.Value),
	}
	customizeAlertDetail.Id = m.ID
	customizeAlertDetail.Name = m.Name
	customizeAlertDetail.AlertType = m.AlertType
	customizeAlertDetail.AlertScope = m.AlertScope
	customizeAlertDetail.AlertScopeId = m.AlertScopeID
	customizeAlertDetail.Enable = m.Enable
	for k, v := range m.Attributes {
		anyData, err := structpb.NewValue(v)
		if err != nil {
			logrus.Errorf("fail transform interface to any type")
			return nil
		}
		customizeAlertDetail.Attributes[k] = anyData
	}
	return customizeAlertDetail
}

func CustomizeAlertRuleFromModel(m *db.CustomizeAlertRule) (*pb.CustomizeAlertRule, error) {
	r := &CustomizeAlertRule{}
	if err := mapstructure.Decode(m.Template, r); err != nil {
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
	result := &pb.CustomizeAlertRule{}
	data, err := json.Marshal(r)
	if err != nil {
		return nil, err
	}
	err = json.Unmarshal(data, result)
	if err != nil {
		return nil, err
	}
	return result, nil
}

func (a *Adapt) CustomizeAlertToModel(customizeAlertDetail *pb.CustomizeAlertDetail) *db.CustomizeAlert {
	data := &db.CustomizeAlert{
		ID:           customizeAlertDetail.Id,
		Name:         customizeAlertDetail.Name,
		AlertType:    customizeAlertDetail.AlertType,
		AlertScope:   customizeAlertDetail.AlertScope,
		AlertScopeID: customizeAlertDetail.AlertScopeId,
		Enable:       customizeAlertDetail.Enable,
	}
	data.Attributes = make(jsonmap.JSONMap)
	for k, v := range customizeAlertDetail.Attributes {
		data.Attributes[k] = v
	}
	return data
}

func (a *Adapt) FromModelWithDetail(m *db.CustomizeAlert, rules []*pb.CustomizeAlertRule, notifies []*CustomizeAlertNotifyTemplate) *pb.CustomizeAlertDetail {
	if len(rules) == 0 || len(notifies) == 0 {
		return nil
	}
	var templates *pb.CustomizeAlertNotifyTemplates
	for _, notify := range notifies {
		if templates == nil {
			templates = &pb.CustomizeAlertNotifyTemplates{
				Id:         notify.ID,
				Name:       notify.Name,
				Title:      notify.Title,
				Content:    notify.Content,
				CreateTime: notify.CreateTime,
				UpdateTime: notify.UpdateTime,
			}
		}
		templates.Targets = append(templates.Targets, notify.Target)
	}
	customizeAlertDetail := &pb.CustomizeAlertDetail{
		Id:           m.ID,
		Name:         m.Name,
		AlertType:    m.AlertType,
		AlertScope:   m.AlertScope,
		AlertScopeId: m.AlertScopeID,
		Enable:       m.Enable,
		Rules:        rules,
		Notifies:     []*pb.CustomizeAlertNotifyTemplates{templates},
		CreateTime:   utils.ConvertTimeToMS(m.CreateTime),
		UpdateTime:   utils.ConvertTimeToMS(m.UpdateTime),
	}
	return customizeAlertDetail
}

// CustomizeAlertDetail .
func (a *Adapt) CustomizeAlertDetail(id uint64) (*pb.CustomizeAlertDetail, error) {
	alert, err := a.db.CustomizeAlert.GetByID(id)
	if err != nil {
		return nil, err
	}
	alertIDs := []uint64{id}
	rulesMap, err := a.getCustomizeAlertRulesByAlertIDs(alertIDs)
	if err != nil {
		return nil, err
	}
	rules := rulesMap[id]
	notifyTemplatesMap, err := a.getCustomizeAlertNotifyTemplatesByAlertIDs(alertIDs)
	if err != nil {
		return nil, err
	}
	notifyTemplates := notifyTemplatesMap[id]
	alertDetail := a.FromModelWithDetail(alert, rules, notifyTemplates)
	if alertDetail == nil {
		return nil, nil
	}
	for _, rule := range alertDetail.Rules {
		// filter
		filters := make([]*pb.CustomizeAlertRuleFilter, 0)
		for _, filter := range rule.Filters {
			if filter.Tag == applicationIdTag && filter.Value.GetStringValue() == applicationIdValue {
				continue
			}

			if filter.Tag == clusterNameTag && filter.Value.GetStringValue() == clusterNameValue {
				continue
			}

			if alert.AlertType == customizeAlertTypeMicroService && a.microServiceFilterTags[filter.Tag] {
				continue
			}
			if alert.AlertType == customizeAlertTypeOrg && a.orgFilterTags[filter.Tag] {
				continue
			}
			filters = append(filters, filter)
		}
		rule.Filters = filters
		// filter group
		groups := make([]string, 0)
		for _, group := range rule.Group {
			if alert.AlertType == customizeAlertTypeMicroService && a.microServiceFilterTags[group] {
				continue
			}
			if alert.AlertType == customizeAlertTypeOrg && a.orgFilterTags[group] {
				continue
			}
			groups = append(groups, group)
		}
		rule.Group = groups
	}
	return alertDetail, nil
}

// CreateCustomizeAlert .
func (a *Adapt) CreateCustomizeAlert(alertDetail *pb.CustomizeAlertDetail, userID string) (alertID uint64, err error) {
	tx := a.db.Begin()
	defer func() {
		if err != nil {
			tx.Rollback()
		} else if err := recover(); err != nil {
			a.l.Errorf("panic: %s", err)
			tx.Rollback()
		} else {
			tx.Commit()
		}
	}()

	if alert, err := tx.CustomizeAlert.GetByScopeAndScopeIDAndName(alertDetail.AlertScope, alertDetail.AlertScopeId, alertDetail.Name); err != nil {
		return 0, err
	} else if alert != nil {
		return 0, ErrorAlreadyExists
	}
	// create alert
	index := a.generateCustomizeAlertIndex()

	// related to the dashboard
	//dashboardID, err := NewDashboard(a).CreateChartDashboard(alertDetail)
	//if err != nil {
	//	return 0, err
	//}

	alertDetail.Id = 0
	alertDetail.Enable = true
	if alertDetail.Attributes == nil {
		alertDetail.Attributes = make(map[string]*structpb.Value)
	}
	alertIndex := structpb.NewStringValue(index)
	//alertDashboardID := structpb.NewStringValue(dashboardID)
	alertDetail.Attributes["alert_index"] = alertIndex
	alertDetail.Attributes["alert_source"] = structpb.NewStringValue("Custom")
	//alertDetail.Attributes["alert_dashboard_id"] = alertDashboardID
	alert := a.CustomizeAlertToModel(alertDetail)
	alert.CreatorID = userID
	if err := tx.CustomizeAlert.Insert(alert); err != nil {
		return 0, err
	}
	alertDetail.Id = alert.ID

	// create alarm rules, only one rule is allowed
	alertRule := alertDetail.Rules[0]
	if alertRule.Name == "" {
		alertRule.Name = alertDetail.Name
	}
	if len(alertRule.Group) == 0 {
		alertRule.Group = make([]string, 0)
	}
	rule := a.ToModel(alertRule, alertDetail, index)
	if err := tx.CustomizeAlertRule.Insert(rule); err != nil {
		return 0, err
	}

	// create an alert notification template
	notifyTargetMap := make(map[string]bool)
	for _, notify := range alertDetail.Notifies {
		var notifyTemplate *CustomizeAlertNotifyTemplate
		for _, target := range notify.Targets {
			// Avoid target duplication
			if ok := notifyTargetMap[target]; ok {
				continue
			}
			notifyTargetMap[target] = true

			if notifyTemplate == nil {
				notifyTemplate = &CustomizeAlertNotifyTemplate{
					ID:         notify.Id,
					Name:       notify.Name,
					Title:      notify.Title,
					Content:    notify.Content,
					CreateTime: notify.CreateTime,
					UpdateTime: notify.UpdateTime,
				}
				if notifyTemplate.Name == "" {
					notifyTemplate.Name = alertDetail.Name
				}
			}
			notifyTemplate.Target = target
			notify, err := notifyTemplate.ToModel(alertDetail, index)
			if err != nil {
				return 0, err
			}
			if err := tx.CustomizeAlertNotifyTemplate.Insert(notify); err != nil {
				return 0, err
			}
		}
	}
	return alertDetail.Id, nil
}

func (a *Adapt) generateCustomizeAlertIndex() string {
	return uuid.NewV4().String()
}

// UpdateCustomizeAlert .
func (a *Adapt) UpdateCustomizeAlert(alertDetail *pb.CustomizeAlertDetail) (err error) {
	tx := a.db.Begin()
	defer func() {
		if err != nil {
			tx.Rollback()
		} else if err := recover(); err != nil {
			a.l.Errorf("panic: %s", err)
			tx.Rollback()
		} else {
			tx.Commit()
		}
	}()
	if alertDetail.Name != "" {
		if alert, err := tx.CustomizeAlert.GetByScopeAndScopeIDAndName(alertDetail.AlertScope, alertDetail.AlertScopeId, alertDetail.Name); err != nil {
			return err
		} else if alert != nil && alert.ID != alertDetail.Id {
			return ErrorAlreadyExists
		}
	}

	// modify alert
	alert, err := tx.CustomizeAlert.GetByID(alertDetail.Id)
	if err != nil {
		return err
	}
	if alert == nil {
		return nil
	}
	index, ok := utils.GetMapValueString(alert.Attributes, "alert_index")
	if !ok {
		return fmt.Errorf("no alert index attributes")
	}

	attributes := make(map[string]*structpb.Value)
	for k, v := range alert.Attributes {
		data, err := structpb.NewValue(v)
		if err != nil {
			logrus.Errorf("transform any type is fail err is %s", err)
		}
		attributes[k] = data
	}
	for k, v := range alertDetail.Attributes {
		attributes[k] = v
	}
	alertIndex := structpb.NewStringValue(index)
	attributes["alert_index"] = alertIndex
	alertDetail.Attributes = attributes
	if err := tx.CustomizeAlert.Update(a.CustomizeAlertToModel(alertDetail)); err != nil {
		return err
	}
	// modify alert rule
	rules, err := a.getCustomizeAlertRulesMapByAlertID(alertDetail.Id)
	if err != nil {
		return err
	}
	saveRuleIDs := make(map[uint64]bool)
	for _, item := range alertDetail.Rules {
		if item.Name == "" {
			item.Name = alertDetail.Name
		}
		rule := a.ToModel(item, alertDetail, index)
		// Modify if it exists, add if it doesn't exist
		if _, ok := rules[rule.ID]; ok {
			if err := tx.CustomizeAlertRule.Update(rule); err != nil {
				return err
			}
			saveRuleIDs[rule.ID] = true
		} else {
			rule.ID = 0
			if err := tx.CustomizeAlertRule.Insert(rule); err != nil {
				return err
			}
		}
	}
	// delete exist expression
	var deleteRuleIDs []uint64
	for ruleID := range rules {
		if _, ok := saveRuleIDs[ruleID]; !ok {
			deleteRuleIDs = append(deleteRuleIDs, ruleID)
		}
	}
	if err := tx.CustomizeAlertRule.DeleteByIDs(deleteRuleIDs); err != nil {
		return err
	}

	// modify alert notify template
	notifyMap, err := a.getCustomizeAlertNotifyTemplateByAlertID(alertDetail.Id)
	if err != nil {
		return err
	}
	notifyTargetMap := make(map[string]*CustomizeAlertNotifyTemplate)
	for _, notify := range notifyMap {
		notifyTargetMap[notify.Name+notify.Target] = notify
	}
	saveNotifyIDs := make(map[uint64]bool)
	for _, notifyDTO := range alertDetail.Notifies {
		var templateDTO *CustomizeAlertNotifyTemplate
		for _, target := range notifyDTO.Targets {
			if templateDTO == nil {
				templateDTO = &CustomizeAlertNotifyTemplate{
					ID:         notifyDTO.Id,
					Name:       notifyDTO.Name,
					Title:      notifyDTO.Title,
					Content:    notifyDTO.Content,
					CreateTime: notifyDTO.CreateTime,
					UpdateTime: notifyDTO.UpdateTime,
				}
				if templateDTO.Name == "" {
					templateDTO.Name = alertDetail.Name
				}
			}
			templateDTO.Target = target
			template, err := templateDTO.ToModel(alertDetail, index)
			if err != nil {
				return err
			}
			// Modify if it exists, add if it doesn't exist
			if notify, ok := notifyTargetMap[template.Name+template.Target]; ok {
				template.ID = notify.ID
				if err := tx.CustomizeAlertNotifyTemplate.Update(template); err != nil {
					return err
				}
				saveNotifyIDs[template.ID] = true
			} else {
				template.ID = 0
				if err := tx.CustomizeAlertNotifyTemplate.Insert(template); err != nil {
					return err
				}
			}
		}
	}
	// delete exist notify template
	var deleteNotifyIDs []uint64
	for notifyID := range notifyMap {
		if _, ok := saveNotifyIDs[notifyID]; !ok {
			deleteNotifyIDs = append(deleteNotifyIDs, notifyID)
		}
	}
	if err := tx.CustomizeAlertNotifyTemplate.DeleteByIDs(deleteNotifyIDs); err != nil {
		return err
	}
	return nil
}

// return rules Map
func (a *Adapt) getCustomizeAlertRulesMapByAlertID(id uint64) (map[uint64]*CustomizeAlertRule, error) {
	rules, err := a.db.CustomizeAlertRule.QueryByAlertIDs([]uint64{id})
	if err != nil {
		return nil, err
	}
	rulesMap := make(map[uint64]*CustomizeAlertRule)
	for _, item := range rules {
		rule, err := (&CustomizeAlertRule{}).FromModel(item)
		if err != nil {
			a.l.Errorf("fail to wrap customize rule: %s", err)
			continue
		} else if rule == nil {
			continue
		}
		rulesMap[item.ID] = rule
	}
	return rulesMap, nil
}

func (a *Adapt) getCustomizeAlertNotifyTemplateByAlertID(alertID uint64) (map[uint64]*CustomizeAlertNotifyTemplate, error) {
	notifyTemplates, err := a.db.CustomizeAlertNotifyTemplate.QueryByAlertIDs([]uint64{alertID})
	if err != nil {
		return nil, err
	}
	notifyTemplateMap := make(map[uint64]*CustomizeAlertNotifyTemplate)
	for _, item := range notifyTemplates {
		notify := (&CustomizeAlertNotifyTemplate{}).FromModel(item)
		if notify == nil {
			continue
		}
		notifyTemplateMap[item.ID] = notify
	}
	return notifyTemplateMap, nil
}

// UpdateCustomizeAlertEnable .
func (a *Adapt) UpdateCustomizeAlertEnable(id uint64, enable bool) (err error) {
	tx := a.db.Begin()
	defer func() {
		if exp := recover(); exp != nil {
			a.l.Errorf("panic: %s", exp)
			tx.Rollback()
		} else if err != nil {
			tx.Rollback()
		} else {
			tx.Commit()
		}
	}()
	// close alert
	if err := tx.CustomizeAlert.UpdateEnable(id, enable); err != nil {
		return err
	}
	// close alert expression
	if err := tx.CustomizeAlertRule.UpdateEnableByAlertID(id, enable); err != nil {
		return err
	}
	// close alert notify
	if err := tx.CustomizeAlertNotifyTemplate.UpdateEnableByAlertID(id, enable); err != nil {
		return err
	}
	return nil
}

// DeleteCustomizeAlert .
func (a *Adapt) DeleteCustomizeAlert(id uint64) (err error) {
	tx := a.db.Begin()
	defer func() {
		if exp := recover(); exp != nil {
			a.l.Errorf("panic: %s", exp)
			tx.Rollback()
		} else if err != nil {
			tx.Rollback()
		} else {
			tx.Commit()
		}
	}()
	if err := tx.CustomizeAlert.DeleteByID(id); err != nil {
		return err
	}
	if err := tx.CustomizeAlertRule.DeleteByAlertID(id); err != nil {
		return err
	}
	if err := tx.CustomizeAlertNotifyTemplate.DeleteByAlertID(id); err != nil {
		return err
	}
	return nil
}
