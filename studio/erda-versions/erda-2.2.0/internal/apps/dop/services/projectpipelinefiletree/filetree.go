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

package projectpipelinefiletree

import (
	"encoding/json"
	"fmt"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/apps/dop/services/autotest"
	"github.com/erda-project/erda/internal/apps/dop/services/filetree"
	"github.com/erda-project/erda/pkg/strutil"
)

// Pipeline pipeline 结构体
type FileTree struct {
	bdl               *bundle.Bundle
	gittarFileTreeSvc *filetree.GittarFileTree
	autoTestSvc       *autotest.Service
}

// Option Pipeline 配置选项
type Option func(*FileTree)

// New Pipeline service
func New(options ...Option) *FileTree {
	r := &FileTree{}
	for _, op := range options {
		op(r)
	}
	return r
}

// WithBundle 配置 bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(f *FileTree) {
		f.bdl = bdl
	}
}

func WithFileTreeSvc(svc *filetree.GittarFileTree) Option {
	return func(f *FileTree) {
		f.gittarFileTreeSvc = svc
	}
}

func WithAutoTestSvc(svc *autotest.Service) Option {
	return func(f *FileTree) {
		f.autoTestSvc = svc
	}
}

func (svc *FileTree) ListFileTreeNodes(req apistructs.UnifiedFileTreeNodeListRequest, orgID uint64) ([]*apistructs.UnifiedFileTreeNode, error) {

	var treeNodes []*apistructs.UnifiedFileTreeNode

	if err := strutil.Validate(req.ScopeID, strutil.MinLenValidator(1)); err != nil {
		return nil, fmt.Errorf("invalid scopeID: %v", err)
	}

	// scope 为空代表查询所有的目录名称
	if req.Scope == "" {
		for _, scope := range apistructs.AllScope {
			treeNodes = append(treeNodes, scopeConvertToUnifiedFileTreeNode(scope, req.ScopeID))
		}
		return treeNodes, nil
	}

	if err := req.BasicValidate(); err != nil {
		return nil, apierrors.ErrGetFileTreeNode.InvalidParameter(err)
	}

	return svc.listFileTreeNodes(req, orgID)
}

func (svc *FileTree) listFileTreeNodes(req apistructs.UnifiedFileTreeNodeListRequest, orgID uint64) ([]*apistructs.UnifiedFileTreeNode, error) {
	switch req.Scope {
	case apistructs.FileTreeScopeProjectApp:
		return svc.gittarFileTreeSvc.ListFileTreeNodes(req, orgID)
	case apistructs.FileTreeScopeProject, apistructs.FileTreeScopeAutoTestPlan, apistructs.FileTreeScopeAutoTestConfigSheet, apistructs.FileTreeScopeAutoTest:
		nodes, err := svc.autoTestSvc.ListFileTreeNodes(req)
		if err != nil {
			return nil, err
		}
		treeNodes := make([]*apistructs.UnifiedFileTreeNode, 0, len(nodes))
		for i := range nodes {
			treeNodes = append(treeNodes, &nodes[i])
		}
		return treeNodes, nil
	}

	return nil, fmt.Errorf("not find this scope %s", req.Scope)
}

func (svc *FileTree) GetFileTreeNode(req apistructs.UnifiedFileTreeNodeGetRequest, orgID uint64) (*apistructs.UnifiedFileTreeNode, error) {
	// 参数校验
	if err := req.BasicValidate(); err != nil {
		return nil, apierrors.ErrGetFileTreeNode.InvalidParameter(err)
	}
	result, err := svc.getFileTreeNode(req, orgID)
	if err != nil {
		return nil, err
	}

	// 设置上snippet_source标识
	if result != nil {
		snippetConfigMapJson, err := json.Marshal(result.Meta[apistructs.AutoTestFileTreeNodeMetaKeySnippetAction])
		if err != nil {
			return result, nil
		}
		snippetAction := apistructs.PipelineYmlAction{}
		err = json.Unmarshal(snippetConfigMapJson, &snippetAction)
		if err != nil {
			return result, nil
		}

		snippetAction.SnippetConfig.Labels[apistructs.LabelSnippetScope] = req.Scope
		result.Meta[apistructs.AutoTestFileTreeNodeMetaKeySnippetAction] = snippetAction
		return result, nil
	}

	return result, nil
}

func (svc *FileTree) getFileTreeNode(req apistructs.UnifiedFileTreeNodeGetRequest, orgID uint64) (*apistructs.UnifiedFileTreeNode, error) {
	switch req.Scope {
	case apistructs.FileTreeScopeProjectApp:
		return svc.gittarFileTreeSvc.GetFileTreeNode(req, orgID)
	case apistructs.FileTreeScopeProject, apistructs.FileTreeScopeAutoTestPlan, apistructs.FileTreeScopeAutoTestConfigSheet, apistructs.FileTreeScopeAutoTest:
		return svc.autoTestSvc.GetFileTreeNode(req)
	}

	return nil, fmt.Errorf("not find this scope %s", req.Scope)
}

func (svc *FileTree) FuzzySearchFileTreeNodes(req apistructs.UnifiedFileTreeNodeFuzzySearchRequest, orgID uint64) ([]apistructs.UnifiedFileTreeNode, error) {
	// fuzzy
	if req.PrefixFuzzy == "" && req.SuffixFuzzy == "" && req.Fuzzy == "" {
		if req.CreatorID == "" {
			return nil, fmt.Errorf("missing fuzzy condition")
		}
	}
	return svc.fuzzySearchFileTreeNodes(req, orgID)
}

