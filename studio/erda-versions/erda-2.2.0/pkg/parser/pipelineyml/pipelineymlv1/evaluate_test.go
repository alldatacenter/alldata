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

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/require"
)

func TestFindFirstPlaceholder(t *testing.T) {
	diceKey := "diceKey: ((dice.projectID)):((dice.env)):((dice.branch)):((dice.operatorID))"
	placeholder, find := findFirstPlaceholder(diceKey)
	require.True(t, find)
	require.Equal(t, "((dice.projectID))", placeholder)

	normal := "diceKey: 666:test:master:92"
	placeholder, find = findFirstPlaceholder(normal)
	require.False(t, find)

	odd := "diceKey: ))((1)):test:master:92"
	placeholder, find = findFirstPlaceholder(odd)
	require.True(t, find)
	require.Equal(t, "((1))", placeholder)
}

func TestPipelineYml_Evaluate(t *testing.T) {
	ymlContent :=
		`
version: 1.0
# ((noneed))
stages: # stage begin...
- name: deploy
  tasks:
    - put: ((put-resource-name))
      params:
        diceKey: ((dice.projectID)):((dice.env)):((dice.branch)):((dice.operatorID))

- name: bp-backend
  type: buildpack
  source:
    context: repo/services
    bp_repo: http://git.terminus.io/buildpacks/dice-bpack-termjava.git
    bp_ver: feature/new-bp
    modules:
    - name: galaxy-admin
      image: { name: ))()()((buildpack.image.galaxy-admin)))()()) }
    - name: galaxy-web
      image: { name: ((buildpack.image.galaxy-web)) }
    - name: galaxy-trade
      image: { name: ((buildpack.image.galaxy-trade)) }
    - name: galaxy-item
      image: { name: ((buildpack.image.galaxy-item)) }
    - name: galaxy-user
      image: { name: ((buildpack.image.galaxy-user)) }
`
	y := New([]byte(ymlContent))
	fmt.Println(string(y.byteData))
	variables := map[string]string{
		"((put-resource-name))":            "deploy",
		"((dice.projectID))":               "666",
		"((dice.env))":                     "test",
		"((dice.operatorID))":              "92",
		"((dice.branch))":                  "feature/pipeline",
		"((buildpack.image.galaxy-admin))": "localhost:5000/galaxy/galaxy-admin:v0.1",
		"((buildpack.image.galaxy-web))":   "localhost:5000/galaxy/galaxy-web:v0.1",
		"((buildpack.image.galaxy-trade))": "localhost:5000/galaxy/galaxy-trade:v0.1",
		"((buildpack.image.galaxy-item))":  "localhost:5000/galaxy/galaxy-item:v0.1",
		"((buildpack.image.galaxy-user))":  "localhost:5000/galaxy/galaxy-user:v0.1",
	}
	err := y.evaluate(Map2MetadataFields(variables))
	require.NoError(t, err)
	fmt.Println(string(y.byteData))
}

func TestRemoveComment(t *testing.T) {
	s := "((dice.id#))#"
	nocomment, comment := removeComment(s)
	require.Equal(t, "((dice.id", nocomment)
	require.Equal(t, "#))#", comment)
}

func TestRenderPlaceholders(t *testing.T) {
	s := "((dice.id)) . ((a)) #comment# ((noneed))"
	output, err := RenderPlaceholders(s, Map2MetadataFields(map[string]string{"((dice.id))": "id", "((a))": "a"}))
	require.NoError(t, err)
	fmt.Println(output)
}
