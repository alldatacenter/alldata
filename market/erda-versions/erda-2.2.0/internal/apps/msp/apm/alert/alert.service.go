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

package alert

import (
	"context"
	"encoding/json"
	"fmt"
	"strconv"
	"strings"

	"github.com/jinzhu/gorm"
	"github.com/mitchellh/mapstructure"
	"google.golang.org/protobuf/types/known/structpb"

	monitor "github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	alert "github.com/erda-project/erda-proto-go/msp/apm/alert/pb"
	tenantpb "github.com/erda-project/erda-proto-go/msp/tenant/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/audit"
	"github.com/erda-project/erda/internal/tools/monitor/core/alert/alert-apis/adapt"
	"github.com/erda-project/erda/internal/tools/monitor/utils"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/common/errors"
	api "github.com/erda-project/erda/pkg/common/httpapi"
)

type alertService struct {
	p *provider
}

func (a *alertService) QueryAlertRule(ctx context.Context, request *alert.QueryAlertRuleRequest) (*alert.QueryAlertRuleResponse, error) {
	req := &monitor.QueryAlertRuleRequest{}
	req.ScopeId = request.TenantGroup
	req.Scope = MicroServiceScope
	context := utils.NewContextWithHeader(ctx)
	resp, err := a.p.Monitor.QueryAlertRule(context, req)
	if err != nil {
		return &alert.QueryAlertRuleResponse{}, errors.NewInternalServerError(err)
	}
	result := &alert.QueryAlertRuleResponse{
		Data: resp.Data,
	}
	return result, nil
}

func (a *alertService) QueryAlert(ctx context.Context, request *alert.QueryAlertRequest) (*alert.QueryAlertResponse, error) {
	data, err := json.Marshal(request)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	req := &monitor.QueryAlertRequest{}
	err = json.Unmarshal(data, req)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	req.Scope = MicroServiceScope
	req.ScopeId = request.TenantGroup
	req.Name = request.Name
	context := utils.NewContextWithHeader(ctx)
	resp, err := a.p.Monitor.QueryAlert(context, req)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	if resp == nil || resp.Data == nil || resp.Data.List == nil {
		return nil, nil
	}

	result := &alert.QueryAlertResponse{
		Data: &alert.QueryAlertData{
			List:  make([]*alert.ApmAlertData, 0),
			Total: resp.Data.Total,
		},
		UserIDs: resp.UserIDs,
	}

	for _, v := range resp.Data.List {
		appIdStr := v.Attributes["application_id"]
		idData := appIdStr.GetListValue().AsSlice()
		appIds := make([]string, 0)
		if idData != nil {
			for _, v := range idData {
				appIds = append(appIds, v.(string))
			}
		}
		apmAlert := &alert.ApmAlertData{}
		data, err := json.Marshal(v)
		if err != nil {
			return nil, errors.NewInternalServerError(err)
		}
		err = json.Unmarshal(data, apmAlert)
		if err != nil {
			return nil, errors.NewInternalServerError(err)
		}
		apmAlert.AppIds = appIds
		result.Data.List = append(result.Data.List, apmAlert)
	}
	return result, nil
}

func (a *alertService) GetAlert(ctx context.Context, request *alert.GetAlertRequest) (*alert.GetAlertResponse, error) {
	alertDetailRequest := &monitor.GetAlertDetailRequest{
		Id: request.Id,
	}
	resp, err := a.p.Monitor.GetAlertDetail(ctx, alertDetailRequest)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	if resp.Data.AlertScope != MicroServiceScope || resp.Data.AlertScopeId != request.TenantGroup {
		return nil, errors.NewPermissionError("monitor_project_alert", "GET", "alertScope or alertScopeId is invalidate")
	}

	getAlertData := &alert.ApmAlertData{
		Id:               int64(resp.Data.Id),
		Name:             resp.Data.Name,
		AlertScope:       resp.Data.AlertScope,
		AlertScopeId:     resp.Data.AlertScopeId,
		Enable:           resp.Data.Enable,
		Rules:            resp.Data.Rules,
		Notifies:         resp.Data.Notifies,
		Domain:           resp.Data.Domain,
		Attributes:       resp.Data.Attributes,
		CreateTime:       resp.Data.CreateTime,
		UpdateTime:       resp.Data.UpdateTime,
		TriggerCondition: resp.Data.TriggerCondition,
	}

	apps := make([]string, 0)
	appIdStr, ok := resp.Data.Attributes["application_id"]
	if !ok {
		appIdStr, ok = resp.Data.Attributes["target_application_id"]
	}
	if ok {
		idData := appIdStr.GetListValue().AsSlice()
		for _, v := range idData {
			appid, err := strconv.Atoi(v.(string))
			if err != nil {
				return nil, errors.NewInternalServerError(err)
			}
			app, err := a.p.bdl.GetApp(uint64(appid))
			if err != nil {
				return nil, errors.NewInternalServerError(err)
			}
			apps = append(apps, app.Name)
		}
	}

	if len(resp.Data.TriggerCondition) == 0 && len(apps) > 0 {
		triggerCondition := []*monitor.TriggerCondition{
			{
				Condition: "application_name",
				Operator:  "in",
				Values:    strings.Join(apps, ","),
			},
		}
		getAlertData.TriggerCondition = triggerCondition
	}
	result := &alert.GetAlertResponse{
		Data: getAlertData,
	}
	return result, nil
}

