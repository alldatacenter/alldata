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

	"github.com/erda-project/erda-proto-go/core/pipeline/action/pb"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/taskerror"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/taskinspect"
	"github.com/erda-project/erda/pkg/metadata"
)

const (
	EnvOpenapiTokenForActionBootstrap = "DICE_OPENAPI_TOKEN_FOR_ACTION_BOOTSTRAP"
	EnvOpenapiToken                   = "DICE_OPENAPI_TOKEN"
)

type ActionCallback struct {
	// show in stdout
	Metadata metadata.Metadata `json:"metadata"`

	Errors taskerror.OrderedErrors `json:"errors"`

	// machine stat
	MachineStat *taskinspect.PipelineTaskMachineStat `json:"machineStat,omitempty"`

	// behind
	PipelineID     uint64 `json:"pipelineID"`
	PipelineTaskID uint64 `json:"pipelineTaskID"`
}

const (
	ActionCallbackTypeLink             = "link"
	ActionCallbackRuntimeID            = "runtimeID"
	ActionCallbackOperatorID           = "operatorID"
	ActionCallbackReleaseID            = "releaseID"
	ActionCallbackPublisherID          = "publisherID"
	ActionCallbackPublishItemID        = "publishItemID"
	ActionCallbackPublishItemVersionID = "publishItemVersionID"
	ActionCallbackQaID                 = "qaID"
)

// detail
type ActionDetailResponse struct {
	Header
	Data interface{} `json:"data"`
}

// list
type ActionListResponse struct {
	Header
	Data interface{} `json:"data"`
}

type ActionCreateResponse struct {
	Header
	Data *ActionItem `json:"data"`
}

type ActionQueryResponse struct {
	Header
	Data []*ActionItem `json:"data"`
}

type ActionCreateRequest struct {
	// action名
	Name string `json:"name"`

	// action版本
	Version string `json:"version" `

	// spec yml 内容
	SpecSrc string `json:"specSrc" `

	// 源action镜像地址
	ImageSrc string `json:"imageSrc"`
}

type ActionSetStatusResponse struct {
	Header
}

type ActionItem struct {
	Name      string    `json:"name"`
	Type      string    `json:"type"`
	Version   string    `json:"version"`
	SpecSrc   string    `json:"specSrc"`
	Spec      string    `json:"spec"`
	ImageSrc  string    `json:"imageSrc"`
	Image     string    `json:"image"`
	IsDefault int       `json:"isDefault"`
	Desc      string    `json:"desc"`
	CreatedAt time.Time `json:"createdAt"`
}

type PipelineActionSaveResponse struct {
	Header
	Action *pb.Action
}

type PipelineActionDeleteResponse struct {
	Header
}
