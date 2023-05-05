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
	"os"

	"github.com/erda-project/erda/bundle"
)

func initBundle() *bundle.Bundle {
	os.Setenv("CORE_SERVICES_ADDR", "http://core-services.project-387-dev.svc.cluster.local:9526")
	os.Setenv("MSP_ADDR", "http://msp.project-387-dev.svc.cluster.local:8080")
	os.Setenv("DOP_ADDR", "http://dop.project-387-dev.svc.cluster.local:9527")
	os.Setenv("ORCHESTRATOR_ADDR", "http://orchestrator.project-387-dev.svc.cluster.local:8081")
	os.Setenv("GITTAR_ADDR", "http://gittar.project-387-dev.svc.cluster.local:5566")
	bdl := bundle.New(
		bundle.WithCoreServices(),
		bundle.WithMSP(),
		bundle.WithDOP(),
		bundle.WithGittar(),
		bundle.WithOrchestrator(),
	)
	return bdl
}

//func TestListProj(t *testing.T) {
//	bdl := initBundle()
//	wb := New(WithBundle(bdl))
//	identity := apistructs.Identity{
//		UserID: "2",
//		OrgID:  "1",
//	}
//
//	for i := 1; i < 10; i += 1 {
//		data, err := wb.ListQueryProjWbData(identity, apistructs.PageRequest{PageNo: uint64(i), PageSize: 1}, "")
//		for _, v := range data.List {
//			if v.ProjectDTO.Type == "MSP" {
//				t.Logf("%v, %v", v.ProjectDTO.ID, v.ProjectDTO.Name)
//			}
//		}
//		if err != nil {
//			t.Logf("list query proj wb data faield, error: %v", err)
//		}
//		// t.Logf("data: %v", data)
//	}
//
//}
//
//func TestListSub(t *testing.T) {
//	bdl := initBundle()
//	wb := New(WithBundle(bdl))
//	identity := apistructs.Identity{
//		// 12028
//		UserID: "12028",
//		OrgID:  "1",
//	}
//
//	data, err := wb.ListSubProjWbData(identity)
//	if err != nil {
//		t.Logf("list query proj wb data faield, error: %v", err)
//	}
//	t.Logf("data: %+v", data)
//}
//
//func TestCreateSub(t *testing.T) {
//	bdl := initBundle()
//	identity := apistructs.Identity{
//		// 12028
//		UserID: "12028",
//		OrgID:  "1",
//	}
//	req := apistructs.CreateSubscribeReq{
//		Type:   "project",
//		TypeID: 3,
//		Name:   "go-demo",
//		UserID: "2",
//		OrgID:  1,
//	}
//
//	data, err := bdl.CreateSubscribe(identity.UserID, identity.OrgID, req)
//	if err != nil {
//		t.Logf("list query proj wb data faield, error: %v", err)
//	}
//	t.Logf("data: %+v", data)
//}
//
//func TestListApp(t *testing.T) {
//	bdl := initBundle()
//	wb := New(WithBundle(bdl))
//	identity := apistructs.Identity{
//		// 12028
//		UserID: "2",
//		OrgID:  "1",
//	}
//
//	data, err := wb.ListAppWbData(identity, apistructs.ApplicationListRequest{PageNo: 1, PageSize: 1}, 1)
//	if err != nil {
//		t.Logf("list query proj wb data faield, error: %v", err)
//	}
//	t.Logf("data: %v", data)
//}
//
//func TestListSubApp(t *testing.T) {
//	bdl := initBundle()
//	wb := New(WithBundle(bdl))
//	identity := apistructs.Identity{
//		// 12028
//		UserID: "12028",
//		OrgID:  "1",
//	}
//
//	data, err := wb.ListSubAppWbData(identity, 1)
//	if err != nil {
//		t.Logf("list query proj wb data faield, error: %v", err)
//	}
//	t.Logf("data: %v", data)
//}
//
//func TestGetAppMr(t *testing.T) {
//	bdl := initBundle()
//	// wb := New(WithBundle(bdl))
//	identity := apistructs.Identity{
//		UserID: "2",
//		OrgID:  "1",
//	}
//	rsp, err := bdl.ListMergeRequest(45, identity.UserID, apistructs.GittarQueryMrRequest{})
//	if err != nil {
//		t.Errorf("error: %v", err)
//	}
//	t.Logf("response: %+v", rsp)
//}
//
//func TestStateIds(t *testing.T) {
//	bdl := initBundle()
//	wb := New(WithBundle(bdl))
//	ids, err := wb.GetAllIssueStateIDs(3)
//	if err != nil {
//		t.Errorf("error: %v", err)
//	}
//	t.Logf("ids: %v", ids)
//}
//
//func TestGetUrlQueries(t *testing.T) {
//	bdl := initBundle()
//	wb := New(WithBundle(bdl))
//	r, err := wb.GetIssueQueries(3)
//	if err != nil {
//		t.Errorf("error: %v", err)
//	}
//	t.Logf("result: %+v", r)
//}
//
//func TestListIssueStreams(t *testing.T) {
//	bdl := initBundle()
//	wb := New(WithBundle(bdl))
//
//	identity := apistructs.Identity{
//		UserID: "2",
//		OrgID:  "1",
//	}
//
//	// list unread message
//	req := apistructs.QueryMBoxRequest{PageNo: int64(1),
//		PageSize: int64(10),
//		Status:   apistructs.MBoxUnReadStatus,
//		Type:     apistructs.MBoxTypeIssue,
//	}
//
//	ms, err := bdl.ListMbox(identity, req)
//	if err != nil {
//		logrus.Errorf("list unread messages failed, identity: %+v,request: %+v, error:%v", identity, req, err)
//		return
//	}
//
//	var ids []uint64
//	for _, v := range ms.List {
//		id, err := getIssueID(v.DeduplicateID)
//		if err != nil {
//			logrus.Warnf("get issue id failed, error: %v", err)
//		}
//		ids = append(ids, id)
//	}
//
//	r, err := wb.ListIssueStreams(ids, 0)
//	if err != nil {
//		t.Errorf("error: %v", err)
//	}
//	for _, v := range r {
//		tr := rt.Readable(v.UpdatedAt).String()
//		t.Logf("update time: %v, read: %v", v.UpdatedAt, tr)
//	}
//
//	t.Logf("result: %+v", r)
//}
//
//func TestListMsg(t *testing.T) {
//	bdl := initBundle()
//	req := apistructs.QueryMBoxRequest{PageNo: 1,
//		PageSize: 10,
//		Status:   apistructs.MBoxUnReadStatus,
//		Type:     apistructs.MBoxTypeIssue,
//	}
//	identity := apistructs.Identity{
//		UserID: "2",
//		OrgID:  "1",
//	}
//	res, err := bdl.ListMbox(identity, req)
//	if err != nil {
//		t.Errorf("get issue failed, error: %v", err)
//	}
//	t.Logf("response: %+v", res)
//
//}
//
//func getIssueID(deduplicateID string) (uint64, error) {
//	if deduplicateID == "" {
//		return 0, fmt.Errorf("deduplicate id is nil")
//	}
//	sli := strings.SplitN(deduplicateID, "-", 2)
//	idStr := sli[1]
//	id, err := strconv.Atoi(idStr)
//	if err != nil {
//		logrus.Errorf("parse deduplicateID failed, content: %v, error: %v", deduplicateID, err)
//		return 0, err
//	}
//	return uint64(id), nil
//}
//
//func TestTime(t *testing.T) {
//	t1 := time.Now().Add(-1 * time.Hour)
//	re := rt.Readable(t1).String()
//	t.Logf("content: %v", re)
//
//}
//func getReadableTimeText(t time.Time) string {
//	r := rt.Readable(t).String()
//	return r
//}
//
//func TestListProjWbOverviewData(t *testing.T)  {
//	bdl := initBundle()
//	wb := New(WithBundle(bdl))
//	req := apistructs.ProjectListRequest{
//		OrgID:    1,
//		PageNo:   1,
//		PageSize: 10,
//		KeepMsp:  true,
//	}
//	pjs, err := bdl.ListMyProject("2", req)
//	if err != nil {
//		t.Errorf("error: %v", err)
//		return
//	}
//
//	identity := apistructs.Identity{
//		UserID: "2",
//		OrgID:  "1",
//	}
//
//	pn, err := wb.GetProjNum(identity, "")
//	if err != nil {
//		t.Errorf("error: %v", err)
//		return
//	}
//	t.Logf("project num: %v", pn)
//
//	an, err := wb.GetAppNum(identity, "")
//	if err != nil {
//		t.Errorf("error: %v", err)
//		return
//	}
//	t.Logf("app num: %v", an)
//
//
//	var ids []uint64
//	for _, v := range pjs.List {
//		ids = append(ids, v.ID)
//	}
//
//	data, err := wb.GetUrlCommonParams(identity.UserID, identity.OrgID, ids)
//	if err != nil {
//		t.Errorf("error: %v", err)
//		return
//	}
//	t.Logf("data: %v", data)
//
//	//for _, v := range pjs.List {
//	//	t.Logf("my project, id: %v, type: %v", v.ID, v.Type)
//	//}
//	//
//	//data, err := wb.ListProjWbOverviewData(identity, pjs.List)
//	//if err != nil {
//	//	t.Logf("error: %v", err)
//	//	return
//	//}
//	//for _, v := range data {
//	//	t.Logf("issue project, id: %v, type: %v", v.ProjectDTO.ID, v.ProjectDTO.Type)
//	//}
//	//t.Logf("workbench data: %v", data)
//
//
//
//	msp, err := wb.GetMspUrlParamsMap(identity, ids, 0)
//	if err != nil {
//		t.Errorf("error: %v", err)
//		return
//	}
//	t.Logf("msp: %v", msp)
//}
