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

// Package deployment 部署相关操作
package deployment

import (
	"context"
	"encoding/json"
	"fmt"
	"net/url"
	"strings"
	"time"
	"unicode/utf8"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"google.golang.org/grpc/metadata"

	"github.com/erda-project/erda-infra/pkg/transport"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda-proto-go/core/dicehub/release/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/internal/tools/orchestrator/events"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/servicegroup"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/addon"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/apierrors"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/environment"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/migration"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/resource"
	"github.com/erda-project/erda/pkg/crypto/encryption"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/strutil"
)

// Deployment 部署对象封装
type Deployment struct {
	db               *dbclient.DBClient
	evMgr            *events.EventManager
	bdl              *bundle.Bundle
	addon            *addon.Addon
	resource         *resource.Resource
	migration        *migration.Migration
	encrypt          *encryption.EnvEncrypt
	releaseSvc       pb.ReleaseServiceServer
	serviceGroupImpl servicegroup.ServiceGroup
	scheduler        *scheduler.Scheduler
	envConfig        *environment.EnvConfig
	clusterSvc       clusterpb.ClusterServiceServer
}

// Option 部署对象配置选项
type Option func(*Deployment)

// New 新建部署对象实例
func New(options ...Option) *Deployment {
	d := &Deployment{}
	for _, op := range options {
		op(d)
	}
	return d
}

// WithDBClient 配置 db client
func WithDBClient(db *dbclient.DBClient) Option {
	return func(d *Deployment) {
		d.db = db
	}
}

// WithEventManager 配置 EventManager
func WithEventManager(evMgr *events.EventManager) Option {
	return func(d *Deployment) {
		d.evMgr = evMgr
	}
}

// WithBundle 配置 bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(d *Deployment) {
		d.bdl = bdl
	}
}

// WithAddon 配置 addon service
func WithAddon(a *addon.Addon) Option {
	return func(d *Deployment) {
		d.addon = a
	}
}

// WithMigration 配置 Migration service
func WithMigration(m *migration.Migration) Option {
	return func(d *Deployment) {
		d.migration = m
	}
}

// WithEncrypt 配置 encrypt service
func WithEncrypt(encrypt *encryption.EnvEncrypt) Option {
	return func(d *Deployment) {
		d.encrypt = encrypt
	}
}

// WithResource 配置 Runtime service
func WithResource(resource *resource.Resource) Option {
	return func(d *Deployment) {
		d.resource = resource
	}
}

// WithReleaseSvc 配置 dicehub release service
func WithReleaseSvc(releaseSvc pb.ReleaseServiceServer) Option {
	return func(d *Deployment) {
		d.releaseSvc = releaseSvc
	}
}

func WithServiceGroup(serviceGroupImpl servicegroup.ServiceGroup) Option {
	return func(d *Deployment) {
		d.serviceGroupImpl = serviceGroupImpl
	}
}

func WithScheduler(scheduler *scheduler.Scheduler) Option {
	return func(d *Deployment) {
		d.scheduler = scheduler
	}
}

func WithEnvConfig(envConfig *environment.EnvConfig) Option {
	return func(d *Deployment) {
		d.envConfig = envConfig
	}
}

func WithClusterSvc(clusterSvc clusterpb.ClusterServiceServer) Option {
	return func(d *Deployment) {
		d.clusterSvc = clusterSvc
	}
}

func (d *Deployment) ContinueDeploy(deploymentID uint64) error {
	// prepare the context
	fsm := NewFSMContext(deploymentID, d.db, d.evMgr, d.bdl, d.addon, d.migration, d.encrypt, d.resource, d.releaseSvc, d.serviceGroupImpl, d.scheduler, d.envConfig, d.clusterSvc)
	if err := fsm.Load(); err != nil {
		return errors.Wrapf(err, "failed to load fsm, deployment: %d, (%v)", deploymentID, err)
	}
	if fsm.Deployment.SkipPushByOrch {
		return nil
	}
	if end, err := fsm.timeout(); end || err != nil {
		return err
	}
	if err := fsm.precheck(); err != nil {
		return fsm.failDeploy(err)
	}
	switch fsm.Deployment.Status {
	case apistructs.DeploymentStatusWaiting:
		return fsm.continueWaiting()
	case apistructs.DeploymentStatusDeploying:
		return fsm.continueDeploying()
	case apistructs.DeploymentStatusCanceling:
		return fsm.continueCanceling()
	default:
		return nil
	}
}

