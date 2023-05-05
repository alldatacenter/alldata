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
	"time"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/conf"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/taskerror"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/taskinspect"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/taskresult"
	"github.com/erda-project/erda/pkg/encoding/jsonparse"
	"github.com/erda-project/erda/pkg/metadata"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

const (
	CtxExecutorChKeyPrefix         = "executor-done-chan"
	CtxExecutorChDataVersionPrefix = "executor-done-chan-data-version"
)

type PipelineTask struct {
	ID         uint64 `json:"id" xorm:"pk autoincr"`
	PipelineID uint64 `json:"pipelineID"`
	StageID    uint64 `json:"stageID"`

	Name         string                    `json:"name"`
	OpType       PipelineTaskOpType        `json:"opType"`         // Deprecated: get, put, task
	Type         string                    `json:"type,omitempty"` // git, buildpack, release, dice ... 当 OpType 为自定义任务时为空
	ExecutorKind PipelineTaskExecutorKind  `json:"executorKind"`   // scheduler, memory
	Status       apistructs.PipelineStatus `json:"status"`
	Extra        PipelineTaskExtra         `json:"extra" xorm:"json"`
	Context      PipelineTaskContext       `json:"context" xorm:"json"`
	Result       *taskresult.Result        `json:"result" xorm:"json"`
	Inspect      taskinspect.Inspect       `json:"inspect" xorm:"json"`

	IsSnippet             bool                                  `json:"isSnippet"`                         // 该节点是否是嵌套流水线节点
	SnippetPipelineID     *uint64                               `json:"snippetPipelineID"`                 // 嵌套的流水线 id
	SnippetPipelineDetail *apistructs.PipelineTaskSnippetDetail `json:"snippetPipelineDetail" xorm:"json"` // 嵌套的流水线详情

	CostTimeSec  int64     `json:"costTimeSec"`                // -1 表示暂无耗时信息, 0 表示确实是0s结束
	QueueTimeSec int64     `json:"queueTimeSec"`               // 等待调度的耗时, -1 暂无耗时信息, 0 表示确实是0s结束 TODO 赋值
	TimeBegin    time.Time `json:"timeBegin"`                  // 执行开始时间
	TimeEnd      time.Time `json:"timeEnd"`                    // 执行结束时间
	TimeCreated  time.Time `json:"timeCreated" xorm:"created"` // 记录创建时间
	TimeUpdated  time.Time `json:"timeUpdated" xorm:"updated"` // 记录更新时间

	IsEdge bool `json:"isEdge"`
}

func (pt *PipelineTask) NodeName() string {
	return pt.Name
}

func (pt *PipelineTask) PrevNodeNames() []string {
	return pt.Extra.RunAfter
}

func (pt *PipelineTask) GetBigDataConf() (apistructs.BigdataSpec, error) {
	if value, ok := pt.Extra.Action.Params["bigDataConf"]; ok {
		var bigdataSpec apistructs.BigdataSpec
		valueStr := jsonparse.JsonOneLine(value)
		if err := json.Unmarshal([]byte(valueStr), &bigdataSpec); err != nil {
			return bigdataSpec, err
		}
		return bigdataSpec, nil
	}
	return apistructs.BigdataSpec{}, nil
}

func (pt *PipelineTask) GetExecutorName() PipelineTaskExecutorName {
	switch pt.ExecutorKind {
	// PipelineTaskExecutorKindScheduler after 2.1 version, scheduler executor is deleted.
	// this is compatible with old scheduler executor
	case PipelineTaskExecutorKindScheduler:
		k := PipelineTaskExecutorKindK8sJob
		if spec, err := pt.GetBigDataConf(); err == nil {
			if spec.FlinkConf != nil {
				k = PipelineTaskExecutorKindK8sFlink
			}
			if spec.SparkConf != nil {
				k = PipelineTaskExecutorKindK8sSpark
			}
		}
		return PipelineTaskExecutorName(fmt.Sprintf("%s-%s", k.GetDefaultExecutorName(), pt.Extra.ClusterName))
	default:
		return pt.Extra.ExecutorName
	}
}

func (*PipelineTask) TableName() string {
	return "pipeline_tasks"
}

