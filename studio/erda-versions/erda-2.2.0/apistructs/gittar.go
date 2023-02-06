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
	"time"
)

const (
	GitPushEvent         = "git_push"
	GitCreateMREvent     = "git_create_mr"
	GitCloseMREvent      = "git_close_mr"
	GitMergeMREvent      = "git_merge_mr"
	GitUpdateMREvent     = "git_update_mr"
	GitCommentMREvent    = "git_comment_mr"
	CheckRunEvent        = "check-run"
	GitDeleteBranchEvent = "git_delete_branch"
	GitDeleteTagEvent    = "git_delete_tag"
)

// GittarPushEvent POST /callback/gittar eventbox回调的gittar事件结构体
type GittarPushEvent struct {
	EventHeader
	Content GittarPushEventRequest `json:"content"`
}

// GittarPushEventRequest 创建向gittar推事件的请求结构
type GittarPushEventRequest struct {
	TotalCommitsCount int         `json:"total_commits_count"`
	IsTag             bool        `json:"is_tag"`
	ObjectKind        string      `json:"object_kind"`
	Ref               string      `json:"ref"`
	After             string      `json:"after"`
	Before            string      `json:"before"`
	Repository        *Repository `json:"repository"`
	Pusher            *Pusher     `json:"pusher"`
}

// Repository represents a Git repository
type Repository struct {
	ProjectID      int    `json:"project_id"`
	OrganizationID int    `json:"organization_id"`
	ApplicationID  int64  `json:"application_id"`
	Organization   string `json:"organization"`
	Repository     string `json:"repository"`
	URL            string `json:"url"`
}

// Pusher 提交代码的用户信息
type Pusher struct {
	ID       string `json:"id"`
	Name     string `json:"name"`
	NickName string `json:"nickname"`
	Email    string `json:"email"`
	Role     string `json:"role"`
}

// GittarPushEventResponse POST /callback/gittar 创建向gittar推事件的返回结构
type GittarPushEventResponse struct {
	Header
	Data string `json:"data"`
}

// GittarFileData represents response data
type GittarFileData struct {
	//是否为二进制文件,如果是lines不会有内容
	Binary  bool   `json:"binary"`
	Content string `json:"content"`
	RefName string `json:"refName"`
	Path    string `json:"path"`
}

// GittarLinesData represents response data
type GittarLinesData struct {
	//是否为二进制文件,如果是lines不会有内容
	Binary  bool     `json:"binary"`
	Lines   []string `json:"lines"`
	RefName string   `json:"refName"`
	Path    string   `json:"path"`
}

// GittarLinesResponse GET /<projectName>/<appName>/blob-range/<commitId>/<path> 从gittar获取指定区间的代码行数
type GittarLinesResponse struct {
	Header
	Data GittarLinesData `json:"data"`
}

// GittarFileResponse GET /<projectName>/<appName>/raw/<fileName> 从gittar获取指定文件内容
type GittarFileResponse struct {
	Header
	Data GittarFileData `json:"data"`
}

// GittarBranchesResponse GET /<projectName>/<appName>/branches 获取分支列表
type GittarBranchesResponse struct {
	Header
	Data []Branch `json:"data"`
}

// GittarTagsResponse GET /<projectName>/<appName>/tags 获取标签列表
type GittarTagsResponse struct {
	Header
	Data []Tag `json:"data"`
}

// GittarStatsResponse GET /<projectName>/<appName>/stats 获取仓库状态
type GittarStatsResponse struct {
	Header
	Data GittarStatsData
}

// GittarStatsData 仓库状态信息
type GittarStatsData struct {
	CommitsCount int `json:"commitsCount"`

	// 提交的人数
	ContributorCount int      `json:"contributorCount"`
	Tags             []string `json:"tags"`
	Branches         []string `json:"branches"`
	DefaultBranch    string   `json:"defaultBranch"`

	// 仓库是否为空
	Empty    bool   `json:"empty"`
	CommitID string `json:"commitId"`

	// open状态的mr数量
	MergeRequestCount int    `json:"mergeRequestCount"`
	Size              int    `json:"size"`
	ReadmeFile        string `json:"readmeFile"`

	ApplicationID int64  `json:"applicationID"`
	ProjectID     uint64 `json:"projectID"`
}

// GittarTreeResponse GET /<projectName>/<appName>/tree/*  获取目录内容
type GittarTreeResponse struct {
	Header
	Data GittarTreeData `json:"data"`
}

