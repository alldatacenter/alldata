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

package api

import (
	"errors"
	"fmt"
	"os"
	"strconv"
	"strings"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/diceworkspace"
	"github.com/erda-project/erda/internal/tools/gittar/conf"
	"github.com/erda-project/erda/internal/tools/gittar/helper"
	"github.com/erda-project/erda/internal/tools/gittar/models"
	"github.com/erda-project/erda/internal/tools/gittar/pkg/gitmodule"
	"github.com/erda-project/erda/internal/tools/gittar/webcontext"
	"github.com/erda-project/erda/pkg/strutil"
	"github.com/erda-project/erda/pkg/template"
)

func CheckMergeStatus(ctx *webcontext.Context) {

	sourceBranch := ctx.Query("sourceBranch")
	targetBranch := ctx.Query("targetBranch")

	conflictInfo, err := ctx.Repository.GetMergeStatus(sourceBranch, targetBranch)

	if err != nil {
		ctx.Abort(err)
		return
	}

	ctx.Success(conflictInfo)

}

func GetMergeTemplates(ctx *webcontext.Context) {
	branch, err := ctx.Repository.GetDefaultBranch()
	if err != nil {
		ctx.Abort(err)
		return
	}
	commit, err := ctx.Repository.GetBranchCommit(branch)
	if err != nil {
		ctx.Abort(err)
		return
	}

	templateData := MergeTemplatesResponseData{
		Branch: branch,
		Names:  []string{},
	}
	entry, err := ctx.Repository.GetTreeEntryByPath(commit.ID, conf.GitMergeTemplatePath())
	//目录不存在返回空
	if err != nil {
		ctx.Success(templateData)
		return
	}

	entries, err := entry.ListEntries()
	if err != nil {
		ctx.Success(templateData)
		return
	}
	templateData.Path = conf.GitMergeTemplatePath()
	for _, entry := range entries {
		templateData.Names = append(templateData.Names, entry.Name)
	}
	ctx.Success(templateData)

}

func CreateMergeRequest(ctx *webcontext.Context) {
	// 检查仓库是否锁定
	isLocked, err := ctx.Service.GetRepoLocked(ctx.Repository.ProjectId, ctx.Repository.ApplicationId)
	if err != nil {
		ctx.Abort(err)
		return
	}
	if isLocked {
		ctx.Abort(ERROR_REPO_LOCKED)
		return
	}

	var createMergeInfo apistructs.MergeRequestInfo
	err = ctx.BindJSON(&createMergeInfo)
	if err != nil {
		ctx.Abort(err)
		return
	}
	request, err := ctx.Service.CreateMergeRequest(ctx.Repository, ctx.User, &createMergeInfo)
	if err != nil {
		ctx.Abort(err)
		return
	}
	go func() {
		rules, err := ctx.Bundle.GetAppBranchRules(uint64(ctx.Repository.ApplicationId))
		if err != nil {
			logrus.Errorf("failed to get branch rules err:%s", err)
		}
		request.TargetBranchRule = diceworkspace.GetValidBranchByGitReference(request.TargetBranch, rules)
		repo := ctx.Repository
		org, err := ctx.Bundle.GetOrg(repo.OrgId)
		if err == nil {
			request.Link = getLink(org.Domain, org.Name, repo.ProjectId, repo.ApplicationId, repo.OrgId, int64(request.RepoMergeId))
		}
		request.MergeUserId = ctx.User.Id
		request.EventName = apistructs.GitCreateMREvent
		ctx.Service.TriggerEvent(ctx.Repository, apistructs.GitCreateMREvent, request)
		// check-run
		conflictInfo, err := ctx.Repository.GetMergeStatus(request.SourceBranch, request.TargetBranch)
		if err != nil {
			ctx.Abort(err)
			return
		}
		if !conflictInfo.HasConflict {
			ctx.Service.TriggerEvent(ctx.Repository, apistructs.CheckRunEvent, request)
		}
	}()
	ctx.Success(request)
}

