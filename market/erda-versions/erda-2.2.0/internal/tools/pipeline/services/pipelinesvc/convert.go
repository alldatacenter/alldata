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

package pipelinesvc

import (
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/numeral"
)

func (s *PipelineSvc) convertPipelineBase(p spec.PipelineBase) apistructs.PipelineDTO {
	var result apistructs.PipelineDTO
	result.ID = p.ID
	result.CronID = p.CronID
	result.Source = p.PipelineSource
	result.YmlName = p.PipelineYmlName
	result.Type = p.Type.String()
	result.TriggerMode = p.TriggerMode.String()
	result.ClusterName = p.ClusterName
	result.Status = p.Status
	result.CostTimeSec = p.CostTimeSec
	result.TimeBegin = p.TimeBegin
	result.TimeEnd = p.TimeEnd
	result.TimeCreated = p.TimeCreated
	result.TimeUpdated = p.TimeUpdated
	return result
}

func (s *PipelineSvc) ConvertPipeline(p *spec.Pipeline) *apistructs.PipelineDTO {
	if p == nil {
		return nil
	}

	var result = s.convertPipelineBase(p.PipelineBase)

	// from extra
	if p.TriggerMode == apistructs.PipelineTriggerModeCron && p.Extra.CronTriggerTime != nil {
		// if pipeline is rerun and rerun failed, don't need to convert trigger time
		pipelineType := p.Labels[apistructs.LabelPipelineType]
		if pipelineType != apistructs.PipelineTypeRerun.String() &&
			pipelineType != apistructs.PipelineTypeRerunFailed.String() {
			result.TimeCreated = p.Extra.CronTriggerTime
			result.TimeBegin = p.Extra.CronTriggerTime
		}
	}
	result.Namespace = p.Extra.Namespace
	result.OrgName = p.GetOrgName()
	result.ProjectName = p.GetLabel(apistructs.LabelProjectName)
	result.ApplicationName = p.GetLabel(apistructs.LabelAppName)
	result.Commit = p.GetCommitID()
	result.CommitDetail = p.CommitDetail
	result.YmlSource = p.GetLabel(apistructs.LabelPipelineYmlSource)
	result.YmlNameV1 = p.Extra.PipelineYmlNameV1
	result.YmlContent = p.PipelineYml
	result.Extra.DiceWorkspace = p.Extra.DiceWorkspace.String()
	result.Extra.SubmitUser = p.Extra.SubmitUser
	result.Extra.RunUser = p.Extra.RunUser
	result.Extra.CancelUser = p.Extra.CancelUser
	result.Extra.CronExpr = p.Extra.CronExpr
	result.Extra.CronTriggerTime = p.Extra.CronTriggerTime
	result.Extra.ShowMessage = p.Extra.ShowMessage
	result.Extra.ConfigManageNamespaces = p.GetConfigManageNamespaces()
	result.Extra.IsAutoRun = p.Extra.IsAutoRun
	result.Extra.CallbackURLs = p.Extra.CallbackURLs
	result.Progress = s.convertProgress(*p)

	// from labels
	orgID, _ := strconv.ParseUint(p.Labels[apistructs.LabelOrgID], 10, 64)
	result.OrgID = orgID
	projectID, _ := strconv.ParseUint(p.Labels[apistructs.LabelProjectID], 10, 64)
	result.ProjectID = projectID
	appID, _ := strconv.ParseUint(p.Labels[apistructs.LabelAppID], 10, 64)
	result.ApplicationID = appID
	result.Branch = p.Labels[apistructs.LabelBranch]

	return &result
}

func (s *PipelineSvc) Convert2PagePipeline(p *spec.Pipeline) *apistructs.PagePipeline {
	result := apistructs.PagePipeline{
		ID:      p.ID,
		CronID:  p.CronID,
		Commit:  p.GetCommitID(),
		Source:  p.PipelineSource,
		YmlName: p.PipelineYmlName,
		Extra: apistructs.PipelineExtra{
			DiceWorkspace:          p.Extra.DiceWorkspace.String(),
			SubmitUser:             p.Extra.SubmitUser,
			RunUser:                p.Extra.RunUser,
			CancelUser:             p.Extra.CancelUser,
			CronExpr:               p.Extra.CronExpr,
			CronTriggerTime:        p.Extra.CronTriggerTime,
			ShowMessage:            p.Extra.ShowMessage,
			ConfigManageNamespaces: p.GetConfigManageNamespaces(),
			IsAutoRun:              p.Extra.IsAutoRun,
			CallbackURLs:           p.Extra.CallbackURLs,
			PipelineYmlNameV1:      p.Extra.PipelineYmlNameV1,
		},
		FilterLabels:     p.Labels,
		NormalLabels:     p.NormalLabels,
		Type:             p.Type.String(),
		TriggerMode:      p.TriggerMode.String(),
		ClusterName:      p.ClusterName,
		Status:           p.Status,
		Progress:         s.convertProgress(*p),
		IsSnippet:        p.IsSnippet,
		ParentPipelineID: p.ParentPipelineID,
		ParentTaskID:     p.ParentTaskID,
		CostTimeSec:      p.CostTimeSec,
		TimeBegin:        p.TimeBegin,
		TimeEnd:          p.TimeEnd,
		TimeCreated:      p.TimeCreated,
		TimeUpdated:      p.TimeUpdated,
	}
	if p.TriggerMode == apistructs.PipelineTriggerModeCron && p.Extra.CronTriggerTime != nil {
		pipelineType := p.Labels[apistructs.LabelPipelineType]
		if pipelineType != apistructs.PipelineTypeRerun.String() &&
			pipelineType != apistructs.PipelineTypeRerunFailed.String() {
			result.TimeCreated = p.Extra.CronTriggerTime
			result.TimeBegin = p.Extra.CronTriggerTime
		}
	}
	if p.Definition != nil && p.Source != nil {
		definitionPageInfo := &apistructs.DefinitionPageInfo{}
		definitionPageInfo.Name = p.Definition.Name
		definitionPageInfo.Creator = p.Definition.Creator
		definitionPageInfo.Executor = p.Definition.Executor
		definitionPageInfo.SourceRef = p.Source.Ref
		definitionPageInfo.SourceRemote = p.Source.Remote
		result.DefinitionPageInfo = definitionPageInfo
	}
	return &result
}

func (s *PipelineSvc) BatchConvert2PagePipeline(pipelines []spec.Pipeline) []apistructs.PagePipeline {
	result := make([]apistructs.PagePipeline, 0, len(pipelines))
	for _, p := range pipelines {
		result = append(result, *s.Convert2PagePipeline(&p))
	}
	return result
}

// input  progress: int
// output progress: float64
func (s *PipelineSvc) convertProgress(p spec.Pipeline) float64 {
	progress, err := s.calculateProgress(p)
	if err != nil {
		return 0
	}
	return numeral.Round(float64(progress)/float64(100), 2)
}
