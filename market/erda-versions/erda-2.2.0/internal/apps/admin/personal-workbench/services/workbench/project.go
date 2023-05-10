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

package workbench

import (
	"context"
	"fmt"
	"runtime/debug"
	"strconv"
	"sync"

	"github.com/sirupsen/logrus"
	"google.golang.org/grpc/metadata"

	"github.com/erda-project/erda-infra/pkg/transport"
	menupb "github.com/erda-project/erda-proto-go/msp/menu/pb"
	projpb "github.com/erda-project/erda-proto-go/msp/tenant/project/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/http/httputil"
)

type UrlParams struct {
	Env         string `json:"env"`
	AddonId     string `json:"addonId"`
	TerminusKey string `json:"terminusKey"`
	TenantGroup string `json:"tenantGroup"`
}

func (w *Workbench) GetProjNum(identity apistructs.Identity, query string) (int, error) {
	orgID, err := strconv.Atoi(identity.OrgID)
	if err != nil {
		return 0, err
	}
	req := apistructs.ProjectListRequest{
		OrgID:    uint64(orgID),
		Query:    query,
		PageNo:   1,
		PageSize: 1,
		KeepMsp:  true,
	}
	projectDTO, err := w.bdl.ListMyProject(identity.UserID, req)
	if err != nil {
		return 0, err
	}
	if projectDTO == nil {
		return 0, nil
	}
	return projectDTO.Total, nil
}

func (w *Workbench) ListProjWbOverviewData(identity apistructs.Identity, projects []apistructs.ProjectDTO) (list []apistructs.WorkbenchProjOverviewItem, err error) {
	var (
		pidList       []uint64
		statisticInfo []*projpb.Project
	)

	issueInfo := &apistructs.WorkbenchResponse{}
	staMapInfo := make(map[uint64]*projpb.Project)

	orgID, err := strconv.Atoi(identity.OrgID)
	if err != nil {
		return nil, err
	}

	for _, p := range projects {
		pidList = append(pidList, p.ID)
	}

	wg := sync.WaitGroup{}
	wg.Add(1)

	// get project issue related info
	go func() {
		defer func() {
			if err := recover(); err != nil {
				logrus.Errorf("")
				logrus.Errorf("%s", debug.Stack())
			}
			// release
			wg.Done()
		}()

		req := apistructs.WorkbenchRequest{
			OrgID:      uint64(orgID),
			ProjectIDs: pidList,
		}
		res, err := w.bdl.GetWorkbenchData(identity.UserID, req)
		if err != nil {
			logrus.Warnf("get project workbench issue info failed, request: %+v, error: %v", req, err)
			return
		}
		issueInfo = res
	}()

	// get project msp statistic related info
	// go func() {
	// 	defer func() {
	// 		if err := recover(); err != nil {
	// 			logrus.Errorf("")
	// 			logrus.Errorf("%s", debug.Stack())
	// 		}
	// 		// release
	// 		wg.Done()
	// 	}()

	// 	res, err := w.bdl.GetMSPTenantProjects(identity.UserID, identity.OrgID, true, pidList)
	// 	if err != nil {
	// 		logrus.Warnf("get project workbench statistic info failed, request: %+v, error: %v", pidList, err)
	// 		return
	// 	}
	// 	statisticInfo = res
	// }()

	// wait complete
	wg.Wait()

	for _, v := range statisticInfo {
		if v != nil {
			tmp := v
			pid, er := strconv.Atoi(tmp.Id)
			if er != nil {
				err = fmt.Errorf("parse msp project id failed, id: %v, error: %v", tmp.Id, err)
				return
			}
			staMapInfo[uint64(pid)] = tmp
		}
	}

	// construct project workbench overview info which may contains issue/statistic info
	for i := range projects {
		p := projects[i]
		item := apistructs.WorkbenchProjOverviewItem{
			ProjectDTO: p,
		}
		is := issueInfo.Data[p.ID]
		if is != nil {
			item.IssueInfo = &apistructs.ProjectIssueInfo{
				TotalIssueNum:       is.TotalIssueNum,
				UnSpecialIssueNum:   is.UnSpecialIssueNum,
				ExpiredIssueNum:     is.ExpiredIssueNum,
				ExpiredOneDayNum:    is.ExpiredOneDayNum,
				ExpiredTomorrowNum:  is.ExpiredTomorrowNum,
				ExpiredSevenDayNum:  is.ExpiredSevenDayNum,
				ExpiredThirtyDayNum: is.ExpiredThirtyDayNum,
				FeatureDayNum:       is.FeatureDayNum,
			}
		}
		st := staMapInfo[p.ID]
		if st != nil {
			item.StatisticInfo = &apistructs.ProjectStatisticInfo{
				ServiceCount:      st.ServiceCount,
				Last24HAlertCount: st.Last24HAlertCount,
			}
		}

		list = append(list, item)
	}
	return list, nil
}