type PipelineTaskExtra struct {
	Namespace               string                     `json:"namespace,omitempty"`
	NotPipelineControlledNs bool                       `json:"notPipelineControlledNs,omitempty"`
	ExecutorName            PipelineTaskExecutorName   `json:"executorName,omitempty"`
	ClusterName             string                     `json:"clusterName,omitempty"`
	AllowFailure            bool                       `json:"allowFailure,omitempty"`
	Pause                   bool                       `json:"pause,omitempty"`
	Timeout                 time.Duration              `json:"timeout,omitempty"`
	PrivateEnvs             map[string]string          `json:"envs,omitempty"`       // PrivateEnvs 由 agent 注入 run 运行时，run 可见，容器内不可见
	PublicEnvs              map[string]string          `json:"publicEnvs,omitempty"` // PublicEnvs 注入容器，run 可见，容器内亦可见
	Labels                  map[string]string          `json:"labels,omitempty"`
	Image                   string                     `json:"image,omitempty"`
	Cmd                     string                     `json:"cmd,omitempty"`
	CmdArgs                 []string                   `json:"cmdArgs,omitempty"`
	Binds                   []apistructs.Bind          `json:"binds,omitempty"`
	TaskContainers          []apistructs.TaskContainer `json:"taskContainers"`
	// Volumes 创建 task 时的 volumes 快照
	// 若一开始 volume 无 volumeID，启动 task 后返回的 volumeID 不会在这里更新，只会更新到 task.Context.OutStorages 里
	Volumes         []metadata.MetadataField `json:"volumes,omitempty"` //
	PreFetcher      *apistructs.PreFetcher   `json:"preFetcher,omitempty"`
	RuntimeResource RuntimeResource          `json:"runtimeResource,omitempty"`
	UUID            string                   `json:"uuid"` // 用于查询日志等，pipeline 开始执行时才会赋值 // 对接多个 executor，不一定每个 executor 都能自定义 UUID，所以这个 uuid 实际上是目标系统的 uuid
	TimeBeginQueue  time.Time                `json:"timeBeginQueue"`
	TimeEndQueue    time.Time                `json:"timeEndQueue"`
	StageOrder      int                      `json:"stageOrder"` // 0,1,2,...

	// RunAfter indicates the tasks this task depends.
	RunAfter []string `json:"runAfter"`

	FlinkSparkConf FlinkSparkConf `json:"flinkSparkConf,omitempty"`

	Action pipelineyml.Action `json:"action,omitempty"`

	OpenapiOAuth2TokenPayload apistructs.OAuth2TokenPayload `json:"openapiOAuth2TokenPayload"`

	LoopOptions *apistructs.PipelineTaskLoopOptions `json:"loopOptions,omitempty"` // 开始执行后保证不为空

	AppliedResources apistructs.PipelineAppliedResources `json:"appliedResources,omitempty"`

	EncryptSecretKeys []string `json:"encryptSecretKeys"` // the encrypt envs' key list

	CurrentPolicy apistructs.Policy `json:"currentPolicy"` // task execution strategy

	ContainerInstanceProvider *apistructs.ContainerInstanceProvider `json:"containerInstanceProvider,omitempty"`
}

type FlinkSparkConf struct {
	// 该部分在 action 的 source 里声明
	Depend    string   `json:"depends,omitempty"`
	MainClass string   `json:"main_class,omitempty"`
	MainArgs  []string `json:"main_args,omitempty"`

	// flink/spark action 运行需要一个 jar resource（flink 为 jarID，spark 为 jarURL）
	// 该部分在运行期动态赋值
	JarResource string `json:"jarResource,omitempty"`
}

type PipelineTaskContext struct {
	InStorages  metadata.Metadata `json:"inStorages,omitempty"`
	OutStorages metadata.Metadata `json:"outStorages,omitempty"`

	CmsDiceFiles metadata.Metadata `json:"cmsDiceFiles,omitempty"`
}

func (c *PipelineTaskContext) Dedup() {
	c.InStorages = c.InStorages.DedupByName()
	c.OutStorages = c.OutStorages.DedupByName()
}

// GenerateOperation
type PipelineTaskOpType string

var (
	PipelineTaskOpTypeGet  PipelineTaskOpType = "get"
	PipelineTaskOpTypePut  PipelineTaskOpType = "put"
	PipelineTaskOpTypeTask PipelineTaskOpType = "task"
)

type PipelineTaskExecutorKind string