func (a *alertService) CreateAlert(ctx context.Context, request *alert.CreateAlertRequest) (*alert.CreateAlertResponse, error) {
	tk, err := a.getTKByTenant(request.TenantGroup)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	projectId, workspace, err := a.getOtherValue(tk)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	ProjectID, err := strconv.Atoi(projectId)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	org, err := a.p.bdl.GetProject(uint64(ProjectID))
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	alertData := &monitor.Alert{}
	alertData.Name = request.Name
	alertData.Rules = request.Rules
	alertData.Notifies = request.Notifies
	alertData.Domain = request.Domain
	alertData.CreateTime = request.CreateTime
	alertData.UpdateTime = request.UpdateTime
	alertData.AlertScope = MicroServiceScope
	alertData.AlertScopeId = request.TenantGroup
	alertData.Attributes = request.Attributes
	alertData.TriggerCondition = request.TriggerCondition
	if alertData.Attributes == nil {
		alertData.Attributes = make(map[string]*structpb.Value)
	}
	alertData.Attributes[DiceOrgId] = structpb.NewStringValue(strconv.Itoa(int(org.OrgID)))
	alertData.Attributes[Domain] = structpb.NewStringValue(request.Domain)
	alertData.Attributes[TenantGroup] = structpb.NewStringValue(request.TenantGroup)
	alertData.Attributes[ProjectId] = structpb.NewStringValue(projectId)
	alertData.Attributes[TargetProjectId] = structpb.NewStringValue(projectId)
	alertData.Attributes[WORKSPACE] = structpb.NewStringValue(workspace)
	alertData.Attributes[TargetWorkspace] = structpb.NewStringValue(workspace)
	alertData.Attributes[TK] = structpb.NewStringValue(tk)
	alertData.Attributes[TkAlias] = structpb.NewStringValue(tk)
	alertData.Attributes[DashboardPath] =
		structpb.NewStringValue(fmt.Sprintf(DashboardPathFormat, projectId, workspace, request.TenantGroup, tk))
	alertData.Attributes[RecordPath] =
		structpb.NewStringValue(fmt.Sprintf(RecordPathFormat, projectId, workspace, request.TenantGroup, tk))
	if len(request.TriggerCondition) > 0 {
		data, err := json.Marshal(request.TriggerCondition)
		if err != nil {
			return nil, errors.NewInternalServerError(err)
		}
		alertData.Attributes[TriggerCondition] = structpb.NewStringValue(string(data))
	}
	ma, err := a.AlertToMonitor(alertData)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	if response := a.p.CheckAlert(ma); response != nil {
		return nil, errors.NewInternalServerError(response.(api.Response).Err)
	}
	data, err := json.Marshal(ma)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	createAlertRequest := &monitor.CreateAlertRequest{}
	err = json.Unmarshal(data, createAlertRequest)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	context := utils.NewContextWithHeader(ctx)
	resp, err := a.p.Monitor.CreateAlert(context, createAlertRequest)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	projectName, workspace, auditProjectId, err := a.getAuditInfo(tk)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	auditContext := map[string]interface{}{
		"projectName": projectName,
		"alertName":   request.Name,
		"workspace":   workspace,
	}
	audit.ContextEntryMap(ctx, auditContext)
	return &alert.CreateAlertResponse{
		Data: &alert.CreateAlertData{
			Id:        resp.Data,
			ProjectId: auditProjectId,
		},
	}, nil
}

func (a *alertService) AlertToMonitor(alertData *monitor.Alert) (*monitor.Alert, error) {
	ma := &monitor.Alert{}
	data, err := json.Marshal(alertData)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	err = json.Unmarshal(data, ma)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	return ma, nil
}

func (a *alertService) UpdateAlert(ctx context.Context, request *alert.UpdateAlertRequest) (*alert.UpdateAlertResponse, error) {
	tk, err := a.getTKByTenant(request.TenantGroup)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	projectId, workspace, err := a.getOtherValue(tk)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}

	getAlertRequest := &monitor.GetAlertRequest{
		Id: int64(request.Id),
	}
	resp, err := a.p.Monitor.GetAlert(ctx, getAlertRequest)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	if MicroServiceScope != resp.Data.AlertScope || request.TenantGroup != resp.Data.AlertScopeId {
		return nil, errors.NewPermissionError("monitor_project_alert", "update", "scope or scopeId is invalidate")
	}
	alertData := &monitor.Alert{}
	alertData.Id = resp.Data.Id
	alertData.Name = request.Name
	alertData.AlertScope = MicroServiceScope
	alertData.AlertScopeId = request.TenantGroup
	alertData.Rules = request.Rules
	alertData.Notifies = request.Notifies
	alertData.CreateTime = request.CreateTime
	alertData.UpdateTime = request.UpdateTime
	alertData.Attributes = request.Attributes
	alertData.Domain = request.Domain
	alertData.TriggerCondition = request.TriggerCondition
	if alertData.Attributes == nil {
		alertData.Attributes = make(map[string]*structpb.Value)
	}
	if request.Attributes != nil {
		for k, v := range request.Attributes {
			alertData.Attributes[k] = v
		}
	}
	//if request.AppIds != nil && len(request.AppIds) > 0 {
	//	alertData.Attributes["application_id"], err = a.p.StringSliceToValue(request.AppIds)
	//	if err != nil {
	//		return nil, errors.NewInternalServerError(err)
	//	}
	//	alertData.Attributes["target_application_id"], err = a.p.StringSliceToValue(request.AppIds)
	//	if err != nil {
	//		return nil, errors.NewInternalServerError(err)
	//	}
	//}
	if request.Domain != "" && len(request.Domain) > 0 {
		alertData.Attributes["alert_domain"] = structpb.NewStringValue(request.Domain)
	}
	dashboardPath := fmt.Sprintf(DashboardPathFormat, projectId, workspace, request.TenantGroup, tk)
	recordPath := fmt.Sprintf(RecordPathFormat, projectId, workspace, request.TenantGroup, tk)
	alertData.Attributes[DashboardPath] = structpb.NewStringValue(dashboardPath)
	alertData.Attributes[RecordPath] = structpb.NewStringValue(recordPath)
	data, err := json.Marshal(alertData)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	updateAlertRequest := &monitor.UpdateAlertRequest{}
	err = json.Unmarshal(data, updateAlertRequest)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	context := utils.NewContextWithHeader(ctx)
	_, err = a.p.Monitor.UpdateAlert(context, updateAlertRequest)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	projectName, workspace, auditProjectId, err := a.getAuditInfo(tk)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	auditContext := map[string]interface{}{
		"projectName": projectName,
		"alertName":   request.Name,
		"workspace":   workspace,
	}
	audit.ContextEntryMap(ctx, auditContext)
	return &alert.UpdateAlertResponse{
		Data: auditProjectId,
	}, nil
}