func (w *Workbench) ListSubProjWbData(identity apistructs.Identity) (data *apistructs.WorkbenchProjOverviewRespData, err error) {
	var (
		projects []apistructs.ProjectDTO
		pidList  []uint64
	)
	data = &apistructs.WorkbenchProjOverviewRespData{}

	subList, err := w.bdl.ListSubscribes(identity.UserID, identity.OrgID, apistructs.GetSubscribeReq{Type: apistructs.ProjectSubscribe})
	if err != nil {
		logrus.Errorf("list subscribes failed, error: %v", err)
		return
	}
	if subList == nil || len(subList.List) == 0 {
		return
	}
	for _, v := range subList.List {
		pidList = append(pidList, v.TypeID)
	}
	rsp, err := w.bdl.GetProjectsMap(apistructs.GetModelProjectsMapRequest{ProjectIDs: pidList, KeepMsp: true})
	if err != nil {
		logrus.Errorf("get projects failed, error: %v", err)
		return nil, err
	}
	for i := range rsp {
		projects = append(projects, rsp[i])
	}

	list, err := w.ListProjWbOverviewData(identity, projects)
	if err != nil {
		logrus.Errorf("list project workbench overview data failed, error: %v", err)
		return nil, err
	}

	data = &apistructs.WorkbenchProjOverviewRespData{
		Total: len(projects),
		List:  list,
	}

	return
}

func (w *Workbench) ListQueryProjWbData(identity apistructs.Identity, page apistructs.PageRequest, query string) (data *apistructs.WorkbenchProjOverviewRespData, err error) {

	data = &apistructs.WorkbenchProjOverviewRespData{}
	orgID, err := strconv.Atoi(identity.OrgID)
	if err != nil {
		return
	}
	req := apistructs.ProjectListRequest{
		OrgID:    uint64(orgID),
		PageNo:   int(page.PageNo),
		PageSize: int(page.PageSize),
		Query:    query,
		KeepMsp:  true,
	}
	projectDTO, err := w.bdl.ListMyProject(identity.UserID, req)
	if err != nil {
		logrus.Errorf("list my project failed, request: %v, error: %v", req, err)
		return
	}
	if projectDTO == nil || len(projectDTO.List) == 0 {
		logrus.Warnf("list my project get empty response")
		return
	}

	list, err := w.ListProjWbOverviewData(identity, projectDTO.List)
	if err != nil {
		logrus.Errorf("list project workbench overview data failed, error: %v", err)
		return
	}

	data = &apistructs.WorkbenchProjOverviewRespData{
		Total: projectDTO.Total,
		List:  list,
	}

	return
}

