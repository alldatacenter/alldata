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
	"fmt"
	"path"
	"path/filepath"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/bdl"
	"github.com/erda-project/erda/internal/apps/dop/dbclient"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/apps/dop/services/uc"
	"github.com/erda-project/erda/pkg/database/sqlparser/migrator"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/swagger/ddlconv"
	"github.com/erda-project/erda/pkg/swagger/oas3"
	"github.com/erda-project/erda/pkg/swagger/oasconv"
)

func (svc *Service) createDoc(ctx context.Context, orgID uint64, userID string, dstPinode, serviceName, content string) (*apistructs.FileTreeNodeRspData, *errorresp.APIError) {
	orgIDStr := strconv.FormatUint(orgID, 10)
	filenameFromRepoRoot := filepath.Join(apiDocsPathFromRepoRoot, MustSuffix(serviceName, ".yaml"))

	// pft: parentFileTree object
	pft, err := bundle.NewGittarFileTree(dstPinode)
	if err != nil || pft.ApplicationID() == "" {
		return nil, apierrors.CreateNode.InvalidParameter(errors.Wrap(err, svc.text(ctx, "InvalidParentNodeID")))
	}

	// 无需前置检查分支是否存在, 如不存在, commit 时会返回错误
	// 无需检查和创建 .dice/apidocs 目录, commit 时会自动创建

	// 检查父目录中是否已存在同名文档
	ft := pft.Clone().SetPathFromRepoRoot(filenameFromRepoRoot)
	if _, err := bdl.Bdl.GetGittarTreeNodeInfo(ft.TreePath(), orgIDStr, userID); err == nil {
		return nil, apierrors.CreateNode.InvalidParameter(svc.text(ctx, "SameNameDocAlreadlyExists"))
	}

	repo := strings.TrimPrefix(pft.RepoPath(), "/")
	message := "create api doc from API Design Center"
	if err = CommitAPIDocCreation(orgID, userID, repo, message, serviceName, content, pft.BranchName()); err != nil {
		return nil, apierrors.CreateNode.InternalError(err)
	}

	rspData := apistructs.FileTreeNodeRspData{
		Type:      "f",
		Inode:     ft.Inode(),
		Pinode:    dstPinode,
		Scope:     "application",
		ScopeID:   ft.ApplicationID(),
		Name:      MustSuffix(serviceName, ""),
		CreatorID: userID,
		UpdaterID: userID,
		Meta:      nil,
	}

	return &rspData, nil
}

func (svc *Service) deleteAPIDoc(ctx context.Context, orgID uint64, userID, dstInode string) *errorresp.APIError {
	// todo: 鉴权 谁能删

	// 查询这个 gittar 路径对应的节点
	ft, err := bundle.NewGittarFileTree(dstInode)
	if err != nil {
		return apierrors.DeleteNode.InvalidParameter(err)
	}
	nodeInfo, err := bdl.Bdl.GetGittarTreeNodeInfo(ft.TreePath(), strconv.FormatUint(orgID, 10), userID)
	if err != nil {
		logrus.Errorf("查询文档节点失败, ft: %+v, err: %v", ft, err)
		return apierrors.DeleteNode.InternalError(errors.Wrap(err, svc.text(ctx, "FailedToFindDocNod")))
	}
	if nodeInfo.Type != "blob" {
		return apierrors.DeleteNode.InvalidParameter(svc.text(ctx, "CanNotDeleteDir"))
	}

	// 正在编辑的文档不可删除
	var (
		lock  apistructs.APIDocLockModel
		where = map[string]interface{}{
			"application_id": ft.ApplicationID(),
			"branch_name":    ft.BranchName(),
			"doc_name":       filepath.Base(ft.PathFromRepoRoot()),
		}
		timeNow = time.Now()
	)
	if record := dbclient.Sq().Where(where).
		Where("is_locked = true AND expired_at > ?", timeNow).
		First(&lock); record.RowsAffected > 0 {
		return apierrors.DeleteNode.InternalError(errors.New(svc.text(ctx, "CanNotNotDeleteDocWhileEditing")))
	}

	// 删除这个文件
	var commitRequest = apistructs.GittarCreateCommitRequest{
		Message: svc.text(ctx, "DeleteFileFromAPIDesign", nodeInfo.Path),
		Actions: []apistructs.EditActionItem{{
			Action:   "delete",
			Content:  "",
			Path:     ft.PathFromRepoRoot(),
			PathType: nodeInfo.Type,
		}},
		Branch: ft.BranchName(),
	}

	resp, err := bdl.Bdl.CreateGittarCommitV3(orgID, userID, ft.RepoPath(), &commitRequest)
	if err != nil {
		return apierrors.DeleteNode.InternalError(err)
	}
	if !resp.Success {
		return apierrors.DeleteNode.InternalError(errors.Errorf("%+v", resp.Error))
	}

	return nil
}

