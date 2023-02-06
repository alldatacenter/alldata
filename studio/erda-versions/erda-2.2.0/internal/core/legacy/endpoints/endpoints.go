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

// Package endpoints 定义所有的 route handle.
package endpoints

import (
	"net/http"

	"github.com/aliyun/aliyun-oss-go-sdk/oss"
	"github.com/gorilla/schema"

	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/core/legacy/dao"
	"github.com/erda-project/erda/internal/core/legacy/services/activity"
	"github.com/erda-project/erda/internal/core/legacy/services/application"
	"github.com/erda-project/erda/internal/core/legacy/services/approve"
	"github.com/erda-project/erda/internal/core/legacy/services/audit"
	"github.com/erda-project/erda/internal/core/legacy/services/errorbox"
	"github.com/erda-project/erda/internal/core/legacy/services/filesvc"
	"github.com/erda-project/erda/internal/core/legacy/services/label"
	"github.com/erda-project/erda/internal/core/legacy/services/manual_review"
	"github.com/erda-project/erda/internal/core/legacy/services/mbox"
	"github.com/erda-project/erda/internal/core/legacy/services/member"
	"github.com/erda-project/erda/internal/core/legacy/services/notice"
	"github.com/erda-project/erda/internal/core/legacy/services/notify"
	"github.com/erda-project/erda/internal/core/legacy/services/org"
	"github.com/erda-project/erda/internal/core/legacy/services/permission"
	"github.com/erda-project/erda/internal/core/legacy/services/project"
	"github.com/erda-project/erda/internal/core/legacy/services/subscribe"
	"github.com/erda-project/erda/internal/core/legacy/services/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/i18n"
	"github.com/erda-project/erda/pkg/jsonstore"
	"github.com/erda-project/erda/pkg/jsonstore/etcd"
	"github.com/erda-project/erda/pkg/license"
	"github.com/erda-project/erda/pkg/ucauth"
)

// Endpoints 定义 endpoint 方法
type Endpoints struct {
	store              jsonstore.JsonStore
	etcdStore          *etcd.Store
	ossClient          *oss.Client
	db                 *dao.DBClient
	uc                 *ucauth.UCClient
	bdl                *bundle.Bundle
	org                *org.Org
	project            *project.Project
	approve            *approve.Approve
	app                *application.Application
	member             *member.Member
	ManualReview       *manual_review.ManualReview
	activity           *activity.Activity
	permission         *permission.Permission
	license            *license.License
	notifyGroup        *notify.NotifyGroup
	mbox               *mbox.MBox
	label              *label.Label
	notice             *notice.Notice
	queryStringDecoder *schema.Decoder
	audit              *audit.Audit
	errorbox           *errorbox.ErrorBox
	fileSvc            *filesvc.FileService
	user               *user.User
	subscribe          *subscribe.Subscribe
	tokenService       tokenpb.TokenServiceServer
}

type Option func(*Endpoints)

// New 创建 Endpoints 对象.
func New(options ...Option) *Endpoints {
	e := &Endpoints{}

	for _, op := range options {
		op(e)
	}

	return e
}

// WithOSSClient 配置OSS Client
func WithOSSClient(client *oss.Client) Option {
	return func(e *Endpoints) {
		e.ossClient = client
	}
}

// WithDBClient 配置 db
func WithDBClient(db *dao.DBClient) Option {
	return func(e *Endpoints) {
		e.db = db
	}
}

// WithBundle 配置 bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(e *Endpoints) {
		e.bdl = bdl
	}
}

// WithUCClient 配置 UC Client
func WithUCClient(uc *ucauth.UCClient) Option {
	return func(e *Endpoints) {
		e.uc = uc
	}
}

// WithJSONStore 配置 jsonstore
func WithJSONStore(store jsonstore.JsonStore) Option {
	return func(e *Endpoints) {
		e.store = store
	}
}

// WithEtcdStore 配置 etcdStore
func WithEtcdStore(etcdStore *etcd.Store) Option {
	return func(e *Endpoints) {
		e.etcdStore = etcdStore
	}
}