func GetMergeRequestDetail(ctx *webcontext.Context) {
	id := ctx.ParamInt32("id", 0)
	if id == 0 {
		ctx.Abort(ERROR_ARG_ID)
		return
	}

	var info apistructs.MergeRequestInfo
	err := ctx.BindJSON(&info)
	if err != nil {
		ctx.Abort(err)
		return
	}

	mergeRequestInfo, err := ctx.Service.GetMergeRequestDetail(ctx.Repository, id)
	if err != nil {
		ctx.Abort(err)
		return
	}
	mergeRequestInfo.CheckRuns, err = ctx.Service.QueryCheckRuns(ctx.Repository, strconv.FormatInt(int64(id), 10))
	if err != nil {
		ctx.Abort(err)
		return
	}
	ctx.Success(mergeRequestInfo, []string{
		mergeRequestInfo.AssigneeId,
		mergeRequestInfo.CloseUserId,
		mergeRequestInfo.MergeUserId,
		mergeRequestInfo.AuthorId,
	})
}

func GetMergeRequests(ctx *webcontext.Context) {
	queryCondition := &apistructs.GittarQueryMrRequest{}
	queryCondition.State = ctx.Query("state")
	queryCondition.Query = ctx.Query("query")
	queryCondition.AuthorId = ctx.Query("authorId")
	queryCondition.AssigneeId = ctx.Query("assigneeId")
	queryCondition.TargetBranch = ctx.Query("targetBranch")
	queryCondition.SourceBranch = ctx.Query("sourceBranch")
	queryCondition.Page = ctx.GetQueryInt32("pageNo", 1)
	queryCondition.Size = ctx.GetQueryInt32("pageSize", 10)
	response, err := ctx.Service.QueryMergeRequests(ctx.Repository, queryCondition)
	if err != nil {
		ctx.Abort(err)
		return
	}
	var userIDs []string
	for _, mergeRequestInfo := range response.List {
		userIDs = append(userIDs,
			mergeRequestInfo.AssigneeId,
			mergeRequestInfo.CloseUserId,
			mergeRequestInfo.MergeUserId,
			mergeRequestInfo.AuthorId,
		)
	}
	ctx.Success(response, userIDs)
}

func UpdateMergeRequest(ctx *webcontext.Context) {
	// 检查仓库是否锁定
	isLocked, err := ctx.Service.GetRepoLocked(ctx.Repository.ProjectId, ctx.Repository.ApplicationId)
	if err != nil {
		ctx.Abort(err)
		return
	}
	if isLocked {
		ctx.Abort(ERROR_REPO_LOCKED)
		return
	}

	id := ctx.ParamInt32("id", 0)
	if id == 0 {
		ctx.Abort(ERROR_ARG_ID)
		return
	}
	var mergeInfo apistructs.MergeRequestInfo
	err = ctx.BindJSON(&mergeInfo)
	if err != nil {
		ctx.Abort(err)
		return
	}

	mergeInfo.RepoMergeId = id

	result, err := ctx.Service.UpdateMergeRequest(ctx.Repository, ctx.User, &mergeInfo)
	if err != nil {
		ctx.Abort(err)
		return
	}
	go func() {
		repo := ctx.Repository
		org, err := ctx.Bundle.GetOrg(repo.OrgId)
		if err == nil {
			result.Link = getLink(org.Domain, org.Name, repo.ProjectId, repo.ApplicationId, repo.OrgId, int64(result.RepoMergeId))
		}
		// check-run
		result.MergeUserId = ctx.User.Id
		conflictInfo, err := ctx.Repository.GetMergeStatus(result.SourceBranch, result.TargetBranch)
		if err != nil {
			ctx.Abort(err)
			return
		}
		if !conflictInfo.HasConflict {
			ctx.Service.TriggerEvent(ctx.Repository, apistructs.CheckRunEvent, result)
		}
	}()
	ctx.Success(result)
}