func (a *alertService) UpdateAlertEnable(ctx context.Context, request *alert.UpdateAlertEnableRequest) (*alert.UpdateAlertEnableResponse, error) {
	getAlertRequest := &monitor.GetAlertRequest{
		Id: request.Id,
	}
	resp, err := a.p.Monitor.GetAlert(ctx, getAlertRequest)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	if MicroServiceScope != resp.Data.AlertScope || request.TenantGroup != resp.Data.AlertScopeId {
		return nil, errors.NewPermissionError("monitor_project_alert", "update", "scope or scopeId is invalidate")
	}
	updateRequest := &monitor.UpdateAlertEnableRequest{}
	data, err := json.Marshal(request)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	err = json.Unmarshal(data, updateRequest)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	_, err = a.p.Monitor.UpdateAlertEnable(ctx, updateRequest)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	tk, err := a.getTKByTenant(request.TenantGroup)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	projectName, workspace, auditProjectId, err := a.getAuditInfo(tk)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	action := "enabled"
	if request.Enable == false {
		action = "disabled"
	}
	auditContext := map[string]interface{}{
		"projectName": projectName,
		"alertName":   resp.Data.Name,
		"workspace":   workspace,
		"action":      action,
	}
	audit.ContextEntryMap(ctx, auditContext)
	return &alert.UpdateAlertEnableResponse{Data: auditProjectId}, nil
}

func (a *alertService) DeleteAlert(ctx context.Context, request *alert.DeleteAlertRequest) (*alert.DeleteAlertResponse, error) {
	getAlertRequest := &monitor.GetAlertRequest{
		Id: request.Id,
	}
	resp, err := a.p.Monitor.GetAlert(ctx, getAlertRequest)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	if MicroServiceScope != resp.Data.AlertScope || request.TenantGroup != resp.Data.AlertScopeId {
		return nil, errors.NewPermissionError("monitor_project_alert", "update", "scope or scopeId is invalidate")
	}
	deleteRequest := &monitor.DeleteAlertRequest{
		Id: request.Id,
	}
	deleteResp, err := a.p.Monitor.DeleteAlert(ctx, deleteRequest)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	if resp != nil {
		tk, err := a.getTKByTenant(request.TenantGroup)
		if err != nil {
			return nil, errors.NewInternalServerError(err)
		}
		if tk == "" {
			return nil, fmt.Errorf("no monitor")
		}
		projectName, workspace, auditProjectId, err := a.getAuditInfo(tk)
		if err != nil {
			return &alert.DeleteAlertResponse{}, errors.NewInternalServerError(err)
		}
		auditContext := map[string]interface{}{
			"projectName": projectName,
			"alertName":   resp.Data.Name,
			"workspace":   workspace,
		}
		audit.ContextEntryMap(ctx, auditContext)
		return &alert.DeleteAlertResponse{
			Data: &alert.DeleteAlertData{
				Name:      deleteResp.Data["name"].String(),
				ProjectId: auditProjectId,
			},
		}, nil
	}
	return &alert.DeleteAlertResponse{}, nil
}

func (a *alertService) QueryCustomizeMetric(ctx context.Context, request *alert.QueryCustomizeMetricRequest) (*alert.QueryCustomizeMetricResponse, error) {
	tk, err := a.getTKByTenant(request.TenantGroup)
	req := &monitor.QueryCustomizeMetricRequest{
		Scope:   MicroServiceScope,
		ScopeId: tk,
	}
	resp, err := a.p.Monitor.QueryCustomizeMetric(ctx, req)
	if err != nil {
		return nil, errors.NewInternalServerError(fmt.Errorf("get metrics failed"))
	}
	tags := a.p.GetMicroServiceFilterTags()
	for _, metric := range resp.Data.Metrics {
		for i := 0; i < len(metric.Tags); i++ {
			tag := metric.Tags[i]
			if _, ok := tags[tag.Tag.Key]; ok {
				metric.Tags = append(metric.Tags[:i], metric.Tags[i+1:]...)
				i--
			}
		}
	}
	resp.Data.NotifySample = NotifyTemplateSample
	lang := apis.Language(ctx)
	if len(lang) > 0 && strings.HasPrefix(strings.ToLower(lang[0].String()), "en") {
		resp.Data.NotifySample = NotifyTemplateSampleEn
	}
	for i := 0; i < len(resp.Data.FunctionOperators); i++ {
		operator := resp.Data.FunctionOperators[i]
		if operator.Type != OperatorTypeOne {
			resp.Data.FunctionOperators = append(resp.Data.FunctionOperators[:i], resp.Data.FunctionOperators[i+1:]...)
			i--
		}
	}
	for i := 0; i < len(resp.Data.FilterOperators); i++ {
		operator := resp.Data.FilterOperators[i]
		if operator.Type != OperatorTypeOne {
			resp.Data.FilterOperators = append(resp.Data.FilterOperators[:i], resp.Data.FilterOperators[i+1:]...)
			i--
		}
	}
	result := &alert.QueryCustomizeMetricResponse{
		Data: resp.Data,
	}
	return result, nil
}

