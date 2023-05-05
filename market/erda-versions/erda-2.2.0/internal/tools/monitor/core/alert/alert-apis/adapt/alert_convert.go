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
	"reflect"
	"strconv"
	"strings"
	"time"

	"github.com/sirupsen/logrus"
	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	"github.com/erda-project/erda/internal/tools/monitor/core/alert/alert-apis/db"
	"github.com/erda-project/erda/internal/tools/monitor/utils"
	"github.com/erda-project/erda/pkg/encoding/jsonmap"
)

// FromCustomizeAlertRule .
func FromCustomizeAlertRule(lang i18n.LanguageCodes, t i18n.Translator, cr *db.CustomizeAlertRule) (*pb.AlertRule, error) {
	r := &pb.AlertRule{}
	rule, err := (&CustomizeAlertRule{}).FromModel(cr)
	if err != nil {
		return nil, err
	}
	r.Id = cr.ID
	r.Name = cr.Name
	r.AlertType = cr.AlertType
	r.AlertScope = cr.AlertScope
	r.AlertIndex = &pb.DisplayKey{
		Key:     cr.AlertIndex,
		Display: t.Text(lang, cr.Name),
	}
	r.Attributes = make(map[string]*structpb.Value)
	r.Template = make(map[string]*structpb.Value)
	for k, v := range cr.Template {
		data, err := structpb.NewValue(v)
		if err != nil {
			logrus.Errorf("%s transform any type is fail err is %s", v, err)
			return nil, err
		}
		r.Template[k] = data
	}
	//r.Template = cr.Template
	r.Window = int64(rule.Window)
	for k, v := range cr.Attributes {
		data, err := structpb.NewValue(v)
		if err != nil {
			logrus.Errorf("%s transform any type is fail err is %s", v, err)
			return nil, err
		}
		r.Attributes[k] = data
	}
	//r.Attributes = cr.Attributes
	r.Enable = cr.Enable
	r.CreateTime = cr.CreateTime.UnixNano() / int64(time.Millisecond)
	r.UpdateTime = cr.UpdateTime.UnixNano() / int64(time.Millisecond)
	r.IsRecover = getRecoverFromAttributes(rule.Attributes)
	for _, item := range rule.Functions {
		value, err := structpb.NewValue(item.Value)
		if err != nil {
			return nil, err
		}
		function := &pb.AlertRuleFunction{
			Field: &pb.DisplayKey{
				Key:     item.Field,
				Display: item.Field,
			},
			Aggregator: item.Aggregator,
			Operator:   item.Operator,
			Value:      value,
			DataType:   item.DataType,
			Unit:       item.Unit,
		}
		r.Functions = append(r.Functions, function)
	}
	return r, nil
}

// FromModel .
func (r *AlertRule) FromModel(lang i18n.LanguageCodes, t i18n.Translator, m *db.AlertRule) *AlertRule {
	r.ID = m.ID
	r.Name = m.Name
	r.AlertType = m.AlertType
	r.AlertScope = m.AlertScope
	r.AlertIndex = &DisplayKey{
		Key:     m.AlertIndex,
		Display: t.Text(lang, m.Name), // default as name
	}
	r.Template = m.Template
	r.IsRecover = getRecoverFromAttributes(m.Attributes)
	r.Attributes = m.Attributes
	r.Version = m.Version
	r.Enable = m.Enable
	r.CreateTime = m.CreateTime.UnixNano() / int64(time.Millisecond)
	r.UpdateTime = m.UpdateTime.UnixNano() / int64(time.Millisecond)
	window, ok := utils.GetMapValueInt64(m.Template, "window")
	if !ok {
		return nil
	}
	r.Window = window
	functions, ok := utils.GetMapValueArr(m.Template, "functions")
	if !ok {
		return nil
	}
	for _, f := range functions {
		function, ok := f.(map[string]interface{})
		if !ok {
			continue
		}

		field, ok := utils.GetMapValueString(function, "field")
		if !ok {
			continue
		}
		aggregator, ok := utils.GetMapValueString(function, "aggregator")
		if !ok {
			continue
		}
		operator, ok := utils.GetMapValueString(function, "operator")
		if !ok {
			continue
		}
		value, ok := function["value"]
		if !ok {
			continue
		}
		unit, _ := utils.GetMapValueString(function, "unit")
		dataType := TypeOf(value)
		if dataType == "" {
			continue
		}
		r.Functions = append(r.Functions, &AlertRuleFunction{
			Field: &DisplayKey{
				Key:     field,
				Display: field,
			},
			Aggregator: aggregator,
			Operator:   operator,
			Value:      value,
			DataType:   dataType,
			Unit:       unit,
		})
	}
	return r
}