func (svc *FileTree) fuzzySearchFileTreeNodes(query apistructs.UnifiedFileTreeNodeFuzzySearchRequest, orgID uint64) (result []apistructs.UnifiedFileTreeNode, err error) {
	gittarList, err := svc.gittarFileTreeSvc.FuzzySearchFileTreeNodes(query, orgID)
	if err != nil {
		return nil, err
	}
	if gittarList != nil && len(gittarList) >= 0 {
		result = append(result, gittarList...)
	}

	qaList, err := svc.autoTestSvc.FuzzySearchFileTreeNodes(query)
	if err != nil {
		return nil, err
	}

	if qaList != nil && len(qaList) >= 0 {
		result = append(result, qaList...)
	}

	return result, nil
}

func (svc *FileTree) DeleteFileTreeNode(req apistructs.UnifiedFileTreeNodeDeleteRequest, orgID uint64, userID string) (*apistructs.UnifiedFileTreeNode, error) {
	if err := req.BasicValidate(); err != nil {
		return nil, apierrors.ErrGetFileTreeNode.InvalidParameter(err)
	}
	if err := strutil.Validate(req.ScopeID, strutil.MinLenValidator(1)); err != nil {
		return nil, fmt.Errorf("invalid scopeID: %v", err)
	}
	if err := strutil.Validate(req.Scope, strutil.MinLenValidator(1)); err != nil {
		return nil, fmt.Errorf("invalid scope: %v", err)
	}

	return svc.deleteFileTreeNodes(req, orgID, userID)
}

func (svc *FileTree) deleteFileTreeNodes(req apistructs.UnifiedFileTreeNodeDeleteRequest, orgID uint64, userID string) (result *apistructs.UnifiedFileTreeNode, err error) {
	switch req.Scope {
	case apistructs.FileTreeScopeProjectApp:
		return svc.gittarFileTreeSvc.DeleteFileTreeNode(req, orgID, userID)
	case apistructs.FileTreeScopeProject, apistructs.FileTreeScopeAutoTestPlan, apistructs.FileTreeScopeAutoTestConfigSheet, apistructs.FileTreeScopeAutoTest:
		return svc.autoTestSvc.DeleteFileTreeNode(req)
	}

	return nil, fmt.Errorf("not find this scope %s", req.Scope)
}

func (svc *FileTree) CreateFileTreeNode(req apistructs.UnifiedFileTreeNodeCreateRequest, orgID uint64) (*apistructs.UnifiedFileTreeNode, error) {
	// 创建前校验
	if err := req.BasicValidate(); err != nil {
		return nil, apierrors.ErrGetFileTreeNode.InvalidParameter(err)
	}
	if err := strutil.Validate(req.ScopeID, strutil.MinLenValidator(1)); err != nil {
		return nil, fmt.Errorf("invalid scopeID: %v", err)
	}
	if err := strutil.Validate(req.Scope, strutil.MinLenValidator(1)); err != nil {
		return nil, fmt.Errorf("invalid scope: %v", err)
	}

	return svc.createFileTreeNodes(req, orgID)
}

func (svc *FileTree) createFileTreeNodes(req apistructs.UnifiedFileTreeNodeCreateRequest, orgID uint64) (result *apistructs.UnifiedFileTreeNode, err error) {
	switch req.Scope {
	case apistructs.FileTreeScopeProjectApp:
		return svc.gittarFileTreeSvc.CreateFileTreeNode(req, orgID)
	case apistructs.FileTreeScopeProject, apistructs.FileTreeScopeAutoTestPlan, apistructs.FileTreeScopeAutoTestConfigSheet, apistructs.FileTreeScopeAutoTest:
		return svc.autoTestSvc.CreateFileTreeNode(req)
	}

	return nil, fmt.Errorf("not find this scope %s", req.Scope)
}

func (svc *FileTree) FindFileTreeNodeAncestors(req apistructs.UnifiedFileTreeNodeFindAncestorsRequest, orgID uint64) ([]apistructs.UnifiedFileTreeNode, error) {
	// 参数校验
	if err := req.BasicValidate(); err != nil {
		return nil, apierrors.ErrFindFileTreeNodeAncestors.InvalidParameter(err)
	}
	return svc.findFileTreeNodeAncestors(req, orgID)
}

func (svc *FileTree) findFileTreeNodeAncestors(req apistructs.UnifiedFileTreeNodeFindAncestorsRequest, orgID uint64) (result []apistructs.UnifiedFileTreeNode, err error) {
	switch req.Scope {
	case apistructs.FileTreeScopeProjectApp:
		return svc.gittarFileTreeSvc.FindFileTreeNodeAncestors(req)
	case apistructs.FileTreeScopeProject, apistructs.FileTreeScopeAutoTestPlan, apistructs.FileTreeScopeAutoTestConfigSheet, apistructs.FileTreeScopeAutoTest:
		return svc.autoTestSvc.FindFileTreeNodeAncestors(req)
	}

	return nil, fmt.Errorf("not find this scope %s", req.Scope)
}

func scopeConvertToUnifiedFileTreeNode(scope string, scopeID string) *apistructs.UnifiedFileTreeNode {
	return &apistructs.UnifiedFileTreeNode{
		Type:    apistructs.UnifiedFileTreeNodeTypeDir,
		Name:    getScopeName(scope),
		ScopeID: scopeID,
		Scope:   scope,
		Pinode:  "-1",
		Inode:   "0",
	}
}

func getScopeName(scope string) string {
	switch scope {
	case apistructs.FileTreeScopeAutoTest:
		return "自动化测试用例"
	case apistructs.FileTreeScopeAutoTestConfigSheet:
		return "配置单用例"
	case apistructs.FileTreeScopeProjectApp:
		return "应用流水线"
	case apistructs.FileTreeScopeProject:
		return "项目流水线"
	case apistructs.FileTreeScopeAutoTestPlan:
		return "自动化测试计划"
	}

	return "未知"
}
