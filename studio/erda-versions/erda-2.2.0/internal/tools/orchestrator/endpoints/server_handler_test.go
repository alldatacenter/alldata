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
	"errors"
	"fmt"
	"reflect"
	"strconv"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/runtime"
	"github.com/erda-project/erda/internal/tools/orchestrator/spec"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

func Test_GenOverlayDataForAudit(t *testing.T) {
	oldServiceData := &diceyml.Service{
		Resources: diceyml.Resources{
			CPU:  1,
			Mem:  1024,
			Disk: 0,
		},
		Deployments: diceyml.Deployments{
			Replicas: 1,
		},
	}

	auditData := genOverlayDataForAudit(oldServiceData)

	assert.Equal(t, float64(1), auditData.Resources.CPU)
	assert.Equal(t, 1024, auditData.Resources.Mem)
	assert.Equal(t, 0, auditData.Resources.Disk)
	assert.Equal(t, 1, auditData.Deployments.Replicas)
}

func TestEndpoints_getRuntimeScaleRecordByRuntimeIds(t *testing.T) {

	s := &Endpoints{
		db: &dbclient.DBClient{},
	}

	ids := []uint64{128, 129}

	monkey.PatchInstanceMethod(reflect.TypeOf(s.db), "FindRuntimesByIds", func(db *dbclient.DBClient, ids []uint64) ([]dbclient.Runtime, error) {
		runtimes := make([]dbclient.Runtime, 0)
		runtimes = append(runtimes, dbclient.Runtime{
			BaseModel: dbengine.BaseModel{
				ID: 128,
			},
			Name:          "feature/develop",
			ApplicationID: 1,
			Workspace:     "DEV",
			GitBranch:     "feature/develop",
			ProjectID:     1,
			Env:           "DEV",
			ClusterName:   "test",
			ClusterId:     1,
			Creator:       "2",
			ScheduleName: dbclient.ScheduleName{
				Namespace: "services",
				Name:      "302615dbf0",
			},
			Status:           "Healthy",
			LegacyStatus:     "INIT",
			Deployed:         true,
			Version:          "1",
			Source:           "IPELINE",
			DiceVersion:      "2",
			CPU:              0.10,
			Mem:              128.00,
			ReadableUniqueId: "dice-orchestrator",
			GitRepoAbbrev:    "xxx-test/test01",
			OrgID:            1,
		})
		runtimes = append(runtimes, dbclient.Runtime{
			BaseModel: dbengine.BaseModel{
				ID: 129,
			},
			Name:          "feature/develop",
			ApplicationID: 21,
			Workspace:     "DEV",
			GitBranch:     "feature/develop",
			ProjectID:     1,
			Env:           "DEV",
			ClusterName:   "test",
			ClusterId:     1,
			Creator:       "2",
			ScheduleName: dbclient.ScheduleName{
				Namespace: "services",
				Name:      "3dbfa5bf4c2",
			},
			Status:           "Healthy",
			LegacyStatus:     "INIT",
			Deployed:         true,
			Version:          "1",
			Source:           "IPELINE",
			DiceVersion:      "2",
			CPU:              0.10,
			Mem:              128.00,
			ReadableUniqueId: "dice-orchestrator",
			GitRepoAbbrev:    "xxx-test/test02",
			OrgID:            1,
		})

		return runtimes, nil
	})

	monkey.PatchInstanceMethod(reflect.TypeOf(s.db), "FindPreDeployment", func(db *dbclient.DBClient, uniqueId spec.RuntimeUniqueId) (*dbclient.PreDeployment, error) {

		uniqeKey := fmt.Sprintf("%s-%s-%s", strconv.Itoa(int(uniqueId.ApplicationId)), uniqueId.Name, uniqueId.Workspace)
		if uniqeKey == "21-feature/develop-DEV" {
			dice := &diceyml.Object{
				Version: "2.0",
				Services: diceyml.Services{
					"xxx": &diceyml.Service{
						Image:         "addon-registry.default.svc.cluster.local:5000/xxx-test/test02:test02-1641494278825921631",
						ImageUsername: "",
						ImagePassword: "",
						Cmd:           "",
						Ports: []diceyml.ServicePort{
							{
								Port:   8080,
								Expose: true,
							},
						},
						Resources: diceyml.Resources{
							CPU:     0.1,
							Mem:     128,
							Network: map[string]string{"mode": "caontainer"},
						},
						Deployments: diceyml.Deployments{
							Replicas: 1,
						},
					},
				},
				AddOns: diceyml.AddOns{
					"mysql": &diceyml.AddOn{
						Plan:    "mysql:basic",
						Options: map[string]string{"create_dbs": "testdb1,testdb2", "version": "5.7.29"},
					},
				},
			}

			b, _ := json.Marshal(dice)
			diceJson := string(b)

			dice_overlay := diceyml.Object{
				Services: diceyml.Services{
					"xxx": &diceyml.Service{
						Resources: diceyml.Resources{
							CPU: 0.1,
							Mem: 128,
						},
						Deployments: diceyml.Deployments{
							Replicas: 1,
						},
					},
				},
			}

			b, _ = json.Marshal(&dice_overlay)
			DiceOverlayJson := string(b)

			return &dbclient.PreDeployment{
				BaseModel: dbengine.BaseModel{
					ID: 23,
				},
				ApplicationId: 21,
				Workspace:     "DEV",
				RuntimeName:   "feature/develop",
				Dice:          diceJson,
				DiceOverlay:   DiceOverlayJson,
				DiceType:      1,
			}, nil
		} else {
			dice := &diceyml.Object{
				Version: "2.0",
				Services: diceyml.Services{
					"go-demo": &diceyml.Service{
						Image:         "addon-registry.default.svc.cluster.local:5000/xxx-test/test01:go-demo-1641494267330770612",
						ImageUsername: "",
						ImagePassword: "",
						Cmd:           "",
						Ports: []diceyml.ServicePort{
							{
								Port:   8080,
								Expose: true,
							},
						},
						Resources: diceyml.Resources{
							CPU:     0.1,
							Mem:     128,
							Network: map[string]string{"mode": "caontainer"},
						},
						Deployments: diceyml.Deployments{
							Replicas: 1,
						},
					},
				},
				AddOns: diceyml.AddOns{
					"kafka": &diceyml.AddOn{
						Plan:    "kafka:basic",
						Options: map[string]string{"version": "2.0.0"},
					},
				},
			}

			b, _ := json.Marshal(dice)
			diceJson := string(b)

			dice_overlay := diceyml.Object{
				Services: diceyml.Services{
					"go-demo": &diceyml.Service{
						Resources: diceyml.Resources{
							CPU: 0.1,
							Mem: 128,
						},
						Deployments: diceyml.Deployments{
							Replicas: 1,
						},
					},
				},
			}

			b, _ = json.Marshal(&dice_overlay)
			DiceOverlayJson := string(b)

			return &dbclient.PreDeployment{
				BaseModel: dbengine.BaseModel{
					ID: 20,
				},
				ApplicationId: 1,
				Workspace:     "DEV",
				RuntimeName:   "feature/develop",
				Dice:          diceJson,
				DiceOverlay:   DiceOverlayJson,
				DiceType:      1,
			}, nil
		}
	})

	_, _, err := s.getRuntimeScaleRecordByRuntimeIds(ids)

	assert.Equal(t, err, nil)
}