var (
	PipelineTaskExecutorKindScheduler PipelineTaskExecutorKind = "SCHEDULER"
	PipelineTaskExecutorKindMemory    PipelineTaskExecutorKind = "MEMORY"
	PipelineTaskExecutorKindAPITest   PipelineTaskExecutorKind = "APITEST"
	PipelineTaskExecutorKindWait      PipelineTaskExecutorKind = "WAIT"
	PipelineTaskExecutorKindK8sJob    PipelineTaskExecutorKind = "K8SJOB"
	PipelineTaskExecutorKindK8sFlink  PipelineTaskExecutorKind = "K8SFLINK"
	PipelineTaskExecutorKindK8sSpark  PipelineTaskExecutorKind = "K8SSPARK"
	PipelineTaskExecutorKindDocker    PipelineTaskExecutorKind = "DOCKER"
	PipelineTaskExecutorKindList                               = []PipelineTaskExecutorKind{PipelineTaskExecutorKindScheduler, PipelineTaskExecutorKindMemory, PipelineTaskExecutorKindAPITest, PipelineTaskExecutorKindWait, PipelineTaskExecutorKindK8sJob}
)

func (that PipelineTaskExecutorKind) Check() bool {
	for _, kind := range PipelineTaskExecutorKindList {
		if string(kind) == string(that) {
			return true
		}
	}
	return false
}

func (that PipelineTaskExecutorKind) IsK8sKind() bool {
	return that == PipelineTaskExecutorKindK8sJob || that == PipelineTaskExecutorKindK8sFlink || that == PipelineTaskExecutorKindK8sSpark
}

func (that PipelineTaskExecutorKind) String() string {
	return string(that)
}

func (that PipelineTaskExecutorKind) GenExecutorNameByClusterName(clusterName string) PipelineTaskExecutorName {
	return PipelineTaskExecutorName(fmt.Sprintf("%s-%s", that.GetDefaultExecutorName(), clusterName))
}

func (that PipelineTaskExecutorKind) GetDefaultExecutorName() PipelineTaskExecutorName {
	switch that {
	case PipelineTaskExecutorKindScheduler:
		return PipelineTaskExecutorNameSchedulerDefault
	case PipelineTaskExecutorKindAPITest:
		return PipelineTaskExecutorNameAPITestDefault
	case PipelineTaskExecutorKindWait:
		return PipelineTaskExecutorNameWaitDefault
	case PipelineTaskExecutorKindK8sJob:
		return PipelineTaskExecutorNameK8sJobDefault
	case PipelineTaskExecutorKindK8sFlink:
		return PipelineTaskExecutorNameK8sFlinkDefault
	case PipelineTaskExecutorKindK8sSpark:
		return PipelineTaskExecutorNameK8sSparkDefault
	}
	return PipelineTaskExecutorNameEmpty
}

type PipelineTaskExecutorName string

func (that PipelineTaskExecutorName) String() string {
	return string(that)
}

var (
	PipelineTaskExecutorNameEmpty            PipelineTaskExecutorName = ""
	PipelineTaskExecutorNameSchedulerDefault PipelineTaskExecutorName = "scheduler"
	PipelineTaskExecutorNameAPITestDefault   PipelineTaskExecutorName = "api-test"
	PipelineTaskExecutorNameWaitDefault      PipelineTaskExecutorName = "wait"
	PipelineTaskExecutorNameK8sJobDefault    PipelineTaskExecutorName = "k8s-job"
	PipelineTaskExecutorNameK8sFlinkDefault  PipelineTaskExecutorName = "k8s-flink"
	PipelineTaskExecutorNameK8sSparkDefault  PipelineTaskExecutorName = "k8s-spark"
	PipelineTaskExecutorNameDockerDefault    PipelineTaskExecutorName = "docker"
	PipelineTaskExecutorNameList                                      = []PipelineTaskExecutorName{PipelineTaskExecutorNameEmpty, PipelineTaskExecutorNameSchedulerDefault, PipelineTaskExecutorNameAPITestDefault, PipelineTaskExecutorNameWaitDefault, PipelineTaskExecutorNameK8sJobDefault}
)

func (that PipelineTaskExecutorName) Check() bool {
	for _, name := range PipelineTaskExecutorNameList {
		if string(name) == string(that) {
			return true
		}
	}
	return false
}

type RuntimeResource struct {
	CPU       float64 `json:"cpu"`
	Memory    float64 `json:"memory"`
	Disk      float64 `json:"disk"`
	MaxCPU    float64 `json:"maxCPU"`
	MaxMemory float64 `json:"maxMemory"`
}