func Merge(ctx *webcontext.Context) {
	// 检查仓库是否锁定
	isLocked, err := ctx.Service.GetRepoLocked(ctx.Repository.ProjectId, ctx.Repository.ApplicationId)
	if err != nil {
		ctx.Abort(err)
		return
	}
	if isLocked {
		ctx.Abort(ERROR_REPO_LOCKED)
		return
	}

	id := ctx.ParamInt32("id", 0)
	if id == 0 {
		ctx.Abort(ERROR_ARG_ID)
		return
	}
	mergeRequestInfo, err := ctx.Service.GetMergeRequestDetail(ctx.Repository, id)
	if err != nil {
		ctx.Abort(ERROR_ARG_ID)
		return
	}
	var mergeOptions models.MergeOptions
	err = ctx.BindJSON(&mergeOptions)
	if err != nil {
		ctx.Abort(err)
		return
	}
	commit, err := ctx.Service.Merge(ctx.Repository, ctx.User, id, &mergeOptions)
	if err != nil {
		ctx.Abort(err)
		return
	}

	pushEvent := &models.PayloadPushEvent{
		Before: mergeRequestInfo.TargetSha,
		After:  commit.ID,
		Ref:    gitmodule.BRANCH_PREFIX + mergeRequestInfo.TargetBranch,
		IsTag:  false,
		Pusher: ctx.User,
	}
	go helper.PostReceiveHook([]*models.PayloadPushEvent{pushEvent}, ctx)
	go func() {
		rules, err := ctx.Bundle.GetAppBranchRules(uint64(ctx.Repository.ApplicationId))
		if err != nil {
			logrus.Errorf("failed to get branch rules err:%s", err)
		}
		request := mergeRequestInfo
		request.TargetBranchRule = diceworkspace.GetValidBranchByGitReference(request.TargetBranch, rules)
		repo := ctx.Repository
		org, err := ctx.Bundle.GetOrg(repo.OrgId)
		if err == nil {
			request.Link = getLink(org.Domain, org.Name, repo.ProjectId, repo.ApplicationId, repo.OrgId, int64(request.RepoMergeId))
		}
		request.MergeUserId = ctx.User.Id
		request.EventName = apistructs.GitMergeMREvent
		request.AssigneeId = mergeRequestInfo.AuthorId
		ctx.Service.TriggerEvent(ctx.Repository, apistructs.GitMergeMREvent, request)
	}()
	ctx.Success(commit)
}

func CloseMR(ctx *webcontext.Context) {
	id := ctx.ParamInt32("id", 0)
	if id == 0 {
		ctx.Abort(ERROR_ARG_ID)
		return
	}
	result, err := ctx.Service.CloseMR(ctx.Repository, ctx.User, id)
	if err != nil {
		ctx.Abort(err)
		return
	}
	go func() {
		rules, err := ctx.Bundle.GetAppBranchRules(uint64(ctx.Repository.ApplicationId))
		if err != nil {
			logrus.Errorf("failed to get branch rules err:%s", err)
		}
		result.TargetBranchRule = diceworkspace.GetValidBranchByGitReference(result.TargetBranch, rules)
		repo := ctx.Repository
		org, err := ctx.Bundle.GetOrg(repo.OrgId)
		if err == nil {
			result.Link = getLink(org.Domain, org.Name, repo.ProjectId, repo.ApplicationId, repo.OrgId, int64(result.RepoMergeId))
		}
		if result.AuthorUser != nil {
			result.AuthorUser.NickName = ctx.User.NickName
		}
		result.MergeUserId = ctx.User.Id
		result.EventName = apistructs.GitCloseMREvent
		ctx.Service.TriggerEvent(ctx.Repository, apistructs.GitCloseMREvent, result)
	}()
	ctx.Success(result)
}

func ReopenMR(ctx *webcontext.Context) {
	id := ctx.ParamInt32("id", 0)
	if id == 0 {
		ctx.Abort(ERROR_ARG_ID)
		return
	}
	result, err := ctx.Service.ReopenMR(ctx.Repository, ctx.User, id)
	if err != nil {
		ctx.Abort(err)
		return
	}
	ctx.Success(result)
}

