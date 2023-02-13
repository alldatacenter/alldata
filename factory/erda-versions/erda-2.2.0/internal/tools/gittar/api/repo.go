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
	"context"
	"errors"
	"fmt"
	"io"
	"io/ioutil"
	"net/http"
	"regexp"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/gittar/conf"
	"github.com/erda-project/erda/internal/tools/gittar/helper"
	"github.com/erda-project/erda/internal/tools/gittar/models"
	"github.com/erda-project/erda/internal/tools/gittar/pkg/gitmodule"
	"github.com/erda-project/erda/internal/tools/gittar/pkg/gitmodule/tool"
	"github.com/erda-project/erda/internal/tools/gittar/pkg/util"
	"github.com/erda-project/erda/internal/tools/gittar/webcontext"
	"github.com/erda-project/erda/pkg/strutil"
)

func isTextType(contentType string) bool {
	return strings.Contains(contentType, "text/")
}

// CreateRepo function
func CreateRepo(context *webcontext.Context) {
	request := &apistructs.CreateRepoRequest{}
	err := context.BindJSON(&request)
	if err != nil {
		context.AbortWithStatus(400, errors.New("request body parse failed"))
		return
	}
	if request.OrgName == "" {
		context.AbortWithStatus(400, errors.New("org_name is empty"))
		return
	}

	if request.ProjectName == "" {
		context.AbortWithStatus(400, errors.New("project_name is empty"))
		return
	}

	if request.AppName == "" {
		context.AbortWithStatus(400, errors.New("app_name is empty"))
		return
	}

	repo, err := context.Service.CreateRepo(request)
	logrus.Infof("create gitRepo org:%s project:%s app:%s", request.OrgName, request.ProjectName, request.AppName)
	if err != nil {
		context.Abort(err)
		return
	}
	context.Success(CreateRepoResponseData{
		ID:       repo.ID,
		RepoPath: repo.Path,
	})
}

// GetRepoBranches function
func GetRepoBranches(context *webcontext.Context) {
	onlyBranchNames := context.GetQueryBool("onlyBranchNames", false)
	findBranch := context.Query("findBranch")
	repository := context.Repository
	branches, err := context.Repository.GetDetailBranches(onlyBranchNames, findBranch)
	if err != nil {
		logrus.Errorf("repo:%v branch error %v", repository.DiskPath(), err)
		context.Abort(errors.New("branch error"))
		return
	}
	b := gitmodule.Branches(branches)
	sort.Sort(b)
	context.Success(b)
}

// SetRepoDefaultBranch 设置默认分支
func SetRepoDefaultBranch(context *webcontext.Context) {
	repository := context.Repository
	branch := context.Param("*")
	err := repository.SetDefaultBranch(branch)
	if err != nil {
		context.Abort(err)
	} else {
		context.Success("")
	}
}

// CreateRepoBranch 创建分支
func CreateRepoBranch(context *webcontext.Context) {
	repository := context.Repository
	// 检查仓库是否锁定
	isLocked, err := context.Service.GetRepoLocked(repository.ProjectId, repository.ApplicationId)
	if err != nil {
		context.Abort(err)
		return
	}
	if isLocked {
		context.Abort(ERROR_REPO_LOCKED)
		return
	}

	request := CreateBranchRequest{}
	err = context.BindJSON(&request)
	if err != nil {
		context.Abort(err)
		return
	}

	err = context.CheckBranchOperatePermission(context.User, request.Name)
	if err != nil {
		context.Abort(err)
		return
	}

	commit, err := repository.CreateBranch(request.Name, request.Ref)
	if err != nil {
		context.Abort(err)
		return
	}
	pushEvent := &models.PayloadPushEvent{
		IsTag:    false,
		Ref:      gitmodule.BRANCH_PREFIX + request.Name,
		After:    commit.ID,
		Before:   gitmodule.INIT_COMMIT_ID,
		IsDelete: false,
		Pusher:   context.User,
	}
	go helper.PostReceiveHook([]*models.PayloadPushEvent{pushEvent}, context)
	context.Success("")
}

