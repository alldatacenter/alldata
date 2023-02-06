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

package deployment

import (
	"io/ioutil"
	"net/http"
	"reflect"
	"testing"
	"time"

	"bou.ke/monkey"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"

	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/internal/tools/orchestrator/events"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/servicegroup"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/addon"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/log"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

func TestConvertGroupLabels(t *testing.T) {
	app := apistructs.ApplicationDTO{
		OrgID:       1,
		OrgName:     "terminus",
		ProjectID:   2,
		ProjectName: "default",
		ID:          3,
		Name:        "pampas-blog",
		Workspaces: []apistructs.ApplicationWorkspace{
			{
				Workspace:       "DEV",
				ClusterName:     "terminus-dev",
				ConfigNamespace: "app-3-DEV",
			},
		},
	}
	runtime := dbclient.Runtime{
		BaseModel: dbengine.BaseModel{
			ID: 4,
		},
		Name:        "master",
		Workspace:   "DEV",
		ClusterName: "terminus-dev",
	}
	groupLabels := convertGroupLabels(&app, &runtime, 5)
	expect := map[string]string{
		"SERVICE_TYPE":              "STATELESS",
		"SERVICE_DISCOVERY_MODE":    "DEPEND", // default values are used for service discovery
		"DICE_ORG":                  "1",
		"DICE_ORG_ID":               "1",
		"DICE_ORG_NAME":             "terminus",
		"DICE_PROJECT":              "2",
		"DICE_PROJECT_ID":           "2",
		"DICE_PROJECT_NAME":         "default",
		"DICE_APPLICATION":          "3",
		"DICE_APPLICATION_ID":       "3",
		"DICE_APPLICATION_NAME":     "pampas-blog",
		"DICE_WORKSPACE":            "dev",
		"DICE_CLUSTER_NAME":         "terminus-dev",
		"DICE_RUNTIME":              "4",
		"DICE_RUNTIME_ID":           "4",
		"DICE_RUNTIME_NAME":         "master",
		"DICE_DEPLOYMENT":           "5",
		"DICE_DEPLOYMENT_ID":        "5",
		"DICE_APP_CONFIG_NAMESPACE": "app-3-DEV",
	}
	assert.Equal(t, expect, groupLabels)
}

func TestFSMTimeout(t *testing.T) {
	f := genFakeFSM()

	defer monkey.UnpatchAll()
	patchUpdateDeploymentStatusToRuntimeAndOrder(f)
	_ = recordUpdateDeployment()
	_ = recordEvent()
	_ = recordDLog()

	// do invoke
	f.Deployment.UpdatedAt = time.Now().Add(-61 * time.Minute)
	end, err := f.timeout()
	if assert.NoError(t, err) {
		assert.True(t, end)
	}

	f.Deployment.UpdatedAt = time.Now().Add(-59 * time.Minute)
	end, err = f.timeout()
	if assert.NoError(t, err) {
		assert.False(t, end)
	}
}

func TestFSMFailDeploy(t *testing.T) {
	f := genFakeFSM()

	defer monkey.UnpatchAll()
	updateC := recordUpdateDeployment()
	eventC := recordEvent()
	loggingC := recordDLog()

	patchUpdateDeploymentStatusToRuntimeAndOrder(f)

	// do invoke
	err := f.failDeploy(errors.Errorf("fake error"))

	if assert.NoError(t, err) {
		updates := collectUpdateDeployment(updateC)
		if assert.Len(t, updates, 1) {
			assert.Equal(t, "FAILED", string(updates[0].Status))
			assert.Equal(t, "INIT", string(updates[0].Phase))
		}

		es := collectEvent(eventC)
		if assert.Len(t, es, 1) {
			assert.Equal(t, "RuntimeDeployFailed", string(es[0].EventName))
			assert.Equal(t, "FAILED", string(es[0].Deployment.Status))
			assert.Equal(t, "INIT", string(es[0].Deployment.Phase))
			assert.Equal(t, uint64(101), es[0].Runtime.ID)
		}

		logging := collectDLog(loggingC)
		assert.Equal(t, []string{
			`deployment is fail, status: WAITING, phrase: INIT, (fake error)`,
		}, logging)
	}
}

func TestConvertErdaServiceTemplate(t *testing.T) {
	var err error
	var result string
	f := genFakeFSM()
	f.Cluster.Type = apistructs.EDAS

	var bdl *bundle.Bundle
	var db *dbclient.DBClient
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetAppsByProjectAndAppName",
		func(_ *bundle.Bundle, projectID, orgID uint64, userID string, appName string, header ...http.Header) (*apistructs.ApplicationListResponseData, error) {
			return &apistructs.ApplicationListResponseData{
				Total: 1,
				List: []apistructs.ApplicationDTO{{
					ID: 3,
				}},
			}, nil
		},
	)

	monkey.PatchInstanceMethod(reflect.TypeOf(db), "FindRuntimesByAppIdAndWorkspace",
		func(_ *dbclient.DBClient, appId uint64, workspace string) ([]dbclient.Runtime, error) {
			return []dbclient.Runtime{{
				ScheduleName: dbclient.ScheduleName{
					Namespace: "",
					Name:      "",
				},
			}}, nil
		},
	)
	f.bdl = bdl
	f.db = db
	_, err = f.convertErdaServiceTemplate("erdaService.aaa.bbb.ccc.bbb", "project-111-prod", 1, 2, "DEV")
	assert.Error(t, err)

	result, err = f.convertErdaServiceTemplate("erdaService.pampas-blog.bbb", "project-111-prod", 1, 2, "DEV")
	assert.Equal(t, "bbb.default.svc.cluster.local", result)

	f.Cluster.Type = apistructs.K8S
	result, err = f.convertErdaServiceTemplate("erdaService.pampas-blog.bbb", "project-111-prod", 1, 2, "DEV")
	assert.Equal(t, "bbb.project-111-prod.svc.cluster.local", result)
}

