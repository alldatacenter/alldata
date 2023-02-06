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

package pipelinesvc

import (
	"testing"

	"bou.ke/monkey"
	"github.com/alecthomas/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/encoding/jsonpath"
)

func Test_handlerActionOutputsWithJq(t *testing.T) {
	var table = []struct {
		action  *apistructs.PipelineYmlAction
		outputs map[string]interface{}
	}{
		{
			action: &apistructs.PipelineYmlAction{},
			outputs: map[string]interface{}{
				"name":  "",
				"name1": "",
				"key1":  "",
			},
		},
		{
			action:  &apistructs.PipelineYmlAction{},
			outputs: map[string]interface{}{},
		},
		{
			action:  &apistructs.PipelineYmlAction{},
			outputs: nil,
		},
	}

	for _, data := range table {

		monkey.Patch(jsonpath.JQ, func(jsonInput, filter string) (interface{}, error) {
			var outputs []string
			for key, _ := range data.outputs {
				outputs = append(outputs, key)
			}
			return outputs, nil
		})

		data.action.Params = data.outputs
		outputs, err := handlerActionOutputsWithJq(data.action, "test")
		assert.NoError(t, err)

		assert.Equal(t, len(outputs), len(data.outputs))
		for _, output := range outputs {
			_, ok := data.outputs[output]
			assert.True(t, ok)
		}
	}
}
