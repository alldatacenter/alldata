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

package endpoints

import (
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"strconv"
	"sync"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	dwfpb "github.com/erda-project/erda-proto-go/dop/devflowrule/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/conf"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/cron"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/strutil"
)

var (
	ProjectStatsCache *sync.Map
	Once              sync.Once
)

// CreateProject 创建项目
func (e *Endpoints) CreateProject(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 获取当前用户
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrCreateProject.NotLogin().ToResp(), nil
	}

	if r.Body == nil {
		return apierrors.ErrCreateProject.MissingParameter("body").ToResp(), nil
	}
	bodyData, err := ioutil.ReadAll(r.Body)
	if err != nil {
		logrus.WithError(err).Errorln("failed to read request body")
		return apierrors.ErrCreateProject.InvalidParameter(err).ToResp(), nil
	}
	logrus.WithField("request body", string(bodyData)).Infoln("CreateProject")
	var projectCreateReq apistructs.ProjectCreateRequest
	if err := json.Unmarshal(bodyData, &projectCreateReq); err != nil {
		return apierrors.ErrCreateProject.InvalidParameter(err).ToResp(), nil
	}
	if !strutil.IsValidPrjOrAppName(projectCreateReq.Name) {
		return apierrors.ErrCreateProject.InvalidParameter(errors.Errorf("project name is invalid %s",
			projectCreateReq.Name)).ToResp(), nil
	}
	logrus.Infof("request body: %+v", projectCreateReq)
	data, _ := json.Marshal(projectCreateReq)
	logrus.Infof("request body data marshaled: %s", string(data))

	// check permission
	req := apistructs.PermissionCheckRequest{
		UserID:   identity.UserID,
		Scope:    apistructs.OrgScope,
		ScopeID:  projectCreateReq.OrgID,
		Resource: apistructs.ProjectResource,
		Action:   apistructs.CreateAction,
	}
	if access, err := e.bdl.CheckPermission(&req); err != nil || !access.Access {
		return apierrors.ErrCreateProject.AccessDenied().ToResp(), nil
	}

	// get org locale
	org, err := e.bdl.GetOrg(projectCreateReq.OrgID)
	if err != nil {
		return apierrors.ErrCreateProject.InternalError(err).ToResp(), nil
	}

	// create project
	projectID, err := e.bdl.CreateProject(projectCreateReq, identity.UserID)
	if err != nil {
		return apierrors.ErrCreateProject.InternalError(err).ToResp(), nil
	}

	// init branchRule
	if err = e.branchRule.InitProjectRules(int64(projectID)); err != nil {
		return apierrors.ErrCreateProject.InternalError(err).ToResp(), nil
	}

	// init projectState
	if err := e.issueState.InitProjectState(int64(projectID), org.Locale); err != nil {
		logrus.Warnf("failed to add state to db when create project, (%v)", err)
	}

	// create devFlowRule
	if _, err = e.DevFlowRule.CreateDevFlowRule(ctx, &dwfpb.CreateDevFlowRuleRequest{ProjectID: projectID, UserID: identity.UserID}); err != nil {
		logrus.Warnf("failed to CreateDevFlowRule, (%v)", err)
	}
	return httpserver.OkResp(projectID)
}