// GittarTreeData tree响应数据
type GittarTreeData struct {
	Type       string      `json:"type"`
	RefName    string      `json:"refName"`
	Path       string      `json:"path"`
	Binary     bool        `json:"binary"`
	Entries    []TreeEntry `json:"entries"`
	Commit     Commit      `json:"commit"`
	TreeID     string      `json:"treeId"`
	ReadmeFile string      `json:"readmeFile"`
	IsLocked   bool        `json:"isLocked"`
}

// GittarCommitResponse GET /<projectName>/<appName>/commit/<commitId>  获取commit详情
type GittarCommitResponse struct {
	Header
	Data GittarDiffData `json:"data"`
}

// GittarDiffData diff响应数据
type GittarDiffData struct {
	Commit Commit `json:"commit"`
	Diff   Diff   `json:"diff"`
}

// GittarCommitsRequest  commits请求
type GittarCommitsRequest struct {
	//commit message过滤条件
	Search   string `query:"search"`
	PageNo   int    `query:"pageNo"`
	PageSize int    `query:"pageSize"`
}

// GittarCommitsResponse GET /<projectName>/<appName>/commits/<ref>  获取指定ref的commits
type GittarCommitsResponse struct {
	Header
	Data []Commit
}

// GittarCompareResponse GET /<projectName>/<appName>/compare/from...target 对比2个ref
type GittarCompareResponse struct {
	Header
	Data GittarCompareData `json:"data"`
}

// GittarCompareData compare响应数据
type GittarCompareData struct {
	From    string   `json:"from"`
	To      string   `json:"to"`
	Commits []Commit `json:"commits"`
	Diff    Diff     `json:"diff"`
}

// GittarBlobResponse GET /<projectName>/<appName>/blob/<*> 获取blob信息
type GittarBlobResponse struct {
	Header
	Data GittarBlobData `json:"data"`
}

// GittarBlobData blob响应数据
type GittarBlobData struct {
	Binary  bool   `json:"binary"`
	Content string `json:"content"`
}

// GittarMergeStatusRequest GET /<projectName>/<appName>/merge-stats 搜索指定ref下的文件
type GittarMergeStatusRequest struct {
	//源分支
	SourceBranch string `query:"sourceBranch"`

	//将要合并到的目标分支
	TargetBranch string `query:"targetBranch"`
}

// GittarMergeStatusResponse GET /<projectName>/<appName>/merge-stats merge状态检测
type GittarMergeStatusResponse struct {
	Header
	Data GittarMergeStatusData `json:"data"`
}

// GittarMergeStatusData mr状态响应数据
type GittarMergeStatusData struct {
	HasConflict bool   `json:"hasConflict"`
	IsMerged    bool   `json:"isMerged"`
	HasError    bool   `json:"hasError"`
	ErrorMsg    string `json:"errorMsg"`
}

// GittarCreateMergeRequest POST /<projectName>/<appName>/merge-requests 创建merge request
type GittarCreateMergeRequest struct {
	Title              string `json:"title"`
	Description        string `json:"description"`
	AssigneeID         string `json:"assigneeId"`
	SourceBranch       string `json:"sourceBranch"`
	TargetBranch       string `json:"targetBranch"`
	RemoveSourceBranch bool   `json:"removeSourceBranch"`
}

type MergeOperationTempBranchOperationType string

const JoinToTempBranch MergeOperationTempBranchOperationType = "joinToTempBranch"
const RemoveFromTempBranch MergeOperationTempBranchOperationType = "removeFromTempBranch"
const ReJoinAllBranchToTempBranch MergeOperationTempBranchOperationType = "reJoinAllBranchToTempBranch"

const JoinTempBranchSuccessStatus = "success"
const JoinTempBranchFailedStatus = "failed"

type GittarMergeOperationTempBranchRequest struct {
	MergeID              uint64 `json:"mergeID"`
	TempBranch           string `json:"tempBranch"`
	JoinTempBranchStatus string `json:"joinTempBranchStatus"`
	IsJoinTempBranch     *bool  `json:"isJoinTempBranch"`
}

type GittarMergeOperationTempBranchResponse struct {
	Header
}

type RepoCreateMrEvent struct {
	EventHeader
	Content MergeRequestInfo `json:"content"`
}

