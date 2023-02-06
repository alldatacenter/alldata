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

// Package runtime 应用实例相关操作
package runtime

import (
	"context"
	"encoding/json"
	"fmt"
	"math"
	"net/url"
	"runtime/debug"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/gorilla/schema"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"google.golang.org/grpc/metadata"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda-infra/pkg/transport"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda-proto-go/core/dicehub/release/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/pkg/diceworkspace"
	"github.com/erda-project/erda/internal/pkg/gitflowutil"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/internal/tools/orchestrator/events"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/clusterinfo"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/servicegroup"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/addon"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/apierrors"
	"github.com/erda-project/erda/internal/tools/orchestrator/spec"
	"github.com/erda-project/erda/internal/tools/orchestrator/utils"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/strutil"
)

// Runtime 应用实例对象封装
type Runtime struct {
	db               *dbclient.DBClient
	evMgr            *events.EventManager
	bdl              *bundle.Bundle
	addon            *addon.Addon
	releaseSvc       pb.ReleaseServiceServer
	serviceGroupImpl servicegroup.ServiceGroup
	clusterinfoImpl  clusterinfo.ClusterInfo
	clusterSvc       clusterpb.ClusterServiceServer
}

// Option 应用实例对象配置选项
type Option func(*Runtime)

// New 新建应用实例 service
func New(options ...Option) *Runtime {
	r := &Runtime{}
	for _, op := range options {
		op(r)
	}
	return r
}

// WithDBClient 配置 db client
func WithDBClient(db *dbclient.DBClient) Option {
	return func(r *Runtime) {
		r.db = db
	}
}

// WithEventManager 配置 EventManager
func WithEventManager(evMgr *events.EventManager) Option {
	return func(r *Runtime) {
		r.evMgr = evMgr
	}
}

// WithBundle 配置 bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(r *Runtime) {
		r.bdl = bdl
	}
}

// WithAddon 配置 addon service
func WithAddon(a *addon.Addon) Option {
	return func(r *Runtime) {
		r.addon = a
	}
}

// WithReleaseSvc 配置 dicehub release service
func WithReleaseSvc(svc pb.ReleaseServiceServer) Option {
	return func(r *Runtime) {
		r.releaseSvc = svc
	}
}

// WithServiceGroup 配置 serviceGroupImpl
func WithServiceGroup(serviceGroupImpl servicegroup.ServiceGroup) Option {
	return func(r *Runtime) {
		r.serviceGroupImpl = serviceGroupImpl
	}
}

func WithClusterInfo(clusterinfo clusterinfo.ClusterInfo) Option {
	return func(r *Runtime) {
		r.clusterinfoImpl = clusterinfo
	}
}

func WithClusterSvc(clusterSvc clusterpb.ClusterServiceServer) Option {
	return func(r *Runtime) {
		r.clusterSvc = clusterSvc
	}
}

func (r *Runtime) CreateByReleaseIDPipeline(ctx context.Context, orgid uint64, operator user.ID, releaseReq *apistructs.RuntimeReleaseCreateRequest) (*apistructs.RuntimeDeployDTO, error) {
	ctx = transport.WithHeader(ctx, metadata.New(map[string]string{httputil.InternalHeader: "true"}))
	releaseResp, err := r.releaseSvc.GetRelease(ctx, &pb.ReleaseGetRequest{ReleaseID: releaseReq.ReleaseID})
	if err != nil {
		return nil, err
	}
	app, err := r.bdl.GetApp(releaseReq.ApplicationID)
	if err != nil {
		return nil, err
	}
	workspaces := strutil.Split(releaseReq.Workspace, ",", true)
	commitid := releaseResp.Data.Labels["gitCommitId"]
	branch := releaseResp.Data.Labels["gitBranch"]

	// check if there is a runtime already being created by release
	pipelines, err := utils.FindCRBRRunningPipeline(releaseReq.ApplicationID, workspaces[0],
		fmt.Sprintf("dice-deploy-release-%s", branch), r.bdl)
	if err != nil {
		return nil, err
	}
	if len(pipelines) != 0 {
		return nil,
			errors.Errorf("There is already a runtime created by releaseID %s, please do not repeat deployment", releaseReq.ReleaseID)
	}

	yml := utils.GenCreateByReleasePipelineYaml(releaseReq.ReleaseID, workspaces)
	b, err := yaml.Marshal(yml)
	if err != nil {
		errstr := fmt.Sprintf("failed to marshal pipelineyml: %v", err)
		logrus.Errorf(errstr)
		return nil, err
	}
	detail := apistructs.CommitDetail{
		CommitID: "",
		Repo:     app.GitRepo,
		RepoAbbr: app.GitRepoAbbrev,
		Author:   "",
		Email:    "",
		Time:     nil,
		Comment:  "",
	}
	if commitid != "" {
		commit, err := r.bdl.GetGittarCommit(app.GitRepoAbbrev, commitid, string(operator))
		if err != nil {
			return nil, err
		}
		detail = apistructs.CommitDetail{
			CommitID: commitid,
			Repo:     app.GitRepo,
			RepoAbbr: app.GitRepoAbbrev,
			Author:   commit.Committer.Name,
			Email:    commit.Committer.Email,
			Time:     &commit.Committer.When,
			Comment:  commit.CommitMessage,
		}
	}
	commitdetail, err := json.Marshal(detail)
	if err != nil {
		return nil, err
	}
	dto, err := r.bdl.CreatePipeline(&apistructs.PipelineCreateRequestV2{
		IdentityInfo: apistructs.IdentityInfo{UserID: operator.String()},
		PipelineYml:  string(b),
		Labels: map[string]string{
			apistructs.LabelBranch:        releaseResp.Data.ReleaseName,
			apistructs.LabelOrgID:         strconv.FormatUint(orgid, 10),
			apistructs.LabelProjectID:     strconv.FormatUint(releaseReq.ProjectID, 10),
			apistructs.LabelAppID:         strconv.FormatUint(releaseReq.ApplicationID, 10),
			apistructs.LabelDiceWorkspace: releaseReq.Workspace,
			apistructs.LabelCommitDetail:  string(commitdetail),
			apistructs.LabelAppName:       app.Name,
			apistructs.LabelProjectName:   app.ProjectName,
		},
		PipelineYmlName: fmt.Sprintf("dice-deploy-release-%s-%d-%s", releaseReq.Workspace,
			releaseReq.ApplicationID, branch),
		ClusterName:    releaseResp.Data.ClusterName,
		PipelineSource: apistructs.PipelineSourceDice,
		AutoRunAtOnce:  true,
	})
	if err != nil {
		return nil, err
	}
	return convertRuntimeDeployDto(app, releaseResp.Data, dto)
}

// Create 创建应用实例
func (r *Runtime) CreateByReleaseID(ctx context.Context, operator user.ID, releaseReq *apistructs.RuntimeReleaseCreateRequest) (*apistructs.DeploymentCreateResponseDTO, error) {
	ctx = transport.WithHeader(ctx, metadata.New(map[string]string{httputil.InternalHeader: "true"}))
	releaseResp, err := r.releaseSvc.GetRelease(ctx, &pb.ReleaseGetRequest{ReleaseID: releaseReq.ReleaseID})
	if err != nil {
		return nil, err
	}
	if releaseReq == nil {
		return nil, errors.Errorf("releaseId does not exist")
	}
	if releaseReq.ProjectID != uint64(releaseResp.Data.ProjectID) {
		return nil, errors.Errorf("release does not correspond to the project")
	}
	if releaseReq.ApplicationID != uint64(releaseResp.Data.ApplicationID) {
		return nil, errors.Errorf("release does not correspond to the application")
	}
	branchWorkspaces, err := r.bdl.GetAllValidBranchWorkspace(releaseReq.ApplicationID, string(operator))
	if err != nil {
		return nil, apierrors.ErrCreateRuntime.InternalError(err)
	}
	_, validArtifactWorkspace := gitflowutil.IsValidBranchWorkspace(branchWorkspaces, apistructs.DiceWorkspace(releaseReq.Workspace))
	if !validArtifactWorkspace {
		return nil, errors.Errorf("release does not correspond to the workspace")
	}

	projectInfo, err := r.bdl.GetProject(releaseReq.ProjectID)
	if err != nil {
		return nil, err
	}
	if projectInfo == nil {
		return nil, errors.Errorf("The project is illegal")
	}

	wsCluster, ok := projectInfo.ClusterConfig[releaseReq.Workspace]
	if !ok {
		return nil, fmt.Errorf("workspace corresponding cluster is empty")
	}
	var targetClusterName string
	// 跨集群部署
	// 部署的目标集群，默认情况下为 release 所属的集群，跨集群部署时，部署到项目环境对应的集群
	if releaseResp.Data.CrossCluster {
		targetClusterName = wsCluster
	} else {
		// 在制品所属集群部署
		// 校验制品所属集群和环境对应集群是否相同
		if releaseResp.Data.ClusterName != wsCluster {
			return nil, fmt.Errorf("release does not correspond to the cluster")
		}
		targetClusterName = releaseResp.Data.ClusterName
	}

	var req apistructs.RuntimeCreateRequest
	req.ClusterName = targetClusterName
	req.Name = releaseResp.Data.ReleaseName
	req.Operator = operator.String()
	req.Source = "RELEASE"
	req.ReleaseID = releaseReq.ReleaseID
	req.SkipPushByOrch = false

	var extra apistructs.RuntimeCreateRequestExtra
	extra.OrgID = uint64(releaseResp.Data.OrgID)
	extra.ProjectID = uint64(releaseResp.Data.ProjectID)
	extra.ApplicationID = uint64(releaseResp.Data.ApplicationID)
	extra.ApplicationName = releaseResp.Data.ApplicationName
	extra.Workspace = releaseReq.Workspace
	extra.DeployType = "RELEASE"
	req.Extra = extra

	return r.Create(operator, &req)
}

