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

// UnifiedFileTree 统一目录树协议
package apistructs

import (
	"fmt"
	"time"

	"github.com/erda-project/erda/pkg/strutil"
)

const RootPinode = "0"
const MaxSetNameLen = 191
const MaxSetDescLen = 512

// 各个系统的 fileTree scope 名称
const (
	FileTreeScopeAutoTest            = "project-autotest-testcase"
	FileTreeScopeAutoTestPlan        = "project-autotest-testplan"
	FileTreeScopeAutoTestConfigSheet = "project-autotest-configsheet"
	FileTreeScopeProjectApp          = "project-app"
	FileTreeScopeProject             = "project"
)

var AllScope = []string{FileTreeScopeAutoTest, FileTreeScopeAutoTestConfigSheet, FileTreeScopeProjectApp, FileTreeScopeProject, FileTreeScopeAutoTestPlan}
var pipelineSourceCorrespondScope = map[FileTreeScope]PipelineSource{
	FileTreeScopeProject:             PipelineSourceProject,
	FileTreeScopeAutoTestConfigSheet: PipelineSourceConfigSheet,
	FileTreeScopeProjectApp:          PipelineSourceDice,
	FileTreeScopeAutoTest:            PipelineSourceAutoTest,
	FileTreeScopeAutoTestPlan:        PipelineSourceAutoTestPlan,
}

type FileTreeScope string

func (that FileTreeScope) ToPipelineSource() PipelineSource {
	return pipelineSourceCorrespondScope[that]
}

// UnifiedFileTreeNodeType 节点类型
type UnifiedFileTreeNodeType string

var (
	UnifiedFileTreeNodeTypeDir  UnifiedFileTreeNodeType = "d"
	UnifiedFileTreeNodeTypeFile UnifiedFileTreeNodeType = "f"
)

func (t UnifiedFileTreeNodeType) IsFile() bool {
	return t == UnifiedFileTreeNodeTypeFile
}
func (t UnifiedFileTreeNodeType) IsDir() bool {
	return t == UnifiedFileTreeNodeTypeDir
}

func (t UnifiedFileTreeNodeType) String() string {
	return string(t)
}

func (t UnifiedFileTreeNodeType) Valid() bool {
	if t == "" {
		return false
	}
	switch t {
	case UnifiedFileTreeNodeTypeDir, UnifiedFileTreeNodeTypeFile:
		return true
	default:
		return false
	}
}

// UnifiedFileTreeNode 统一目录树节点
type UnifiedFileTreeNode struct {
	Type      UnifiedFileTreeNodeType `json:"type"`    // 类型
	Inode     string                  `json:"inode"`   // 节点 inode
	Pinode    string                  `json:"pinode"`  // 父节点 inode
	Scope     string                  `json:"scope"`   // scope
	ScopeID   string                  `json:"scopeID"` // scope id
	Name      string                  `json:"name"`
	Desc      string                  `json:"desc"`
	CreatorID string                  `json:"creatorID"`
	UpdaterID string                  `json:"updaterID"`
	CreatedAt time.Time               `json:"createdAt"`
	UpdatedAt time.Time               `json:"updatedAt"`
	Meta      UnifiedFileTreeNodeMeta `json:"meta,omitempty"` // 元信息，各后端返回自定义数据满足个性化业务需求
}

// UnifiedFileTreeNodeMeta 统一目录树节点元信息
type UnifiedFileTreeNodeMeta map[string]interface{}

func (n UnifiedFileTreeNode) GetUserIDs() []string {
	return strutil.DedupSlice([]string{n.CreatorID, n.UpdaterID}, true)
}

// CheckSameScope 校验 scope & scopeID 是否一致
func (n UnifiedFileTreeNode) CheckSameScope(n2 UnifiedFileTreeNode) error {
	if n.Scope != n2.Scope {
		return fmt.Errorf("different scope: %s, %s", n.Scope, n2.Scope)
	}
	if n.ScopeID != n2.ScopeID {
		return fmt.Errorf("different scopeID: %s, %s", n.ScopeID, n2.ScopeID)
	}
	return nil
}

// 节点创建
type UnifiedFileTreeNodeCreateRequest struct {
	Type    UnifiedFileTreeNodeType `json:"type"`
	Scope   string                  `json:"scope"`
	ScopeID string                  `json:"scopeID"`
	Pinode  string                  `json:"pinode"` // 创建根目录时 Pinode 为空
	Name    string                  `json:"name"`
	Desc    string                  `json:"desc"`

	IdentityInfo
}
type UnifiedFileTreeNodeCreateResponse struct {
	Header
	Data *UnifiedFileTreeNode `json:"data,omitempty"`
}

