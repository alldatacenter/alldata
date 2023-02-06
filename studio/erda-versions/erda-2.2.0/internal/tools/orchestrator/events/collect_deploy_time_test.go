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

package events

// func TestDeployTimeCollector_collectDeployTimes(t *testing.T) {
// 	db := initDb(t)
// 	c := NewDeployTimeCollector(nil, db)
//
// 	now := time.Now()
// 	deployment := &dbclient.Deployment{
// 		Status: apistructs.DeploymentStatusDeploying,
// 		Phase:  apistructs.DeploymentPhaseInit,
// 	}
// 	err := db.CreateDeployment(deployment)
// 	if !assert.NoError(t, err) {
// 		return
// 	}
// 	e := RuntimeEvent{
// 		EventName: RuntimeDeployStatusChanged,
// 		Deployment: &apistructs.Deployment{
// 			ID:     deployment.ID,
// 			Status: apistructs.DeploymentStatusDeploying,
// 			Phase:  apistructs.DeploymentPhaseInit,
// 		},
// 	}
// 	// init
// 	(*c).OnEvent(&e)
// 	deployment, err = db.GetDeployment(deployment.ID)
// 	if !assert.NoError(t, err) {
// 		return
// 	}
// 	assert.True(t, deployment.Extra.AddonPhaseStartAt == nil)
// 	assert.True(t, deployment.Extra.AddonPhaseEndAt == nil)
// 	assert.True(t, deployment.Extra.ServicePhaseStartAt == nil)
// 	assert.True(t, deployment.Extra.ServicePhaseEndAt == nil)
//
// 	// leap to addon
// 	deployment.Phase = apistructs.DeploymentPhaseAddon
// 	err = db.UpdateDeployment(deployment)
// 	if !assert.NoError(t, err) {
// 		return
// 	}
// 	e.Deployment.Phase = apistructs.DeploymentPhaseAddon
// 	(*c).OnEvent(&e)
// 	deployment, err = db.GetDeployment(deployment.ID)
// 	if !assert.NoError(t, err) {
// 		return
// 	}
// 	assert.True(t, deployment.Extra.AddonPhaseStartAt.After(now))
// 	assert.True(t, deployment.Extra.AddonPhaseEndAt == nil)
// 	assert.True(t, deployment.Extra.ServicePhaseStartAt == nil)
// 	assert.True(t, deployment.Extra.ServicePhaseEndAt == nil)
//
// 	// leap to service
// 	deployment.Phase = apistructs.DeploymentPhaseScript
// 	err = db.UpdateDeployment(deployment)
// 	if !assert.NoError(t, err) {
// 		return
// 	}
// 	e.Deployment.Phase = apistructs.DeploymentPhaseScript
// 	(*c).OnEvent(&e)
// 	deployment, err = db.GetDeployment(deployment.ID)
// 	if !assert.NoError(t, err) {
// 		return
// 	}
// 	assert.True(t, deployment.Extra.AddonPhaseStartAt.After(now))
// 	assert.True(t, deployment.Extra.AddonPhaseEndAt.After(now))
// 	assert.True(t, deployment.Extra.ServicePhaseStartAt.After(now))
// 	assert.True(t, deployment.Extra.ServicePhaseEndAt == nil)
//
// 	// all completed
// 	deployment.Phase = apistructs.DeploymentPhaseCompleted
// 	err = db.UpdateDeployment(deployment)
// 	if !assert.NoError(t, err) {
// 		return
// 	}
// 	e.Deployment.Phase = apistructs.DeploymentPhaseCompleted
// 	(*c).OnEvent(&e)
// 	deployment, err = db.GetDeployment(deployment.ID)
// 	if !assert.NoError(t, err) {
// 		return
// 	}
// 	assert.True(t, deployment.Extra.AddonPhaseStartAt.After(now))
// 	assert.True(t, deployment.Extra.AddonPhaseEndAt.After(now))
// 	assert.True(t, deployment.Extra.ServicePhaseStartAt.After(now))
// 	assert.True(t, deployment.Extra.ServicePhaseEndAt.After(now))
// }
//
// func initDb(t *testing.T) *dbclient.DBClient {
// 	os.Setenv("MYSQL_HOST", "127.0.0.1")
// 	os.Setenv("MYSQL_PORT", "3306")
// 	os.Setenv("MYSQL_DATABASE", "orchestrator")
// 	os.Setenv("MYSQL_USERNAME", "root")
// 	client, err := dbclient.Open()
// 	if assert.Nil(t, err) {
// 		return client
// 	} else {
// 		panic(err)
// 	}
// }