// Create 创建应用实例
func (r *Runtime) Create(operator user.ID, req *apistructs.RuntimeCreateRequest) (
	*apistructs.DeploymentCreateResponseDTO, error) {
	// TODO: 需要等 pipeline action 调用走内网后，再从 header 中取 User-ID (operator)
	// TODO: should not assign like this
	//req.Operator = operator.String()
	if err := checkRuntimeCreateReq(req); err != nil {
		return nil, apierrors.ErrCreateRuntime.InvalidParameter(err)
	}
	var appID uint64
	if req.Source == apistructs.ABILITY {
		return nil, apierrors.ErrCreateRuntime.InvalidParameter("end support: ABILITY")
	} else {
		// appID already bean checked
		appID = req.Extra.ApplicationID
	}
	app, err := r.bdl.GetApp(appID)
	if err != nil {
		// TODO: shall minimize unknown error
		return nil, apierrors.ErrCreateRuntime.InternalError(err)
	}
	// TODO 暂时封闭
	//if app.Mode == string(apistructs.ApplicationModeLibrary) {
	//	return nil, apierrors.ErrCreateRuntime.InvalidParameter("Non-business applications cannot be published.")
	//}

	resource := apistructs.NormalBranchResource
	rules, err := r.bdl.GetProjectBranchRules(app.ProjectID)
	if err != nil {
		return nil, apierrors.ErrCreateRuntime.InternalError(err)
	}
	if diceworkspace.GetValidBranchByGitReference(req.Name, rules).IsProtect {
		resource = apistructs.ProtectedBranchResource
	}
	perm, err := r.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   operator.String(),
		Scope:    apistructs.AppScope,
		ScopeID:  app.ID,
		Resource: resource,
		Action:   apistructs.OperateAction,
	})
	if err != nil {
		return nil, apierrors.ErrCreateRuntime.InternalError(err)
	}
	if !perm.Access {
		return nil, apierrors.ErrCreateRuntime.AccessDenied()
	}
	ctx := transport.WithHeader(context.Background(), metadata.New(map[string]string{httputil.InternalHeader: "cmp"}))
	resp, err := r.clusterSvc.GetCluster(ctx, &clusterpb.GetClusterRequest{IdOrName: req.ClusterName})
	if err != nil {
		return nil, apierrors.ErrCreateRuntime.InvalidState(fmt.Sprintf("cluster: %v not found", req.ClusterName))
	}
	cluster := resp.Data

	// build runtimeUniqueId
	uniqueID := spec.RuntimeUniqueId{ApplicationId: appID, Workspace: req.Extra.Workspace, Name: req.Name}

	// prepare runtime
	// TODO: we do not need RepoAbbrev
	runtime, created, err := r.db.FindRuntimeOrCreate(uniqueID, req.Operator, req.Source, req.ClusterName,
		uint64(cluster.Id), app.GitRepoAbbrev, req.Extra.ProjectID, app.OrgID, req.DeploymentOrderId, req.ReleaseVersion, req.ExtraParams)
	if err != nil {
		return nil, apierrors.ErrCreateRuntime.InternalError(err)
	}
	if created {
		// emit runtime add event
		event := events.RuntimeEvent{
			EventName: events.RuntimeCreated,
			Operator:  req.Operator,
			Runtime:   dbclient.ConvertRuntimeDTO(runtime, app),
		}
		r.evMgr.EmitEvent(&event)
	}

	// find last deployment
	last, err := r.db.FindLastDeployment(runtime.ID)
	if err != nil {
		return nil, apierrors.ErrCreateRuntime.InternalError(err)
	}
	if last != nil && IsDeploying(last.Status) {
		return nil, apierrors.ErrCreateRuntime.InvalidState("正在部署中，请不要重复部署")
	}
	deploytype := "BUILD"
	if req.Extra.DeployType == "RELEASE" {
		deploytype = "RELEASE"
	}
	deployContext := DeployContext{
		Runtime:           runtime,
		App:               app,
		LastDeployment:    last,
		ReleaseID:         req.ReleaseID,
		Operator:          req.Operator,
		BuildID:           req.Extra.BuildID,
		DeployType:        deploytype,
		AddonActions:      req.Extra.AddonActions,
		InstanceID:        req.Extra.InstanceID.String(),
		SkipPushByOrch:    req.SkipPushByOrch,
		Param:             req.Param,
		DeploymentOrderId: req.DeploymentOrderId,
	}

	return r.doDeployRuntime(&deployContext)
}

// StopRuntime scale 0 服务
func (r *Runtime) StopRuntime(operator user.ID, orgID uint64, runtimeID uint64) (*apistructs.DeploymentCreateResponseDTO, error) {
	runtime, err := r.db.GetRuntime(runtimeID)
	if err != nil {
		return nil, apierrors.ErrDeployRuntime.InternalError(err)
	}
	app, err := r.bdl.GetApp(runtime.ApplicationID)
	if err != nil {
		return nil, apierrors.ErrDeployRuntime.InternalError(err)
	}
	deployment, err := r.db.FindLastDeployment(runtimeID)
	if err != nil {
		return nil, apierrors.ErrDeployRuntime.InternalError(err)
	}
	if deployment == nil {
		// it will happen, but it often implicit some errors
		return nil, apierrors.ErrDeployRuntime.InvalidState("last deployment not found")
	}
	if deployment.ReleaseId == "" {
		return nil, apierrors.ErrDeployRuntime.InvalidState("抱歉，检测到不兼容的部署任务，请去重新构建")
	}
	switch deployment.Status {
	case apistructs.DeploymentStatusWaitApprove, apistructs.DeploymentStatusInit, apistructs.DeploymentStatusWaiting, apistructs.DeploymentStatusDeploying:
		// we do not cancel, just report error
		return nil, apierrors.ErrDeployRuntime.InvalidState("正在部署中，请不要重复部署")
	}

	deployContext := DeployContext{
		Runtime:        runtime,
		App:            app,
		LastDeployment: deployment,
		ReleaseID:      deployment.ReleaseId,
		Operator:       operator.String(),
		Scale0:         true,
	}
	return r.doDeployRuntime(&deployContext)
}

func (r *Runtime) RedeployPipeline(ctx context.Context, operator user.ID, orgID uint64, runtimeID uint64) (*apistructs.RuntimeDeployDTO, error) {
	runtime, err := r.db.GetRuntime(runtimeID)
	if err != nil {
		return nil, err
	}
	yml := utils.GenRedeployPipelineYaml(runtimeID)
	app, err := r.bdl.GetApp(runtime.ApplicationID)
	if err != nil {
		return nil, err
	}
	deployment, err := r.db.FindLastDeployment(runtimeID)
	if err != nil {
		return nil, err
	}
	ctx = transport.WithHeader(ctx, metadata.New(map[string]string{httputil.InternalHeader: "true"}))
	releaseResp, err := r.releaseSvc.GetRelease(ctx, &pb.ReleaseGetRequest{ReleaseID: deployment.ReleaseId})
	if err != nil {
		return nil, err
	}
	commitid := releaseResp.Data.Labels["gitCommitId"]
	detail := apistructs.CommitDetail{
		CommitID: "",
		Repo:     app.GitRepo,
		RepoAbbr: app.GitRepoAbbrev,
		Author:   "",
		Email:    "",
		Time:     nil,
		Comment:  "",
	}
	if commitid != "" {
		commit, err := r.bdl.GetGittarCommit(app.GitRepoAbbrev, commitid, string(operator))
		if err != nil {
			return nil, err
		}
		detail = apistructs.CommitDetail{
			CommitID: commitid,
			Repo:     app.GitRepo,
			RepoAbbr: app.GitRepoAbbrev,
			Author:   commit.Committer.Name,
			Email:    commit.Committer.Email,
			Time:     &commit.Committer.When,
			Comment:  commit.CommitMessage,
		}
	}
	commitdetail, err := json.Marshal(detail)
	if err != nil {
		return nil, err
	}
	b, err := yaml.Marshal(yml)
	if err != nil {
		errstr := fmt.Sprintf("failed to marshal pipelineyml: %v", err)
		logrus.Errorf(errstr)
		return nil, err
	}
	if err := r.setClusterName(runtime); err != nil {
		logrus.Errorf("get cluster info failed, cluster name: %s, error: %v", runtime.ClusterName, err)
	}
	dto, err := r.bdl.CreatePipeline(&apistructs.PipelineCreateRequestV2{
		IdentityInfo: apistructs.IdentityInfo{UserID: operator.String()},
		PipelineYml:  string(b),
		Labels: map[string]string{
			apistructs.LabelBranch:        runtime.Name,
			apistructs.LabelOrgID:         strconv.FormatUint(orgID, 10),
			apistructs.LabelProjectID:     strconv.FormatUint(runtime.ProjectID, 10),
			apistructs.LabelAppID:         strconv.FormatUint(runtime.ApplicationID, 10),
			apistructs.LabelDiceWorkspace: runtime.Workspace,
			apistructs.LabelCommitDetail:  string(commitdetail),
			apistructs.LabelAppName:       app.Name,
			apistructs.LabelProjectName:   app.ProjectName,
		},
		PipelineYmlName: getRedeployPipelineYmlName(*runtime),
		ClusterName:     runtime.ClusterName,
		PipelineSource:  apistructs.PipelineSourceDice,
		AutoRunAtOnce:   true,
	})
	if err != nil {
		return nil, err
	}

	return convertRuntimeDeployDto(app, releaseResp.Data, dto)
}

func (r *Runtime) setClusterName(rt *dbclient.Runtime) error {
	clusterInfo, err := r.clusterinfoImpl.Info(rt.ClusterName)
	if err != nil {
		logrus.Errorf("get cluster info failed, cluster name: %s, error: %v", rt.ClusterName, err)
		return err
	}
	jobCluster := clusterInfo.Get(apistructs.JOB_CLUSTER)
	if jobCluster != "" {
		rt.ClusterName = jobCluster
	}
	return nil
}

// Redeploy 重新部署
func (r *Runtime) Redeploy(operator user.ID, orgID uint64, runtimeID uint64) (*apistructs.DeploymentCreateResponseDTO, error) {
	runtime, err := r.db.GetRuntime(runtimeID)
	if err != nil {
		return nil, apierrors.ErrDeployRuntime.InternalError(err)
	}
	app, err := r.bdl.GetApp(runtime.ApplicationID)
	if err != nil {
		return nil, apierrors.ErrDeployRuntime.InternalError(err)
	}
	perm, err := r.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   operator.String(),
		Scope:    apistructs.AppScope,
		ScopeID:  app.ID,
		Resource: "runtime-" + strutil.ToLower(runtime.Workspace),
		Action:   apistructs.OperateAction,
	})
	if err != nil {
		return nil, apierrors.ErrDeployRuntime.InternalError(err)
	}
	if !perm.Access {
		return nil, apierrors.ErrDeployRuntime.AccessDenied()
	}
	deployment, err := r.db.FindLastDeployment(runtimeID)
	if err != nil {
		return nil, apierrors.ErrDeployRuntime.InternalError(err)
	}
	if deployment == nil {
		// it will happen, but it often implicit some errors
		return nil, apierrors.ErrDeployRuntime.InvalidState("last deployment not found")
	}
	if deployment.ReleaseId == "" {
		return nil, apierrors.ErrDeployRuntime.InvalidState("抱歉，检测到不兼容的部署任务，请去重新构建")
	}
	switch deployment.Status {
	case apistructs.DeploymentStatusWaitApprove, apistructs.DeploymentStatusInit, apistructs.DeploymentStatusWaiting, apistructs.DeploymentStatusDeploying:
		// we do not cancel, just report error
		return nil, apierrors.ErrDeployRuntime.InvalidState("正在部署中，请不要重复部署")
	}

	deployContext := DeployContext{
		Runtime:        runtime,
		App:            app,
		LastDeployment: deployment,
		DeployType:     "REDEPLOY",
		ReleaseID:      deployment.ReleaseId,
		Operator:       operator.String(),
		SkipPushByOrch: false,
	}
	return r.doDeployRuntime(&deployContext)
}