type MergeRequestInfo struct {
	Id                   int64        `json:"id"`
	RepoMergeId          int          `json:"mergeId"`
	AppID                int64        `json:"appId"`
	RepoID               int64        `json:"repoId"`
	Title                string       `json:"title"`
	AuthorId             string       `json:"authorId"`
	AuthorUser           *UserInfoDto `json:"authorUser"`
	Description          string       `json:"description"`
	AssigneeId           string       `json:"assigneeId"`
	AssigneeUser         *UserInfoDto `json:"assigneeUser"`
	MergeUserId          string       `json:"mergeUserId"`
	MergeUser            *UserInfoDto `json:"mergeUser"`
	CloseUserId          string       `json:"closeUserId"`
	CloseUser            *UserInfoDto `json:"closeUser"`
	SourceBranch         string       `json:"sourceBranch"`
	TargetBranch         string       `json:"targetBranch"`
	SourceSha            string       `json:"sourceSha"`
	TargetSha            string       `json:"targetSha"`
	RemoveSourceBranch   bool         `json:"removeSourceBranch"`
	State                string       `json:"state"`
	IsCheckRunValid      bool         `json:"isCheckRunValid"`
	TargetBranchRule     *ValidBranch `json:"targetBranchRule"`
	DefaultCommitMessage string       `json:"defaultCommitMessage"`
	CreatedAt            time.Time    `json:"createdAt"`
	UpdatedAt            *time.Time   `json:"updatedAt"`
	CloseAt              *time.Time   `json:"closeAt"`
	MergeAt              *time.Time   `json:"mergeAt"`
	Link                 string       `json:"link"`
	Score                int          `json:"score"`    //总评分
	ScoreNum             int          `json:"scoreNum"` // 评分人数
	RebaseBranch         string       `json:"rebaseBranch" default:"-"`
	EventName            string       `json:"eventName"`
	CheckRuns            CheckRuns    `json:"checkRuns,omitempty"`
	JoinTempBranchStatus string       `json:"joinTempBranchStatus"`
	IsJoinTempBranch     bool         `json:"isJoinTempBranch"`
}

type MergeStatusInfo struct {
	HasConflict bool   `json:"hasConflict"`
	IsMerged    bool   `json:"isMerged"`
	HasError    bool   `json:"hasError"`
	ErrorMsg    string `json:"errorMsg"`
}

// GittarCreateMergeResponse 创建mr响应
type GittarCreateMergeResponse struct {
	Header
	Data *MergeRequestInfo `json:"data"`
}

// GittarQueryMrRequest  GET /<projectName>/<appName>/merge-requests 查询MR列表
type GittarQueryMrRequest struct {
	//状态 open/closed/merged
	State string `query:"state"`
	// 查询title模糊匹配或者merge_id精确匹配
	Query string `query:"query"`
	//创建人
	AuthorId string `query:"authorId"`
	//分配人
	AssigneeId string `query:"assigneeId"`
	//评分
	Score int `query:"score"`
	//页数
	Page int `query:"pageNo"`
	//每页数量
	Size int `query:"pageSize"`
	// targetBranch
	TargetBranch string `query:"targetBranch"`
	// sourceBranch
	SourceBranch string `query:"sourceBranch"`
}

// GittarQueryMrResponse 查询mr响应
type GittarQueryMrResponse struct {
	Header
	Data QueryMergeRequestsData `json:"data"`
}

// QueryMergeRequestsData 查询mr响应数据
type QueryMergeRequestsData struct {
	List  []*MergeRequestInfo `json:"list"`
	Total int                 `json:"total"`
}

// GittarQueryMrDetailResponse  GET /<projectName>/<appName>/merge-requests 获取单个MR详情
type GittarQueryMrDetailResponse struct {
	Header
	Data MergeRequestInfo `json:"data"`
}

// GittarCreateTagRequest POST /<projectName>/<appName>/tags 创建tag
type GittarCreateTagRequest struct {
	Name    string `json:"name"`
	Message string `json:"message"`

	// 引用, branch/tag/commit
	Ref string `json:"ref"`
}

// GittarCreateTagResponse 创建tag响应
type GittarCreateTagResponse struct {
	Header
}

// DeleteEvent Gittar的删除事件
type DeleteEvent struct {
	Event     TemplateName `json:"event"`
	AppName   string       `json:"appName"`
	Name      string       `json:"name"`
	AppID     int64        `json:"appId"`
	ProjectID int64        `json:"projectId"`
}

