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

package taskpolicy

import (
	"reflect"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/mysqlxorm"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

type config struct {
}

type provider struct {
	Log   logs.Logger
	Cfg   *config
	MySQL mysqlxorm.Interface

	dbClient          *dbclient.Client
	supportedPolicies map[apistructs.PolicyType]Interface
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.dbClient = &dbclient.Client{Engine: p.MySQL.DB()}
	p.supportedPolicies = map[apistructs.PolicyType]Interface{
		apistructs.TryLatestSuccessResultPolicyType: tryLastSuccessResult{p: p},
		apistructs.NewRunPolicyType:                 newRun{p: p},
		apistructs.TryLatestResultPolicyType:        tryLastResult{p: p},
	}
	return nil
}

func (p *provider) resetTask(task *spec.PipelineTask, statuses ...apistructs.PipelineStatus) error {
	if !task.IsSnippet {
		return nil
	}

	if task.Extra.Action.SnippetConfig == nil {
		return nil
	}

	ymlName, source := getPipelineSourceAndNameBySnippetConfig(task.Extra.Action.SnippetConfig)

	strStatuses := make([]string, 0, len(statuses))
	for _, status := range statuses {
		strStatuses = append(strStatuses, status.String())
	}
	result, err := p.dbClient.PageListPipelines(apistructs.PipelinePageListRequest{
		Sources:        []apistructs.PipelineSource{source},
		YmlNames:       []string{ymlName},
		Statuses:       strStatuses,
		PageNum:        1,
		PageSize:       1,
		IncludeSnippet: true,
		DescCols:       []string{apistructs.PipelinePageListRequestIdColumn},
	})
	if err != nil {
		return err
	}
	statusPipelines := result.Pipelines
	if len(statusPipelines) <= 0 {
		return nil
	}
	pipeline := statusPipelines[0]

	beforeSuccessTask, err := p.dbClient.GetPipelineTask(pipeline.ParentTaskID)
	if err != nil {
		return err
	}

	if beforeSuccessTask.ID <= 0 {
		return nil
	}

	task.Status = beforeSuccessTask.Status
	task.Result = beforeSuccessTask.Result
	task.IsSnippet = beforeSuccessTask.IsSnippet
	task.SnippetPipelineDetail = beforeSuccessTask.SnippetPipelineDetail
	task.TimeBegin = beforeSuccessTask.TimeBegin
	task.TimeEnd = beforeSuccessTask.TimeEnd
	task.CostTimeSec = beforeSuccessTask.CostTimeSec
	task.QueueTimeSec = beforeSuccessTask.QueueTimeSec
	task.SnippetPipelineID = beforeSuccessTask.SnippetPipelineID
	if task.Extra.Action.Policy != nil {
		task.Extra.CurrentPolicy = apistructs.Policy{
			Type: task.Extra.Action.Policy.Type,
		}
	}
	return nil
}

func getPipelineSourceAndNameBySnippetConfig(snippetConfig *pipelineyml.SnippetConfig) (ymlName string, sources apistructs.PipelineSource) {
	return snippetConfig.Name, apistructs.PipelineSource(snippetConfig.Source)
}

func init() {
	interfaceType := reflect.TypeOf((*Interface)(nil)).Elem()
	servicehub.Register("task-policy", &servicehub.Spec{
		Services:     []string{"task-policy"},
		Types:        []reflect.Type{interfaceType},
		Dependencies: nil,
		Description:  "pipeline task policy",
		ConfigFunc:   func() interface{} { return &config{} },
		Creator:      func() servicehub.Provider { return &provider{} },
	})
}
