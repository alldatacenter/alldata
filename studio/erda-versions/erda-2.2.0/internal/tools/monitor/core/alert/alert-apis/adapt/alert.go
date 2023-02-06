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
	"errors"
	"fmt"
	"strconv"
	"strings"

	"github.com/sirupsen/logrus"
	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/monitor/core/alert/alert-apis/db"
	"github.com/erda-project/erda/internal/tools/monitor/core/expression"
	"github.com/erda-project/erda/internal/tools/monitor/utils"
)

type (
	// AlertRule .
	AlertRule struct {
		ID         uint64                 `json:"id"`
		Name       string                 `json:"-"`
		AlertScope string                 `json:"-"`
		AlertType  string                 `json:"-"`
		AlertIndex *DisplayKey            `json:"alertIndex"`
		Template   map[string]interface{} `json:"-"`
		Window     int64                  `json:"window"`
		Functions  []*AlertRuleFunction   `json:"functions"`
		IsRecover  bool                   `json:"isRecover"`
		Attributes map[string]interface{} `json:"-"`
		Version    string                 `json:"-"`
		Enable     bool                   `json:"-"`
		CreateTime int64                  `json:"createTime"`
		UpdateTime int64                  `json:"updateTime"`
	}
	// AlertRuleFunction .
	AlertRuleFunction struct {
		Field      *DisplayKey `json:"field"`
		Aggregator string      `json:"aggregator"`
		Operator   string      `json:"operator"`
		Value      interface{} `json:"value"`
		DataType   string      `json:"dataType"`
		Unit       string      `json:"unit" translate:"true"`
	}
	// AlertTypeRuleResp .
	AlertTypeRuleResp struct {
		AlertTypeRules []*AlertTypeRule `json:"alertTypeRules"`
		Windows        []int64          `json:"windows"`
		Operators      []*Operator      `json:"operators"`
		Aggregator     []*DisplayKey    `json:"aggregator"`
		Silence        []*NotifySilence `json:"silence"`
	}
	// AlertTypeRule .
	AlertTypeRule struct {
		AlertType *DisplayKey  `json:"alertType"`
		Rules     []*AlertRule `json:"rules"`
	}
	// AlertNotify .
	AlertNotify struct {
		ID          uint64                  `json:"id"`
		Type        string                  `json:"type"`
		GroupID     int64                   `json:"groupId"`
		GroupType   string                  `json:"groupType"`
		NotifyGroup *apistructs.NotifyGroup `json:"notifyGroup"`
		DingdingURL string                  `json:"dingdingUrl"`
		Silence     *AlertNotifySilence     `json:"silence"`
		CreateTime  int64                   `json:"createTime"`
		UpdateTime  int64                   `json:"updateTime"`
	}
	// AlertNotifySilence .
	AlertNotifySilence struct {
		Value  int64  `json:"value"`
		Unit   string `json:"unit"`
		Policy string `json:"policy"`
	}
	// AlertExpression .
	AlertExpression struct {
		ID         uint64                     `json:"id"`
		RuleID     uint64                     `json:"ruleId"`
		AlertIndex string                     `json:"alertIndex"`
		Window     int64                      `json:"window"`
		Functions  []*AlertExpressionFunction `json:"functions"`
		IsRecover  bool                       `json:"isRecover"`
		CreateTime int64                      `json:"createTime"`
		UpdateTime int64                      `json:"updateTime"`
	}
	// AlertExpressionFunction .
	AlertExpressionFunction struct {
		Field      string      `json:"field"`
		Aggregator string      `json:"aggregator"`
		Operator   string      `json:"operator"`
		Value      interface{} `json:"value"`
	}
	// Alert .
	Alert struct {
		ID           uint64                 `json:"id"`
		Name         string                 `json:"name"`
		AlertScope   string                 `json:"alertScope"`
		AlertScopeID string                 `json:"alertScopeId"`
		Enable       bool                   `json:"enable"`
		Rules        []*AlertExpression     `json:"rules"`
		Notifies     []*AlertNotify         `json:"notifies"`
		Filters      map[string]interface{} `json:"filters"`
		Attributes   map[string]interface{} `json:"attributes"`
		ClusterNames []string               `json:"clusterNames"`
		Domain       string                 `json:"domain"`
		CreateTime   int64                  `json:"createTime"`
		UpdateTime   int64                  `json:"updateTime"`
	}
)

// NotifyTargetType .
type NotifyTargetType string