// TODO: the response should be apistructs.RuntimeDTO
func (r *Runtime) doDeployRuntime(ctx *DeployContext) (*apistructs.DeploymentCreateResponseDTO, error) {
	// fetch & parse diceYml
	dice, err := r.bdl.GetDiceYAML(ctx.ReleaseID, ctx.Runtime.Workspace)
	if err != nil {
		return nil, err
	}

	// build runtimeUniqueId
	uniqueID := spec.RuntimeUniqueId{
		ApplicationId: ctx.Runtime.ApplicationID,
		Workspace:     ctx.Runtime.Workspace,
		Name:          ctx.Runtime.Name,
	}

	// prepare pre deployments
	pre, err := r.db.FindPreDeploymentOrCreate(uniqueID, dice)
	if err != nil {
		return nil, apierrors.ErrDeployRuntime.InternalError(err)
	}
	diceYmlObj := dice.Obj()
	if pre.DiceOverlay != "" {
		var overlay diceyml.Object
		err = json.Unmarshal([]byte(pre.DiceOverlay), &overlay)
		if err != nil {
			return nil, apierrors.ErrDeployRuntime.InternalError(err)
		}
		utils.ApplyOverlay(diceYmlObj, &overlay)
	}
	if ctx.Scale0 {
		var scaleValue = 0
		for _, v := range diceYmlObj.Services {
			v.Deployments.Replicas = scaleValue
		}
	}

	// do sync RuntimeService table after dice.yml changed
	err = r.syncRuntimeServices(ctx.Runtime.ID, dice)
	if err != nil {
		return nil, apierrors.ErrDeployRuntime.InternalError(err)
	}

	// double check last deployment not active
	if ctx.LastDeployment != nil {
		switch ctx.LastDeployment.Status {
		case apistructs.DeploymentStatusWaitApprove, apistructs.DeploymentStatusInit, apistructs.DeploymentStatusWaiting, apistructs.DeploymentStatusDeploying:
			return nil, apierrors.ErrDeployRuntime.InvalidState("正在部署中，请不要重复部署")
		}
	}

	images := make(map[string]string)
	for name, s := range dice.Obj().Services {
		images[name] = s.Image
	}
	// check all services has it's image
	for name := range diceYmlObj.Services {
		if images[name] == "" {
			errMsg := fmt.Sprintf("bad release(%s), no image exist for service: %s",
				ctx.ReleaseID, name)
			return nil, apierrors.ErrDeployRuntime.InvalidState(errMsg)
		}
	}
	imageJSONByte, err := json.Marshal(images)
	if err != nil {
		return nil, apierrors.ErrDeployRuntime.InternalError(err)
	}
	imageJSON := string(imageJSONByte)
	diceJSONByte, err := json.Marshal(diceYmlObj)
	if err != nil {
		return nil, apierrors.ErrDeployRuntime.InternalError(err)
	}
	diceJSON := string(diceJSONByte)

	// 检查是否处于封网状态
	blocked, err := r.checkOrgDeployBlocked(ctx.Runtime.OrgID, ctx.Runtime)
	if err != nil {
		return nil, apierrors.ErrRollbackRuntime.InternalError(err)
	}
	status := apistructs.DeploymentStatusWaiting
	reason := ""
	needApproval := false
	branchrules, err := r.bdl.GetProjectBranchRules(ctx.Runtime.ProjectID)
	if err != nil {
		return nil, apierrors.ErrDeployRuntime.InternalError(err)
	}
	branch := diceworkspace.GetValidBranchByGitReference(ctx.Runtime.Name, branchrules)
	if blocked {
		status = apistructs.DeploymentStatusFailed
		reason = "企业封网中,无法部署"
	} else {
		// 检查 branchrule 来判断是否需要审批
		if branch.NeedApproval {
			status = apistructs.DeploymentStatusWaitApprove
			needApproval = true
		}
	}
	deployment := dbclient.Deployment{
		RuntimeId:         ctx.Runtime.ID,
		Status:            status,
		Phase:             "INIT",
		FailCause:         reason,
		Operator:          ctx.Operator,
		ReleaseId:         ctx.ReleaseID,
		BuildId:           ctx.BuildID,
		Dice:              diceJSON,
		Type:              ctx.DeployType,
		DiceType:          1,
		BuiltDockerImages: imageJSON,
		NeedApproval:      needApproval,
		ApprovalStatus:    map[bool]string{true: "WaitApprove", false: ""}[needApproval],
		SkipPushByOrch:    ctx.SkipPushByOrch,
		Param:             ctx.Param,
		DeploymentOrderId: ctx.DeploymentOrderId,
	}
	if err := r.db.CreateDeployment(&deployment); err != nil {
		return nil, apierrors.ErrDeployRuntime.InternalError(err)
	}

	// 发送 审批站内信
	if !blocked && branch.NeedApproval {
		for range []int{0} {
			approvers, err := r.bdl.ListMembers(apistructs.MemberListRequest{
				ScopeType: "project",
				ScopeID:   int64(ctx.Runtime.ProjectID),
				Roles:     []string{"Owner", "Lead"},
				PageSize:  99,
			})
			if err != nil {
				logrus.Errorf("failed to listmembers: %v", err)
				break
			}
			approverIDs := []string{}
			emails := []string{}
			for _, approver := range approvers {
				approverIDs = append(approverIDs, approver.UserID)
				emails = append(emails, approver.Email)
			}

			memberlist, err := r.bdl.ListUsers(apistructs.UserListRequest{
				UserIDs: []string{deployment.Operator},
			})
			if err != nil {
				logrus.Errorf("failed to listuser(%s): %v", deployment.Operator, err)
				break
			}
			if len(memberlist.Users) == 0 {
				break
			}
			member := memberlist.Users[0].Name
			proj, err := r.bdl.GetProject(ctx.Runtime.ProjectID)
			if err != nil {
				logrus.Errorf("failed to get project(%d): %v", ctx.Runtime.ProjectID, err)
				break
			}
			app, err := r.bdl.GetApp(ctx.Runtime.ApplicationID)
			if err != nil {
				logrus.Errorf("failed to get app(%d): %v", ctx.Runtime.ApplicationID, err)
				break
			}
			d, err := r.clusterinfoImpl.Info(ctx.Runtime.ClusterName)
			if err != nil {
				logrus.Errorf("failed to QueryClusterInfo: %v", err)
			}
			protocols := strutil.Split(d.Get(apistructs.DICE_PROTOCOL), ",")
			protocol := "https"
			if len(protocols) > 0 {
				protocol = protocols[0]
			}
			domain := d.Get(apistructs.DICE_ROOT_DOMAIN)
			org, err := r.bdl.GetOrg(ctx.Runtime.OrgID)
			if err != nil {
				logrus.Errorf("failed to getorg(%v):%v", ctx.Runtime.OrgID, err)
				break
			}

			url := fmt.Sprintf("%s://%s-org.%s/workBench/approval/my-approve/pending?id=%d",
				protocol, org.Name, domain, deployment.ID)
			if err := r.bdl.CreateMboxNotify("notify.deployapproval.launch.markdown_template",
				map[string]string{
					"title":       fmt.Sprintf("【重要】请及时审核%s项目%s应用部署合规性", proj.Name, app.Name),
					"member":      member,
					"projectname": proj.Name,
					"appName":     app.Name,
					"url":         url,
				},
				"zh-CN", ctx.Runtime.OrgID, approverIDs); err != nil {
				logrus.Errorf("failed to CreateMboxNotify: %v", err)
			}
			if err := r.bdl.CreateEmailNotify("notify.deployapproval.launch.markdown_template",
				map[string]string{
					"title":       fmt.Sprintf("【重要】请及时审核%s项目%s应用部署合规性", proj.Name, app.Name),
					"member":      member,
					"projectname": proj.Name,
					"appName":     app.Name,
					"url":         url,
				},
				"zh-CN", ctx.Runtime.OrgID, emails); err != nil {
				logrus.Errorf("failed to CreateEmailNotify: %v", err)
			}
		}
	}

	// emit runtime deploy start event
	event := events.RuntimeEvent{
		EventName:  events.RuntimeDeployStart,
		Operator:   ctx.Operator,
		Runtime:    dbclient.ConvertRuntimeDTO(ctx.Runtime, ctx.App),
		Deployment: deployment.Convert(),
	}
	r.evMgr.EmitEvent(&event)

	// TODO: the response should be apistructs.RuntimeDTO
	return &apistructs.DeploymentCreateResponseDTO{
		DeploymentID:  deployment.ID,
		ApplicationID: ctx.Runtime.ApplicationID, // TODO: will all runtime has applicationId ?
		RuntimeID:     ctx.Runtime.ID,
	}, nil
}

