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

// Package apistructs api request/response结构体定义
package apistructs

import (
	"net/url"
	"strconv"
	"time"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/pkg/parser/diceyml"
)

// ResourceType release可管理的资源类型
type ResourceType string

const (
	// MigrationResourceKey release出来的migration信息key，作用于服务的release->resource信息中
	MigrationResourceKey string = "dice-migration"
)

const (
	ReleaseTypeProject     = "project"
	ReleaseTypeApplication = "application"
)

const (
	// ResourceTypeDiceYml 资源类型为dice.yml
	ResourceTypeDiceYml ResourceType = "diceyml"
	// ResourceTypeAddonYml 资源类型为addon.yml
	ResourceTypeAddonYml ResourceType = "addonyml"
	// ResourceTypeBinary 资源类型为二进制可执行文件
	ResourceTypeBinary ResourceType = "binary"
	// ResourceTypeScript 资源类型为可执行的脚本文件, eg: shell/python/ruby, etc
	ResourceTypeScript ResourceType = "script"
	// ResourceTypeSQL 资源类型为可执行的sql文本
	ResourceTypeSQL ResourceType = "sql"
	// ResourceTypeDataSet 资源类型为数据文本文件
	ResourceTypeDataSet ResourceType = "data"
	// ResourceTypeAndroid android类型文件
	ResourceTypeAndroid ResourceType = "android"
	// ResourceTypeAndroidAppBundle android aab 类型文件
	ResourceTypeAndroidAppBundle ResourceType = "aab"
	// ResourceTypeIOS ios类型文件
	ResourceTypeIOS ResourceType = "ios"
	// ResourceTypeMigration 资源类型为migration文件releaseID
	ResourceTypeMigration ResourceType = "migration"
	// ResourceTypeH5 h5类型的资源文件
	ResourceTypeH5 ResourceType = "h5"
)

// ReleaseCreateRequest 创建Release API(POST /api/releases)使用
type ReleaseCreateRequest struct {
	// ReleaseName 任意字符串，便于用户识别，最大长度255，必填
	ReleaseName string `json:"releaseName"`

	// Desc 详细描述此release功能, 选填
	Desc string `json:"desc,omitempty"`

	// Dice 资源类型为diceyml时, 存储dice.yml内容, 选填
	Dice string `json:"dice,omitempty"`

	// Addon addon注册时，release包含dice.yml与addon.yml，选填
	Addon string `json:"addon,omitempty"`

	// Tags
	Tags []string `json:"tags,omitempty"`

	// IsStable stable表示非临时制品
	IsStable bool `json:"isStable,omitempty"`

	// IsFormal 是否为正式版
	IsFormal bool `json:"isFormal,omitempty"`

	// IsProjectRelease 是否为项目级别制品
	IsProjectRelease bool `json:"isProjectRelease,omitempty"`

	// Changelog 用于保存changelog
	Changelog string `json:"changelog,omitempty"`

	// Modes 制品部署模式
	Modes map[string]ReleaseDeployMode `json:"modes,omitempty"`

	// Labels 用于release分类，描述release类别，map类型, 最大长度1000, 选填
	Labels map[string]string `json:"labels,omitempty"`

	// Version 存储release版本信息, 同一企业同一项目同一应用下唯一，最大长度100，选填
	Version string `json:"version,omitempty"`

	// OrgID 企业标识符，描述release所属企业，选填
	OrgID int64 `json:"orgId,omitempty"`

	// ProjectID 项目标识符，描述release所属项目，选填
	ProjectID int64 `json:"projectId,omitempty"`

	// ApplicationID 应用标识符，描述release所属应用，选填
	ApplicationID int64 `json:"applicationId,omitempty"`

	// ProjectName 项目名称，描述release所属项目，选填
	ProjectName string `json:"projectName,omitempty"`

	// ApplicationName 应用名称，描述release所属应用，选填
	ApplicationName string `json:"applicationName,omitempty"`

	// UserID 用户标识符, 描述release所属用户，最大长度50，选填
	UserID string `json:"userId,omitempty"`

	// ClusterName 集群名称，描述release所属集群，最大长度80，选填
	ClusterName string `json:"clusterName,omitempty"`

	// Resources release包含的资源，包含类型、名称、资源存储路径, 为兼容现有diceyml，先选填
	Resources []ReleaseResource `json:"resources,omitempty"`

	// CrossCluster 跨集群
	CrossCluster bool `json:"crossCluster,omitempty"`

	// 分支
	GitBranch string `json:"gitBranch,omitempty"`
}

