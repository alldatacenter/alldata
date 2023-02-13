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

package apis

import (
	"fmt"
	"net/http"

	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/monitor/core/alert/alert-apis/adapt"
	block "github.com/erda-project/erda/internal/tools/monitor/core/dataview/v1-chart-block"
	api "github.com/erda-project/erda/pkg/common/httpapi"
)

type (
	MicroAlertAPI interface {
		GetMicroServiceFilterTags() map[string]bool

		// micro alert apis
		QueryAlertRule(r *http.Request, scope, scopeId string) (*pb.AlertTypeRuleResp, error)
		QueryAlert(r *http.Request, scope, scopeId string, pageNum, pageSize uint64, name string) ([]*pb.Alert, []string, error)
		GetAlert(lang i18n.LanguageCodes, id uint64) (*pb.Alert, error)
		CountAlert(scope, scopeID, name string) (int, error)
		GetAlertDetail(r *http.Request, id uint64) (*pb.Alert, error)
		CheckAlert(alert *pb.Alert) interface{}
		CreateAlert(alert *pb.Alert) (alertID uint64, err error)
		UpdateAlert(alertID uint64, alert *pb.Alert) (err error)
		UpdateAlertEnable(id uint64, enable bool) (err error)
		DeleteAlert(id uint64) (err error)

		// micro custom alert apis
		CustomizeMetrics(lang i18n.LanguageCodes, scope, scopeID string, names []string) (*pb.CustomizeMetrics, error)
		NotifyTargetsKeys(lang i18n.LanguageCodes, orgId string) []*pb.DisplayKey
		CustomizeAlerts(lang i18n.LanguageCodes, scope, scopeID string, pageNo, pageSize int, name string) ([]*pb.CustomizeAlertOverview, int, error)
		CustomizeAlert(id uint64) (*pb.CustomizeAlertDetail, error)
		CustomizeAlertDetail(id uint64) (*pb.CustomizeAlertDetail, error)
		CheckCustomizeAlert(alert *pb.CustomizeAlertDetail) error
		CreateCustomizeAlert(alertDetail *pb.CustomizeAlertDetail, userID string) (alertID uint64, err error)
		UpdateCustomizeAlert(alertDetail *pb.CustomizeAlertDetail) (err error)
		UpdateCustomizeAlertEnable(id uint64, enable bool) (err error)
		DeleteCustomizeAlert(id uint64) (err error)

		//micro custom alert records
		GetAlertRecordAttr(lang i18n.LanguageCodes, scope string) (*pb.AlertRecordAttr, error)
		QueryAlertRecord(lang i18n.LanguageCodes, scope, scopeId string, alertGroup, alertState, alertType,
			handleState, handlerId []string, pageNo, pageSize int64) ([]*pb.AlertRecord, error)
		CountAlertRecord(scope, scopeId string, alertGroups, alertStates, alertTypes, handleStates, handlerIDs []string) (int, error)
		GetAlertRecord(lang i18n.LanguageCodes, groupId string) (*pb.AlertRecord, error)
		QueryAlertHistory(lang i18n.LanguageCodes, groupId string, start, end int64, limit uint) ([]*pb.AlertHistory, error)
		CreateAlertRecordIssue(groupId string, issueCreate *apistructs.IssueCreateRequest) (uint64, error)
		UpdateAlertRecordIssue(groupId string, issueId uint64, request *apistructs.IssueUpdateRequest) error
		DashboardPreview(alert *pb.CustomizeAlertDetail) (res *block.View, err error)
	}
)

func (p *provider) GetMicroServiceFilterTags() map[string]bool {
	return p.microServiceFilterTags
}

func (p *provider) QueryAlertRule(r *http.Request, scope, scopeId string) (*pb.AlertTypeRuleResp, error) {
	return p.a.QueryAlertRule(api.Language(r), scope, scopeId)
}

func (p *provider) QueryAlert(r *http.Request, scope, scopeId string, pageNum, pageSize uint64, name string) ([]*pb.Alert, []string, error) {
	return p.a.QueryAlert(api.Language(r), scope, scopeId, pageNum, pageSize, name)
}

func (p *provider) GetAlert(lang i18n.LanguageCodes, id uint64) (*pb.Alert, error) {
	return p.a.GetAlert(lang, id)
}

func (p *provider) GetAlertDetail(r *http.Request, id uint64) (*pb.Alert, error) {
	return p.a.GetAlertDetail(api.Language(r), id)
}

func (p *provider) CountAlert(scope, scopeID, name string) (int, error) {
	return p.a.CountAlert(scope, scopeID, name)
}

func (p *provider) CheckAlert(alert *pb.Alert) interface{} {
	return p.checkAlert(alert)
}

func (p *provider) checkAlert(alert *pb.Alert) interface{} {
	if alert.Name == "" {
		return api.Errors.MissingParameter("alert name")
	}
	if alert.AlertScope == "" {
		return api.Errors.MissingParameter("alert scope")
	}
	if alert.AlertScopeId == "" {
		return api.Errors.MissingParameter("alert scopeId")
	}
	if len(alert.Rules) == 0 {
		return api.Errors.MissingParameter("alert rules")
	}
	if len(alert.Notifies) == 0 {
		return api.Errors.MissingParameter("alert notifies")
	}
	return nil
}

func (p *provider) CreateAlert(alert *pb.Alert) (alertID uint64, err error) {
	return p.a.CreateAlert(alert)
}