// GetRepoBranchDetail .
func GetRepoBranchDetail(context *webcontext.Context) {
	ref := context.Param("*")
	commit, err := context.Repository.GetBranchCommit(ref)
	if err != nil {
		context.Abort(err)
		return
	}
	context.Success(Map{
		"commit": commit,
	})
}

func getRepoLink(domain, orgName string, projectID, appID int64) string {
	protocols := strutil.Split(conf.DiceProtocol(), ",", true)
	protocol := "https"
	if len(protocols) > 0 {
		protocol = protocols[0]
	}
	return fmt.Sprintf("%s://%s/%s/dop/projects/%d/apps/%d/repo", protocol, domain, orgName, projectID, appID)
}

// DeleteRepoBranch 删除分支
func DeleteRepoBranch(context *webcontext.Context) {
	// 检查仓库是否锁定
	isLocked, err := context.Service.GetRepoLocked(context.Repository.ProjectId, context.Repository.ApplicationId)
	if err != nil {
		context.Abort(err)
		return
	}
	if isLocked {
		context.Abort(ERROR_REPO_LOCKED)
		return
	}

	branch := context.Param("*")
	err = context.CheckBranchOperatePermission(context.User, branch)
	if err != nil {
		context.Abort(err)
		return
	}
	commit, err := context.Repository.GetCommitByAny(branch)
	if err != nil {
		context.Abort(err)
		return
	}

	err = context.Repository.DeleteBranch(branch)
	if err != nil {
		context.Abort(err)
		return
	}

	pushEvent := &models.PayloadPushEvent{
		IsTag:    false,
		Ref:      gitmodule.BRANCH_PREFIX + branch,
		After:    gitmodule.INIT_COMMIT_ID,
		Before:   commit.ID,
		IsDelete: false,
		Pusher:   context.User,
	}
	go helper.PostReceiveHook([]*models.PayloadPushEvent{pushEvent}, context)

	go func() {
		repo := context.Repository
		result := apistructs.BranchInfo{
			Name:         branch,
			OperatorID:   context.User.Id,
			OperatorName: context.User.NickName,
			EventName:    apistructs.GitDeleteBranchEvent,
		}
		org, err := context.Bundle.GetOrg(repo.OrgId)
		if err == nil {
			result.Link = getRepoLink(org.Domain, org.Name, repo.ProjectId, repo.ApplicationId)
		}
		context.Service.TriggerEvent(context.Repository, apistructs.GitDeleteBranchEvent, &result)
	}()
	response := apistructs.DeleteEvent{
		AppName:   context.Repository.Path,
		Name:      branch,
		Event:     apistructs.DeleteBranchTemplate,
		AppID:     context.Repository.ApplicationId,
		ProjectID: context.Repository.ProjectId,
	}
	context.Success(response)
}

// GetRepoTags function
func GetRepoTags(context *webcontext.Context) {
	repository := context.Repository
	findTags := context.Query("findTags")
	tags, err := repository.GetDetailTags(findTags)
	if err != nil {
		logrus.Errorf("repo:%v branch error %v", repository.DiskPath(), err)
		context.Abort(errors.New("tags error"))
	} else {
		context.Success(tags)
	}
}

// CreateRepoTag 创建tag
func CreateRepoTag(context *webcontext.Context) {
	if err := context.CheckPermission(models.PermissionCreateTAG); err != nil {
		context.Abort(err)
		return
	}

	repository := context.Repository
	request := CreateTagRequest{}
	err := context.BindJSON(&request)
	if err != nil {
		context.Abort(err)
		return
	}
	err = repository.CreateTag(request.Name, request.Ref, context.User.ToGitSignature(), request.Message)
	if err != nil {
		context.Abort(err)
		return
	}
	context.Success("")
}