// DeleteProject delete project
func (e *Endpoints) DeleteProject(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	orgIDStr := r.Header.Get(httputil.OrgHeader)
	orgID, err := strutil.Atoi64(orgIDStr)
	if err != nil {
		return apierrors.ErrDeleteProject.InvalidParameter(err).ToResp(), nil
	}

	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrDeleteProject.NotLogin().ToResp(), nil
	}

	projectID, err := strutil.Atoi64(vars["projectID"])
	if err != nil {
		return apierrors.ErrDeleteProject.InvalidParameter(err).ToResp(), nil
	}

	// check permission
	req := apistructs.PermissionCheckRequest{
		UserID:   identity.UserID,
		Scope:    apistructs.ProjectScope,
		ScopeID:  uint64(projectID),
		Resource: apistructs.ProjectResource,
		Action:   apistructs.DeleteAction,
	}
	if access, err := e.bdl.CheckPermission(&req); err != nil || !access.Access {
		// check org permission
		req.Scope = apistructs.OrgScope
		req.ScopeID = uint64(orgID)
		if access, err = e.bdl.CheckPermission(&req); err != nil || !access.Access {
			return apierrors.ErrDeleteProject.AccessDenied().ToResp(), nil
		}
	}

	// Check if basic addon exists
	addOnListResp, err := e.bdl.ListAddonByProjectID(projectID, orgID)
	if err != nil {
		return nil, err
	}
	if addOnListResp != nil && len(addOnsFilterIn(addOnListResp.Data, func(addOn *apistructs.AddonFetchResponseData) bool {
		// The platformServiceType is 0 means it can be deleted by the platform
		return addOn.PlatformServiceType == 0
	})) > 0 {
		return apierrors.ErrDeleteProject.InternalError(errors.Errorf("failed to delete project(there exists basic addons)")).ToResp(), nil
	}

	// Clean up non-basic addon before deleting project. like monitor,log-analytics,api-gateway...
	if addOnListResp != nil {
		go e.cleanupNonBasicAddon(addOnListResp.Data, orgIDStr, identity.UserID)
	}

	// delete project
	project, err := e.bdl.DeleteProject(uint64(projectID), uint64(orgID), identity.UserID)
	if err != nil {
		return apierrors.ErrDeleteProject.InternalError(err).ToResp(), nil
	}

	//  delete branch rule
	if err = e.db.DeleteBranchRuleByScope(apistructs.ProjectScope, projectID); err != nil {
		logrus.Warnf("failed to delete project branch rules, (%v)", err)
		return apierrors.ErrDeleteProject.InternalError(err).ToResp(), nil
	}

	// delete issue state
	if err = e.db.DeleteIssuesStateByProjectID(projectID); err != nil {
		logrus.Warnf("failed to delete project state, (%v)", err)
		return apierrors.ErrDeleteProject.InternalError(err).ToResp(), nil
	}

	// delete devFlowRule
	if _, err = e.DevFlowRule.DeleteDevFlowRule(ctx, &dwfpb.DeleteDevFlowRuleRequest{ProjectID: uint64(projectID), UserID: identity.UserID}); err != nil {
		logrus.Warnf("failed to DeleteDevFlowRule, (%v)", err)
	}

	return httpserver.OkResp(project)
}

// cleanupNonBasicAddon Clean up non-basic addon
func (e *Endpoints) cleanupNonBasicAddon(addons []apistructs.AddonFetchResponseData, orgID, userID string) {
	nonBasicAddons := addOnsFilterIn(addons, func(addOn *apistructs.AddonFetchResponseData) bool {
		// The platformServiceType is 1 means it is non-basic addon
		return addOn.PlatformServiceType == 1
	})
	for _, v := range nonBasicAddons {
		logrus.Infof("[cleanupNonBasicAddon] begin deleting addon, addonID: %s", v.ID)
		_, err := e.bdl.DeleteAddon(v.ID, orgID, userID)
		if err != nil {
			logrus.Errorf("[cleanupNonBasicAddon] failed to DeleteAddon, addonID: %s, err: %s", v.ID, err.Error())
		}
	}
}

func addOnsFilterIn(addOns []apistructs.AddonFetchResponseData, fn func(addOn *apistructs.AddonFetchResponseData) bool) (newAddons []apistructs.AddonFetchResponseData) {
	for i := range addOns {
		if fn(&addOns[i]) {
			newAddons = append(newAddons, addOns[i])
		}
	}
	return
}