func TestFSMPushOnPhase(t *testing.T) {
	f := genFakeFSM()

	defer monkey.UnpatchAll()
	updateC := recordUpdateDeployment()
	eventC := recordEvent()

	// do invoke
	err := f.pushOnPhase(apistructs.DeploymentPhaseAddon)

	if assert.NoError(t, err) {
		updates := collectUpdateDeployment(updateC)
		if assert.Len(t, updates, 1) {
			assert.Equal(t, "WAITING", string(updates[0].Status)) // although this is not legal
			assert.Equal(t, "ADDON_REQUESTING", string(updates[0].Phase))
		}

		es := collectEvent(eventC)
		if assert.Len(t, es, 1) {
			assert.Equal(t, "RuntimeDeployStatusChanged", string(es[0].EventName))
		}
	}
}

func TestFSMContinueCanceling(t *testing.T) {
	f := genFakeFSM()
	f.Deployment.Phase = apistructs.DeploymentPhaseAddon
	f.Deployment.Status = apistructs.DeploymentStatusCanceling
	f.Runtime.Status = apistructs.RuntimeStatusHealthy
	f.scheduler = &scheduler.Scheduler{}

	patchUpdateDeploymentStatusToRuntimeAndOrder(f)

	updateC := recordUpdateDeployment()
	emitC := recordEvent()
	loggingC := recordDLog()

	monkey.PatchInstanceMethod(reflect.TypeOf(f.scheduler), "CancelServiceGroup",
		func(_ *scheduler.Scheduler, namespace, name string) (interface{}, error) {
			assert.Equal(t, "fake", namespace)
			assert.Equal(t, "schedule", name)
			return nil, nil
		},
	)

	// do invoke
	err := f.continueCanceling()
	if assert.NoError(t, err) {
		updates := collectUpdateDeployment(updateC)
		assert.Len(t, updates, 1)

		es := collectEvent(emitC)
		assert.Len(t, es, 1)

		logging := collectDLog(loggingC)
		assert.Equal(t, []string{`deployment canceled`}, logging)
	}
}

func recordUpdateDeployment() chan dbclient.Deployment {
	var db *dbclient.DBClient
	c := make(chan dbclient.Deployment, 1000)
	monkey.PatchInstanceMethod(reflect.TypeOf(db), "UpdateDeployment",
		func(_ *dbclient.DBClient, toUpdate *dbclient.Deployment) error {
			c <- *toUpdate
			return nil
		},
	)
	return c
}

func collectUpdateDeployment(c chan dbclient.Deployment) []dbclient.Deployment {
	close(c)
	updates := make([]dbclient.Deployment, 0)
	for u := range c {
		updates = append(updates, u)
	}
	return updates
}

func recordEvent() chan events.RuntimeEvent {
	var event *events.EventManager
	c := make(chan events.RuntimeEvent, 1000)
	monkey.PatchInstanceMethod(reflect.TypeOf(event), "EmitEvent",
		func(_ *events.EventManager, e *events.RuntimeEvent) {
			c <- *e
		},
	)
	return c
}

func collectEvent(c chan events.RuntimeEvent) []events.RuntimeEvent {
	defer close(c)
	es := make([]events.RuntimeEvent, 0)
	for {
		select {
		case e := <-c:
			es = append(es, e)
		default:
			return es
		}
	}
}

func recordDLog() chan string {
	var logger *log.DeployLogHelper
	c := make(chan string, 1000)
	monkey.PatchInstanceMethod(reflect.TypeOf(logger), "Log",
		func(_ *log.DeployLogHelper, content string, tags map[string]string) {
			c <- content
		},
	)
	return c
}