func TestEndpoints_batchRuntimeReDeploy(t *testing.T) {

	s := &Endpoints{
		db:      &dbclient.DBClient{},
		runtime: &runtime.Runtime{},
	}

	userID := user.ID("2")

	runtime1 := dbclient.Runtime{
		BaseModel: dbengine.BaseModel{
			ID: 129,
		},
		Name:          "feature/develop",
		ApplicationID: 21,
		Workspace:     "DEV",
		GitBranch:     "feature/develop",
		ProjectID:     1,
		Env:           "DEV",
		ClusterName:   "test",
		ClusterId:     1,
		Creator:       "2",
		ScheduleName: dbclient.ScheduleName{
			Namespace: "services",
			Name:      "3dbfa5bf4c2",
		},
		Status:           "Healthy",
		LegacyStatus:     "INIT",
		Deployed:         true,
		Version:          "1",
		Source:           "IPELINE",
		DiceVersion:      "2",
		CPU:              0.10,
		Mem:              128.00,
		ReadableUniqueId: "dice-orchestrator",
		GitRepoAbbrev:    "xxx-test/test02",
		OrgID:            1,
	}

	runtime2 := dbclient.Runtime{
		BaseModel: dbengine.BaseModel{
			ID: 128,
		},
		Name:          "feature/develop",
		ApplicationID: 1,
		Workspace:     "DEV",
		GitBranch:     "feature/develop",
		ProjectID:     1,
		Env:           "DEV",
		ClusterName:   "test",
		ClusterId:     1,
		Creator:       "2",
		ScheduleName: dbclient.ScheduleName{
			Namespace: "services",
			Name:      "302615dbf0",
		},
		Status:           "Healthy",
		LegacyStatus:     "INIT",
		Deployed:         true,
		Version:          "1",
		Source:           "IPELINE",
		DiceVersion:      "2",
		CPU:              0.10,
		Mem:              128.00,
		ReadableUniqueId: "dice-orchestrator",
		GitRepoAbbrev:    "xxx-test/test01",
		OrgID:            1,
	}

	runtime3 := dbclient.Runtime{
		BaseModel: dbengine.BaseModel{
			ID: 130,
		},
		Name:          "feature/develop",
		ApplicationID: 22,
		Workspace:     "DEV",
		GitBranch:     "feature/develop",
		ProjectID:     1,
		Env:           "DEV",
		ClusterName:   "test",
		ClusterId:     1,
		Creator:       "2",
		ScheduleName: dbclient.ScheduleName{
			Namespace: "services",
			Name:      "302615dbf1",
		},
		Status:           "Healthy",
		LegacyStatus:     "INIT",
		Deployed:         true,
		Version:          "1",
		Source:           "IPELINE",
		DiceVersion:      "2",
		CPU:              0.10,
		Mem:              128.00,
		ReadableUniqueId: "dice-orchestrator",
		GitRepoAbbrev:    "xxx-test/test03",
		OrgID:            1,
	}
	runtimes := []dbclient.Runtime{runtime1, runtime2}
	runtimeScaleRecords1 := apistructs.RuntimeScaleRecords{
		IDs: []uint64{128, 129},
	}

	rsr1 := apistructs.RuntimeScaleRecord{
		ApplicationId: 1,
		Workspace:     "DEV",
		Name:          "feature/develop",
		PayLoad: apistructs.PreDiceDTO{
			Services: make(map[string]*apistructs.RuntimeInspectServiceDTO),
		},
	}
	rsr1.PayLoad.Services["go-demo"] = &apistructs.RuntimeInspectServiceDTO{
		Deployments: apistructs.RuntimeServiceDeploymentsDTO{
			Replicas: 1,
		},
		Resources: apistructs.RuntimeServiceResourceDTO{
			CPU: 0.1,
			Mem: 128,
		},
	}

	rsr2 := apistructs.RuntimeScaleRecord{
		ApplicationId: 21,
		Workspace:     "DEV",
		Name:          "feature/develop",
		PayLoad: apistructs.PreDiceDTO{
			Services: make(map[string]*apistructs.RuntimeInspectServiceDTO),
		},
	}
	rsr1.PayLoad.Services["go-demo"] = &apistructs.RuntimeInspectServiceDTO{
		Deployments: apistructs.RuntimeServiceDeploymentsDTO{
			Replicas: 1,
		},
		Resources: apistructs.RuntimeServiceResourceDTO{
			CPU: 0.1,
			Mem: 128,
		},
	}

	rsr3 := apistructs.RuntimeScaleRecord{
		ApplicationId: 22,
		Workspace:     "DEV",
		Name:          "feature/develop",
		PayLoad: apistructs.PreDiceDTO{
			Services: make(map[string]*apistructs.RuntimeInspectServiceDTO),
		},
	}
	rsr3.PayLoad.Services["xxxyyyy"] = &apistructs.RuntimeInspectServiceDTO{
		Deployments: apistructs.RuntimeServiceDeploymentsDTO{
			Replicas: 1,
		},
		Resources: apistructs.RuntimeServiceResourceDTO{
			CPU: 0.1,
			Mem: 128,
		},
	}

	runtimeScaleRecords2 := apistructs.RuntimeScaleRecords{
		Runtimes: []apistructs.RuntimeScaleRecord{rsr1, rsr2},
	}

	runtimeScaleRecords3 := apistructs.RuntimeScaleRecords{
		Runtimes: []apistructs.RuntimeScaleRecord{rsr3},
		IDs:      []uint64{130},
	}

	monkey.PatchInstanceMethod(reflect.TypeOf(s.db), "FindRuntime", func(db *dbclient.DBClient, uniqueId spec.RuntimeUniqueId) (*dbclient.Runtime, error) {
		uniqeKey := fmt.Sprintf("%s-%s-%s", strconv.Itoa(int(uniqueId.ApplicationId)), uniqueId.Name, uniqueId.Workspace)
		if uniqeKey == "21-feature/develop-DEV" {
			runtime := &dbclient.Runtime{
				BaseModel: dbengine.BaseModel{
					ID: 129,
				},
				Name:          "feature/develop",
				ApplicationID: 21,
				Workspace:     "DEV",
				GitBranch:     "feature/develop",
				ProjectID:     1,
				Env:           "DEV",
				ClusterName:   "test",
				ClusterId:     1,
				Creator:       "2",
				ScheduleName: dbclient.ScheduleName{
					Namespace: "services",
					Name:      "3dbfa5bf4c2",
				},
				Status:           "Healthy",
				LegacyStatus:     "INIT",
				Deployed:         true,
				Version:          "1",
				Source:           "IPELINE",
				DiceVersion:      "2",
				CPU:              0.10,
				Mem:              128.00,
				ReadableUniqueId: "dice-orchestrator",
				GitRepoAbbrev:    "xxx-test/test02",
				OrgID:            1,
			}
			return runtime, nil
		}
		if uniqeKey == "1-feature/develop-DEV" {
			runtime := &dbclient.Runtime{
				BaseModel: dbengine.BaseModel{
					ID: 128,
				},
				Name:          "feature/develop",
				ApplicationID: 1,
				Workspace:     "DEV",
				GitBranch:     "feature/develop",
				ProjectID:     1,
				Env:           "DEV",
				ClusterName:   "test",
				ClusterId:     1,
				Creator:       "2",
				ScheduleName: dbclient.ScheduleName{
					Namespace: "services",
					Name:      "302615dbf0",
				},
				Status:           "Healthy",
				LegacyStatus:     "INIT",
				Deployed:         true,
				Version:          "1",
				Source:           "IPELINE",
				DiceVersion:      "2",
				CPU:              0.10,
				Mem:              128.00,
				ReadableUniqueId: "dice-orchestrator",
				GitRepoAbbrev:    "xxx-test/test01",
				OrgID:            1,
			}
			return runtime, nil
		} else {
			runtime := &dbclient.Runtime{
				BaseModel: dbengine.BaseModel{
					ID: 130,
				},
				Name:          "feature/develop",
				ApplicationID: 22,
				Workspace:     "DEV",
				GitBranch:     "feature/develop",
				ProjectID:     1,
				Env:           "DEV",
				ClusterName:   "test",
				ClusterId:     1,
				Creator:       "2",
				ScheduleName: dbclient.ScheduleName{
					Namespace: "services",
					Name:      "302615dbf1",
				},
				Status:           "Healthy",
				LegacyStatus:     "INIT",
				Deployed:         true,
				Version:          "1",
				Source:           "IPELINE",
				DiceVersion:      "2",
				CPU:              0.10,
				Mem:              128.00,
				ReadableUniqueId: "dice-orchestrator",
				GitRepoAbbrev:    "xxx-test/test03",
				OrgID:            1,
			}
			return runtime, nil
		}
	})

	monkey.PatchInstanceMethod(reflect.TypeOf(s.runtime), "RedeployPipeline", func(rt *runtime.Runtime, ctx context.Context, operator user.ID, orgID uint64, runtimeID uint64) (*apistructs.RuntimeDeployDTO, error) {
		if runtimeID == 128 {
			ret := &apistructs.RuntimeDeployDTO{
				PipelineID:      10000260,
				ApplicationID:   1,
				ApplicationName: "test01",
				ProjectID:       1,
				ProjectName:     "test",
				OrgID:           1,
				OrgName:         "xxx",
				ServicesNames:   []string{"go-demo"},
			}
			return ret, nil
		}
		if runtimeID == 129 {
			ret := &apistructs.RuntimeDeployDTO{
				PipelineID:      10000259,
				ApplicationID:   21,
				ApplicationName: "test02",
				ProjectID:       1,
				ProjectName:     "test",
				OrgID:           1,
				OrgName:         "xxx",
				ServicesNames:   []string{"xxx"},
			}
			return ret, nil
		} else {
			ret := &apistructs.RuntimeDeployDTO{
				ApplicationID:   22,
				ApplicationName: "test03",
				ProjectID:       1,
				ProjectName:     "test",
				OrgID:           1,
				OrgName:         "xxx",
				ServicesNames:   []string{"xxxyyy"},
			}
			return ret, errors.New("failed")
		}
	})

	want1 := apistructs.BatchRuntimeReDeployResults{
		Total:   0,
		Success: 2,
		Failed:  0,
		ReDeployed: []apistructs.RuntimeDeployDTO{
			{
				PipelineID:      10000260,
				ApplicationID:   1,
				ApplicationName: "test01",
				ProjectID:       1,
				ProjectName:     "test",
				OrgID:           1,
				OrgName:         "xxx",
				ServicesNames:   []string{"go-demo"},
			},
			{
				PipelineID:      10000259,
				ApplicationID:   21,
				ApplicationName: "test02",
				ProjectID:       1,
				ProjectName:     "test",
				OrgID:           1,
				OrgName:         "xxx",
				ServicesNames:   []string{"xxx"},
			},
		},
		ReDeployedIds:   []uint64{128, 129},
		UnReDeployed:    []apistructs.RuntimeDTO{},
		UnReDeployedIds: []uint64{},
		ErrMsg:          []string{},
	}
	want2 := want1
	want2.Total = 2

	want3 := apistructs.BatchRuntimeReDeployResults{
		Total:         0,
		Success:       0,
		Failed:        1,
		ReDeployed:    []apistructs.RuntimeDeployDTO{},
		ReDeployedIds: []uint64{},
		UnReDeployed: []apistructs.RuntimeDTO{
			{
				ID:              130,
				Name:            "xxxyyy",
				GitBranch:       "feature/develop",
				Workspace:       "DEV",
				ClusterName:     "test",
				ClusterId:       1,
				Status:          "",
				ApplicationID:   22,
				ApplicationName: "xxxyyy",
				ProjectID:       1,
				ProjectName:     "test",
				OrgID:           1,
			},
		},
		UnReDeployedIds: []uint64{130},
		ErrMsg:          []string{"failed"},
	}
	ctx := context.Background()
	got := s.batchRuntimeReDeploy(ctx, userID, runtimes, runtimeScaleRecords1)
	if len(got.ReDeployedIds) != len(want1.ReDeployedIds) {
		t.Errorf("batchRuntimeReDeploy() = %v, want %v", got, want1)
	}

	gotMap := make(map[uint64]bool)
	for _, id := range got.ReDeployedIds {
		gotMap[id] = true
	}
	for _, id := range want1.ReDeployedIds {
		if _, ok := gotMap[id]; !ok {
			t.Errorf("batchRuntimeReDeploy() = %v, want %v", got, want1)
		}
	}

	var rts []dbclient.Runtime
	got = s.batchRuntimeReDeploy(ctx, userID, rts, runtimeScaleRecords2)
	if len(got.ReDeployedIds) != len(want2.ReDeployedIds) {
		t.Errorf("batchRuntimeReDeploy() = %v, want %v", got, want2)
	}
	gotMap1 := make(map[uint64]bool)
	for _, id := range got.ReDeployedIds {
		gotMap1[id] = true
	}
	for _, id := range want2.ReDeployedIds {
		if _, ok := gotMap1[id]; !ok {
			t.Errorf("batchRuntimeReDeploy() = %v, want %v", got, want2)
		}
	}

	runtimes3 := []dbclient.Runtime{runtime3}
	got = s.batchRuntimeReDeploy(ctx, userID, runtimes3, runtimeScaleRecords3)
	if len(got.UnReDeployedIds) != len(want3.UnReDeployedIds) {
		t.Errorf("batchRuntimeReDeploy() = %v, want %v", got, want3)
	}

	gotMap = make(map[uint64]bool)
	for _, id := range got.UnReDeployedIds {
		gotMap[id] = true
	}
	for _, id := range want3.UnReDeployedIds {
		if _, ok := gotMap[id]; !ok {
			t.Errorf("batchRuntimeReDeploy() = %v, want %v", got, want3)
		}
	}

}

