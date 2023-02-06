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

package unit_test_report

import (
	"encoding/json"
	"fmt"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/aop"
	"github.com/erda-project/erda/internal/tools/pipeline/aop/aoptypes"
)

const taskType = "unit-test"
const actionTypeUnitTest = "unit-test"

// +provider
type provider struct {
	aoptypes.TaskBaseTunePoint
}

func (p *provider) Name() string { return "unit-test-report" }

func (p *provider) Handle(ctx *aoptypes.TuneContext) error {

	if ctx.SDK.Task.Type != taskType {
		return nil
	}

	metadata := ctx.SDK.Task.GetMetadata()
	if len(metadata) == 0 {
		return nil
	}

	var meta = map[string]interface{}{}
	for _, v := range metadata {
		var err error
		switch v.Name {
		case "results":
			var results apistructs.TestResults
			err = json.Unmarshal([]byte(v.Value), &results)
			meta["results"] = results
		case "totals":
			var totals apistructs.TestTotals
			err = json.Unmarshal([]byte(v.Value), &totals)
			meta["totals"] = totals
		case "suites":
			var suites []apistructs.TestSuite
			err = json.Unmarshal([]byte(v.Value), &suites)
			meta["suites"] = suites
		}
		if err != nil {
			return fmt.Errorf("unmarshal unit-test report error: %v", err)
		}
	}

	meta["taskId"] = ctx.SDK.Task.ID

	_, err := ctx.SDK.Report.Create(apistructs.PipelineReportCreateRequest{
		PipelineID: ctx.SDK.Pipeline.ID,
		Type:       actionTypeUnitTest,
		Meta:       meta,
	})
	if err != nil {
		return err
	}

	return nil
}

func (p *provider) Init(ctx servicehub.Context) error {
	err := aop.RegisterTunePoint(p)
	if err != nil {
		panic(err)
	}
	return nil
}

func init() {
	servicehub.Register(aop.NewProviderNameByPluginName(&provider{}), &servicehub.Spec{
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