func (a *alertService) QueryCustomizeNotifyTarget(ctx context.Context, request *alert.QueryCustomizeNotifyTargetRequest) (*alert.QueryCustomizeNotifyTargetResponse, error) {
	req := &monitor.QueryCustomizeNotifyTargetRequest{}
	context := utils.NewContextWithHeader(ctx)
	resp, err := a.p.Monitor.QueryCustomizeNotifyTarget(context, req)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	return &alert.QueryCustomizeNotifyTargetResponse{
		Data: resp.Data,
	}, nil
}

func (a *alertService) QueryCustomizeAlerts(ctx context.Context, request *alert.QueryCustomizeAlertsRequest) (*alert.QueryCustomizeAlertsResponse, error) {
	if request.PageSize > 100 {
		request.PageSize = 20
	}
	req := &monitor.QueryCustomizeAlertRequest{
		Scope:    MicroServiceScope,
		ScopeId:  request.TenantGroup,
		PageNo:   request.PageNo,
		PageSize: request.PageSize,
		Name:     request.Name,
	}
	context := utils.NewContextWithHeader(ctx)
	resp, err := a.p.Monitor.QueryCustomizeAlert(context, req)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	result := &alert.QueryCustomizeAlertsResponse{
		Data:    resp.Data,
		UserIDs: resp.UserIDs,
	}
	return result, nil
}

func (a *alertService) GetCustomizeAlert(ctx context.Context, request *alert.GetCustomizeAlertRequest) (*alert.GetCustomizeAlertResponse, error) {
	req := &monitor.GetCustomizeAlertDetailRequest{
		Id: request.Id,
	}
	context := utils.NewContextWithHeader(ctx)
	resp, err := a.p.Monitor.GetCustomizeAlertDetail(context, req)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	if resp.Data != nil {
		if resp.Data.AlertScopeId != request.TenantGroup {
			return nil, errors.NewPermissionError("monitor_project_alert", "update", "scopeId is invalidate")
		}
	}
	result := &alert.GetCustomizeAlertResponse{
		Data: resp.Data,
	}
	return result, nil
}

func (a *alertService) CreateCustomizeAlert(ctx context.Context, request *alert.CreateCustomizeAlertRequest) (*alert.CreateCustomizeAlertResponse, error) {
	tk, err := a.getTKByTenant(request.TenantGroup)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	customizeMetricsReq := &monitor.QueryCustomizeMetricRequest{
		Scope:   MicroServiceScope,
		ScopeId: tk,
	}
	context := utils.NewContextWithHeader(ctx)
	customizeMetrics, err := a.p.Monitor.QueryCustomizeMetric(context, customizeMetricsReq)
	if err != nil {
		return nil, errors.NewInternalServerError(fmt.Errorf("get metric meta failed"))
	}

	data, err := json.Marshal(request)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	alertDetail := &monitor.CustomizeAlertDetail{}
	err = json.Unmarshal(data, alertDetail)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	err = checkCustomMetric(customizeMetrics.Data, alertDetail)
	if err != nil {
		return nil, err
	}
	if alertDetail.AlertType == "" {
		alertDetail.AlertType = "micro_service_customize"
	}
	alertDetail.AlertScope = MicroServiceScope
	alertDetail.AlertScopeId = request.TenantGroup
	alertDetail.Attributes = nil

	for _, rule := range alertDetail.Rules {
		rule.Attributes = map[string]*structpb.Value{}
		rule.Attributes[Scope] = structpb.NewStringValue(fmt.Sprintf("{{%s}}", tk))
		if rule.Filters == nil {
			rule.Filters = make([]*monitor.CustomizeAlertRuleFilter, 0)
		}
		scopeFilter := monitor.CustomizeAlertRuleFilter{}
		scopeFilter.Tag = "_metric_scope"
		scopeFilter.Operator = OperateEq
		scopeFilter.Value = structpb.NewStringValue(MicroServiceScope)
		rule.Filters = append(rule.Filters, &scopeFilter)

		scopeIDFilter := monitor.CustomizeAlertRuleFilter{}
		scopeIDFilter.Tag = "_metric_scope_id"
		scopeIDFilter.Operator = OperateEq
		scopeIDFilter.Value = structpb.NewStringValue(tk)
		rule.Filters = append(rule.Filters, &scopeIDFilter)

		if rule.Metric == StatusPage {
			continue
		}
	}
	requestData, err := json.Marshal(alertDetail)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	createAlertRequest := &monitor.CreateCustomizeAlertRequest{}
	err = json.Unmarshal(requestData, createAlertRequest)
	resp, err := a.p.Monitor.CreateCustomizeAlert(context, createAlertRequest)
	if err != nil {
		if adapt.IsAlreadyExistsError(err) {
			return nil, errors.NewAlreadyExistsError("alert")
		}
		return nil, errors.NewInternalServerError(err)
	}
	projectName, workspace, auditProjectId, err := a.getAuditInfo(tk)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	auditContext := map[string]interface{}{
		"projectName": projectName,
		"alertName":   request.Name,
		"workspace":   workspace,
	}
	audit.ContextEntryMap(ctx, auditContext)
	return &alert.CreateCustomizeAlertResponse{
		Data: &alert.CreateCustomizeAlertData{
			Id:        resp.Data,
			ProjectId: auditProjectId,
		},
	}, nil
}