func (svc *Service) renameAPIDoc(orgID uint64, userID, dstInode, docName string) (*apistructs.FileTreeNodeRspData, *errorresp.APIError) {
	ft, err := bundle.NewGittarFileTree(dstInode)
	if err != nil {
		return nil, apierrors.UpdateNode.InvalidParameter("不合法的 inode")
	}

	// 编辑中的文档不能重命名
	var (
		lock  apistructs.APIDocLockModel
		where = map[string]interface{}{
			"application_id": ft.ApplicationID(),
			"branch_name":    ft.BranchName(),
			"doc_name":       filepath.Base(ft.PathFromRepoRoot()),
		}
		timeNow = time.Now()
	)
	if record := dbclient.Sq().Where(where).
		Where("is_locked = true AND expired_at > ?", timeNow).
		First(&lock); record.RowsAffected > 0 {
		return nil, apierrors.DeleteNode.InternalError(errors.New("文档正在编辑中, 不能重命名"))
	}

	// 检查 inode 是否是用户自己构造出来的 (不允许指向别的目录)
	if filepath.Dir(ft.PathFromRepoRoot()) != apiDocsPathFromRepoRoot {
		return nil, apierrors.UpdateNode.InternalError(errors.New("不合法的 inode"))
	}

	docName = MustSuffix(docName, suffixYaml)
	if docName == filepath.Base(ft.PathFromRepoRoot()) {
		return nil, apierrors.UpdateNode.InvalidParameter(errors.New("重命名的文档名与原文档名一致"))
	}

	// 目标名称不能与目录下文件名同名
	newPath := filepath.Join(apiDocsPathFromRepoRoot, docName)
	nft := ft.Clone().SetPathFromRepoRoot(newPath) // nft: newFileTree
	if _, err := bdl.Bdl.GetGittarTreeNodeInfo(nft.TreePath(), strconv.FormatUint(orgID, 10), userID); err == nil {
		return nil, apierrors.CreateNode.InvalidParameter("目标名称与其他节点重名")
	}

	// 查询文档内容
	contentRsp, err2 := svc.getAPIDocContent(orgID, userID, dstInode)
	if err2 != nil {
		return nil, err2
	}
	if contentRsp.Meta == nil {
		return nil, apierrors.UpdateNode.InternalError(errors.New("未查询到原文档内容"))
	}

	var apiDocMeta apistructs.APIDocMeta
	if err = json.Unmarshal(contentRsp.Meta, &apiDocMeta); err != nil {
		return nil, apierrors.UpdateNode.InternalError(errors.Wrap(err, "未查询到原文档内容"))
	}
	if apiDocMeta.Blob == nil {
		return nil, apierrors.UpdateNode.InternalError(errors.New("未查询到原文档内容, blob is nil"))
	}

	// 提交修改
	var commit = apistructs.GittarCreateCommitRequest{
		Message: fmt.Sprintf("文档 %s 重命名为 %s", ft.PathFromRepoRoot(), docName),
		Actions: []apistructs.EditActionItem{{
			Action:   "add",
			Content:  apiDocMeta.Blob.Content,
			Path:     newPath,
			PathType: "blob",
		}, {
			Action:   "delete",
			Content:  "",
			Path:     ft.PathFromRepoRoot(),
			PathType: "blob",
		}},
		Branch: ft.BranchName(),
	}

	resp, err := bdl.Bdl.CreateGittarCommitV3(orgID, userID, ft.RepoPath(), &commit)
	if err != nil {
		return nil, apierrors.UpdateNode.InternalError(err)
	}
	if !resp.Success {
		return nil, apierrors.UpdateNode.InternalError(errors.Errorf("%+v", resp.Error))
	}

	data := &apistructs.FileTreeNodeRspData{
		Type:      "f",
		Inode:     nft.Inode(),
		Pinode:    nft.Clone().DeletePathFromRepoRoot().Inode(),
		Scope:     "application",
		ScopeID:   nft.ApplicationID(),
		Name:      MustSuffix(docName, ""),
		CreatorID: "",
		UpdaterID: "",
		Meta:      nil,
	}
	return data, nil
}