// WithOrg 配置 org service
func WithOrg(org *org.Org) Option {
	return func(e *Endpoints) {
		e.org = org
	}
}

// WithProject 配置 project service
func WithProject(project *project.Project) Option {
	return func(e *Endpoints) {
		e.project = project
	}
}

// WithApp 配置 app service
func WithApp(app *application.Application) Option {
	return func(e *Endpoints) {
		e.app = app
	}
}

// WithMember 配置 member service
func WithMember(member *member.Member) Option {
	return func(e *Endpoints) {
		e.member = member
	}
}

// WithManualReview 配置 ManualReview service
func WithManualReview(ManualReview *manual_review.ManualReview) Option {
	return func(e *Endpoints) {
		e.ManualReview = ManualReview
	}
}

// WithActivity 配置 activity service
func WithActivity(activity *activity.Activity) Option {
	return func(e *Endpoints) {
		e.activity = activity
	}
}

// WithPermission 配置 permission service
func WithPermission(permission *permission.Permission) Option {
	return func(e *Endpoints) {
		e.permission = permission
	}
}

// WithNotify 配置 notify group service
func WithNotify(notifyGroup *notify.NotifyGroup) Option {
	return func(e *Endpoints) {
		e.notifyGroup = notifyGroup
	}
}

// WithMBox 配置 mbox service
func WithMBox(mbox *mbox.MBox) Option {
	return func(e *Endpoints) {
		e.mbox = mbox
	}
}

// WithLicense 配置 license
func WithLicense(license *license.License) Option {
	return func(e *Endpoints) {
		e.license = license
	}
}

// WithLabel 配置 label
func WithLabel(label *label.Label) Option {
	return func(e *Endpoints) {
		e.label = label
	}
}

// WithNotice 设置 notice service
func WithNotice(notice *notice.Notice) Option {
	return func(e *Endpoints) {
		e.notice = notice
	}
}

func WithApprove(approve *approve.Approve) Option {
	return func(e *Endpoints) {
		e.approve = approve
	}
}

// WithQueryStringDecoder 配置 queryStringDecoder
func WithQueryStringDecoder(decoder *schema.Decoder) Option {
	return func(e *Endpoints) {
		e.queryStringDecoder = decoder
	}
}

// WithAudit 配置 Audit
func WithAudit(audit *audit.Audit) Option {
	return func(e *Endpoints) {
		e.audit = audit
	}
}

func WithErrorBox(errorbox *errorbox.ErrorBox) Option {
	return func(e *Endpoints) {
		e.errorbox = errorbox
	}
}

func WithFileSvc(svc *filesvc.FileService) Option {
	return func(e *Endpoints) {
		e.fileSvc = svc
	}
}

func WithUserSvc(svc *user.User) Option {
	return func(e *Endpoints) {
		e.user = svc
	}
}

func WithSubscribe(sub *subscribe.Subscribe) Option {
	return func(e *Endpoints) {
		e.subscribe = sub
	}
}

func WithTokenSvc(tokenService tokenpb.TokenServiceServer) Option {
	return func(e *Endpoints) {
		e.tokenService = tokenService
	}
}

// DBClient 获取db client
func (e *Endpoints) DBClient() *dao.DBClient {
	return e.db
}

// GetLocale 获取本地化资源
func (e *Endpoints) GetLocale(request *http.Request) *i18n.LocaleResource {
	return e.bdl.GetLocaleByRequest(request)
}

func (e *Endpoints) UserSvc() *user.User {
	return e.user
}