// DeleteRepoTag 删除tag
func DeleteRepoTag(context *webcontext.Context) {
	if err := context.CheckPermission(models.PermissionDeleteTAG); err != nil {
		context.Abort(models.NO_PERMISSION_ERROR)
		return
	}
	tag := context.Param("*")
	repository := context.Repository

	tags, err := repository.GetDetailTags(tag)
	if err != nil {
		context.Abort(err)
		return
	}
	var result apistructs.TagInfo
	for _, each := range tags {
		if each.Name == tag {
			result.Message = each.Message
			result.Object = each.Object
			result.ID = each.ID
		}
	}

	err = repository.DeleteTag(tag)
	if err != nil {
		context.Abort(err)
		return
	}
	go func() {
		repo := context.Repository
		result.Name = tag
		result.Link = ""
		result.OperatorName = context.User.NickName
		result.OperatorID = context.User.Id
		result.EventName = apistructs.GitDeleteTagEvent
		org, err := context.Bundle.GetOrg(repo.OrgId)
		if err == nil {
			result.Link = getRepoLink(org.Domain, org.Name, repo.ProjectId, repo.ApplicationId)
		}
		context.Service.TriggerEvent(context.Repository, apistructs.GitDeleteTagEvent, &result)
	}()
	response := apistructs.DeleteEvent{
		AppName:   context.Repository.Path,
		Name:      tag,
		Event:     apistructs.DeleteTagTemplate,
		AppID:     context.Repository.ApplicationId,
		ProjectID: context.Repository.ProjectId,
	}
	context.Success(response)
}

// GetRepoCommits function
func GetRepoCommits(context *webcontext.Context) {
	path := context.Param("*")
	err := context.Repository.ParseRefAndTreePath(path)
	if err != nil {
		context.Abort(err)
		return
	}

	page := context.GetQueryInt32("pageNo", 1)
	size := context.GetQueryInt32("pageSize", 10)
	search := context.Query("search")
	commitID := context.Repository.Commit.ID
	treePath := context.Repository.TreePath
	commits, err := context.Repository.CommitsByFileAndRangeSize(
		commitID,
		treePath,
		search,
		page, size)

	if err != nil {
		context.Abort(err)
	} else {
		context.Success(commits)
	}
}

func Commit(ctx *webcontext.Context) {
	sha1 := ctx.Param("sha")
	newCommit, err := ctx.Repository.GetCommit(sha1)
	if err != nil {
		ctx.AbortWithStatus(404, err)
		return
	}

	var oldCommit *gitmodule.Commit
	if len(newCommit.Parents) == 0 {
		oldCommit = nil
	} else {
		oldCommit, err = ctx.Repository.GetCommit(newCommit.Parents[0])
		if err != nil {
			ctx.AbortWithStatus(404, err)
			return
		}
	}

	diff, err := ctx.Repository.GetDiff(newCommit, oldCommit)
	if err != nil {
		ctx.AbortWithStatus(404, err)
		return
	}
	ctx.Success(Map{
		"diff":   diff,
		"commit": newCommit,
	})
}

func DiffFile(ctx *webcontext.Context) {
	oldRef := ctx.Query("old_ref")
	newRef := ctx.Query("new_ref")
	path := ctx.Query("path")

	commitTo, err := ctx.Repository.GetCommitByAny(oldRef)
	if err != nil {
		ctx.Abort(err)
		return
	}
	commitFrom, err := ctx.Repository.GetCommitByAny(newRef)
	if err != nil {
		ctx.Abort(err)
		return
	}

	diffFile, err := ctx.Repository.GetDiffFile(commitFrom, commitTo, path, path)

	if err != nil {
		ctx.Abort(err)
		return
	}
	ctx.Success(diffFile)

}