func FromPBAlertRuleModel(lang i18n.LanguageCodes, t i18n.Translator, m *db.AlertRule) *pb.AlertRule {
	r := &pb.AlertRule{}
	r.Id = m.ID
	r.Name = m.Name
	r.AlertType = m.AlertType
	r.AlertScope = m.AlertScope
	r.AlertIndex = &pb.DisplayKey{
		Key:     m.AlertIndex,
		Display: t.Text(lang, m.Name), // default as name
	}
	r.Template = make(map[string]*structpb.Value)
	r.Attributes = make(map[string]*structpb.Value)
	for k, v := range m.Template {
		data, err := structpb.NewValue(v)
		if err != nil {
			logrus.Errorf("%s transform any type is fail err is %s", v, err)
			return nil
		}
		r.Template[k] = data
	}
	r.IsRecover = getRecoverFromAttributes(m.Attributes)
	for k, v := range m.Attributes {
		data, err := structpb.NewValue(v)
		if err != nil {
			logrus.Errorf("%s transform any type is fail err is %s", v, err)
			return nil
		}
		r.Attributes[k] = data
	}
	r.Version = m.Version
	r.Enable = m.Enable
	window, ok := utils.GetMapValueInt64(m.Template, "window")
	if !ok {
		return nil
	}
	r.Window = window
	functions, ok := utils.GetMapValueArr(m.Template, "functions")
	if !ok {
		return nil
	}
	for _, f := range functions {
		function, ok := f.(map[string]interface{})
		if !ok {
			continue
		}

		field, ok := utils.GetMapValueString(function, "field")
		if !ok {
			continue
		}
		aggregator, ok := utils.GetMapValueString(function, "aggregator")
		if !ok {
			continue
		}
		operator, ok := utils.GetMapValueString(function, "operator")
		if !ok {
			continue
		}
		value, ok := function["value"]
		if !ok {
			continue
		}
		unit, _ := utils.GetMapValueString(function, "unit")
		dataType := TypeOf(value)
		if dataType == "" {
			continue
		}
		valueData, err := structpb.NewValue(value)
		if err != nil {
			logrus.Errorf("%s transform any type is fail err is %s", value, err)
			return nil
		}
		r.Functions = append(r.Functions, &pb.AlertRuleFunction{
			Field: &pb.DisplayKey{
				Key:     field,
				Display: field,
			},
			Aggregator: aggregator,
			Operator:   operator,
			Value:      valueData,
			DataType:   dataType,
			Unit:       unit,
		})
	}
	return r
}