// NotifyTargetType values
const (
	SysNotifyTarget                NotifyTargetType = "sys"
	OrgNotifyTarget                NotifyTargetType = "org"
	ProjectNotifyTarget            NotifyTargetType = "project"
	AppNotifyTarget                NotifyTargetType = "app"
	UserNotifyTarget               NotifyTargetType = "user"
	ExternalUserNotifyTarget       NotifyTargetType = "external_user"
	DingdingNotifyTarget           NotifyTargetType = "dingding"
	DingdingWorkNoticeNotifyTarget NotifyTargetType = "dingding_worknotice"
	WebhookNotifyTarget            NotifyTargetType = "webhook"

	dashboardPath = "/dataCenter/customDashboard"
	recordPath    = "/cmp/alarm/record/events/{{family_id}}"

	TriggerCondition = "trigger_condition"
)

// QueryAlertRule .
func (a *Adapt) QueryAlertRule(lang i18n.LanguageCodes, scope, scopeID string) (*pb.AlertTypeRuleResp, error) {
	//rules, err := a.db.AlertRule.QueryEnabledByScope(scope)
	//if err != nil {
	//	return nil, err
	//}
	rules := make([]*db.AlertRule, 0)
	for k, v := range expression.AlertConfig {
		if v.AlertScope == scope {
			expressionIndex := expression.ExpressionIndex[k]
			expressionConfig := expression.AlertConfig[k]
			rule := &db.AlertRule{
				Name:       expressionConfig.Name,
				AlertScope: expressionConfig.AlertScope,
				AlertType:  expressionConfig.AlertType,
				AlertIndex: expressionConfig.Id,
				Template:   expressionIndex.Expression,
				Attributes: expressionConfig.Attributes,
			}
			rules = append(rules, rule)
		}
	}

	customizeRules, err := a.db.CustomizeAlertRule.QueryEnabledByScope(scope, scopeID)
	if err != nil {
		return nil, err
	}
	rulesMap := make(map[string][]*pb.AlertRule)
	for _, item := range customizeRules {
		rule, err := FromCustomizeAlertRule(lang, a.t, item)
		if err != nil {
			return nil, err
		}
		rulesMap[item.AlertType] = append(rulesMap[item.AlertType], rule)
	}
	for _, item := range rules {
		rule := FromPBAlertRuleModel(lang, a.t, item)
		rulesMap[item.AlertType] = append(rulesMap[item.AlertType], rule)
	}
	var alertTypeRules []*pb.AlertTypeRule
	for alertType, rules := range rulesMap {
		alertTypeRules = append(alertTypeRules, &pb.AlertTypeRule{
			AlertType: &pb.DisplayKey{
				Key:     alertType,
				Display: a.t.Text(lang, alertType),
			},
			Rules: rules,
		})
	}
	// only show single value operation
	var operators []*pb.Operator
	for _, op := range a.FunctionOperatorKeys(lang) {
		if op.Type == OperatorTypeOne {
			operators = append(operators, op)
		}
	}
	return &pb.AlertTypeRuleResp{
		AlertTypeRules: alertTypeRules,
		Windows:        windowKeys,
		Operators:      operators,
		Aggregator:     a.AggregatorKeys(lang),
		Silence:        a.NotifySilences(lang),
	}, nil
}

// QueryOrgAlertRule .
func (a *Adapt) QueryOrgAlertRule(lang i18n.LanguageCodes, orgID uint64) (*pb.AlertTypeRuleResp, error) {
	return a.QueryAlertRule(lang, "org", strconv.FormatUint(orgID, 10))
}