type ReleaseDeployMode struct {
	DependOn               []string   `json:"dependOn,omitempty"`
	Expose                 bool       `json:"expose"`
	ApplicationReleaseList [][]string `json:"applicationReleaseList,omitempty"`
}

type ReleaseUploadRequest struct {
	// DiceFileID 上传的dice.yml文件ID，必填
	DiceFileID string `json:"diceFileID,omitempty"`
	// ProjectID 项目ID，必填
	ProjectID int64 `json:"projectID,omitempty"`
	// ProjectName 项目名称，选填
	ProjectName string `json:"projectName,omitempty"`
	// OrgID 企业标识符，描述release所属企业，选填
	OrgID int64 `json:"orgId,omitempty"`
	// UserID 用户标识符, 描述release所属用户，最大长度50，选填
	UserID string `json:"userId,omitempty"`
	// ClusterName 集群名称，描述release所属集群，最大长度80，选填
	ClusterName string `json:"clusterName,omitempty"`
}

type ParseReleaseFileRequest struct {
	DiceFileID string `json:"diceFileID,omitempty"`
}

type ParseReleaseFileResponse struct {
	Header
	Data ParseReleaseFileResponseData `json:"data"`
}

type ParseReleaseFileResponseData struct {
	Version string `json:"version,omitempty"`
}

// ReleaseResource release资源结构
type ReleaseResource struct {
	// Type 资源类型, 必填

	// 资源类型
	Type ResourceType `json:"type"`
	// Name 资源名称，选填, eg: init.sql/upgrade.sql

	// 资源名称
	Name string `json:"name"`
	// URL 资源URL, 可直接wget获取到资源, 选填(当type为diceyml, 资源作为release的dice字段在mysql存储)

	// 资源URL, 可wget获取
	URL string `json:"url"`

	Meta map[string]interface{} `json:"meta"`
}

// ReleaseCreateResponse 创建 release API响应数据结构
type ReleaseCreateResponse struct {
	Header
	Data ReleaseCreateResponseData `json:"data"`
}

// ReleaseCreateResponseData 创建 release 实际返回数据
type ReleaseCreateResponseData struct {
	ReleaseID string `json:"releaseId"`
}

// ReleaseUpdateRequest 更新 release API(PUT /api/releases/{releaseId})使用
type ReleaseUpdateRequest struct {
	ReleaseID string                   `json:"-" path:"releaseId"`
	Body      ReleaseUpdateRequestData `json:"body"`
}

// ReleaseUpdateRequestData 更新 release 请求数据结构
type ReleaseUpdateRequestData struct {
	Version   string                       `json:"version,omitempty"`
	Desc      string                       `json:"desc,omitempty"`
	Changelog string                       `json:"changelog,omitempty"`
	Dice      string                       `json:"dice,omitempty"` // 项目级别制品使用
	Modes     map[string]ReleaseDeployMode `json:"modes,omitempty"`
	// 以下信息主要为了version覆盖使用，找出之前的version清除

	// 企业标识
	OrgID int64 `json:"orgId"`

	// 项目Id
	ProjectID int64 `json:"projectId"`

	// 应用Id
	ApplicationID int64 `json:"applicationId"`
}

// ReleaseUpdateResponse 更新 release API 响应数据结构
type ReleaseUpdateResponse struct {
	Header
	Data string `json:"data"` // Update succ
}

// ReleaseReferenceUpdateRequest 更新Reference API(/api/releases/{releaseId}/reference/actions/change)使用
type ReleaseReferenceUpdateRequest struct {
	ReleaseID string `json:"-" path:"releaseId"`
	Increase  bool   `json:"increase"` // true:reference+1  false:reference-1
}