func (d *Deployment) CancelLastDeploy(runtimeID uint64, operator string, force bool) error {
	deployment, err := d.db.FindLastDeployment(runtimeID)
	if err != nil {
		return apierrors.ErrCancelDeployment.InternalError(err)
	}
	if deployment == nil {
		return apierrors.ErrCancelDeployment.NotFound()
	}
	fsm := NewFSMContext(deployment.ID, d.db, d.evMgr, d.bdl, d.addon, d.migration, d.encrypt, d.resource, d.releaseSvc, d.serviceGroupImpl, d.scheduler, d.envConfig, d.clusterSvc)
	if err := fsm.Load(); err != nil {
		return err
	}
	perm, err := d.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   operator,
		Scope:    apistructs.AppScope,
		ScopeID:  fsm.Runtime.ApplicationID,
		Resource: "runtime-" + strutil.ToLower(fsm.Runtime.Workspace),
		Action:   apistructs.OperateAction,
	})
	if err != nil {
		return apierrors.ErrCancelDeployment.InternalError(err)
	}
	if !perm.Access {
		return apierrors.ErrCancelDeployment.AccessDenied()
	}
	return fsm.doCancelDeploy(operator, force)
}

// ListOrg 查询部署记录(列出orgid下所有有权限的deployments)
func (d *Deployment) ListOrg(ctx context.Context, userID user.ID, orgID uint64, needFilterProjectRole bool,
	needApproval *bool, approvedBy *user.ID, operateUsers []string, approved *bool,
	approvalStatus *string, types []string, ids []uint64, page apistructs.PageInfo) (
	*apistructs.DeploymentDetailListData, error) {
	myapps, err := d.bdl.GetMyApps(userID.String(), orgID)
	if err != nil {
		return nil, apierrors.ErrListDeployment.InternalError(err)
	}
	myapps_ := []apistructs.ApplicationDTO{}
	if needFilterProjectRole {
		userrolelist, err := d.bdl.ListMemberRolesByUser(apistructs.ListMemberRolesByUserRequest{
			UserID:    userID.String(),
			ScopeType: "project",
			ParentID:  int64(orgID),
			PageSize:  9999,
		})
		if err != nil {
			return nil, apierrors.ErrListDeployment.InternalError(err)
		}
		projectlist := []int64{}
		for _, ur := range userrolelist.List {
			if strutil.Contains(strutil.Concat(ur.Roles...), "Owner", "Lead", "owner", "lead") {
				projectlist = append(projectlist, ur.ScopeID)
			}
		}
		for _, app := range myapps.List {
			found := false
			for _, proj := range projectlist {
				if proj == int64(app.ProjectID) {
					found = true
					break
				}
			}
			if found {
				myapps_ = append(myapps_, app)
			}
		}
	} else {
		myapps_ = myapps.List
	}
	allruntimeids := []uint64{}
	for _, app := range myapps_ {
		runtimes, err := d.db.FindRuntimesByAppId(app.ID)
		if err != nil {
			return nil, apierrors.ErrListDeployment.InternalError(err)
		}
		for _, r := range runtimes {
			allruntimeids = append(allruntimeids, r.ID)
		}
	}
	filter := dbclient.DeploymentFilter{
		StatusIn:     nil,
		NeedApproved: needApproval,
		Approved:     approved,
	}

	if approvedBy != nil {
		approvedBy_aux := string(*approvedBy)
		filter.ApprovedByUser = &approvedBy_aux
	}
	if len(operateUsers) > 0 {
		filter.OperateUsers = operateUsers
	}
	if len(types) > 0 {
		filter.Types = types
	}
	if len(ids) > 0 {
		filter.IDs = ids
	}
	if approvalStatus != nil {
		filter.ApprovalStatus = approvalStatus
	}
	deployments, total, err := d.db.FindMultiRuntimesDeployments(allruntimeids, filter, page.GetOffset(), page.GetLimit())
	if err != nil {
		return nil, apierrors.ErrListDeployment.InternalError(err)
	}

	list := make([]*apistructs.DeploymentWithDetail, 0, len(deployments))
	for i := range deployments {
		dd := apistructs.DeploymentWithDetail{}
		deploy := deployments[i].Convert()
		dd.Deployment = *deploy

		runtime, err := d.db.GetRuntime(deployments[i].RuntimeId)
		if err != nil {
			return nil, apierrors.ErrListDeployment.InternalError(err)
		}
		dd.RuntimeName = runtime.Name
		dd.BranchName = runtime.GitBranch
		app, err := d.bdl.GetApp(runtime.ApplicationID)
		if err != nil {
			return nil, apierrors.ErrListDeployment.InternalError(err)
		}
		dd.ApplicationID = app.ID
		dd.ApplicationName = app.Name
		proj, err := d.bdl.GetProject(app.ProjectID)
		if err != nil {
			return nil, apierrors.ErrListDeployment.InternalError(err)
		}
		dd.ProjectID = proj.ID
		dd.ProjectName = proj.Name

		ctx = transport.WithHeader(ctx, metadata.New(map[string]string{httputil.InternalHeader: "true"}))
		releaseResp, err := d.releaseSvc.GetRelease(ctx, &pb.ReleaseGetRequest{ReleaseID: deployments[i].ReleaseId})
		if err != nil {
			// release 可能已经过期清理，忽略该条
			continue
		}
		release := releaseResp.Data
		dd.CommitID = release.Labels["gitCommitId"]
		dd.CommitMessage = release.Labels["gitCommitMessage"]
		list = append(list, &dd)
	}
	data := apistructs.DeploymentDetailListData{
		Total: total,
		List:  list,
	}
	return &data, nil
}