func (req UnifiedFileTreeNodeCreateRequest) BasicValidate() error {
	if !req.Type.Valid() {
		return fmt.Errorf("invalid type: %s", req.Type.String())
	}
	if err := strutil.Validate(req.Name, strutil.MinLenValidator(1)); err != nil {
		return fmt.Errorf("invalid name: %v", err)
	}
	if err := strutil.Validate(req.Desc, strutil.MaxLenValidator(255)); err != nil {
		return fmt.Errorf("invalid desc: %v", err)
	}
	return nil
}

func (req UnifiedFileTreeNodeCreateRequest) ValidateRootDir() error {
	if err := req.BasicValidate(); err != nil {
		return err
	}
	// root dir need pinode == ""
	if req.Pinode != "" {
		return fmt.Errorf("root dir doesn't need pinode")
	}
	// root dir need scope & scope_id
	if err := strutil.Validate(req.Scope, strutil.MinLenValidator(1)); err != nil {
		return fmt.Errorf("invalid scope: %v", err)
	}
	if err := strutil.Validate(req.ScopeID, strutil.MinLenValidator(1)); err != nil {
		return fmt.Errorf("invalid scopeID: %v", err)
	}
	return nil
}

func (req UnifiedFileTreeNodeCreateRequest) ValidateNonRootDir() error {
	if err := req.BasicValidate(); err != nil {
		return err
	}
	// non-root dir need pinode
	if req.Pinode == "" {
		return fmt.Errorf("non-root dir need pinode")
	}
	// non-root dir inherit scope & scopeID from pinode
	return nil
}

func (req UnifiedFileTreeNodeCreateRequest) ValidateFile() error {
	if err := req.BasicValidate(); err != nil {
		return err
	}
	// file need pinode
	if req.Pinode == "" {
		return fmt.Errorf("file need pinode")
	}
	return nil
}

// 节点删除
type UnifiedFileTreeNodeDeleteRequest struct {
	Inode string `json:"inode"`

	Scope   string `json:"scope"`
	ScopeID string `json:"scopeID"`

	IdentityInfo
}
type UnifiedFileTreeNodeDeleteResponse struct {
	Header
	Data *UnifiedFileTreeNode `json:"data,omitempty"`
}

func (req UnifiedFileTreeNodeDeleteRequest) BasicValidate() error {
	if err := strutil.Validate(req.Inode, strutil.MinLenValidator(1)); err != nil {
		return fmt.Errorf("invalid inode: %v", err)
	}
	return nil
}

// 节点查询详情 (meta 包含详情)
type UnifiedFileTreeNodeGetRequest struct {
	Inode string `json:"inode"`

	Scope   string `json:"scope"`
	ScopeID string `json:"scopeID"`

	IdentityInfo
}
type UnifiedFileTreeNodeGetResponse struct {
	Header
	Data *UnifiedFileTreeNode `json:"data,omitempty"`
}

func (req UnifiedFileTreeNodeGetRequest) BasicValidate() error {
	if err := strutil.Validate(req.Inode, strutil.MinLenValidator(1)); err != nil {
		return fmt.Errorf("invalid inode: %v", err)
	}
	return nil
}

// 节点更新基本信息
type UnifiedFileTreeNodeUpdateBasicInfoRequest struct {
	Inode string `json:"inode"`

	Name *string `json:"name"`
	Desc *string `json:"desc"`

	IdentityInfo
}
type UnifiedFileTreeNodeUpdateBasicInfoResponse struct {
	Header
	Data *UnifiedFileTreeNode `json:"data,omitempty"`
}

func (req UnifiedFileTreeNodeUpdateBasicInfoRequest) BasicValidate() error {
	if err := strutil.Validate(req.Inode, strutil.MinLenValidator(1)); err != nil {
		return fmt.Errorf("invalid inode: %v", err)
	}
	return nil
}

// 节点移动
type UnifiedFileTreeNodeMoveRequest struct {
	Inode string `json:"inode"`

	Pinode string `json:"pinode"`

	IdentityInfo
}
type UnifiedFileTreeNodeMoveResponse struct {
	Header
	Data *UnifiedFileTreeNode `json:"data,omitempty"`
}

func (req UnifiedFileTreeNodeMoveRequest) BasicValidate() error {
	if err := strutil.Validate(req.Inode, strutil.MinLenValidator(1)); err != nil {
		return fmt.Errorf("invalid inode: %v", err)
	}
	if err := strutil.Validate(req.Pinode, strutil.MinLenValidator(1)); err != nil {
		return fmt.Errorf("invalid pinode: %v", err)
	}
	return nil
}