func (svc *Service) moveAPIDco(ctx context.Context, orgID uint64, userID, srcInode, dstPinode string) (*apistructs.FileTreeNodeRspData, *errorresp.APIError) {
	ft, err := bundle.NewGittarFileTree(srcInode)
	if err != nil {
		return nil, apierrors.MoveNode.InvalidParameter(errors.New("不合法的 inode"))
	}

	// 正在编辑中的文档不能移动
	var lock apistructs.APIDocLockModel
	if err = dbclient.Sq().First(&lock, map[string]interface{}{
		"project_id":     ft.ProjectID(),
		"application_id": ft.ApplicationID(),
		"branch_name":    ft.BranchName(),
		"doc_name":       filepath.Base(ft.PathFromRepoRoot()),
	}).Error; err == nil {
		return nil, apierrors.DeleteNode.InternalError(errors.New("文档正在编辑中, 不能删除"))
	}

	// 拷贝文档到目标分支
	data, err2 := svc.copyAPIDoc(ctx, orgID, userID, srcInode, dstPinode)
	if err2 != nil {
		return nil, err2
	}

	// 在本分支中删除文档
	if err2 := svc.deleteAPIDoc(ctx, orgID, userID, srcInode); err2 != nil {
		return nil, err2
	}

	return data, nil
}

func (svc *Service) copyAPIDoc(ctx context.Context, orgID uint64, userID, srcInode, dstPinode string) (*apistructs.FileTreeNodeRspData, *errorresp.APIError) {
	// 查找文档内容
	ft, err := bundle.NewGittarFileTree(srcInode)
	if err != nil {
		return nil, apierrors.CopyNode.InvalidParameter("不合法的 inode")
	}
	if dstPinode == ft.Clone().DeletePathFromRepoRoot().Inode() {
		return nil, apierrors.CopyNode.InvalidParameter("目标分支不得与源分支相同")
	}
	blob, err := bdl.Bdl.GetGittarBlobNodeInfo(ft.BlobPath(), strconv.FormatUint(orgID, 10), userID)
	if err != nil {
		return nil, apierrors.CopyNode.InternalError(err)
	}

	// 将文档内容写入目标路径
	data, err2 := svc.createDoc(ctx, orgID, userID, dstPinode, filepath.Base(blob.Path), blob.Content)
	if err2 != nil {
		return nil, err2
	}

	return data, nil
}

// listBranches lists all branches as nodes
func (svc *Service) listBranches(orgID, appID uint64, userID string) ([]*apistructs.FileTreeNodeRspData, *errorresp.APIError) {
	// query branches by appID
	// there is no project-level file tree, so "pinode != 0" is not discussed
	appIDStr := strconv.FormatUint(appID, 10)
	branches, err := svc.branchRuleSvc.GetAllValidBranchWorkspaces(int64(appID), userID)
	if err != nil {
		return nil, apierrors.ListChildrenNodes.InternalError(errors.Wrap(err, "failed to GetAllValidBranchWorkspace"))
	}
	// query application data and make a tree node
	app, err := bdl.Bdl.GetApp(appID)
	if err != nil {
		return nil, apierrors.ListChildrenNodes.InternalError(errors.Wrap(err, "failed to GetApp"))
	}
	proIDStr := strconv.FormatUint(app.ProjectID, 10)
	ft, _ := bundle.NewGittarFileTree("")
	ft.SetProjectIDName(proIDStr, app.ProjectName)
	ft.SetApplicationIDName(appIDStr, app.Name)

	var (
		results      []*apistructs.FileTreeNodeRspData
		isManager, _ = bdl.IsManager(userID, apistructs.AppScope, appID)
	)

	for _, branch := range branches {
		readOnly := !isManager && branch.IsProtect
		meta, _ := json.Marshal(map[string]bool{"readOnly": readOnly})
		data := &apistructs.FileTreeNodeRspData{
			Type:      "d",
			Inode:     ft.Clone().SetBranchName(branch.Name).Inode(),
			Pinode:    ft.Inode(),
			Scope:     "application",
			ScopeID:   appIDStr,
			Name:      branch.Name,
			CreatorID: "",
			UpdaterID: "",
			Meta:      meta,
		}
		results = append(results, data)
	}

	sort.Slice(results, func(i, j int) bool {
		return results[i].Name < results[j].Name
	})

	return results, nil
}

