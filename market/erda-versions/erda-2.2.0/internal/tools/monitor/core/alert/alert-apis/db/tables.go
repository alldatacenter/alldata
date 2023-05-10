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

package db

import (
	"time"

	"github.com/erda-project/erda/pkg/encoding/jsonmap"
)

// tables name
const (
	TableAlertRecord                  = "sp_alert_record"
	TableCustomizeAlert               = "sp_customize_alert"
	TableCustomizeAlertRule           = "sp_customize_alert_rule"
	TableCustomizeAlertNotifyTemplate = "sp_customize_alert_notify_template"
	TableAlertRules                   = "sp_alert_rules"
	TableAlertNotify                  = "sp_alert_notify"
	TableAlertNotifyTemplate          = "sp_alert_notify_template"
	TableAlertExpression              = "sp_alert_expression"
	TableMetricExpression             = "sp_metric_expression"
	TableAlert                        = "sp_alert"
	TableAlertEvent                   = "sp_alert_event"
	TableAlertEventSuppress           = "sp_alert_event_suppress"
)

type AlertEvent struct {
	Id               string    `gorm:"column:id;primary_key"`
	Name             string    `gorm:"column:name"`
	OrgID            int64     `gorm:"column:org_id"`
	AlertGroupID     string    `gorm:"column:alert_group_id"`
	AlertGroup       string    `gorm:"column:alert_group"`
	Scope            string    `gorm:"column:scope"`
	ScopeID          string    `gorm:"column:scope_id"`
	AlertID          uint64    `gorm:"column:alert_id"`
	AlertName        string    `gorm:"column:alert_name"`
	AlertType        string    `gorm:"column:alert_type"`
	AlertIndex       string    `gorm:"column:alert_index"`
	AlertLevel       string    `gorm:"column:alert_level"`
	AlertSource      string    `gorm:"column:alert_source"`
	AlertSubject     string    `gorm:"column:alert_subject"`
	AlertState       string    `gorm:"column:alert_state"`
	RuleID           uint64    `gorm:"column:rule_id"`
	RuleName         string    `gorm:"column:rule_name"`
	ExpressionID     uint64    `gorm:"column:expression_id"`
	LastTriggerTime  time.Time `gorm:"column:last_trigger_time"`
	FirstTriggerTime time.Time `gorm:"column:first_trigger_time"`
}

// TableName .
func (AlertEvent) TableName() string { return TableAlertEvent }

type AlertEventSuppress struct {
	Id           string    `gorm:"column:id;primary_key"`
	OrgID        int64     `gorm:"column:org_id"`
	Scope        string    `gorm:"column:scope"`
	ScopeID      string    `gorm:"column:scope_id"`
	AlertEventID string    `gorm:"column:alert_event_id"`
	SuppressType string    `gorm:"column:suppress_type"`
	ExpireTime   time.Time `gorm:"column:expire_time"`
	Enabled      bool      `gorm:"column:enabled"`
}

func (AlertEventSuppress) TableName() string { return TableAlertEventSuppress }

type AlertRecord struct {
	GroupID       string    `gorm:"column:group_id;primary_key"`
	Scope         string    `gorm:"column:scope"`
	ScopeKey      string    `gorm:"column:scope_key"`
	AlertGroup    string    `gorm:"column:alert_group"`
	Title         string    `gorm:"column:title"`
	AlertState    string    `gorm:"column:alert_state"`
	AlertType     string    `gorm:"column:alert_type"`
	AlertIndex    string    `gorm:"column:alert_index"`
	ExpressionKey string    `gorm:"column:expression_key"`
	AlertID       uint64    `gorm:"column:alert_id"`
	AlertName     string    `gorm:"column:alert_name"`
	RuleID        uint64    `gorm:"column:rule_id"`
	IssueID       uint64    `gorm:"column:issue_id"`
	HandleState   string    `gorm:"column:handle_state"`
	HandlerID     string    `gorm:"column:handler_id"`
	AlertTime     time.Time `gorm:"column:alert_time"`
	HandleTime    time.Time `gorm:"column:handle_time;default:null"`
	CreateTime    time.Time `gorm:"column:create_time"`
	UpdateTime    time.Time `gorm:"column:update_time"`
}

