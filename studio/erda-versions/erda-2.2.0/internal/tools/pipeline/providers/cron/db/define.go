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

package db

import (
	"strconv"
	"time"

	"google.golang.org/protobuf/types/known/timestamppb"
	"google.golang.org/protobuf/types/known/wrapperspb"

	"github.com/erda-project/erda-proto-go/core/pipeline/pb"
	"github.com/erda-project/erda/apistructs"
)

const (
	PipelineCronCronExpr = "cron_expr"
	PipelineDefinitionID = "pipeline_definition_id"
	PipelineCronEnable   = "enable"
	Extra                = "extra"
	PipelineCronIsEdge   = "is_edge"
)

type PipelineCron struct {
	ID          uint64    `json:"id" xorm:"pk autoincr"`
	TimeCreated time.Time `json:"timeCreated" xorm:"created"` // 记录创建时间
	TimeUpdated time.Time `json:"timeUpdated" xorm:"updated"` // 记录更新时间

	PipelineSource  apistructs.PipelineSource `json:"pipelineSource"`
	PipelineYmlName string                    `json:"pipelineYmlName"`

	CronExpr string `json:"cronExpr"`
	//PipelineSource  string            `json:"pipelineSource"`
	Enable *bool             `json:"enable"` // 1 true, 0 false
	Extra  PipelineCronExtra `json:"extra,omitempty" xorm:"json"`

	// Deprecated
	ApplicationID uint64 `json:"applicationID" xorm:"application_id"`
	// Deprecated
	Branch string `json:"branch"`
	// Deprecated
	BasePipelineID uint64 `json:"basePipelineID"` // 用于记录最开始创建出这条 cron 记录的 pipeline id
	// definition id
	PipelineDefinitionID string `json:"pipelineDefinitionID"`
	// cluster name
	ClusterName string `json:"clusterName" xorm:"cluster_name"`

	IsEdge *bool `json:"is_edge"`
}

func (pc PipelineCron) GetIsEdge() bool {
	if pc.IsEdge == nil {
		return false
	}
	return *pc.IsEdge
}

// PipelineCronExtra cron 扩展信息, 不参与过滤
type PipelineCronExtra struct {
	PipelineYml            string            `json:"pipelineYml"`
	ClusterName            string            `json:"clusterName"`
	FilterLabels           map[string]string `json:"labels"`
	NormalLabels           map[string]string `json:"normalLabels"` // userID 存储提交流水线的用户 ID
	Envs                   map[string]string `json:"envs"`
	ConfigManageNamespaces []string          `json:"configManageNamespaces,omitempty"`
	IncomingSecrets        map[string]string `json:"incomingSecrets,omitempty"`
	CronStartFrom          *time.Time        `json:"cronStartFrom,omitempty"`
	// 新版为 v2
	Version string `json:"version"`

	// compensate
	// Compensator 老的 cron 为空，经过补偿后会自动赋值默认配置；新创建的 cron 一定会有值。
	Compensator *apistructs.CronCompensator `json:"compensator,omitempty"`
	//每次中断补偿执行的时间，下次中断补偿从这个时间开始查询
	LastCompensateAt *time.Time `json:"lastCompensateAt,omitempty"`
}

func (pc *PipelineCron) GenCompensateCreatePipelineReqNormalLabels(triggerTime time.Time) map[string]string {
	normalLabels := make(map[string]string)
	for k, v := range pc.Extra.NormalLabels {
		normalLabels[k] = v
	}
	normalLabels[apistructs.LabelPipelineTriggerMode] = apistructs.PipelineTriggerModeCron.String()
	normalLabels[apistructs.LabelPipelineType] = apistructs.PipelineTypeNormal.String()
	normalLabels[apistructs.LabelPipelineYmlSource] = apistructs.PipelineYmlSourceContent.String()
	normalLabels[apistructs.LabelPipelineCronTriggerTime] = strconv.FormatInt(triggerTime.UnixNano(), 10)
	normalLabels[apistructs.LabelPipelineCronID] = strconv.FormatUint(pc.ID, 10)
	return normalLabels
}

