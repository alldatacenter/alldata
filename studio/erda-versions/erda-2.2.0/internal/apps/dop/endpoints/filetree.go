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

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apidocsvc"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/http/httputil"
)

func (e *Endpoints) CreateNode(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.CreateNode.NotLogin().ToResp(), nil
	}
	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.CreateNode.MissingParameter("Org-ID").ToResp(), nil
	}

	treeName, ok := vars["treeName"]
	if !ok || treeName != apidocsvc.TreeNameAPIDocs {
		return apierrors.CreateNode.NotFound().ToResp(), nil
	}

	var (
		req  apistructs.APIDocCreateNodeReq
		body apistructs.APIDocCreateUpdateNodeBody
	)
	if err = json.NewDecoder(r.Body).Decode(&body); err != nil {
		return apierrors.CreateNode.InvalidParameter(err).ToResp(), nil
	}

	req.Identity = &identity
	req.OrgID = orgID
	req.Body = &body
	req.URIParams = &apistructs.FileTreeDetailURI{TreeName: apidocsvc.TreeNameAPIDocs}

	data, err2 := e.fileTreeSvc.CreateNode(ctx, &req)
	if err2 != nil {
		return err2.ToResp(), nil
	}

	return httpserver.OkResp(data)
}

func (e *Endpoints) DeleteNode(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.DeleteNode.NotLogin().ToResp(), nil
	}
	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.DeleteNode.MissingParameter("Org-ID").ToResp(), nil
	}

	if treeName, ok := vars["treeName"]; !ok || treeName != apidocsvc.TreeNameAPIDocs {
		return apierrors.DeleteNode.ToResp(), nil
	}

	var (
		uriParams = apistructs.FileTreeDetailURI{
			TreeName: apidocsvc.TreeNameAPIDocs,
			Inode:    vars["inode"],
		}
		req = apistructs.APIDocDeleteNodeReq{
			OrgID:     orgID,
			Identity:  &identity,
			URIParams: &uriParams,
		}
	)

	if err2 := e.fileTreeSvc.DeleteNode(ctx, &req); err2 != nil {
		return err2.ToResp(), nil
	}

	return httpserver.OkResp(nil)
}

func (e *Endpoints) UpdateNode(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.UpdateNode.NotLogin().ToResp(), nil
	}
	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.UpdateNode.MissingParameter("Org-ID").ToResp(), nil
	}

	treeName, ok := vars["treeName"]
	if !ok || treeName != apidocsvc.TreeNameAPIDocs {
		return apierrors.UpdateNode.NotFound().ToResp(), nil
	}

	inode, ok := vars["inode"]
	if !ok || len(inode) == 0 {
		return apierrors.UpdateNode.NotFound().ToResp(), nil
	}

	var body apistructs.RenameAPIDocBody
	if err = json.NewDecoder(r.Body).Decode(&body); err != nil {
		return apierrors.UpdateNode.InvalidParameter(err).ToResp(), nil
	}

	var (
		uri = apistructs.FileTreeDetailURI{
			TreeName: treeName,
			Inode:    inode,
		}
		req = apistructs.APIDocUpdateNodeReq{
			OrgID:     orgID,
			Identity:  &identity,
			URIParams: &uri,
			Body:      &body,
		}
	)

	data, err2 := e.fileTreeSvc.UpdateNode(&req)
	if err2 != nil {
		return err2.ToResp(), nil
	}

	return httpserver.OkResp(data)
}

func (e *Endpoints) MvCpNode(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.MoveNode.NotLogin().ToResp(), nil
	}
	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.MoveNode.MissingParameter("Org-ID").ToResp(), nil
	}

	treeName, ok := vars["treeName"]
	if !ok || treeName != apidocsvc.TreeNameAPIDocs {
		return apierrors.MoveNode.NotFound().ToResp(), nil
	}

	var body apistructs.APIDocMvCpNodeReqBody
	if err = json.NewDecoder(r.Body).Decode(&body); err != nil {
		return apierrors.MoveNode.InvalidParameter(err).ToResp(), nil
	}

	inode, ok := vars["inode"]
	if !ok || len(inode) == 0 {
		return apierrors.MoveNode.NotFound().ToResp(), nil
	}

	action := vars["action"]

	var req = apistructs.APIDocMvCpNodeReq{
		OrgID:    orgID,
		Identity: &identity,
		URIParams: &apistructs.FileTreeActionURI{
			TreeName: treeName,
			Inode:    inode,
			Action:   action,
		},
		Body: &body,
	}

	if action == "move" {
		data, err2 := e.fileTreeSvc.MoveNode(ctx, &req)
		if err2 != nil {
			return err2.ToResp(), nil
		}
		return httpserver.OkResp(data)
	}

	if action == "copy" {
		data, err2 := e.fileTreeSvc.CopyNode(ctx, &req)
		if err2 != nil {
			return err2.ToResp(), nil
		}
		return httpserver.OkResp(data)
	}

	return apierrors.MoveNode.NotFound().ToResp(), nil
}

func (e *Endpoints) ListChildrenNodes(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ListChildrenNodes.NotLogin().ToResp(), nil
	}
	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.ListChildrenNodes.NotLogin().ToResp(), nil
	}

	var params apistructs.FileTreeQueryParameters
	if err = e.queryStringDecoder.Decode(&params, r.URL.Query()); err != nil {
		return apierrors.PagingAPIAssets.InvalidParameter(err).ToResp(), nil
	}

	var req = apistructs.APIDocListChildrenReq{
		OrgID:       orgID,
		Identity:    &identity,
		URIParams:   &apistructs.FileTreeDetailURI{TreeName: vars["treeName"]},
		QueryParams: &params,
	}

	data, err2 := e.fileTreeSvc.ListChildren(&req)
	if err2 != nil {
		return err2.ToResp(), nil
	}

	return httpserver.OkResp(data)
}