// TableName .
func (AlertRecord) TableName() string { return TableAlertRecord }

// CustomizeAlert .
type CustomizeAlert struct {
	ID           uint64          `gorm:"column:id"`
	Name         string          `gorm:"column:name"`
	AlertType    string          `gorm:"column:alert_type"`
	AlertScope   string          `gorm:"column:alert_scope"`
	AlertScopeID string          `gorm:"column:alert_scope_id"`
	Attributes   jsonmap.JSONMap `gorm:"column:attributes"`
	Enable       bool            `gorm:"column:enable"`
	CreatorID    string          `gorm:"column:creator_id"`
	CreateTime   time.Time       `gorm:"column:create_time"`
	UpdateTime   time.Time       `gorm:"column:update_time"`
}

// TableName .
func (CustomizeAlert) TableName() string { return TableCustomizeAlert }

// CustomizeAlertRule .
type CustomizeAlertRule struct {
	ID               uint64          `gorm:"column:id"`
	Name             string          `gorm:"column:name"`
	CustomizeAlertID uint64          `gorm:"column:customize_alert_id"`
	AlertType        string          `gorm:"column:alert_type"`
	AlertIndex       string          `gorm:"column:alert_index"`
	AlertScope       string          `gorm:"column:alert_scope"`
	AlertScopeID     string          `gorm:"column:alert_scope_id"`
	Template         jsonmap.JSONMap `gorm:"column:template"`
	Attributes       jsonmap.JSONMap `gorm:"column:attributes"`
	Enable           bool            `gorm:"column:enable"`
	CreateTime       time.Time       `gorm:"column:create_time"`
	UpdateTime       time.Time       `gorm:"column:update_time"`
}

// TableName .
func (CustomizeAlertRule) TableName() string { return TableCustomizeAlertRule }

// CustomizeAlertNotifyTemplate .
type CustomizeAlertNotifyTemplate struct {
	ID               uint64          `gorm:"column:id" json:"id"`
	Name             string          `gorm:"column:name" json:"name"`
	CustomizeAlertID uint64          `gorm:"column:customize_alert_id" json:"customize_alert_id"`
	AlertType        string          `gorm:"column:alert_type" json:"alert_type"`
	AlertIndex       string          `gorm:"column:alert_index" json:"alert_index"`
	Target           string          `gorm:"column:target" json:"target"`
	Trigger          string          `gorm:"column:trigger" json:"trigger"`
	Title            string          `gorm:"column:title" json:"title"`
	Template         string          `gorm:"column:template" json:"template"`
	Formats          jsonmap.JSONMap `gorm:"column:formats" json:"formats"`
	Version          string          `gorm:"column:version" json:"version"`
	Enable           bool            `gorm:"column:Enable" json:"enable"`
	CreateTime       time.Time       `gorm:"column:create_time" json:"create_time"`
	UpdateTime       time.Time       `gorm:"column:update_time" json:"update_time"`
}

// TableName .
func (CustomizeAlertNotifyTemplate) TableName() string { return TableCustomizeAlertNotifyTemplate }

// AlertRule .
type AlertRule struct {
	ID         uint64          `gorm:"column:id"`
	Name       string          `gorm:"column:name"`
	AlertScope string          `gorm:"column:alert_scope"`
	AlertType  string          `gorm:"column:alert_type"`
	AlertIndex string          `gorm:"column:alert_index"`
	Template   jsonmap.JSONMap `gorm:"column:template"`
	Attributes jsonmap.JSONMap `gorm:"column:attributes"`
	Version    string          `gorm:"column:version"`
	Enable     bool            `gorm:"column:enable"`
	CreateTime time.Time       `gorm:"column:create_time"`
	UpdateTime time.Time       `gorm:"column:update_time"`
}