func (r *Runtime) checkOrgDeployBlocked(orgID uint64, runtime *dbclient.Runtime) (bool, error) {
	org, err := r.bdl.GetOrg(orgID)
	if err != nil {
		return false, err
	}
	blocked := false
	switch runtime.Workspace {
	case "DEV":
		blocked = org.BlockoutConfig.BlockDEV
	case "TEST":
		blocked = org.BlockoutConfig.BlockTEST
	case "STAGING":
		blocked = org.BlockoutConfig.BlockStage
	case "PROD":
		blocked = org.BlockoutConfig.BlockProd
	}
	if blocked {
		app, err := r.bdl.GetApp(runtime.ApplicationID)
		if err != nil {
			return false, err
		}
		if app.UnBlockStart == nil || app.UnBlockEnd == nil {
			return true, nil
		}
		now := time.Now()
		if app.UnBlockStart.Before(now) && app.UnBlockEnd.After(now) {
			return false, nil
		}
	}
	return blocked, nil
}
func (r *Runtime) RollbackPipeline(ctx context.Context, operator user.ID, orgID uint64, runtimeID uint64, deploymentID uint64) (
	*apistructs.RuntimeDeployDTO, error) {
	runtime, err := r.db.GetRuntime(runtimeID)
	if err != nil {
		return nil, err
	}
	yml := apistructs.PipelineYml{
		Version: "1.1",
		Stages: [][]*apistructs.PipelineYmlAction{
			{{
				Type:    "dice-deploy-rollback",
				Alias:   "dice-deploy-rollback",
				Version: "1.0",
				Params: map[string]interface{}{
					"runtime_id":    strconv.FormatUint(runtimeID, 10),
					"deployment_id": strconv.FormatUint(deploymentID, 10),
				},
			}},
			{{
				Type:    "dice-deploy-addon",
				Version: "1.0",
				Params: map[string]interface{}{
					"deployment_id": "${dice-deploy-rollback:OUTPUT:deployment_id}",
				},
			}},
			{{
				Type:    "dice-deploy-service",
				Version: "1.0",
				Params: map[string]interface{}{
					"deployment_id": "${dice-deploy-rollback:OUTPUT:deployment_id}",
				},
			}},
			{{
				Type:    "dice-deploy-domain",
				Version: "1.0",
				Params: map[string]interface{}{
					"deployment_id": "${dice-deploy-rollback:OUTPUT:deployment_id}",
				},
			}},
		},
	}
	b, err := yaml.Marshal(yml)
	if err != nil {
		errstr := fmt.Sprintf("failed to marshal pipelineyml: %v", err)
		logrus.Errorf(errstr)
		return nil, err
	}
	app, err := r.bdl.GetApp(runtime.ApplicationID)
	if err != nil {
		return nil, err
	}
	deployment, err := r.db.GetDeployment(deploymentID)
	if err != nil {
		return nil, err
	}
	ctx = transport.WithHeader(ctx, metadata.New(map[string]string{httputil.InternalHeader: "true"}))
	releaseResp, err := r.releaseSvc.GetRelease(ctx, &pb.ReleaseGetRequest{ReleaseID: deployment.ReleaseId})
	if err != nil {
		return nil, err
	}
	commitid := releaseResp.Data.Labels["gitCommitId"]
	detail := apistructs.CommitDetail{
		CommitID: "",
		Repo:     app.GitRepo,
		RepoAbbr: app.GitRepoAbbrev,
		Author:   "",
		Email:    "",
		Time:     nil,
		Comment:  "",
	}
	if commitid != "" {
		commit, err := r.bdl.GetGittarCommit(app.GitRepoAbbrev, commitid, string(operator))
		if err != nil {
			return nil, err
		}
		detail = apistructs.CommitDetail{
			CommitID: commitid,
			Repo:     app.GitRepo,
			RepoAbbr: app.GitRepoAbbrev,
			Author:   commit.Committer.Name,
			Email:    commit.Committer.Email,
			Time:     &commit.Committer.When,
			Comment:  commit.CommitMessage,
		}
	}
	commitdetail, err := json.Marshal(detail)
	if err != nil {
		return nil, err
	}
	dto, err := r.bdl.CreatePipeline(&apistructs.PipelineCreateRequestV2{
		IdentityInfo: apistructs.IdentityInfo{UserID: operator.String()},
		PipelineYml:  string(b),
		Labels: map[string]string{
			apistructs.LabelBranch:        runtime.Name,
			apistructs.LabelOrgID:         strconv.FormatUint(orgID, 10),
			apistructs.LabelProjectID:     strconv.FormatUint(runtime.ProjectID, 10),
			apistructs.LabelAppID:         strconv.FormatUint(runtime.ApplicationID, 10),
			apistructs.LabelDiceWorkspace: runtime.Workspace,
			apistructs.LabelCommitDetail:  string(commitdetail),
			apistructs.LabelAppName:       app.Name,
			apistructs.LabelProjectName:   app.ProjectName,
		},
		PipelineYmlName: fmt.Sprintf("dice-deploy-rollback-%d", runtime.ID),
		ClusterName:     runtime.ClusterName,
		PipelineSource:  apistructs.PipelineSourceDice,
		AutoRunAtOnce:   true,
	})
	if err != nil {
		return nil, err
	}
	return convertRuntimeDeployDto(app, releaseResp.Data, dto)
}

func (r *Runtime) Rollback(operator user.ID, orgID uint64, runtimeID uint64, deploymentID uint64) (
	*apistructs.DeploymentCreateResponseDTO, error) {
	runtime, err := r.db.GetRuntime(runtimeID)
	if err != nil {
		return nil, apierrors.ErrRollbackRuntime.InternalError(err)
	}
	app, err := r.bdl.GetApp(runtime.ApplicationID)
	if err != nil {
		return nil, err
	}
	perm, err := r.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   operator.String(),
		Scope:    apistructs.AppScope,
		ScopeID:  app.ID,
		Resource: "runtime-" + strutil.ToLower(runtime.Workspace),
		Action:   apistructs.OperateAction,
	})
	if err != nil {
		return nil, apierrors.ErrRollbackRuntime.InternalError(err)
	}
	if !perm.Access {
		return nil, apierrors.ErrRollbackRuntime.AccessDenied()
	}
	// find last deployment
	last, err := r.db.FindLastDeployment(runtime.ID)
	if err != nil {
		return nil, apierrors.ErrRollbackRuntime.InternalError(err)
	}
	if last != nil {
		switch last.Status {
		case apistructs.DeploymentStatusWaitApprove, apistructs.DeploymentStatusInit, apistructs.DeploymentStatusWaiting, apistructs.DeploymentStatusDeploying:
			// we do not cancel, just report error
			return nil, apierrors.ErrRollbackRuntime.InvalidState("正在部署中，请不要重复部署")
		}
	}
	rollbackTo, err := r.db.GetDeployment(deploymentID)
	if err != nil {
		return nil, apierrors.ErrRollbackRuntime.InternalError(err)
	}
	if rollbackTo.Status != apistructs.DeploymentStatusOK {
		return nil, apierrors.ErrRollbackRuntime.InvalidState("回滚到的部署单未成功")
	}

	// 检查是否处于封网状态
	blocked, err := r.checkOrgDeployBlocked(orgID, runtime)
	if err != nil {
		return nil, apierrors.ErrRollbackRuntime.InternalError(err)
	}
	status := apistructs.DeploymentStatusWaiting
	reason := ""
	needApproval := false
	branchrules, err := r.bdl.GetProjectBranchRules(runtime.ProjectID)
	if err != nil {
		return nil, apierrors.ErrDeployRuntime.InternalError(err)
	}
	branch := diceworkspace.GetValidBranchByGitReference(runtime.Name, branchrules)

	if blocked {
		status = apistructs.DeploymentStatusFailed
		reason = "企业封网中,无法部署"
	} else {
		// 检查 branchrule 来判断是否需要审批
		if branch.NeedApproval {
			status = apistructs.DeploymentStatusWaitApprove
			needApproval = true
		}
	}
	deployment := dbclient.Deployment{
		RuntimeId:         runtime.ID,
		Status:            status,
		Phase:             "INIT",
		FailCause:         reason,
		Operator:          operator.String(),
		ReleaseId:         rollbackTo.ReleaseId,
		Dice:              rollbackTo.Dice,
		Type:              "REDEPLOY",
		BuildId:           0,
		BuiltDockerImages: rollbackTo.BuiltDockerImages,
		NeedApproval:      needApproval,
		ApprovalStatus:    map[bool]string{true: "WaitApprove", false: ""}[needApproval],
		SkipPushByOrch:    false,
		Param:             rollbackTo.Param,
		DeploymentOrderId: rollbackTo.DeploymentOrderId,
	}
	if err := r.db.CreateDeployment(&deployment); err != nil {
		return nil, apierrors.ErrRollbackRuntime.InternalError(err)
	}
	if !blocked && branch.NeedApproval {
		for range []int{0} {
			approvers, err := r.bdl.ListMembers(apistructs.MemberListRequest{
				ScopeType: "project",
				ScopeID:   int64(runtime.ProjectID),
				Roles:     []string{"Owner", "Lead"},
				PageSize:  99,
			})
			if err != nil {
				logrus.Errorf("failed to listmembers: %v", err)
				break
			}
			approverIDs := []string{}
			emails := []string{}
			for _, approver := range approvers {
				approverIDs = append(approverIDs, approver.UserID)
				emails = append(emails, approver.Email)
			}

			memberlist, err := r.bdl.ListUsers(apistructs.UserListRequest{
				UserIDs: []string{deployment.Operator},
			})
			if err != nil {
				logrus.Errorf("failed to listuser(%s): %v", deployment.Operator, err)
				break
			}
			if len(memberlist.Users) == 0 {
				break
			}
			member := memberlist.Users[0].Name
			proj, err := r.bdl.GetProject(runtime.ProjectID)
			if err != nil {
				logrus.Errorf("failed to get project(%d): %v", runtime.ProjectID, err)
				break
			}
			app, err := r.bdl.GetApp(runtime.ApplicationID)
			if err != nil {
				logrus.Errorf("failed to get app(%d): %v", runtime.ApplicationID, err)
				break
			}

			d, err := r.clusterinfoImpl.Info(runtime.ClusterName)
			if err != nil {
				logrus.Errorf("failed to QueryClusterInfo: %v", err)
			}
			protocols := strutil.Split(d.Get(apistructs.DICE_PROTOCOL), ",")
			protocol := "https"
			if len(protocols) > 0 {
				protocol = protocols[0]
			}
			domain := d.Get(apistructs.DICE_ROOT_DOMAIN)
			org, err := r.bdl.GetOrg(runtime.OrgID)
			if err != nil {
				logrus.Errorf("failed to getorg(%v):%v", runtime.OrgID, err)
				break
			}

			url := fmt.Sprintf("%s://%s-org.%s/workBench/approval/my-approve/pending?id=%d",
				protocol, org.Name, domain, deployment.ID)
			if err := r.bdl.CreateMboxNotify("notify.deployapproval.launch.markdown_template",
				map[string]string{
					"title":       fmt.Sprintf("【重要】请及时审核%s项目%s应用部署合规性", proj.Name, app.Name),
					"member":      member,
					"projectname": proj.Name,
					"appName":     app.Name,
					"url":         url,
				},
				"zh-CN", runtime.OrgID, approverIDs); err != nil {
				logrus.Errorf("failed to CreateMboxNotify: %v", err)
			}
			if err := r.bdl.CreateEmailNotify("notify.deployapproval.launch.markdown_template",
				map[string]string{
					"title":       fmt.Sprintf("【重要】请及时审核%s项目%s应用部署合规性", proj.Name, app.Name),
					"member":      member,
					"projectname": proj.Name,
					"appName":     app.Name,
					"url":         url,
				},
				"zh-CN", runtime.OrgID, emails); err != nil {
				logrus.Errorf("failed to CreateEmailNotify: %v", err)
			}
		}
	}
	// emit runtime deploy start event
	event := events.RuntimeEvent{
		EventName:  events.RuntimeDeployStart,
		Operator:   operator.String(),
		Runtime:    dbclient.ConvertRuntimeDTO(runtime, app),
		Deployment: deployment.Convert(),
	}
	r.evMgr.EmitEvent(&event)

	return &apistructs.DeploymentCreateResponseDTO{
		DeploymentID:  deployment.ID,
		ApplicationID: runtime.ApplicationID, // TODO: will all runtime has applicationId ?
		RuntimeID:     runtime.ID,
	}, nil
}

