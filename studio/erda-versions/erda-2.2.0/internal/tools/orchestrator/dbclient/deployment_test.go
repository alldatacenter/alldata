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

package dbclient

// func TestDBClient_CreateDeployment(t *testing.T) {
// 	client := initDb(t)
//
// 	err := client.CreateDeployment(&Deployment{
// 		FailCause: "nothing",
// 		BuildId:   0,
// 		RuntimeId: 1,
// 		Status:    apistructs.DeploymentStatusCanceled,
// 		Phase:     apistructs.DeploymentPhaseAddon,
// 		Operator:  "55",
// 	})
// 	assert.Nil(t, err)
// }

// func TestDBClient_FindPreDeploymentOrCreate(t *testing.T) {
// 	client := initDb(t)
//
// 	pre, err := client.FindPreDeploymentOrCreate(spec.RuntimeUniqueId{ApplicationId: 1, Workspace: "DEV", Name: "test121"},
// 		&spec.LegacyDice{
// 			Name:      "123",
// 			GlobalEnv: map[string]string{},
// 			Addons: map[string]*spec.Addon{
// 				"123": {
// 					Name: "123",
// 				},
// 			},
// 			Services: map[string]*spec.Service{
// 				"service-nginx": {
// 					Environment: map[string]string{
// 						"HOST": "123",
// 					},
// 				},
// 			},
// 		})
// 	assert.NoError(t, err)
// 	fmt.Println(pre)
// }

// func TestDBClient_FindLastDeployment(t *testing.T) {
// 	client := initDb(t)
//
// 	deployment, err := client.FindLastDeployment(123)
// 	assert.NoError(t, err)
//
// 	fmt.Println(deployment)
// }

// func TestDeploymentExtra(t *testing.T) {
// 	client := initDb(t)
//
// 	init := Deployment{
// 		RuntimeId: 1,
// 	}
//
// 	var err error
//
// 	err = client.CreateDeployment(&init)
// 	assert.NoError(t, err)
//
// 	found, err := client.GetDeployment(init.ID)
// 	assert.NoError(t, err)
// 	assert.Equal(t, uint64(1), found.RuntimeId)
// 	assert.Equal(t, uint64(0), found.Extra.FakeHealthyCount)
//
// 	found.Extra.FakeHealthyCount = 1
// 	err = client.UpdateDeployment(found)
// 	assert.NoError(t, err)
//
// 	found2, err := client.GetDeployment(init.ID)
// 	assert.NoError(t, err)
// 	assert.Equal(t, uint64(1), found2.RuntimeId)
// 	assert.Equal(t, uint64(1), found2.Extra.FakeHealthyCount)
//
// 	found2.Extra.FakeHealthyCount = 0
// 	err = client.UpdateDeployment(found2)
// 	assert.NoError(t, err)
//
// 	newCreate := Deployment{
// 		RuntimeId: 2,
// 		Extra: DeploymentExtra{
// 			FakeHealthyCount: 2,
// 		},
// 	}
// 	err = client.UpdateDeployment(&newCreate)
// 	assert.NoError(t, err)
// }

// func TestResetPreDice(t *testing.T) {
// 	client := initDb(t)
//
// 	uniqueId := spec.RuntimeUniqueId{ApplicationId: 1, Workspace: "DEV", Name: "test121"}
//
// 	pre, err := client.FindPreDeploymentOrCreate(uniqueId,
// 		&spec.LegacyDice{
// 			Name:      "123",
// 			GlobalEnv: map[string]string{},
// 			Addons: map[string]*spec.Addon{
// 				"123": {
// 					Name: "123",
// 				},
// 			},
// 			Services: map[string]*spec.Service{
// 				"service-nginx": {
// 					Environment: map[string]string{
// 						"HOST": "123",
// 					},
// 				},
// 			},
// 		})
// 	assert.NoError(t, err)
//
// 	overlay := spec.LegacyDice{
// 		Name: "321",
// 		Services: map[string]*spec.Service{
// 			"service-xxx": {
// 				Environment: map[string]string{
// 					"HOST": "321",
// 				},
// 			},
// 		},
// 	}
// 	b, err := json.Marshal(&overlay)
// 	assert.NoError(t, err)
// 	pre.DiceOverlay = string(b)
// 	for i := 0; i < 2; i++ {
// 		err = client.UpdatePreDeployment(pre)
// 		assert.NoError(t, err)
// 	}
//
// 	found, err := client.FindPreDeployment(uniqueId)
// 	assert.NoError(t, err)
// 	assert.NotEmpty(t, found.DiceOverlay)
//
// 	err = client.ResetPreDice(uniqueId)
// 	assert.NoError(t, err)
//
// 	found2, err := client.FindPreDeployment(uniqueId)
// 	assert.NoError(t, err)
// 	assert.Empty(t, found2.DiceOverlay)
// }

// func TestDBClient_FindDeployments(t *testing.T) {
// 	client := initDb(t)
//
// 	list, total, err := client.FindDeployments(1, DeploymentFilter{}, 2, 1)
// 	if assert.NoError(t, err) {
// 		assert.Equal(t, 3, total)
// 		assert.Len(t, list, 1)
// 	}
// }
