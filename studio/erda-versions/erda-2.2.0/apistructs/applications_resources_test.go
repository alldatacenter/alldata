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

package apistructs_test

import (
	"strconv"
	"strings"
	"testing"

	"github.com/erda-project/erda/apistructs"
)

func TestApplicationsResourcesItem_AddResource(t *testing.T) {
	var i = new(apistructs.ApplicationsResourcesItem)
	i.AddResource("prod", 1, 10, 50)
	i.AddResource("staging", 1, 20, 60)
	i.AddResource("test", 1, 30, 70)
	i.AddResource("dev", 1, 40, 80)
	if i.PodsCount != 4 {
		t.Fatal("i.PodsCount error")
	}
	if i.CPURequest != 10+20+30+40 {
		t.Fatal("i.CPURequest error")
	}
	if i.MemRequest != 50+60+70+80 {
		t.Fatal("i.MemRequest error")
	}
}

func TestApplicationsResourcesResponse_OrderBy(t *testing.T) {
	var r = &apistructs.ApplicationsResourcesResponse{
		Total: 0,
		List: []*apistructs.ApplicationsResourcesItem{
			{Name: "1", PodsCount: 20, CPURequest: 10, MemRequest: 10},
			{Name: "2", PodsCount: 20, CPURequest: 8, MemRequest: 12},
			{Name: "3", PodsCount: 30, CPURequest: 9, MemRequest: 12},
			{Name: "4", PodsCount: 20, CPURequest: 10, MemRequest: 9},
		},
	}
	r.OrderBy("-podsCount", "-cpuRequest", "-memRequest")
	for _, item := range r.List {
		t.Logf("name: %s, podsCount: %v, cpuRequest: %v, memRequest: %v",
			item.Name, item.PodsCount, item.CPURequest, item.MemRequest)
	}
	for i, name := range []string{"3", "1", "4", "2"} {
		if r.List[i].Name != name {
			t.Errorf("error")
		}
	}

	r.OrderBy("podsCount", "cpuRequest", "memRequest")
	for _, item := range r.List {
		t.Logf("name: %s, podsCount: %v, cpuRequest: %v, memRequest: %v",
			item.Name, item.PodsCount, item.CPURequest, item.MemRequest)
	}
	for i, name := range []string{"2", "4", "1", "3"} {
		if r.List[i].Name != name {
			t.Errorf("error")
		}
	}
}

func initApplicationsResourcesResponseList() (list []*apistructs.ApplicationsResourcesItem) {
	for i := 0; i < 50; i++ {
		list = append(list, &apistructs.ApplicationsResourcesItem{Name: strconv.FormatInt(int64(i+1), 10)})
	}
	return list
}

func extractNames(list []*apistructs.ApplicationsResourcesItem) (names []string) {
	for _, item := range list {
		names = append(names, item.Name)
	}
	return names
}

func TestApplicationsResourcesResponse_Paging(t *testing.T) {
	var r = new(apistructs.ApplicationsResourcesResponse)
	var (
		pageNo   uint64 = 1
		pageSize uint64 = 6
	)
	for pageNo = 1; pageNo <= 10; pageNo++ {
		r.List = initApplicationsResourcesResponseList()
		r.Paging(int64(pageSize), int64(pageNo))
		names := extractNames(r.List)
		t.Logf("pageSize: %v, pageNo: %v, names: %s", pageSize, pageNo, strings.Join(names, ","))
		if len(names) == int(pageSize) {
			if names[int(pageSize)-1] != strconv.FormatUint(pageNo*pageSize, 10) {
				t.Errorf("error")
			}
		}
	}
}

func TestApplicationsResourceQuery_Validate(t *testing.T) {
	var arr apistructs.ApplicationsResourcesRequest
	if err := arr.Validate(); err == nil {
		t.Fatal("the orgID should not be valid")
	}
	arr.OrgID = "0"
	if err := arr.Validate(); err == nil {
		t.Fatal("the userID should not be valid")
	}
	arr.UserID = "0"
	if err := arr.Validate(); err == nil {
		t.Fatal("the projectID should not be valid")
	}
	arr.ProjectID = "0"
	if err := arr.Validate(); err != nil {
		t.Fatal(err)
	}
}

func TestApplicationsResourceQuery(t *testing.T) {
	var arq apistructs.ApplicationsResourceQuery
	arq.AppsIDs = []string{"0", "1", "2", "3"}
	arq.OwnerIDs = []string{"0", "1", "2", "3"}
	arq.OrderBy = []string{"-createdAt", "updatedAt"}
	arq.GetAppIDs()
	arq.GetOwnerIDs()
	arq.GetPageNo()
	arq.GetPageSize()
	arq.PageNo = "10"
	arq.PageSize = "10"
	arq.GetPageNo()
	arq.GetPageSize()
}
