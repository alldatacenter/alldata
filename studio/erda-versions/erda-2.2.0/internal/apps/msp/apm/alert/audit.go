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
	"fmt"
	"strconv"

	monitor "github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	"github.com/erda-project/erda-proto-go/msp/apm/alert/pb"
	"github.com/erda-project/erda/apistructs"
)

var skipAudit = map[string]interface{}{
	"isSkip": true,
}

var getTenantGroupDetails = func(a *alertService, tenantGroup string) (*apistructs.TenantGroupDetails, error) {
	return a.p.bdl.GetTenantGroupDetails(tenantGroup)
}

var getProject = func(a *alertService, id uint64) (*apistructs.ProjectDTO, error) {
	return a.p.bdl.GetProject(id)
}

var queryAlert = func(a *alertService, ctx context.Context, request *monitor.QueryAlertRequest) (*monitor.QueryAlertsResponse, error) {
	return a.p.Monitor.QueryAlert(ctx, request)
}

var queryCustomizeAlert = func(a *alertService, ctx context.Context, request *monitor.QueryCustomizeAlertRequest) (*monitor.QueryCustomizeAlertResponse, error) {
	return a.p.Monitor.QueryCustomizeAlert(ctx, request)
}

func (a *alertService) auditOperateMicroserviceAlert(tmp apistructs.TemplateName, act string) func(ctx context.Context, req interface{}, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
	return func(ctx context.Context, req interface{}, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
		action := act

		enable := false
		tg := ""

		switch req.(type) {
		case *pb.UpdateAlertRequest:
			reqq := req.(*pb.UpdateAlertRequest)
			enable = reqq.Enable
			tg = reqq.TenantGroup
		case *pb.DeleteAlertRequest:
			reqq := req.(*pb.DeleteAlertRequest)
			enable = false
			tg = reqq.TenantGroup
		case *pb.UpdateAlertEnableRequest:
			reqq := req.(*pb.UpdateAlertEnableRequest)
			enable = reqq.Enable
			tg = reqq.TenantGroup
		default:
			return nil, skipAudit, nil
		}

		if action == "" {
			enable := enable
			if enable == true {
				action = "enabled"
			} else if enable == false {
				action = "disabled"
			}
		}
		info, err := getTenantGroupDetails(a, tg)
		if err != nil {
			return nil, nil, err
		}
		if len(info.ProjectID) <= 0 {
			return nil, nil, nil
		}
		projectID, err := strconv.ParseUint(info.ProjectID, 10, 64)
		if err != nil {
			return nil, nil, err
		}
		project, err := getProject(a, projectID)
		if err != nil {
			return nil, nil, err
		}
		if project == nil {
			return nil, skipAudit, nil
		}
		name := "tg=" + tg
		if tmp == apistructs.DeleteMicroserviceAlert {
			ress := resp.(*pb.DeleteAlertResponse)
			if err == nil && ress.Data != nil && len(ress.Data.Name) > 0 {
				name = fmt.Sprint(ress.Data.Name)
			}
		} else {
			data, err := queryAlert(a, ctx, &monitor.QueryAlertRequest{
				Scope:    "micro_service",
				ScopeId:  tg,
				PageSize: 1,
				PageNo:   1,
			})
			if err != nil {
				return nil, nil, err
			}

			if len(data.Data.List) <= 0 {
				return nil, skipAudit, nil
			}
			if err == nil && data != nil {
				name = data.Data.List[0].Name
			}
		}

		return uint64(projectID), map[string]interface{}{
			"projectId":   projectID,
			"projectName": project.Name,
			"alertName":   name,
			"action":      action,
		}, nil
	}
}

func (a *alertService) auditOperateMicroserviceCustomAlert(tmp apistructs.TemplateName, act string) func(ctx context.Context, req interface{}, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
	return func(ctx context.Context, req interface{}, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
		enable := false
		tg := ""

		switch req.(type) {
		case *pb.UpdateCustomizeAlertRequest:
			reqq := req.(*pb.UpdateCustomizeAlertRequest)
			enable = reqq.Enable
			tg = reqq.TenantGroup
		case *pb.DeleteCustomizeAlertRequest:
			reqq := req.(*pb.DeleteCustomizeAlertRequest)
			enable = false
			tg = reqq.TenantGroup
		case *pb.UpdateCustomizeAlertEnableRequest:
			reqq := req.(*pb.UpdateCustomizeAlertEnableRequest)
			enable = reqq.Enable
			tg = reqq.TenantGroup
		default:
			return nil, skipAudit, nil
		}

		action := act
		if action == "" {
			if enable == true {
				action = "enabled"
			} else if enable == false {
				action = "disabled"
			}
		}

		info, err := getTenantGroupDetails(a, tg)
		if err != nil {
			return nil, nil, err
		}
		if len(info.ProjectID) <= 0 {
			return nil, nil, nil
		}
		projectID, err := strconv.ParseUint(info.ProjectID, 10, 64)
		if err != nil {
			return nil, nil, err
		}
		project, err := getProject(a, projectID)
		if err != nil {
			return nil, nil, err
		}
		if project == nil {
			return nil, skipAudit, nil
		}
		name := "tg=" + tg
		if tmp == apistructs.DeleteMicroserviceCustomAlert {
			respBody := resp.(*pb.DeleteCustomizeAlertResponse)
			if err == nil && respBody.Data != nil && len(respBody.Data.Name) != 0 {
				name = fmt.Sprint(respBody.Data.Name)
			}
		} else {
			data, err := queryCustomizeAlert(a, ctx, &monitor.QueryCustomizeAlertRequest{
				Scope:    "micro_service",
				ScopeId:  tg,
				PageSize: 1,
				PageNo:   1,
			})
			if err != nil {
				return nil, nil, err
			}
			if len(data.Data.List) <= 0 {
				return nil, nil, nil
			}
			name = data.Data.List[0].Name
		}
		return uint64(projectID), map[string]interface{}{
			"projectId":   projectID,
			"projectName": project.Name,
			"alertName":   name,
			"action":      action,
		}, nil
	}
}