// TableName .
func (AlertRule) TableName() string { return TableAlertRules }

// AlertNotify .
type AlertNotify struct {
	ID             uint64          `gorm:"column:id" json:"id"`
	AlertID        uint64          `gorm:"column:alert_id" json:"alert_id"`
	NotifyKey      string          `gorm:"column:notify_key" json:"notify_key"`
	NotifyTarget   jsonmap.JSONMap `gorm:"column:notify_target" json:"notify_target"`
	NotifyTargetID string          `gorm:"column:notify_target_id" json:"notify_target_id"`
	Silence        int64           `gorm:"column:silence" json:"silence"`
	SilencePolicy  string          `gorm:"column:silence_policy" json:"silence_policy"`
	Enable         bool            `gorm:"column:enable" json:"enable"`
	Created        time.Time       `gorm:"column:created" json:"created"`
	Updated        time.Time       `gorm:"column:updated" json:"updated"`
}

// TableName .
func (AlertNotify) TableName() string { return TableAlertNotify }

// AlertNotifyTemplate .
type AlertNotifyTemplate struct {
	ID         uint64          `gorm:"column:id"`
	Name       string          `gorm:"column:name"`
	AlertType  string          `gorm:"column:alert_type"`
	AlertIndex string          `gorm:"column:alert_index"`
	Target     string          `gorm:"column:target"`
	Trigger    string          `gorm:"column:trigger"`
	Title      string          `gorm:"column:title"`
	Template   string          `gorm:"column:template"`
	Formats    jsonmap.JSONMap `gorm:"column:formats"`
	Version    string          `gorm:"column:version"`
	Enable     bool            `gorm:"column:enable"`
	CreateTime time.Time       `gorm:"column:create_time"`
	UpdateTime time.Time       `gorm:"column:update_time"`
}

// TableName 。
func (AlertNotifyTemplate) TableName() string { return TableAlertNotifyTemplate }

// AlertExpression .
type AlertExpression struct {
	ID         uint64          `gorm:"column:id" json:"id"`
	AlertID    uint64          `gorm:"column:alert_id" json:"alert_id"`
	Attributes jsonmap.JSONMap `gorm:"column:attributes" json:"attributes"`
	Expression jsonmap.JSONMap `gorm:"column:expression" json:"expression"`
	Version    string          `gorm:"column:version" json:"version"`
	Enable     bool            `gorm:"column:enable" json:"enable"`
	Created    time.Time       `gorm:"column:created" json:"created"`
	Updated    time.Time       `gorm:"column:updated" json:"updated"`
}

// TableName 。
func (AlertExpression) TableName() string { return TableAlertExpression }

type MetricExpression struct {
	ID         uint64          `gorm:"column:id" json:"id"`
	Attributes jsonmap.JSONMap `gorm:"column:attributes" json:"attributes"`
	Expression jsonmap.JSONMap `gorm:"column:expression" json:"expression"`
	Version    string          `gorm:"column:version" json:"version"`
	Enable     bool            `gorm:"column:enable" json:"enable"`
	Created    time.Time       `gorm:"column:created" json:"created"`
	Updated    time.Time       `gorm:"column:updated" json:"updated"`
}

func (MetricExpression) TableName() string { return TableMetricExpression }

// Alert .
type Alert struct {
	ID           uint64          `gorm:"column:id"`
	Name         string          `gorm:"column:name"`
	AlertScope   string          `gorm:"column:alert_scope"`
	AlertScopeID string          `gorm:"column:alert_scope_id"`
	Attributes   jsonmap.JSONMap `gorm:"column:attributes"`
	Enable       bool            `gorm:"column:enable"`
	CreatorID    string          `gorm:"column:creator_id"`
	Created      time.Time       `gorm:"column:created"`
	Updated      time.Time       `gorm:"column:updated"`
}

// TableName 。
func (Alert) TableName() string { return TableAlert }
