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

package precheck

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/precheck/prechecktype"
)

func TestPrecheck(t *testing.T) {
	ctx := prechecktype.InitContext()
	yamlByte := []byte(`
version: 1.1
stages:
- stage:
  - release:
      params:
        cross_cluster: false
`)
	items := prechecktype.ItemsForCheck{
		PipelineYml: "",
		Files: map[string]string{
			"dice.yml": `
version: "2.0"
services:
  java-demo:
    ports:
      - 8080
    expose:
      - 8080
    resources:
      cpu: 0.2
      mem: 512
    deployments:
      replicas: 1
`,
		},
		ActionSpecs: map[string]apistructs.ActionSpec{
			"release": {
				Params: []apistructs.ActionSpecParam{
					{
						Name:     "cross_cluster",
						Required: false,
						Default:  true,
					},
				},
			},
		},
	}
	_, _ = PreCheck(ctx, yamlByte, items)
	assert.False(t, prechecktype.GetContextResult(ctx, prechecktype.CtxResultKeyCrossCluster).(bool))
}
