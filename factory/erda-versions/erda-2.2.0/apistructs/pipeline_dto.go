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

package apistructs

import (
	"time"

	"github.com/erda-project/erda-proto-go/core/pipeline/pb"
)

type PipelineDTO struct {
	// 应用相关信息
	ID              uint64  `json:"id,omitempty"`
	CronID          *uint64 `json:"cronID,omitempty"`
	OrgID           uint64  `json:"orgID,omitempty"`
	OrgName         string  `json:"orgName,omitempty"`
	ProjectID       uint64  `json:"projectID,omitempty"`
	ProjectName     string  `json:"projectName,omitempty"`
	ApplicationID   uint64  `json:"applicationID,omitempty"`
	ApplicationName string  `json:"applicationName,omitempty"`

	// 分支相关信息
	Branch       string            `json:"branch,omitempty"`
	Commit       string            `json:"commit,omitempty"`
	CommitDetail CommitDetail      `json:"commitDetail,omitempty" xorm:"json"`
	Labels       map[string]string `json:"labels,omitempty"`

	// pipeline.yml 相关信息
	Source     PipelineSource `json:"source,omitempty"`
	YmlSource  string         `json:"ymlSource,omitempty"` // yml 文件来源
	YmlName    string         `json:"ymlName,omitempty"`   // yml 文件名
	YmlNameV1  string         `json:"ymlNameV1,omitempty"`
	YmlContent string         `json:"ymlContent,omitempty"` // yml 文件内容
	Extra      PipelineExtra  `json:"extra,omitempty" xorm:"json"`

	// 运行时相关信息
	Namespace   string         `json:"namespace"`
	Type        string         `json:"type,omitempty"`
	TriggerMode string         `json:"triggerMode,omitempty"`
	ClusterName string         `json:"clusterName,omitempty"`
	Status      PipelineStatus `json:"status,omitempty"`
	Progress    float64        `json:"progress"` // pipeline 执行进度, eg: 0.8 即 80%

	// 时间
	CostTimeSec int64      `json:"costTimeSec,omitempty"`                // pipeline 总耗时/秒
	TimeBegin   *time.Time `json:"timeBegin,omitempty"`                  // 执行开始时间
	TimeEnd     *time.Time `json:"timeEnd,omitempty"`                    // 执行结束时间
	TimeCreated *time.Time `json:"timeCreated,omitempty" xorm:"created"` // 记录创建时间
	TimeUpdated *time.Time `json:"timeUpdated,omitempty" xorm:"updated"` // 记录更新时间
}

type CommitDetail struct {
	CommitID string     `json:"commitID,omitempty"`
	Repo     string     `json:"repo,omitempty"`
	RepoAbbr string     `json:"repoAbbr,omitempty"`
	Author   string     `json:"author,omitempty"`
	Email    string     `json:"email,omitempty"`
	Time     *time.Time `json:"time,omitempty"`
	Comment  string     `json:"comment,omitempty"`
}

type (
	PipelineExtra struct {
		DiceWorkspace          string        `json:"diceWorkspace,omitempty"`
		PipelineYmlNameV1      string        `json:"pipelineYmlNameV1,omitempty"`
		SubmitUser             *PipelineUser `json:"submitUser,omitempty"`
		RunUser                *PipelineUser `json:"runUser,omitempty"`
		CancelUser             *PipelineUser `json:"cancelUser,omitempty"`
		CronExpr               string        `json:"cronExpr,omitempty"`
		CronTriggerTime        *time.Time    `json:"cronTriggerTime,omitempty"` // 秒级精确，毫秒级误差请忽略，cron expr 精确度同样为秒级
		ShowMessage            *ShowMessage  `json:"showMessage,omitempty"`
		ConfigManageNamespaces []string      `json:"configmanageNamespaces,omitempty"`

		IsAutoRun bool `json:"isAutoRun,omitempty"` // 创建后是否自动开始执行

		CallbackURLs []string `json:"callbackURLs,omitempty"`
	}

	PipelineUser struct {
		ID     interface{} `json:"id,omitempty"`
		Name   string      `json:"name,omitempty"`
		Avatar string      `json:"avatar,omitempty"`
	}

	ShowMessage struct {
		Msg      string   `json:"msg"`
		Stacks   []string `json:"stacks"`
		AbortRun bool     `json:"abortRun"` // if false, canManualRun should be false
	}

	StorageConfig struct {
		EnableNFS   bool `json:"enableNfs"`
		EnableLocal bool `json:"enableLocal"`
	}
)

// whether to close the mounting of the network storage
// after closing, some special pipeline syntax ( ${{ dirs.xxx }} or old ${xxx} ) will not be available
func (conf StorageConfig) EnableNFSVolume() bool {
	return conf.EnableNFS
}

// whether to open shared storage
// after open, the context directory in the pipeline will be shared
func (conf *StorageConfig) EnableShareVolume() bool {
	return conf.EnableLocal
}

// PipelineDetailDTO contains pipeline, stages, tasks and others
type PipelineDetailDTO struct {
	PipelineDTO
	PipelineStages        []PipelineStageDetailDTO `json:"pipelineStages"`
	PipelineSnippetStages []PipelineStageDetailDTO `json:"pipelineSnippetStages"`
	PipelineCron          *pb.Cron                 `json:"pipelineCron"`

	// 按钮
	PipelineButton PipelineButton `json:"pipelineButton"`
	// task 的 action 详情
	PipelineTaskActionDetails map[string]PipelineTaskActionDetail `json:"pipelineTaskActionDetails"`

	RunParams []PipelineParamDTO `json:"runParams"`

	Events []*PipelineEvent `json:"events,omitempty"`

	NeedApproval bool `json:"needApproval"`
}

type PipelineParamDTO struct {
	PipelineParam
	Value interface{} `json:"value,omitempty"`
}

type PipelineTaskActionDetail struct {
	LogoUrl     string `json:"logoUrl"`
	DisplayName string `json:"displayName"`
	Description string `json:"description"`
}

type PipelineButton struct {
	// 手动开始
	CanManualRun bool `json:"canManualRun"`

	// 取消
	CanCancel      bool `json:"canCancel"`
	CanForceCancel bool `json:"canForceCancel"`

	// 重试
	CanRerun       bool `json:"canRerun"`
	CanRerunFailed bool `json:"canRerunFailed"`

	// 定时
	CanStartCron bool `json:"canStartCron"`
	CanStopCron  bool `json:"canStopCron"`

	// TODO 暂停
	CanPause   bool `json:"canPause"`
	CanUnpause bool `json:"canUnpause"`

	// 删除
	CanDelete bool `json:"canDelete"`
}

type PipelineExecuteRecord struct {
	PipelineID  uint64     `json:"pipelineID"`
	Status      string     `json:"status"`
	TriggerMode string     `json:"triggerMode"`
	TimeCreated time.Time  `json:"timeCreated"`
	TimeBegin   *time.Time `json:"timeBegin"`
	TimeEnd     *time.Time `json:"timeEnd"`
}

type PipelineStageDetailDTO struct {
	PipelineStageDTO
	PipelineTasks []PipelineTaskDTO `json:"pipelineTasks"`
}

func (user *UserInfo) ConvertToPipelineUser() *PipelineUser {

	var name = user.Nick
	if len(name) <= 0 {
		name = user.Name
	}

	return &PipelineUser{
		ID:     user.ID,
		Name:   name,
		Avatar: user.Avatar,
	}
}