func (a *alertService) getTKByTenant(tenantGroup string) (string, error) {
	tk, err := a.p.authDb.InstanceTenant.QueryTkByTenantGroup(tenantGroup)
	if err != nil {
		if err == gorm.ErrRecordNotFound {
			mspRecord, err := a.p.mspDb.MspTenant.QueryTenant(tenantGroup)
			if err != nil {
				return "", fmt.Errorf("get msp tenant failed")
			}
			if mspRecord != nil {
				tk = mspRecord.Id
			}
		} else {
			return "", fmt.Errorf("get msp tenant failed")
		}
	}
	return tk, nil
}

func (a *alertService) getOtherValue(terminusKey string) (string, string, error) {
	monitorInstance, err := a.p.authDb.Monitor.GetInstanceByTk(terminusKey)
	if err != nil {
		if err == gorm.ErrRecordNotFound {
			mspTenant, err := a.p.mspDb.MspTenant.QueryTenant(terminusKey)
			if err != nil {
				return "", "", errors.NewInternalServerError(err)
			}
			return mspTenant.RelatedProjectId, mspTenant.RelatedWorkspace, nil
		}
	}
	return monitorInstance.ProjectId, monitorInstance.Workspace, nil
}

func checkCustomMetric(customMetrics *monitor.CustomizeMetrics, alert *monitor.CustomizeAlertDetail) error {

	if customMetrics == nil {
		return errors.NewInternalServerError(fmt.Errorf("metric meta not exist"))
	}
	metrics, _, functions, aggregations := selectCustomMetrics(customMetrics)

	aliases := make(map[string]string)
	for _, rule := range alert.Rules {
		// check metric
		if metric, ok := metrics[rule.Metric]; !ok {
			return errors.NewNotFoundError("rule.Metric not exist")
		} else {
			tagsMap := make(map[string]*monitor.TagMeta)
			fieldsMap := make(map[string]*monitor.FieldMeta)
			rule.Select = make(map[string]string)
			for _, tag := range metric.Tags {
				tagsMap[tag.Tag.Key] = tag
				rule.Select[tag.Tag.Key] = "#" + tag.Tag.Key
			}
			for _, field := range metric.Fields {
				fieldsMap[field.Field.Key] = field
			}

			for _, function := range rule.Functions {
				if function.Alias == "" {
					return errors.NewInternalServerError(fmt.Errorf("the %s function alias is empty", function.Field))
				}
				if alias, ok := aliases[function.Alias]; ok {
					return errors.NewInternalServerError(fmt.Errorf("alias :%s duplicate", alias))
				} else {
					aliases[alias] = function.Alias
				}

				if field, ok := fieldsMap[function.Field]; !ok {
					return errors.NewInternalServerError(fmt.Errorf("not support rule function field %s", field))
				}
				if aggregation, ok := aggregations[function.Aggregator]; !ok {
					return errors.NewInternalServerError(fmt.Errorf(fmt.Sprintf("not support rule function aggregator %s", aggregation)))
				}
				if operator, ok := functions[function.Operator]; !ok {
					return errors.NewInternalServerError(fmt.Errorf("not support rule function operator %s", operator))
				}

			}

			for _, filter := range rule.Filters {
				if tag, ok := tagsMap[filter.Tag]; !ok {
					return errors.NewInternalServerError(fmt.Errorf("not support rule filter tag %s", tag))
				}
				if operator, ok := functions[filter.Operator]; !ok {
					return errors.NewInternalServerError(fmt.Errorf("not support rule filter operator %s", operator))
				}
			}

			for _, group := range rule.Group {
				if group, ok := tagsMap[group]; !ok {
					return errors.NewInternalServerError(fmt.Errorf("not support rule filter tag %s", group))
				}
			}
			rule.Outputs = append(rule.Outputs, "alert")
		}
	}
	return nil
}

func selectCustomMetrics(customMetric *monitor.CustomizeMetrics) (map[string]*monitor.MetricMeta,
	map[string]*monitor.Operator, map[string]*monitor.Operator, map[string]*monitor.DisplayKey) {

	metricsMap := make(map[string]*monitor.MetricMeta)
	filtersMap := make(map[string]*monitor.Operator)
	functionsMap := make(map[string]*monitor.Operator)
	aggregationsMap := make(map[string]*monitor.DisplayKey)

	// tags
	for _, filter := range customMetric.FilterOperators {
		filtersMap[filter.Key] = filter
	}

	// fields
	for _, field := range customMetric.FunctionOperators {
		functionsMap[field.Key] = field
	}

	// customMetric
	for _, metric := range customMetric.Metrics {
		metricsMap[metric.Name.Key] = metric
	}

	// aggregations
	for _, key := range customMetric.Aggregator {
		aggregationsMap[key.Key] = key
	}

	return metricsMap, filtersMap, functionsMap, aggregationsMap
}

