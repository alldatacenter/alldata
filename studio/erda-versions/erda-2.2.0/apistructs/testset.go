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

package apistructs

// TestSetCreateRequest POST /api/testsets 创建测试集返回结构
type TestSet struct {
	// 测试集ID
	ID uint64 `json:"id"`
	// 测试集名称
	Name string `json:"name"`
	// 项目 ID
	ProjectID uint64 `json:"projectID"`
	// 父测试集ID
	ParentID uint64 `json:"parentID"`
	// 是否回收
	Recycled bool `json:"recycled"`
	// 显示的目录地址
	Directory string `json:"directoryName"`
	// 排序
	Order int `json:"order"`
	// 创建人ID
	CreatorID string `json:"creatorID"`
	// 更新人ID
	UpdaterID string `json:"updaterID"`
}

type TestSetWithAncestors struct {
	TestSet
	// ancestors
	Ancestors []TestSet `json:"ancestors,omitempty"`
}

type TestSetGetRequest struct {
	ID uint64 `json:"id"`
}
type TestSetGetResponse struct {
	Header
	Data *TestSetWithAncestors `json:"data"`
}

// TestSetCreateRequest POST /api/testsets 创建测试集
type TestSetCreateRequest struct {
	// 项目ID
	ProjectID *uint64 `json:"projectID"`
	// 父测试集ID
	ParentID *uint64 `json:"parentID"`
	// 名称
	Name string `json:"name"`

	IdentityInfo
}

//  TestSetCreateResponse POST /api/testsets 创建测试集合
type TestSetCreateResponse struct {
	Header
	Data *TestSet `json:"data"`
}

// TestSetListRequest GET /api/testsets 测试集列表
type TestSetListRequest struct {
	// 是否回收
	Recycled bool `schema:"recycled"`
	// 父测试集 ID
	ParentID *uint64 `schema:"parentID"`
	// 项目 ID
	ProjectID *uint64 `schema:"projectID"`

	// 指定 id 列表
	TestSetIDs []uint64 `schema:"testSetID"`

	// 是否不递归，默认 false，即默认递归
	NoSubTestSets bool `json:"noSubTestSets"`
}

//  TestSetListResponse GET /api/testsets 测试集列表
type TestSetListResponse struct {
	Header
	Data []TestSet `json:"data"`
}

// TestSetUpdateRequest PUT /api/testset 更新测试集
type TestSetUpdateRequest struct {
	// 基础信息
	TestSetID uint64 `json:"testsetID"`

	// 待更新项
	Name           *string `json:"name"`
	MoveToParentID *uint64 `json:"moveToParentID"` // 移动至哪个父测试集下

	IdentityInfo
}
type TestSetUpdateResponse struct {
	TestSetGetResponse
}

// TestSetCopyRequest 测试集复制请求
type TestSetCopyRequest struct {
	CopyToTestSetID uint64 `json:"copyToTestSetID"`

	TestSetID uint64 `json:"-"`

	IdentityInfo
}

type TestSetCopyAsyncRequest struct {
	SourceTestSet *TestSet
	DestTestSet   *TestSet
	IdentityInfo
}

type TestSetCopyResponse struct {
	Header
	Data uint64 `json:"data"`
}

// TestSetRecycleRequest 回收测试集至回收站
type TestSetRecycleRequest struct {
	TestSetID uint64 `json:"-"`

	// IsRoot 表示递归回收测试集时是否是最外层的根测试集
	// 如果是根测试集，且 parentID != 0，回收时需要将 parentID 置为 0，否则在回收站中无法找到
	IsRoot bool `json:"-"`

	IdentityInfo
}
type TestSetRecycleResponse struct {
	Header
}

// TestSetCleanFromRecycleBinRequest 从回收站彻底删除测试集
type TestSetCleanFromRecycleBinRequest struct {
	TestSetID uint64 `json:"-"`

	IdentityInfo
}
type TestSetCleanFromRecycleBinResponse struct {
	Header
}

// TestSetRecoverFromRecycleBinRequest 从回收站恢复测试集
type TestSetRecoverFromRecycleBinRequest struct {
	TestSetID          uint64  `json:"-"`
	RecoverToTestSetID *uint64 `json:"recoverToTestSetID"`

	IdentityInfo
}
type TestSetRecoverFromRecycleBinResponse struct {
	Header
}

//  TestSetCommonResponse 通用返回结构
type TestSetCommonResponse struct {
	Header
	Data string `json:"data"`
}
