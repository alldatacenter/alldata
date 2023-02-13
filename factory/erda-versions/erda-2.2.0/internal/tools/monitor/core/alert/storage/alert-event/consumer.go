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

package alert_event

import (
	"encoding/json"
	"reflect"
	"strconv"
	"strings"
	"time"

	"github.com/erda-project/erda/internal/tools/monitor/core/alert/alert-apis/db"
	"github.com/erda-project/erda/pkg/database/gormutil"
)

type kafkaAlertRecord struct {
	AlertEventFamilyId string `json:"alertEventFamilyId"`
	OrgID              int64  `json:"orgId"`
	GroupID            string `json:"groupId,omitempty"`
	Scope              string `json:"scope,omitempty"`
	ScopeKey           string `json:"scopeKey,omitempty"`
	AlertGroup         string `json:"alertGroup,omitempty"`
	Title              string `json:"title,omitempty"`
	AlertState         string `json:"alertState,omitempty"`
	AlertType          string `json:"alertType,omitempty"`
	AlertIndex         string `json:"alertIndex,omitempty"`
	ExpressionKey      string `json:"expressionKey,omitempty"`
	AlertID            uint64 `json:"alertId,omitempty"`
	AlertName          string `json:"alertName,omitempty"`
	RuleID             uint64 `json:"ruleId,omitempty"`
	RuleName           string `json:"ruleName,omitempty"`
	AlertSource        string `json:"alertSource"`
	AlertSubject       string `json:"alertSubject"`
	AlertLevel         string `json:"alertLevel"`
	AlertTimeMs        int64  `json:"alertTime,omitempty"`
}

func (k *kafkaAlertRecord) toAlertEventModel() *db.AlertEvent {
	exprId, _ := strconv.ParseUint(strings.TrimLeft(k.ExpressionKey, "alert_"), 10, 64)
	return &db.AlertEvent{
		Id:               k.AlertEventFamilyId,
		Name:             k.Title,
		OrgID:            k.OrgID,
		AlertGroupID:     k.GroupID,
		AlertGroup:       k.AlertGroup,
		Scope:            k.Scope,
		ScopeID:          k.ScopeKey,
		AlertID:          k.AlertID,
		AlertName:        k.AlertName,
		AlertType:        k.AlertType,
		AlertIndex:       k.AlertIndex,
		AlertLevel:       k.AlertLevel,
		AlertSource:      k.AlertSource,
		AlertSubject:     k.AlertSubject,
		AlertState:       k.AlertState,
		RuleID:           k.RuleID,
		RuleName:         k.RuleName,
		ExpressionID:     exprId,
		FirstTriggerTime: time.Unix(k.AlertTimeMs/1e3, k.AlertTimeMs%1e3*1e6),
		LastTriggerTime:  time.Unix(k.AlertTimeMs/1e3, k.AlertTimeMs%1e3*1e6),
	}
}

func (p *provider) invoke(key []byte, value []byte, topic *string, timestamp time.Time) error {
	alertRecord := &kafkaAlertRecord{}
	if err := json.Unmarshal(value, alertRecord); err != nil {
		return err
	}

	alertEvent := alertRecord.toAlertEventModel()
	existEvent, err := p.alertEventDB.GetById(alertEvent.Id)
	if err != nil {
		return err
	}
	if existEvent == nil {
		//create
		err = p.alertEventDB.CreateAlertEvent(alertEvent)
		return err
	} else {
		//update
		err = p.alertEventDB.UpdateAlertEvent(existEvent.Id, p.calcNeedUpdateFields(existEvent, alertEvent))
		return err
	}
}

var alertEventFieldColumnsMap = gormutil.GetFieldToColumnMap(reflect.TypeOf(db.AlertEvent{}))
var alertEventFieldOrderMap = func() map[int]string {
	m := map[int]string{}
	typ := reflect.TypeOf(db.AlertEvent{})
	for i := 0; i < typ.NumField(); i++ {
		m[i] = typ.Field(i).Name
	}
	return m
}()

func (p *provider) calcNeedUpdateFields(old *db.AlertEvent, new *db.AlertEvent) map[string]interface{} {
	oldValue := reflect.ValueOf(old).Elem()
	newValue := reflect.ValueOf(new).Elem()
	updateFields := map[string]interface{}{}

	for i := 0; i < oldValue.NumField(); i++ {
		if val := newValue.Field(i).Interface(); val != oldValue.Field(i).Interface() && val != "" {
			fieldName := alertEventFieldOrderMap[i]
			if fieldName == "FirstTriggerTime" {
				continue
			}
			updateFields[alertEventFieldColumnsMap[fieldName]] = val
		}
	}

	return updateFields
}