// Compare 比较
func Compare(ctx *webcontext.Context) {
	path := ctx.Param("*")
	path = strings.Trim(path, "/")
	logrus.Infof("compare path %v", path)

	skip := ctx.GetQueryInt32("skip", 0)
	limit := ctx.GetQueryInt32("limit", 20)

	commitEnd := "/commits"
	isCommit := false
	if strings.HasSuffix(path, commitEnd) {
		isCommit = true
		path = path[0 : len(path)-len(commitEnd)]
	}

	commits := strings.Split(path, "...")

	commitTo, err := ctx.Repository.GetCommitByAny(commits[1])
	if err != nil {
		ctx.AbortWithStatus(404, err)
		return
	}

	var commitFrom *gitmodule.Commit
	matchEmpty, _ := regexp.MatchString("^0+$", commits[0])
	if matchEmpty {
		commitFrom = nil
	} else {
		commitFrom, err = ctx.Repository.GetCommitByAny(commits[0])
		if err != nil {
			ctx.AbortWithStatus(404, err)
			return
		}
	}

	baseCommit, err := ctx.Repository.GetMergeBase(commitFrom, commitTo)
	if err != nil {
		ctx.AbortWithStatus(500, err)
		return
	}

	var diff *gitmodule.Diff
	if !isCommit {
		diff, err = ctx.Repository.GetDiff(commitFrom, baseCommit)
		if err != nil {
			ctx.AbortWithStatus(500, err)
			return
		}
	}

	betweenCommits, err := ctx.Repository.CommitsBetweenLimit(commitFrom, baseCommit, skip, limit)

	commitsCount, err := ctx.Repository.CommitsCountBetween(commitFrom, baseCommit)
	if err != nil {
		ctx.AbortWithStatus(500, err)
		return
	}

	ctx.Success(Map{
		"commits":      betweenCommits,
		"from":         commits[0],
		"to":           commits[1],
		"commitsCount": commitsCount,
		"diff":         diff,
	})
}

func GetRepoBlobRange(context *webcontext.Context) {
	repository := context.Repository
	path := context.Param("*")
	err := repository.ParseRefAndTreePath(path)
	if err != nil {
		context.Abort(err)
		return
	}

	treeEntry, err := repository.GetParsedTreeEntry()
	if err != nil {
		context.AbortWithStatus(404, err)
		return
	}
	if treeEntry.IsDir() {
		context.AbortWithStatus(404, errors.New("path not a file"))
		return
	}
	dataRc, err := treeEntry.Blob().Data()
	if err != nil {
		context.Abort(err)
	}
	buf := make([]byte, 1024)
	n, err := dataRc.Read(buf)
	if err != nil {
		context.Abort(err)
		return
	}
	buf = buf[:n]
	contentType := http.DetectContentType(buf)
	isTextFile := isTextType(contentType)

	treePath := repository.TreePath

	if !isTextFile {
		context.AbortWithString(500, "not text file")
		return
	}
	blobData := struct {
		Binary bool     `json:"binary"`
		Lines  []string `json:"lines"`
		Path   string   `json:"path"`
	}{
		Binary: !isTextFile,
		Lines:  []string{},
		Path:   treePath,
	}

	since := context.GetQueryInt32("since", 1)
	to := context.GetQueryInt32("to", 10)
	if isTextFile {
		d, err := ioutil.ReadAll(dataRc)
		if err != nil {
			context.Abort(err)
			return
		}
		buf = append(buf, d...)
		allTextLines := strings.Split(string(buf), "\n")

		lineCount := len(allTextLines)
		if lineCount < to {
			to = lineCount
		}

		for i := since; i <= to; i++ {
			line := allTextLines[i-1]
			blobData.Lines = append(blobData.Lines, line)
		}
	}
	context.Success(blobData)
}