// GetProject gets the project info
func (e *Endpoints) GetProject(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	l := logrus.WithField("func", "*Endpoints.GetProject")

	// 检查projectID合法性
	projectID, err := strutil.Atoi64(vars["projectID"])
	if err != nil {
		return apierrors.ErrGetProject.InvalidParameter(err).ToResp(), nil
	}

	orgIDStr := r.Header.Get(httputil.OrgHeader)
	internalClient := r.Header.Get(httputil.InternalHeader)
	if internalClient == "" {
		userID, err := user.GetUserID(r)
		if err != nil {
			return apierrors.ErrGetProject.NotLogin().ToResp(), nil
		}
		// 操作鉴权
		req := apistructs.PermissionCheckRequest{
			UserID:   userID.String(),
			Scope:    apistructs.ProjectScope,
			ScopeID:  uint64(projectID),
			Resource: apistructs.ProjectResource,
			Action:   apistructs.GetAction,
		}
		if access, err := e.bdl.CheckPermission(&req); err != nil || !access.Access {
			orgID, err := strconv.ParseUint(orgIDStr, 10, 64)
			if err != nil {
				return apierrors.ErrGetProject.InvalidParameter(err).ToResp(), nil
			}
			// 若非项目管理员，判断用户是否为企业管理员(数据中心)
			req := apistructs.PermissionCheckRequest{
				UserID:   userID.String(),
				Scope:    apistructs.OrgScope,
				ScopeID:  orgID,
				Resource: apistructs.ProjectResource,
				Action:   apistructs.GetAction,
			}
			if access, err := e.bdl.CheckPermission(&req); err != nil || !access.Access {
				return apierrors.ErrGetProject.AccessDenied().ToResp(), nil
			}
		}
	}

	dto, apiError := e.project.Get(ctx, uint64(projectID))
	if apiError != nil {
		l.Errorf("failed to Get: %s", apiError.Error())
		return apiError.ToResp(), nil
	}

	return httpserver.OkResp(dto, append(dto.Owners, dto.Creator))
}

// ListProject list project
func (e *Endpoints) ListProject(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// get user
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrListProject.NotLogin().ToResp(), nil
	}

	// get params
	params, err := getListProjectsParam(r)
	if err != nil {
		return apierrors.ErrListProject.InvalidParameter(err).ToResp(), nil
	}

	// check permission
	internalClient := r.Header.Get(httputil.InternalHeader)
	if internalClient == "" {
		req := apistructs.PermissionCheckRequest{
			UserID:   identity.UserID,
			Scope:    apistructs.OrgScope,
			ScopeID:  params.OrgID,
			Resource: apistructs.ProjectResource,
			Action:   apistructs.ListAction,
		}
		perm, err := e.bdl.CheckPermission(&req)
		if err != nil {
			return apierrors.ErrListProject.InternalError(err).ToResp(), nil
		}
		if !perm.Access {
			return apierrors.ErrListProject.AccessDenied().ToResp(), nil
		}
	}

	// list project
	pagingProjects, err := e.bdl.ListProject(identity.UserID, *params)
	if err != nil {
		return apierrors.ErrListProject.InternalError(err).ToResp(), nil
	}
	if pagingProjects == nil {
		return httpserver.OkResp(&apistructs.PagingProjectDTO{})
	}

	if err := e.setProjectResource(pagingProjects.List); err != nil {
		return apierrors.ErrListProject.InternalError(err).ToResp(), nil
	}

	var userIDs []string
	for _, v := range pagingProjects.List {
		userIDs = append(userIDs, v.Owners...)
	}

	return httpserver.OkResp(*pagingProjects, userIDs)
}

