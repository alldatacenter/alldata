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
	"testing"

	"github.com/erda-project/erda/apistructs"
)

func TestGetQuotaOnClustersResponse_ReCalcu(t *testing.T) {
	var project = apistructs.ProjectQuotaOnClusters{
		ID:          0,
		Name:        "project-1",
		DisplayName: "project-2",
		CPUQuota:    0,
		MemQuota:    0,
	}
	var owner = apistructs.OwnerQuotaOnClusters{
		ID:       0,
		Name:     "erda",
		Nickname: "erda",
		CPUQuota: 0,
		MemQuota: 0,
		Projects: nil,
	}
	owner.Projects = []*apistructs.ProjectQuotaOnClusters{&project}
	var resp apistructs.GetQuotaOnClustersResponse
	resp.ClusterNames = []string{"erda-hongkong"}
	resp.Owners = []*apistructs.OwnerQuotaOnClusters{&owner}

	project.AccuQuota(1000, 1024*1024*1024)
	resp.ReCalcu()
	t.Logf("resp: %+v", resp)
	t.Logf("project: %+v", project)
	if resp.CPUQuota != 1 {
		t.Fatal("cpu quota error")
	}
	if resp.MemQuota != 1 {
		t.Fatal("mem quota error")
	}
}

func TestProjectNamespaces_PatchClusters(t *testing.T) {
	var (
		p apistructs.ProjectNamespaces
		q = apistructs.ProjectQuota{
			ProdClusterName:    "prod",
			StagingClusterName: "staging",
			TestClusterName:    "test",
			DevClusterName:     "dev",
		}
		filter = []string{"prod", "staging"}
	)
	p.PatchClusters(&q, filter)
	t.Logf("%+v", p.Clusters)
	if _, ok := p.Clusters["prod"]; !ok {
		t.Fatal("error")
	}
	if _, ok := p.Clusters["staging"]; !ok {
		t.Fatal("error")
	}
	if _, ok := p.Clusters["test"]; ok {
		t.Fatal("error")
	}
	if _, ok := p.Clusters["dev"]; ok {
		t.Fatal("error")
	}
}

func TestProjectNamespaces_PatchClustersNamespaces(t *testing.T) {
	var (
		p          apistructs.ProjectNamespaces
		namespaces = make(map[string][]string)
	)
	p.PatchClustersNamespaces(namespaces)

	p.Clusters = map[string][]string{"erda-hongkong": nil, "terminus-dev": nil}
	namespaces = map[string][]string{
		"erda-hongkong": {"default", "project-387-test"},
		"terminus-dev":  {"default", "project-387-dev"},
	}
	p.PatchClustersNamespaces(namespaces)

	var quota = &apistructs.ProjectQuota{
		ProdClusterName:    "erda-hongkong",
		StagingClusterName: "staging",
		TestClusterName:    "test",
		DevClusterName:     "terminus-dev",
		ProdCPUQuota:       0,
		ProdMemQuota:       1,
		StagingCPUQuota:    2,
		StagingMemQuota:    3,
		TestCPUQuota:       4,
		TestMemQuota:       5,
		DevCPUQuota:        6,
		DevMemQuota:        7,
	}
	p.PatchQuota(quota)
	if p.CPUQuota != 0+6 || p.MemQuota != 1+7 {
		t.Fatal("patch quota error")
	}
}
