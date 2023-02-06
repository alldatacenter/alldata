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

package pipelineyml

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"reflect"
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
)

func TestConvertGraphPipelineYml(t *testing.T) {
	fb, err := ioutil.ReadFile("./samples/graph_pipelineyml.yaml")
	assert.NoError(t, err)
	b, err := ConvertGraphPipelineYmlContent(fb)
	assert.NoError(t, err)
	fmt.Println(string(b))
}

func TestConvertToGraphPipelineYml(t *testing.T) {
	fb, err := ioutil.ReadFile("./samples/pipeline_cicd.yml")
	assert.NoError(t, err)
	graph, err := ConvertToGraphPipelineYml(fb)
	assert.NoError(t, err)
	b, err := json.MarshalIndent(graph, "", "  ")
	assert.NoError(t, err)
	fmt.Println(string(b))
}

func Test_cronCompensatorReset(t *testing.T) {
	type args struct {
		cronCompensator *apistructs.CronCompensator
	}
	tests := []struct {
		name string
		args args
		want *apistructs.CronCompensator
	}{
		{
			name: "test_nil",
			args: args{
				cronCompensator: nil,
			},
			want: nil,
		},
		{
			name: "test_default",
			args: args{
				cronCompensator: &apistructs.CronCompensator{
					Enable:               DefaultCronCompensator.Enable,
					LatestFirst:          DefaultCronCompensator.LatestFirst,
					StopIfLatterExecuted: DefaultCronCompensator.StopIfLatterExecuted,
				},
			},
			want: nil,
		},
		{
			name: "test_other",
			args: args{
				cronCompensator: &apistructs.CronCompensator{
					Enable:               true,
					LatestFirst:          false,
					StopIfLatterExecuted: true,
				},
			},
			want: &apistructs.CronCompensator{
				Enable:               true,
				LatestFirst:          false,
				StopIfLatterExecuted: true,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := cronCompensatorReset(tt.args.cronCompensator); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("cronCompensatorReset() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestTest(t *testing.T) {
	b, err := ioutil.ReadFile("./samples/trigger.yml")
	assert.NoError(t, err)
	newYml, err := ConvertToGraphPipelineYml(b)
	assert.NoError(t, err)
	assert.Contains(t, newYml.YmlContent, "trigger")
	//n := &apistructs.PipelineYml{}
	//assert.EqualValues(t, newYml.On, n.On)
	fmt.Println(newYml.YmlContent)
}