// ReleaseDeleteRequest 删除 release API(DELETE /api/releases/{releaseId})使用
type ReleaseDeleteRequest struct {
	ReleaseID string `json:"-" path:"releaseId"`
}

// ReleaseDeleteResponse 删除 release API响应数据结构
type ReleaseDeleteResponse struct {
	Header
	Data string `json:"data"` // Delete succ
}

// ReleaseGetRequest release详情 API(GET /api/releases/{releaseId})使用
type ReleaseGetRequest struct {
	ReleaseID string `json:"-" path:"releaseId"`
}

// ReleaseGetResponse release 详情API响应数据结构
type ReleaseGetResponse struct {
	Header
	Data ReleaseGetResponseData `json:"data"`
}

// ReleaseGetResponseData release 详情API实际返回数据
type ReleaseGetResponseData struct {
	ReleaseID        string                              `json:"releaseId"`
	ReleaseName      string                              `json:"releaseName"`
	Diceyml          string                              `json:"diceyml"`
	Desc             string                              `json:"desc,omitempty"`
	Addon            string                              `json:"addon,omitempty"`
	Changelog        string                              `json:"changelog,omitempty"`
	IsStable         bool                                `json:"isStable"`
	IsFormal         bool                                `json:"isFormal"`
	IsProjectRelease bool                                `json:"isProjectRelease"`
	Modes            map[string]ReleaseDeployModeSummary `json:"modes,omitempty"`
	Resources        []ReleaseResource                   `json:"resources,omitempty"`
	Images           []string                            `json:"images,omitempty"`
	ServiceImages    []*ServiceImagePair                 `json:"serviceImages"`
	Labels           map[string]string                   `json:"labels,omitempty"`
	Tags             []ReleaseTag                        `json:"tags,omitempty"`
	Version          string                              `json:"version,omitempty"`

	// CrossCluster 是否可以跨集群
	CrossCluster bool `json:"crossCluster,omitempty"`

	// 当前被部署次数
	Reference int64 `json:"reference"`

	// 企业标识
	OrgID int64 `json:"orgId"`

	// 项目Id
	ProjectID int64 `json:"projectId"`

	// 应用Id
	ApplicationID int64 `json:"applicationId"`

	// 项目Name
	ProjectName string `json:"projectName"`

	// 应用Name
	ApplicationName string `json:"applicationName"`

	// 操作用户Id
	UserID string `json:"userId,omitempty"`

	// 集群名称
	ClusterName string    `json:"clusterName"`
	CreatedAt   time.Time `json:"createdAt"`
	UpdatedAt   time.Time `json:"updatedAt"`
	// IsLatest 是否为分支最新
	IsLatest bool `json:"isLatest"`
}

type ReleaseDeployModeSummary struct {
	DependOn               []string                       `json:"dependOn,omitempty"`
	Expose                 bool                           `json:"expose"`
	ApplicationReleaseList [][]*ApplicationReleaseSummary `json:"applicationReleaseList,omitempty"`
}

func (r *ReleaseGetResponseData) ReLoadImages() error {
	for name := range r.Modes {
		for i := 0; i < len(r.Modes[name].ApplicationReleaseList); i++ {
			for j := 0; j < len(r.Modes[name].ApplicationReleaseList[i]); j++ {
				if err := r.Modes[name].ApplicationReleaseList[i][j].ReLoadImages(); err != nil {
					return err
				}
			}
		}
	}

	if r.Diceyml == "" {
		return nil
	}
	deployable, err := diceyml.NewDeployable([]byte(r.Diceyml), diceyml.WS_PROD, false)
	if err != nil {
		return err
	}
	var obj = deployable.Obj()
	r.Images = nil
	r.ServiceImages = nil
	for name, service := range obj.Services {
		r.Images = append(r.Images, service.Image)
		r.ServiceImages = append(r.ServiceImages, &ServiceImagePair{
			ServiceName: name,
			Image:       service.Image,
		})
	}

	return nil
}