// Delete 标记应用实例删除
func (r *Runtime) Delete(operator user.ID, orgID uint64, runtimeID uint64) (*apistructs.RuntimeDTO, error) {
	runtime, err := r.db.GetRuntime(runtimeID)
	if err != nil {
		return nil, apierrors.ErrDeleteRuntime.InternalError(err)
	}
	// TODO: do not query app
	app, err := r.bdl.GetApp(runtime.ApplicationID)
	if err != nil {
		return nil, err
	}
	perm, err := r.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   operator.String(),
		Scope:    apistructs.AppScope,
		ScopeID:  app.ID,
		Resource: "runtime-" + strutil.ToLower(runtime.Workspace),
		Action:   apistructs.DeleteAction,
	})
	if err != nil {
		return nil, apierrors.ErrDeleteRuntime.InternalError(err)
	}
	if !perm.Access {
		return nil, apierrors.ErrDeleteRuntime.AccessDenied()
	}
	if runtime.LegacyStatus == dbclient.LegacyStatusDeleting {
		// already marked
		return dbclient.ConvertRuntimeDTO(runtime, app), nil
	}
	if runtime.FileToken != "" {
		if _, err = r.bdl.InvalidateOAuth2Token(apistructs.OAuth2TokenInvalidateRequest{AccessToken: runtime.FileToken}); err != nil {
			logrus.Errorf("failed to invalidate openapi oauth2 token of runtime %v, token: %v, err: %v",
				runtime.ID, runtime.FileToken, err)
		}
	}
	// set status to DELETING
	runtime.LegacyStatus = dbclient.LegacyStatusDeleting
	if err := r.db.UpdateRuntime(runtime); err != nil {
		return nil, apierrors.ErrDeleteRuntime.InternalError(err)
	}

	event := events.RuntimeEvent{
		EventName: events.RuntimeDeleting,
		Runtime:   dbclient.ConvertRuntimeDTO(runtime, app),
		Operator:  operator.String(),
	}
	r.evMgr.EmitEvent(&event)
	// TODO: should emit RuntimeDeleted after really deleted or RuntimeDeleteFailed if failed
	return dbclient.ConvertRuntimeDTO(runtime, app), nil
}

// CountPRByWorkspace count project runtimes by workspace .
func (r *Runtime) CountPRByWorkspace(projectId uint64, env string) (uint64, error) {
	var l = logrus.WithField("func", "*Runtime.CountPRByWorkspace")
	cnt, err := r.db.GetProjectRuntimeNumberByWorkspace(projectId, env)
	if err != nil {
		l.WithError(err).Errorln("failed to CountPRByWorkspace")
		return 0, err
	}
	return cnt, nil
}

// CountARByWorkspace count appliaction runtimes by workspace .
func (r *Runtime) CountARByWorkspace(appId uint64, env string) (uint64, error) {
	var l = logrus.WithField("func", "*Runtime.CountPRByWorkspace")
	cnt, err := r.db.GetAppRuntimeNumberByWorkspace(appId, env)
	if err != nil {
		l.WithError(err).Errorln("failed to CountPRByWorkspace")
		return 0, err
	}
	return cnt, nil
}

// Destroy 摧毁应用实例
func (r *Runtime) Destroy(runtimeID uint64) error {
	// 调用hepa完成runtime删除
	logrus.Infof("delete runtime and request hepa, runtimeId is %d", runtimeID)
	if err := r.bdl.DeleteRuntimeService(runtimeID); err != nil {
		logrus.Errorf("failed to request hepa, (%v)", err)
	}
	logrus.Debugf("do delete runtime %d", runtimeID)
	runtime, err := r.db.GetRuntime(runtimeID)
	if err != nil {
		return err
	}
	app, err := r.bdl.GetApp(runtime.ApplicationID)
	if err != nil {
		return err
	}
	if err := r.db.DeleteDomainsByRuntimeId(runtimeID); err != nil {
		return err
	}
	if err := r.addon.RuntimeAddonRemove(strconv.FormatUint(runtimeID, 10), runtime.Workspace, runtime.Creator, app.ProjectID); err != nil {
		// TODO: need addon-platform to fix the 405 error
		//return err
		logrus.Infof("failed to delete addons when delete runtimes: %v", err)
	}
	// TODO: delete alert rules
	uniqueID := spec.RuntimeUniqueId{
		ApplicationId: runtime.ApplicationID,
		Workspace:     runtime.Workspace,
		Name:          runtime.Name,
	}
	if err := r.db.ResetPreDice(uniqueID); err != nil {
		return err
	}
	// clear reference
	r.MarkOutdatedForDelete(runtimeID)
	if runtime.Source == apistructs.ABILITY {
		// TODO: delete ability info
	}
	if runtime.ScheduleName.Name != "" {
		// delete scheduler group
		var req apistructs.ServiceGroupDeleteRequest
		ctx := transport.WithHeader(context.Background(), metadata.New(map[string]string{httputil.InternalHeader: "cmp"}))
		resp, err := r.clusterSvc.GetCluster(ctx, &clusterpb.GetClusterRequest{IdOrName: runtime.ClusterName})
		if err != nil {
			logrus.Errorf("get cluster info failed, cluster name: %s, error: %v", runtime.ClusterName, err)
			return err
		}
		cInfo := resp.Data
		if cInfo != nil && cInfo.OpsConfig != nil && cInfo.OpsConfig.Status == apistructs.ClusterStatusOffline {
			req.Force = true
		}
		req.Namespace = runtime.ScheduleName.Namespace
		req.Name = runtime.ScheduleName.Name
		forceDelete := "false"
		if req.Force {
			forceDelete = "true"
		}
		if err := r.serviceGroupImpl.Delete(req.Namespace, req.Name, forceDelete); err != nil {
			// TODO: we should return err if delete failed (even if err is group not exist?)
			logrus.Errorf("[alert] failed delete group in scheduler: %v, (%v)",
				runtime.ScheduleName, err)
			return err
		}
	}
	// TODO: delete services & instances table

	// do delete runtime table
	if err := r.db.DeleteRuntime(runtimeID); err != nil {
		return err
	}
	event := events.RuntimeEvent{
		EventName: events.RuntimeDeleted,
		Runtime:   dbclient.ConvertRuntimeDTO(runtime, app),
		// TODO: no activity yet, so operator useless currently
		//Operator:  operator,
	}
	r.evMgr.EmitEvent(&event)
	return nil
}

func (r *Runtime) syncRuntimeServices(runtimeID uint64, dice *diceyml.DiceYaml) error {
	for name, service := range dice.Obj().Services {
		var envs string
		envsStr, err := json.Marshal(service.Envs)
		if err == nil {
			envs = string(envsStr)
		}
		var ports string
		portsStr, err := json.Marshal(service.Ports)
		if err == nil {
			ports = string(portsStr)
		}
		err = r.db.CreateOrUpdateRuntimeService(&dbclient.RuntimeService{
			RuntimeId:   runtimeID,
			ServiceName: name,
			Replica:     service.Deployments.Replicas,
			Status:      apistructs.ServiceStatusUnHealthy,
			Cpu:         fmt.Sprintf("%f", service.Resources.CPU),
			Mem:         service.Resources.Mem,
			Environment: envs,
			Ports:       ports,
		}, false)
		if err != nil {
			return err
		}
	}
	return nil
}

// List 查询应用实例列表
func (r *Runtime) List(userID user.ID, orgID uint64, appID uint64, workspace, name string) ([]apistructs.RuntimeSummaryDTO, error) {
	var l = logrus.WithField("func", "*Runtime.List")
	var runtimes []dbclient.Runtime
	if len(workspace) > 0 && len(name) > 0 {
		r, err := r.db.FindRuntime(spec.RuntimeUniqueId{ApplicationId: appID, Workspace: workspace, Name: name})
		if err != nil {
			return nil, apierrors.ErrListRuntime.InternalError(err)
		}
		if r != nil {
			runtimes = append(runtimes, *r)
		}
	} else {
		v, err := r.db.FindRuntimesByAppId(appID)
		if err != nil {
			return nil, apierrors.ErrListRuntime.InternalError(err)
		}
		runtimes = v
	}
	app, err := r.bdl.GetApp(appID)
	if err != nil {
		return nil, err
	}

	// check four env perm
	rtEnvPermBranchMark := make(map[string][]string)
	anyPerm := false
	for _, env := range []string{"dev", "test", "staging", "prod"} {
		perm, err := r.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   userID.String(),
			Scope:    apistructs.AppScope,
			ScopeID:  app.ID,
			Resource: "runtime-" + env,
			Action:   apistructs.GetAction,
		})
		if err != nil {
			return nil, apierrors.ErrGetRuntime.InternalError(err)
		}
		if perm.Access {
			rtEnvPermBranchMark[env] = []string{}
			anyPerm = true
		}
	}
	if !anyPerm {
		return nil, apierrors.ErrGetRuntime.AccessDenied()
	}

	// TODO: apistructs.RuntimeSummaryDTO should be combine into apistructs.Runtime
	var data []apistructs.RuntimeSummaryDTO
	for _, runtime := range runtimes {
		if runtime.OrgID != orgID {
			continue
		}
		env := strutil.ToLower(runtime.Workspace)
		// If the user does not have the permission of this environment,
		// the runtime data in this environment will not be returned
		if _, exists := rtEnvPermBranchMark[env]; !exists {
			continue
		}
		// record all runtime's branchs in each environment
		rtEnvPermBranchMark[env] = append(rtEnvPermBranchMark[env], runtime.GitBranch)
		deployment, err := r.db.FindLastDeployment(runtime.ID)
		if err != nil {
			l.WithError(err).WithField("runtime.ID", runtime.ID).
				Warnln("failed to build summary item, failed to get last deployment")
			return nil, err
		}
		var d apistructs.RuntimeSummaryDTO
		if err := r.convertRuntimeSummaryDTOFromRuntimeModel(&d, runtime, deployment); err != nil {
			l.WithError(err).WithField("runtime.ID", runtime.ID).
				Warnln("failed to convertRuntimeSummaryDTOFromRuntimeModel")
			continue
		}
		data = append(data, d)
	}

	return data, nil
}

// ListGroupByApps lists all runtimes for given apps.
// The key in the returned result map is appID.
func (r *Runtime) ListGroupByApps(appIDs []uint64, env string) (map[uint64][]*apistructs.RuntimeSummaryDTO, error) {
	var l = logrus.WithField("func", "*Runtime.ListGroupByApps")
	runtimes, ids, err := r.db.FindRuntimesInApps(appIDs, env)
	if err != nil {
		l.WithError(err).Errorln("failed to FindRuntimesInApps")
		return nil, err
	}
	deploymentIDs, err := r.db.FindLastDeploymentIDsByRutimeIDs(ids)
	if err != nil {
		l.WithError(err).Errorf("failed to list runtimes: %+v\n", err)
		return nil, err
	}
	deployments, err := r.db.FindDeploymentsByIDs(deploymentIDs)
	// note: internal API, do not check the permission
	var result = struct {
		sync.RWMutex
		m map[uint64][]*apistructs.RuntimeSummaryDTO
	}{m: make(map[uint64][]*apistructs.RuntimeSummaryDTO)}
	var wg sync.WaitGroup
	for appID, runtimeList := range runtimes {
		for _, runtime := range runtimeList {
			wg.Add(1)
			go func(runtime *dbclient.Runtime, deployment dbclient.Deployment) {
				r.generateListGroupAppResult(&result, appID, runtime, deployment, &wg)
			}(runtime, deployments[runtime.ID])
		}
	}
	wg.Wait()
	return result.m, nil
}

