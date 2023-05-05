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
	"bytes"
	"encoding/json"
	"fmt"
	"net/url"
	"strconv"

	"github.com/erda-project/erda-proto-go/dop/issue/stream/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/tools/cli/command"
	"github.com/erda-project/erda/tools/cli/utils"
)

func GetIssue(ctx *command.Context, orgID, projectID, issueID uint64) (*apistructs.Issue, error) {
	var resp apistructs.IssueGetResponse
	var b bytes.Buffer

	response, err := ctx.Get().Path(fmt.Sprintf("/api/issues/%d", issueID)).
		Param("orgID", strconv.FormatUint(orgID, 10)).
		Param("projectID", strconv.FormatUint(projectID, 10)).
		Do().Body(&b)
	if err != nil {
		return nil, fmt.Errorf(utils.FormatErrMsg(
			"get issue", "failed to request ("+err.Error()+")", false))
	}

	if !response.IsOK() {
		return nil, fmt.Errorf(utils.FormatErrMsg("get issue",
			fmt.Sprintf("failed to request, status-code: %d, content-type: %s, raw bod: %s",
				response.StatusCode(), response.ResponseHeader("Content-Type"), b.String()), false))
	}

	if err := json.Unmarshal(b.Bytes(), &resp); err != nil {
		return nil, fmt.Errorf(utils.FormatErrMsg("get issue",
			fmt.Sprintf("failed to unmarshal response ("+err.Error()+")"), false))
	}

	if !resp.Success {
		return nil, fmt.Errorf(utils.FormatErrMsg("get issue",
			fmt.Sprintf("failed to request, error code: %s, error message: %s",
				resp.Error.Code, resp.Error.Msg), false))
	}

	return resp.Data, nil
}

func ListMyIssue(ctx *command.Context, req *apistructs.IssuePagingRequest) (*apistructs.IssuePagingResponseData, error) {
	var resp apistructs.IssuePagingResponse
	var b bytes.Buffer

	values := url.Values{}
	for _, s := range req.State {
		values.Add("state", strconv.FormatInt(s, 10))
	}
	for _, a := range req.Assignees {
		values.Add("assignee", a)
	}
	for _, t := range req.Type {
		values.Add("type", string(t))
	}

	response, err := ctx.Get().Path("/api/issues").
		Param("pageNo", strconv.FormatUint(req.PageNo, 10)).
		Param("pageSize", strconv.FormatUint(req.PageSize, 10)).
		Param("orgID", strconv.FormatInt(req.OrgID, 10)).
		Param("projectID", strconv.FormatUint(req.ProjectID, 10)).
		Param("orderBy", req.OrderBy).Param("asc", "false").
		Params(values).Do().Body(&b)
	if err != nil {
		return nil, fmt.Errorf(utils.FormatErrMsg(
			"get issues", "failed to request ("+err.Error()+")", false))
	}

	if !response.IsOK() {
		return nil, fmt.Errorf(utils.FormatErrMsg("list issue",
			fmt.Sprintf("failed to request, status-code: %d, content-type: %s, raw bod: %s",
				response.StatusCode(), response.ResponseHeader("Content-Type"), b.String()), false))
	}

	if err := json.Unmarshal(b.Bytes(), &resp); err != nil {
		return nil, fmt.Errorf(utils.FormatErrMsg("list issue",
			fmt.Sprintf("failed to unmarshal response ("+err.Error()+")"), false))
	}

	if !resp.Success {
		return nil, fmt.Errorf(utils.FormatErrMsg("list issue",
			fmt.Sprintf("failed to request, error code: %s, error message: %s",
				resp.Error.Code, resp.Error.Msg), false))
	}

	return resp.Data, nil
}

