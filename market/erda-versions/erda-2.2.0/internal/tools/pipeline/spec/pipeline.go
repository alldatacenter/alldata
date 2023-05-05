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

package spec

import (
	"encoding/json"
	"fmt"
	"strconv"
	"time"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/conf"
	definitiondb "github.com/erda-project/erda/internal/tools/pipeline/providers/definition/db"
	sourcedb "github.com/erda-project/erda/internal/tools/pipeline/providers/source/db"
	"github.com/erda-project/erda/pkg/strutil"
)

// Pipeline
type Pipeline struct {
	PipelineBase
	PipelineExtra
	Definition *definitiondb.PipelineDefinition
	Source     *sourcedb.PipelineSource
	Labels     map[string]string
}

type PipelineWithStage struct {
	Pipeline
	PipelineStages []*PipelineStageWithTask
}

type PipelineStageWithTask struct {
	PipelineStage
	PipelineTasks []*PipelineTask
}

type PipelineWithStageAndTask struct {
	Pipeline
	PipelineStages []PipelineStage
	PipelineTasks  []PipelineTask
}

type PipelineCombosReq struct {
	Branches []string `json:"branches"`
	Sources  []string `json:"sources"`
	YmlNames []string `json:"ymlNames"`
}

type PipelineWithTasks struct {
	Pipeline *Pipeline
	Tasks    []*PipelineTask
}

func (p *PipelineWithTasks) DoneTasks() []string {
	var dones []string
	for _, task := range p.Tasks {
		if task.Status.IsEndStatus() || task.Status == apistructs.PipelineStatusDisabled {
			dones = append(dones, task.Name)
		}
	}
	return dones
}

func (p *Pipeline) GenIdentityInfo() apistructs.IdentityInfo {
	// 默认从快照获取
	if !p.Snapshot.IdentityInfo.Empty() {
		return p.Snapshot.IdentityInfo
	}
	// 老数据从 extra 里获取
	var userID string
	if p.Extra.SubmitUser != nil && p.Extra.SubmitUser.ID != nil {
		userID = fmt.Sprintf("%v", p.Extra.SubmitUser.ID)
	}
	return apistructs.IdentityInfo{
		UserID:         userID,
		InternalClient: p.Extra.InternalClient,
	}
}

func (p *Pipeline) GetSubmitUserID() string {
	var userID string
	if p.Extra.SubmitUser != nil && p.Extra.SubmitUser.ID != nil {
		userID = fmt.Sprintf("%v", p.Extra.SubmitUser.ID)
	}
	return mustUserID(userID)
}

func (p *Pipeline) GetRunUserID() string {
	var userID string
	if p.Extra.RunUser != nil && p.Extra.RunUser.ID != nil {
		userID = fmt.Sprintf("%v", p.Extra.RunUser.ID)
	}
	return mustUserID(userID)
}

// GetUserID first to get run userID,if not exists, to get submit userID
// Not included internal UserID
func (p *Pipeline) GetUserID() string {
	if p.Extra.RunUser != nil && p.Extra.RunUser.ID != nil {
		return fmt.Sprintf("%v", p.Extra.RunUser.ID)
	}
	if p.Extra.SubmitUser != nil && p.Extra.SubmitUser.ID != nil {
		return fmt.Sprintf("%v", p.Extra.SubmitUser.ID)
	}
	return ""
}

func (p *Pipeline) GetCancelUserID() string {
	var userID string
	if p.Extra.CancelUser != nil && p.Extra.CancelUser.ID != nil {
		userID = fmt.Sprintf("%v", p.Extra.CancelUser.ID)
	}
	return mustUserID(userID)
}

func (p *Pipeline) GetLabel(labelKey string) string {
	return p.MergeLabels()[labelKey]
}

func mustUserID(userID string) string {
	if userID != "" {
		return userID
	}
	return conf.InternalUserID()
}