func (e *Endpoints) getProjectStats(projectID uint64) (*apistructs.ProjectStats, error) {
	totalApp, err := e.bdl.CountAppByProID(projectID)
	if err != nil {
		return nil, errors.Errorf("get project states err: get app err: %v", err)
	}
	totalMembers, err := e.bdl.CountMembersWithoutExtraByScope(string(apistructs.ProjectScope), projectID)
	if err != nil {
		return nil, errors.Errorf("get project states err: get member err: %v", err)
	}

	iterations, err := e.db.FindIterations(projectID)
	if err != nil {
		return nil, errors.Errorf("get project states err: get iterations err: %v", err)
	}
	totalIterations := len(iterations)

	runningIterations, planningIterations := make([]int64, 0), make(map[int64]bool, 0)
	now := time.Now()
	for i := 0; i < totalIterations; i++ {
		if iterations[i].StartedAt != nil && iterations[i].FinishedAt != nil &&
			!iterations[i].StartedAt.After(now) && iterations[i].FinishedAt.After(now) {
			runningIterations = append(runningIterations, int64(iterations[i].ID))
		}

		if iterations[i].StartedAt != nil && iterations[i].StartedAt.After(now) {
			planningIterations[int64(iterations[i].ID)] = true
		}
	}

	var totalManHour, usedManHour, planningManHour, totalBug, doneBug int64
	totalIssues, _, err := e.db.PagingIssues(apistructs.IssuePagingRequest{
		IssueListRequest: apistructs.IssueListRequest{
			ProjectID: projectID,
			Type:      []apistructs.IssueType{apistructs.IssueTypeBug, apistructs.IssueTypeTask},
			External:  true,
		},
		PageNo:   1,
		PageSize: 99999,
	}, false)
	if err != nil {
		return nil, errors.Errorf("get project states err: get issues err: %v", err)
	}

	// 事件状态map
	closedBugStatsMap := make(map[int64]struct{}, 0)
	bugState, err := e.db.GetClosedBugState(int64(projectID))
	if err != nil {
		return nil, errors.Errorf("get project states err: get issues stats err: %v", err)
	}
	for _, v := range bugState {
		closedBugStatsMap[int64(v.ID)] = struct{}{}
	}

	for _, v := range totalIssues {
		var manHour apistructs.IssueManHour
		json.Unmarshal([]byte(v.ManHour), &manHour)
		// set total and used man-hour
		totalManHour += manHour.EstimateTime
		usedManHour += manHour.ElapsedTime
		// set plannning man-hour
		if _, ok := planningIterations[v.IterationID]; ok {
			planningManHour += manHour.EstimateTime
		}
		if v.Type == apistructs.IssueTypeBug {
			if _, ok := closedBugStatsMap[v.State]; ok {
				doneBug++
			}
			totalBug++
		}
	}

	tManHour, _ := strconv.ParseFloat(fmt.Sprintf("%.2f", float64(totalManHour)/480), 64)
	uManHour, _ := strconv.ParseFloat(fmt.Sprintf("%.2f", float64(usedManHour)/480), 64)
	pManHour, _ := strconv.ParseFloat(fmt.Sprintf("%.2f", float64(planningManHour)/480), 64)
	var dBugPer float64 = 100
	if totalBug != 0 {
		dBugPer, _ = strconv.ParseFloat(fmt.Sprintf("%.0f", float64(doneBug)*100/float64(totalBug)), 64)
	}
	return &apistructs.ProjectStats{
		CountApplications:       int(totalApp),
		CountMembers:            totalMembers,
		TotalApplicationsCount:  int(totalApp),
		TotalMembersCount:       totalMembers,
		TotalIterationsCount:    totalIterations,
		RunningIterationsCount:  len(runningIterations),
		PlanningIterationsCount: len(planningIterations),
		TotalManHourCount:       tManHour,
		UsedManHourCount:        uManHour,
		PlanningManHourCount:    pManHour,
		DoneBugCount:            doneBug,
		TotalBugCount:           totalBug,
		DoneBugPercent:          dBugPer,
	}, nil
}

