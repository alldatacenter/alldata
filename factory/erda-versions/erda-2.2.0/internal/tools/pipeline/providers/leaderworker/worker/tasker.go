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

package worker

// LogicTask is the logic task for worker to process.
type LogicTask interface {
	GetLogicID() LogicTaskID
	GetData() []byte
}

type defaultTask struct {
	logicID LogicTaskID
	data    []byte
}

func NewLogicTask(logicID LogicTaskID, data []byte) LogicTask {
	return &defaultTask{logicID: logicID, data: data}
}
func (d *defaultTask) GetLogicID() LogicTaskID { return d.logicID }
func (d *defaultTask) GetData() []byte         { return d.data }
