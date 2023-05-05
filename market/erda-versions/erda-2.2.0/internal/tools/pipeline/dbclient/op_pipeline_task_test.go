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

package dbclient

//import (
//	"fmt"
//	"testing"
//
//	"github.com/stretchr/testify/require"
//
//	"github.com/erda-project/erda/apistructs"
//)
//
//func TestClient_UpdatePipelineTask(t *testing.T) {
//	task, err := client.GetPipelineTask(1)
//	require.NoError(t, err)
//	fmt.Println(task.CostTimeSec)
//	fmt.Println(task.Status)
//
//	task.CostTimeSec = 100
//	task.QueueTimeSec = 20
//	task.Extra.Namespace = "xxx-xxx"
//	task.Status = apistructs.PipelineStatusFailed
//	err = client.UpdatePipelineTask(task.ID, &task)
//	require.NoError(t, err)
//	require.True(t, task.Status == apistructs.PipelineStatusFailed)
//	require.True(t, task.CostTimeSec == 100)
//	require.True(t, task.QueueTimeSec == 20)
//	require.True(t, task.Extra.Namespace == "xxx-xxx")
//}