// getListProjectsParam get list project param
func getListProjectsParam(r *http.Request) (*apistructs.ProjectListRequest, error) {
	// 获取企业Id
	orgIDStr := r.Header.Get(httputil.OrgHeader)
	if orgIDStr == "" {
		orgIDStr = r.URL.Query().Get("orgId")
		if orgIDStr == "" {
			return nil, errors.Errorf("invalid param, orgId is empty")
		}
	}
	orgID, err := strconv.ParseInt(orgIDStr, 10, 64)
	if err != nil {
		return nil, errors.Errorf("invalid param, orgId is invalid")
	}

	// 按项目名称搜索
	keyword := r.URL.Query().Get("q")

	var keepMsp bool
	keepMspStr := r.URL.Query().Get("keepMsp")
	if keepMspStr == "true" {
		keepMsp = true
	}

	// 获取pageSize
	pageSizeStr := r.URL.Query().Get("pageSize")
	if pageSizeStr == "" {
		pageSizeStr = "20"
	}
	pageSize, err := strconv.Atoi(pageSizeStr)
	if err != nil {
		return nil, errors.Errorf("invalid param, pageSize is invalid")
	}
	// 获取pageNo
	pageNoStr := r.URL.Query().Get("pageNo")
	if pageNoStr == "" {
		pageNoStr = "1"
	}
	pageNo, err := strconv.Atoi(pageNoStr)
	if err != nil {
		return nil, errors.Errorf("invalid param, pageNo is invalid")
	}
	// 获取isPublic
	var isPublic bool
	isPublicStr := r.URL.Query().Get("is_public")
	if isPublicStr == "true" {
		isPublic = true
	}
	var asc bool
	ascStr := r.URL.Query().Get("asc")
	if ascStr == "true" {
		asc = true
	}
	orderBy := r.URL.Query().Get("orderBy")
	switch orderBy {
	case "cpuQuota":
		orderBy = "cpu_quota"
	case "memQuota":
		orderBy = "mem_quota"
	case "activeTime":
		orderBy = "active_time"
	case "name":
		orderBy = "name"
	default:
		orderBy = ""
	}

	return &apistructs.ProjectListRequest{
		OrgID:    uint64(orgID),
		Query:    keyword,
		Name:     r.URL.Query().Get("name"),
		PageNo:   pageNo,
		PageSize: pageSize,
		OrderBy:  orderBy,
		Asc:      asc,
		IsPublic: isPublic,
		KeepMsp:  keepMsp,
	}, nil
}

func (e *Endpoints) getProjectID(vars map[string]string) (uint64, error) {
	projectIDStr := vars["projectID"]
	if projectIDStr == "" {
		return 0, fmt.Errorf("empty project id")
	}
	projectID, err := strconv.ParseUint(projectIDStr, 10, 64)
	if err != nil {
		return 0, err
	}
	return projectID, nil
}

func (e *Endpoints) getOrgID(vars map[string]string) (int64, error) {
	orgIDStr := vars["orgID"]
	if orgIDStr == "" {
		return 0, fmt.Errorf("empty org id")
	}
	orgID, err := strconv.ParseInt(orgIDStr, 10, 64)
	if err != nil {
		return 0, err
	}
	return orgID, nil
}

// SetProjectStatsCache 设置项目状态缓存
func SetProjectStatsCache() {
	c := cron.New()
	if err := c.AddFunc(conf.ProjectStatsCacheCron(), func() {
		// 清空缓存
		logrus.Info("start set project stats")
		ProjectStatsCache = new(sync.Map)
	}); err != nil {
		logrus.Errorf("cron set setProjectStatsCache failed: %v", err)
	}

	c.Start()
}

func (e *Endpoints) setProjectResource(projectDTOs []apistructs.ProjectDTO) error {
	projectIDs := make([]uint64, 0, len(projectDTOs))
	for i := range projectDTOs {
		projectIDs = append(projectIDs, uint64(projectDTOs[i].ID))
	}
	resp, err := e.bdl.ProjectResource(projectIDs)
	if err != nil {
		return err
	}
	for i := range projectDTOs {
		if v, ok := resp.Data[projectDTOs[i].ID]; ok {
			projectDTOs[i].CpuServiceUsed = v.CpuServiceUsed
			projectDTOs[i].MemServiceUsed = v.MemServiceUsed
			projectDTOs[i].CpuAddonUsed = v.CpuAddonUsed
			projectDTOs[i].MemAddonUsed = v.MemAddonUsed
		}
	}
	return nil
}

