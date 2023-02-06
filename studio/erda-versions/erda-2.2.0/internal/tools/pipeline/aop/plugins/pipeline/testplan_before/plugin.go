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

package testplan_before

import (
	"errors"
	"strings"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/autotest"
	"github.com/erda-project/erda/internal/tools/pipeline/aop"
	"github.com/erda-project/erda/internal/tools/pipeline/aop/aoptypes"
)

// +provider
type provider struct {
	aoptypes.PipelineBaseTunePoint
}

func (p *provider) Name() string { return "testplan-before" }

func (p *provider) Handle(ctx *aoptypes.TuneContext) error {
	// source = autotest
	if ctx.SDK.Pipeline.PipelineSource != apistructs.PipelineSourceAutoTest || ctx.SDK.Pipeline.IsSnippet {
		return nil
	}

	// filter PipelineYmlName is not autotest-plan-xxx
	isTestPlan := checkPipelineYmlName(ctx.SDK.Pipeline.PipelineYmlName)
	if !isTestPlan {
		return nil
	}

	if _, ok := ctx.SDK.Pipeline.Snapshot.Secrets[autotest.CmsCfgKeyAPIGlobalConfig]; !ok {
		return errors.New("pipeline config is not existed, pipelineYmlName: " + ctx.SDK.Pipeline.PipelineYmlName)
	}

	meta := make(apistructs.PipelineReportMeta)
	meta["data"] = ctx.SDK.Pipeline.Snapshot.Secrets[autotest.CmsCfgKeyAPIGlobalConfig]
	// add cfg display name to auto-test-execute-config report
	meta[autotest.CmsCfgKeyDisplayName] = ctx.SDK.Pipeline.Snapshot.Secrets[autotest.CmsCfgKeyDisplayName]

	// report
	_, err := ctx.SDK.Report.Create(apistructs.PipelineReportCreateRequest{
		PipelineID: ctx.SDK.Pipeline.ID,
		Type:       apistructs.PipelineReportTypeAutotestPlan,
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

// checkPipelineYmlName check PipelineYmlName is autotest-plan-xxx
func checkPipelineYmlName(s string) bool {
	pipelineNamePre := apistructs.PipelineSourceAutoTestPlan.String() + "-"
	if !strings.HasPrefix(s, pipelineNamePre) {
		return false
	}
	return true
}