// GetRepoBlob function
func GetRepoBlob(context *webcontext.Context) {
	repository := context.Repository
	path := context.Param("*")
	err := repository.ParseRefAndTreePath(path)
	if err != nil {
		context.Abort(err)
		return
	}

	treeEntry, err := repository.GetParsedTreeEntry()
	if err != nil {
		context.AbortWithStatus(404, err)
		return
	}
	if treeEntry.IsDir() {
		context.AbortWithStatus(404, errors.New("path not a file"))
		return
	}
	dataRc, err := treeEntry.Blob().Data()
	if err != nil {
		context.Abort(err)
	}
	buf := make([]byte, 1024)
	n, _ := dataRc.Read(buf)
	buf = buf[:n]
	contentType := http.DetectContentType(buf)
	isTextFile := isTextType(contentType)

	refName := repository.RefName
	if refName == "" {
		refName = "sha/" + repository.Commit.ID
	}
	treePath := repository.TreePath

	//range 模式
	if context.Query("mode") == "range" {
		if !isTextFile {
			context.AbortWithString(500, "not text file")
		}
		blobData := struct {
			Binary  bool        `json:"binary"`
			Lines   interface{} `json:"lines"`
			RefName string      `json:"refName"`
			Path    string      `json:"path"`
		}{
			Binary:  !isTextFile,
			Lines:   "",
			Path:    treePath,
			RefName: refName,
		}

		bottom := context.GetQueryBool("bottom", false)
		unfold := context.GetQueryBool("unfold", false)
		offset := context.GetQueryInt32("offset", 0)

		since := context.GetQueryInt32("since", 1)
		to := context.GetQueryInt32("to", 10)
		if isTextFile {
			d, _ := ioutil.ReadAll(dataRc)
			buf = append(buf, d...)
			line := fmt.Sprintf("%s,%s", strconv.Itoa(since), strconv.Itoa(to))
			matchLine := fmt.Sprintf("@@ %s+%s @@", line, line)
			allTextLines := strings.Split(string(buf), "\n")

			lineCount := len(allTextLines)
			if lineCount < to {
				to = lineCount
			}
			result := []gitmodule.DiffLine{}

			//底部展开 或者 显示声明unfold=false.不添加diff header
			if unfold && !bottom {
				result = append(result, gitmodule.DiffLine{
					Content:   matchLine,
					Type:      gitmodule.DIFF_LINE_SECTION,
					NewLineNo: -1,
					OldLineNo: -1,
				})
			}

			for i := since; i <= to; i++ {
				line := allTextLines[i-1]
				result = append(result, gitmodule.DiffLine{
					Content:   line,
					Type:      gitmodule.DIFF_LINE_CONTEXT,
					NewLineNo: i,
					OldLineNo: i - offset,
				})
			}

			//底部展开条,有额外内容才显示
			if bottom && lineCount > to {
				result = append(result, gitmodule.DiffLine{
					Content:   "",
					Type:      gitmodule.DIFF_LINE_SECTION,
					NewLineNo: -1,
					OldLineNo: -1,
				})
			}

			blobData.Lines = result
		}
		context.Success(blobData)
	} else {
		blobData := struct {
			Binary  bool   `json:"binary"`
			Content string `json:"content"`
			RefName string `json:"refName"`
			Path    string `json:"path"`
			Size    int64  `json:"size"`
		}{
			Binary:  !isTextFile,
			Content: "",
			Path:    treePath,
			RefName: refName,
			Size:    treeEntry.Size(),
		}
		if isTextFile {
			d, _ := ioutil.ReadAll(dataRc)
			buf = append(buf, d...)

			blobData.Content = string(buf)
		}
		context.Success(blobData)
	}
}