func TestEndpoints_batchRuntimeDelete(t *testing.T) {

	s := &Endpoints{
		db:      &dbclient.DBClient{},
		runtime: &runtime.Runtime{},
	}

	userID := user.ID("2")

	runtime1 := dbclient.Runtime{
		BaseModel: dbengine.BaseModel{
			ID: 129,
		},
		Name:          "feature/develop",
		ApplicationID: 21,
		Workspace:     "DEV",
		GitBranch:     "feature/develop",
		ProjectID:     1,
		Env:           "DEV",
		ClusterName:   "test",
		ClusterId:     1,
		Creator:       "2",
		ScheduleName: dbclient.ScheduleName{
			Namespace: "services",
			Name:      "3dbfa5bf4c2",
		},
		Status:           "Healthy",
		LegacyStatus:     "INIT",
		Deployed:         true,
		Version:          "1",
		Source:           "IPELINE",
		DiceVersion:      "2",
		CPU:              0.10,
		Mem:              128.00,
		ReadableUniqueId: "dice-orchestrator",
		GitRepoAbbrev:    "xxx-test/test02",
		OrgID:            1,
	}

	runtime2 := dbclient.Runtime{
		BaseModel: dbengine.BaseModel{
			ID: 128,
		},
		Name:          "feature/develop",
		ApplicationID: 1,
		Workspace:     "DEV",
		GitBranch:     "feature/develop",
		ProjectID:     1,
		Env:           "DEV",
		ClusterName:   "test",
		ClusterId:     1,
		Creator:       "2",
		ScheduleName: dbclient.ScheduleName{
			Namespace: "services",
			Name:      "302615dbf0",
		},
		Status:           "Healthy",
		LegacyStatus:     "INIT",
		Deployed:         true,
		Version:          "1",
		Source:           "IPELINE",
		DiceVersion:      "2",
		CPU:              0.10,
		Mem:              128.00,
		ReadableUniqueId: "dice-orchestrator",
		GitRepoAbbrev:    "xxx-test/test01",
		OrgID:            1,
	}

	runtimes := []dbclient.Runtime{runtime1, runtime2}
	runtimeScaleRecords1 := apistructs.RuntimeScaleRecords{
		IDs: []uint64{128, 129},
	}

	rsr1 := apistructs.RuntimeScaleRecord{
		ApplicationId: 1,
		Workspace:     "DEV",
		Name:          "feature/develop",
		PayLoad: apistructs.PreDiceDTO{
			Services: make(map[string]*apistructs.RuntimeInspectServiceDTO),
		},
	}
	rsr1.PayLoad.Services["go-demo"] = &apistructs.RuntimeInspectServiceDTO{
		Deployments: apistructs.RuntimeServiceDeploymentsDTO{
			Replicas: 1,
		},
		Resources: apistructs.RuntimeServiceResourceDTO{
			CPU: 0.1,
			Mem: 128,
		},
	}

	rsr2 := apistructs.RuntimeScaleRecord{
		ApplicationId: 21,
		Workspace:     "DEV",
		Name:          "feature/develop",
		PayLoad: apistructs.PreDiceDTO{
			Services: make(map[string]*apistructs.RuntimeInspectServiceDTO),
		},
	}
	rsr1.PayLoad.Services["go-demo"] = &apistructs.RuntimeInspectServiceDTO{
		Deployments: apistructs.RuntimeServiceDeploymentsDTO{
			Replicas: 1,
		},
		Resources: apistructs.RuntimeServiceResourceDTO{
			CPU: 0.1,
			Mem: 128,
		},
	}

	runtimeScaleRecords2 := apistructs.RuntimeScaleRecords{
		Runtimes: []apistructs.RuntimeScaleRecord{rsr1, rsr2},
	}

	monkey.PatchInstanceMethod(reflect.TypeOf(s.runtime), "Delete", func(rt *runtime.Runtime, operator user.ID, orgID uint64, runtimeID uint64) (*apistructs.RuntimeDTO, error) {
		if runtimeID == 128 {
			ret := &apistructs.RuntimeDTO{
				ID:              128,
				Name:            "feature/develop",
				GitBranch:       "feature/develop",
				Workspace:       "DEV",
				ClusterName:     "test",
				ClusterId:       1,
				Status:          "Healthy",
				ApplicationID:   1,
				ApplicationName: "test01",
				ProjectID:       1,
				ProjectName:     "test",
				OrgID:           1,
				Errors:          []apistructs.ErrorResponse{},
			}
			return ret, nil
		} else {
			ret := &apistructs.RuntimeDTO{
				ID:              129,
				Name:            "feature/develop",
				GitBranch:       "feature/develop",
				Workspace:       "DEV",
				ClusterName:     "test",
				ClusterId:       1,
				Status:          "Healthy",
				ApplicationID:   21,
				ApplicationName: "test02",
				ProjectID:       1,
				ProjectName:     "test",
				OrgID:           1,
				Errors:          []apistructs.ErrorResponse{},
			}
			return ret, nil
		}
	})

	monkey.PatchInstanceMethod(reflect.TypeOf(s.db), "FindRuntime", func(db *dbclient.DBClient, uniqueId spec.RuntimeUniqueId) (*dbclient.Runtime, error) {
		uniqeKey := fmt.Sprintf("%s-%s-%s", strconv.Itoa(int(uniqueId.ApplicationId)), uniqueId.Name, uniqueId.Workspace)
		if uniqeKey == "21-feature/develop-DEV" {
			runtime := &dbclient.Runtime{
				BaseModel: dbengine.BaseModel{
					ID: 129,
				},
				Name:          "feature/develop",
				ApplicationID: 21,
				Workspace:     "DEV",
				GitBranch:     "feature/develop",
				ProjectID:     1,
				Env:           "DEV",
				ClusterName:   "test",
				ClusterId:     1,
				Creator:       "2",
				ScheduleName: dbclient.ScheduleName{
					Namespace: "services",
					Name:      "3dbfa5bf4c2",
				},
				Status:           "Healthy",
				LegacyStatus:     "INIT",
				Deployed:         true,
				Version:          "1",
				Source:           "IPELINE",
				DiceVersion:      "2",
				CPU:              0.10,
				Mem:              128.00,
				ReadableUniqueId: "dice-orchestrator",
				GitRepoAbbrev:    "xxx-test/test02",
				OrgID:            1,
			}
			return runtime, nil
		} else {
			runtime := &dbclient.Runtime{
				BaseModel: dbengine.BaseModel{
					ID: 128,
				},
				Name:          "feature/develop",
				ApplicationID: 1,
				Workspace:     "DEV",
				GitBranch:     "feature/develop",
				ProjectID:     1,
				Env:           "DEV",
				ClusterName:   "test",
				ClusterId:     1,
				Creator:       "2",
				ScheduleName: dbclient.ScheduleName{
					Namespace: "services",
					Name:      "302615dbf0",
				},
				Status:           "Healthy",
				LegacyStatus:     "INIT",
				Deployed:         true,
				Version:          "1",
				Source:           "IPELINE",
				DiceVersion:      "2",
				CPU:              0.10,
				Mem:              128.00,
				ReadableUniqueId: "dice-orchestrator",
				GitRepoAbbrev:    "xxx-test/test01",
				OrgID:            1,
			}
			return runtime, nil
		}
	})

	want1 := apistructs.BatchRuntimeDeleteResults{
		Total:   0,
		Success: 2,
		Failed:  0,
		Deleted: []apistructs.RuntimeDTO{
			{
				ID:              128,
				Name:            "",
				GitBranch:       "feature/develop",
				Workspace:       "DEV",
				ClusterName:     "test",
				ClusterId:       1,
				Status:          "",
				ApplicationID:   1,
				ApplicationName: "test01",
				ProjectID:       1,
				ProjectName:     "test",
				OrgID:           1,
			},
			{
				ID:              129,
				Name:            "",
				GitBranch:       "feature/develop",
				Workspace:       "DEV",
				ClusterName:     "test",
				ClusterId:       1,
				Status:          "",
				ApplicationID:   1,
				ApplicationName: "test02",
				ProjectID:       1,
				ProjectName:     "test",
				OrgID:           1,
			},
		},
		DeletedIds:   []uint64{128, 129},
		UnDeleted:    []apistructs.RuntimeDTO{},
		UnDeletedIds: []uint64{},
		ErrMsg:       []string{},
	}
	want2 := want1
	want2.Total = 2

	got := s.batchRuntimeDelete(userID, runtimes, runtimeScaleRecords1)
	if len(got.DeletedIds) != len(want1.DeletedIds) {
		t.Errorf("batchRuntimeReDeploy() = %v, want %v", got, want1)
	}

	gotMap := make(map[uint64]bool)
	for _, id := range got.DeletedIds {
		gotMap[id] = true
	}
	for _, id := range want1.DeletedIds {
		if _, ok := gotMap[id]; !ok {
			t.Errorf("batchRuntimeReDeploy() = %v, want %v", got, want1)
		}
	}

	var rts []dbclient.Runtime
	got = s.batchRuntimeDelete(userID, rts, runtimeScaleRecords2)
	if len(got.DeletedIds) != len(want2.DeletedIds) {
		t.Errorf("batchRuntimeReDeploy() = %v, want %v", got, want2)
	}
	gotMap1 := make(map[uint64]bool)
	for _, id := range got.DeletedIds {
		gotMap1[id] = true
	}
	for _, id := range want2.DeletedIds {
		if _, ok := gotMap1[id]; !ok {
			t.Errorf("batchRuntimeReDeploy() = %v, want %v", got, want2)
		}
	}

}