type Volume struct {
	HostPath      string `json:"hostPath"`
	ContainerPath string `json:"containerPath"`
	ReadOnly      bool   `json:"readOnly"`
}

func (pt *PipelineTask) Convert2DTO() *apistructs.PipelineTaskDTO {
	if pt == nil {
		return nil
	}
	task := apistructs.PipelineTaskDTO{
		ID:         pt.ID,
		PipelineID: pt.PipelineID,
		StageID:    pt.StageID,
		Name:       pt.Name,
		OpType:     string(pt.OpType),
		Type:       string(pt.Type),
		Status:     pt.Status,
		Extra: apistructs.PipelineTaskExtra{
			UUID:           pt.Extra.UUID,
			AllowFailure:   pt.Extra.AllowFailure,
			TaskContainers: pt.Extra.TaskContainers,
		},
		Labels:       pt.Extra.Action.Labels,
		CostTimeSec:  pt.CostTimeSec,
		QueueTimeSec: pt.QueueTimeSec,
		TimeBegin:    pt.TimeBegin,
		TimeEnd:      pt.TimeEnd,
		TimeCreated:  pt.TimeCreated,
		TimeUpdated:  pt.TimeUpdated,

		IsSnippet:             pt.IsSnippet,
		SnippetPipelineID:     pt.SnippetPipelineID,
		SnippetPipelineDetail: pt.SnippetPipelineDetail,
	}
	task.Result.Metadata = pt.GetMetadata()
	task.Result.MachineStat = pt.Inspect.MachineStat
	task.Result.Inspect = pt.Inspect.Inspect
	task.Result.Events = pt.Inspect.Events
	task.Result.Errors = pt.MergeErrors()
	// handle metadata
	for _, field := range task.Result.Metadata {
		field.Level = field.GetLevel()
	}

	if task.Status.IsSuccessStatus() {
		task.Result.Errors = nil
		notErrorMeta, _ := task.Result.Metadata.FilterNoErrorLevel()
		task.Result.Metadata = notErrorMeta
	}

	if task.Type == "manual-review" {
		task.Status = task.Status.ChangeStateForManualReview()
	}

	return &task
}

func (pt *PipelineTask) RuntimeID() string {
	for _, meta := range pt.GetMetadata() {
		if meta.Type == apistructs.ActionCallbackTypeLink &&
			meta.Name == apistructs.ActionCallbackRuntimeID {
			return meta.Value
		}
	}
	return ""
}

func (pt *PipelineTask) ReleaseID() string {
	for _, meta := range pt.GetMetadata() {
		if meta.Type == apistructs.ActionCallbackTypeLink &&
			meta.Name == apistructs.ActionCallbackReleaseID {
			return meta.Value
		}
	}
	return ""
}

func (pt *PipelineTask) GetMetadata() metadata.Metadata {
	if pt.Result == nil {
		return metadata.Metadata{}
	}
	return pt.Result.Metadata
}

func (pt *PipelineTask) MergeErrors() taskerror.OrderedErrors {
	o := make(taskerror.OrderedErrors, 0)
	o = append(o, pt.Inspect.Errors...)
	if pt.Result != nil {
		o = append(o, pt.Result.Errors...)
	}
	o.ConvertErrors()
	return o
}

type ExecutorDoneChanData struct {
	Data    interface{}
	Version string
}

func (pt *PipelineTask) GenerateExecutorDoneChanDataVersion() string {
	if pt.Extra.LoopOptions == nil {
		return fmt.Sprintf("%s-%d", CtxExecutorChDataVersionPrefix, pt.ID)
	}
	return fmt.Sprintf("%s-%d-loop-%d", CtxExecutorChDataVersionPrefix, pt.ID, pt.Extra.LoopOptions.LoopedTimes)
}

func (pt *PipelineTask) CheckExecutorDoneChanDataVersion(actualVersion string) error {
	expectedVersion := pt.GenerateExecutorDoneChanDataVersion()
	if expectedVersion != actualVersion {
		return fmt.Errorf("executor data expected version: %s, actual version: %s", expectedVersion, actualVersion)
	}
	return nil
}

func GenDefaultTaskResource() RuntimeResource {
	return RuntimeResource{
		CPU:    conf.TaskDefaultCPU(),
		Memory: conf.TaskDefaultMEM(),
		Disk:   0,
	}
}

func MakeTaskExecutorCtxKey(task *PipelineTask) string {
	return fmt.Sprintf("%s-%d", CtxExecutorChKeyPrefix, task.ID)
}