// listAPIDocs lists all api docs names as nodes
func (svc *Service) listAPIDocs(orgID uint64, userID, pinode string) ([]*apistructs.FileTreeNodeRspData, *errorresp.APIError) {
	return svc.listServices(orgID, userID, pinode, apiDocsPathFromRepoRoot, func(node apistructs.TreeEntry) bool {
		return node.Type == "blob" && MatchSuffix(node.Name, suffixYaml, suffixYml)
	})
}

// listSchemas lists all module's migration schemas names as nodes
func (svc *Service) listSchemas(orgID uint64, userID, pinode string) ([]*apistructs.FileTreeNodeRspData, *errorresp.APIError) {
	return svc.listServices(orgID, userID, pinode, migrationsPathFromRepoRoot, func(node apistructs.TreeEntry) bool {
		return node.Type == "tree"
	})
}

// listServices lists services names as nodes.
// pathFromRepoRoot the path to read from repo root,
// filter the condition to filter services.
func (svc *Service) listServices(orgID uint64, userID, pinode, pathFromRepoRoot string, filter func(node apistructs.TreeEntry) bool) ([]*apistructs.FileTreeNodeRspData, *errorresp.APIError) {
	ft, err := bundle.NewGittarFileTree(pinode)
	if err != nil {
		return nil, apierrors.ListChildrenNodes.InvalidParameter(err)
	}
	ft.SetPathFromRepoRoot(pathFromRepoRoot)

	appID, err := strconv.ParseUint(ft.ApplicationID(), 10, 64)
	if err != nil {
		return nil, apierrors.ListChildrenNodes.InvalidParameter(err)
	}

	orgIDStr := strconv.FormatUint(orgID, 10)

	// query the docs under the path,
	// if the error is not nil, it is considered that there is no document here.
	treeData, err := bdl.Bdl.GetGittarTreeNode(ft.TreePath(), orgIDStr, true, userID)
	if err != nil {
		logrus.Errorf("failed to GetGittarTreeNode, err: %v", err)
	}
	if len(treeData.Entries) == 0 {
		return nil, nil
	}

	// is the doc readonly
	isManager, _ := bdl.IsManager(userID, apistructs.AppScope, appID)
	readOnly := !isManager && isBranchProtected(ft.ApplicationID(), ft.BranchName(), svc.branchRuleSvc)
	meta, _ := json.Marshal(map[string]bool{"readOnly": readOnly})

	var (
		results   []*apistructs.FileTreeNodeRspData
		nodeTypeM = map[string]apistructs.NodeType{"blob": "f", "tree": "d"}
	)
	for _, node := range treeData.Entries {
		if !filter(node) {
			continue
		}
		// cft: childFileTree
		cft := ft.Clone().SetPathFromRepoRoot(filepath.Join(ft.PathFromRepoRoot(), node.Name))
		results = append(results, &apistructs.FileTreeNodeRspData{
			Type:      nodeTypeM[node.Type],
			Inode:     cft.Inode(),
			Pinode:    pinode,
			Scope:     "application",
			ScopeID:   cft.ApplicationID(),
			Name:      MustSuffix(node.Name, ""),
			CreatorID: "",
			UpdaterID: "",
			Meta:      meta,
		})
	}

	return results, nil
}