func (a *alertService) UpdateCustomizeAlert(ctx context.Context, request *alert.UpdateCustomizeAlertRequest) (*alert.UpdateCustomizeAlertResponse, error) {
	req := &monitor.GetCustomizeAlertRequest{
		Id: request.Id,
	}
	context := utils.NewContextWithHeader(ctx)
	customAlertResp, err := a.p.Monitor.GetCustomizeAlert(context, req)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	if customAlertResp == nil {
		return nil, errors.NewNotFoundError("monitor_project_alert")
	}
	if customAlertResp.Data.AlertScope != MicroServiceScope || customAlertResp.Data.AlertScopeId != request.TenantGroup {
		return nil, errors.NewPermissionError("monitor_project_alert", "update", "scope or scopeId is invalidate")
	}
	tk, err := a.getTKByTenant(request.TenantGroup)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	if tk == "" {
		return nil, fmt.Errorf("no monitor")
	}
	customizeMetricsReq := &monitor.QueryCustomizeMetricRequest{
		Scope:   MicroServiceScope,
		ScopeId: tk,
	}
	customizeMetricsResp, err := a.p.Monitor.QueryCustomizeMetric(context, customizeMetricsReq)
	if err != nil {
		return nil, errors.NewInternalServerError(fmt.Errorf("get metric meta failed"))
	}
	data, err := json.Marshal(request)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	alertDetail := &monitor.CustomizeAlertDetail{}
	err = json.Unmarshal(data, alertDetail)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	err = checkCustomMetric(customizeMetricsResp.Data, alertDetail)
	if err != nil {
		api.Errors.Internal(err)
	}
	if request.AlertType == "" {
		alertDetail.AlertType = "micro_service_customize"
	}
	alertDetail.Enable = customAlertResp.Data.Enable
	alertDetail.AlertScope = MicroServiceScope
	alertDetail.AlertScopeId = request.TenantGroup
	alertDetail.Attributes = customAlertResp.Data.Attributes
	for _, rule := range alertDetail.Rules {
		rule.Attributes = map[string]*structpb.Value{}
		rule.Attributes[Scope] = structpb.NewStringValue(fmt.Sprintf("{{%s}}", tk))
		scopeFilter := monitor.CustomizeAlertRuleFilter{}
		scopeFilter.Tag = MetricScope
		scopeFilter.Operator = "eq"
		scopeFilter.Value = structpb.NewStringValue(MicroServiceScope)
		rule.Filters = append(rule.Filters, &scopeFilter)

		scopeIDFilter := monitor.CustomizeAlertRuleFilter{}
		scopeIDFilter.Tag = MetricScopeId
		scopeIDFilter.Operator = "eq"
		scopeIDFilter.Value = structpb.NewStringValue(tk)
		rule.Filters = append(rule.Filters, &scopeIDFilter)

		if rule.Metric == StatusPage {
			continue
		}
		scopeApplicationFilter := monitor.CustomizeAlertRuleFilter{}
		scopeApplicationFilter.Tag = "application_id"
		scopeApplicationFilter.Operator = OperateIn
		scopeApplicationFilter.Value = structpb.NewStringValue("$" + "application_id")
		rule.Filters = append(rule.Filters, &scopeApplicationFilter)
	}
	err = a.p.checkCustomizeAlert(alertDetail)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	data, err = json.Marshal(alertDetail)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	updateCustomizeAlertReq := &monitor.UpdateCustomizeAlertRequest{}
	err = json.Unmarshal(data, updateCustomizeAlertReq)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	_, err = a.p.Monitor.UpdateCustomizeAlert(context, updateCustomizeAlertReq)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	projectName, workspace, auditProjectId, err := a.getAuditInfo(tk)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	auditContext := map[string]interface{}{
		"projectName": projectName,
		"alertName":   request.Name,
		"workspace":   workspace,
	}
	audit.ContextEntryMap(ctx, auditContext)
	return &alert.UpdateCustomizeAlertResponse{
		Data: auditProjectId,
	}, nil
}

func (a *alertService) getAuditInfo(tenantGroup string) (string, string, uint64, error) {
	projectData, err := a.p.Tenant.GetTenantProject(context.Background(), &tenantpb.GetTenantProjectRequest{
		ScopeId: tenantGroup,
	})
	if err != nil {
		return "", "", 0, err
	}
	auditProjectId, err := strconv.Atoi(projectData.Data.ProjectId)
	if err != nil {
		return "", "", 0, err
	}
	project, err := a.p.bdl.GetProject(uint64(auditProjectId))
	if err != nil {
		return "", "", 0, err
	}
	return project.Name, projectData.Data.Workspace, uint64(auditProjectId), nil
}

func (a *alertService) UpdateCustomizeAlertEnable(ctx context.Context, request *alert.UpdateCustomizeAlertEnableRequest) (*alert.UpdateCustomizeAlertEnableResponse, error) {
	req := &monitor.GetCustomizeAlertRequest{
		Id: request.Id,
	}
	context := utils.NewContextWithHeader(ctx)
	resp, err := a.p.Monitor.GetCustomizeAlert(context, req)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	if resp == nil {
		return nil, errors.NewNotFoundError("id = " + strconv.FormatUint(request.Id, 10))
	}
	if resp.Data.AlertScopeId != request.TenantGroup {
		return nil, errors.NewPermissionError("monitor_project_alert", "update", "scopeId is invalidate")
	}
	updateReq := &monitor.UpdateCustomizeAlertEnableRequest{
		Id:     request.Id,
		Enable: request.Enable,
	}
	_, err = a.p.Monitor.UpdateCustomizeAlertEnable(context, updateReq)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	return nil, nil
}

