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

import (
	"strconv"
	"time"
)

// AutoTestSpaceStatus 测试空间状态
type AutoTestSpaceStatus string

func (a AutoTestSpace) IsOpen() bool {
	return a.Status == TestSpaceOpen
}

var (
	// TestSpaceCopying 复制中
	TestSpaceCopying AutoTestSpaceStatus = "copying"
	// TestSpaceLocked 被（复制）锁定
	TestSpaceLocked AutoTestSpaceStatus = "locked"
	// TestSpaceOpen open
	TestSpaceOpen AutoTestSpaceStatus = "open"
	// TestSpaceFailed （复制）失败
	TestSpaceFailed AutoTestSpaceStatus = "failed"
)

type AutoTestSpaceArchiveStatus string

const (
	TestSpaceInit       AutoTestSpaceArchiveStatus = "Init"
	TestSpaceInProgress AutoTestSpaceArchiveStatus = "InProgress"
	TestSpaceCompleted  AutoTestSpaceArchiveStatus = "Completed"
)

func (s AutoTestSpaceArchiveStatus) GetZhName() string {
	switch s {
	case TestSpaceInit:
		return "未开始"
	case TestSpaceInProgress:
		return "进行中"
	case TestSpaceCompleted:
		return "已完成"
	default:
		return ""
	}
}

func (s AutoTestSpaceArchiveStatus) GetFrontEndStatus() string {
	switch s {
	case TestSpaceInit:
		return "default"
	case TestSpaceInProgress:
		return "processing"
	case TestSpaceCompleted:
		return "success"
	default:
		return ""
	}
}

func (s AutoTestSpaceArchiveStatus) Valid() bool {
	switch s {
	case TestSpaceInit, TestSpaceInProgress, TestSpaceCompleted:
		return true
	default:
		return false
	}
}

// AutoTestSpace 测试空间
type AutoTestSpace struct {
	ID            uint64                     `json:"id"`
	Name          string                     `json:"name"`
	ProjectID     int64                      `json:"projectId"`
	Description   string                     `json:"description"`
	CreatorID     string                     `json:"creatorId"`
	UpdaterID     string                     `json:"updaterId"`
	Status        AutoTestSpaceStatus        `json:"status"`
	ArchiveStatus AutoTestSpaceArchiveStatus `json:"archiveStatus"`
	// 被复制的源测试空间
	SourceSpaceID *uint64 `json:"sourceSpaceId,omitempty"`
	// CreatedAt 创建时间
	CreatedAt time.Time `json:"createdAt"`
	// UpdatedAt 更新时间
	UpdatedAt time.Time `json:"updatedAt"`
	// DeletedAt 删除时间
	DeletedAt *time.Time `json:"deletedAt"`
}

// record the structure of the information before and after the scene collection is copied
// to update those scenes that refer to the old scene set to refer to the new scene set
type AutoTestSceneCopyRef struct {
	PreSetID     uint64 // the id of the copied scene set
	PreSpaceID   uint64 // the id of the copied space
	AfterSetID   uint64 // id of the scene set to be copied
	AfterSpaceID uint64 // id of the space to be copied
}

// AutoTestSpaceCopy 测试空间复制
type AutoTestSpaceCopy struct {
	Name      string `json:"name"`
	SourceID  uint64 `json:"sourceId"`
	ProjectID int64  `json:"projectId"`
}

// AutoTestSpaceCreateRequest 测试空间创建请求
type AutoTestSpaceCreateRequest struct {
	Name          string                     `json:"name"`
	ProjectID     int64                      `json:"projectId"`
	Description   string                     `json:"description"`
	SourceSpaceID *uint64                    `json:"sourceSpaceId"`
	ArchiveStatus AutoTestSpaceArchiveStatus `json:"archiveStatus"`
	IdentityInfo
}

// AutoTestSpaceCreateResponse 测试空间创建响应
type AutoTestSpaceResponse struct {
	Header
	Data *AutoTestSpace `json:"data"`
}

type AutoTestSpaceListRequest struct {
	Name          string
	ProjectID     int64
	PageNo        int64
	PageSize      int64
	Order         string
	ArchiveStatus []string
}

func (ats *AutoTestSpaceListRequest) URLQueryString() map[string][]string {
	query := make(map[string][]string)
	if ats.Name != "" {
		query["name"] = append(query["name"], ats.Name)
	}
	if ats.Order != "" {
		query["order"] = append(query["order"], ats.Order)
	}
	if len(ats.ArchiveStatus) > 0 {
		query["archiveStatus"] = append(query["archiveStatus"], ats.ArchiveStatus...)
	}
	if ats.ProjectID != 0 {
		query["projectID"] = []string{strconv.FormatInt(ats.ProjectID, 10)}
	}
	if ats.PageNo != 0 {
		query["pageNo"] = []string{strconv.FormatInt(ats.PageNo, 10)}
	}
	if ats.PageSize != 0 {
		query["pageSize"] = []string{strconv.FormatInt(ats.PageSize, 10)}
	}
	return query
}

// AutoTestSpaceListResponse 获取测试空间列表响应
type AutoTestSpaceListResponse struct {
	Header
	Data *AutoTestSpaceList `json:"data"`
}

// AutoTestSpaceList 获取测试空间列表
type AutoTestSpaceList struct {
	List  []AutoTestSpace `json:"list"`
	Total int             `json:"total"`
}

// AutoTestSpaceExportRequest export autotest space
type AutoTestSpaceExportRequest struct {
	ID        uint64            `json:"id"`
	Locale    string            `schema:"-"`
	IsCopy    bool              `json:"-"`
	FileType  TestSpaceFileType `schema:"fileType"`
	ProjectID uint64            `json:"projectID"`
	SpaceName string            `json:"spaceName"`

	IdentityInfo
}

type AutoTestSpaceExportResponse struct {
	Header
	Data uint64 `json:"data"`
}

type TestSpaceFileType string

var (
	TestSpaceFileTypeExcel TestSpaceFileType = "excel"
)

func (t TestSpaceFileType) Valid() bool {
	switch t {
	case TestSpaceFileTypeExcel:
		return true
	default:
		return false
	}
}

type AutoTestSpaceImportRequest struct {
	ProjectID uint64            `schema:"projectID"`
	FileType  TestSpaceFileType `schema:"fileType"`

	IdentityInfo
}

type AutoTestSpaceImportResponse struct {
	Header
	Data uint64 `json:"data"`
}

type AutoTestSpaceStatsRequest struct {
	SpaceIDs []uint64 `json:"spaceIDs"`
}

type AutoTestSpaceStatsResponse struct {
	Header
	Data map[uint64]*AutoTestSpaceStats `json:"data"`
}

type AutoTestSpaceStats struct {
	SetNum   int
	SceneNum int
	StepNum  int
}

type AutoTestSceneCount struct {
	Count int
}

type AutoTestSceneStepCount struct {
	Count int
}