// 节点拷贝
type UnifiedFileTreeNodeCopyRequest struct {
	Inode string `json:"inode"`

	Pinode string `json:"pinode"`

	IdentityInfo
}
type UnifiedFileTreeNodeCopyResponse struct {
	Header
	Data *UnifiedFileTreeNode `json:"data,omitempty"`
}

func (req UnifiedFileTreeNodeCopyRequest) BasicValidate() error {
	if err := strutil.Validate(req.Inode, strutil.MinLenValidator(1)); err != nil {
		return fmt.Errorf("invalid inode: %v", err)
	}
	if err := strutil.Validate(req.Pinode, strutil.MinLenValidator(1)); err != nil {
		return fmt.Errorf("invalid pinode: %v", err)
	}
	return nil
}

// 节点列表
type UnifiedFileTreeNodeListRequest struct {
	Scope   string `schema:"scope"`
	ScopeID string `schema:"scopeID"`

	Pinode              string `schema:"pinode"`
	PipelineCategoryKey string `schema:"pipelineCategoryKey"`

	IdentityInfo `schema:"-"`
}
type UnifiedFileTreeNodeListResponse struct {
	Header
	Data []UnifiedFileTreeNode `json:"data,omitempty"`
}

func (req UnifiedFileTreeNodeListRequest) BasicValidate() error {
	if err := strutil.Validate(req.Pinode, strutil.MinLenValidator(1)); err != nil {
		return fmt.Errorf("invalid pinode: %v", err)
	}
	// pinode = 0 时，无法区分，必须要声明 scope & scopeID
	if req.Pinode == RootPinode {
		if err := strutil.Validate(req.Scope, strutil.MinLenValidator(1)); err != nil {
			return fmt.Errorf("invalid scope: %v", err)
		}
		if err := strutil.Validate(req.ScopeID, strutil.MinLenValidator(1)); err != nil {
			return fmt.Errorf("invalid scopeID: %v", err)
		}
	}
	return nil
}

// 节点寻祖
type UnifiedFileTreeNodeFindAncestorsRequest struct {
	Inode string `json:"inode"`

	Scope   string `json:"scope"`
	ScopeID string `json:"scopeID"`

	IdentityInfo
}
type UnifiedFileTreeNodeFindAncestorsResponse struct {
	Header
	Data []UnifiedFileTreeNode `json:"data,omitempty"`
}

func (req UnifiedFileTreeNodeFindAncestorsRequest) BasicValidate() error {
	if err := strutil.Validate(req.Inode, strutil.MinLenValidator(1)); err != nil {
		return fmt.Errorf("invalid inode: %v", err)
	}
	return nil
}

// 节点搜索
type UnifiedFileTreeNodeFuzzySearchRequest struct {
	Scope   string `schema:"scope"`
	ScopeID string `schema:"scopeID"`

	// 从哪个父节点开始搜索
	FromPinode string `schema:"fromPinode"`

	// 是否需要递归，若不递归，则只返回当前层
	Recursive bool `schema:"recursive" default:"false"`

	// fuzzy search
	PrefixFuzzy  string `schema:"prefixFuzzy,omitempty"`
	SuffixFuzzy  string `schema:"suffixFuzzy,omitempty"`
	Fuzzy        string `schema:"fuzzy,omitempty"`
	CreatorID    string `schema:"creatorID,omitempty"`
	IdentityInfo `schema:"-"`
}
type UnifiedFileTreeNodeFuzzySearchResponse struct {
	Header
	Data []UnifiedFileTreeNode `json:"data,omitempty"`
}

func (req UnifiedFileTreeNodeFuzzySearchRequest) BasicValidate() error {
	// 需要指定 scope & scopeID
	if err := strutil.Validate(req.Scope, strutil.MinLenValidator(1)); err != nil {
		return fmt.Errorf("invalid scope: %v", err)
	}
	if err := strutil.Validate(req.ScopeID, strutil.MinLenValidator(1)); err != nil {
		return fmt.Errorf("invalid scopeID: %v", err)
	}

	// fuzzy
	if req.PrefixFuzzy == "" && req.SuffixFuzzy == "" && req.Fuzzy == "" {
		if req.CreatorID != "" {
			return nil
		}
		return fmt.Errorf("missing fuzzy condition")
	}

	return nil
}

// 节点历史搜索
type UnifiedFileTreeNodeHistorySearchRequest struct {
	Inode string `json:"inode"`
	IdentityInfo
}
