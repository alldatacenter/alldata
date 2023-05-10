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

// MigrationStatus addon规格信息返回res
type MigrationStatusDesc struct {
	// Status 返回的运行状态
	Status StatusCode `json:"status"`
	// Desc 说明信息
	Desc string `json:"desc"`
}

const (
	// MigrationStatusInit migration初始化
	MigrationStatusInit string = "INIT"
	// MigrationStatusPending migration等待
	MigrationStatusPending string = "PENDING"
	// MigrationStatusRunning migration running
	MigrationStatusRunning string = "RUNNING"
	// MigrationStatusFail migration 失败
	MigrationStatusFail string = "FAIL"
	// MigrationStatusFinish migration 完成
	MigrationStatusFinish string = "FINISH"
	// MigrationStatusDeleted migration 删除
	MigrationStatusDeleted string = "DELETE"
)