func (pc *PipelineCron) GenCompensateCreatePipelineReqFilterLabels() map[string]string {
	filterLabels := make(map[string]string)
	for k, v := range pc.Extra.FilterLabels {
		filterLabels[k] = v
	}
	if _, ok := filterLabels[apistructs.LabelPipelineTriggerMode]; ok {
		filterLabels[apistructs.LabelPipelineTriggerMode] = apistructs.PipelineTriggerModeCron.String()
	}
	filterLabels[apistructs.LabelPipelineCronCompensated] = "true"
	return filterLabels
}

func (pc *PipelineCron) Convert2DTO() *pb.Cron {
	if pc == nil {
		return nil
	}

	result := &pb.Cron{
		ID:                     pc.ID,
		TimeCreated:            timestamppb.New(pc.TimeCreated),
		TimeUpdated:            timestamppb.New(pc.TimeUpdated),
		ApplicationID:          pc.ApplicationID,
		Branch:                 pc.Branch,
		CronExpr:               pc.CronExpr,
		PipelineYmlName:        pc.PipelineYmlName,
		BasePipelineID:         pc.BasePipelineID,
		PipelineYml:            pc.Extra.PipelineYml,
		ConfigManageNamespaces: pc.Extra.ConfigManageNamespaces,
		Secrets:                pc.Extra.IncomingSecrets,
		UserID:                 pc.GetUserID(),
		OrgID:                  pc.GetOrgID(),
		PipelineDefinitionID:   pc.PipelineDefinitionID,
		PipelineSource:         pc.PipelineSource.String(),
		IsEdge:                 wrapperspb.Bool(pc.GetIsEdge()),
		ClusterName:            pc.ClusterName,
	}

	extra := &pb.CronExtra{
		PipelineYml:            pc.Extra.PipelineYml,
		ClusterName:            pc.Extra.ClusterName,
		Labels:                 pc.Extra.FilterLabels,
		NormalLabels:           pc.Extra.NormalLabels,
		Envs:                   pc.Extra.Envs,
		ConfigManageNamespaces: pc.Extra.ConfigManageNamespaces,
		IncomingSecrets:        pc.Extra.IncomingSecrets,
		CronStartFrom: func() *timestamppb.Timestamp {
			if pc.Extra.CronStartFrom == nil {
				return nil
			}
			return timestamppb.New(*pc.Extra.CronStartFrom)
		}(),
		Version: pc.Extra.Version,
		Compensator: func() *pb.CronCompensator {
			if pc.Extra.Compensator == nil {
				return nil
			}
			return &pb.CronCompensator{
				Enable:               wrapperspb.Bool(pc.Extra.Compensator.Enable),
				LatestFirst:          wrapperspb.Bool(pc.Extra.Compensator.LatestFirst),
				StopIfLatterExecuted: wrapperspb.Bool(pc.Extra.Compensator.StopIfLatterExecuted),
			}
		}(),
		LastCompensateAt: func() *timestamppb.Timestamp {
			if pc.Extra.LastCompensateAt == nil {
				return nil
			}
			return timestamppb.New(*pc.Extra.LastCompensateAt)
		}(),
	}
	result.Extra = extra

	if pc.Extra.CronStartFrom != nil {
		result.CronStartTime = timestamppb.New(*pc.Extra.CronStartFrom)
	}
	if pc.Enable != nil {
		result.Enable = wrapperspb.Bool(*pc.Enable)
	}
	return result
}

// GetUserID if user is empty, means it doesn't exist
func (pc *PipelineCron) GetUserID() string {
	userID := pc.Extra.FilterLabels[apistructs.LabelUserID]
	if userID != "" {
		return userID
	}
	return pc.Extra.NormalLabels[apistructs.LabelUserID]
}

// GetOrgID if org is 0, means it doesn't exist
func (pc *PipelineCron) GetOrgID() uint64 {
	orgIDStr := pc.Extra.FilterLabels[apistructs.LabelOrgID]
	if orgIDStr == "" {
		orgIDStr = pc.Extra.NormalLabels[apistructs.LabelOrgID]
	}
	orgID, _ := strconv.ParseUint(orgIDStr, 10, 64)
	return orgID
}

func (PipelineCron) TableName() string {
	return "pipeline_crons"
}