func collectDLog(c chan string) []string {
	defer close(c)
	lo := make([]string, 0)
	for {
		select {
		case l := <-c:
			lo = append(lo, l)
		default:
			return lo
		}
	}
}

func genFakeFSM(specPath ...string) *DeployFSMContext {
	fsm := DeployFSMContext{
		Deployment: &dbclient.Deployment{
			BaseModel: dbengine.BaseModel{
				ID: 100,
			},
			RuntimeId: 101,
			Status:    apistructs.DeploymentStatusWaiting,
			Phase:     apistructs.DeploymentPhaseInit,
			ReleaseId: "xxx-yyy",
			Operator:  "fake user",
		},
		Runtime: &dbclient.Runtime{
			BaseModel: dbengine.BaseModel{
				ID: 101,
			},
			Name:          "fake runtime",
			ApplicationID: 102,
			Workspace:     "DEV",
			ClusterName:   "terminus-test",
			ClusterId:     999,
			ScheduleName:  dbclient.ScheduleName{Namespace: "fake", Name: "schedule"},
			GitRepoAbbrev: "fake/runtime",
		},
		App: &apistructs.ApplicationDTO{
			ID:          102,
			Name:        "fake app",
			ProjectID:   103,
			ProjectName: "fake project",
			OrgID:       104,
			OrgName:     "fake org",
			Workspaces: []apistructs.ApplicationWorkspace{
				{
					Workspace:       "DEV",
					ClusterName:     "terminus-test",
					ConfigNamespace: "test-config-namespace",
				},
			},
		},
		Cluster: &clusterpb.ClusterInfo{
			Name:           "terminus-test",
			OrgID:          104,
			WildcardDomain: "test.terminus.io",
		},
		d: &log.DeployLogHelper{},
	}
	if len(specPath) > 0 {
		b, err := ioutil.ReadFile(specPath[0])
		if err != nil {
			panic(err)
		}
		y, err := diceyml.New(b, true)
		if err != nil {
			panic(err)
		}
		err = y.MergeEnv("DEV")
		if err != nil {
			panic(err)
		}
		fsm.Spec = y.Obj()
	}
	return &fsm
}

func patchUpdateDeploymentStatusToRuntimeAndOrder(f *DeployFSMContext) {
	monkey.PatchInstanceMethod(reflect.TypeOf(f), "UpdateDeploymentStatusToRuntimeAndOrder", func(*DeployFSMContext) error {
		return nil
	})
}

func TestUpdateServiceGroupWithLoop(t *testing.T) {
	var (
		bdl              *bundle.Bundle
		serviceGroupImpl *servicegroup.ServiceGroupImpl
	)
	monkey.PatchInstanceMethod(reflect.TypeOf(serviceGroupImpl), "Update", func(_ *servicegroup.ServiceGroupImpl, sg apistructs.ServiceGroupUpdateV2Request) (apistructs.ServiceGroup, error) {
		return apistructs.ServiceGroup{}, nil
	})

	defer monkey.UnpatchAll()

	fsm := DeployFSMContext{
		bdl:              bdl,
		serviceGroupImpl: serviceGroupImpl,
	}
	group := apistructs.ServiceGroupCreateV2Request{}
	if err := fsm.UpdateServiceGroupWithLoop(group); err != nil {
		t.Fatal(err)
	}
}

func Test_requestAddons(t *testing.T) {
	ad := &addon.Addon{}
	monkey.PatchInstanceMethod(reflect.TypeOf(ad), "BatchCreate", func(a *addon.Addon, req *apistructs.AddonCreateRequest) error {
		return nil
	})

	var (
		bdl *bundle.Bundle
	)
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "PushLog", func(*bundle.Bundle, *apistructs.LogPushRequest) error {
		return nil
	})
	defer monkey.UnpatchAll()

	fsm := DeployFSMContext{
		d:          &log.DeployLogHelper{Bdl: bdl},
		addon:      ad,
		App:        &apistructs.ApplicationDTO{},
		Deployment: &dbclient.Deployment{},
		Runtime:    &dbclient.Runtime{},
		Spec:       &diceyml.Object{AddOns: map[string]*diceyml.AddOn{"empty-addon": nil}},
	}
	err := fsm.requestAddons()
	assert.NoError(t, err)
}

func Test_genProjectNamespace(t *testing.T) {
	var (
		bdl *bundle.Bundle
	)
	fsm := DeployFSMContext{
		d:          &log.DeployLogHelper{Bdl: bdl},
		App:        &apistructs.ApplicationDTO{},
		Deployment: &dbclient.Deployment{},
		Runtime:    &dbclient.Runtime{},
		Spec:       &diceyml.Object{AddOns: map[string]*diceyml.AddOn{"empty-addon": nil}},
	}
	nsInfo := fsm.genProjectNamespace("111")
	assert.Equal(t, "project-111-prod", nsInfo["PROD"])

}
