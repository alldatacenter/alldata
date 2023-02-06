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

// Resource Role
const (
	ResourceRoleCreator  string = "CREATOR"
	ResourceRoleAssignee string = "ASSIGNEE"
	ResourceRoleOwner    string = "OWNER"
	ResourceRolePartner  string = "PARTNER"
)

// 权限资源集
const (
	OrgResource                string = "org"
	ResourceInfoResource       string = "resourceInfo"
	ProjectResource            string = "project"
	ProjectPublicResource      string = "project-public"
	ProjectTemplateResource    string = "project-template"
	ProjectPackageResource     string = "project-package"
	AppResource                string = "app"
	AppPublicResource          string = "app-public"
	MemberResource             string = "member"
	CloudAccountResource       string = "cloudaccount"
	CloudResourceResource      string = "cloudresource"
	UserManageResource         string = "usermanage" // 用户管理
	ClusterResource            string = "cluster"
	EdgeSiteResource           string = "edgesite"
	HostResource               string = "host"
	NotifyResource             string = "notify"
	TicketResource             string = "ticket"
	SubscribeResource          string = "subscribe"
	IterationResource          string = "iteration"
	IssueRequirementResource   string = "issue-requirement"
	IssueTaskResource          string = "issue-task"
	IssueBugResource           string = "issue-bug"
	IssueTicketResource        string = "issue-ticket"
	IssueEpicResource          string = "issue-epic"
	IssueTypeResource          string = "issue-type"
	IssueStateResource         string = "issue-state"
	IssueImportResource        string = "issue-import"
	IssuePanelResource         string = "issue-panel"
	PublisherResource          string = "publisher"
	PmpResource                string = "pmp"
	NoticeResource             string = "notice"
	CertificateResource        string = "certificate"
	ApproveResource            string = "approve"
	QuoteCertificateResource   string = "quote-certificate"
	LibReferenceResource       string = "libReference"
	ConfigResource             string = "config"
	TestPlanResource           string = "testplan"
	TestPlanV2Resource         string = "testplanV2"
	TestPlanUsecaseRelResource string = "testplanCaseRel"
	TestSpaceResource          string = "autotestSpace"
	PipelineResource           string = "pipeline"
	NormalBranchResource       string = "normalBranch"
	ProtectedBranchResource    string = "protectedBranch"
	AuditResource              string = "audit"
	ProjectFunctionResource    string = "projectFunction"
	NotifyConfigResource       string = "notify-config"
	AutotestSceneResource      string = "autotest-scene"
	SceneSetResource           string = "sceneset"
	CustomAddonResource        string = "customAddon"
)

// 权限操作集
const (
	CreateAction  string = "CREATE"
	UpdateAction  string = "UPDATE"
	DeleteAction  string = "DELETE"
	GetAction     string = "GET"
	ReadAction    string = "READ"
	ListAction    string = "LIST"
	OperateAction string = "OPERATE"
	OtherAction   string = "OTHER"
)

// ScopeRole 权限
type ScopeRole struct {
	Scope  Scope    `json:"scope"`
	Access bool     `json:"access"`
	Roles  []string `json:"roles"`
}

// ScopeRoleAccessRequest Request for API `POST /api/permissions/actions/access`
type ScopeRoleAccessRequest struct {
	Scope Scope `json:"scope"`
}

// ScopeRoleAccessResponse Response for API `POST /api/permissions/actions/access`
type ScopeRoleAccessResponse struct {
	Header
	Data ScopeRole `json:"data"`
}

// ScopeRoleListResponse Response for API `GET /api/permissions`
type ScopeRoleListResponse struct {
	Header
	Data ScopeRoleList `json:"data"`
}

// ScopeRoleList 权限列表
type ScopeRoleList struct {
	List []ScopeRole `json:"list"`
}

// PermissionCheckRequest 鉴权请求
type PermissionCheckRequest struct {
	UserID string `json:"userID"`
	// Scope 可选值: org/project/app
	Scope ScopeType `json:"scope"`
	// ScopeID scope具体值
	ScopeID uint64 `json:"scopeID"`
	// Resource 资源类型， eg: ticket/release
	Resource string `json:"resource"`
	// Action Create/Update/Delete/
	Action string `json:"action"`
	// resource 角色: Creator, Assignee
	ResourceRole string `json:"resourceRole"`
}

// PermissionCheckResponse 鉴权响应
type PermissionCheckResponse struct {
	Header
	Data PermissionCheckResponseData `json:"data"`
}

// PermissionCheckResponseData 鉴权响应数据
type PermissionCheckResponseData struct {
	Access bool `json:"access"`
}

// StatePermissionCheckResponse 鉴权响应
type StatePermissionCheckResponse struct {
	Header
	Data StatePermissionCheckResponseData `json:"data"`
}

// StatePermissionCheckResponseData 鉴权响应数据
type StatePermissionCheckResponseData struct {
	Access bool     `json:"access"`
	Roles  []string `json:"roles"`
}

// ScopeResource Scope 对应的权限信息
type ScopeResource struct {
	// Resource 资源类型， eg: ticket/release
	Resource string `json:"resource"`
	// Action Create/Update/Delete/
	Action string `json:"action"`
	// resource 角色: Creator, Assignee
	ResourceRole string `json:"resourceRole,omitempty"`
}

// PermissionList Scope 对应的权限列表
type PermissionList struct {
	Access           bool            `json:"access"`
	Roles            []string        `json:"roles"`
	PermissionList   []ScopeResource `json:"permissionList"`
	ResourceRoleList []ScopeResource `json:"resourceRoleList"`

	// 当项目/应用被删除时，鉴权为false，用于告诉前端是被删除了
	Exist bool `json:"exist"`

	// 无权限（access=false）时，该字段返回联系人 ID 列表，例如无应用权限时，返回应用管理员列表
	ContactsWhenNoPermission []string   `json:"contactsWhenNoPermission,omitempty"`
	ScopeInfo                *ScopeInfo `json:"scopeInfo"`
}

type ScopeInfo struct {
	ProjectName string `json:"projectName"`
	AppName     string `json:"appName"`
}

// PermissionListResponse 权限列表响应信息
type PermissionListResponse struct {
	Header
	Data PermissionList `json:"data"`
}