func (e *Endpoints) GetNodeDetail(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.GetNodeDetail.NotLogin().ToResp(), nil
	}
	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.GetNodeDetail.MissingParameter("Org-ID").ToResp(), nil
	}

	var req = apistructs.APIDocNodeDetailReq{
		OrgID:    orgID,
		Identity: &identity,
		URIParams: &apistructs.FileTreeDetailURI{
			TreeName: vars["treeName"],
			Inode:    vars["inode"],
		},
	}

	data, err2 := e.fileTreeSvc.GetNodeDetail(&req)
	if err2 != nil {
		return err2.ToResp(), nil
	}

	return httpserver.OkResp(data)
}

func (e *Endpoints) ListGittarFileTreeNodes(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {

	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrListGittarFileTreeNodes.NotLogin().ToResp(), nil
	}

	var req apistructs.UnifiedFileTreeNodeListRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrListGittarFileTreeNodes.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo

	// 获取企业id
	orgID, err := getOrgId(r)
	if err != nil {
		return apierrors.ErrListGittarFileTreeNodes.MissingParameter("org id").ToResp(), nil
	}

	nodes, err := e.fileTree.ListFileTreeNodes(req, orgID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(nodes)
}

func (e *Endpoints) GetGittarFileTreeNode(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetGittarFileTreeNode.NotLogin().ToResp(), nil
	}

	req := apistructs.UnifiedFileTreeNodeGetRequest{
		Inode:        vars["inode"],
		IdentityInfo: identityInfo,
	}

	// 获取企业id
	orgID, err := getOrgId(r)
	if err != nil {
		return apierrors.ErrGetGittarFileTreeNode.MissingParameter("org id").ToResp(), nil
	}

	// TODO: 鉴权

	node, err := e.fileTree.GetFileTreeNode(req, orgID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(node)
}

func (e *Endpoints) GetGittarFileByPipelineId(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	pipelineId := r.URL.Query().Get("pipelineId")
	pipelineIdInt, err := strconv.Atoi(pipelineId)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrFuzzySearchGittarFileTreeNodes.NotLogin().ToResp(), nil
	}

	// 获取企业id
	orgID, err := getOrgId(r)
	if err != nil {
		return apierrors.ErrGetGittarFileTreeNode.MissingParameter("org id").ToResp(), nil
	}
	node, err := e.fileTree.GetGittarFileByPipelineId(uint64(pipelineIdInt), orgID, identity)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	return httpserver.OkResp(node)
}

func (e *Endpoints) FuzzySearchGittarFileTreeNodes(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrFuzzySearchGittarFileTreeNodes.NotLogin().ToResp(), nil
	}

	var req apistructs.UnifiedFileTreeNodeFuzzySearchRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrFuzzySearchGittarFileTreeNodes.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo

	// 获取企业id
	orgID, err := getOrgId(r)
	if err != nil {
		return apierrors.ErrGetGittarFileTreeNode.MissingParameter("org id").ToResp(), nil
	}

	// TODO: 鉴权

	nodes, err := e.fileTree.FuzzySearchFileTreeNodes(req, orgID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(nodes)
}

func getOrgId(r *http.Request) (uint64, error) {
	orgIDStr := r.Header.Get(httputil.OrgHeader)
	if orgIDStr == "" {
		return 0, apierrors.ErrListGittarFileTreeNodes.MissingParameter("org id")
	}
	orgID, err := strconv.ParseInt(orgIDStr, 10, 64)
	if err != nil {
		return 0, apierrors.ErrListGittarFileTreeNodes.InvalidParameter(err)
	}
	return uint64(orgID), nil
}

func (e *Endpoints) CreateGittarFileTreeNode(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrCreateGittarFileTreeNode.NotLogin().ToResp(), nil
	}

	// 校验 body 合法性
	if r.ContentLength == 0 {
		return apierrors.ErrCreateGittarFileTreeNode.InvalidParameter("missing request body").ToResp(), nil
	}
	var req apistructs.UnifiedFileTreeNodeCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrCreateGittarFileTreeNode.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo

	// 获取企业id
	orgID, err := getOrgId(r)
	if err != nil {
		return apierrors.ErrCreateGittarFileTreeNode.MissingParameter("org id").ToResp(), nil
	}

	// TODO: 鉴权

	unifiedNode, err := e.fileTree.CreateFileTreeNode(req, orgID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(unifiedNode)
}

func (e *Endpoints) DeleteGittarFileTreeNode(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrDeleteGittarFileTreeNode.NotLogin().ToResp(), nil
	}

	req := apistructs.UnifiedFileTreeNodeDeleteRequest{
		Inode:        vars["inode"],
		IdentityInfo: identityInfo,
	}

	// 获取企业id
	orgID, err := getOrgId(r)
	if err != nil {
		return apierrors.ErrCreateGittarFileTreeNode.MissingParameter("org id").ToResp(), nil
	}

	// TODO: 鉴权

	unifiedNode, err := e.fileTree.DeleteFileTreeNode(req, orgID, identityInfo.UserID)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(unifiedNode)
}

func (e *Endpoints) FindGittarFileTreeNodeAncestors(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrFindGittarFileTreeNodeAncestors.NotLogin().ToResp(), nil
	}

	// TODO: 鉴权

	req := apistructs.UnifiedFileTreeNodeFindAncestorsRequest{
		Inode:        vars["inode"],
		IdentityInfo: identityInfo,
	}
	ancestors, err := e.fileTree.FindFileTreeNodeAncestors(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(ancestors)
}
