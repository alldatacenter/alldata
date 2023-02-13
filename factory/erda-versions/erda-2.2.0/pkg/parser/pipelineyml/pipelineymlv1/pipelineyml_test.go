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

package pipelineymlv1

//import (
//	"fmt"
//	"io/ioutil"
//	"testing"
//
//	"github.com/stretchr/testify/require"
//
//	"github.com/erda-project/erda/apistructs"
//)
//
//func TestPipelineYml_Unmarshal(t *testing.T) {
//	b, err := ioutil.ReadFile("../pipeline-anchor.yml")
//	require.NoError(t, err)
//	y := New(b)
//	err = y.Parse()
//	require.NoError(t, err)
//}
//
//func TestPipelineYml_Parse(t *testing.T) {
//	b, err := ioutil.ReadFile("../pipeline-get-put.yml")
//	require.NoError(t, err)
//	y := New(b)
//	err = y.Parse()
//	require.NoError(t, err)
//
//	yamlWithUnknownFields := []byte(
//		`version: '1.0'
//
//stages:
//- name: stage-test
//  source:
//    context: repo/ui
//`)
//	y = New(yamlWithUnknownFields)
//	err = y.Parse()
//	require.Error(t, err)
//}
//
//func TestPipelineYml_Triggers(t *testing.T) {
//	b, err := ioutil.ReadFile("../pipeline-trigger.yml")
//	require.NoError(t, err)
//
//	y := New(b)
//	err = y.Parse(WithNFSRealPath("/"))
//	require.NoError(t, err)
//	fmt.Printf("%#v\n", y.obj.Triggers)
//}
//
//func TestPipelinYmlDuplicate(t *testing.T) {
//	b, err := ioutil.ReadFile("../pipeline-duplicate.yml")
//	require.NoError(t, err)
//
//	y := New(b)
//	err = y.Parse()
//	require.NoError(t, err)
//
//	fmt.Println(y.YAML())
//}
//
//func TestPipelinYmlErrTasktype(t *testing.T) {
//	b, err := ioutil.ReadFile("../pipeline-err-tasktype.yml")
//	require.NoError(t, err)
//
//	y := New(b)
//	err = y.Parse()
//	require.NoError(t, err)
//
//	fmt.Println(y.YAML())
//}
//
//func TestPipelineYml_ValidateSingleTaskConfig(t *testing.T) {
//	b, err := ioutil.ReadFile("../pipeline-decode.yml")
//	require.NoError(t, err)
//
//	y := New(b)
//	err = y.Parse()
//	require.NoError(t, err)
//
//	fmt.Println(y.YAML())
//}
//
//func TestPipelineYml_GenerateTemplateEnvs(t *testing.T) {
//	y := New([]byte("version: '1.0'"))
//	err := y.Parse(WithPlaceholders([]metadata.MetadataField{{Name: "A", Value: "B"}, {Name: "C", Value: "D"}}))
//	require.NoError(t, err)
//	require.True(t, len(y.metadata.PlaceHolderEnvs) == 2)
//}
