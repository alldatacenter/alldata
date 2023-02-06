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
	"context"
	"encoding/json"
	"net/http"
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/service/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
)

// CreateRule creates release rule record
func (e *Endpoints) CreateRule(ctx context.Context, r *http.Request, _ map[string]string) (httpserver.Responser, error) {
	request := ctx.Value("CreateUpdateDeleteReleaseRuleRequest").(*apistructs.CreateUpdateDeleteReleaseRuleRequest)
	data, apiError := e.releaseRule.Create(request)
	if apiError != nil {
		return apiError.ToResp(), nil
	}
	return httpserver.OkResp(data)
}

// ListRules lists release rules for the given project
func (e *Endpoints) ListRules(ctx context.Context, r *http.Request, _ map[string]string) (httpserver.Responser, error) {
	request := ctx.Value("CreateUpdateDeleteReleaseRuleRequest").(*apistructs.CreateUpdateDeleteReleaseRuleRequest)
	data, apiError := e.releaseRule.List(request)
	if apiError != nil {
		return apiError.ToResp(), nil
	}
	return httpserver.OkResp(data)
}

// UpdateRule updates the given release rule
func (e *Endpoints) UpdateRule(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	request := ctx.Value("CreateUpdateDeleteReleaseRuleRequest").(*apistructs.CreateUpdateDeleteReleaseRuleRequest)
	data, apiError := e.releaseRule.Update(request)
	if apiError != nil {
		return apiError.ToResp(), nil
	}
	return httpserver.OkResp(data)
}

// DeleteRule deletes the given release rule
func (e *Endpoints) DeleteRule(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	request := ctx.Value("CreateUpdateDeleteReleaseRuleRequest").(*apistructs.CreateUpdateDeleteReleaseRuleRequest)
	apiError := e.releaseRule.Delete(request)
	if apiError != nil {
		return apiError.ToResp(), nil
	}
	return httpserver.OkResp(nil)
}

func (e *Endpoints) ReleaseRuleMiddleware(handler httpserver.Handler) httpserver.Handler {
	return func(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
		l := logrus.WithField("func", "*Endpoints.ReleaseMiddleWare")

		info, apierr := getUserInfo(r)
		if apierr != nil {
			return apierr.ToResp(), nil
		}

		var (
			oldReleaseRule *apistructs.BranchReleaseRuleModel
			err            error
		)
		id := vars["id"]
		if id != "" {
			oldReleaseRule, err = e.releaseRule.Get(info.projectID, id)
			if err != nil {
				l.WithError(err).Errorln("failed to get releaseRule")
				return apierrors.ErrGetReleaseRule.InternalError(err).ToResp(), nil
			}
		}

		if apiError := e.releaseRuleAuth(info, &ctx, r, vars); apiError != nil {
			return apiError.ToResp(), nil
		}
		resp, err := handler(ctx, r, vars)
		if err != nil {
			return resp, err
		}

		go func() {
			if err := e.releaseRuleAudit(r, vars, info, oldReleaseRule); err != nil {
				l.WithError(err).Errorln("failed to audit")
			}
		}()
		return resp, nil
	}
}

type userInfo struct {
	orgID     uint64
	projectID uint64
	userID    uint64
}

