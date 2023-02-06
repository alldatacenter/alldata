//  Copyright (c) 2021 Terminus, Inc.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package apistructs_test

import (
	"encoding/json"
	"testing"

	"github.com/erda-project/erda/apistructs"
	calcu "github.com/erda-project/erda/pkg/resourcecalculator"
)

const createProjectRequest = `{
    "template": "DevOps",
    "displayName": "add project test 9",
    "name": "add-project-test-9",
    "resourceConfig": {
        "DEV": {
            "clusterName": "terminus-dev",
            "cpuQuota": 1,
            "memQuota": 1
        },
        "TEST": {
            "clusterName": "terminus-dev",
            "cpuQuota": 1,
            "memQuota": 1
        },
        "STAGING": {
            "clusterName": "terminus-dev",
            "cpuQuota": 1,
            "memQuota": 1
        },
        "PROD": {
            "clusterName": "terminus-dev",
            "cpuQuota": 1,
            "memQuota": 1
        }
    },
    "orgId": 1
}`

func TestProjectCreateRequest(t *testing.T) {
	var project apistructs.ProjectCreateRequest
	if err := json.Unmarshal([]byte(createProjectRequest), &project); err != nil {
		t.Error(err)
	}
	t.Logf("project: %+v", project)

	quota := &apistructs.ProjectQuota{
		ProjectID:          0,
		ProjectName:        project.Name,
		ProdClusterName:    project.ResourceConfigs.PROD.ClusterName,
		StagingClusterName: project.ResourceConfigs.STAGING.ClusterName,
		TestClusterName:    project.ResourceConfigs.TEST.ClusterName,
		DevClusterName:     project.ResourceConfigs.DEV.ClusterName,
		ProdCPUQuota:       calcu.CoreToMillcore(project.ResourceConfigs.PROD.CPUQuota),
		ProdMemQuota:       calcu.GibibyteToByte(project.ResourceConfigs.PROD.MemQuota),
		StagingCPUQuota:    calcu.CoreToMillcore(project.ResourceConfigs.STAGING.CPUQuota),
		StagingMemQuota:    calcu.GibibyteToByte(project.ResourceConfigs.STAGING.MemQuota),
		TestCPUQuota:       calcu.CoreToMillcore(project.ResourceConfigs.TEST.CPUQuota),
		TestMemQuota:       calcu.GibibyteToByte(project.ResourceConfigs.TEST.MemQuota),
		DevCPUQuota:        calcu.CoreToMillcore(project.ResourceConfigs.DEV.CPUQuota),
		DevMemQuota:        calcu.GibibyteToByte(project.ResourceConfigs.DEV.MemQuota),
		CreatorID:          "0",
		UpdaterID:          "0",
	}
	data, _ := json.MarshalIndent(quota, "", "  ")
	t.Log(string(data))
}