func ListState(ctx *command.Context, orgID uint64, req apistructs.IssueStateRelationGetRequest) ([]apistructs.IssueStateRelation, error) {
	var resp apistructs.IssueStateRelationGetResponse
	var b bytes.Buffer

	response, err := ctx.Get().Path("/api/issues/actions/get-state-relations").
		Param("Org-ID", strconv.FormatUint(orgID, 10)).
		Param("projectID", strconv.FormatUint(req.ProjectID, 10)).
		Do().Body(&b)

	if err != nil {
		return nil, fmt.Errorf(utils.FormatErrMsg(
			"get state-relations detail", "failed to request ("+err.Error()+")", false))
	}

	if !response.IsOK() {
		return nil, fmt.Errorf(utils.FormatErrMsg("get state-relations",
			fmt.Sprintf("failed to request, status-code: %d, content-type: %s, raw bod: %s",
				response.StatusCode(), response.ResponseHeader("Content-Type"), b.String()), false))
	}

	if err := json.Unmarshal(b.Bytes(), &resp); err != nil {
		return nil, fmt.Errorf(utils.FormatErrMsg("get state-relations",
			fmt.Sprintf("failed to unmarshal response ("+err.Error()+")"), false))
	}

	if !resp.Success {
		return nil, fmt.Errorf(utils.FormatErrMsg("get state-relations",
			fmt.Sprintf("failed to request, error code: %s, error message: %s",
				resp.Error.Code, resp.Error.Msg), false))
	}

	return resp.Data, nil
}

func GetTodoStateIds(states []apistructs.IssueStateRelation) ([]int64, error) {
	var todoStates []int64
	for _, s := range states {
		switch s.IssueType {
		case apistructs.IssueTypeRequirement,
			apistructs.IssueTypeBug,
			apistructs.IssueTypeTask:
			// goto next switch
		default:
			continue
		}

		switch s.StateBelong {
		case apistructs.IssueStateBelongOpen,
			apistructs.IssueStateBelongWorking,
			apistructs.IssueStateBelongReopen:
			todoStates = append(todoStates, s.StateID)
		}
	}

	return todoStates, nil
}

func CreateIssueComment(ctx *command.Context, orgID uint64, request *pb.CommentIssueStreamBatchCreateRequest) error {
	var resp apistructs.Header
	var b bytes.Buffer

	respponse, err := ctx.Post().Path("/api/issues/actions/batch-create-comment-stream").JSONBody(request).
		Header("Org-ID", strconv.FormatUint(orgID, 10)).
		Do().Body(&b)
	if err != nil {
		return fmt.Errorf(
			utils.FormatErrMsg("create", "failed to request ("+err.Error()+")", false))
	}

	if !respponse.IsOK() {
		return fmt.Errorf(utils.FormatErrMsg("create issue comments",
			fmt.Sprintf("failed to request, status-code: %d, content-type: %s, raw bod: %s",
				respponse.StatusCode(), respponse.ResponseHeader("Content-Type"), b.String()), false))
	}

	if err := json.Unmarshal(b.Bytes(), &resp); err != nil {
		return fmt.Errorf(utils.FormatErrMsg("create issue comments",
			fmt.Sprintf("failed to unmarshal create response ("+err.Error()+")"), false))
	}

	if !resp.Success {
		return fmt.Errorf(utils.FormatErrMsg("create issue comments",
			fmt.Sprintf("failed to request, error code: %s, error message: %s",
				resp.Error.Code, resp.Error.Msg), false))
	}

	return nil
}

func UpdateIssue(ctx *command.Context, orgID uint64, request *apistructs.IssueUpdateRequest) error {
	var resp apistructs.IssueUpdateResponse
	var b bytes.Buffer

	path := fmt.Sprintf("/api/issues/%d", request.ID)
	response, err := ctx.Put().Path(path).JSONBody(request).
		Header("Org-ID", strconv.FormatUint(orgID, 10)).
		Do().Body(&b)
	if err != nil {
		return fmt.Errorf(utils.FormatErrMsg(
			"get issues", "failed to request ("+err.Error()+")", false))
	}

	if !response.IsOK() {
		return fmt.Errorf(utils.FormatErrMsg("update issue",
			fmt.Sprintf("failed to request, status-code: %d, content-type: %s, raw bod: %s",
				response.StatusCode(), response.ResponseHeader("Content-Type"), b.String()), false))
	}

	if err := json.Unmarshal(b.Bytes(), &resp); err != nil {
		return fmt.Errorf(utils.FormatErrMsg("update issue",
			fmt.Sprintf("failed to unmarshal response ("+err.Error()+")"), false))
	}

	if !resp.Success {
		return fmt.Errorf(utils.FormatErrMsg("update issue",
			fmt.Sprintf("failed to request, error code: %s, error message: %s",
				resp.Error.Code, resp.Error.Msg), false))
	}

	return nil
}