type ServiceImagePair struct {
	ServiceName string `json:"name"`
	Image       string `json:"image"`
}

type ApplicationReleaseSummary struct {
	ReleaseID       string              `json:"releaseID,omitempty"`
	ReleaseName     string              `json:"releaseName,omitempty"`
	Version         string              `json:"version,omitempty"`
	ApplicationID   int64               `json:"applicationID"`
	ApplicationName string              `json:"applicationName,omitempty"`
	Services        []*ServiceImagePair `json:"services"`
	CreatedAt       string              `json:"createdAt,omitempty"`
	DiceYml         string              `json:"-"`
}

func (r *ApplicationReleaseSummary) ReLoadImages() error {
	if r.DiceYml == "" {
		return errors.Errorf("invalid release file: it is empty, applicationID: %v, applicationName: %s",
			r.ApplicationID, r.ApplicationName)
	}
	deployable, err := diceyml.NewDeployable([]byte(r.DiceYml), diceyml.WS_PROD, false)
	if err != nil {
		return err
	}
	var obj = deployable.Obj()
	r.Services = nil
	for name, service := range obj.Services {
		r.Services = append(r.Services, &ServiceImagePair{
			ServiceName: name,
			Image:       service.Image,
		})
	}
	return nil
}

// ReleaseListRequest release列表 API(GET /api/releases)使用
type ReleaseListRequest struct {
	// 查询参数，releaseId/releaseName/version
	Query string `json:"-" query:"q"` // 查询参数，可根据releaseId/releaseName/version模糊匹配

	// releaseID 可通过半角逗号间隔，精确匹配多个release
	ReleaseID string `json:"-" query:"releaseID"`

	// release 名字精确匹配
	ReleaseName string `json:"-" query:"releaseName"`

	// 集群名称
	Cluster string `json:"-" query:"cluster"`

	// 分支名
	Branch string `json:"-" query:"branchName"`

	// 是否为每个分支的最新制品
	Latest bool `json:"-" query:"latest"`

	// stable表示非临时制品
	IsStable *bool `json:"-" query:"isStable"`

	// 是否为正式版本
	IsFormal *bool `json:"-" query:"isFormal"`

	// 是否为项目制品
	IsProjectRelease *bool `json:"-" query:"isProjectRelease"`

	// 提交用户
	UserID []string `json:"-" query:"userID"`

	// Version
	Version string `json:"version" query:"version"`

	// commit ID
	CommitID string `json:"-" query:"commitID"`

	// tag
	Tags string `json:"-" query:"tags"`

	// 只列出有 version 的 release
	IsVersion bool `json:"-" query:"isVersion"`

	// 跨集群
	CrossCluster *bool `json:"-" query:"crossCluster"`

	// 跨集群或指定集群
	CrossClusterOrSpecifyCluster *string `json:"-" query:"crossClusterOrSpecifyCluster"`

	// 应用Id
	ApplicationID []string `json:"-" query:"applicationId"`

	// 项目ID
	ProjectID int64 `json:"-" query:"projectId"`

	// 开始时间, ms
	StartTime int64 `json:"-" query:"startTime"`

	// 结束时间,ms
	EndTime int64 `json:"-" query:"endTime"`

	// 分页大小,默认值20
	PageSize int64 `json:"-" query:"pageSize"`

	// 当前页号，默认值1
	PageNum int64 `json:"-" query:"pageNo"`

	// 排序字段
	OrderBy string `json:"orderBy,omitempty"`

	// 升序或降序
	Order string `json:"descOrder,omitempty"`
}