func (e *Endpoints) ExportProjectTemplate(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var exportReq apistructs.ExportProjectTemplateRequest
	projectID, err := e.getProjectID(vars)
	if err != nil {
		return apierrors.ErrExportProjectTemplate.InvalidParameter("projectID").ToResp(), nil
	}
	exportReq.ProjectID = projectID
	orgID, err := e.getOrgID(vars)
	if err != nil {
		return apierrors.ErrExportProjectTemplate.InvalidParameter("orgID").ToResp(), nil
	}
	exportReq.OrgID = orgID
	// check permission
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrExportProjectTemplate.NotLogin().ToResp(), nil
	}
	exportReq.IdentityInfo = identityInfo
	if exportReq.OrgID == 0 || exportReq.ProjectID == 0 {
		return apierrors.ErrExportProjectTemplate.InvalidParameter(fmt.Errorf("orgID and projectID can't be empty")).ToResp(), nil
	}
	if !identityInfo.IsInternalClient() {
		access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.OrgScope,
			ScopeID:  uint64(exportReq.OrgID),
			Resource: apistructs.ProjectTemplateResource,
			Action:   apistructs.GetAction,
		})
		if err != nil {
			return apierrors.ErrExportProjectTemplate.InternalError(err).ToResp(), nil
		}
		if !access.Access {
			return apierrors.ErrExportProjectTemplate.AccessDenied().ToResp(), nil
		}
	}
	project, err := e.bdl.GetProject(exportReq.ProjectID)
	if err != nil {
		return apierrors.ErrExportProjectTemplate.InternalError(err).ToResp(), nil
	}
	if project.OrgID != uint64(exportReq.OrgID) {
		return apierrors.ErrImportProjectTemplate.InvalidParameter("projectID").ToResp(), nil
	}
	exportReq.ProjectName = project.Name
	exportReq.ProjectDisplayName = project.DisplayName

	fileID, err := e.project.ExportTemplate(exportReq)
	if err != nil {
		return apierrors.ErrExportProjectTemplate.InternalError(err).ToResp(), nil
	}

	ok, _, err := e.testcase.GetFirstFileReady(apistructs.FileSpaceActionTypeExport)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	if ok {
		e.ExportChannel <- fileID
	}

	return httpserver.OkResp(fileID)
}

func (e *Endpoints) ImportProjectTemplate(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrImportExcelIssue.NotLogin().ToResp(), nil
	}
	var req apistructs.ImportProjectTemplateRequest
	projectID, err := e.getProjectID(vars)
	if err != nil {
		return apierrors.ErrImportProjectTemplate.InvalidParameter("projectID").ToResp(), nil
	}
	req.ProjectID = projectID
	orgID, err := e.getOrgID(vars)
	if err != nil {
		return apierrors.ErrImportProjectTemplate.InvalidParameter("orgID").ToResp(), nil
	}
	req.OrgID = orgID
	req.IdentityInfo = identityInfo
	if req.ProjectID == 0 {
		return apierrors.ErrExportProjectTemplate.InvalidParameter("projectID").ToResp(), nil
	}
	if req.OrgID == 0 {
		return apierrors.ErrImportProjectTemplate.InvalidParameter("orgID").ToResp(), nil
	}
	if !identityInfo.IsInternalClient() {
		access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.OrgScope,
			ScopeID:  uint64(req.OrgID),
			Resource: apistructs.ProjectTemplateResource,
			Action:   apistructs.CreateAction,
		})
		if err != nil {
			return apierrors.ErrImportProjectTemplate.InternalError(err).ToResp(), nil
		}
		if !access.Access {
			return apierrors.ErrImportProjectTemplate.AccessDenied().ToResp(), nil
		}
	}
	project, err := e.bdl.GetProject(req.ProjectID)
	if err != nil {
		return apierrors.ErrExportProjectTemplate.InternalError(err).ToResp(), nil
	}
	if project.OrgID != uint64(req.OrgID) {
		return apierrors.ErrImportProjectTemplate.InvalidParameter("projectID").ToResp(), nil
	}
	req.ProjectName = project.Name
	req.ProjectDisplayName = project.DisplayName
	recordID, err := e.project.ImportTemplate(req, r)
	if err != nil {
		return apierrors.ErrImportProjectTemplate.InternalError(err).ToResp(), nil
	}

	ok, _, err := e.testcase.GetFirstFileReady(apistructs.FileSpaceActionTypeImport)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	if ok {
		e.ImportChannel <- recordID
	}
	return httpserver.OkResp(recordID)
}

