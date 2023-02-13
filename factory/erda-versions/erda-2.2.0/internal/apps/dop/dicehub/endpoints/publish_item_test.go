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
package endpoints

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
)

func Test_getMobileType(t *testing.T) {
	tp0, err := getMobileType("aab")
	assert.NoError(t, err)
	assert.Equal(t, tp0, apistructs.ResourceTypeAndroidAppBundle)

	tp1, err := getMobileType("android")
	assert.NoError(t, err)
	assert.Equal(t, tp1, apistructs.ResourceTypeAndroid)

	tp2, err := getMobileType("ios")
	assert.NoError(t, err)
	assert.Equal(t, tp2, apistructs.ResourceTypeIOS)

	tp3, err := getMobileType("h5")
	assert.NoError(t, err)
	assert.Equal(t, tp3, apistructs.ResourceTypeH5)

	tp4, err := getMobileType("")
	assert.NoError(t, err)
	assert.Equal(t, tp4, apistructs.ResourceType(""))

	tp5, err := getMobileType("unknow")
	assert.NotNil(t, err)
	assert.Equal(t, tp5, apistructs.ResourceType(""))
}
