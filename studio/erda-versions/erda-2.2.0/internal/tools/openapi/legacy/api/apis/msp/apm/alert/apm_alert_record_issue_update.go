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

import "github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"

var APM_ALERT_RECORD_ISSUE_UPDATE = apis.ApiSpec{
	Path:        "/api/tmc/tenantGroup/<tenantGroup>/alert-records/<groupId>/issues/<issueId>",
	BackendPath: "/api/msp/apm/<tenantGroup>/alert-records/<groupId>/issues",
	Host:        "msp.marathon.l4lb.thisdcos.directory:8080",
	Scheme:      "http",
	Method:      "PUT",
	CheckLogin:  true,
	CheckToken:  true,
	Doc:         "summary: 修改微服务告警记录工单",
}