func (e *Endpoints) ParseProjectTemplate(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	file, _, err := r.FormFile("file")
	if err != nil {
		return apierrors.ErrParseProjectTemplate.InvalidParameter("file").ToResp(), nil
	}
	templateData, err := e.project.ParseTemplatePackage(file)
	if err != nil {
		return apierrors.ErrParseProjectTemplate.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(templateData)
}

func (e *Endpoints) ExportProjectPackage(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	ppReq, apiErr := e.ConstructProjectPackgeRequset(r, vars, apierrors.ErrExportProjectPackage)
	if apiErr != nil {
		return apiErr.ToResp(), nil
	}
	exportReq := apistructs.ExportProjectPackageRequest{
		ProjectPackageRequest: *ppReq,
	}

	artifacts := []apistructs.Artifact{}
	if err := json.NewDecoder(r.Body).Decode(&artifacts); err != nil {
		return apierrors.ErrExportProjectPackage.InvalidParameter(fmt.Errorf("invalid artifacts: %v", err)).ToResp(), nil
	}
	if len(artifacts) == 0 {
		return apierrors.ErrExportProjectPackage.InvalidParameter(fmt.Errorf("artifacts can't be empty")).ToResp(), nil
	}
	exportReq.Artifacts = artifacts

	fileID, err := e.project.ExportPackage(exportReq)
	if err != nil {
		return apierrors.ErrExportProjectPackage.InternalError(err).ToResp(), nil
	}

	ok, _, err := e.testcase.GetFirstFileReady(apistructs.FileProjectPackageExport)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	if ok {
		e.ExportChannel <- fileID
	}

	return httpserver.OkResp(fileID)
}

func (e *Endpoints) ImportProjectPackage(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	ippReq, apiErr := e.ConstructProjectPackgeRequset(r, vars, apierrors.ErrImportProjectPackage)
	if apiErr != nil {
		return apiErr.ToResp(), nil
	}

	req := apistructs.ImportProjectPackageRequest{
		ProjectPackageRequest: *ippReq,
	}

	recordID, err := e.project.ImportPackage(req, r)
	if err != nil {
		return apierrors.ErrImportProjectPackage.InternalError(err).ToResp(), nil
	}

	ok, _, err := e.testcase.GetFirstFileReady(apistructs.FileProjectPackageImport)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	if ok {
		e.ImportChannel <- recordID
	}
	return httpserver.OkResp(recordID)
}

func (e *Endpoints) ConstructProjectPackgeRequset(r *http.Request, vars map[string]string, apiErr *errorresp.APIError) (*apistructs.ProjectPackageRequest, *errorresp.APIError) {
	projectID, err := e.getProjectID(vars)
	if err != nil {
		return nil, apiErr.InvalidParameter("projectID")
	}
	if projectID == 0 {
		return nil, apiErr.InvalidParameter("projectID")
	}
	orgID, err := e.getOrgID(vars)
	if err != nil {
		return nil, apiErr.InvalidParameter("orgID")
	}
	if orgID == 0 {
		return nil, apiErr.InvalidParameter("orgID")
	}
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return nil, apiErr.NotLogin()
	}
	if !identityInfo.IsInternalClient() {
		access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.OrgScope,
			ScopeID:  uint64(orgID),
			Resource: apistructs.ProjectPackageResource,
			Action:   apistructs.CreateAction,
		})
		if err != nil {
			return nil, apiErr.InternalError(err)
		}
		if !access.Access {
			return nil, apiErr.AccessDenied()
		}
	}

	org, err := e.bdl.GetOrg(orgID)
	if err != nil {
		return nil, apiErr.InternalError(err)
	}
	project, err := e.bdl.GetProject(projectID)
	if err != nil {
		return nil, apiErr.InternalError(err)
	}
	if project.OrgID != uint64(orgID) {
		return nil, apiErr.InvalidParameter("projectID")
	}

	req := &apistructs.ProjectPackageRequest{}
	req.OrgID = uint64(orgID)
	req.OrgName = org.Name
	req.ProjectID = projectID
	req.ProjectName = project.Name
	req.ProjectDisplayName = project.DisplayName
	req.IdentityInfo = identityInfo

	return req, nil
}

func (e *Endpoints) ParseProjectPackage(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	file, _, err := r.FormFile("file")
	if err != nil {
		return apierrors.ErrParseProjectPackage.InvalidParameter("file").ToResp(), nil
	}
	packageData, err := e.project.ParsePackage(file)
	if err != nil {
		return apierrors.ErrParseProjectPackage.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(packageData)
}