// Routes 返回 endpoints 的所有 endpoint 方法，也就是 route.
func (e *Endpoints) Routes() []httpserver.Endpoint {
	return []httpserver.Endpoint{
		// health check
		{Path: "/_api/health", Method: http.MethodGet, Handler: e.Health},

		// the interface of org
		{Path: "/api/orgs", Method: http.MethodPost, Handler: e.CreateOrg},
		{Path: "/api/orgs/{orgID}", Method: http.MethodPut, Handler: e.UpdateOrg},
		{Path: "/api/orgs/{idOrName}", Method: http.MethodGet, Handler: e.GetOrg},
		{Path: "/api/orgs/{idOrName}", Method: http.MethodDelete, Handler: e.DeleteOrg},
		{Path: "/api/orgs", Method: http.MethodGet, Handler: e.ListOrg},
		{Path: "/api/orgs/actions/list-public", Method: http.MethodGet, Handler: e.ListPublicOrg},
		// {Path: "/api/orgs/ingress/{orgID}/actions/update-ingress", Method: http.MethodGet, Handler: e.UpdateOrgIngress},
		{Path: "/api/orgs/actions/get-by-domain", Method: http.MethodGet, Handler: e.GetOrgByDomain},
		{Path: "/api/orgs/actions/switch", Method: http.MethodPost, Handler: e.ChangeCurrentOrg},
		{Path: "/api/orgs/actions/relate-cluster", Method: http.MethodPost, Handler: e.CreateOrgClusterRelation},
		{Path: "/api/orgs/clusters/relations", Method: http.MethodGet, Handler: e.ListOrgClusterRelation},
		{Path: "/api/orgs/{orgID}/actions/set-release-cross-cluster", Method: http.MethodPost, Handler: e.SetReleaseCrossCluster},
		{Path: "/api/orgs/actions/gen-verify-code", Method: http.MethodPost, Handler: e.GenVerifiCode},
		{Path: "/api/orgs/{orgID}/actions/set-notify-config", Method: http.MethodPost, Handler: e.SetNotifyConfig},
		{Path: "/api/orgs/{orgID}/actions/get-notify-config", Method: http.MethodGet, Handler: e.GetNotifyConfig},
		{Path: "/api/orgs/clusters/relations/{orgID}", Method: http.MethodGet, Handler: e.GetOrgClusterRelationsByOrg},
		{Path: "/api/clusters/actions/dereference", Method: http.MethodPut, Handler: e.DereferenceCluster},

		// the interface of project
		{Path: "/api/projects", Method: http.MethodPost, Handler: e.CreateProject},
		{Path: "/api/projects/{projectID}", Method: http.MethodPut, Handler: e.UpdateProject},
		{Path: "/api/projects/{projectID}", Method: http.MethodGet, Handler: e.GetProject},
		{Path: "/api/projects/{projectID}", Method: http.MethodDelete, Handler: e.DeleteProject},
		{Path: "/api/projects", Method: http.MethodGet, Handler: e.ListProject},
		{Path: "/api/projects/resource/{resourceType}/actions/list-usage-histogram", Method: http.MethodGet, Handler: e.ListProjectResourceUsage},
		{Path: "/api/projects/actions/list-my-projects", Method: http.MethodGet, Handler: e.ListMyProject},
		{Path: "/api/projects/actions/list-public-projects", Method: http.MethodGet, Handler: e.ListPublicProject},
		{Path: "/api/projects/actions/refer-cluster", Method: http.MethodGet, Handler: e.ReferCluster},
		{Path: "/api/projects/actions/get-project-functions", Method: http.MethodGet, Handler: e.GetFunctions},
		{Path: "/api/projects/actions/set-project-functions", Method: http.MethodPost, Handler: e.SetFunctions},
		{Path: "/api/projects/actions/update-active-time", Method: http.MethodPut, Handler: e.UpdateProjectActiveTime},
		{Path: "/api/projects/{projectID}/actions/get-ns-info", Method: http.MethodGet, Handler: e.GetNSInfo},
		{Path: "/api/projects/actions/list-my-projectIDs", Method: http.MethodGet, Handler: e.ListMyProjectIDs},
		{Path: "/api/projects/actions/list-by-states", Method: http.MethodGet, Handler: e.GetProjectListByStates},
		{Path: "/api/projects/actions/list-all", Method: http.MethodGet, Handler: e.GetAllProjects},
		{Path: "/api/projects/actions/get-projects-map", Method: http.MethodGet, Handler: e.GetModelProjectsMap},
		{Path: "/api/projects/{projectID}/workspaces/{workspace}/namespaces", Method: http.MethodGet, Handler: e.GetAllNamespaces},
		{Path: "/api/projects/{projectID}/workspaces/{workspace}/quota", Method: http.MethodGet, Handler: e.GetWorkspaceQuota},

		// interface of project workspace abilities CRDU
		{Path: "/api/project-workspace-abilities/{projectID}/{workspace}", Method: http.MethodGet, Handler: e.GetProjectWorkSpace},
		{Path: "/api/project-workspace-abilities", Method: http.MethodPost, Handler: e.CreateProjectWorkSpace},
		{Path: "/api/project-workspace-abilities", Method: http.MethodPut, Handler: e.UpdateProjectWorkSpace},
		{Path: "/api/project-workspace-abilities", Method: http.MethodDelete, Handler: e.DeleteProjectWorkSpace},

		// cmp dependencies
		{Path: "/api/projects-quota", Method: http.MethodGet, Handler: e.GetProjectQuota},
		{Path: "/api/projects-namespaces", Method: http.MethodGet, Handler: e.GetNamespacesBelongsTo},
		{Path: "/api/quota-records", Method: http.MethodGet, Handler: e.ListQuotaRecords},

		// the interface of application
		{Path: "/api/applications", Method: http.MethodPost, Handler: e.CreateApplication},
		{Path: "/api/applications/{applicationID}", Method: http.MethodPut, Handler: e.UpdateApplication},
		{Path: "/api/applications/{applicationID}", Method: http.MethodGet, Handler: e.GetApplication},
		{Path: "/api/applications/{applicationID}", Method: http.MethodDelete, Handler: e.DeleteApplication},
		{Path: "/api/applications", Method: http.MethodGet, Handler: e.ListApplication},
		{Path: "/api/applications/actions/list-my-applications", Method: http.MethodGet, Handler: e.ListMyApplication},
		{Path: "/api/applications/{applicationID}/actions/pin", Method: http.MethodPut, Handler: e.PinApplication},
		{Path: "/api/applications/{applicationID}/actions/unpin", Method: http.MethodPut, Handler: e.UnPinApplication},
		{Path: "/api/applications/actions/list-templates", Method: http.MethodGet, Handler: e.ListAppTemplates},
		{Path: "/api/applications/actions/count", Method: http.MethodGet, Handler: e.CountAppByProID},
		{Path: "/api/applications/actions/get-id-by-names", Method: http.MethodGet, Handler: e.GetAppIDByNames},

		// the interface of notice
		{Path: "/api/notices", Method: http.MethodPost, Handler: e.CreateNotice},
		{Path: "/api/notices/{id}", Method: http.MethodPut, Handler: e.UpdateNotice},
		{Path: "/api/notices/{id}/actions/publish", Method: http.MethodPut, Handler: e.PublishNotice},
		{Path: "/api/notices/{id}/actions/unpublish", Method: http.MethodPut, Handler: e.UnpublishNotice},
		{Path: "/api/notices/{id}", Method: http.MethodDelete, Handler: e.DeleteNotice},
		{Path: "/api/notices", Method: http.MethodGet, Handler: e.ListNotice},

		// the interface of member
		{Path: "/api/members", Method: http.MethodPost, Handler: e.CreateOrUpdateMember},
		{Path: "/api/members/actions/remove", Method: http.MethodPost, Handler: e.DeleteMember},
		{Path: "/api/members/actions/destroy", Method: http.MethodPost, Handler: e.DestroyMember},
		{Path: "/api/members", Method: http.MethodGet, Handler: e.ListMember},
		{Path: "/api/members/actions/get-by-token", Method: http.MethodGet, Handler: e.GetMemberByToken},
		{Path: "/api/members/actions/list-roles", Method: http.MethodGet, Handler: e.ListMemberRoles},
		{Path: "/api/members/actions/list-user-roles", Method: http.MethodGet, Handler: e.ListMemberRolesByUser},
		{Path: "/api/members/actions/get-all-organizational", Method: http.MethodGet, Handler: e.GetAllOrganizational},
		{Path: "/api/members/actions/update-userinfo", Method: http.MethodPut, Handler: e.UpdateMemberUserInfo},
		{Path: "/api/members/actions/create-by-invitecode", Method: http.MethodPost, Handler: e.CreateMemberByInviteCode},
		{Path: "/api/members/actions/list-labels", Method: http.MethodGet, Handler: e.ListMeberLabels}, // 成员标签
		{Path: "/api/members/actions/list-by-scopeID", Method: http.MethodGet, Handler: e.ListScopeManagersByScopeID},
		{Path: "/api/members/actions/count-by-only-scopeID", Method: http.MethodGet, Handler: e.CountMembersWithoutExtraByScope},
		{Path: "/api/members/actions/get-by-user-and-scope", Method: http.MethodGet, Handler: e.GetMemberByUserAndScope},

		// the interface of permission
		{Path: "/api/permissions", Method: http.MethodGet, Handler: e.ListScopeRole},
		{Path: "/api/permissions/actions/access", Method: http.MethodPost, Handler: e.ScopeRoleAccess},
		{Path: "/api/permissions/actions/check", Method: http.MethodPost, Handler: e.CheckPermission},
		{Path: "/api/permissions/actions/stateCheck", Method: http.MethodPost, Handler: e.StateCheckPermission},

		// the interface of license
		{Path: "/api/license", Method: http.MethodGet, Handler: e.GetLicense},

		// the interface of label
		{Path: "/api/labels", Method: http.MethodPost, Handler: e.CreateLabel},
		{Path: "/api/labels/{id}", Method: http.MethodDelete, Handler: e.DeleteLabel},
		{Path: "/api/labels/{id}", Method: http.MethodPut, Handler: e.UpdateLabel},
		{Path: "/api/labels/{id}", Method: http.MethodGet, Handler: e.GetLabel},
		{Path: "/api/labels", Method: http.MethodGet, Handler: e.ListLabel},
		{Path: "/api/labels/actions/list-by-projectID-and-names", Method: http.MethodGet, Handler: e.ListByNamesAndProjectID},
		{Path: "/api/labels/actions/list-by-ids", Method: http.MethodGet, Handler: e.ListLabelByIDs},

		// the interface of mbox
		{Path: "/api/mboxs", Method: http.MethodGet, Handler: e.QueryMBox},
		{Path: "/api/mboxs", Method: http.MethodPost, Handler: e.CreateMBox},
		{Path: "/api/mboxs/actions/stats", Method: http.MethodGet, Handler: e.GetMBoxStats},
		{Path: "/api/mboxs/actions/set-read", Method: http.MethodPost, Handler: e.SetMBoxReadStatus},
		{Path: "/api/mboxs/{mboxID}", Method: http.MethodGet, Handler: e.GetMBox},
		{Path: "/api/mboxs/actions/read-all", Method: http.MethodPost, Handler: e.OneClickRead},

		// the interface of error box
		{Path: "/api/task-error/actions/create", Method: http.MethodPost, Handler: e.CreateOrUpdateErrorLog},

		// the interface of review
		{Path: "/api/reviews/actions/list-launched-approval", Method: http.MethodGet, Handler: e.GetReviewsBySponsorId},
		{Path: "/api/reviews/actions/list-approved", Method: http.MethodGet, Handler: e.GetReviewsByUserId},
		{Path: "/api/reviews/actions/review/approve", Method: http.MethodPost, Handler: e.CreateReview},
		{Path: "/api/reviews/actions/authority", Method: http.MethodGet, Handler: e.GetAuthorityByUserId},
		{Path: "/api/reviews/actions/updateReview", Method: http.MethodPut, Handler: e.UpdateApproval},
		{Path: "/api/reviews/actions/user/create", Method: http.MethodPost, Handler: e.CreateReviewUser},
		{Path: "/api/reviews/actions/{taskId}", Method: http.MethodGet, Handler: e.GetReviewByTaskId},

		// the interface of activity
		{Path: "/api/activities", Method: http.MethodGet, Handler: e.ListActivity},

		// the interface of notify
		{Path: "/api/notifies", Method: http.MethodPost, Handler: e.CreateNotify},
		{Path: "/api/notifies", Method: http.MethodGet, Handler: e.QueryNotifies},
		{Path: "/api/notifies/{notifyID}", Method: http.MethodGet, Handler: e.GetNotify},
		{Path: "/api/notifies/{notifyID}", Method: http.MethodPut, Handler: e.UpdateNotify},
		{Path: "/api/notifies/{notifyID}", Method: http.MethodDelete, Handler: e.DeleteNotify},
		{Path: "/api/notifies/{notifyID}/actions/enable", Method: http.MethodPut, Handler: e.NotifyEnable},
		{Path: "/api/notifies/{notifyID}/actions/disable", Method: http.MethodPut, Handler: e.NotifyDisable},
		{Path: "/api/notify-sources", Method: http.MethodDelete, Handler: e.DeleteNotifySource},
		{Path: "/api/notify-items", Method: http.MethodGet, Handler: e.QueryNotifyItems},
		{Path: "/api/notify-items/{notifyItemID}", Method: http.MethodPut, Handler: e.UpdateNotifyItem},
		{Path: "/api/notify-histories", Method: http.MethodGet, Handler: e.QueryNotifyHistories},
		{Path: "/api/notify-histories", Method: http.MethodPost, Handler: e.CreateNotifyHistory},
		{Path: "/api/notifies/actions/search-by-source", Method: http.MethodGet, Handler: e.QueryNotifiesBySource},
		{Path: "/api/notifies/actions/fuzzy-query-by-source", Method: http.MethodGet, Handler: e.FuzzyQueryNotifiesBySource},

		// the interface of audit
		{Path: "/api/audits/actions/create", Method: http.MethodPost, Handler: e.CreateAudits},
		{Path: "/api/audits/actions/batch-create", Method: http.MethodPost, Handler: e.BatchCreateAudits},
		{Path: "/api/audits/actions/list", Method: http.MethodGet, Handler: e.ListAudits},
		{Path: "/api/audits/actions/setting", Method: http.MethodPut, Handler: e.PutAuditsSettings},
		{Path: "/api/audits/actions/setting", Method: http.MethodGet, Handler: e.GetAuditsSettings},
		{Path: "/api/audits/actions/export-excel", Method: http.MethodGet, WriterHandler: e.ExportExcelAudit},

		// the interface of approval
		{Path: "/api/approves", Method: http.MethodPost, Handler: e.CreateApprove},
		{Path: "/api/approves/{approveId}", Method: http.MethodPut, Handler: e.UpdateApprove},
		{Path: "/api/approves/{approveId}", Method: http.MethodGet, Handler: e.GetApprove},
		{Path: "/api/approves/actions/list-approves", Method: http.MethodGet, Handler: e.ListApproves},

		// the interface of file
		{Path: "/api/files", Method: http.MethodPost, Handler: e.UploadFile},
		{Path: "/api/files", Method: http.MethodGet, WriterHandler: e.DownloadFile},
		{Path: "/api/files/{uuid}", Method: http.MethodGet, WriterHandler: e.DownloadFile},
		{Path: "/api/files/{uuid}", Method: http.MethodHead, WriterHandler: e.HeadFile},
		{Path: "/api/files/{uuid}", Method: http.MethodDelete, Handler: e.DeleteFile},
		{Path: "/api/images/actions/upload", Method: http.MethodPost, Handler: e.UploadImage},

		// the interface of user
		{Path: "/api/users", Method: http.MethodGet, Handler: e.ListUser},
		{Path: "/api/users/current", Method: http.MethodGet, Handler: e.GetCurrentUser},
		{Path: "/api/users/actions/search", Method: http.MethodGet, Handler: e.SearchUser},
		{Path: "/api/users/actions/get-uc-user-id", Method: http.MethodGet, Handler: e.GetUcUserID},

		// the interface of subscribe
		{Path: "/api/subscribe", Method: http.MethodPost, Handler: e.Subscribe},
		{Path: "/api/subscribe", Method: http.MethodDelete, Handler: e.UnSubscribe},
		{Path: "/api/subscribe", Method: http.MethodGet, Handler: e.GetSubscribes},
	}
}
