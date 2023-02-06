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
	"fmt"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/lifecycle_hook_client"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

const CheckResultSuccess = "success"
const CheckResultFailed = "failed"
const CheckResultEnd = "end"
const HookType = "before-run-check"

type HttpBeforeCheckRun struct {
	PipelineID      uint64
	Bdl             *bundle.Bundle
	DBClient        *dbclient.Client
	LifeCycleClient *lifecycle_hook_client.LifeCycleService
}

type RetryOption struct {
	IntervalSecond      uint64 `json:"intervalSecond"`
	IntervalMillisecond uint64 `json:"intervalMillisecond"`
}

type CheckRunResult struct {
	CheckResult string      `json:"checkResult"`
	RetryOption RetryOption `json:"retryOption"`
	Message     string      `json:"message"`
}

type CheckRunResultRequest struct {
	Hook                string                 `json:"hook"`
	Labels              map[string]interface{} `json:"labels"`
	Source              string                 `json:"source"`
	PipelineYamlName    string                 `json:"pipelineYamlName"`
	PipelineYamlContent string                 `json:"pipelineYamlContent"`
}

type CheckRunResultResponse struct {
	apistructs.Header
	CheckRunResult CheckRunResult `json:"data"`
}

type HookBeforeCheckRun interface {
	CheckRun() (*CheckRunResult, error)
	GetPipelineWithTasks()
}

// filter out the corresponding type of hook
func (beforeCheckRun *HttpBeforeCheckRun) matchHookType(lifecycle []*pipelineyml.NetworkHookInfo) []*pipelineyml.NetworkHookInfo {
	var result []*pipelineyml.NetworkHookInfo
	for _, v := range lifecycle {
		if v.Hook == HookType {
			result = append(result, v)
		}
	}
	return result
}

func (beforeCheckRun HttpBeforeCheckRun) CheckRun() (result *CheckRunResult, err error) {
	if beforeCheckRun.PipelineID <= 0 {
		return nil, fmt.Errorf("pipelineID can not empty")
	}

	pipelineWithTasks, err := beforeCheckRun.DBClient.GetPipelineWithTasks(beforeCheckRun.PipelineID)
	if err != nil {
		return nil, err
	}

	// if pipeline is end status return end result
	if pipelineWithTasks.Pipeline.Status.IsEndStatus() {
		return &CheckRunResult{
			CheckResult: CheckResultEnd,
		}, nil
	}

	yml, err := pipelineyml.New(
		[]byte(pipelineWithTasks.Pipeline.PipelineYml),
	)
	if err != nil {
		return nil, err
	}

	// if the network hook is not specified
	//the network hook will be passed by success
	matchInfo := beforeCheckRun.matchHookType(yml.Spec().Lifecycle)
	if matchInfo == nil {
		return &CheckRunResult{
			CheckResult: CheckResultSuccess,
		}, nil
	}
	pipeline := pipelineWithTasks.Pipeline

	for _, info := range matchInfo {
		var checkRunResultRequest CheckRunResultRequest

		checkRunResultRequest.Hook = info.Hook
		checkRunResultRequest.PipelineYamlContent = pipeline.PipelineYml
		checkRunResultRequest.Source = pipeline.PipelineSource.String()
		checkRunResultRequest.PipelineYamlName = pipeline.PipelineYmlName

		checkRunResultRequest.Labels = map[string]interface{}{}
		checkRunResultRequest.Labels["pipelineID"] = pipeline.ID
		checkRunResultRequest.Labels["pipelineLabels"] = info.Labels

		var response CheckRunResultResponse
		err := beforeCheckRun.LifeCycleClient.PostLifecycleHookHttpClient(info.Client, checkRunResultRequest, &response)
		if err != nil {
			return nil, err
		}

		if !response.Success {
			return nil, fmt.Errorf("response is empty or response not success")
		}

		// return directly if there is an failed
		if response.CheckRunResult.CheckResult == CheckResultFailed {
			return &response.CheckRunResult, nil
		}
	}

	return &CheckRunResult{
		CheckResult: CheckResultSuccess,
	}, nil
}
