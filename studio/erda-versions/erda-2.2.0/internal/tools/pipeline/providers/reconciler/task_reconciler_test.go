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

package reconciler

import (
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/taskresult"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/metadata"
)

func Test_overwriteTaskWithLatest(t *testing.T) {
	client := &dbclient.Client{}
	pm1 := monkey.PatchInstanceMethod(reflect.TypeOf(client), "GetPipelineTask", func(_ *dbclient.Client, id interface{}) (spec.PipelineTask, error) {
		return spec.PipelineTask{
			ID: 1,
			Result: &taskresult.Result{
				Metadata: metadata.Metadata{
					{
						Name:  "result",
						Value: "success",
					},
				},
			},
		}, nil
	})
	defer pm1.Unpatch()
	tr := &defaultTaskReconciler{
		dbClient: client,
	}
	task := &spec.PipelineTask{
		ID: 1,
	}
	err := tr.overwriteTaskWithLatest(task)
	assert.NoError(t, err)
	assert.Equal(t, "success", task.Result.Metadata[0].Value)
}
