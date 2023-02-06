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
	"net/url"
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/http/httputil"
)

func (e *Endpoints) ListFileTreeNodes(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {

	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrListFileTreeNodes.NotLogin().ToResp(), nil
	}

	var req apistructs.UnifiedFileTreeNodeListRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrListFileTreeNodes.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo

	// 获取企业id
	orgID, err := getPOrgId(r)
	if err != nil {
		return apierrors.ErrListFileTreeNodes.MissingParameter("org id").ToResp(), nil
	}

	nodes, err := e.pFileTree.ListFileTreeNodes(req, orgID)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	return httpserver.OkResp(nodes)
}

func (e *Endpoints) GetFileTreeNode(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetFileTreeNode.NotLogin().ToResp(), nil
	}

	req := apistructs.UnifiedFileTreeNodeGetRequest{}
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrListFileTreeNodes.InvalidParameter(err).ToResp(), nil
	}
	inode, err := url.QueryUnescape(vars["inode"])
	if err != nil {
		logrus.Errorf("QueryUnescape pipeline filetree inode %v failed", vars["inode"])
		inode = vars["inode"]
	}

	req.Inode = inode
	req.IdentityInfo = identityInfo

	// 获取企业id
	orgID, err := getPOrgId(r)
	if err != nil {
		return apierrors.ErrGetFileTreeNode.MissingParameter("org id").ToResp(), nil
	}

	// TODO: 鉴权

	node, err := e.pFileTree.GetFileTreeNode(req, orgID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(node)
}

func (e *Endpoints) FuzzySearchFileTreeNodes(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrFuzzySearchFileTreeNodes.NotLogin().ToResp(), nil
	}

	var req apistructs.UnifiedFileTreeNodeFuzzySearchRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrFuzzySearchFileTreeNodes.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo

	// 获取企业id
	orgID, err := getPOrgId(r)
	if err != nil {
		return apierrors.ErrGetFileTreeNode.MissingParameter("org id").ToResp(), nil
	}

	// TODO: 鉴权

	nodes, err := e.pFileTree.FuzzySearchFileTreeNodes(req, orgID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(nodes)
}

func (e *Endpoints) CreateFileTreeNode(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrCreateFileTreeNode.NotLogin().ToResp(), nil
	}

	// 校验 body 合法性
	if r.ContentLength == 0 {
		return apierrors.ErrCreateFileTreeNode.InvalidParameter("missing request body").ToResp(), nil
	}
	var req apistructs.UnifiedFileTreeNodeCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrCreateFileTreeNode.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo

	// 获取企业id
	orgID, err := getPOrgId(r)
	if err != nil {
		return apierrors.ErrCreateFileTreeNode.MissingParameter("org id").ToResp(), nil
	}

	// TODO: 鉴权

	unifiedNode, err := e.pFileTree.CreateFileTreeNode(req, orgID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(unifiedNode)
}

func (e *Endpoints) DeleteFileTreeNode(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrDeleteFileTreeNode.NotLogin().ToResp(), nil
	}

	inode, err := url.QueryUnescape(vars["inode"])
	if err != nil {
		logrus.Errorf("QueryUnescape pipeline filetree inode %v failed", vars["inode"])
		inode = vars["inode"]
	}

	req := apistructs.UnifiedFileTreeNodeDeleteRequest{
		Inode:        inode,
		IdentityInfo: identityInfo,
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrCreateFileTreeNode.InvalidParameter(err).ToResp(), nil
	}

	// 获取企业id
	orgID, err := getPOrgId(r)
	if err != nil {
		return apierrors.ErrDeleteFileTreeNode.MissingParameter("org id").ToResp(), nil
	}

	// TODO: 鉴权

	unifiedNode, err := e.pFileTree.DeleteFileTreeNode(req, orgID, identityInfo.UserID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(unifiedNode)
}

func (e *Endpoints) FindFileTreeNodeAncestors(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrFindFileTreeNodeAncestors.NotLogin().ToResp(), nil
	}

	inode, err := url.QueryUnescape(vars["inode"])
	if err != nil {
		logrus.Errorf("QueryUnescape pipeline filetree inode %v failed", vars["inode"])
		inode = vars["inode"]
	}

	// 获取企业id
	orgID, err := getPOrgId(r)
	if err != nil {
		return apierrors.ErrDeleteFileTreeNode.MissingParameter("org id").ToResp(), nil
	}

	// TODO: 鉴权

	req := apistructs.UnifiedFileTreeNodeFindAncestorsRequest{
		Inode:        inode,
		IdentityInfo: identityInfo,
	}

	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrListFileTreeNodes.InvalidParameter(err).ToResp(), nil
	}
	ancestors, err := e.pFileTree.FindFileTreeNodeAncestors(req, orgID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(ancestors)
}

func getPOrgId(r *http.Request) (uint64, error) {
	orgIDStr := r.Header.Get(httputil.OrgHeader)
	if orgIDStr == "" {
		return 0, apierrors.ErrListFileTreeNodes.MissingParameter("org id")
	}
	orgID, err := strconv.ParseInt(orgIDStr, 10, 64)
	if err != nil {
		return 0, apierrors.ErrListFileTreeNodes.InvalidParameter(err)
	}
	return uint64(orgID), nil
}