// GittarDeleteResponse 删除响应
type GittarDeleteResponse struct {
	Header
	Data DeleteEvent `json:"data"`
}

// GittarCreateBranchRequest POST /<projectName>/<appName>/branches 创建分支
type GittarCreateBranchRequest struct {
	Name string `json:"name"`

	// 引用, branch/tag/commit
	Ref string `json:"ref"`
}

// GittarCreateBranchResponse 创建分支响应
type GittarCreateBranchResponse struct {
	Header
}

// GittarDeleteBranchResponse 删除分支响应
type GittarDeleteBranchResponse struct {
	Header
}

// GittarCreateCommitRequest POST /<projectName>/<appName>/commits 创建commit
type GittarCreateCommitRequest struct {
	Message string `json:"message"`

	//变更操作列表
	Actions []EditActionItem `json:"actions"`

	//更新到的分支
	Branch string `json:"branch"`
}

// GittarCreateCommitResponse 创建commit响应
type GittarCreateCommitResponse struct {
	Header
}

// GittarTreeSearchRequest GET /<projectName>/<appName>/tree-search 搜索指定ref下的文件
type GittarTreeSearchRequest struct {
	//支持引用名: branch/tag/commit
	Ref string `query:"ref"`

	//文件通配符 例如 *.workflow
	Pattern string `query:"pattern"`
}

// GittarTreeSearchResponse 文件搜索响应
type GittarTreeSearchResponse struct {
	Header
	Data []*TreeEntry `json:"data"`
}

// CreateRepoRequest 创建repo请求
type CreateRepoRequest struct {
	OrgID       int64          `json:"org_id"`
	ProjectID   int64          `json:"project_id"`
	AppID       int64          `json:"app_id"`
	OrgName     string         `json:"org_name"`
	ProjectName string         `json:"project_name"`
	AppName     string         `json:"app_name"`
	IsExternal  bool           `json:"is_external"`
	Config      *GitRepoConfig `json:"config"`

	// 是否锁定
	IsLocked bool `json:"isLocked"`
	//做仓库创建检测，不实际创建
	OnlyCheck bool `json:"check"`
}

// CreateRepoResponse 创建repo响应
type CreateRepoResponse struct {
	Header
	Data CreateRepoResponseData `json:"data"`
}

// CreateRepoResponseData 创建repo响应data
type CreateRepoResponseData struct {
	ID int64 `json:"id"`

	// 仓库相对路径
	RepoPath string `json:"repo_path"`
}

// UpdateRepoRequest 更新repo配置
type UpdateRepoRequest struct {
	AppID  int64          `json:"-"`
	Config *GitRepoConfig `json:"config"`
}

// LockedRepoRequest 仓库锁定请求
type LockedRepoRequest struct {
	AppID     int64  `json:"appId"`
	ProjectID int64  `json:"projectId"`
	IsLocked  bool   `json:"isLocked"`
	AppName   string `json:"appName"`
}

// LockedRepoResponse 仓库锁定响应
type LockedRepoResponse struct {
	Header
	Data LockedRepoRequest `json:"data"`
}

// UpdateRepoResponse 更新repo响应
type UpdateRepoResponse struct {
	Header
}

// DeleteRepoResponse 删除repo响应
type DeleteRepoResponse struct {
	Header
}

// RepoBranchEvent 分支事件
type RepoBranchEvent struct {
	EventHeader
	Content BranchInfo `json:"content"`
}

// Branch 分支
type Branch struct {
	ID        string  `json:"id"`
	Name      string  `json:"name"`
	Commit    *Commit `json:"commit"`
	IsDefault bool    `json:"isDefault"`
	IsProtect bool    `json:"isProtect"`
	IsMerged  bool    `json:"isMerged"`
}

// BranchInfo 分支详情
type BranchInfo struct {
	ID           string  `json:"id"`
	Name         string  `json:"name"`
	Commit       *Commit `json:"commit"`
	OperatorID   string  `json:"operatorId"`
	OperatorName string  `json:"operatorName"`
	Link         string  `json:"link"`
	EventName    string  `json:"eventName"`
}

// Commit commit
type Commit struct {
	ID            string     `json:"id"`
	Author        *Signature `json:"-"`
	Committer     *Signature `json:"committer"`
	CommitMessage string     `json:"commitMessage"`
	ParentSha     string     `json:"parentSha"`
}

