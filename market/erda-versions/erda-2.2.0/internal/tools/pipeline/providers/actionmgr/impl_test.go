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

package actionmgr

import (
	"reflect"
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/edgepipeline_register"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
	"github.com/erda-project/erda/pkg/strutil"
)

func TestMakeActionTypeVersion(t *testing.T) {
	p := &provider{}
	item := p.MakeActionTypeVersion(&pipelineyml.Action{Type: "git", Version: "1.0"})
	assert.Equal(t, item, "git@1.0")

	item = p.MakeActionTypeVersion(&pipelineyml.Action{Type: "git"})
	assert.Equal(t, item, "git")
}

func Test_provider_MakeActionLocationsBySource(t *testing.T) {
	p := &provider{}

	type args struct {
		inputSource               apistructs.PipelineSource
		expectedOutputLocationNum int
		expectedOutputLocations   []string
	}

	cases := []args{
		// fdp
		{
			inputSource:               apistructs.PipelineSourceCDPDev,
			expectedOutputLocationNum: 2,
			expectedOutputLocations:   []string{apistructs.PipelineTypeFDP.String() + "/", apistructs.PipelineTypeDefault.String() + "/"},
		},
		{
			inputSource:               apistructs.PipelineSourceCDPTest,
			expectedOutputLocationNum: 2,
			expectedOutputLocations:   []string{apistructs.PipelineTypeFDP.String() + "/", apistructs.PipelineTypeDefault.String() + "/"},
		},
		{
			inputSource:               apistructs.PipelineSourceCDPStaging,
			expectedOutputLocationNum: 2,
			expectedOutputLocations:   []string{apistructs.PipelineTypeFDP.String() + "/", apistructs.PipelineTypeDefault.String() + "/"},
		},
		{
			inputSource:               apistructs.PipelineSourceCDPProd,
			expectedOutputLocationNum: 2,
			expectedOutputLocations:   []string{apistructs.PipelineTypeFDP.String() + "/", apistructs.PipelineTypeDefault.String() + "/"},
		},
		{
			inputSource:               apistructs.PipelineSourceBigData,
			expectedOutputLocationNum: 2,
			expectedOutputLocations:   []string{apistructs.PipelineTypeFDP.String() + "/", apistructs.PipelineTypeDefault.String() + "/"},
		},
		// cicd
		{
			inputSource:               apistructs.PipelineSourceDice,
			expectedOutputLocationNum: 2,
			expectedOutputLocations:   []string{apistructs.PipelineTypeCICD.String() + "/", apistructs.PipelineTypeDefault.String() + "/"},
		},
		{
			inputSource:               apistructs.PipelineSourceProject,
			expectedOutputLocationNum: 2,
			expectedOutputLocations:   []string{apistructs.PipelineTypeCICD.String() + "/", apistructs.PipelineTypeDefault.String() + "/"},
		},
		{
			inputSource:               apistructs.PipelineSourceProjectLocal,
			expectedOutputLocationNum: 2,
			expectedOutputLocations:   []string{apistructs.PipelineTypeCICD.String() + "/", apistructs.PipelineTypeDefault.String() + "/"},
		},
		{
			inputSource:               apistructs.PipelineSourceOps,
			expectedOutputLocationNum: 2,
			expectedOutputLocations:   []string{apistructs.PipelineTypeCICD.String() + "/", apistructs.PipelineTypeDefault.String() + "/"},
		},
		{
			inputSource:               apistructs.PipelineSourceQA,
			expectedOutputLocationNum: 2,
			expectedOutputLocations:   []string{apistructs.PipelineTypeCICD.String() + "/", apistructs.PipelineTypeDefault.String() + "/"},
		},
		// default
		{
			inputSource:               "unknown",
			expectedOutputLocationNum: 1,
			expectedOutputLocations:   []string{apistructs.PipelineTypeDefault.String() + "/"},
		},
		{
			inputSource:               "",
			expectedOutputLocationNum: 1,
			expectedOutputLocations:   []string{apistructs.PipelineTypeDefault.String() + "/"},
		},
	}

	for _, c := range cases {
		locations := p.MakeActionLocationsBySource(c.inputSource)
		if len(locations) != c.expectedOutputLocationNum {
			t.Fatalf("location num mismatch, actual: %d, expected: %d", len(locations), c.expectedOutputLocationNum)
		}
		for _, el := range c.expectedOutputLocations {
			if !strutil.Exist(c.expectedOutputLocations, el) {
				t.Fatalf("missing expected output location %s", el)
			}
		}
	}
}

func Test_provider_searchFromDiceHub(t *testing.T) {
	type args struct {
		notFindNameVersion []string
	}
	tests := []struct {
		name string
		args args
		want map[string]apistructs.ExtensionVersion
	}{
		{
			name: "test is edge return",
			args: args{
				notFindNameVersion: []string{"custom"},
			},
			want: map[string]apistructs.ExtensionVersion{},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &provider{}
			s.EdgeRegister = &edgepipeline_register.MockEdgeRegister{}
			if got := s.searchFromDiceHub(tt.args.notFindNameVersion); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("searchFromDiceHub() = %v, want %v", got, tt.want)
			}
		})
	}
}
