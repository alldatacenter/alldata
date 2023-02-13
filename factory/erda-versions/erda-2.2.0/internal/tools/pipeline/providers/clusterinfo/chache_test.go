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

package clusterinfo

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
)

func TestGetClusterInfoByName(t *testing.T) {
	ca := NewClusterInfoCache()
	ca.cache = map[string]apistructs.ClusterInfo{
		"dev": {
			Name: "dev",
			CM: map[apistructs.ClusterInfoMapKey]string{
				"host": "localhost",
			},
		},
	}

	c1, ok := ca.GetClusterInfoByName("dev")
	if !ok {
		t.Error("Expected to find cluster info")
	}
	assert.Equal(t, "dev", c1.Name)
	c2, ok := ca.GetClusterInfoByName("dev")
	if !ok {
		t.Error("Expected to find cluster info")
	}
	assert.Equal(t, "dev", c2.Name)
	delete(c1.CM, "host")
	if c2.CM["host"] != "localhost" {
		t.Error("expected to get new clusterinfo data")
	}
}