func ToPBAlertNotify(m *db.AlertNotify, notifyGroupMap map[int64]*pb.NotifyGroup) *pb.AlertNotify {
	t, unit := convertMillisecondToUnit(m.Silence)
	n := &pb.AlertNotify{}
	n.Id = m.ID
	n.Silence = &pb.AlertNotifySilence{Value: t, Unit: unit, Policy: m.SilencePolicy}
	n.CreateTime = m.Created.UnixNano() / int64(time.Millisecond)
	n.UpdateTime = m.Updated.UnixNano() / int64(time.Millisecond)
	// fill in the alarm notification target
	notifyType, ok := utils.GetMapValueString(m.NotifyTarget, "type")
	if !ok {
		return nil
	}
	if notifyType == "notify_group" {
		// notify group ID
		groupID, ok := utils.GetMapValueInt64(m.NotifyTarget, "group_id")
		if !ok {
			return nil
		}
		// notify group method
		groupType, ok := utils.GetMapValueString(m.NotifyTarget, "group_type")
		if !ok {
			return nil
		}
		groupLevel, _ := utils.GetMapValueString(m.NotifyTarget, "level")
		n.Type = notifyType
		n.GroupId = groupID
		n.GroupType = groupType
		n.NotifyGroup = notifyGroupMap[groupID]
		n.Level = groupLevel
	} else if notifyType == "dingding" {
		dingdingURL, ok := utils.GetMapValueString(m.NotifyTarget, "dingding_url")
		if !ok {
			return nil
		}
		n.Type = notifyType
		n.DingdingUrl = dingdingURL
	} else {
		return nil
	}
	return n
}

// ToModel .
func (n *AlertNotify) ToModel(alert *Alert, silencePolicies map[string]bool) *db.AlertNotify {
	if alert == nil || n.Silence == nil {
		return nil
	}
	t := convertMillisecondByUnit(n.Silence.Value, n.Silence.Unit)
	if t < 0 {
		return nil
	}
	if n.Silence.Policy == "" || !silencePolicies[n.Silence.Policy] {
		n.Silence.Policy = fixedSliencePolicy
	}
	target := make(map[string]interface{})
	if n.GroupID != 0 && n.GroupType != "" {
		target["type"] = "notify_group"
		target["group_id"] = n.GroupID
		target["group_type"] = n.GroupType
	} else if n.DingdingURL != "" {
		target["type"] = "dingding"
		target["dingding_url"] = n.DingdingURL
	} else {
		return nil
	}
	return &db.AlertNotify{
		ID:             n.ID,
		AlertID:        alert.ID,
		NotifyTarget:   target,
		NotifyTargetID: "",
		Silence:        t,
		SilencePolicy:  n.Silence.Policy,
		Enable:         alert.Enable,
	}
}

func FromDBAlertToModel(n *pb.AlertNotify, alert *pb.Alert, silencePolicies map[string]bool) *db.AlertNotify {
	if alert == nil || n.Silence == nil {
		return nil
	}
	t := convertMillisecondByUnit(n.Silence.Value, n.Silence.Unit)
	if t < 0 {
		return nil
	}
	if n.Silence.Policy == "" || !silencePolicies[n.Silence.Policy] {
		n.Silence.Policy = fixedSliencePolicy
	}
	target := make(map[string]interface{})
	if n.GroupId != 0 && n.GroupType != "" {
		target["type"] = "notify_group"
		target["group_id"] = n.GroupId
		target["group_type"] = n.GroupType
		target["level"] = n.Level
	} else if n.DingdingUrl != "" {
		target["type"] = "dingding"
		target["dingding_url"] = n.DingdingUrl
	} else {
		return nil
	}
	return &db.AlertNotify{
		ID:             n.Id,
		AlertID:        alert.Id,
		NotifyTarget:   target,
		NotifyTargetID: "",
		Silence:        t,
		SilencePolicy:  n.Silence.Policy,
		Enable:         alert.Enable,
	}
}

