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
	"net/http"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/strutil"
)

func (e *Endpoints) GetWorkbenchData(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var workReq apistructs.WorkbenchRequest
	if err := e.queryStringDecoder.Decode(&workReq, r.URL.Query()); err != nil {
		return apierrors.ErrGetWorkBenchData.InvalidParameter(err).ToResp(), nil
	}
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrGetWorkBenchData.NotLogin().ToResp(), nil
	}

	// if projectIDs not specified, get all my projectIDs by default
	if len(workReq.ProjectIDs) == 0 {
		projectIDs, err := e.bdl.GetMyProjectIDs(workReq.OrgID, userID.String())
		if err != nil {
			return apierrors.ErrGetWorkBenchData.InternalError(err).ToResp(), nil
		}
		workReq.ProjectIDs = projectIDs
	}

	res, err := e.workBench.GetUndoneProjectItems(workReq, userID.String())
	if err != nil {
		return apierrors.ErrGetWorkBenchData.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(res.Data)
}

func (e *Endpoints) GetIssuesForWorkbench(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var pageReq apistructs.IssuePagingRequest
	if err := e.queryStringDecoder.Decode(&pageReq, r.URL.Query()); err != nil {
		return apierrors.ErrPagingIssues.InvalidParameter(err).ToResp(), nil
	}

	switch pageReq.OrderBy {
	case "":
	case "planStartedAt":
		pageReq.OrderBy = "plan_started_at"
	case "planFinishedAt":
		pageReq.OrderBy = "plan_finished_at"
	case "assignee":
		pageReq.OrderBy = "assignee"
	default:
		return apierrors.ErrPagingIssues.InvalidParameter("orderBy").ToResp(), nil
	}

	// Authentication
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrPagingIssues.NotLogin().ToResp(), nil
	}
	pageReq.IdentityInfo = identityInfo
	pageReq.External = true

	issues, total, err := e.issue.PagingForWorkbench(pageReq)
	if err != nil {
		return apierrors.ErrPagingIssues.InternalError(err).ToResp(), nil
	}
	userIDs := pageReq.GetUserIDs()
	for _, issue := range issues {
		userIDs = append(userIDs, issue.Creator, issue.Assignee, issue.Owner)
	}
	userIDs = strutil.DedupSlice(userIDs, true)
	return httpserver.OkResp(apistructs.IssuePagingResponseData{
		Total: total,
		List:  issues,
	})
}
