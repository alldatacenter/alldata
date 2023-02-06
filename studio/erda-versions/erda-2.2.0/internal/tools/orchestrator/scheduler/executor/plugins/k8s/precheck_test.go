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

package k8s

import (
	"testing"

	"github.com/stretchr/testify/assert"
	v1 "k8s.io/api/core/v1"
)

func Test_matchNodeLabels(t *testing.T) {
	r1 := matchNodeLabels([]string{"a", "b", "c"},
		[][2][]string{
			{{"b", "c"}, {"a"}}, // false
			{{"a", "b"}, {}},    // true
		})
	if !r1 {
		t.Errorf("matchNodeLabels r1 failed")
	}

	r2 := matchNodeLabels([]string{"a", "b", "c"},
		[][2][]string{
			{{"b", "c"}, {"a"}}, // false
			{{}, {"c"}},         // false
		})
	if r2 {
		t.Errorf("matchNodeLabels r2 failed")
	}

	r3 := matchNodeLabels([]string{"a", "b", "c"},
		[][2][]string{
			{{"b"}, {"a"}}, // false
			{{"a"}, {"c"}}, // false
			{{}, {}},       // true
		})
	if !r3 {
		t.Errorf("matchNodeLabels r3 failed")
	}
}

func Test_extractLabels(t *testing.T) {
	terms := []v1.NodeSelectorTerm{
		{
			MatchExpressions: []v1.NodeSelectorRequirement{
				{Key: "a", Operator: "Exists"},
				{Key: "b", Operator: "Exists"},
				{Key: "c", Operator: "DoesNotExist"},
			},
		},
		{
			MatchExpressions: []v1.NodeSelectorRequirement{
				{Key: "d", Operator: "Exists"},
				{Key: "e", Operator: "DoesNotExist"},
			},
		},
	}

	r := extractLabels(terms)
	assert.True(t, len(r) == 2)
	assert.True(t, len(r[0][0]) == 2)
	assert.True(t, len(r[0][1]) == 1)
	assert.True(t, len(r[1][0]) == 1)
	assert.True(t, len(r[1][1]) == 1)

}
