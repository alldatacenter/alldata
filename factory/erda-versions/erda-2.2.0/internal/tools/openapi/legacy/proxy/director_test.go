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

package proxy

import (
	"os"
	"testing"

	"gotest.tools/assert"
)

func TestReplaceServiceName(t *testing.T) {
	os.Setenv("ERDA_SYSTEM_FQDN", "erda-system.svc.cluster.local")
	defer os.Unsetenv("ERDA_SYSTEM_FQDN")
	result := replaceServiceName(os.Getenv("ERDA_SYSTEM_FQDN"), "openapi.default.svc.cluster.local")
	assert.Equal(t, "openapi.erda-system.svc.cluster.local", result)

	// only fdp custom namespace
	result = replaceServiceName("project-865-dev.svc.cluster.local", "fdp")
	assert.Equal(t, "fdp.project-865-dev.svc.cluster.local", result)
}

func Test_getServiceName(t *testing.T) {
	assert.Equal(t, "fdp", getServiceName("fdp.project-865-dev.svc.cluster.local"))
	assert.Equal(t, "fdp", getServiceName("fdp"))
}