func (req ReleaseListRequest) ConvertToQueryParams() url.Values {
	values := make(url.Values)
	if req.Query != "" {
		values.Add("q", req.Query)
	}
	if req.ReleaseName != "" {
		values.Add("releaseName", req.ReleaseName)
	}
	if req.Cluster != "" {
		values.Add("cluster", req.Cluster)
	}
	if req.CrossCluster != nil {
		values.Add("crossCluster", strconv.FormatBool(*req.CrossCluster))
	}
	if req.CrossClusterOrSpecifyCluster != nil {
		values.Add("crossClusterOrSpecifyCluster", *req.CrossClusterOrSpecifyCluster)
	}
	for _, id := range req.ApplicationID {
		values.Add("applicationId", id)
	}
	if req.ProjectID > 0 {
		values.Add("projectId", strconv.FormatInt(req.ProjectID, 10))
	}
	if req.StartTime > 0 {
		values.Add("startTime", strconv.FormatInt(req.StartTime, 10))
	}
	if req.EndTime > 0 {
		values.Add("endTime", strconv.FormatInt(req.EndTime, 10))
	}
	if req.PageSize > 0 {
		values.Add("pageSize", strconv.FormatInt(req.PageSize, 10))
	}
	if req.PageNum > 0 {
		values.Add("pageNo", strconv.FormatInt(req.PageNum, 10))
	}
	if req.IsVersion {
		values.Add("isVersion", "true")
	}
	if req.Branch != "" {
		values.Add("branchName", req.Branch)
	}
	if req.Latest {
		values.Add("latest", "true")
	}
	if req.IsStable != nil {
		values.Add("isStable", strconv.FormatBool(*req.IsStable))
	}
	if req.IsFormal != nil {
		values.Add("isFormal", strconv.FormatBool(*req.IsFormal))
	}
	if req.IsProjectRelease != nil {
		values.Add("isProjectRelease", strconv.FormatBool(*req.IsProjectRelease))
	}
	for _, id := range req.UserID {
		values.Add("userId", id)
	}
	if req.CommitID != "" {
		values.Add("commitId", req.CommitID)
	}
	if req.Version != "" {
		values.Add("version", req.Version)
	}
	if req.ReleaseID != "" {
		values.Add("releaseId", req.ReleaseID)
	}
	if req.Tags != "" {
		values.Add("tags", req.Tags)
	}
	if req.OrderBy != "" {
		values.Add("orderBy", req.OrderBy)
		values.Add("order", req.Order)
	}

	return values
}

// ReleaseListResponse release 列表API响应数据结构
type ReleaseListResponse struct {
	Header
	Data ReleaseListResponseData `json:"data"`
}

// ReleaseListResponseData release 列表API实际响应数据
type ReleaseListResponseData struct {
	// release总数，用于分页
	Total    int64         `json:"total"`
	Releases []ReleaseData `json:"list"`
}

type ReleaseTag struct {
	CreatedAt time.Time `json:"createdAt,omitempty"`
	UpdatedAt time.Time `json:"updatedAt,omitempty"`
	Creator   string    `json:"creator,omitempty"`
	Id        int64     `json:"id,omitempty"`
	Color     string    `json:"color,omitempty"`
	Name      string    `json:"name,omitempty"`
	Type      string    `json:"type,omitempty"`
	ProjectID int64     `json:"projectID,omitempty"`
}

// ReleaseData release 列表API实际返回数据
type ReleaseData struct {
	ReleaseID              string              `json:"releaseId"`
	ReleaseName            string              `json:"releaseName"`
	Diceyml                string              `json:"diceyml"`
	Desc                   string              `json:"desc,omitempty"`
	Addon                  string              `json:"addon,omitempty"`
	Changelog              string              `json:"changelog,omitempty"`
	IsStable               bool                `json:"isStable"`
	IsFormal               bool                `json:"isFormal"`
	IsProjectRelease       bool                `json:"isProjectRelease"`
	ApplicationReleaseList string              `json:"applicationReleaseList,omitempty"`
	Resources              []ReleaseResource   `json:"resources,omitempty"`
	Images                 []string            `json:"images,omitempty"`
	ServiceImages          []*ServiceImagePair `json:"serviceImages"`
	Labels                 map[string]string   `json:"labels,omitempty"`
	Tags                   []ReleaseTag        `json:"tags,omitempty"`
	Version                string              `json:"version,omitempty"`

	// CrossCluster 是否可以跨集群
	CrossCluster bool `json:"crossCluster,omitempty"`

	// 当前被部署次数
	Reference int64 `json:"reference"`

	// 企业标识
	OrgID int64 `json:"orgId"`

	// 项目Id
	ProjectID int64 `json:"projectId"`

	// 应用Id
	ApplicationID int64 `json:"applicationId"`

	// 项目Name
	ProjectName string `json:"projectName"`

	// 应用Name
	ApplicationName string `json:"applicationName"`

	// 操作用户Id
	UserID string `json:"userId,omitempty"`

	// 集群名称
	ClusterName string    `json:"clusterName"`
	CreatedAt   time.Time `json:"createdAt"`
	UpdatedAt   time.Time `json:"updatedAt"`
	// 是否为分支最新
	IsLatest bool `json:"isLatest"`
}

