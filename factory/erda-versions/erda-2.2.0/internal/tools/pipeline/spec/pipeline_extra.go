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
	"time"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/metadata"
)

// PipelineExtra represents `pipeline_extras` table.
// `pipeline_extras` 与 `pipeline_bases` 一一对应
type PipelineExtra struct {
	PipelineID uint64 `json:"pipelineID,omitempty" xorm:"pk 'pipeline_id'"`

	// PipelineYml 流水线定义文件
	PipelineYml string `json:"pipelineYml"`

	// Extra 额外信息
	Extra PipelineExtraInfo `json:"extra" xorm:"json"`

	// NormalLabels 普通标签，仅展示，不可过滤
	NormalLabels map[string]string `json:"normalLabels" xorm:"json"`

	// Snapshot 运行时的快照
	Snapshot Snapshot `json:"snapshot" xorm:"json"`

	// CommitDetail 提交详情
	CommitDetail apistructs.CommitDetail `json:"commitDetail" xorm:"json"`

	// Progress 流水线整体执行进度，0-100
	// -1 表示未设置
	// progress 只存最终结果，若 >= 0，直接返回，无需再计算
	Progress int `json:"progress"`

	ExtraTimeCreated *time.Time `json:"timeCreated,omitempty" xorm:"created 'time_created'"`
	ExtraTimeUpdated *time.Time `json:"timeUpdated,omitempty" xorm:"updated 'time_updated'"`
}

func (*PipelineExtra) TableName() string {
	return "pipeline_extras"
}

type PipelineExtraInfo struct {
	Namespace               string                       `json:"namespace"`
	NotPipelineControlledNs bool                         `json:"notPipelineControlledNs"`
	DiceWorkspace           apistructs.DiceWorkspace     `json:"diceWorkspace,omitempty"`
	PipelineYmlSource       apistructs.PipelineYmlSource `json:"pipelineYmlSource,omitempty"`
	SubmitUser              *apistructs.PipelineUser     `json:"submitUser,omitempty"`
	RunUser                 *apistructs.PipelineUser     `json:"runUser,omitempty"`
	CancelUser              *apistructs.PipelineUser     `json:"cancelUser,omitempty"`
	InternalClient          string                       `json:"internalClient,omitempty"`
	CronExpr                string                       `json:"cronExpr,omitempty"`
	CronTriggerTime         *time.Time                   `json:"cronTriggerTime,omitempty"` // 秒级精确，毫秒级误差请忽略，cron expr 精确度同样为秒级
	ShowMessage             *apistructs.ShowMessage      `json:"showMessage,omitempty"`
	Messages                []string                     `json:"errors,omitempty"` // TODO ShowMessage 和 Message
	// Deprecated
	ConfigManageNamespaceOfSecretsDefault string `json:"configManageNamespaceOfSecretsDefault,omitempty"`
	// Deprecated
	ConfigManageNamespaceOfSecrets string            `json:"configManageNamespaceOfSecrets,omitempty"`
	ConfigManageNamespaces         []string          `json:"configManageNamespaces,omitempty"`
	IncomingSecrets                map[string]string `json:"incomingSecrets,omitempty"`

	CopyFromPipelineID *uint64            `json:"copyFromPipelineID,omitempty"` // 是否是从其他节点拷贝过来
	RerunFailedDetail  *RerunFailedDetail `json:"rerunFailedDetail,omitempty"`

	IsAutoRun      bool                     `json:"isAutoRun,omitempty"` // 创建后是否自动开始执行
	ShareVolumeID  string                   `json:"shareVolumeId,omitempty"`
	TaskWorkspaces []string                 `json:"taskWorkspaces,omitempty"` //工作目录,例如git
	StorageConfig  apistructs.StorageConfig `json:"storageConfig,omitempty"`  // 挂载设置

	CallbackURLs []string `json:"callbackURLs,omitempty"`

	Version string `json:"version,omitempty"` // 1.1, 1.0

	// 是否已经 完成 Reconciler GC
	CompleteReconcilerGC bool `json:"completeReconcilerGC"`

	// 是否已完成 Reconcile teardown
	CompleteReconcilerTeardown bool `json:"completeReconcilerTeardown"`

	// 用于保存自动转换前的 v1 pipelineYmlName（通过 V1 API 创建的流水线，通过该参数调用 gittar 获取内容）
	PipelineYmlNameV1 string `json:"pipelineYmlNameV1,omitempty"`

	// pipeline 运行时的输入参数
	RunPipelineParams []apistructs.PipelineRunParam `json:"runPipelineParams,omitempty"`

	// GC
	GC apistructs.PipelineGC `json:"gc,omitempty"`

	// OutputDefines
	DefinedOutputs []apistructs.PipelineOutput `json:"definedOutputs,omitempty"`

	SnippetChain []uint64 `json:"snippetChain,omitempty"`

	QueueInfo *QueueInfo `json:"queueInfo,omitempty"`

	TaskOperates []apistructs.PipelineTaskOperateRequest `json:"taskTaskOperates,omitempty"`

	ContainerInstanceProvider *apistructs.ContainerInstanceProvider `json:"containerInstanceProvider,omitempty"`
}

