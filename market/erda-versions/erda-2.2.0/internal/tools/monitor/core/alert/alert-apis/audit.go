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
	"context"
	"fmt"
	"strconv"

	"github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/common/apis"
)

var getOrg = func(m *alertService, idOrName interface{}) (*apistructs.OrgDTO, error) {
	return m.p.bdl.GetOrg(idOrName)
}
var getAlert = func(m *alertService, ctx context.Context, req *pb.GetAlertRequest) (*pb.GetAlertResponse, error) {
	return m.p.alertService.GetAlert(ctx, req)
}
var getGetCustomizeAlert = func(m *alertService, ctx context.Context, request *pb.GetCustomizeAlertRequest) (*pb.GetCustomizeAlertResponse, error) {
	return m.p.alertService.GetCustomizeAlert(ctx, request)
}

func (m *alertService) auditOperateOrgAlert(act string) func(ctx context.Context, req interface{}, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
	return func(ctx context.Context, req, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
		reqq := req.(*pb.UpdateOrgAlertEnableRequest)
		action := act
		if action == "" {
			enable := reqq.Enable
			if enable == true {
				action = "enabled"
			} else {
				action = "disabled"
			}
		}
		org, err := getOrg(m, apis.GetOrgID(ctx))
		if err != nil {
			return apis.GetOrgID(ctx), nil, err
		}
		if org == nil {
			return apis.GetOrgID(ctx), nil, nil
		}
		name := ""
		alert, err := getAlert(m, ctx, &pb.GetAlertRequest{Id: reqq.Id})
		if err == nil && alert != nil && alert.Data != nil {
			name = alert.Data.Name
		}

		return apis.GetOrgID(ctx), map[string]interface{}{
			"alertID":   reqq.Id,
			"alertName": name,
			"orgName":   org.Name,
			"action":    action,
		}, nil
	}
}
func (m *alertService) auditOperateOrgCustomAlert(tmp apistructs.TemplateName, act string) func(ctx context.Context, req interface{}, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
	return func(ctx context.Context, req, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
		action := act
		reqq := req.(*pb.UpdateOrgCustomizeAlertEnableRequest)
		if action == "" {
			enable := reqq.Enable
			if enable == true {
				action = "enabled"
			} else if enable == false {
				action = "disabled"
			}
		}
		org, err := getOrg(m, apis.GetOrgID(ctx))
		if err != nil {
			return apis.GetOrgID(ctx), nil, err
		}
		if org == nil {
			return apis.GetOrgID(ctx), nil, nil
		}

		id := uint64(reqq.Id)
		name := strconv.FormatInt(reqq.Id, 10)
		if tmp == apistructs.DeleteOrgCustomAlert {
			respBody := resp.(*struct {
				apistructs.Header
				Data map[string]interface{} `json:"data"`
			})

			if err == nil && respBody.Data != nil && respBody.Data["name"] != nil {
				name = fmt.Sprint(respBody.Data["name"])
			}
		} else {
			alert, err := getGetCustomizeAlert(m, ctx, &pb.GetCustomizeAlertRequest{
				Id: id,
			})
			if err == nil && alert != nil && alert.Data != nil {
				name = alert.Data.Name
			}
		}
		return apis.GetOrgID(ctx), map[string]interface{}{
			"alertID":   id,
			"alertName": name,
			"orgName":   org.Name,
			"action":    action,
		}, nil
	}
}
