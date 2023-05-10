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
	"fmt"
	"io/ioutil"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestOn(t *testing.T) {
	b, err := ioutil.ReadFile("./samples/on.yml")
	assert.NoError(t, err)
	newYml, err := ConvertToGraphPipelineYml(b)
	assert.NoError(t, err)
	assert.Contains(t, newYml.YmlContent, "push")
	//n := &apistructs.PipelineYml{}
	//assert.EqualValues(t, newYml.On, n.On)
	fmt.Println(newYml.YmlContent)
}