func (r *Runtime) generateListGroupAppResult(result *struct {
	sync.RWMutex
	m map[uint64][]*apistructs.RuntimeSummaryDTO
}, appID uint64,
	runtime *dbclient.Runtime, deployment dbclient.Deployment, wg *sync.WaitGroup) {
	var l = logrus.WithField("func", "*Runtime.ListGroupByApps")
	var d apistructs.RuntimeSummaryDTO
	if err := r.convertRuntimeSummaryDTOFromRuntimeModel(&d, *runtime, &deployment); err != nil {
		l.WithError(err).WithField("runtime.ID", runtime.ID).
			Warnln("failed to convertRuntimeSummaryDTOFromRuntimeModel")
	}
	result.Lock()
	result.m[appID] = append(result.m[appID], &d)
	result.Unlock()
	wg.Done()
}

func (r *Runtime) GetServiceByRuntime(runtimeIDs []uint64) (map[uint64]*apistructs.RuntimeSummaryDTO, error) {
	logrus.Debug("get services started")
	var l = logrus.WithField("func", "*Runtime.GetServiceByRuntime")
	runtimes, err := r.db.FindRuntimesByIds(runtimeIDs)
	if err != nil {
		return nil, err
	}
	var servicesMap = struct {
		sync.RWMutex
		m map[uint64]*apistructs.RuntimeSummaryDTO
	}{m: make(map[uint64]*apistructs.RuntimeSummaryDTO)}
	wg := sync.WaitGroup{}
	for i := 0; i < len(runtimes); i++ {
		runtime := runtimes[i]
		deployment, err := r.db.FindLastDeployment(runtime.ID)
		if err != nil {
			l.WithError(err).WithField("runtime.ID", runtime.ID).
				Warnln("failed to build summary item, failed to get last deployment")
			continue
		}
		if deployment == nil {
			// make a fake deployment
			deployment = &dbclient.Deployment{
				RuntimeId: runtime.ID,
				Status:    apistructs.DeploymentStatusInit,
				BaseModel: dbengine.BaseModel{
					UpdatedAt: runtime.UpdatedAt,
				},
			}
		}
		if runtime.ScheduleName.Namespace != "" && runtime.ScheduleName.Name != "" {
			wg.Add(1)
			go func(rt dbclient.Runtime, wg *sync.WaitGroup, servicesMap *struct {
				sync.RWMutex
				m map[uint64]*apistructs.RuntimeSummaryDTO
			}, deployment *dbclient.Deployment) {
				d := apistructs.RuntimeSummaryDTO{}
				sg, err := r.serviceGroupImpl.InspectServiceGroupWithTimeout(rt.ScheduleName.Namespace, rt.ScheduleName.Name)
				if err != nil {
					l.WithError(err).Warnf("failed to inspect servicegroup: %s/%s",
						rt.ScheduleName.Namespace, rt.ScheduleName.Name)
				} else if sg.Status == "Ready" || sg.Status == "Healthy" {
					d.Status = apistructs.RuntimeStatusHealthy
				}
				var dice diceyml.Object
				if err = json.Unmarshal([]byte(deployment.Dice), &dice); err != nil {
					logrus.Error(apierrors.ErrGetRuntime.InvalidState(strutil.Concat("dice.json invalid: ", err.Error())))
					return
				}
				d.Services = make(map[string]*apistructs.RuntimeInspectServiceDTO)
				fillRuntimeDataWithServiceGroup(&d.RuntimeInspectDTO, dice.Services, dice.Jobs, sg, nil, string(deployment.Status))
				servicesMap.Lock()
				servicesMap.m[rt.ID] = &d
				servicesMap.Unlock()
				wg.Done()
			}(runtime, &wg, &servicesMap, deployment)
		}
	}
	wg.Wait()
	logrus.Debug("get services finished")
	return servicesMap.m, nil

}

func (r *Runtime) convertRuntimeSummaryDTOFromRuntimeModel(d *apistructs.RuntimeSummaryDTO, runtime dbclient.Runtime, deployment *dbclient.Deployment) error {
	var l = logrus.WithField("func", "Runtime.convertRuntimeInspectDTOFromRuntimeModel")

	if d == nil {
		err := errors.New("the DTO is nil")
		l.WithError(err).Warnln()
		return err
	}

	isFakeRuntime := false
	// TODO: Deprecated, instead from runtime deployment_status filed
	if deployment == nil {
		isFakeRuntime = true
		// make a fake deployment
		deployment = &dbclient.Deployment{
			RuntimeId: runtime.ID,
			Status:    apistructs.DeploymentStatusInit,
			BaseModel: dbengine.BaseModel{
				UpdatedAt: runtime.UpdatedAt,
			},
		}
	}

	d.ID = runtime.ID
	d.Name = runtime.Name
	d.ServiceGroupNamespace = runtime.ScheduleName.Namespace
	d.ServiceGroupName = runtime.ScheduleName.Name
	d.Source = runtime.Source
	d.Status = runtime.Status
	d.Services = make(map[string]*apistructs.RuntimeInspectServiceDTO)
	d.DeployStatus = deployment.Status
	// 如果还 deployment 的状态不是终态, runtime 的状态返回为 init(前端显示为部署中效果),
	// 不然开始部署直接变为不健康不合理
	if deployment.Status == apistructs.DeploymentStatusDeploying ||
		deployment.Status == apistructs.DeploymentStatusWaiting ||
		deployment.Status == apistructs.DeploymentStatusInit ||
		deployment.Status == apistructs.DeploymentStatusWaitApprove {
		d.Status = apistructs.RuntimeStatusInit
	}
	if runtime.LegacyStatus == dbclient.LegacyStatusDeleting {
		d.DeleteStatus = dbclient.LegacyStatusDeleting
	}
	d.DeploymentOrderId = runtime.DeploymentOrderId
	d.DeploymentOrderName = utils.ParseOrderName(runtime.DeploymentOrderId)
	d.ReleaseVersion = runtime.ReleaseVersion
	d.ReleaseID = deployment.ReleaseId
	d.ClusterID = runtime.ClusterId
	d.ClusterName = runtime.ClusterName
	d.ReleaseVersion = runtime.ReleaseVersion
	d.Creator = runtime.Creator
	d.ApplicationID = runtime.ApplicationID
	d.CreatedAt = runtime.CreatedAt
	d.UpdatedAt = runtime.UpdatedAt
	d.RawStatus = runtime.Status
	d.RawDeploymentStatus = string(deployment.Status)
	d.TimeCreated = runtime.CreatedAt
	d.Extra = map[string]interface{}{
		"applicationId": runtime.ApplicationID,
		"workspace":     runtime.Workspace,
		"buildId":       deployment.BuildId,
		"fakeRuntime":   isFakeRuntime,
	}
	d.ProjectID = runtime.ProjectID
	updateStatusToDisplay(&d.RuntimeInspectDTO)
	if deployment.Status == apistructs.DeploymentStatusDeploying {
		updateStatusWhenDeploying(&d.RuntimeInspectDTO)
	}
	d.LastOperator = deployment.Operator
	d.LastOperateTime = deployment.UpdatedAt // TODO: use a standalone OperateTime
	d.LastOperatorId = deployment.ID
	return nil
}

// Get 查询应用实例
func (r *Runtime) Get(ctx context.Context, userID user.ID, orgID uint64, idOrName string, appID string, workspace string) (*apistructs.RuntimeInspectDTO, error) {
	runtime, err := r.findRuntimeByIDOrName(idOrName, appID, workspace)
	if err != nil {
		return nil, err
	}
	if runtime == nil {
		return nil, apierrors.ErrGetRuntime.NotFound()
	}
	perm, err := r.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   userID.String(),
		Scope:    apistructs.AppScope,
		ScopeID:  runtime.ApplicationID,
		Resource: "runtime-" + strutil.ToLower(runtime.Workspace),
		Action:   apistructs.GetAction,
	})
	if err != nil {
		return nil, apierrors.ErrGetRuntime.InternalError(err)
	}
	if !perm.Access {
		return nil, apierrors.ErrGetRuntime.AccessDenied()
	}
	deployment, err := r.db.FindLastDeployment(runtime.ID)
	if err != nil {
		return nil, apierrors.ErrGetRuntime.InternalError(err)
	}
	if deployment == nil {
		return nil, apierrors.ErrGetRuntime.InvalidState("last deployment not found")
	}
	var dice diceyml.Object
	if err := json.Unmarshal([]byte(deployment.Dice), &dice); err != nil {
		return nil, apierrors.ErrGetRuntime.InvalidState(strutil.Concat("dice.json invalid: ", err.Error()))
	}
	domains, err := r.db.FindDomainsByRuntimeId(runtime.ID)
	if err != nil {
		return nil, apierrors.ErrGetRuntime.InternalError(err)
	}
	domainMap := make(map[string][]string)
	for _, d := range domains {
		if domainMap[d.EndpointName] == nil {
			domainMap[d.EndpointName] = make([]string, 0)
		}
		domainMap[d.EndpointName] = append(domainMap[d.EndpointName], "http://"+d.Domain)
	}
	// TODO: use the newest api instead of InspectGroup
	var sg *apistructs.ServiceGroup
	if runtime.ScheduleName.Name != "" {
		sg, _ = r.serviceGroupImpl.InspectServiceGroupWithTimeout(runtime.ScheduleName.Namespace, runtime.ScheduleName.Name)
	}

	ctx = transport.WithHeader(ctx, metadata.New(map[string]string{httputil.InternalHeader: "cmp"}))
	resp, err := r.clusterSvc.GetCluster(ctx, &clusterpb.GetClusterRequest{IdOrName: runtime.ClusterName})
	if err != nil {
		return nil, apierrors.ErrGetRuntime.InvalidState(fmt.Sprintf("cluster: %v not found", runtime.ClusterName))
	}
	cluster := resp.Data

	// return value
	var data apistructs.RuntimeInspectDTO
	data.ID = runtime.ID
	data.Name = runtime.Name
	data.ServiceGroupNamespace = runtime.ScheduleName.Namespace
	data.ServiceGroupName = runtime.ScheduleName.Name
	data.Source = runtime.Source
	data.Status = apistructs.RuntimeStatusUnHealthy
	if runtime.ScheduleName.Namespace != "" && runtime.ScheduleName.Name != "" && sg != nil {
		if sg.Status == "Ready" || sg.Status == "Healthy" {
			data.Status = apistructs.RuntimeStatusHealthy
		}
	}

	data.DeployStatus = deployment.Status
	if deployment.Status == apistructs.DeploymentStatusDeploying ||
		deployment.Status == apistructs.DeploymentStatusWaiting ||
		deployment.Status == apistructs.DeploymentStatusInit ||
		deployment.Status == apistructs.DeploymentStatusWaitApprove {
		data.Status = apistructs.RuntimeStatusInit
	}

	if runtime.LegacyStatus == "DELETING" {
		data.DeleteStatus = "DELETING"
	}
	data.ReleaseID = deployment.ReleaseId
	data.ClusterID = runtime.ClusterId
	data.ClusterName = runtime.ClusterName
	data.ClusterType = cluster.Type
	data.Extra = map[string]interface{}{
		"applicationId": runtime.ApplicationID,
		"workspace":     runtime.Workspace,
		"buildId":       deployment.BuildId,
	}
	app, err := r.bdl.GetApp(runtime.ApplicationID)
	if err != nil {
		return nil, err
	}
	data.ProjectID = app.ProjectID
	data.ApplicationName = app.Name
	data.CreatedAt = runtime.CreatedAt
	data.Creator = runtime.Creator
	data.UpdatedAt = runtime.UpdatedAt
	data.TimeCreated = runtime.CreatedAt
	data.Services = make(map[string]*apistructs.RuntimeInspectServiceDTO)

	fillRuntimeDataWithServiceGroup(&data, dice.Services, dice.Jobs, sg, domainMap, string(deployment.Status))

	updateStatusToDisplay(&data)
	if deployment.Status == apistructs.DeploymentStatusDeploying {
		updateStatusWhenDeploying(&data)
	}

	return &data, nil
}