func ToPBAlertExpressionModel(expression *db.AlertExpression) *pb.AlertExpression {
	e := &pb.AlertExpression{}
	e.Id = expression.ID
	e.CreateTime = expression.Created.Unix()
	e.UpdateTime = expression.Updated.Unix()
	ruleIDStr, ok := utils.GetMapValueString(expression.Attributes, "rule_id")
	if !ok {
		return nil
	}
	ruleID, err := strconv.ParseUint(ruleIDStr, 10, 64)
	if err != nil {
		return nil
	}
	e.RuleId = ruleID
	alertIndex, ok := utils.GetMapValueString(expression.Attributes, "alert_index")
	if !ok {
		return nil
	}
	e.AlertIndex = alertIndex
	e.IsRecover = getRecoverFromAttributes(expression.Attributes)
	window, ok := utils.GetMapValueInt64(expression.Expression, "window")
	if !ok {
		return nil
	}
	e.Window = window
	level, ok := utils.GetMapValueString(expression.Attributes, "level")
	if !ok {
		return nil
	}
	e.Level = strings.Title(strings.ToLower(level))

	functions, ok := utils.GetMapValueArr(expression.Expression, "functions")
	if !ok {
		return nil
	}
	for _, functionValue := range functions {
		function, ok := functionValue.(map[string]interface{})
		if !ok {
			continue
		}
		field, ok := utils.GetMapValueString(function, "field")
		if !ok {
			continue
		}
		aggregator, ok := utils.GetMapValueString(function, "aggregator")
		if !ok {
			continue
		}
		operator, ok := utils.GetMapValueString(function, "operator")
		if !ok {
			continue
		}
		value, ok := function["value"]
		if !ok {
			continue
		}
		respValue, err := structpb.NewValue(value)
		if err != nil {
			logrus.Errorf("function value %v transform is fail", value)
			return nil
		}
		e.Functions = append(e.Functions, &pb.AlertExpressionFunction{
			Field:      field,
			Aggregator: aggregator,
			Operator:   operator,
			Value:      respValue,
		})
	}
	return e
}