type QueueInfo struct {
	QueueID          uint64                          `json:"queueID"`
	CustomPriority   int64                           `json:"customPriority"`
	EnqueueCondition apistructs.EnqueueConditionType `json:"enqueueCondition"`
	// Pipeline priority changed history from initial to latest in queue
	PriorityChangeHistory []int64 `json:"priorityChangeHistory,omitempty"`
}

type Snapshot struct {
	PipelineYml     string            `json:"pipeline_yml,omitempty"` // 对占位符进行渲染
	Secrets         map[string]string `json:"secrets,omitempty"`
	PlatformSecrets map[string]string `json:"platformSecrets,omitempty"`
	CmsDiceFiles    map[string]string `json:"cmsDiceFiles,omitempty"`
	Envs            map[string]string `json:"envs,omitempty"`

	AnalyzedCrossCluster *bool `json:"analyzedCrossCluster,omitempty"`

	RunPipelineParams apistructs.PipelineRunParamsWithValue `json:"runPipelineParams,omitempty"` // 流水线运行时参数

	// IdentityInfo 身份信息
	IdentityInfo apistructs.IdentityInfo `json:"identityInfo" xorm:"json"`

	// OutputValues output 定义和从 task 里采集上来的值
	OutputValues []apistructs.PipelineOutputWithValue `json:"outputValues,omitempty"`

	// AppliedResources calculated by all actions
	AppliedResources apistructs.PipelineAppliedResources `json:"appliedResources,omitempty"`

	// BindQueue stores the binding queue info if have.
	BindQueue *apistructs.PipelineQueue `json:"bindQueue,omitempty"`

	// Events stores pipeline level k8s-like events
	Events []*apistructs.PipelineEvent `json:"events,omitempty"`

	// EncryptSecretKeys the encrypt envs' key list
	EncryptSecretKeys []string `json:"encryptSecretKeys"`
}

// FromDB 兼容 Snapshot 老数据
func (s *Snapshot) FromDB(b []byte) error {

	// 先用 map[string]string 解析
	if err := json.Unmarshal(b, s); err == nil {
		return nil
	}

	// 用老数据结构 []metadata.MetadataField 进行解析，并赋值回 s
	so := struct {
		PipelineYml string                   `json:"pipeline_yml,omitempty"`
		Secrets     []metadata.MetadataField `json:"secrets,omitempty"`
	}{}
	if err := json.Unmarshal(b, &so); err == nil {
		s.PipelineYml = so.PipelineYml
		s.Secrets = make(map[string]string)
		for _, item := range so.Secrets {
			s.Secrets[item.Name] = item.Value
		}
	}

	// 反序列化失败，忽略错误，该字段可忽略

	return nil
}

func (s *Snapshot) ToDB() ([]byte, error) {
	return json.Marshal(s)
}

type RerunFailedDetail struct {
	RerunPipelineID uint64            `json:"rerunPipelineID,omitempty"`
	StageIndex      int               `json:"stageIndex,omitempty"`
	SuccessTasks    map[string]uint64 `json:"successTasks,omitempty"`
	FailedTasks     map[string]uint64 `json:"failedTasks,omitempty"`
	NotExecuteTasks map[string]uint64 `json:"notExecuteTasks,omitempty"`
}

func (extra *PipelineExtra) GetCommitID() string {
	return extra.CommitDetail.CommitID
}

func (extra *PipelineExtra) GetOrgName() string {
	if extra.NormalLabels != nil {
		orgName, ok := extra.NormalLabels[apistructs.LabelOrgName]
		if ok {
			return orgName
		}
	}
	return ""
}