// fillRuntimeDataWithServiceGroup use serviceGroup's data to fill RuntimeInspectDTO
func fillRuntimeDataWithServiceGroup(data *apistructs.RuntimeInspectDTO, targetService diceyml.Services, targetJob diceyml.Jobs,
	sg *apistructs.ServiceGroup, domainMap map[string][]string, status string) {
	statusServiceMap := map[string]string{}
	replicaMap := map[string]int{}
	resourceMap := map[string]apistructs.RuntimeServiceResourceDTO{}
	statusMap := map[string]map[string]string{}
	if sg != nil {
		if sg.Status != apistructs.StatusReady && sg.Status != apistructs.StatusHealthy {
			for _, serviceItem := range sg.Services {
				statusMap[serviceItem.Name] = map[string]string{
					"Msg":    serviceItem.LastMessage,
					"Reason": serviceItem.Reason,
				}
			}
		}
		data.ModuleErrMsg = statusMap

		for _, v := range sg.Services {
			statusServiceMap[v.Name] = string(v.StatusDesc.Status)
			if statusServiceMap[v.Name] == "Ready" || statusServiceMap[v.Name] == "Healthy" {
				statusServiceMap[v.Name] = apistructs.RuntimeStatusHealthy
			}
			replicaMap[v.Name] = v.Scale
			resourceMap[v.Name] = apistructs.RuntimeServiceResourceDTO{
				CPU:  v.Resources.Cpu,
				Mem:  int(v.Resources.Mem),
				Disk: int(v.Resources.Disk),
			}
		}
	}

	// TODO: no diceJson and no overlay, we just read dice from releaseId
	for k, v := range targetService {
		var expose []string
		var svcPortExpose bool
		// serv.Expose will abandoned, serv.Ports.Expose is recommended
		for _, svcPort := range v.Ports {
			if svcPort.Expose {
				svcPortExpose = true
			}
		}
		if (len(v.Expose) != 0 || svcPortExpose) && domainMap != nil {
			expose = domainMap[k]
		}

		runtimeInspectService := &apistructs.RuntimeInspectServiceDTO{
			Resources: apistructs.RuntimeServiceResourceDTO{
				CPU:  v.Resources.CPU,
				Mem:  int(v.Resources.Mem),
				Disk: int(v.Resources.Disk),
			},
			Envs:        v.Envs,
			Addrs:       convertInternalAddrs(sg, k),
			Expose:      expose,
			Status:      status,
			Deployments: apistructs.RuntimeServiceDeploymentsDTO{Replicas: 0},
		}
		if sgStatus, ok := statusServiceMap[k]; ok {
			runtimeInspectService.Status = sgStatus
		}
		if sgReplicas, ok := replicaMap[k]; ok {
			runtimeInspectService.Deployments.Replicas = sgReplicas
		}
		if sgResources, ok := resourceMap[k]; ok {
			runtimeInspectService.Resources = sgResources
		}

		data.Services[k] = runtimeInspectService
	}
	for k, v := range targetJob {
		runtimeInspectService := &apistructs.RuntimeInspectServiceDTO{
			Resources: apistructs.RuntimeServiceResourceDTO{
				CPU:  v.Resources.CPU,
				Mem:  int(v.Resources.Mem),
				Disk: int(v.Resources.Disk),
			},
			Envs:        v.Envs,
			Type:        "job",
			Status:      statusServiceMap[k],
			Deployments: apistructs.RuntimeServiceDeploymentsDTO{Replicas: 1},
		}
		data.Services[k] = runtimeInspectService
	}
	data.Resources = apistructs.RuntimeServiceResourceDTO{CPU: 0, Mem: 0, Disk: 0}
	for _, v := range data.Services {
		if v.Type == "job" {
			continue
		}
		data.Resources.CPU += v.Resources.CPU * float64(v.Deployments.Replicas)
		data.Resources.Mem += v.Resources.Mem * v.Deployments.Replicas
		data.Resources.Disk += v.Resources.Disk * v.Deployments.Replicas
	}
}

// GetSpec 查询应用实例规格
func (r *Runtime) GetSpec(userID user.ID, orgID uint64, runtimeID uint64) (*apistructs.ServiceGroup, error) {
	runtime, err := r.db.GetRuntime(runtimeID)
	if err != nil {
		return nil, apierrors.ErrGetRuntime.InternalError(err)
	}
	perm, err := r.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   userID.String(),
		Scope:    apistructs.OrgScope,
		ScopeID:  runtime.OrgID,
		Resource: "runtime-config",
		Action:   apistructs.GetAction,
	})
	if err != nil {
		return nil, apierrors.ErrGetRuntime.InternalError(err)
	}
	if !perm.Access {
		return nil, apierrors.ErrGetRuntime.AccessDenied()
	}
	return r.serviceGroupImpl.InspectServiceGroupWithTimeout(runtime.ScheduleName.Namespace, runtime.ScheduleName.Name)
}

func (r *Runtime) KillPod(runtimeID uint64, podname string) error {
	runtime, err := r.db.GetRuntime(runtimeID)
	if err != nil {
		return err
	}

	if runtime.ScheduleName.Namespace == "" || runtime.ScheduleName.Name == "" || podname == "" {
		return errors.New("empty namespace or name or podname")
	}
	return r.serviceGroupImpl.KillPod(context.Background(), runtime.ScheduleName.Namespace, runtime.ScheduleName.Name, podname)
}

// TODO: this work is weird
func (r *Runtime) findRuntimeByIDOrName(idOrName string, appIDStr string, workspace string) (*dbclient.Runtime, error) {
	runtimeID, err := strconv.ParseUint(idOrName, 10, 64)
	if err == nil {
		// parse int success, idOrName is id
		return r.db.GetRuntimeAllowNil(runtimeID)
	}
	// idOrName is name
	if workspace == "" {
		return nil, apierrors.ErrGetRuntime.MissingParameter("workspace")
	}
	appID, err := strconv.ParseUint(appIDStr, 10, 64)
	if err != nil {
		return nil, apierrors.ErrGetRuntime.InvalidParameter(strutil.Concat("applicationID: ", appIDStr))
	}
	// TODO: we shall not un-escape runtimeName, after we fix existing data and deny '/'
	name, err := url.PathUnescape(idOrName)
	if err != nil {
		return nil, apierrors.ErrGetRuntime.InvalidParameter(strutil.Concat("idOrName: ", idOrName))
	}
	return r.db.FindRuntime(spec.RuntimeUniqueId{Name: name, Workspace: workspace, ApplicationId: appID})
}

func convertInternalAddrs(sg *apistructs.ServiceGroup, serviceName string) []string {
	addrs := make([]string, 0)
	if sg == nil {
		return addrs
	}
	for _, s := range sg.Services {
		if s.Name != serviceName {
			continue
		}
		for _, p := range s.Ports {
			addrs = append(addrs, s.Vip+":"+strconv.Itoa(p.Port))
		}
	}
	return addrs
}

// 修改内部状态为展示状态
func updateStatusToDisplay(runtime *apistructs.RuntimeInspectDTO) {
	if runtime == nil {
		return
	}
	runtime.Status = isStatusForDisplay(runtime.Status)
	for key := range runtime.Services {
		runtime.Services[key].Status = isStatusForDisplay(runtime.Services[key].Status)
	}
}

func isStatusForDisplay(status string) string {
	switch status {
	case apistructs.RuntimeStatusHealthy, apistructs.RuntimeStatusUnHealthy, apistructs.RuntimeStatusInit:
		return status
	case "Ready", "ready":
		return apistructs.RuntimeStatusHealthy
	default:
		return status
	}
}

// 将 UnHealthy 修改为 Progressing（部署中）
func updateStatusWhenDeploying(runtime *apistructs.RuntimeInspectDTO) {
	if runtime == nil {
		return
	}
	if runtime.Status == "UnHealthy" {
		runtime.Status = "Progressing"
	}
	for _, v := range runtime.Services {
		if v.Status == "UnHealthy" {
			v.Status = "Progressing"
		}
	}
}

func checkRuntimeCreateReq(req *apistructs.RuntimeCreateRequest) error {
	if req.Name == "" {
		return errors.New("runtime name is not specified")
	}
	if req.ReleaseID == "" {
		return errors.New("releaseId is not specified")
	}
	if req.Operator == "" {
		return errors.New("operator is not specified")
	}
	if req.ClusterName == "" {
		return errors.New("clusterName is not specified")
	}
	switch req.Source {
	case apistructs.PIPELINE:
	case apistructs.ABILITY:
	case apistructs.RUNTIMEADDON:
	case apistructs.RELEASE:
	default:
		return errors.New("source is unknown")
	}
	if req.Extra.OrgID == 0 {
		return errors.New("extra.orgId is not specified")
	}
	if req.Source == apistructs.PIPELINE || req.Source == apistructs.RUNTIMEADDON || req.Source == apistructs.RELEASE {
		if req.Extra.ProjectID == 0 {
			return errors.New("extra.projectId is not specified, for pipeline")
		}
		if req.Extra.ApplicationID == 0 {
			return errors.New("extra.applicationId is not specified, for pipeline")
		}
	}
	if req.Source == apistructs.RUNTIMEADDON {
		if len(req.Extra.InstanceID) == 0 {
			return errors.New("extra.instanceId is not specified, for runtimeaddon")
		}
	}
	if req.Source == apistructs.ABILITY {
		if req.Extra.ApplicationName == "" {
			return errors.New("extra.applicationName are not specified, for ability")
		}
	}
	if req.Extra.Workspace == "" {
		return errors.New("extra.workspace is not specified")
	}
	return nil
}

