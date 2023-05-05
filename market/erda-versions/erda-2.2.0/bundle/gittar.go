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

// Package bundle 见 bundle.go
package bundle

import (
	"fmt"
	"io"
	"net/url"
	"os"
	"strconv"
	"strings"
	"time"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

// GittarLines 获取指定代码行数的参数
type GittarLines struct {
	Repo     string
	CommitID string
	Path     string
	Since    string
	To       string
}

var (
	RepoNotExist = errors.New("repo not exist.")
)

// DeleteGitRepo 从gittar删除应用gitRepo
func (b *Bundle) DeleteGitRepo(gitRepo string) error {
	host, err := b.urls.Gittar()
	if err != nil {
		return err
	}
	hc := b.hc

	resp, err := hc.Delete(host).Path("/" + gitRepo).Do().DiscardBody()
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		// TODO: should gittar return more error info?
		return apierrors.ErrInvoke.InternalError(
			errors.Errorf("failed to delete git repo, repo: %s, status-code: %d", gitRepo, resp.StatusCode()))
	}
	return nil
}

// CreateRepo 从gittar创建应用
func (b *Bundle) CreateRepo(repo apistructs.CreateRepoRequest) (*apistructs.CreateRepoResponseData, error) {
	host, err := b.urls.Gittar()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	var response apistructs.CreateRepoResponse
	resp, err := hc.Post(host).Path("/_system/repos").JSONBody(repo).Do().JSON(&response)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !response.Success {
		return nil, toAPIError(resp.StatusCode(), response.Error)
	}
	return &response.Data, nil
}

// UpdateRepo 更新仓库配置
func (b *Bundle) UpdateRepo(request apistructs.UpdateRepoRequest) error {
	host, err := b.urls.Gittar()
	if err != nil {
		return err
	}
	hc := b.hc

	var response apistructs.UpdateRepoResponse
	resp, err := hc.Put(host).Path(fmt.Sprintf("/_system/apps/%d", request.AppID)).JSONBody(request).Do().JSON(&response)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !response.Success {
		return toAPIError(resp.StatusCode(), response.Error)
	}
	return nil
}

// DeleteRepo 从gittar删除应用gitRepo 新接口
func (b *Bundle) DeleteRepo(appID int64) error {
	host, err := b.urls.Gittar()
	if err != nil {
		return err
	}
	hc := b.hc

	var response apistructs.DeleteRepoResponse
	resp, err := hc.Delete(host).Path("/_system/apps/" + strconv.FormatInt(appID, 10)).Do().JSON(&response)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !response.Success {
		return toAPIError(resp.StatusCode(), response.Error)
	}
	return nil
}

