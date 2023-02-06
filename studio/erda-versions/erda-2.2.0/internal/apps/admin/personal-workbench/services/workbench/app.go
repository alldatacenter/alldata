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
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
)

func (w *Workbench) GetAppNum(identity apistructs.Identity, query string) (int, error) {
	orgID, err := strconv.Atoi(identity.OrgID)
	if err != nil {
		return 0, err
	}
	req := apistructs.ApplicationListRequest{
		PageNo:   1,
		PageSize: 1,
		Query:    query,
		IsSimple: true,
	}
	appDTO, err := w.bdl.GetAllMyApps(identity.UserID, uint64(orgID), req)
	if err != nil {
		return 0, err
	}
	if appDTO == nil {
		return 0, nil
	}
	return appDTO.Total, nil
}

// ListAppWbData
// default set pageSize/pageNo; when need query, set query field
func (w *Workbench) ListAppWbData(identity apistructs.Identity, req apistructs.ApplicationListRequest, limit int) (data *apistructs.AppWorkbenchResponseData, err error) {
	var (
		appIDs []uint64
		orgID  int
	)
	data = &apistructs.AppWorkbenchResponseData{}
	orgID, err = strconv.Atoi(identity.OrgID)
	if err != nil {
		return
	}
	req.OrderBy = "name"
	req.IsSimple = false

	// list app
	appRes, err := w.bdl.GetAllMyApps(identity.UserID, uint64(orgID), req)
	if err != nil {
		logrus.Errorf("get my apps failed, identity: %v, request: %v, error: %v", identity, req, err)
		return
	}

	if appRes == nil || len(appRes.List) == 0 {
		logrus.Warnf("get my apps empty response, request: %v", req)
		return
	}

	for i := range appRes.List {
		appIDs = append(appIDs, appRes.List[i].ID)
	}

	// list app related open mr
	mrResult, err := w.bdl.MergeRequestCount(identity.UserID, apistructs.MergeRequestCountRequest{
		AppIDs: appIDs,
		State:  "open",
	})
	if err != nil {
		logrus.Errorf("list open mr failed, appIDs: %v, error: %v", appIDs, err)
		return
	}

	// construct AppWorkBenchItem
	data.TotalApps = appRes.Total
	for i := range appRes.List {
		data.List = append(data.List, apistructs.AppWorkBenchItem{
			ApplicationDTO: appRes.List[i],
			AppRuntimeNum:  int(appRes.List[i].Stats.CountRuntimes),
			AppOpenMrNum:   mrResult[strconv.FormatUint(appRes.List[i].ID, 10)],
		})
	}
	return
}

func (w *Workbench) ListSubAppWbData(identity apistructs.Identity, limit int) (data *apistructs.AppWorkbenchResponseData, err error) {
	var (
		idList []uint64
	)
	data = &apistructs.AppWorkbenchResponseData{}
	subList, err := w.bdl.ListSubscribes(identity.UserID, identity.OrgID, apistructs.GetSubscribeReq{Type: apistructs.AppSubscribe})
	if err != nil {
		logrus.Errorf("list subscribes failed, error: %v", err)
		return
	}
	if subList == nil || len(subList.List) == 0 {
		return
	}
	for _, v := range subList.List {
		idList = append(idList, v.TypeID)
	}

	req := apistructs.ApplicationListRequest{
		PageNo:        1,
		PageSize:      len(idList),
		ApplicationID: idList,
	}

	return w.ListAppWbData(identity, req, limit)
}