// Signature git操作人结构
type Signature struct {
	Email string    `json:"email"`
	Name  string    `json:"name"`
	When  time.Time `json:"when"`
}

// RepoBranchEvent 分支事件
type RepoTagEvent struct {
	EventHeader
	Content TagInfo `json:"content"`
}

// Tag 标签
type Tag struct {
	Name    string     `json:"name"`
	ID      string     `json:"id"`
	Object  string     `json:"object"` // The id of this commit object
	Tagger  *Signature `json:"tagger"`
	Message string     `json:"message"`
}

// TagInfo 标签详情
type TagInfo struct {
	Name         string     `json:"name"`
	ID           string     `json:"id"`
	Object       string     `json:"object"` // The id of this commit object
	Tagger       *Signature `json:"tagger"`
	Message      string     `json:"message"`
	OperatorID   string     `json:"operatorId"`
	OperatorName string     `json:"operatorName"`
	Link         string     `json:"link"`
	EventName    string     `json:"eventName"`
}

// TreeEntry 文件结构
type TreeEntry struct {
	ID        string  `json:"id"`
	Type      string  `json:"type"`
	Name      string  `json:"name"`
	EntrySize int64   `json:"size"`
	Commit    *Commit `json:"commit"`
}

// Diff 对比数据
type Diff struct {
	FilesChanged  int         `json:"filesChanged"`
	TotalAddition int         `json:"totalAddition"`
	TotalDeletion int         `json:"totalDeletion"`
	Files         []*DiffFile `json:"files"`
	IsFinish      bool        `json:"isFinish"`
}

// DiffFile 单文件对比数据
type DiffFile struct {
	Name    string `json:"name"`
	OldName string `json:"oldName"`

	// 40-byte SHA, Changed/New: new SHA; Deleted: old SHA
	Index       string         `json:"index"`
	Addition    int            `json:"addition"`
	Deletion    int            `json:"deletion"`
	Type        string         `json:"type"`
	IsBin       bool           `json:"isBin"`
	IsSubmodule bool           `json:"isSubmodule"`
	Sections    []*DiffSection `json:"sections"`
	HasMore     bool           `json:"hasMore"`
	OldMode     string         `json:"oldMode"`
	NewMode     string         `json:"newMode"`
}

// DiffSection 对比块
type DiffSection struct {
	Lines []*DiffLine `json:"lines"`
}

// DiffLine 差异行
type DiffLine struct {
	OldLineNo int    `json:"oldLineNo"`
	NewLineNo int    `json:"newLineNo"`
	Type      string `json:"type"`
	Content   string `json:"content"`
}

// UserInfoDto 用户数据
type UserInfoDto struct {
	AvatarURL string      `json:"avatarUrl,omitempty"`
	Email     string      `json:"email,omitempty"`
	UserID    interface{} `json:"id,omitempty"`
	NickName  string      `json:"nickName,omitempty"`
	Phone     string      `json:"phone,omitempty"`
	RealName  string      `json:"realName,omitempty"`
	Username  string      `json:"username,omitempty"`
}

// EditActionItem 编辑操作
type EditActionItem struct {
	//支持操作 add/delete
	Action  string `json:"action"`
	Content string `json:"content"`
	Path    string `json:"path"`

	//支持类型 tree/blob
	PathType string `json:"pathType"`
}

// GittarMergeTemplatesResponse
type GittarMergeTemplatesResponse struct {
	Header
	Data MergeTemplatesResponseData `json:"data"`
}

// MergeTemplatesResponseData mr模板数据
type MergeTemplatesResponseData struct {
	//所在分支
	Branch string `json:"branch"`
	//模板所在目录
	Path string `json:"path"`
	//模板文件列表
	Names []string `json:"names"`
}

// GittarBlameResponse GET /<projectName>/<appName>/blame/*  blame响应
type GittarBlameResponse struct {
	Header
	Data []*Blame `json:"data"`
}

// Blame 单条Blame信息
type Blame struct {
	//起始行号
	StartLineNo int `json:"startLineNo"`
	//结束行号
	EndLineNo int `json:"endLineNo"`
	//提交commit
	Commit *Commit `json:"commit"`
}

// GittarRegisterHook POST /_system/hooks 请求
type GittarRegisterHookRequest struct {
	Name       string `json:"name"`
	URL        string `json:"url"`
	PushEvents bool   `json:"push_events"`
}