// GetGittarLines 从gittar获取指定区间的代码行数
func (b *Bundle) GetGittarLines(lines *GittarLines, userName, passWord string) ([]string, error) {
	var (
		host string
		err  error
	)

	hc := b.hc
	hc.BasicAuth(userName, passWord)
	host, err = b.urls.Gittar()
	if err != nil {
		return nil, err
	}

	if lines.Repo == "" {
		return nil, apierrors.ErrInvoke.InvalidState("nil gittar repo")
	}

	URL, err := url.Parse(lines.Repo)
	if err != nil {
		return nil, apierrors.ErrInvoke.InvalidState(
			fmt.Sprintf("failed to parse gittar repo, repo: %s, (%+v)", lines.Repo, err))
	}

	linesResp := apistructs.GittarLinesResponse{}
	reqPath := fmt.Sprintf("%s/blob-range/%s/%s", URL.Path, lines.CommitID, lines.Path)

	resp, err := hc.Get(host).Path(reqPath).Param("since", lines.Since).Param("to", lines.To).Do().JSON(&linesResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !linesResp.Success {
		return nil, toAPIError(resp.StatusCode(), linesResp.Error)
	}

	return linesResp.Data.Lines, nil
}

// GetGittarFile 从gittar获取指定文件内容
func (b *Bundle) GetGittarFile(repoUrl, ref, filePath, userName, passWord, userID string) (string, error) {
	var (
		host string
		err  error
	)

	hc := b.hc
	hc.BasicAuth(userName, passWord)
	host, err = b.urls.Gittar()
	if err != nil {
		return "", err
	}

	if ref == "" {
		return "", apierrors.ErrInvoke.InvalidState("nil ref")
	}

	if filePath == "" {
		return "", apierrors.ErrInvoke.InvalidState("nil file path")
	}

	URL, err := url.Parse(repoUrl)
	if err != nil {
		return "", apierrors.ErrInvoke.InvalidState(
			fmt.Sprintf("failed to parse gittar repo, repo: %s, (%+v)", repoUrl, err))
	}

	fileResp := apistructs.GittarFileResponse{}
	reqPath := fmt.Sprintf("%s/blob/%s/%s", URL.Path, ref, filePath)

	resp, err := hc.Get(host).
		Header(httputil.UserHeader, userID).
		Path(reqPath).Do().JSON(&fileResp)
	if err != nil {
		return "", apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !fileResp.Success {
		return "", toAPIError(resp.StatusCode(), fileResp.Error)
	}

	return fileResp.Data.Content, nil
}

func (b *Bundle) GetGittarHost() (string, error) {
	host, err := b.urls.Gittar()
	if err != nil {
		return "", err
	}

	return host, nil
}

// RegisterGittarHook  向 gittar 注册 webhook
func (b *Bundle) RegisterGittarHook(r apistructs.GittarRegisterHookRequest) error {
	host, err := b.urls.Gittar()
	if err != nil {
		return err
	}
	hc := b.hc

	var response apistructs.GittarRegisterHookResponse
	resp, err := hc.Post(host).Path("/_system/hooks").
		Header("Accept", "application/json").
		JSONBody(&r).
		Do().JSON(&response)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return apierrors.ErrInvoke.InternalError(
			errors.Errorf("failed to create gittar webhook, status-code: %d, register-url: %s",
				resp.StatusCode(), r.URL))
	}
	if !response.Success {
		return apierrors.ErrInvoke.InternalError(
			errors.Errorf("failed to create gittar webhook: %+v, register-url: %s", response.Error, r.URL))
	}

	return nil
}

// CreateGittarCommit 创建commit
func (b *Bundle) CreateGittarCommit(repo string, request apistructs.GittarCreateCommitRequest, userID string) (*apistructs.GittarCreateCommitResponse, error) {
	hc := b.hc
	host, err := b.urls.Gittar()
	if err != nil {
		return nil, err
	}
	var createCommitResp apistructs.GittarCreateCommitResponse
	resp, err := hc.Get(host).
		Header(httputil.UserHeader, userID).
		Path("/" + repo + "/commits").JSONBody(request).
		Do().JSON(&createCommitResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !createCommitResp.Success {
		return nil, toAPIError(resp.StatusCode(), createCommitResp.Error)
	}
	return &createCommitResp, nil
}

// CreateGittarCommitV2 创建commit
func (b *Bundle) CreateGittarCommitV2(repo string, request apistructs.GittarCreateCommitRequest, orgID int, userID string) (*apistructs.GittarCreateCommitResponse, error) {
	hc := b.hc
	host, err := b.urls.Gittar()
	if err != nil {
		return nil, err
	}
	var createCommitResp apistructs.GittarCreateCommitResponse
	resp, err := hc.Post(host).
		Path("/"+repo+"/commits").
		Header(httputil.UserHeader, userID).
		Header("Org-ID", strconv.Itoa(orgID)).
		JSONBody(request).
		Do().JSON(&createCommitResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !createCommitResp.Success {
		return nil, toAPIError(resp.StatusCode(), createCommitResp.Error)
	}
	return &createCommitResp, nil
}

// GetGittarCommit 获取commit 历史信息
// pageNo==pageSize==1
func (b *Bundle) GetGittarCommit(repo, ref, userID string) (*apistructs.Commit, error) {
	var (
		host   string
		err    error
		commit apistructs.GittarCommitsListResponse
	)
	hc := b.hc
	host, err = b.urls.Gittar()
	if err != nil {
		return nil, err
	}

	resp, err := hc.Get(host).
		Path("/"+repo+"/commits/"+ref).
		Header(httputil.UserHeader, userID).
		Param("pageNo", "1").
		Param("pageSize", "1").
		Do().JSON(&commit)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !commit.Success {
		return nil, toAPIError(resp.StatusCode(), commit.Error)
	}

	if len(commit.Data) == 0 {
		return nil, errors.New("no commit found")
	}
	return &commit.Data[0], nil
}

func (b *Bundle) ListGittarCommit(repo, ref, userID string, orgID string) (*apistructs.Commit, error) {
	var (
		host   string
		err    error
		commit apistructs.GittarCommitsListResponse
	)
	hc := b.hc
	host, err = b.urls.Gittar()
	if err != nil {
		return nil, err
	}

	resp, err := hc.Get(host).
		Path(repo+"/commits/"+ref).
		Header(httputil.UserHeader, userID).
		Header(httputil.OrgHeader, orgID).
		Param("pageNo", "1").
		Param("pageSize", "1").
		Do().JSON(&commit)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !commit.Success {
		return nil, toAPIError(resp.StatusCode(), commit.Error)
	}

	if len(commit.Data) == 0 {
		return nil, errors.New("no commit found")
	}
	return &commit.Data[0], nil
}

// GetGittarBranches 获取指定应用的所有分支
func (b *Bundle) GetGittarBranches(repo, userID string) ([]string, error) {
	var (
		host       string
		err        error
		branchResp apistructs.GittarBranchesResponse
	)
	hc := b.hc
	host, err = b.urls.Gittar()
	if err != nil {
		return nil, err
	}

	resp, err := hc.Get(host).
		Path(repo+"/branches").
		Header(httputil.UserHeader, userID).
		Do().JSON(&branchResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !branchResp.Success {
		return nil, toAPIError(resp.StatusCode(), branchResp.Error)
	}

	var branches []string
	for _, branch := range branchResp.Data {
		branches = append(branches, branch.Name)
	}
	return branches, nil
}

// GetGittarBranchesV2 获取指定应用的所有分支
func (b *Bundle) GetGittarBranchesV2(repo string, orgID string, onlyBranchNames bool, userID string) ([]string, error) {
	var (
		host       string
		err        error
		branchResp apistructs.GittarBranchesResponse
	)
	hc := b.hc
	host, err = b.urls.Gittar()
	if err != nil {
		return nil, err
	}

	resp, err := hc.Get(host).
		Path(repo+"/branches").
		Header("Org-ID", orgID).
		Header(httputil.UserHeader, userID).
		Param("onlyBranchNames", strconv.FormatBool(onlyBranchNames)).
		Do().JSON(&branchResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !branchResp.Success {
		return nil, toAPIError(resp.StatusCode(), branchResp.Error)
	}

	var branches []string
	for _, branch := range branchResp.Data {
		branches = append(branches, branch.Name)
	}
	return branches, nil
}

func (b *Bundle) GetGittarBranchDetail(repo, orgID, branch, userID string) (*apistructs.BranchDetail, error) {
	var (
		host       string
		err        error
		branchResp apistructs.GittarBranchDetailResponse
	)
	hc := b.hc
	host, err = b.urls.Gittar()
	if err != nil {
		return nil, err
	}

	resp, err := hc.Get(host).
		Path(repo+"/branches/"+branch).
		Header("Org-ID", orgID).
		Header(httputil.UserHeader, userID).
		Do().JSON(&branchResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !branchResp.Success {
		return nil, toAPIError(resp.StatusCode(), branchResp.Error)
	}
	return branchResp.Data, nil
}

func (b *Bundle) DeleteGittarBranch(repo, orgID, branch, userID string) error {
	var (
		host       string
		err        error
		branchResp apistructs.GittarDeleteBranchResponse
	)
	hc := b.hc
	host, err = b.urls.Gittar()
	if err != nil {
		return err
	}

	resp, err := hc.Delete(host).
		Path(repo+"/branches/"+branch).
		Header("Org-ID", orgID).
		Header(httputil.UserHeader, userID).
		Do().JSON(&branchResp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !branchResp.Success {
		return toAPIError(resp.StatusCode(), branchResp.Error)
	}
	return nil
}

// GetGittarBranchesV2 获取指定应用的所有分支
func (b *Bundle) CreateGittarBranch(repo string, branchInfo apistructs.GittarCreateBranchRequest, orgID string, userID string) error {
	var (
		host       string
		err        error
		createResp apistructs.GittarCreateBranchResponse
	)
	hc := b.hc
	host, err = b.urls.Gittar()
	if err != nil {
		return err
	}

	resp, err := hc.Post(host).
		Path(repo+"/branches").
		Header(httputil.OrgHeader, orgID).
		Header(httputil.UserHeader, userID).
		JSONBody(branchInfo).
		Do().JSON(&createResp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !createResp.Success {
		return toAPIError(resp.StatusCode(), createResp.Error)
	}

	return nil
}

// GetGittarTreeNode 获取目录的子节点
func (b *Bundle) GetGittarTreeNode(repo string, orgID string, simple bool, userID string) (*apistructs.GittarTreeData, error) {
	var (
		host     string
		err      error
		treeResp apistructs.GittarTreeResponse
	)
	hc := b.hc
	host, err = b.urls.Gittar()
	if err != nil {
		return nil, err
	}

	resp, err := hc.Get(host).
		Header("Org-ID", orgID).
		Header(httputil.UserHeader, userID).
		Path(repo).
		Param("simple", strconv.FormatBool(simple)).
		Do().JSON(&treeResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !treeResp.Success {
		return nil, toAPIError(resp.StatusCode(), treeResp.Error)
	}

	return &treeResp.Data, nil
}

// 获取文件的内容
func (b *Bundle) GetGittarBlobNode(repo, orgID, userID string) (string, error) {
	var (
		host     string
		err      error
		blobResp apistructs.GittarBlobResponse
	)
	hc := b.hc
	host, err = b.urls.Gittar()
	if err != nil {
		return "", err
	}

	resp, err := hc.Get(host).
		Header("Org-ID", orgID).
		Header(httputil.UserHeader, userID).
		Path(repo).
		Do().JSON(&blobResp)
	if err != nil {
		return "", apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !blobResp.Success {
		return "", toAPIError(resp.StatusCode(), blobResp.Error)
	}

	return blobResp.Data.Content, nil
}

// GetGittarTree 获取目录内容
func (b *Bundle) GetGittarTree(repo, orgID, userID string) (*apistructs.GittarTreeData, error) {
	var treeResp apistructs.GittarTreeResponse

	hc := b.hc
	host, err := b.urls.Gittar()
	if err != nil {
		return nil, err
	}

	resp, err := hc.Get(host).
		Header("Org-ID", orgID).
		Header(httputil.UserHeader, userID).
		Path(repo).
		Do().JSON(&treeResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !treeResp.Success {
		return nil, toAPIError(resp.StatusCode(), treeResp.Error)
	}

	return &treeResp.Data, nil
}

// GetGittarTags 获取指定应用的所有Tag
func (b *Bundle) GetGittarTags(repo, userID string) ([]string, error) {
	var (
		host    string
		err     error
		tagResp apistructs.GittarTagsResponse
	)
	hc := b.hc
	host, err = b.urls.Gittar()
	if err != nil {
		return nil, err
	}

	resp, err := hc.Get(host).
		Header(httputil.UserHeader, userID).
		Path("/" + repo + "/tags").
		Do().JSON(&tagResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !tagResp.Success {
		return nil, toAPIError(resp.StatusCode(), tagResp.Error)
	}

	var tags []string
	for _, tag := range tagResp.Data {
		tags = append(tags, tag.Name)
	}
	return tags, nil
}

// GetGittarStats 获取仓库概览信息
func (b *Bundle) GetGittarStats(appID int64, userID string) (*apistructs.GittarStatsData, error) {
	var (
		host          string
		err           error
		statsResponse apistructs.GittarStatsResponse
	)
	hc := b.hc
	host, err = b.urls.Gittar()
	if err != nil {
		return nil, err
	}

	resp, err := hc.Get(host).
		Header(httputil.UserHeader, userID).
		Path(fmt.Sprintf("/app-repo/%d/stats/", appID)).
		Do().JSON(&statsResponse)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !statsResponse.Success {
		return nil, toAPIError(resp.StatusCode(), statsResponse.Error)
	}

	return &statsResponse.Data, nil
}

func (b *Bundle) CreateCheckRun(appID int64, request apistructs.CheckRun, userID string) (*apistructs.CheckRun, error) {
	var (
		host     string
		err      error
		response apistructs.CreateCheckRunResponse
	)
	hc := b.hc
	host, err = b.urls.Gittar()
	if err != nil {
		return nil, err
	}

	resp, err := hc.Post(host).
		Header(httputil.UserHeader, userID).
		Path(fmt.Sprintf("/app-repo/%d/check-runs", appID)).JSONBody(request).
		Do().JSON(&response)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !response.Success {
		return nil, toAPIError(resp.StatusCode(), response.Error)
	}

	return response.Data, nil
}

// SearchGittarFiles 获取仓库概览信息
func (b *Bundle) SearchGittarFiles(appID int64, ref string, pattern string, basePath string, depth int64, userID string) ([]*apistructs.TreeEntry, error) {
	var (
		host               string
		err                error
		treeSearchResponse apistructs.GittarTreeSearchResponse
	)
	hc := b.hc
	host, err = b.urls.Gittar()
	if err != nil {
		return nil, err
	}

	resp, err := hc.Get(host).
		Path(fmt.Sprintf("/app-repo/%d/tree-search", appID)).
		Header(httputil.UserHeader, userID).
		Param("ref", ref).
		Param("pattern", pattern).
		Param("basePath", basePath).
		Param("depth", strconv.FormatInt(depth, 10)).
		Do().JSON(&treeSearchResponse)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !treeSearchResponse.Success {
		return nil, toAPIError(resp.StatusCode(), treeSearchResponse.Error)
	}

	return treeSearchResponse.Data, nil
}

// CloseMergeRequest 关闭mr
func (b *Bundle) CloseMergeRequest(appID int64, mrID int, userID string) error {
	var (
		host string
		err  error
	)
	hc := b.hc
	host, err = b.urls.Gittar()
	if err != nil {
		return err
	}

	resp, err := hc.Get(host).
		Header(httputil.UserHeader, userID).
		Path(fmt.Sprintf("/app-repo/%d/merge-request/%d/close", appID, mrID)).
		Do().DiscardBody()
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return apierrors.ErrInvoke.InternalError(
			errors.Errorf("failed to close Mr"))
	}
	return nil
}

// OperationTempBranch operation mr temp branch
func (b *Bundle) OperationTempBranch(appID uint64, userID string, req apistructs.GittarMergeOperationTempBranchRequest) error {
	var (
		host string
		err  error
		rsp  apistructs.GittarMergeOperationTempBranchResponse
	)
	hc := b.hc
	host, err = b.urls.Gittar()
	if err != nil {
		return err
	}

	resp, err := hc.Post(host).
		Header(httputil.UserHeader, userID).
		Path(fmt.Sprintf("/app-repo/%v/merge-requests/%v/operation-temp-branch", appID, req.MergeID)).
		JSONBody(req).
		Do().JSON(&rsp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}

	if !resp.IsOK() {
		return apierrors.ErrInvoke.InternalError(errors.Errorf("failed to operationTempBranch"))
	}
	return nil
}

func (b *Bundle) CreateMergeRequest(appID uint64, userID string, req apistructs.GittarCreateMergeRequest) (*apistructs.MergeRequestInfo, error) {
	var (
		host string
		err  error
		rsp  apistructs.GittarCreateMergeResponse
	)
	hc := b.hc
	host, err = b.urls.Gittar()
	if err != nil {
		return nil, err
	}

	resp, err := hc.Post(host).
		Header(httputil.UserHeader, userID).
		Path(fmt.Sprintf("/app-repo/%d/merge-requests", appID)).
		JSONBody(req).
		Do().JSON(&rsp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return nil, apierrors.ErrInvoke.InternalError(errors.Errorf("failed to list Mr"))
	}
	return rsp.Data, nil
}

// ListMergeRequest list mrs
func (b *Bundle) ListMergeRequest(appID uint64, userID string, req apistructs.GittarQueryMrRequest) (*apistructs.QueryMergeRequestsData, error) {
	var (
		host string
		err  error
		rsp  apistructs.GittarQueryMrResponse
	)
	hc := b.hc
	host, err = b.urls.Gittar()
	if err != nil {
		return nil, err
	}

	resp, err := hc.Get(host).
		Header(httputil.UserHeader, userID).
		Path(fmt.Sprintf("/app-repo/%d/merge-requests", appID)).
		Param("state", req.State).
		Param("targetBranch", req.TargetBranch).
		Param("sourceBranch", req.SourceBranch).
		Param("pageNo", strconv.FormatInt(int64(req.Page), 10)).
		Param("pageSize", strconv.FormatInt(int64(req.Size), 10)).
		Do().JSON(&rsp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return nil, apierrors.ErrInvoke.InternalError(errors.Errorf("failed to list Mr"))
	}
	return &rsp.Data, nil
}

// GetMergeRequestDetail get mr derails
func (b *Bundle) GetMergeRequestDetail(appID uint64, userID string, mrID uint64) (*apistructs.MergeRequestInfo, error) {
	var (
		host string
		err  error
		rsp  apistructs.GittarQueryMrDetailResponse
	)
	hc := b.hc
	host, err = b.urls.Gittar()
	if err != nil {
		return nil, err
	}

	resp, err := hc.Get(host).
		Header(httputil.UserHeader, userID).
		Path(fmt.Sprintf("/app-repo/%d/merge-requests/%v", appID, mrID)).
		Do().JSON(&rsp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return nil, apierrors.ErrInvoke.InternalError(errors.Errorf("failed to get Mr"))
	}
	return &rsp.Data, nil
}

// GetGittarCompare gittar compare between commits
func (b *Bundle) GetGittarCompare(after, before string, appID int64, userID string) (*apistructs.GittarCompareData, error) {
	var (
		host            string
		err             error
		compareResponse apistructs.GittarCompareResponse
	)
	hc := b.hc
	host, err = b.urls.Gittar()
	if err != nil {
		return nil, err
	}

	resp, err := hc.Get(host).
		Header(httputil.UserHeader, userID).
		Path(fmt.Sprintf("/app-repo/%d/compare/%s", appID, fmt.Sprintf("%s...%s", after, before))).
		Do().JSON(&compareResponse)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !compareResponse.Success {
		return nil, toAPIError(resp.StatusCode(), compareResponse.Error)
	}

	return &compareResponse.Data, nil
}

func (b *Bundle) MergeRequestCount(userID string, req apistructs.MergeRequestCountRequest) (map[string]int, error) {
	var (
		host string
		err  error
		rsp  apistructs.MergeRequestCountResponse
	)
	hc := b.hc
	host, err = b.urls.Gittar()
	if err != nil {
		return nil, err
	}

	values := url.Values{}
	for _, i := range req.AppIDs {
		values.Add("appIDs", strconv.FormatUint(i, 10))
	}

	resp, err := hc.Get(host).
		Header(httputil.UserHeader, userID).
		Param("state", req.State).
		Path(fmt.Sprintf("/api/merge-requests-count")).
		Params(values).
		Do().JSON(&rsp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return nil, apierrors.ErrInvoke.InternalError(errors.Errorf("failed to list merge request count"))
	}
	return rsp.Data, nil
}

func (b *Bundle) GetArchive(userID string, req apistructs.GittarArchiveRequest, distDir string) (string, error) {
	host, err := b.urls.Gittar()
	if err != nil {
		return "", err
	}
	hc := b.hc

	path := fmt.Sprintf("/%s/dop/%s/%s/archive/%s.zip", req.Org, req.Project, req.Application, req.Ref)
	resp, err := hc.Get(host).Path(path).
		Header(httputil.UserHeader, userID).
		Do().RAW()
	if err != nil {
		return "", apierrors.ErrInvoke.InternalError(err)
	}
	defer resp.Body.Close()

	if resp.StatusCode/100 != 2 {
		if resp.StatusCode == 404 {
			return "", RepoNotExist
		}
		return "", fmt.Errorf("archive %s response %d", req.Application, resp.StatusCode)
	}

	zipfile := fmt.Sprintf("tmp-%d.zip", time.Now().Unix())
	attachmentInfo := resp.Header.Get("Content-Disposition")
	attachment := strings.Split(attachmentInfo, "=")
	if len(attachment) == 2 {
		zipfile = attachment[1]
	}

	f, err := os.Create(distDir + "/" + zipfile)
	if err != nil {
		return "", err
	}
	defer f.Close()

	_, err = io.Copy(f, resp.Body)
	if err != nil {
		return zipfile, err
	}

	return zipfile, nil
}

func (b *Bundle) MergeWithBranch(userID string, req apistructs.GittarMergeWithBranchRequest) (*apistructs.Commit, error) {
	var (
		host string
		err  error
		rsp  apistructs.MergeWithBranchResponse
	)
	hc := b.hc
	host, err = b.urls.Gittar()
	if err != nil {
		return nil, err
	}

	path := fmt.Sprintf("/app-repo/%d/merge-with-branch", req.AppID)
	resp, err := hc.Post(host).
		Header(httputil.UserHeader, userID).
		Path(path).
		JSONBody(req).
		Do().JSON(&rsp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	if !resp.IsOK() {
		return nil, apierrors.ErrInvoke.InternalError(errors.Errorf(rsp.Header.Error.Msg))
	}
	return rsp.Data, nil
}

func (b *Bundle) GetMergeBase(userID string, req apistructs.GittarMergeBaseRequest) (*apistructs.Commit, error) {
	var (
		host string
		err  error
		rsp  apistructs.MergeBaseResponse
	)
	hc := b.hc
	host, err = b.urls.Gittar()
	if err != nil {
		return nil, err
	}

	path := fmt.Sprintf("/app-repo/%d/merge-base", req.AppID)
	resp, err := hc.Get(host).
		Header(httputil.UserHeader, userID).
		Path(path).
		Param("sourceBranch", req.SourceBranch).
		Param("targetBranch", req.TargetBranch).
		Do().JSON(&rsp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	if !resp.IsOK() {
		return nil, apierrors.ErrInvoke.InternalError(errors.Errorf("failed to merge base"))
	}
	return rsp.Data, nil
}
