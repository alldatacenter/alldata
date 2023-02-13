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

package entrance

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestFindMainEntranceFileName(t *testing.T) {
	mainFileName, found := FindMainEntranceFileName()
	assert.False(t, found)
	assert.Empty(t, mainFileName)
}

func Test_getAppNameFromModulePath(t *testing.T) {
	assert.Equal(t, "collector", getAppNameFromModulePath("monitor/collector"))
	assert.Equal(t, "monitor", getAppNameFromModulePath("monitor"))
}

func Test_getModulePathFromMainEntranceFileName(t *testing.T) {
	assert.Equal(t, "monitor/monitor", getModulePathFromMainEntranceFileName("/go/src/github.com/erda-project/erda/cmd/monitor/monitor/main.go"))
	assert.Equal(t, "pipeline", getModulePathFromMainEntranceFileName("/go/src/github.com/erda-project/erda/cmd/pipeline/main.go"))
	assert.Equal(t, "", getModulePathFromMainEntranceFileName("/go/src/github.com/erda-project/erda/cmd/main.go"))
}