func (a *alertService) DeleteCustomizeAlert(ctx context.Context, request *alert.DeleteCustomizeAlertRequest) (*alert.DeleteCustomizeAlertResponse, error) {
	alertDetailReq := &monitor.GetCustomizeAlertDetailRequest{
		Id: request.Id,
	}
	context := utils.NewContextWithHeader(ctx)
	alertDetailResp, err := a.p.Monitor.GetCustomizeAlertDetail(context, alertDetailReq)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	req := &monitor.DeleteCustomizeAlertRequest{
		Id: request.Id,
	}
	resp, err := a.p.Monitor.DeleteCustomizeAlert(context, req)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	result := &alert.DeleteCustomizeAlertResponse{
		Data: &alert.DeleteCustomizeAlertData{
			Name: resp.Data,
		},
	}
	tk, err := a.getTKByTenant(request.TenantGroup)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	if tk == "" {
		return nil, fmt.Errorf("no monitor")
	}
	projectName, workspace, auditProjectId, err := a.getAuditInfo(tk)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	auditContext := map[string]interface{}{
		"projectName": projectName,
		"alertName":   alertDetailResp.Data.Name,
		"workspace":   workspace,
	}
	audit.ContextEntryMap(ctx, auditContext)
	result.Data.ProjectId = auditProjectId
	return result, nil
}

func (a *alertService) GetAlertRecordAttrs(ctx context.Context, request *alert.GetAlertRecordAttrsRequest) (*alert.GetAlertRecordAttrsResponse, error) {
	req := &monitor.GetAlertRecordAttrRequest{
		Scope: MicroServiceScope,
	}
	context := utils.NewContextWithHeader(ctx)
	resp, err := a.p.Monitor.GetAlertRecordAttr(context, req)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	if resp == nil {
		return nil, nil
	}
	if resp.Data.AlertType == nil {
		return nil, nil
	}
	resp.Data.AlertType = append(resp.Data.AlertType, &monitor.DisplayKey{
		Key:     "micro_service_customize",
		Display: "微服务自定义",
	})
	result := &alert.GetAlertRecordAttrsResponse{
		Data: resp.Data,
	}
	return result, nil
}

func (a *alertService) GetAlertRecords(ctx context.Context, request *alert.GetAlertRecordsRequest) (*alert.GetAlertRecordsResponse, error) {
	tk, err := a.getTKByTenant(request.TenantGroup)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	if tk == "" {
		return nil, errors.NewInternalServerError(fmt.Errorf("tenantGroup has no tk"))
	}
	mspProjectId, _, err := a.getOtherValue(tk)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	if request.PageNo == 0 || request.PageNo < 1 {
		request.PageNo = 1
	}
	if request.PageSize <= 0 || request.PageSize > 100 {
		request.PageSize = 20
	}
	req := &monitor.QueryAlertRecordRequest{
		Scope:       MicroServiceScope,
		ScopeKey:    request.TenantGroup,
		AlertGroup:  request.AlertGroup,
		AlertState:  request.AlertState,
		AlertType:   request.AlertType,
		HandleState: request.HandleState,
		HandlerId:   request.HandlerId,
		PageNo:      uint64(request.PageNo),
		PageSize:    uint64(request.PageSize),
	}
	context := utils.NewContextWithHeader(ctx)
	resp, err := a.p.Monitor.QueryAlertRecord(context, req)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	var projectId int
	if mspProjectId != "" {
		projectId, err = strconv.Atoi(mspProjectId)
		if err != nil {
			return nil, errors.NewInternalServerError(err)
		}
	}
	userIds := make([]string, 0)
	if resp != nil {
		for index, value := range resp.Data.List {
			if value.HandlerId != "" {
				userIds = append(userIds, value.HandlerId)
			}
			if projectId != 0 {
				resp.Data.List[index].ProjectId = uint64(projectId)
			}
		}
	}
	return &alert.GetAlertRecordsResponse{
		Data: &alert.GetAlertRecordsData{
			List:  resp.Data.List,
			Total: resp.Data.Total,
		},
	}, nil
}

func (a *alertService) GetAlertRecord(ctx context.Context, request *alert.GetAlertRecordRequest) (*alert.GetAlertRecordResponse, error) {
	tk, err := a.getTKByTenant(request.TenantGroup)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	mspProjectId, _, err := a.getOtherValue(tk)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	var projectId int
	if mspProjectId != "" {
		projectId, err = strconv.Atoi(mspProjectId)
		if err != nil {
			return nil, errors.NewInternalServerError(err)
		}
	}
	req := &monitor.GetAlertRecordRequest{
		GroupId: request.GroupId,
	}
	context := utils.NewContextWithHeader(ctx)
	resp, err := a.p.Monitor.GetAlertRecord(context, req)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	if resp == nil {
		return nil, nil
	}
	if resp.Data.Scope != MicroServiceScope || resp.Data.ScopeKey != request.TenantGroup {
		return nil, errors.NewPermissionError("monitor_project_alert", "delete", "scopeId or scope is invalidate")
	}
	if projectId != 0 {
		resp.Data.ProjectId = uint64(projectId)
	}
	result := &alert.GetAlertRecordResponse{
		Data: resp.Data,
	}
	return result, nil
}