func ToDBAlertExpressionModel(e *pb.AlertExpression, orgName string, alert *pb.Alert, rule *pb.AlertRule) (*db.AlertExpression, error) {
	attributes := make(map[string]interface{})
	for k, v := range rule.Attributes {
		attributes[k] = v.AsInterface()
	}
	for k, v := range alert.Attributes {
		attributes[k] = v.AsInterface()
	}
	attributes["alert_id"] = strconv.FormatUint(alert.Id, 10)
	attributes["rule_id"] = strconv.FormatUint(rule.Id, 10)
	attributes["alert_type"] = rule.AlertType
	attributes["alert_index"] = rule.AlertIndex.Key
	attributes["alert_name"] = rule.Name
	attributes["alert_title"] = alert.Name
	attributes["alert_scope"] = alert.AlertScope
	attributes["alert_scope_id"] = alert.AlertScopeId
	attributes["recover"] = strconv.FormatBool(e.IsRecover)
	attributes["level"] = e.Level
	// remove some fields that are not needed by flink to avoid too long attributes
	for _, item := range []string{"alert_domain", "alert_dashboard_id", "alert_dashboard_path", "alert_record_path"} {
		delete(attributes, item)
	}

	expression := rule.Template
	window := structpb.NewNumberValue(float64(e.Window))
	expression["window"] = window
	// fill expression filters
	expressionMap := (&Adapt{}).ValueMapToInterfaceMap(expression)
	filters, _ := utils.GetMapValueArr(expressionMap, "filters")
	for index, filterValue := range filters {
		filterMap, ok := filterValue.(map[string]interface{})
		if !ok {
			continue
		}
		tag, ok := utils.GetMapValueString(filterMap, "tag")
		if !ok {
			continue
		}
		operator, ok := utils.GetMapValueString(filterMap, "operator")
		if !ok {
			continue
		}
		opType := filterOperatorRel[operator]
		value, ok := filterMap["value"]
		if !ok {
			continue
		}
		if (tag == applicationIdTag && value == applicationIdValue) || (tag == clusterNameTag && value == clusterNameValue) {
			filters = append(filters[0:index], filters[index+1:]...)
			continue
		}
		if tag == targetTerminusKey {
			tag = terminusKey
		}
		if attr, ok := attributes[tag]; ok {
			val, err := formatOperatorValue(opType, utils.StringType, attr)
			if err != nil {
				return nil, err
			}
			value = val
		}
		filterMap["value"] = value
	}
	for _, v := range alert.TriggerCondition {
		filterMap := make(map[string]interface{})
		tag := v.Condition
		operator := v.Operator
		if operator == all {
			operator = any
		}
		value := v.Values
		opType := filterOperatorRel[operator]
		val, err := formatOperatorValue(opType, utils.StringType, value)
		if err != nil {
			return nil, err
		}
		filterMap["tag"] = tag
		filterMap["operator"] = operator
		if operator != any {
			filterMap["value"] = val
		}
		filters = append(filters, filterMap)
	}
	filtersValue, err := structpb.NewList(filters)
	if err != nil {
		return nil, err
	}
	expression["filters"] = structpb.NewListValue(filtersValue)

	// fill expression functions
	functionMap := make(map[string]*pb.AlertExpressionFunction)
	for _, function := range e.Functions {
		functionMap[function.Aggregator+"-"+function.Field] = function
	}
	functions, _ := utils.GetMapValueArr(expressionMap, "functions")
	for _, functionValue := range functions {
		function, ok := functionValue.(map[string]interface{})
		if !ok {
			continue
		}
		field, ok := utils.GetMapValueString(function, "field")
		if !ok {
			continue
		}
		aggregator, _ := utils.GetMapValueString(function, "aggregator")
		if !ok {
			continue
		}
		newFunction, ok := functionMap[aggregator+"-"+field]
		if !ok {
			continue
		}
		operator, _ := utils.GetMapValueString(function, "operator")
		if operator != "" && newFunction.Operator != "" {
			operator = newFunction.Operator
		}
		function["operator"] = operator

		value, _ := function["value"]
		if value != nil && newFunction.Value != nil {
			opType := functionOperatorRel[operator]
			dataType := utils.TypeOf(value)
			val, err := formatOperatorValue(opType, dataType, newFunction.Value.AsInterface())
			if err != nil {
				return nil, err
			}
			function["value"] = val
		}
	}

	functionsValue, err := structpb.NewList(functions)
	if err != nil {
		return nil, err
	}
	expression["functions"] = structpb.NewListValue(functionsValue)

	// transform alert url
	alertDomain := alert.Domain
	if alertDomain == "" {
		if s, ok := utils.GetMapValueString(attributes, "alert_domain"); ok {
			alertDomain = s
		}
	}
	if strings.HasSuffix(alertDomain, "/") {
		alertDomain = alertDomain[0 : len(alertDomain)-1]
	}
	ruleAttributesMap := (&Adapt{}).ValueMapToInterfaceMap(rule.Attributes)
	if routeID, ok := utils.GetMapValueString(ruleAttributesMap, "display_url_id"); ok {
		if alertURL := convertAlertURL(alertDomain, orgName, routeID, attributes); alertURL != "" {
			attributes["display_url"] = alertURL
		}
	}
	alertAttributesMap := (&Adapt{}).ValueMapToInterfaceMap(alert.Attributes)
	if dashboardID, ok := utils.GetMapValueString(ruleAttributesMap, "alert_dashboard_id"); ok {
		if dashboardPath, ok := utils.GetMapValueString(alertAttributesMap, "alert_dashboard_path"); ok {
			// get group's value
			var groups []string
			group, _ := utils.GetMapValueArr(expressionMap, "group")
			for _, item := range group {
				groups = append(groups, item.(string))
			}
			attributes["display_url"] = convertDashboardURL(alertDomain, orgName, dashboardPath, dashboardID, groups)
		}
	}

	// transform record url
	if recordPath, ok := utils.GetMapValueString(alertAttributesMap, "alert_record_path"); ok {
		attributes["record_url"] = convertRecordURL(alertDomain, orgName, recordPath)
	}
	expressionJsonMap := jsonmap.JSONMap{}
	for k, v := range expression {
		expressionJsonMap[k] = v.AsInterface()
	}
	return &db.AlertExpression{
		ID:         e.Id,
		AlertID:    alert.Id,
		Expression: expressionJsonMap,
		Attributes: attributes,
		Version:    "3.0",
		Enable:     alert.Enable,
	}, nil
}