func (e *Endpoints) releaseRuleAuth(info *userInfo, ctx *context.Context, r *http.Request, vars map[string]string) *errorresp.APIError {
	var l = logrus.WithField("func", "*Endpoints.ReleaseRuleMiddleware")

	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		l.WithError(err).Errorln("failed to GetIdentityInfo")
		return apierrors.ErrAuthReleaseRule.NotLogin()
	}
	access := false
	// 鉴权
	switch r.Method {
	case http.MethodPost, http.MethodPut, http.MethodDelete:
		access, err = e.hasWriteAccess(identity, int64(info.projectID), true, 0)
	default:
		access, err = e.hasReadAccess(identity, int64(info.projectID))
	}
	if err != nil {
		l.WithError(err).Errorln("failed to authenticate")
		return apierrors.ErrAuthReleaseRule.InternalError(err)
	}
	if !access {
		return apierrors.ErrAuthReleaseRule.AccessDenied()
	}

	// 封装参数
	var request = apistructs.CreateUpdateDeleteReleaseRuleRequest{
		OrgID:     info.orgID,
		ProjectID: info.projectID,
		UserID:    info.userID,
		RuleID:    vars["id"],
		Body:      new(apistructs.CreateUpdateReleaseRuleRequestBody),
	}
	switch r.Method {
	case http.MethodPost, http.MethodPut:
		if err := json.NewDecoder(r.Body).Decode(request.Body); err != nil {
			return apierrors.ErrAuthReleaseRule.InvalidParameter("can not read request body")
		}
	}
	*ctx = context.WithValue(*ctx, "CreateUpdateDeleteReleaseRuleRequest", &request)
	return nil
}

func (e *Endpoints) releaseRuleAudit(r *http.Request, vars map[string]string, info *userInfo,
	releaseRule *apistructs.BranchReleaseRuleModel) error {

	org, err := e.bdl.GetOrg(info.orgID)
	if err != nil {
		return err
	}
	project, err := e.bdl.GetProject(info.projectID)
	if err != nil {
		return err
	}

	auditCtx := map[string]interface{}{
		"orgName":     org.Name,
		"projectName": project.Name,
	}

	var templateName string
	switch r.Method {
	case http.MethodPost:
		templateName = string(apistructs.CreateReleaseRuleTemplate)
		auditCtx["releaseRule"] = releaseRule.Pattern
	case http.MethodPut:
		templateName = string(apistructs.UpdateReleaseRuleTemplate)
		auditCtx["oldReleaseRule"] = releaseRule.Pattern
		id := vars["id"]
		newReleaseRule, err := e.releaseRule.Get(info.projectID, id)
		if err != nil {
			return err
		}
		auditCtx["newReleaseRule"] = newReleaseRule.Pattern
	case http.MethodDelete:
		templateName = string(apistructs.DeleteReleaseRuleTemplate)
		auditCtx["releaseRule"] = releaseRule.Pattern
	default:
		return nil
	}

	return e.audit(r, auditParams{
		orgID:        int64(info.orgID),
		projectID:    int64(info.projectID),
		userID:       strconv.FormatUint(info.userID, 10),
		templateName: templateName,
		ctx:          auditCtx,
	})
}

func getUserInfo(r *http.Request) (*userInfo, *errorresp.APIError) {
	l := logrus.WithField("func", "*getUserInfo")
	userIDStr, err := user.GetUserID(r)
	if err != nil {
		l.WithError(err).Errorln("failed to GetUserID")
		return nil, apierrors.ErrAuthReleaseRule.NotLogin()
	}
	userID, err := strconv.ParseUint(userIDStr.String(), 10, 32)
	if err != nil {
		l.WithError(err).WithField("userID", userIDStr).Errorln("failed to ParseUint")
		return nil, apierrors.ErrAuthReleaseRule.InvalidParameter("invalid userID").NotLogin()
	}
	orgID, err := user.GetOrgID(r)
	if err != nil {
		l.WithError(err).Errorln("failed to GetOrgID")
		return nil, apierrors.ErrAuthReleaseRule.NotLogin()
	}
	projectIDStr := r.URL.Query().Get("projectID")
	projectID, err := strconv.ParseUint(projectIDStr, 10, 64)
	if err != nil {
		l.WithError(err).WithField("projectID", projectIDStr).Errorln("failed to parseUint")
		return nil, apierrors.ErrAuthReleaseRule.InvalidParameter("invalid query parameter projectID")
	}
	return &userInfo{
		orgID:     orgID,
		projectID: projectID,
		userID:    userID,
	}, nil
}