// List 查询部署记录列表
func (d *Deployment) List(userID user.ID, orgID uint64, runtimeID uint64, statuses []string, page apistructs.PageInfo) (
	*apistructs.DeploymentListData, error) {
	runtime, err := d.db.GetRuntime(runtimeID)
	if err != nil {
		return nil, apierrors.ErrListDeployment.InternalError(err)
	}
	perm, err := d.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   userID.String(),
		Scope:    apistructs.AppScope,
		ScopeID:  runtime.ApplicationID,
		Resource: "runtime-" + strutil.ToLower(runtime.Workspace),
		Action:   apistructs.GetAction,
	})
	if err != nil {
		return nil, apierrors.ErrListDeployment.InternalError(err)
	}
	if !perm.Access {
		return nil, apierrors.ErrListDeployment.AccessDenied()
	}

	filter := dbclient.DeploymentFilter{StatusIn: statuses}
	deployments, _, err := d.db.FindDeployments(runtimeID, filter, page.GetOffset(), page.GetLimit())
	if err != nil {
		return nil, apierrors.ErrListDeployment.InternalError(err)
	}
	list := make([]*apistructs.Deployment, 0, len(deployments))
	for i := range deployments {
		list = append(list, deployments[i].Convert())
	}
	data := apistructs.DeploymentListData{
		Total: len(list),
		List:  list,
	}
	return &data, nil
}

// ListAllDeployments 查询所有部署记录列表
func (d *Deployment) ListAllDeployments(userID user.ID, orgID uint64, runtimeID uint64, statuses []string) (
	*apistructs.DeploymentListData, error) {
	runtime, err := d.db.GetRuntime(runtimeID)
	if err != nil {
		return nil, apierrors.ErrListDeployment.InternalError(err)
	}
	perm, err := d.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   userID.String(),
		Scope:    apistructs.AppScope,
		ScopeID:  runtime.ApplicationID,
		Resource: "runtime-" + strutil.ToLower(runtime.Workspace),
		Action:   apistructs.GetAction,
	})
	if err != nil {
		return nil, apierrors.ErrListDeployment.InternalError(err)
	}
	if !perm.Access {
		return nil, apierrors.ErrListDeployment.AccessDenied()
	}

	filter := dbclient.DeploymentFilter{StatusIn: statuses}
	deployments, err := d.db.FindAllDeployments(runtimeID, filter)
	if err != nil {
		return nil, apierrors.ErrListDeployment.InternalError(err)
	}
	list := make([]*apistructs.Deployment, 0, len(deployments))
	for i := range deployments {
		list = append(list, deployments[i].Convert())
	}
	data := apistructs.DeploymentListData{
		Total: len(list),
		List:  list,
	}
	return &data, nil
}