func (svc *Service) getAPIDocContent(orgID uint64, userID, inode string) (*apistructs.FileTreeNodeRspData, *errorresp.APIError) {
	data, apiError := FetchAPIDocContent(orgID, userID, inode, oasconv.OAS3JSON, svc.branchRuleSvc)
	if apiError != nil {
		return nil, apiError
	}

	var meta apistructs.APIDocMeta
	if err := json.Unmarshal(data.Meta, &meta); err != nil {
		return nil, apierrors.GetNodeDetail.InternalError(err)
	}

	if meta.Blob == nil {
		marshal, _ := json.Marshal(meta)
		data.Meta = marshal
		return data, nil
	}

	// 查询文档锁状态
	meta.Lock = &apistructs.APIDocMetaLock{Locked: false}
	if ft, err := bundle.NewGittarFileTree(inode); err == nil {
		var lock apistructs.APIDocLockModel
		var where = map[string]interface{}{
			"application_id": ft.ApplicationID(),
			"branch_name":    ft.BranchName(),
			"doc_name":       filepath.Base(ft.TreePath()),
		}
		if record := dbclient.Sq().First(&lock, where); record.RowsAffected > 0 {
			meta.Lock.Locked = true
			meta.Lock.UserID = lock.CreatorID
			if users, _ := uc.GetUsers([]string{lock.CreatorID}); len(users) > 0 {
				if user := users[lock.CreatorID]; user != nil {
					meta.Lock.NickName = user.Nick
				}
			}
		}
	}

	// 校验 oas3 格式合法性 只做基础的反序列化校验
	meta.Valid = true
	v3, err := oas3.LoadFromData([]byte(meta.Blob.Content))
	if err != nil {
		meta.Valid = false
		meta.Error = err.Error()
		marshal, _ := json.Marshal(meta)
		data.Meta = marshal
		return data, nil
	}

	// 查询集市相关信息
	meta.Asset = new(apistructs.APIDocMetaAssetInfo)
	var version apistructs.APIAssetVersionsModel
	first := dbclient.Sq().First(&version, map[string]interface{}{
		"source":       apistructs.CreateAPIAssetSourceDesignCenter,
		"service_name": data.Name,
	})
	if first.Error != nil {
		meta.Asset.Major = 1
	} else {
		meta.Asset.AssetID = version.AssetID
		meta.Asset.AssetName = version.AssetName
		_ = dbclient.GenSemVer(orgID, version.AssetID, v3.Info.Version, &meta.Asset.Major, &meta.Asset.Minor, &meta.Asset.Patch)
	}

	data.Meta, _ = json.Marshal(meta)

	return data, nil
}

func (svc *Service) getSchemaContent(orgID uint64, userID, inode string) (*apistructs.FileTreeNodeRspData, *errorresp.APIError) {
	// 解析路径
	ft, err := bundle.NewGittarFileTree(inode)
	if err != nil {
		return nil, apierrors.GetNodeDetail.InternalError(err)
	}
	serviceName := MustSuffix(filepath.Base(ft.PathFromRepoRoot()), "")
	ft.SetPathFromRepoRoot(filepath.Join(migrationsPathFromRepoRoot, serviceName))

	orgIDStr := strconv.FormatUint(orgID, 10)

	// query all files in the directory, allows error or 0 node.
	treeData, err := bdl.Bdl.GetGittarTreeNode(ft.TreePath(), orgIDStr, true, userID)
	if err != nil {
		logrus.Errorf("failed to GetGittarTreeNode, err: %v", err)
	}
	if len(treeData.Entries) == 0 {
		return nil, nil
	}

	// generate the openapi from DDLs
	var (
		openapi, _ = ddlconv.New()
		module     migrator.Module
	)
	for _, node := range treeData.Entries {
		if node.Type != "blob" || filepath.Ext(node.Name) != ".sql" {
			continue
		}

		cft := ft.Clone().SetPathFromRepoRoot(filepath.Join(ft.PathFromRepoRoot(), node.Name))
		nodeInfo, err := bdl.Bdl.GetGittarBlobNodeInfo(cft.BlobPath(), orgIDStr, userID)
		if err != nil {
			logrus.WithError(err).WithField("blobPath", cft.BlobPath()).Errorln("failed to GetGittarBlobInfo")
			continue
		}
		script, err := migrator.NewScriptFromData("", cft.PathFromRepoRoot(), []byte(nodeInfo.Content))
		if err != nil {
			return nil, apierrors.GetNodeDetail.InternalError(
				errors.Wrapf(err, "failed to read .sql file as SQL script, file path: %s", cft.PathFromRepoRoot()))
		}
		module.Scripts = append(module.Scripts, script)
	}
	module.Sort()

	for _, script := range module.Scripts {
		if _, err := openapi.Write(script.GetData()); err != nil {
			logrus.Errorf("failed to WriteString(sql) to openapi, err: %v", err)
			return nil, apierrors.GetNodeDetail.InternalError(err)
		}
	}

	meta, _ := json.Marshal(openapi.Components)
	var data = apistructs.FileTreeNodeRspData{
		Type:      "f",
		Inode:     inode,
		Pinode:    ft.Clone().DeletePathFromRepoRoot().Inode(),
		Scope:     "application",
		ScopeID:   ft.ApplicationID(),
		Name:      path.Base(ft.PathFromRepoRoot()),
		CreatorID: "",
		UpdaterID: "",
		Meta:      json.RawMessage(meta),
	}

	return &data, nil
}