// CreateCommit 创建Commit
func CreateCommit(context *webcontext.Context) {
	util.HandleRequest(context.HttpRequest())

	if err := context.CheckPermission(models.PermissionPush); err != nil {
		context.Abort(err)
		return
	}

	repository := context.Repository
	isLocked, err := context.Service.GetRepoLocked(repository.ProjectId, repository.ApplicationId)
	if err != nil {
		context.Abort(err)
		return
	}
	if isLocked {
		context.Abort(ERROR_REPO_LOCKED)
		return
	}

	var createCommitRequest gitmodule.CreateCommit
	if err = context.BindJSON(&createCommitRequest); err != nil {
		context.Abort(err)
		return
	}

	if err = createCommitRequest.Validate(); err != nil {
		context.Abort(err)
		return
	}

	if err := context.CheckBranchOperatePermission(context.User, createCommitRequest.Branch); err != nil {
		context.Abort(err)
		return
	}

	beforeCommitID := gitmodule.INIT_COMMIT_ID
	beforeCommit, err := context.Repository.GetBranchCommit(createCommitRequest.Branch)
	if err == nil {
		beforeCommitID = beforeCommit.ID
	}
	createCommitRequest.Signature = context.User.ToGitSignature()

	commit, err := repository.CreateCommit(&createCommitRequest, context.EtcdClient)
	if err != nil {
		context.Abort(err)
		return
	}

	pushEvent := &models.PayloadPushEvent{
		Before: beforeCommitID,
		After:  commit.ID,
		Ref:    gitmodule.BRANCH_PREFIX + createCommitRequest.Branch,
		IsTag:  false,
		Pusher: context.MustGet("user").(*models.User),
	}
	go helper.PostReceiveHook([]*models.PayloadPushEvent{pushEvent}, context)

	context.Success(Map{
		"commit": commit,
	})
}

// GetRepoRaw function
func GetRepoRaw(context *webcontext.Context) {
	repository := context.Repository
	path := context.Param("*")
	err := repository.ParseRefAndTreePath(path)
	if err != nil {
		context.Abort(err)
		return
	}

	treeEntry, err := repository.GetParsedTreeEntry()
	if err != nil {
		context.AbortWithStatus(404, ERROR_PATH_NOT_FOUND)
		return
	}
	if treeEntry.IsDir() {
		context.AbortWithStatus(404, ERROR_NOT_FILE)
		return
	}
	dataRc, err := treeEntry.Blob().Data()

	buf := make([]byte, 1024)
	n, _ := dataRc.Read(buf)
	bufHead := buf[:n]
	contentType := http.DetectContentType(bufHead)

	if err != nil {
		context.AbortWithStatus(404, ERROR_PATH_NOT_FOUND)
		return
	}
	// 防止html渲染，强制转为text/plain
	contentType = strings.Replace(contentType, "text/html", "text/plain", -1)
	context.Header("Content-Type", contentType)
	context.GetWriter().Write(bufHead)
	io.Copy(context.GetWriter(), dataRc)
	context.Status(http.StatusOK)
}

// BlameFile function
func BlameFile(context *webcontext.Context) {
	repository := context.Repository
	path := context.Param("*")
	err := repository.ParseRefAndTreePath(path)
	if err != nil {
		context.Abort(err)
		return
	}

	treeEntry, err := repository.GetParsedTreeEntry()
	if err != nil {
		context.AbortWithStatus(404, ERROR_PATH_NOT_FOUND)
		return
	}
	if treeEntry.IsDir() {
		context.AbortWithStatus(404, ERROR_NOT_FILE)
		return
	}
	blames, err := repository.BlameFile(repository.Commit, repository.TreePath)
	if err != nil {
		context.Abort(err)
		return
	}
	context.Success(blames)

}

// DeleteRepo 通过repoID删除关联仓库
func DeleteRepo(context *webcontext.Context) {
	id, err := strconv.ParseInt(context.Param("id"), 10, 64)
	if err != nil {
		context.AbortWithStatus(400, ERROR_ARG_ID)
		return
	}
	repo, err := context.Service.GetRepoById(id)
	if err != nil {
		context.Abort(err)
		return
	}
	err = context.Service.DeleteRepo(repo)
	if err != nil {
		context.Abort(err)
		return
	}
	context.Success("")
}