// GetStatus 查询部署状态
func (d *Deployment) GetStatus(deploymentID uint64) (*apistructs.DeploymentStatusDTO, error) {
	deployment, err := d.db.GetDeployment(deploymentID)
	if err != nil {
		return nil, apierrors.ErrGetDeployment.InternalError(err)
	}
	// TODO: not returns runtime info later
	runtime, err := d.db.GetRuntime(deployment.RuntimeId)
	if err != nil {
		return nil, apierrors.ErrGetDeployment.InternalError(err)
	}
	statusMap := map[string]string{}

	if deployment.NeedApproval {
		statusMap["approval_status"] = deployment.ApprovalStatus
		statusMap["approval_reason"] = deployment.ApprovalReason
		switch deployment.Status {
		case apistructs.DeploymentStatusCanceled:
			statusMap["approval_status"] = "Canceled"
		}
	}

	sg, err := d.serviceGroupImpl.InspectServiceGroupWithTimeout(runtime.ScheduleName.Namespace, runtime.ScheduleName.Name)

	var rt *apistructs.DeploymentStatusRuntimeDTO
	if deployment.Status == apistructs.DeploymentStatusOK {
		if err != nil {
			// TODO: we should not treat NOT_FOUND err as DEPLOYING
			// TODO: because scheduler return NOT_FOUND as an err
			return &apistructs.DeploymentStatusDTO{
				DeploymentID: deploymentID,
				Status:       apistructs.DeploymentStatusOK,
			}, nil
		}
		rt = convertDeploymentRuntimeDTO(sg)
	} else {
		if sg != nil {
			if sg.Status != apistructs.StatusReady && sg.Status != apistructs.StatusHealthy {
				for _, serviceItem := range sg.Services {
					statusMap[fmt.Sprintf("Error.Msg.%s", serviceItem.Name)] = serviceItem.LastMessage
					statusMap[fmt.Sprintf("Error.Reason.%s", serviceItem.Name)] = serviceItem.Reason
				}
				cc, _ := json.Marshal(statusMap)
				logrus.Errorf("InspectServiceGroupWithTimeout statusMap: %v", string(cc))
			}
		}
	}
	return &apistructs.DeploymentStatusDTO{
		DeploymentID: deploymentID,
		Status:       deployment.Status,
		Phase:        deployment.Phase,
		FailCause:    deployment.FailCause,
		ModuleErrMsg: statusMap,
		Runtime:      rt,
	}, nil
}