// format operation
func formatOperatorValue(opType string, dataType string, value interface{}) (interface{}, error) {
	if opType == "" || dataType == "" {
		return nil, invalidParameter("%s not support %s data type", opType, dataType)
	}
	switch opType {
	case OperatorTypeNone:
		return nil, nil
	case OperatorTypeOne:
		val, err := convertDataByType(value, dataType)
		if err != nil {
			return nil, err
		}
		return val, nil
	case OperatorTypeMore:
		switch value.(type) {
		case string, []byte:
			value, ok := convertString(value)
			if !ok {
				return nil, invalidParameter("%s not support %v data", opType, value)
			}
			var values []interface{}
			for _, val := range strings.Split(value, ",") {
				v, err := convertDataByType(val, dataType)
				if err != nil {
					return nil, err
				}
				values = append(values, v)
			}
			return values, nil
		default:
			var values []interface{}
			valueOf := reflect.ValueOf(value)
			switch valueOf.Kind() {
			case reflect.Slice, reflect.Array:
				for i := 0; i < valueOf.Len(); i++ {
					val, err := convertDataByType(valueOf.Index(i).Interface(), dataType)
					if err != nil {
						return nil, err
					}
					values = append(values, val)
				}
			default:
				val, err := convertDataByType(value, dataType)
				if err != nil {
					return nil, err
				}
				values = append(values, val)
			}
			return values, nil
		}
	}
	return nil, invalidParameter("%s not support", opType)
}

// FromModel .
//func (a *Alert) FromModel(m *db.Alert) *Alert {
//	a.ID = m.ID
//	a.Name = m.Name
//	a.AlertScope = m.AlertScope
//	a.AlertScopeID = m.AlertScopeID
//	a.Attributes = m.Attributes
//	a.Enable = m.Enable
//	a.CreateTime = m.Created.UnixNano() / int64(time.Millisecond)
//	a.UpdateTime = m.Updated.UnixNano() / int64(time.Millisecond)
//	return a
//}

func FromDBAlertModel(m *db.Alert) *pb.Alert {
	a := &pb.Alert{}
	a.Id = m.ID
	a.Name = m.Name
	a.AlertScope = m.AlertScope
	a.AlertScopeId = m.AlertScopeID
	a.Enable = m.Enable
	a.CreateTime = m.Created.UnixNano() / int64(time.Millisecond)
	a.UpdateTime = m.Updated.UnixNano() / int64(time.Millisecond)
	a.Attributes = make(map[string]*structpb.Value)
	a.Creator = m.CreatorID
	for k, v := range m.Attributes {
		data, err := structpb.NewValue(v)
		if err != nil {
			logrus.Errorf("%s transform any type is fail err is %s", v, err)
			return nil
		}
		a.Attributes[k] = data
	}
	return a
}

// ToModel .
//func (a *Alert) ToModel() *db.Alert {
//	return &db.Alert{
//		ID:           a.ID,
//		Name:         a.Name,
//		AlertScope:   a.AlertScope,
//		AlertScopeID: a.AlertScopeID,
//		Attributes:   a.Attributes,
//		Enable:       a.Enable,
//	}
//}
func ToDBAlertModel(a *pb.Alert) *db.Alert {
	dbAlert := &db.Alert{
		ID:           a.Id,
		Name:         a.Name,
		AlertScope:   a.AlertScope,
		AlertScopeID: a.AlertScopeId,
		Enable:       a.Enable,
		Attributes:   make(jsonmap.JSONMap),
		CreatorID:    a.Attributes["creator_id"].GetStringValue(),
	}
	for k, v := range a.Attributes {
		dbAlert.Attributes[k] = v.AsInterface()
	}
	return dbAlert
}

func getRecoverFromAttributes(m map[string]interface{}) bool {
	if s, ok := utils.GetMapValueString(m, "recover"); ok {
		if v, err := strconv.ParseBool(s); err == nil {
			return v
		}
	}
	return false
}