// GetUrlCommonParams get url params used by icon
func (w *Workbench) GetUrlCommonParams(userID, orgID string, projectIDs []uint64) (urlParams []UrlParams, err error) {
	urlParams = make([]UrlParams, len(projectIDs))
	ctx := transport.WithHeader(context.Background(), metadata.New(map[string]string{
		httputil.InternalHeader: "admin",
		httputil.UserHeader:     userID,
		httputil.OrgHeader:      orgID,
	}))
	var pidList []string
	for _, p := range projectIDs {
		pidList = append(pidList, strconv.Itoa(int(p)))
	}
	resp, err := w.tenantProjectSvc.GetProjects(ctx, &projpb.GetProjectsRequest{
		ProjectId: pidList,
		WithStats: false,
	})
	if err != nil {
		logrus.Errorf("failed to get msp tenant project , err: %v", err)
		return
	}
	projectDTO := resp.Data
	for i, project := range projectDTO {
		var menues []*menupb.MenuItem
		urlParams[i].Env = project.Relationship[len(project.Relationship)-1].Workspace
		tenantId := project.Relationship[len(project.Relationship)-1].TenantID
		urlParams[i].TenantGroup = tenantId
		urlParams[i].AddonId = tenantId
		pType := project.Type

		resp, err := w.menuSvc.GetMenu(ctx, &menupb.GetMenuRequest{
			TenantId: tenantId,
			Type:     pType,
		})
		if err != nil || len(resp.Data) == 0 {
			logrus.Errorf("failed to get env and tenant id ,err: %v", err)
			continue
		}
		menues = resp.Data
		if tg, ok := menues[len(menues)-1].Params["tenantGroup"]; ok {
			urlParams[i].TenantGroup = tg
		}
		if tk, ok := menues[len(menues)-1].Params["terminusKey"]; ok {
			urlParams[i].TerminusKey = tk
		}
	}
	return
}

// GetMspUrlParamsMap get url params used by icon
func (w *Workbench) GetMspUrlParamsMap(identity apistructs.Identity, projectIDs []uint64, limit int) (urlParams map[string]UrlParams, err error) {
	urlParams = make(map[string]UrlParams)
	var projectDTO []*projpb.Project
	// projectDTO, err := w.bdl.GetMSPTenantProjects(identity.UserID, identity.OrgID, false, projectIDs)
	// if err != nil {
	// 	logrus.Errorf("failed to get msp tenant project , err: %v", err)
	// 	return
	// }

	if limit <= 0 {
		limit = 5
	}

	store := new(sync.Map)
	limitCh := make(chan struct{}, limit)
	wg := sync.WaitGroup{}
	defer close(limitCh)

	for _, project := range projectDTO {
		// get
		limitCh <- struct{}{}
		wg.Add(1)

		go func(project *projpb.Project) {
			defer func() {
				if err := recover(); err != nil {
					logrus.Errorf("")
					logrus.Errorf("%s", debug.Stack())
				}
				// release
				<-limitCh
				wg.Done()
			}()

			params, err := w.GetMspUrlParams(identity.UserID, identity.OrgID, project)
			if err != nil {
				logrus.Errorf("get msp url params failed, request: %v, error: %v", project, err)
				return
			}
			store.Store(project.Id, params)
		}(project)
	}

	// wait done
	wg.Wait()
	store.Range(func(k interface{}, v interface{}) bool {
		id, ok := k.(string)
		if !ok {
			err = fmt.Errorf("project id: [string], assert failed")
			return false
		}
		param, ok := v.(UrlParams)
		if !ok {
			err = fmt.Errorf("UrlParams, assert failed")
			return false
		}
		urlParams[id] = param
		return true
	})

	return
}

// GetMspUrlParams get url params used by icon
func (w *Workbench) GetMspUrlParams(userID, orgID string, project *projpb.Project) (urlParams UrlParams, err error) {
	var menues []*menupb.MenuItem

	urlParams.Env = project.Relationship[len(project.Relationship)-1].Workspace
	tenantId := project.Relationship[len(project.Relationship)-1].TenantID
	urlParams.TenantGroup = tenantId
	urlParams.AddonId = tenantId
	pType := project.Type

	ctx := transport.WithHeader(context.Background(), metadata.New(map[string]string{
		httputil.InternalHeader: "admin",
		httputil.UserHeader:     userID,
		httputil.OrgHeader:      orgID,
	}))
	resp, err := w.menuSvc.GetMenu(ctx, &menupb.GetMenuRequest{
		TenantId: tenantId,
		Type:     pType,
	})
	if err != nil || len(resp.Data) == 0 {
		logrus.Errorf("failed to get env and tenant id ,err: %v", err)
		return
	}
	menues = resp.Data
	if tg, ok := menues[len(menues)-1].Params["tenantGroup"]; ok {
		urlParams.TenantGroup = tg
	}
	if tk, ok := menues[len(menues)-1].Params["terminusKey"]; ok {
		urlParams.TerminusKey = tk
	}
	return
}