func (a *alertService) GetAlertHistories(ctx context.Context, request *alert.GetAlertHistoriesRequest) (*alert.GetAlertHistoriesResponse, error) {
	req := &monitor.QueryAlertHistoryRequest{
		GroupId: request.GroupId,
		Start:   request.Start,
		End:     request.End,
		Limit:   uint64(request.Limit),
	}
	context := utils.NewContextWithHeader(ctx)
	resp, err := a.p.Monitor.QueryAlertHistory(context, req)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	result := &alert.GetAlertHistoriesResponse{
		Data: resp.Data,
	}
	return result, nil
}

func (a *alertService) CreateAlertRecordIssue(ctx context.Context, request *alert.CreateAlertRecordIssueRequest) (*alert.CreateAlertRecordIssueResponse, error) {
	tk, err := a.getTKByTenant(request.TenantGroup)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	mspProjectId, _, err := a.getOtherValue(tk)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}

	context := utils.NewContextWithHeader(ctx)
	userID := apis.GetUserID(ctx)
	getRecordReq := &monitor.GetAlertRecordRequest{
		GroupId: request.GroupId,
	}
	record, err := a.p.Monitor.GetAlertRecord(context, getRecordReq)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	if record == nil || record.Data.IssueId != 0 {
		return nil, nil
	}
	if record.Data.Scope != MicroServiceScope || record.Data.ScopeKey != request.TenantGroup {
		return nil, errors.NewPermissionError("monitor_project_alert", "create", "scopeId or scope is invalidate")
	}
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	if mspProjectId == "" {
		return nil, errors.NewInternalServerError(fmt.Errorf("monitor has no project id"))
	}
	projectId, err := strconv.Atoi(mspProjectId)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	if request.Body == nil {
		request.Body = make(map[string]*structpb.Value)
		request.Body["creator"] = structpb.NewStringValue(userID)
		request.Body["projectID"] = structpb.NewNumberValue(float64(projectId))
	}
	createIssue := &apistructs.IssueCreateRequest{}
	err = mapstructure.Decode(request.Body, createIssue)
	req := &monitor.CreateAlertIssueRequest{}
	data, err := json.Marshal(createIssue)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	err = json.Unmarshal(data, req)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	req.GroupId = request.GroupId
	_, err = a.p.Monitor.CreateAlertIssue(context, req)
	if err != nil {
		return nil, errors.NewInternalServerError(fmt.Errorf("alert record issue create fail"))
	}
	return nil, nil
}

func (a *alertService) UpdateAlertRecordIssue(ctx context.Context, request *alert.UpdateAlertRecordIssueRequest) (*alert.UpdateAlertRecordIssueResponse, error) {
	req := &monitor.GetAlertRecordRequest{
		GroupId: request.GroupId,
	}
	context := utils.NewContextWithHeader(ctx)
	record, err := a.p.Monitor.GetAlertRecord(context, req)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	if record == nil || record.Data.IssueId != 0 {
		return nil, nil
	}
	if record.Data.Scope != MicroServiceScope || record.Data.ScopeKey != request.TenantGroup {
		return nil, errors.NewPermissionError("monitor_project_alert", "update", "scopeId or scope is invalidate")
	}
	issueUpdate := &apistructs.IssueUpdateRequest{}
	err = mapstructure.Decode(request.Body, issueUpdate)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	data, err := json.Marshal(issueUpdate)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	updateDataIssueReq := &monitor.UpdateAlertIssueRequest{}
	err = json.Unmarshal(data, updateDataIssueReq)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	updateDataIssueReq.GroupId = request.GroupId
	_, err = a.p.Monitor.UpdateAlertIssue(context, updateDataIssueReq)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	return nil, nil
}

func (a *alertService) DashboardPreview(ctx context.Context, request *alert.DashboardPreviewRequest) (*alert.DashboardPreviewResponse, error) {
	request.AlertScope = MicroServiceScope
	tk, err := a.getTKByTenant(request.TenantGroup)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	if tk == "" {
		return nil, errors.NewInternalServerError(fmt.Errorf("no monitor"))
	}
	request.AlertScopeId = tk
	data, err := json.Marshal(request)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	alertDetail := &monitor.QueryDashboardByAlertRequest{}
	err = json.Unmarshal(data, alertDetail)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	context := utils.NewContextWithHeader(ctx)
	view, err := a.p.Monitor.QueryDashboardByAlert(context, alertDetail)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	result := &alert.DashboardPreviewResponse{
		Data: view.Data,
	}
	return result, nil
}

func (a *alertService) GetAlertConditions(ctx context.Context, request *alert.GetAlertConditionsRequest) (*alert.GetAlertConditionsResponse, error) {
	conditionReq := &monitor.GetAlertConditionsRequest{
		ScopeType: request.ScopeType,
	}
	context := utils.NewContextWithHeader(ctx)
	result, err := a.p.Monitor.GetAlertConditions(context, conditionReq)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	data, err := json.Marshal(result.Data)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	resp := &alert.GetAlertConditionsResponse{}
	err = json.Unmarshal(data, &resp.Data)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	return resp, nil
}

func (a *alertService) GetAlertConditionsValue(ctx context.Context, request *alert.GetAlertConditionsValueRequest) (*alert.GetAlertConditionsValueResponse, error) {
	conditionReq := &monitor.GetAlertConditionsValueRequest{
		Conditions: request.Conditions,
	}
	context := utils.NewContextWithHeader(ctx)
	result, err := a.p.Monitor.GetAlertConditionsValue(context, conditionReq)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	data, err := json.Marshal(result.Data)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	resp := &alert.GetAlertConditionsValueResponse{
		Data: make([]*monitor.AlertConditionsValue, 0),
	}
	err = json.Unmarshal(data, &resp.Data)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	return resp, nil
}