// GittarRegisterHookResponse POST /_system/hooks 响应
type GittarRegisterHookResponse struct {
	Header
	Data GittarRegisterHookResponseData `json:"data"`
}

// GittarRegisterHookResponseData POST /_system/hooks 响应数据
type GittarRegisterHookResponseData struct {
	Id   int64  `json:"id"`
	UUId string `json:"uuid"`
}

// GittarCommitsListResponse GET /<repo>/commits/<ref> 根据 branch 获取 commit 历史信息
type GittarCommitsListResponse struct {
	Header
	Data []Commit `json:"data"`
}

type GittarBranchDetailResponse struct {
	Header
	Data *BranchDetail `json:"data"`
}

type BranchDetail struct {
	Commit *Commit `json:"commit"`
}

type GitRepoConfig struct {
	// 类型, 支持类型:general
	Type string `json:"type"`
	// 仓库地址
	Url string `json:"url"`

	Desc string `json:"desc"`
	// 仓库用户名
	Username string `json:"username"`
	// 仓库密码
	Password string `json:"password"`
}

type CheckRunStatus string
type CheckRunResult string

const (
	CheckRunStatusCompleted  CheckRunStatus = "completed"
	CheckRunStatusInProgress CheckRunStatus = "progress"

	CheckRunResultSuccess   CheckRunResult = "success"
	CheckRunResultFailure   CheckRunResult = "failure"
	CheckRunResultCancelled CheckRunResult = "cancelled"
	CheckRunResultTimeout   CheckRunResult = "timeout"
)

// CheckRun
type CheckRun struct {
	ID int64 `json:"id"`
	// 检查任务名称 golang-lint/java-lint/api-test
	Name string `json:"name"`
	// Merge-Request ID
	MrID int64 `json:"mrId"`
	// 检查类型 CI
	Type string `json:"type"`
	// 外部系统 ID
	ExternalID string `json:"externalId"`
	// 提交commitID
	Commit string `json:"commit"`
	// 流水线 ID
	PipelineID string `json:"pipelineId"`
	// 运行状态 in_progress：进行中 completed：已完成
	Status CheckRunStatus `json:"status"`
	// 运行结果 success：成功 failed：失败 cancel：取消 timeout：超时
	Result CheckRunResult `json:"result"`
	Output string         `json:"output"`
	// 完成时间
	CompletedAt *time.Time
	// 创建时间
	CreatedAt time.Time `json:"createdAt"`
	// 仓库ID
	RepoID int64 `json:"repoId"`
}

type CheckRuns struct {
	CheckRun []*CheckRun    `json:"checkrun"`
	Result   CheckRunResult `json:"result"`
	Mergable bool           `json:"mergable"`
}

// CreateCheckRunResponse
type CreateCheckRunResponse struct {
	Header
	Data *CheckRun `json:"data"`
}

// QueryCheckRunRequest
type QueryCheckRunRequest struct {
	Commit string `query:"commit"`
}

// QueryCheckRunResponse
type QueryCheckRunResponse struct {
	Header
	Data []*CheckRun `json:"data"`
}

type UpdateMR struct {
	Header
	Data *MergeRequestInfo `json:"data"`
}

type CheckRunRequest struct {
	Path   string `json:"path"`
	MRID   int64  `json:"mrId"`
	Branch string `json:"branch"`
}

type MergeRequestCountRequest struct {
	AppIDs []uint64 `query:"appIDs"`
	State  string   `query:"state"`
}

type MergeRequestCountResponse struct {
	Header
	Data map[string]int `json:"data"`
}

type GittarArchiveRequest struct {
	Org         string `json:"org"`
	Project     string `json:"project"`
	Application string `json:"application"`
	Ref         string `json:"ref"`
}

type GittarMergeWithBranchRequest struct {
	SourceBranch string `json:"sourceBranch"`
	TargetBranch string `json:"targetBranch"`
	AppID        uint64 `json:"appID"`
}

type MergeWithBranchResponse struct {
	Header
	Data *Commit `json:"data"`
}

type GittarMergeBaseRequest struct {
	SourceBranch string `json:"sourceBranch"`
	TargetBranch string `json:"targetBranch"`
	AppID        uint64 `json:"appID"`
}

type MergeBaseResponse struct {
	Header
	Data *Commit `json:"data"`
}