func (p *Pipeline) MergeLabels() map[string]string {
	mergeLabels := make(map[string]string)
	for k, v := range p.NormalLabels {
		mergeLabels[k] = v
	}
	for k, v := range p.Labels {
		mergeLabels[k] = v
	}
	return mergeLabels
}

// GenerateNormalLabelsForCreateV2
// pipeline.createV2 有一些字段通过标签来传递，例如 commit
func (p *Pipeline) GenerateNormalLabelsForCreateV2() map[string]string {
	labels := p.MergeLabels()

	// org
	labels[apistructs.LabelOrgName] = p.GetOrgName()
	// workspace
	labels[apistructs.LabelDiceWorkspace] = p.Extra.DiceWorkspace.String()
	// commit
	labels[apistructs.LabelCommit] = p.GetCommitID()
	commitDetailStr, err := json.Marshal(p.CommitDetail)
	if err == nil && string(commitDetailStr) != "{}" {
		labels[apistructs.LabelCommitDetail] = string(commitDetailStr)
	}
	// userID
	labels[apistructs.LabelUserID] = p.GetSubmitUserID()

	labels[apistructs.LabelPipelineType] = p.Type.String()
	labels[apistructs.LabelPipelineYmlSource] = p.Extra.PipelineYmlSource.String()
	labels[apistructs.LabelPipelineTriggerMode] = p.TriggerMode.String()
	var cronTriggerTimeStr string
	if p.Extra.CronTriggerTime != nil {
		cronTriggerTimeStr = strconv.FormatInt(p.Extra.CronTriggerTime.UnixNano(), 10)
	}
	labels[apistructs.LabelPipelineCronTriggerTime] = cronTriggerTimeStr
	if p.CronID != nil {
		labels[apistructs.LabelPipelineCronID] = strconv.FormatUint(*p.CronID, 10)
	}

	for k, v := range labels {
		if v == "" {
			delete(labels, k)
		}
	}
	return labels
}

// GenerateV1UniquePipelineYmlName 为 v1 pipeline 返回 pipelineYmlName，该 name 在 source 下唯一
// 生成规则: AppID/DiceWorkspace/Branch/PipelineYmlPath
// 1) 100/PROD/master/ec/dws/itm/workflow/item_1d_df_process.workflow
// 2) 200/DEV/feature/dice/pipeline.yml
func (p *Pipeline) GenerateV1UniquePipelineYmlName(originPipelineYmlPath string) string {
	// source != (dice || bigdata) 时无需转换
	if !(p.PipelineSource == apistructs.PipelineSourceDice || p.PipelineSource == apistructs.PipelineSourceBigData) {
		return originPipelineYmlPath
	}
	// 若 originPipelineYmlPath 已经符合生成规则，则直接返回
	ss := strutil.Split(originPipelineYmlPath, "/", true)
	if len(ss) > 3 {
		appID, _ := strconv.ParseUint(ss[0], 10, 64)
		workspace := ss[1]
		branchWithYmlName := strutil.Join(ss[2:], "/", true)
		branchPrefix := strutil.Concat(p.Labels[apistructs.LabelBranch], "/")
		if strconv.FormatUint(appID, 10) == p.Labels[apistructs.LabelAppID] &&
			workspace == p.Extra.DiceWorkspace.String() &&
			strutil.HasPrefixes(branchWithYmlName, branchPrefix) &&
			len(branchWithYmlName) > len(branchPrefix) {
			return originPipelineYmlPath
		}
	}
	return fmt.Sprintf("%s/%s/%s/%s", p.Labels[apistructs.LabelAppID], p.Extra.DiceWorkspace.String(),
		p.Labels[apistructs.LabelBranch], originPipelineYmlPath)
}

// DecodeV1UniquePipelineYmlName 根据 GenerateV1UniquePipelineYmlName 生成规则，反解析得到 originName
func (p *Pipeline) DecodeV1UniquePipelineYmlName(name string) string {
	prefix := fmt.Sprintf("%s/%s/%s/", p.Labels[apistructs.LabelAppID], p.Extra.DiceWorkspace.String(), p.Labels[apistructs.LabelBranch])
	return strutil.TrimPrefixes(name, prefix)
}

