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

package endpoints

import (
	"net/http"
	"strconv"
	"time"

	"github.com/erda-project/erda/apistructs"
)

type auditParams struct {
	orgID        int64
	projectID    int64
	userID       string
	templateName string
	ctx          map[string]interface{}
}

func (e *Endpoints) audit(req *http.Request, params auditParams) error {
	org, err := e.bdl.GetOrg(params.orgID)
	if err != nil {
		return err
	}

	project, err := e.bdl.GetProject(uint64(params.projectID))
	if err != nil {
		return err
	}

	params.ctx["orgName"] = org.Name
	params.ctx["projectName"] = project.Name

	now := strconv.FormatInt(time.Now().Unix(), 10)
	return e.bdl.CreateAuditEvent(&apistructs.AuditCreateRequest{
		Audit: apistructs.Audit{
			UserID:       params.userID,
			ScopeType:    apistructs.ProjectScope,
			ScopeID:      uint64(params.projectID),
			OrgID:        uint64(params.orgID),
			ProjectID:    uint64(params.projectID),
			Context:      params.ctx,
			TemplateName: apistructs.TemplateName(params.templateName),
			Result:       "success",
			StartTime:    now,
			EndTime:      now,
			ClientIP:     getRealIP(req),
			UserAgent:    req.UserAgent(),
		},
	})
}