// DeleteRepoByApp  通过appID删除关联仓库
func DeleteRepoByApp(ctx *webcontext.Context) {
	id, err := strconv.ParseInt(ctx.Param("id"), 10, 64)
	if err != nil {
		ctx.AbortWithStatus(400, ERROR_ARG_ID)
		return
	}
	repo, err := ctx.Service.GetRepoByApp(id)
	if err != nil {
		ctx.Abort(err)
		return
	}
	err = ctx.Service.DeleteRepo(repo)
	if err != nil {
		ctx.Abort(err)
		return
	}
	ctx.Success("")
}

// CreateRepo function
func UpdateRepoByApp(ctx *webcontext.Context) {
	request := &apistructs.UpdateRepoRequest{}
	err := ctx.BindJSON(&request)
	if err != nil {
		ctx.AbortWithStatus(400, errors.New("request body parse failed"))
		return
	}
	id, err := strconv.ParseInt(ctx.Param("id"), 10, 64)
	if err != nil {
		ctx.AbortWithStatus(400, ERROR_ARG_ID)
		return
	}

	repo, err := ctx.Service.GetRepoByApp(id)
	if err != nil {
		ctx.Abort(err)
		return
	}

	if repo.IsExternal {
		err := ctx.Service.UpdateRepo(repo, request)
		if err != nil {
			ctx.Abort(err)
			return
		}
	}
	ctx.Success("")
}

// GetRepoTree function
func SearchRepoTree(ctx *webcontext.Context) {
	ref := ctx.Query("ref")
	basePath := ctx.Query("basePath")
	pattern := ctx.Query("pattern")
	depthStr := ctx.Query("depth")
	if pattern == "" {
		pattern = "*"
	}
	var depth int64
	depth = conf.GitRepoTreeSearchDepth()
	commit, err := ctx.Repository.GetCommitByAny(ref)
	if err != nil {
		ctx.Abort(err)
		return
	}
	treeEntry, err := ctx.Repository.GetTreeEntryByPath(commit.ID, basePath)
	if err != nil {
		ctx.Success([]interface{}{})
		return
	}

	if depthStr != "" {
		depth, err = strconv.ParseInt(depthStr, 10, 64)
		if err != nil {
			ctx.Abort(err)
			return
		}
	}

	ctxTimeout, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	entries, err := treeEntry.PtrTree.Search(depth, pattern, ctxTimeout)
	if err != nil {
		ctx.Abort(err)
		return
	}
	ctx.Success(entries)
}

// GetRepoTree function
func GetRepoTree(context *webcontext.Context) {
	repository := context.Repository
	path := context.Param("*")

	simple := context.GetQueryBool("simple", false)

	err := repository.ParseRefAndTreePath(path)
	if err != nil {
		context.Abort(err)
		return
	}

	treeEntry, err := repository.GetParsedTreeEntry()

	if err != nil {
		context.Abort(err)
	} else {
		refName := repository.RefName
		if refName == "" {
			refName = "sha/" + repository.Commit.ID
		}

		if treeEntry.IsDir() {
			entries, err := treeEntry.PtrTree.ListEntries(context.GetQueryBool("expand", true))
			if err != nil {
				context.Abort(err)
			} else {
				readmeFile := ""
				if !simple {
					repository.FillTreeEntriesCommitInfo(repository.Commit.ID, repository.TreePath, treeEntry)
					if context.GetQueryBool("comment", false) {
						repository.FillTreeEntriesCommitInfo(repository.Commit.ID, repository.TreePath, entries...)
					}
					repository.FillSubmoduleInfo(repository.Commit.ID, entries...)
					for _, v := range entries {
						if tool.IsReadmeFile(v.Name) {
							readmeFile = v.Name
							break
						}
					}
				}

				context.Success(Map{
					"type":       gitmodule.OBJECT_TREE,
					"refName":    refName,
					"path":       repository.TreePath,
					"binary":     false,
					"entries":    entries,
					"commit":     treeEntry.Commit,
					"treeId":     treeEntry.ID,
					"readmeFile": readmeFile,
				})
			}

		} else {
			//取父目录
			pathParts := strings.Split(repository.TreePath, "/")
			dirPath := strings.Join(pathParts[:(len(pathParts)-1)], "/")

			if !simple {
				repository.FillTreeEntriesCommitInfo(repository.Commit.ID, dirPath, treeEntry)
				repository.FillSubmoduleInfo(repository.Commit.ID, treeEntry)
			}

			treeEntry.Size()
			commit := treeEntry.Commit
			treeEntry.Commit = nil

			isTextFile := true
			if !simple {
				dataRc, err := treeEntry.Blob().Data()
				if err != nil {
					context.Abort(err)
					return
				}
				buf := make([]byte, 1024)
				n, _ := dataRc.Read(buf)
				buf = buf[:n]
				isTextFile = tool.IsTextFile(buf)
			}

			context.Success(Map{
				"type":    gitmodule.OBJECT_BLOB,
				"refName": refName,
				"path":    repository.TreePath,
				"binary":  !isTextFile,
				"entry":   treeEntry,
				"treeId":  treeEntry.ID,
				"commit":  commit,
			})
		}
	}
}