func (p *Pipeline) GetConfigManageNamespaces() []string {
	return strutil.DedupSlice(
		append([]string{p.Extra.ConfigManageNamespaceOfSecretsDefault, p.Extra.ConfigManageNamespaceOfSecrets},
			p.Extra.ConfigManageNamespaces...), true)
}

// EnsureGC without nil field
func (p *Pipeline) EnsureGC() {
	gc := &p.Extra.GC
	// resource
	if gc.ResourceGC.SuccessTTLSecond == nil {
		gc.ResourceGC.SuccessTTLSecond = &[]uint64{conf.SuccessPipelineDefaultResourceGCTTLSec()}[0]
	}
	if gc.ResourceGC.FailedTTLSecond == nil {
		gc.ResourceGC.FailedTTLSecond = &[]uint64{conf.FailedPipelineDefaultResourceGCTTLSec()}[0]
	}
	// database
	if gc.DatabaseGC.Analyzed.NeedArchive == nil {
		gc.DatabaseGC.Analyzed.NeedArchive = &[]bool{false}[0]
	}
	if gc.DatabaseGC.Analyzed.TTLSecond == nil {
		gc.DatabaseGC.Analyzed.TTLSecond = &[]uint64{conf.AnalyzedPipelineDefaultDatabaseGCTTLSec()}[0]
	}
	if gc.DatabaseGC.Finished.NeedArchive == nil {
		gc.DatabaseGC.Finished.NeedArchive = &[]bool{true}[0]
	}
	if gc.DatabaseGC.Finished.TTLSecond == nil {
		gc.DatabaseGC.Finished.TTLSecond = &[]uint64{conf.FinishedPipelineDefaultDatabaseGCTTLSec()}[0]
	}
}

func (p *Pipeline) GetResourceGCTTL() uint64 {
	p.EnsureGC()
	resourceGCTTL := *p.Extra.GC.ResourceGC.FailedTTLSecond
	if p.Status.IsSuccessStatus() {
		resourceGCTTL = *p.Extra.GC.ResourceGC.SuccessTTLSecond
	}
	return resourceGCTTL
}

// GetPipelineQueueID return pipeline queue id if exist, or 0.
func (p *Pipeline) GetPipelineQueueID() (uint64, bool) {
	if p.Extra.QueueInfo == nil {
		return 0, false
	}
	return p.Extra.QueueInfo.QueueID, true
}

// GetPipelineAppliedResources return limited and min resource when pipeline run.
func (p *Pipeline) GetPipelineAppliedResources() apistructs.PipelineAppliedResources {
	return p.Snapshot.AppliedResources
}

// CanSkipRunningCheck if pipeline bind queue and EnqueueCondition is skip running, pipeline can skip limit running
func (p *Pipeline) CanSkipRunningCheck() bool {
	if p.Extra.QueueInfo != nil && p.Extra.QueueInfo.EnqueueCondition.IsSkipAlreadyRunningLimit() {
		return true
	}
	return false
}

func (p *Pipeline) CanDelete() (bool, string) {
	// status
	if !p.Status.CanDelete() {
		return false, fmt.Sprintf("invalid status: %s", p.Status)
	}
	// Need to check ttl if status is endStatus
	if p.Status.IsEndStatus() {
		if p.TimeEnd != nil && p.Extra.GC.DatabaseGC.Finished.TTLSecond != nil {
			ok := p.TimeEnd.Before(time.Now().Add(-time.Duration(int64(*p.Extra.GC.DatabaseGC.Finished.TTLSecond)) * time.Second))
			if ok {
				return true, ""
			}
		}
		if !p.Extra.CompleteReconcilerGC {
			return false, fmt.Sprintf("waiting gc")
		}
	}
	return true, ""
}

func (p *Pipeline) CanArchive() (bool, string) {
	return p.CanDelete()
}