// ReleaseNameListRequest releaseName列表请求
type ReleaseNameListRequest struct {
	// 应用Id
	ApplicationID int64 `json:"-" query:"applicationId"`
}

// ReleaseNameListResponse releaseName列表响应
type ReleaseNameListResponse struct {
	Header
	Data []string `json:"data"`
}

// ReleasePullRequest release dice.yml内容获取API(GET /api/releases/<releaseId>/actions/pull)
type ReleasePullRequest struct {
	ReleaseID string `json:"-" path:"releaseId"`
}

// ReleaseGetDiceYmlRequest release 请求 dice.yml 格式
type ReleaseGetDiceYmlRequest struct {
	ReleaseID string `json:"-" path:"releaseId"`
}

// ReleaseEventData release 事件格式
type ReleaseEventData ReleaseGetResponseData

// ReleasesDeleteRequest release 批量删除请求结构
type ReleasesDeleteRequest struct {
	// Use to audit
	IsProjectRelease bool     `json:"isProjectRelease"`
	ProjectID        int64    `json:"projectId"`
	ReleaseID        []string `json:"releaseID"`
}

// ReleasesToFormalRequest release 批量转正请求结构
type ReleasesToFormalRequest struct {
	// Use to audit
	IsProjectRelease bool     `json:"isProjectRelease"`
	ProjectID        int64    `json:"projectId"`
	ReleaseID        []string `json:"releaseID"`
}

type ReleasesToFormalResponse struct {
	Header
	Data string `json:"data"`
}

type ReleaseMetadata struct {
	ApiVersion string        `json:"apiVersion,omitempty"`
	Author     string        `json:"author,omitempty"`
	CreatedAt  string        `json:"createdAt,omitempty"`
	Source     ReleaseSource `json:"source"`

	Version   string                         `json:"version,omitempty"`
	Desc      string                         `json:"desc,omitempty"`
	ChangeLog string                         `json:"changeLog,omitempty"`
	Modes     map[string]ReleaseModeMetadata `json:"modes,omitempty"`
}

type ReleaseModeMetadata struct {
	DependOn []string        `json:"dependOn,omitempty"`
	Expose   bool            `json:"expose"`
	AppList  [][]AppMetadata `json:"appList,omitempty"`
}

type ReleaseSource struct {
	Org     string `json:"org,omitempty"`
	Project string `json:"project,omitempty"`
	URL     string `json:"url,omitempty"`
}

type AppMetadata struct {
	AppName          string `json:"appName,omitempty"`
	GitBranch        string `json:"gitBranch,omitempty"`
	GitCommitID      string `json:"gitCommitId,omitempty"`
	GitCommitMessage string `json:"gitCommitMessage,omitempty"`
	GitRepo          string `json:"gitRepo,omitempty"`
	ChangeLog        string `json:"changeLog,omitempty"`
	Version          string `json:"version,omitempty"`
}

type ReleaseCheckVersionRequest struct {
	OrgID            int64  `json:"orgID"`
	IsProjectRelease bool   `json:"isProjectRelease"`
	ProjectID        int64  `json:"projectID"`
	AppID            int64  `json:"appID"`
	Version          string `json:"version,omitempty"`
}

type ReleaseCheckVersionResponse struct {
	Header
	Data ReleaseCheckVersionResponseData `json:"data"`
}

type ReleaseCheckVersionResponseData struct {
	IsUnique bool `json:"isUnique"`
}