// FullGC 定时全量 GC 过期的部署单
func (r *Runtime) FullGC() {
	defer func() {
		if err := recover(); err != nil {
			debug.PrintStack()
			logrus.Errorf("[alert] failed to fullGC, panic: %v", err)
		}
	}()

	rollbackCfg, err := r.getRollbackConfig()
	if err != nil {
		logrus.Errorf("[alert] failed to get all rollback config: %v", err)
		return
	}

	bulk := 100
	lastRuntimeID := uint64(0)
	for {
		runtimes, err := r.db.FindRuntimesNewerThan(lastRuntimeID, bulk)
		if err != nil {
			logrus.Errorf("[alert] failed to find runtimes after: %v, (%v)", lastRuntimeID, err)
			break
		}
		for i := range runtimes {
			keep, ok := rollbackCfg[runtimes[i].ProjectID][strings.ToUpper(runtimes[i].Workspace)]
			if !ok || keep <= 0 || keep > 100 {
				keep = 5
			}
			r.fullGCForSingleRuntime(runtimes[i].ID, keep)
		}
		if len(runtimes) < bulk {
			// ended
			break
		}
		lastRuntimeID = runtimes[len(runtimes)-1].ID
	}
}

// getRollbackConfig return the number of rollback record for each project and workspace
// key1: project_id, key2: workspace, value: the limit of rollback record
func (r *Runtime) getRollbackConfig() (map[uint64]map[string]int, error) {
	result := make(map[uint64]map[string]int, 0)
	// TODO: use cache to get project info
	projects, err := r.bdl.GetAllProjects()
	if err != nil {
		return nil, err
	}
	for _, prj := range projects {
		result[prj.ID] = prj.RollbackConfig
	}
	return result, nil
}

func (r *Runtime) fullGCForSingleRuntime(runtimeID uint64, keep int) {
	top, err := r.db.FindTopDeployments(runtimeID, keep)
	if err != nil {
		logrus.Errorf("[alert] failed to find top %d deployments for gc, (%v)", keep, err)
	}
	if len(top) < keep {
		// all of deployments should keep
		return
	}
	oldestID := top[len(top)-1].ID
	if deployments, err := r.db.FindNotOutdatedOlderThan(runtimeID, oldestID); err != nil {
		logrus.Errorf("[alert] failed to set outdated, runtimeID: %v, maxID: %v, (%v)",
			runtimeID, oldestID, err)
	} else {
		for i := range deployments {
			r.markOutdated(&deployments[i])
		}
	}
}

// ReferCluster 查看 runtime & addon 是否有使用集群
func (r *Runtime) ReferCluster(clusterName string, orgID uint64) bool {
	runtimes, err := r.db.ListRuntimeByOrgCluster(clusterName, orgID)
	if err != nil {
		logrus.Warnf("failed to list runtime, %v", err)
		return true
	}
	if len(runtimes) > 0 {
		return true
	}

	routingInstances, err := r.db.ListRoutingInstanceByOrgCluster(clusterName, orgID)
	if err != nil {
		logrus.Warnf("failed to list addon, %v", err)
		return true
	}
	if len(routingInstances) > 0 {
		return true
	}

	return false
}

// MarkOutdatedForDelete 将删除的应用实例，他们的所有部署单，标记为废弃
func (r *Runtime) MarkOutdatedForDelete(runtimeID uint64) {
	deployments, err := r.db.FindNotOutdatedOlderThan(runtimeID, math.MaxUint32)
	if err != nil {
		logrus.Errorf("[alert] failed to query all not outdated deployment before delete runtime: %v, (%v)",
			runtimeID, err)
	}
	for i := range deployments {
		r.markOutdated(&deployments[i])
	}
}

func (r *Runtime) markOutdated(deployment *dbclient.Deployment) {
	if deployment.Outdated {
		// already outdated
		return
	}
	deployment.Outdated = true
	if err := r.db.UpdateDeployment(deployment); err != nil {
		logrus.Errorf("[alert] failed to set deployment: %v outdated, (%v)", deployment.ID, err)
		return
	}
	if len(deployment.ReleaseId) > 0 {
		ctx := transport.WithHeader(context.Background(), metadata.New(map[string]string{httputil.InternalHeader: "true"}))
		if _, err := r.releaseSvc.UpdateReleaseReference(ctx, &pb.ReleaseReferenceUpdateRequest{
			ReleaseID: deployment.ReleaseId,
			Increase:  false,
		}); err != nil {
			logrus.Errorf("[alert] failed to decrease reference of release: %s, (%v)",
				deployment.ReleaseId, err)
		}
	}
}

// RuntimeDeployLogs deploy发布日志接口
func (r *Runtime) RuntimeDeployLogs(userID user.ID, orgID uint64, orgName string, deploymentID uint64, paramValues url.Values) (*apistructs.DashboardSpotLogData, error) {
	deployment, err := r.db.GetDeployment(deploymentID)
	if err != nil {
		return nil, apierrors.ErrGetRuntime.InternalError(err)
	}
	if deployment == nil {
		return nil, apierrors.ErrGetRuntime.InvalidParameter(errors.Errorf("deployment not found, id: %d", deploymentID))
	}
	if err := r.checkRuntimeScopePermission(userID, deployment.RuntimeId); err != nil {
		return nil, err
	}
	return r.requestMonitorLog(strconv.FormatUint(deploymentID, 10), orgName, paramValues, apistructs.DashboardSpotLogSourceDeploy)
}

// OrgJobLogs 数据中心--->任务列表 日志接口
func (r *Runtime) OrgJobLogs(ctx context.Context, userID user.ID, orgID uint64, orgName string, jobID, clusterName string, paramValues url.Values) (*apistructs.DashboardSpotLogData, error) {
	if clusterName == "" {
		logrus.Errorf("job instance infos without cluster, jobID is: %s", jobID)
		return nil, apierrors.ErrOrgLog.AccessDenied()
	}
	ctx = transport.WithHeader(ctx, metadata.New(map[string]string{httputil.InternalHeader: "cmp"}))
	resp, err := r.clusterSvc.GetCluster(ctx, &clusterpb.GetClusterRequest{IdOrName: clusterName})
	if err != nil {
		return nil, err
	}
	clusterInfo := resp.Data
	if clusterInfo == nil {
		logrus.Errorf("can not find cluster info, clusterName: %s", clusterName)
		return nil, apierrors.ErrOrgLog.AccessDenied()
	}
	orgID = uint64(clusterInfo.OrgID)
	if err := r.checkOrgScopePermission(userID, orgID); err != nil {
		return nil, err
	}
	return r.requestMonitorLog(jobID, orgName, paramValues, apistructs.DashboardSpotLogSourceJob)
}

// checkRuntimeScopePermission 检测runtime级别的权限
func (r *Runtime) checkRuntimeScopePermission(userID user.ID, runtimeID uint64) error {
	runtime, err := r.db.GetRuntime(runtimeID)
	if err != nil {
		return err
	}
	perm, err := r.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   userID.String(),
		Scope:    apistructs.AppScope,
		ScopeID:  runtime.ApplicationID,
		Resource: "runtime-" + strutil.ToLower(runtime.Workspace),
		Action:   apistructs.GetAction,
	})
	if err != nil {
		return err
	}
	if !perm.Access {
		return apierrors.ErrGetRuntime.AccessDenied()
	}

	return nil
}

// checkOrgScopePermission 检测org级别的权限
func (r *Runtime) checkOrgScopePermission(userID user.ID, orgID uint64) error {
	perm, err := r.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   userID.String(),
		Scope:    apistructs.OrgScope,
		ScopeID:  orgID,
		Resource: "monitor_org_alert",
		Action:   apistructs.GetAction,
	})
	if err != nil {
		return err
	}
	if !perm.Access {
		return apierrors.ErrGetRuntime.AccessDenied()
	}

	return nil
}

// requestMonitorLog 调用bundle monitor log接口获取数据
func (r *Runtime) requestMonitorLog(requestID string, orgName string, paramValues url.Values, source apistructs.DashboardSpotLogSource) (*apistructs.DashboardSpotLogData, error) {
	// 获取日志
	var logReq apistructs.DashboardSpotLogRequest
	if err := queryStringDecoder.Decode(&logReq, paramValues); err != nil {
		return nil, err
	}
	logReq.ID = requestID
	logReq.Source = source

	logResult, err := r.bdl.GetLog(orgName, logReq)
	if err != nil {
		return nil, err
	}
	return logResult, nil
}

var queryStringDecoder *schema.Decoder

func init() {
	queryStringDecoder = schema.NewDecoder()
	queryStringDecoder.IgnoreUnknownKeys(true)
}

func getRedeployPipelineYmlName(runtime dbclient.Runtime) string {
	return fmt.Sprintf("%d/%s/%s/pipeline.yml", runtime.ApplicationID, runtime.Workspace, runtime.Name)
}

func convertRuntimeDeployDto(app *apistructs.ApplicationDTO, release *pb.ReleaseGetResponseData, dto *apistructs.PipelineDTO) (*apistructs.RuntimeDeployDTO, error) {
	names, err := getServicesNames(release.Diceyml)
	if err != nil {
		return nil, err
	}
	return &apistructs.RuntimeDeployDTO{
		ApplicationID:   app.ID,
		ApplicationName: app.Name,
		ProjectID:       app.ProjectID,
		ProjectName:     app.ProjectName,
		OrgID:           app.OrgID,
		OrgName:         app.OrgName,
		PipelineID:      dto.ID,
		ServicesNames:   names,
	}, nil
}

// getServicesNames get servicesNames by diceYml
func getServicesNames(diceYml string) ([]string, error) {
	yml, err := diceyml.New([]byte(diceYml), false)
	if err != nil {
		return nil, err
	}
	names := make([]string, 0)
	for k := range yml.Obj().Services {
		names = append(names, k)
	}
	return names, nil
}

func IsDeploying(status apistructs.DeploymentStatus) bool {
	switch status {
	// report error, we no longer support auto-cancel
	case apistructs.DeploymentStatusWaitApprove, apistructs.DeploymentStatusInit, apistructs.DeploymentStatusWaiting, apistructs.DeploymentStatusDeploying:
		return true
	default:
		return false
	}
}
