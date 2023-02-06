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

import "time"

type DashboardCreateRequest struct {
	// 布局相关json
	Layout string `json:"layout"`

	// 绘制相关json
	DrawerInfoMap string `json:"drawerInfoMap"`
}

type DashboardCreateResponse struct {
	Header
	Data uint64 `json:"data"`
}

type DashboardDetailRequest struct {
	// 配置id
	Id uint64 `path:"id"`
}

type DashboardDetailResponse struct {
	Header
	Data DashBoardDTO `json:"data"`
}

type DashboardListResponse struct {
	Header
	Data []DashBoardDTO `json:"data"`
}

type DashBoardDTO struct {
	// 记录主键id
	Id uint64 `json:"id"`

	// 唯一标识
	UniqueId string `json:"uniqueId"`

	// 绘制信息
	DrawerInfoMap string `json:"drawerInfoMap"`

	// 布局信息
	Layout string `json:"layout"`
}

type DashboardSpotLogLine struct {
	ID        string `json:"id"`
	Source    string `json:"source"`
	Stream    string `json:"stream"`
	TimeStamp string `json:"timestamp"`
	Content   string `json:"content"`
	Offset    int64  `json:"offset"`
	Level     string `json:"level"`
	RequestID string `json:"requestId"`
}

type DashboardSpotLogData struct {
	Lines []DashboardSpotLogLine `json:"lines"`
}

type DashboardSpotLogResponse struct {
	Header
	Data DashboardSpotLogData `json:"data"`
}

type DashboardSpotLogRequest struct {
	ID     string
	Source DashboardSpotLogSource
	Stream DashboardSpotLogStream
	Count  int64
	Start  time.Duration // 纳秒
	End    time.Duration // 纳秒
	Debug  bool
}

type DashboardSpotLogStream string

var (
	DashboardSpotLogStreamStdout DashboardSpotLogStream = "stdout"
	DashboardSpotLogStreamStderr DashboardSpotLogStream = "stderr"
)

type DashboardSpotLogSource string

var (
	DashboardSpotLogSourceJob       DashboardSpotLogSource = "job"
	DashboardSpotLogSourceContainer DashboardSpotLogSource = "container"
	DashboardSpotLogSourceDeploy    DashboardSpotLogSource = "deploy"
)