// QueryAlert .
func (a *Adapt) QueryAlert(code i18n.LanguageCodes, scope, scopeID string, pageNo, pageSize uint64, name string) ([]*pb.Alert, []string, error) {
	alerts, err := a.db.Alert.QueryByScopeAndScopeID(scope, scopeID, pageNo, pageSize, name)
	if err != nil {
		return nil, nil, err
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
	notifyMap, err := a.getAlertNotifysByAlertIDs(alertIDs)
	if err != nil {
		return nil, nil, err
	}
	ruleCountMap, err := a.getRuleCountByAlertIDs(alertIDs)
	if err != nil {
		return nil, nil, err
	}
	var list []*pb.Alert
	for _, item := range alerts {
		alert := FromDBAlertModel(item)
		alert.Notifies = notifyMap[alert.Id]
		alert.RuleCount = ruleCountMap[alert.Id]
		list = append(list, alert)
	}
	return list, userIDs, nil
}

// get alert rules count
func (a *Adapt) getRuleCountByAlertIDs(alertIDs []uint64) (map[uint64]int64, error) {
	if len(alertIDs) == 0 {
		return nil, nil
	}
	ruleCounts, err := a.db.AlertExpression.QueryRuleCount(alertIDs)
	if err != nil {
		return nil, err
	}
	return ruleCounts, nil
}

// according to alertID get alert
func (a *Adapt) getAlertNotifysByAlertIDs(alertIDs []uint64) (map[uint64][]*pb.AlertNotify, error) {
	if len(alertIDs) == 0 {
		return nil, nil
	}
	notifies, err := a.db.AlertNotify.QueryByAlertIDs(alertIDs)
	if err != nil {
		return nil, err
	}
	var notifyGroupIDs []string
	for _, notify := range notifies {
		if groupID, ok := utils.GetMapValueUint64(notify.NotifyTarget, "group_id"); ok {
			notifyGroupIDs = append(notifyGroupIDs, strconv.FormatUint(groupID, 10))
		}
	}
	notifyGroupMap := a.getNotifyGroupRelByIDs(notifyGroupIDs)

	notifysMap := make(map[uint64][]*pb.AlertNotify)
	for _, notify := range notifies {
		notifyTarget := ToPBAlertNotify(notify, notifyGroupMap)
		if notifyTarget == nil {
			continue
		}
		notifysMap[notify.AlertID] = append(notifysMap[notify.AlertID], notifyTarget)
	}
	return notifysMap, nil
}

// get notify groups
func (a *Adapt) getNotifyGroupRelByIDs(groupIDs []string) map[int64]*pb.NotifyGroup {
	if len(groupIDs) == 0 {
		return nil
	}
	notifyGroupsData, err := a.cmdb.QueryNotifyGroup(groupIDs)
	if err != nil {
		a.l.Errorf("fail to query notify group from cmdb error: %s", err)
		return nil
	}
	notifyGroupMap := make(map[int64]*pb.NotifyGroup)
	for _, notifyGroup := range notifyGroupsData {
		data, err := json.Marshal(notifyGroup)
		if err != nil {
			a.l.Errorf("json marshal is fail err is %s", err)
			return nil
		}
		notifyGroupPB := &pb.NotifyGroup{}
		err = json.Unmarshal(data, notifyGroupPB)
		if err != nil {
			a.l.Errorf("json unMarshal is fail is %s", err)
			return nil
		}
		notifyGroupMap[notifyGroup.ID] = notifyGroupPB
	}
	return notifyGroupMap
}

// QueryOrgAlert .
func (a *Adapt) QueryOrgAlert(lang i18n.LanguageCodes, orgID uint64, pageNo, pageSize uint64, name string) ([]*pb.Alert, []string, error) {
	scopeID := strconv.FormatUint(orgID, 10)
	alerts, userIds, err := a.QueryAlert(lang, "org", scopeID, pageNo, pageSize, name)
	if err != nil {
		return nil, nil, err
	}
	for _, alert := range alerts {
		output := a.ValueMapToInterfaceMap(alert.Attributes)
		if clusterNames, ok := utils.GetMapValueArr(output, "cluster_name"); ok {
			for _, v := range clusterNames {
				if clusterName, ok := v.(string); ok {
					alert.ClusterNames = append(alert.ClusterNames, clusterName)
				}
			}
		} else if clusterName, ok := utils.GetMapValueString(output, "cluster_name"); ok {
			alert.ClusterNames = append(alert.ClusterNames, clusterName)
		}
		alert.Attributes = nil
	}
	return alerts, userIds, nil
}

// CountAlert .
func (a *Adapt) CountAlert(scope, scopeID, name string) (int, error) {
	count, err := a.db.Alert.CountByScopeAndScopeID(scope, scopeID, name)
	if err != nil {
		return 0, err
	}
	return count, nil
}

// CountOrgAlert .
func (a *Adapt) CountOrgAlert(orgID uint64, name string) (int, error) {
	return a.CountAlert("org", strconv.FormatUint(orgID, 10), name)
}

// GetAlert .
func (a *Adapt) GetAlert(lang i18n.LanguageCodes, id uint64) (*pb.Alert, error) {
	alert, err := a.db.Alert.GetByID(id)
	if err != nil {
		return nil, err
	} else if alert == nil {
		return nil, nil
	}
	return FromDBAlertModel(alert), nil
}

// GetAlertDetail .
func (a *Adapt) GetAlertDetail(lang i18n.LanguageCodes, id uint64) (*pb.Alert, error) {
	alert, err := a.db.Alert.GetByID(id)
	if err != nil {
		return nil, err
	} else if alert == nil {
		return nil, nil
	}
	// get alert expression
	expressions, err := a.getAlertExpressionsByAlertID(alert.ID)
	if err != nil {
		return nil, err
	}
	// filter alarm expressions that do not match the rule
	var indices []string
	for _, item := range expressions {
		indices = append(indices, item.AlertIndex)
	}
	rulesMap, err := a.getEnabledAlertRulesByScopeAndIndices(lang, alert.AlertScope, alert.AlertScopeID, indices)
	if err != nil {
		return nil, err
	}
	var rules []*pb.AlertExpression
	for _, expression := range expressions {
		if _, ok := rulesMap[expression.AlertIndex]; ok {
			rules = append(rules, expression)
		}
	}
	// get alert notify
	notifys, err := a.getAlertNotifysByAlertID(alert.ID)
	if err != nil {
		return nil, err
	}
	data := FromDBAlertModel(alert)
	data.Rules = rules
	data.Notifies = notifys
	return data, nil
}

func (a *Adapt) getAlertExpressionsByAlertID(alertID uint64) ([]*pb.AlertExpression, error) {
	expressions, err := a.db.AlertExpression.QueryByAlertIDs([]uint64{alertID})
	if err != nil {
		return nil, err
	}
	var list []*pb.AlertExpression
	for _, item := range expressions {
		expression := ToPBAlertExpressionModel(item)
		if expression == nil {
			continue
		}
		list = append(list, expression)
	}
	return list, nil
}

func (a *Adapt) getAlertNotifysByAlertID(alertID uint64) ([]*pb.AlertNotify, error) {
	if alertID == 0 {
		return nil, nil
	}
	notifysMap, err := a.getAlertNotifysByAlertIDs([]uint64{alertID})
	if err != nil {
		return nil, err
	}
	return notifysMap[alertID], nil
}

// obtain open alarm rules based on scope and index
func (a *Adapt) getEnabledAlertRulesByScopeAndIndices(lang i18n.LanguageCodes, scope, scopeID string, indices []string) (map[string]*pb.AlertRule, error) {
	if len(indices) == 0 {
		return nil, nil
	}
	rules := make([]*db.AlertRule, 0)
	for _, v := range indices {
		rule, ok := expression.ExpressionIndex[v]
		if ok {
			expressionConfig := expression.AlertConfig[v]
			alertRule := &db.AlertRule{
				Name:       expressionConfig.Name,
				AlertScope: expressionConfig.AlertScope,
				AlertIndex: expressionConfig.Id,
				Template:   rule.Expression,
				Attributes: expressionConfig.Attributes,
				AlertType:  expressionConfig.AlertType,
			}
			rules = append(rules, alertRule)
		}
	}

	customizeRules, err := a.db.CustomizeAlertRule.QueryEnabledByScopeAndIndices(scope, scopeID, indices)
	if err != nil {
		return nil, err
	}
	rulesMap := make(map[string]*pb.AlertRule)
	for _, item := range rules {
		rulesMap[item.AlertIndex] = FromPBAlertRuleModel(lang, a.t, item)
	}
	for _, item := range customizeRules {
		rulesMap[item.AlertIndex], err = FromCustomizeAlertRule(lang, a.t, item)
	}
	return rulesMap, nil
}

// GetOrgAlertDetail .
func (a *Adapt) GetOrgAlertDetail(lang i18n.LanguageCodes, id uint64) (*pb.Alert, error) {
	alert, err := a.GetAlertDetail(lang, id)
	if err != nil {
		return nil, err
	}
	if alert == nil {
		return nil, nil
	}
	output := a.ValueMapToInterfaceMap(alert.Attributes)
	if clusterNames, ok := utils.GetMapValueArr(output, "cluster_name"); ok {
		names := make([]string, 0)
		condition := &pb.TriggerCondition{
			Condition: ClusterName,
			Operator:  "in",
		}
		for _, v := range clusterNames {
			if clusterName, ok := v.(string); ok {
				names = append(names, clusterName)
			}
		}
		condition.Values = strings.Join(names, ",")
		alert.TriggerCondition = append(alert.TriggerCondition, condition)
	} else if clusterName, ok := utils.GetMapValueString(output, "cluster_name"); ok {
		condition := &pb.TriggerCondition{
			Condition: ClusterName,
			Operator:  "in",
			Values:    clusterName,
		}
		alert.TriggerCondition = append(alert.TriggerCondition, condition)
	} else {
		if condition, ok := output[TriggerCondition]; ok {
			conditions := make([]*pb.TriggerCondition, 0)
			err = json.Unmarshal([]byte(condition.(string)), &conditions)
			if err != nil {
				return nil, err
			}
			alert.TriggerCondition = conditions
		}
	}
	alert.Attributes = nil
	return alert, nil
}

// CreateAlert .
func (a *Adapt) CreateAlert(alert *pb.Alert) (alertID uint64, err error) {
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
	orgName := alert.Attributes["org_name"].AsInterface().(string)
	dbAlert, err := tx.Alert.GetByScopeAndScopeIDAndName(alert.AlertScope, alert.AlertScopeId, alert.Name)
	if err != nil {
		return 0, err
	}
	if dbAlert != nil {
		return 0, ErrorAlreadyExists
	}
	alert.Enable = true
	data := ToDBAlertModel(alert)
	data.ID = 0
	err = tx.Alert.Insert(data)
	if err != nil {
		return 0, nil
	}
	alert.Id = data.ID

	// 创建告警表达式
	var (
		types, indexes []string
		expressionLen  int
	)
	for _, expression := range alert.Rules {
		indexes = append(indexes, expression.AlertIndex)
	}
	ruleMap, err := a.getEnabledAlertRulesByScopeAndIndices(i18n.LanguageCodes{}, alert.AlertScope, alert.AlertScopeId, indexes)
	if err != nil {
		return 0, err
	}
	for _, expression := range alert.Rules {
		rule, ok := ruleMap[expression.AlertIndex]
		if !ok || rule.AlertScope != alert.AlertScope {
			return 0, invalidParameter("rule %s is not scope: %s", rule.AlertIndex.Key, alert.AlertScope)
		}
		exp, err := ToDBAlertExpressionModel(expression, orgName, alert, rule)
		if err != nil {
			return 0, err
		}
		exp.ID = 0
		if err := tx.AlertExpression.Insert(exp); err != nil {
			return 0, err
		}
		expressionLen++
		types = append(types, rule.AlertType)
	}
	if expressionLen == 0 {
		return 0, errors.New("expression is not valid")
	}

	// create alert notify
	var (
		silence   *pb.AlertNotifySilence
		notifyLen int
	)
	for _, item := range alert.Notifies {
		silence = item.Silence
		notify := FromDBAlertToModel(item, alert, a.silencePolicies)
		if notify == nil {
			continue
		}
		notify.ID = 0
		if err := tx.AlertNotify.Insert(notify); err != nil {
			return 0, err
		}
		notifyLen++
	}
	if notifyLen == 0 {
		return 0, errors.New("notify is not valid")
	}
	// create ticket alert notify
	notify := a.newTicketAlertNotify(alert.Id, silence)
	if err := tx.AlertNotify.Insert(notify); err != nil {
		return 0, err
	}
	return alert.Id, nil
}

// crate ticket alert notify
func (a *Adapt) newTicketAlertNotify(alertID uint64, silence *pb.AlertNotifySilence) *db.AlertNotify {
	if silence == nil {
		return nil
	}
	t := convertMillisecondByUnit(silence.Value, silence.Unit)
	if t < 0 {
		return nil
	}
	if silence.Policy == "" || !a.silencePolicies[silence.Policy] {
		silence.Policy = fixedSliencePolicy
	}
	return &db.AlertNotify{
		AlertID: alertID,
		NotifyTarget: map[string]interface{}{
			"type": "ticket",
		},
		NotifyTargetID: "",
		Silence:        t,
		SilencePolicy:  silence.Policy,
		Enable:         true,
	}
}

// CreateOrgAlert .
func (a *Adapt) CreateOrgAlert(alert *pb.Alert, orgID string) (alertID uint64, err error) {
	alert.AlertScope = "org"
	alert.AlertScopeId = orgID
	alert.Attributes["alert_domain"] = structpb.NewStringValue(alert.Domain)
	alertDashboardPath := structpb.NewStringValue(dashboardPath)
	alert.Attributes["alert_dashboard_path"] = alertDashboardPath
	alertRecordPath := structpb.NewStringValue(recordPath)
	alert.Attributes["alert_record_path"] = alertRecordPath
	diceOrgId := structpb.NewStringValue(orgID)
	alert.Attributes["dice_org_id"] = diceOrgId
	return a.CreateAlert(alert)
}

func (a *Adapt) StringSliceToValue(input []string) (*structpb.Value, error) {
	arr := make([]interface{}, len(input))
	for i, v := range input {
		arr[i] = v
	}
	respList, err := structpb.NewList(arr)
	if err != nil {
		return nil, err
	}
	return structpb.NewListValue(respList), nil
}

func (a *Adapt) ValueMapToInterfaceMap(input map[string]*structpb.Value) map[string]interface{} {
	output := make(map[string]interface{})
	for k, v := range input {
		output[k] = v.AsInterface()
	}
	return output
}

func (a *Adapt) InterfaceMapToValueMap(input map[string]interface{}) (map[string]*structpb.Value, error) {
	output := make(map[string]*structpb.Value)
	for k, v := range input {
		data, err := structpb.NewValue(v)
		if err != nil {
			logrus.Errorf("InterfaceMapToValueMap NewValue is err:%v", err)
			vd, err := json.Marshal(data)
			if err != nil {
				return nil, err
			}
			data, err = structpb.NewValue(vd)
			if err != nil {
				return nil, err
			}
		}
		output[k] = data
	}
	return output, nil
}

// UpdateOrgAlert .
func (a *Adapt) UpdateOrgAlert(alertID uint64, alert *pb.Alert, orgID string) error {
	// data authorization
	origin, err := a.db.Alert.GetByID(alertID)
	if err != nil {
		return err
	}
	if alert == nil {
		return nil
	}
	if origin.AlertScope != "org" || origin.AlertScopeID != orgID {
		return fmt.Errorf("permission denied")
	}

	// supplement data
	alert.AlertScope = origin.AlertScope
	alert.AlertScopeId = origin.AlertScopeID
	alert.Enable = origin.Enable
	for k, v := range origin.Attributes {
		alert.Attributes[k], err = structpb.NewValue(v)
		if err != nil {
			return err
		}
	}
	if alert.Domain != "" {
		alert.Attributes["alert_domain"] = structpb.NewStringValue(alert.Domain)
	}
	alertDashboardPath := structpb.NewStringValue(dashboardPath)
	alert.Attributes["alert_dashboard_path"] = alertDashboardPath
	alertRecordPath := structpb.NewStringValue(recordPath)
	alert.Attributes["alert_record_path"] = alertRecordPath

	//TODO update trigger condition in attribute
	if len(alert.TriggerCondition) > 0 {
		data, err := json.Marshal(alert.TriggerCondition)
		if err != nil {
			return err
		}
		alert.Attributes[TriggerCondition] = structpb.NewStringValue(string(data))
	} else {
		delete(alert.Attributes, TriggerCondition)
	}

	return a.UpdateAlert(alertID, alert)
}

// UpdateAlert .
func (a *Adapt) UpdateAlert(alertID uint64, alert *pb.Alert) (err error) {
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
	orgName := alert.Attributes["org_name"].AsInterface().(string)
	if alert.Name != "" {
		dbAlert, err := tx.Alert.GetByScopeAndScopeIDAndName(alert.AlertScope, alert.AlertScopeId, alert.Name)
		if err != nil {
			return err
		}
		if dbAlert != nil && dbAlert.ID != alertID {
			return ErrorAlreadyExists
		}
	}

	dbAlert, err := tx.Alert.GetByID(alertID)
	if err != nil {
		return err
	}
	if dbAlert == nil {
		return nil
	}
	attributes := make(map[string]interface{})
	for k, v := range dbAlert.Attributes {
		attributes[k] = v
	}
	for k, v := range alert.Attributes {
		attributes[k] = v.AsInterface()
	}
	if len(alert.TriggerCondition) > 0 {
		data, err := json.Marshal(alert.TriggerCondition)
		if err != nil {
			return err
		}
		attributes[TriggerCondition] = string(data)
	} else {
		delete(attributes, TriggerCondition)
	}
	alert.Id = alertID
	for k, v := range attributes {
		value, err := structpb.NewValue(v)
		if err != nil {
			return err
		}
		alert.Attributes[k] = value
	}
	alert.Enable = dbAlert.Enable
	dbAlert = ToDBAlertModel(alert)
	if err := tx.Alert.Update(dbAlert); err != nil {
		return err
	}

	// modify alert expression
	var (
		indexes       []string
		expressionLen int
	)
	for _, expression := range alert.Rules {
		indexes = append(indexes, expression.AlertIndex)
	}
	ruleMap, err := a.getEnabledAlertRulesByScopeAndIndices(i18n.LanguageCodes{}, alert.AlertScope, alert.AlertScopeId, indexes)
	if err != nil {
		return err
	}
	expressionMap, err := a.getAlertExpressionsMapByAlertID(alertID)
	if err != nil {
		return err
	}
	saveExpressionIDs := make(map[uint64]bool)
	for _, item := range alert.Rules {
		rule, ok := ruleMap[item.AlertIndex]
		if !ok || rule.AlertScope != alert.AlertScope {
			return invalidParameter("rule %s is not scope: %s", rule.AlertIndex.Key, alert.AlertScope)
		}
		expression, err := ToDBAlertExpressionModel(item, orgName, alert, rule)
		if err != nil {
			return err
		}
		// if the expression is exist modify it else create
		if _, ok := expressionMap[expression.ID]; ok {
			if err := tx.AlertExpression.Update(expression); err != nil {
				return err
			}
			saveExpressionIDs[item.Id] = true
		} else {
			expression.ID = 0
			if err := tx.AlertExpression.Insert(expression); err != nil {
				return err
			}
		}
		expressionLen++
	}
	if expressionLen == 0 {
		return errors.New("expression is not valid")
	}
	// delete exist expression
	var deleteExpressionIDs []uint64
	for expressionID := range expressionMap {
		if _, ok := saveExpressionIDs[expressionID]; !ok {
			deleteExpressionIDs = append(deleteExpressionIDs, expressionID)
		}
	}
	if err := tx.AlertExpression.DeleteByIDs(deleteExpressionIDs); err != nil {
		return err
	}

	// modify alert notify
	var (
		silence   *pb.AlertNotifySilence
		notifyLen int
	)
	notifyMap, err := a.getAlertNotifysMapByAlertID(alertID)
	if err != nil {
		return err
	}
	saveNotifyIDs := make(map[uint64]bool)
	for _, item := range alert.Notifies {
		silence = item.Silence
		alertNotify := FromDBAlertToModel(item, alert, a.silencePolicies)
		if alertNotify == nil {
			continue
		}
		// if the expression is exist modify it else create
		if _, ok := notifyMap[alertNotify.ID]; ok {
			saveNotifyIDs[alertNotify.ID] = true
			if err := tx.AlertNotify.Update(alertNotify); err != nil {
				return err
			}
		} else {
			var notifyID uint64
			for _, notify := range notifyMap {
				if match := a.compareNotify(alertNotify, notify); match {
					notifyID = notify.ID
					break
				}
			}
			if notifyID != 0 {
				alertNotify.ID = notifyID
				saveNotifyIDs[notifyID] = true
				if err := tx.AlertNotify.Update(alertNotify); err != nil {
					return err
				}
			} else {
				alertNotify.ID = 0
				if err := tx.AlertNotify.Insert(alertNotify); err != nil {
					return err
				}
			}
		}
		notifyLen++
	}
	if notifyLen == 0 {
		return errors.New("notify is not valid")
	}
	// delete exist notify
	var (
		deleteNotifyIDs []uint64
		hasTicket       bool
	)
	for notifyID, notify := range notifyMap {
		if notifyType, ok := utils.GetMapValueString(notify.NotifyTarget, "type"); ok && notifyType == "ticket" {
			hasTicket = true
			// modify ticket notify silence
			t := convertMillisecondByUnit(silence.Value, silence.Unit)
			notify.Silence = t
			if err := tx.AlertNotify.Update(notify); err != nil {
				return err
			}
			continue
		}
		if _, ok := saveNotifyIDs[notifyID]; !ok {
			deleteNotifyIDs = append(deleteNotifyIDs, notifyID)
		}
	}
	if !hasTicket {
		// create ticket alert expression
		notify := a.newTicketAlertNotify(alertID, silence)
		if err := tx.AlertNotify.Insert(notify); err != nil {
			return err
		}
	}
	if err := tx.AlertNotify.DeleteByIDs(deleteNotifyIDs); err != nil {
		return err
	}
	return nil
}

func (a *Adapt) getAlertExpressionsMapByAlertID(alertID uint64) (map[uint64]*db.AlertExpression, error) {
	if alertID == 0 {
		return nil, nil
	}
	expressions, err := a.db.AlertExpression.QueryByAlertIDs([]uint64{alertID})
	if err != nil {
		return nil, err
	}
	expressionsMap := make(map[uint64]*db.AlertExpression)
	for _, expression := range expressions {
		expressionsMap[expression.ID] = expression
	}
	return expressionsMap, nil
}

func (a *Adapt) getAlertNotifysMapByAlertID(alertID uint64) (map[uint64]*db.AlertNotify, error) {
	if alertID == 0 {
		return nil, nil
	}
	notifies, err := a.db.AlertNotify.QueryByAlertIDs([]uint64{alertID})
	if err != nil {
		return nil, err
	}
	notifys := make(map[uint64]*db.AlertNotify)
	for _, notify := range notifies {
		notifys[notify.ID] = notify
	}
	return notifys, nil
}

func (*Adapt) compareNotify(a, b *db.AlertNotify) bool {
	if a == b {
		return true
	}
	if a == nil || b == nil {
		return false
	}
	if a.NotifyTarget == nil || b.NotifyTarget == nil {
		return false
	}
	notifyType, ok := utils.GetMapValueString(a.NotifyTarget, "type")
	if !ok {
		return false
	}
	if notifyType == "notify_group" {
		bNotifyType, _ := utils.GetMapValueString(b.NotifyTarget, "type")
		if notifyType != bNotifyType {
			return false
		}
		aGroupID, _ := utils.GetMapValueInt64(a.NotifyTarget, "group_id")
		bGroupID, _ := utils.GetMapValueInt64(b.NotifyTarget, "group_id")
		if aGroupID != bGroupID {
			return false
		}
		aGroupType, _ := utils.GetMapValueString(a.NotifyTarget, "group_type")
		bGroupType, _ := utils.GetMapValueString(b.NotifyTarget, "group_type")
		if aGroupType != bGroupType {
			return false
		}
	} else if notifyType == "dingding" {
		aAddr, _ := utils.GetMapValueString(a.NotifyTarget, "dingding_url")
		bAddr, _ := utils.GetMapValueString(b.NotifyTarget, "dingding_url")
		if aAddr != bAddr {
			return false
		}
	}
	return true
}

// DeleteAlert .
func (a *Adapt) DeleteAlert(id uint64) (err error) {
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
	err = a.db.Alert.DeleteByID(id)
	if err != nil {
		return err
	}
	err = a.db.AlertNotify.DeleteByAlertID(id)
	if err != nil {
		return err
	}
	err = a.db.AlertExpression.DeleteByAlertID(id)
	if err != nil {
		return err
	}
	return nil
}

// DeleteOrgAlert .
func (a *Adapt) DeleteOrgAlert(id uint64, orgID string) (err error) {
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
	alert, err := a.db.Alert.GetByID(id)
	if err != nil {
		return err
	}
	if alert == nil {
		return nil
	}
	if alert.AlertScope != "org" || alert.AlertScopeID != orgID {
		return fmt.Errorf("permission denied")
	}
	err = a.db.Alert.DeleteByID(id)
	if err != nil {
		return err
	}
	err = a.db.AlertNotify.DeleteByAlertID(id)
	if err != nil {
		return err
	}
	err = a.db.AlertExpression.DeleteByAlertID(id)
	if err != nil {
		return err
	}
	return nil
}

// UpdateAlertEnable .
func (a *Adapt) UpdateAlertEnable(id uint64, enable bool) (err error) {
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
	// close alert
	if err := a.db.Alert.UpdateEnable(id, enable); err != nil {
		return err
	}
	// close alert expression
	if err := a.db.AlertExpression.UpdateEnableByAlertID(id, enable); err != nil {
		return err
	}
	// close alert notify
	if err := a.db.AlertNotify.UpdateEnableByAlertID(id, enable); err != nil {
		return err
	}
	return nil
}

// UpdateOrgAlertEnable .
func (a *Adapt) UpdateOrgAlertEnable(id uint64, enable bool, orgID string) (err error) {
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
	alert, err := a.db.Alert.GetByID(id)
	if err != nil {
		return err
	}
	if alert == nil {
		return nil
	}
	if alert.AlertScope != "org" || alert.AlertScopeID != orgID {
		return fmt.Errorf("permission denied")
	}
	// close alert
	if err := a.db.Alert.UpdateEnable(id, enable); err != nil {
		return err
	}
	// close alert expression
	if err := a.db.AlertExpression.UpdateEnableByAlertID(id, enable); err != nil {
		return err
	}
	// close alert notify
	if err := a.db.AlertNotify.UpdateEnableByAlertID(id, enable); err != nil {
		return err
	}
	return nil
}