func QueryNotes(ctx *webcontext.Context) {
	id := ctx.ParamInt32("id", 0)
	if id == 0 {
		ctx.Abort(ERROR_ARG_ID)
		return
	}

	noteType := ctx.Query("type")
	if noteType == "" {
		noteType = "all"
	}

	request, err := ctx.Service.GetMergeRequestDetail(ctx.Repository, id)
	if err != nil {
		ctx.Abort(err)
		return
	}

	var result []models.Note
	if noteType == "all" {
		result, err = ctx.Service.QueryAllNotes(ctx.Repository, request.Id)
		if err != nil {
			ctx.Abort(err)
			return
		}
	} else {
		result, err = ctx.Service.QueryDiffNotes(ctx.Repository, request.Id, request.SourceSha, request.TargetSha)
		if err != nil {
			ctx.Abort(err)
			return
		}
	}
	userIDs := []string{}
	for _, note := range result {
		userIDs = append(userIDs, note.AuthorId)
	}
	ctx.Success(result, userIDs)
}

func CreateNotes(ctx *webcontext.Context) {
	id := ctx.ParamInt32("id", 0)
	if id == 0 {
		ctx.Abort(ERROR_ARG_ID)
		return
	}

	var noteRequest models.NoteRequest
	err := ctx.BindJSON(&noteRequest)
	if err != nil {
		ctx.Abort(err)
		return
	}

	if strings.TrimSpace(noteRequest.Note) == "" {
		ctx.Abort(errors.New("评论不能为空"))
		return
	}

	if noteRequest.Score < 0 || noteRequest.Score > 100 {
		ctx.Abort(errors.New("评分应在(0,100]范围内, 0为默认不打分"))
		return
	}

	mergeRequestInfo, err := ctx.Service.GetMergeRequestDetail(ctx.Repository, id)
	if err != nil {
		ctx.Abort(err)
		return
	}

	result, err := ctx.Service.CreateNote(ctx.Repository, ctx.User, mergeRequestInfo.Id, noteRequest)
	if err != nil {
		ctx.Abort(err)
		return
	}

	if noteRequest.Type == models.NoteTypeNormal && noteRequest.Score > 0 {
		mergeRequestInfo.Score += noteRequest.Score
		mergeRequestInfo.ScoreNum++
		_, err = ctx.Service.UpdateMergeRequest(ctx.Repository, ctx.User, mergeRequestInfo)
		if err != nil {
			ctx.Abort(err)
			return
		}
	}
	go func() {
		rules, err := ctx.Bundle.GetAppBranchRules(uint64(ctx.Repository.ApplicationId))
		if err != nil {
			logrus.Errorf("failed to get branch rules err:%s", err)
		}
		result, err := ctx.Service.GetMergeRequestDetail(ctx.Repository, id)
		if err != nil {
			logrus.Errorf("failed to get MR Detail err:%s", err)
		}
		result.TargetBranchRule = diceworkspace.GetValidBranchByGitReference(result.TargetBranch, rules)
		repo := ctx.Repository
		org, err := ctx.Bundle.GetOrg(repo.OrgId)
		if err == nil {
			result.Link = getLink(org.Domain, org.Name, repo.ProjectId, repo.ApplicationId, repo.OrgId, int64(id))
		}
		result.Description = noteRequest.Note
		result.AuthorUser.NickName = ctx.User.NickName
		result.MergeUserId = ctx.User.Id
		result.EventName = apistructs.GitCommentMREvent
		ctx.Service.TriggerEvent(ctx.Repository, apistructs.GitCommentMREvent, result)
	}()
	ctx.Success(result)
}

// getLink get the link of ding push
func getLink(domain, orgName string, projectId, appId, orgId, mrId int64) string {
	protocols := strutil.Split(os.Getenv(string(apistructs.DICE_PROTOCOL)), ",", true)
	protocol := "https"
	if len(protocols) > 0 {
		protocol = protocols[0]
	}
	return strutil.Concat(protocol, "://", domain, "/", orgName,
		template.Render(conf.MergePathTemplate(), map[string]string{
			"projectId": strconv.FormatInt(projectId, 10),
			"appId":     strconv.FormatInt(appId, 10),
			"orgId":     strconv.FormatInt(orgId, 10),
			"mrId":      strconv.FormatInt(mrId, 10),
		}))
}

