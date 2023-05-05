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

package bundle

//import (
//	"os"
//	"testing"
//
//	"github.com/davecgh/go-spew/spew"
//	"github.com/stretchr/testify/assert"
//)
//
//func TestBundle_GetNexusOrgDockerCredentialByImage(t *testing.T) {
//	os.Setenv("CMDB_ADDR", "localhost:9093")
//	defer os.Unsetenv("CMDB_ADDR")
//	bdl := New(WithCMDB())
//	user, err := bdl.GetNexusOrgDockerCredentialByImage(1, "nginx")
//	assert.NoError(t, err)
//	assert.Nil(t, user)
//
//	user, err = bdl.GetNexusOrgDockerCredentialByImage(1, "docker-hosted-org-1-nexus-sys.dev.terminus.io/terminus-dice-dev/test-release-cross-cluster:dockerfile-1593662620685244179")
//	assert.NoError(t, err)
//	assert.NotNil(t, user)
//	spew.Dump(user)
//}
