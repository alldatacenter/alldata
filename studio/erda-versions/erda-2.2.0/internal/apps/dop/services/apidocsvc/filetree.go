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

package apidocsvc

import (
	"context"
	"encoding/json"
	"strconv"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
)

const (
	TreeNameAPIDocs = "api-docs"
	TreeNameSchemas = "schemas"
)

const (
	apiDocsPathFromRepoRoot    = ".dice/apidocs"
	migrationsPathFromRepoRoot = ".erda/migrations"
	suffixYml                  = ".yml"
	suffixYaml                 = ".yaml"
)

const (
	actionAdd    = "add"
	actionDelete = "delete"
)

func (svc *Service) CreateNode(ctx context.Context, req *apistructs.APIDocCreateNodeReq) (*apistructs.FileTreeNodeRspData, *errorresp.APIError) {
	if req.Body.Type != apistructs.NodeTypeFile {
		return nil, apierrors.CreateNode.InvalidParameter(svc.text(ctx, "InvalidNodeType"))
	}

	var meta apistructs.CreateAPIDocMeta
	_ = json.Unmarshal(req.Body.Meta, &meta)

	// 主流程在 createDoc 中完成
	return svc.createDoc(ctx, req.OrgID, req.Identity.UserID, req.Body.Pinode, req.Body.Name, meta.Content)
}

func (svc *Service) DeleteNode(ctx context.Context, req *apistructs.APIDocDeleteNodeReq) *errorresp.APIError {
	switch req.URIParams.TreeName {
	case TreeNameAPIDocs:
		return svc.deleteAPIDoc(ctx, req.OrgID, req.Identity.UserID, req.URIParams.Inode)
	default:
		return apierrors.DeleteNode.NotFound()
	}
}

func (svc *Service) UpdateNode(req *apistructs.APIDocUpdateNodeReq) (*apistructs.FileTreeNodeRspData, *errorresp.APIError) {
	switch req.URIParams.TreeName {
	case TreeNameAPIDocs:
		return svc.renameAPIDoc(req.OrgID, req.Identity.UserID, req.URIParams.Inode, req.Body.Name)
	default:
		return nil, apierrors.UpdateNode.NotFound()
	}
}

func (svc *Service) MoveNode(ctx context.Context, req *apistructs.APIDocMvCpNodeReq) (*apistructs.FileTreeNodeRspData, *errorresp.APIError) {
	switch req.URIParams.TreeName {
	case TreeNameAPIDocs:
		return svc.moveAPIDco(ctx, req.OrgID, req.Identity.UserID, req.URIParams.Inode, req.Body.Pinode)
	default:
		return nil, apierrors.MoveNode.NotFound()
	}
}

func (svc *Service) CopyNode(ctx context.Context, req *apistructs.APIDocMvCpNodeReq) (*apistructs.FileTreeNodeRspData, *errorresp.APIError) {
	switch req.URIParams.TreeName {
	case TreeNameAPIDocs:
		return svc.copyAPIDoc(ctx, req.OrgID, req.Identity.UserID, req.URIParams.Inode, req.Body.Pinode)
	default:
		return nil, apierrors.CopyNode.NotFound()
	}
}

// ListChildren lists all children nodes
// if the parent node is 0, it lists all branches,
// else if the TreeName is "api-docs", lists all api docs,  if the TreeName is "schemas", lists schemas.
func (svc *Service) ListChildren(req *apistructs.APIDocListChildrenReq) ([]*apistructs.FileTreeNodeRspData, *errorresp.APIError) {
	// if pinode==0, list all branches
	switch {
	case req.QueryParams.Pinode == "0" && req.QueryParams.Scope == "application":
		appID, err := strconv.ParseUint(req.QueryParams.ScopeID, 10, 64)
		if err != nil {
			return nil, apierrors.ListChildrenNodes.InvalidParameter("invalid appID")
		}
		return svc.listBranches(req.OrgID, appID, req.Identity.UserID)

	case req.QueryParams.Pinode == "0":
		return nil, apierrors.ListChildrenNodes.InvalidParameter(errors.Errorf("scope error, there is no preject-level file tree, scope: %s", req.QueryParams.Scope))
	}

	// pinode != 0, query docs or schemas
	switch req.URIParams.TreeName {
	case TreeNameAPIDocs:
		return svc.listAPIDocs(req.OrgID, req.Identity.UserID, req.QueryParams.Pinode)

	case TreeNameSchemas:
		return svc.listSchemas(req.OrgID, req.Identity.UserID, req.QueryParams.Pinode)

	default:
		return nil, apierrors.ListChildrenNodes.NotFound()
	}
}

func (svc *Service) GetNodeDetail(req *apistructs.APIDocNodeDetailReq) (*apistructs.FileTreeNodeRspData, *errorresp.APIError) {
	switch req.URIParams.TreeName {
	case TreeNameAPIDocs:
		return svc.getAPIDocContent(req.OrgID, req.Identity.UserID, req.URIParams.Inode)

	case TreeNameSchemas:
		return svc.getSchemaContent(req.OrgID, req.Identity.UserID, req.URIParams.Inode)

	default:
		return nil, apierrors.GetNodeDetail.NotFound()
	}
}