func MergeRequestCount(ctx *webcontext.Context) {
	params := ctx.EchoContext.QueryParams()
	state := ctx.Query("state")
	appIDs, ok := params["appIDs"]
	if !ok {
		ctx.Abort(errors.New("invalid parameters"))
		return
	}
	response, err := ctx.Service.CountMergeRequests(appIDs, state)
	if err != nil {
		ctx.Abort(err)
		return
	}
	ctx.Success(response)
}

func GetMergeRequestsStats(ctx *webcontext.Context) {
	queryCondition := &apistructs.GittarQueryMrRequest{}
	queryCondition.Query = ctx.Query("query")
	queryCondition.AuthorId = ctx.Query("authorId")
	queryCondition.AssigneeId = ctx.Query("assigneeId")
	response, err := ctx.Service.QueryMergeRequestsStats(ctx.Repository, queryCondition)
	if err != nil {
		ctx.Abort(err)
		return
	}
	ctx.Success(response)
}

func OperationTempBranch(ctx *webcontext.Context) {
	id := ctx.ParamInt32("id", 0)
	if id == 0 {
		ctx.Abort(ERROR_ARG_ID)
		return
	}

	req := apistructs.GittarMergeOperationTempBranchRequest{}
	err := ctx.BindJSON(&req)
	if err != nil {
		ctx.Abort(err)
		return
	}

	err = ctx.Service.UpdateJoinTempBranchStatus(id, req.JoinTempBranchStatus, req.IsJoinTempBranch)
	if err != nil {
		ctx.Abort(err)
		return
	}
	ctx.Success(nil)
}

func MergeWithBranch(ctx *webcontext.Context) {
	isLocked, err := ctx.Service.GetRepoLocked(ctx.Repository.ProjectId, ctx.Repository.ApplicationId)
	if err != nil {
		ctx.Abort(err)
		return
	}
	if isLocked {
		ctx.Abort(ERROR_REPO_LOCKED)
		return
	}
	var req apistructs.GittarMergeWithBranchRequest
	if err = ctx.BindJSON(&req); err != nil {
		ctx.Abort(err)
		return
	}

	if req.TargetBranch == "" || req.SourceBranch == "" {
		ctx.Abort(fmt.Errorf("the targetBranch or targetBranch is empty"))
		return
	}

	commitMessage := fmt.Sprintf("Merge branch '%s' into '%s'", req.SourceBranch, req.TargetBranch)
	commit, err := ctx.Repository.Merge(req.SourceBranch, req.TargetBranch, ctx.User.ToGitSignature(), commitMessage)
	if err != nil {
		ctx.Abort(err)
		return
	}

	targetCommit, err := ctx.Repository.GetBranchCommit(req.TargetBranch)
	if err != nil {
		ctx.Abort(err)
		return
	}
	pushEvent := &models.PayloadPushEvent{
		Before: targetCommit.ID,
		After:  commit.ID,
		Ref:    gitmodule.BRANCH_PREFIX + req.TargetBranch,
		IsTag:  false,
		Pusher: ctx.User,
	}
	go helper.PostReceiveHook([]*models.PayloadPushEvent{pushEvent}, ctx)
	ctx.Success(commit)
}

func GetMergeBase(ctx *webcontext.Context) {
	sourceBranch := ctx.Query("sourceBranch")
	targetBranch := ctx.Query("targetBranch")

	ourCommit, err := ctx.Repository.GetBranchCommit(sourceBranch)
	if err != nil {
		ctx.Abort(err)
		return
	}
	theirCommit, err := ctx.Repository.GetBranchCommit(targetBranch)
	if err != nil {
		ctx.Abort(err)
		return
	}
	baseCommit, err := ctx.Repository.GetMergeBase(ourCommit, theirCommit)
	if err != nil {
		ctx.Abort(err)
		return
	}

	ctx.Success(baseCommit)

}
