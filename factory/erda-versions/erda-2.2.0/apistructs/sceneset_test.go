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

package apistructs

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"k8s.io/apimachinery/pkg/util/rand"
)

func TestSceneSetRequestValidate(t *testing.T) {
	tt := []struct {
		req  SceneSetRequest
		want bool
	}{
		{SceneSetRequest{Name: rand.String(50), Description: "1"}, true},
		{SceneSetRequest{Name: rand.String(51), Description: "1"}, false},
		{SceneSetRequest{Name: "1", Description: rand.String(255)}, true},
		{SceneSetRequest{Name: "1", Description: rand.String(256)}, false},
		{SceneSetRequest{Name: "****", Description: "1"}, false},
		{SceneSetRequest{Name: "/", Description: "1"}, false},
		{SceneSetRequest{Name: "_abd1", Description: "1"}, true},
		{SceneSetRequest{Name: "_测试", Description: "1"}, true},
		{SceneSetRequest{Name: "1_测试a", Description: "1"}, true},
		{SceneSetRequest{Name: "a_测试1", Description: "1"}, true},
	}
	for _, v := range tt {
		assert.Equal(t, v.want, v.req.Validate() == nil)
	}

}
