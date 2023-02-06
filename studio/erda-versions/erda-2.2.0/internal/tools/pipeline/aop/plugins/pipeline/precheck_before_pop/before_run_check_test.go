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

package precheck_before_pop

import (
	"bytes"
	"encoding/json"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/lifecycle_hook_client"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

func Test_matchHookType(t *testing.T) {
	var table = []struct {
		lifecycle []*pipelineyml.NetworkHookInfo
		matchLen  int
	}{
		{
			lifecycle: []*pipelineyml.NetworkHookInfo{
				{
					Hook: HookType,
				},
				{
					Hook: "after-run-check",
				},
			},
			matchLen: 1,
		},

		{
			lifecycle: []*pipelineyml.NetworkHookInfo{
				{
					Hook: "pre-run-check",
				},
				{
					Hook: "after-run-check",
				},
			},
			matchLen: 0,
		},

		{
			lifecycle: []*pipelineyml.NetworkHookInfo{
				{
					Hook: HookType,
				},
				{
					Hook: "after-create-check",
				},
				{
					Hook: HookType,
				},
			},
			matchLen: 2,
		},
	}
	var httpBeforeCheckRun HttpBeforeCheckRun
	for _, data := range table {
		result := httpBeforeCheckRun.matchHookType(data.lifecycle)
		assert.Len(t, result, data.matchLen)
	}
}

func TestCheckRun(t *testing.T) {
	var table = []struct {
		CheckResult           CheckRunResult
		haveError             bool
		matchOtherLabel       string
		httpBeforeCheckRun    HttpBeforeCheckRun
		mockPipelineWithTasks *spec.PipelineWithTasks
	}{
		{
			CheckResult: CheckRunResult{
				CheckResult: CheckResultSuccess,
			},
			matchOtherLabel: "otherLabel",
			haveError:       false,
			httpBeforeCheckRun: HttpBeforeCheckRun{
				PipelineID:      1000,
				LifeCycleClient: &lifecycle_hook_client.LifeCycleService{},
			},
			mockPipelineWithTasks: &spec.PipelineWithTasks{
				Pipeline: &spec.Pipeline{
					PipelineBase: spec.PipelineBase{
						ID:              1000,
						PipelineSource:  "FDP",
						PipelineYmlName: "230",
					},
					PipelineExtra: spec.PipelineExtra{
						PipelineYml: `
version: 1.1
lifecycle:
  - hook: "` + HookType + `"
    labels: 
       "otherLabel": 123
`,
					},
				},
				Tasks: []*spec.PipelineTask{},
			},
		},
		{
			CheckResult: CheckRunResult{
				CheckResult: CheckResultSuccess,
			},
			haveError:       false,
			matchOtherLabel: "otherLabel2",
			httpBeforeCheckRun: HttpBeforeCheckRun{
				PipelineID: 1000,
			},
			mockPipelineWithTasks: &spec.PipelineWithTasks{
				Pipeline: &spec.Pipeline{
					PipelineBase: spec.PipelineBase{
						ID:              1000,
						PipelineSource:  "FDP",
						PipelineYmlName: "230",
					},
					PipelineExtra: spec.PipelineExtra{
						PipelineYml: `
version: 1.1
lifecycle:
  - hook: "` + HookType + `"
    labels: 
       "otherLabel2": 123
  - hook: "` + HookType + `"
    labels: 
       "otherLabel2": 123
`,
					},
				},
				Tasks: []*spec.PipelineTask{},
			},
		},
		{
			CheckResult: CheckRunResult{
				CheckResult: CheckResultSuccess,
			},
			haveError:       false,
			matchOtherLabel: "",
			httpBeforeCheckRun: HttpBeforeCheckRun{
				PipelineID:      10001,
				LifeCycleClient: &lifecycle_hook_client.LifeCycleService{},
			},
			mockPipelineWithTasks: &spec.PipelineWithTasks{
				Pipeline: &spec.Pipeline{
					PipelineBase: spec.PipelineBase{
						ID:              10001,
						PipelineSource:  "FDP",
						PipelineYmlName: "230",
					},
					PipelineExtra: spec.PipelineExtra{
						PipelineYml: `
version: 1.1
lifecycle:
  - hook: "` + HookType + `1"
    labels: 
       "otherLabel1": 123
`,
					},
				},
				Tasks: []*spec.PipelineTask{},
			},
		},

		{
			CheckResult: CheckRunResult{
				CheckResult: CheckResultFailed,
				RetryOption: RetryOption{
					IntervalSecond: 203,
				},
			},
			haveError:       false,
			matchOtherLabel: "otherLabel1",
			httpBeforeCheckRun: HttpBeforeCheckRun{
				PipelineID: 10001,
			},
			mockPipelineWithTasks: &spec.PipelineWithTasks{
				Pipeline: &spec.Pipeline{
					PipelineBase: spec.PipelineBase{
						ID:              10001,
						PipelineSource:  "FDP",
						PipelineYmlName: "230",
					},
					PipelineExtra: spec.PipelineExtra{
						PipelineYml: `
version: 1.1
lifecycle:
  - hook: "` + HookType + `"
    labels: 
       "otherLabel1": 123
`,
					},
				},
				Tasks: []*spec.PipelineTask{},
			},
		},

		{
			CheckResult: CheckRunResult{
				CheckResult: CheckResultFailed,
				RetryOption: RetryOption{
					IntervalSecond: 203,
				},
			},
			haveError:       true,
			matchOtherLabel: "otherLabel1",
			httpBeforeCheckRun: HttpBeforeCheckRun{
				PipelineID: 0,
			},
		},

		{
			CheckResult: CheckRunResult{
				CheckResult: CheckResultEnd,
			},
			haveError:       false,
			matchOtherLabel: "",
			httpBeforeCheckRun: HttpBeforeCheckRun{
				PipelineID: 10001,
			},
			mockPipelineWithTasks: &spec.PipelineWithTasks{
				Pipeline: &spec.Pipeline{
					PipelineBase: spec.PipelineBase{
						ID:              10001,
						PipelineSource:  "FDP",
						PipelineYmlName: "230",
						Status:          apistructs.PipelineStatusStopByUser,
					},
					PipelineExtra: spec.PipelineExtra{
						PipelineYml: `
version: 1.1
lifecycle:
  - hook: "` + HookType + `1"
    labels: 
       "otherLabel1": 123
`,
					},
				},
				Tasks: []*spec.PipelineTask{},
			},
		},
	}

	for _, v := range table {
		var e dbclient.Client
		guard := monkey.PatchInstanceMethod(reflect.TypeOf(&e), "GetPipelineWithTasks", func(client *dbclient.Client, id uint64) (*spec.PipelineWithTasks, error) {
			return v.mockPipelineWithTasks, nil
		})
		guard1 := monkey.PatchInstanceMethod(reflect.TypeOf(&e), "ListLabelsByPipelineID", func(client *dbclient.Client, pipelineID uint64, ops ...dbclient.SessionOption) ([]spec.PipelineLabel, error) {
			return nil, nil
		})
		guard2 := monkey.PatchInstanceMethod(reflect.TypeOf(v.httpBeforeCheckRun.LifeCycleClient), "PostLifecycleHookHttpClient", func(_ *lifecycle_hook_client.LifeCycleService, source string, req interface{}, resp interface{}) error {
			checkRunResultRequest := req.(CheckRunResultRequest)
			if checkRunResultRequest.Labels != nil {
				pipelineLabels := checkRunResultRequest.Labels["pipelineLabels"].(map[string]interface{})
				assert.Equal(t, pipelineLabels[v.matchOtherLabel], 123)
			}

			var checkRunResultResponse CheckRunResultResponse
			checkRunResultResponse.Success = true
			checkRunResultResponse.CheckRunResult = v.CheckResult

			checkRunResultResponseJson, _ := json.Marshal(checkRunResultResponse)
			buffer := bytes.NewBuffer(checkRunResultResponseJson)
			err := json.NewDecoder(buffer).Decode(&resp)
			assert.NoError(t, err)
			return nil
		})
		v.httpBeforeCheckRun.DBClient = &e
		defer guard.Unpatch()
		defer guard1.Unpatch()
		defer guard2.Unpatch()

		result, err := v.httpBeforeCheckRun.CheckRun()
		if err != nil {
			assert.True(t, v.haveError, err)
		} else {
			assert.NotEmpty(t, result)
			assert.Equal(t, v.CheckResult.CheckResult, result.CheckResult)
			assert.Equal(t, v.CheckResult.RetryOption.IntervalSecond, result.RetryOption.IntervalSecond)
		}
	}

}