// GetRepoStats function
func GetRepoStats(context *webcontext.Context) {
	repository := context.Repository
	path := context.Param("*")

	stats, err := repository.GetRepoStats(path)

	if err != nil {
		context.Abort(err)
		return
	}
	if repository.Size == 0 {
		size, err := repository.CalcRepoSize()
		if err == nil {
			repository.Size = size
			err := context.Service.UpdateRepoSizeCache(repository.ID, size)
			if err != nil {
				context.Abort(err)
				return
			}
		}
	}
	stats["innerPath"] = repository.Path
	stats["size"] = repository.Size

	stats["mergeRequestCount"], _ = context.Service.CountMR(repository, "open")
	stats["applicationID"] = repository.ApplicationId
	stats["projectID"] = repository.ProjectId
	stats["username"] = conf.GitTokenUserName()
	stats["isLocked"], err = context.Service.GetRepoLocked(repository.ProjectId, repository.ApplicationId)
	if err != nil {
		context.Abort(err)
		return
	}
	context.Success(stats)
}

// SetLocked 仓库锁定
func SetLocked(context *webcontext.Context) {
	repository := context.Repository
	id := repository.ApplicationId
	if id == 0 {
		context.Abort(ERROR_LOCKED_DENIED)
		return
	}
	var repoInfo apistructs.LockedRepoRequest
	err := context.BindJSON(&repoInfo)
	if err != nil {
		context.Abort(err)
		return
	}
	repoInfo.AppID = id
	result, err := context.Service.SetLocked(context.Repository, context.User, &repoInfo)
	if err != nil {
		context.Abort(err)
		return
	}
	result.AppName = context.Repository.Path
	result.AppID = context.Repository.ApplicationId
	result.ProjectID = context.Repository.ProjectId
	context.Success(result)
}

// GetArchive 打包下载
func GetArchive(ctx *webcontext.Context) {
	fileName := ctx.Param("*")
	supportFormats := []string{
		"tar.gz",
		"zip",
		"tar",
	}
	format := ""
	ref := ""
	for _, v := range supportFormats {
		if strings.HasSuffix(fileName, v) {
			format = v
			break
		}
	}
	if format == "" {
		ctx.Abort(errors.New("invalid format "))
		return
	}

	ref = fileName[0 : len(fileName)-len(format)-1]
	if ref == "" {
		ctx.AbortWithString(400, "invalid ref ")
		return
	}
	_, err := ctx.Repository.GetCommitByAny(ref)
	if err != nil {
		ctx.AbortWithString(404, "ref not found "+ref)
		return
	}
	helper.RunArchive(ctx, ref, format)
}