func (d *Deployment) Approve(userID user.ID, orgID uint64, deploymentID uint64, reject bool, reason string, referer string) error {
	deployment, err := d.db.GetDeployment(deploymentID)
	if err != nil {
		return apierrors.ErrApproveDeployment.InternalError(err)
	}
	if deployment.Status == apistructs.DeploymentStatusCanceled {
		return apierrors.ErrApproveDeployment.InvalidParameter(fmt.Errorf("该部署(%d)已被取消", deploymentID))
	}
	if deployment.Status != apistructs.DeploymentStatusWaitApprove {
		return apierrors.ErrApproveDeployment.InvalidParameter(fmt.Errorf("该部署(%d)已被审批过", deploymentID))
	}
	if !deployment.NeedApproval {
		return apierrors.ErrApproveDeployment.InvalidParameter(fmt.Errorf("deployment(%d) NeedApproval = false ", deploymentID))
	}
	if deployment.ApprovedAt != nil {
		return apierrors.ErrApproveDeployment.InvalidParameter(fmt.Errorf("该部署(%d)已被审批过", deploymentID))
	}
	if utf8.RuneCountInString(reason) > 100 {
		return apierrors.ErrApproveDeployment.InvalidParameter(fmt.Errorf("拒绝理由过长"))
	}

	deployment.Status = apistructs.DeploymentStatusWaiting
	now := time.Now()
	deployment.ApprovedAt = &now
	deployment.ApprovedByUser = userID.String()
	deployment.ApprovalReason = reason
	deployment.ApprovalStatus = "Accept"
	if reject {
		deployment.ApprovalStatus = "Reject"
		deployment.Status = apistructs.DeploymentStatusFailed
	}
	if err := d.db.UpdateDeployment(deployment); err != nil {
		return apierrors.ErrApproveDeployment.InternalError(err)
	}
	for range []int{0} {
		runtime, err := d.db.GetRuntime(deployment.RuntimeId)
		if err != nil {
			logrus.Errorf("failed to get runtime(%d): %v", deployment.RuntimeId, err)
			break
		}
		proj, err := d.bdl.GetProject(runtime.ProjectID)
		if err != nil {
			logrus.Errorf("failed to get project(%d): %v", proj.ID, err)
			break
		}
		app, err := d.bdl.GetApp(runtime.ApplicationID)
		if err != nil {
			logrus.Errorf("failed to get app(%d): %v", app.ID, err)
			break
		}
		user, err := d.bdl.ListUsers(apistructs.UserListRequest{
			UserIDs: []string{deployment.Operator},
		})
		if err != nil || len(user.Users) != 1 {
			logrus.Errorf("failed to get user(%s): %v", deployment.Operator, err)
			break
		}
		u, err := url.Parse(referer)
		if err != nil {
			logrus.Errorf("failed to parse referer(%s): %v", referer, err)
			break
		}

		url := fmt.Sprintf("%s://%s/workBench/approval/my-initiate/%s?id=%d",
			u.Scheme, u.Host, deployment.ApprovalStatus, deployment.ID)
		if err := d.bdl.CreateMboxNotify("notify.deployapproval.done.markdown_template",
			map[string]string{
				"title":       fmt.Sprintf("【通知】%s项目%s应用部署审核完成", proj.Name, app.Name),
				"projectName": proj.Name,
				"appName":     app.Name,
				"url":         url,
			}, "zh-CN", proj.OrgID, []string{user.Users[0].ID}); err != nil {
			logrus.Errorf("failed to CreateMboxNotify: %v", err)
		}
		if err := d.bdl.CreateEmailNotify("notify.deployapproval.done.markdown_template",
			map[string]string{
				"title":       fmt.Sprintf("【通知】%s项目%s应用部署审核完成", proj.Name, app.Name),
				"projectName": proj.Name,
				"appName":     app.Name,
				"url":         url,
			}, "zh-CN", proj.OrgID, []string{user.Users[0].Email}); err != nil {
			logrus.Errorf("failed to CreateEmailNotify: %v", err)
		}
	}
	return nil
}

// Deprecated
func convertDeploymentRuntimeDTO(group *apistructs.ServiceGroup) *apistructs.DeploymentStatusRuntimeDTO {
	var runtimeDTO apistructs.DeploymentStatusRuntimeDTO
	runtimeDTO.Endpoints = make(map[string]*apistructs.DeploymentStatusRuntimeServiceDTO)
	runtimeDTO.Services = make(map[string]*apistructs.DeploymentStatusRuntimeServiceDTO)
	for _, service := range group.Services {
		serviceDTO := apistructs.DeploymentStatusRuntimeServiceDTO{
			Host:  service.Vip,
			Ports: diceyml.ComposeIntPortsFromServicePorts(service.Ports),
		}
		vHosts := service.Labels["HAPROXY_0_VHOST"]
		if len(vHosts) > 0 {
			serviceDTO.PublicHosts = strings.Split(vHosts, ",")
			runtimeDTO.Endpoints[service.Name] = &serviceDTO
		} else {
			runtimeDTO.Services[service.Name] = &serviceDTO
		}
	}
	return &runtimeDTO
}
