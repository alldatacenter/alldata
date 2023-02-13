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

package common

import (
	"context"
	"strconv"
	"strings"
	"time"

	"github.com/pkg/errors"
	log "github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/bundle"
	"github.com/erda-project/erda/pkg/common/apis"
)

type ScopeInfo struct {
	ProjectId   string
	Workspace   string
	AppId       string
	ServiceName string
	RuntimeName string
}

func MakeAuditInfo(reqCtx context.Context, scopeInfo ScopeInfo, name apistructs.TemplateName, errInfo error, ctx map[string]interface{}) *apistructs.Audit {
	if reqCtx == nil {
		return nil
	}
	orgId := apis.GetOrgID(reqCtx)
	if orgId == "" {
		return nil
	}
	userId := apis.GetUserID(reqCtx)
	if userId == "" {
		return nil
	}
	id, err := strconv.ParseUint(scopeInfo.ProjectId, 10, 64)
	if err != nil {
		log.Errorf("parse failed, err:%+v", errors.WithStack(err))
		return nil
	}
	projectInfo, err := bundle.Bundle.GetProject(id)
	if err != nil {
		log.Errorf("get project failed, err:%+v", errors.WithStack(err))
		return nil
	}
	ctx["projectId"] = scopeInfo.ProjectId
	ctx["project"] = projectInfo.Name
	audit := &apistructs.Audit{
		UserID:       userId,
		ScopeType:    "project",
		ScopeID:      id,
		TemplateName: name,
		// TODO: get start time from reqCtx
		StartTime: strconv.FormatInt(time.Now().Unix(), 10),
	}
	audit.ProjectID = id
	id, err = strconv.ParseUint(orgId, 10, 64)
	if err != nil {
		return nil
	}
	audit.OrgID = id
	if scopeInfo.AppId != "" {
		ctx["appId"] = scopeInfo.AppId
		id, _ = strconv.ParseUint(scopeInfo.AppId, 10, 64)
		appInfo, err := bundle.Bundle.GetApp(id)
		if err != nil {
			log.Errorf("get app fialed, err:%+v", err)
		}
		ctx["app"] = appInfo.Name
		audit.AppID = id
	}
	if scopeInfo.Workspace != "" {
		ctx["workspace"] = strings.ToUpper(scopeInfo.Workspace)
	}
	if scopeInfo.ServiceName != "" {
		ctx["service"] = scopeInfo.ServiceName
	}
	if scopeInfo.RuntimeName != "" {
		ctx["runtime"] = scopeInfo.RuntimeName
	}
	if errInfo == nil {
		audit.Result = apistructs.SuccessfulResult
	} else {
		return nil
	}
	audit.Context = ctx
	audit.EndTime = strconv.FormatInt(time.Now().Unix(), 10)
	return audit
}