func (p *provider) UpdateAlert(alertID uint64, alert *pb.Alert) (err error) {
	return p.a.UpdateAlert(alertID, alert)
}
func (p *provider) UpdateAlertEnable(id uint64, enable bool) (err error) {
	return p.a.UpdateAlertEnable(id, enable)
}

func (p *provider) DeleteAlert(id uint64) (err error) {
	return p.a.DeleteAlert(id)
}

func (p *provider) CustomizeMetrics(lang i18n.LanguageCodes, scope, scopeID string, names []string) (*pb.CustomizeMetrics, error) {
	return p.a.CustomizeMetrics(lang, scope, scopeID, names)
}

func (p *provider) NotifyTargetsKeys(lang i18n.LanguageCodes, config map[string]bool) []*pb.DisplayKey {
	return p.a.NotifyTargetsKeys(lang, config)
}

func (p *provider) CustomizeAlerts(lang i18n.LanguageCodes, scope, scopeID string, pageNo, pageSize int, name string) ([]*pb.CustomizeAlertOverview, []string, int, error) {
	return p.a.CustomizeAlerts(lang, scope, scopeID, pageNo, pageSize, name)
}

func (p *provider) CustomizeAlert(id uint64) (*pb.CustomizeAlertDetail, error) {
	return p.a.CustomizeAlert(id)
}

func (p *provider) CustomizeAlertDetail(id uint64) (*pb.CustomizeAlertDetail, error) {
	return p.a.CustomizeAlertDetail(id)
}

func (p *provider) CheckCustomizeAlert(alert *pb.CustomizeAlertDetail) error {
	return p.checkCustomizeAlert(alert)
}

func (p *provider) checkCustomizeAlert(alert *pb.CustomizeAlertDetail) error {
	if alert.Name == "" {
		return fmt.Errorf("alert name must not be empty")
	}
	if alert.AlertScope == "" {
		return fmt.Errorf("alert scope must not be empty")
	}
	if alert.AlertScopeId == "" {
		return fmt.Errorf("alert scope id must not be empty")
	}
	if len(alert.Rules) == 0 {
		return fmt.Errorf("alert rules id must not be empty")
	}
	if len(alert.Notifies) == 0 {
		return fmt.Errorf("alert notifies must not be empty")
	}
	// 必须包含ticket类型的通知方式，用于告警历史展示
	hasTicket := false
	for _, notify := range alert.Notifies {
		for _, target := range notify.Targets {
			if target == "ticket" {
				hasTicket = true
				break
			}
		}
	}
	if !hasTicket {
		return fmt.Errorf("alert notifies must has ticket")
	}
	return nil
}

func (p *provider) CreateCustomizeAlert(alertDetail *pb.CustomizeAlertDetail, userID string) (alertID uint64, err error) {
	return p.a.CreateCustomizeAlert(alertDetail, userID)
}

func (p *provider) UpdateCustomizeAlert(alertDetail *pb.CustomizeAlertDetail) (err error) {
	return p.a.UpdateCustomizeAlert(alertDetail)
}

func (p *provider) UpdateCustomizeAlertEnable(id uint64, enable bool) (err error) {
	return p.a.UpdateCustomizeAlertEnable(id, enable)
}

func (p *provider) DeleteCustomizeAlert(id uint64) (err error) {
	return p.a.DeleteCustomizeAlert(id)
}

func (p *provider) GetAlertRecordAttr(lang i18n.LanguageCodes, scope string) (*pb.AlertRecordAttr, error) {
	return p.a.GetAlertRecordAttr(lang, scope)
}

func (p *provider) QueryAlertRecord(lang i18n.LanguageCodes, scope, scopeId string, alertGroup, alertState, alertType,
	handleState, handlerId []string, pageNo, pageSize int64) ([]*pb.AlertRecord, error) {
	return p.a.QueryAlertRecord(lang, scope, scopeId, alertGroup, alertState, alertType, handleState,
		handlerId, uint(pageNo), uint(pageSize))
}

func (p *provider) CountAlertRecord(scope, scopeId string, alertGroups, alertStates, alertTypes, handleStates, handlerIDs []string) (int, error) {
	return p.a.CountAlertRecord(scope, scopeId, alertGroups, alertStates, alertTypes, handleStates, handlerIDs)
}

func (p *provider) GetAlertRecord(lang i18n.LanguageCodes, groupId string) (*pb.AlertRecord, error) {
	return p.a.GetAlertRecord(lang, groupId)
}

func (p *provider) QueryAlertHistory(lang i18n.LanguageCodes, groupId string, start, end int64, limit uint) ([]*pb.AlertHistory, error) {
	return p.a.QueryAlertHistory(lang, groupId, start, end, limit)
}

func (p *provider) CreateAlertRecordIssue(groupId string, issueCreate *apistructs.IssueCreateRequest) (uint64, error) {
	return p.a.CreateAlertIssue(groupId, *issueCreate)
}

func (p *provider) UpdateAlertRecordIssue(groupId string, issueId uint64, request *apistructs.IssueUpdateRequest) error {
	return p.a.UpdateAlertIssue(groupId, issueId, *request)
}

func (p *provider) DashboardPreview(alert *pb.CustomizeAlertDetail) (res *pb.View, err error) {
	return adapt.NewDashboard(p.a).GenerateDashboardPreView(alert)
}
