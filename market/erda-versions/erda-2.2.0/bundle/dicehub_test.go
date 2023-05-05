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
//	"github.com/stretchr/testify/assert"
//
//	"github.com/erda-project/erda/apistructs"
//)
//
//func TestBundle_QueryExtensionVersions(t *testing.T) {
//	os.Setenv("DICEHUB_ADDR", "http://127.0.0.1:10000")
//	bdl := New(WithDiceHub())
//
//	result, err := bdl.QueryExtensionVersions(apistructs.ExtensionVersionQueryRequest{
//		Name: "mysql",
//	})
//	assert.Nil(t, err)
//	t.Log(result)
//
//	os.Unsetenv("DICEHUB_ADDR")
//}
//
//func TestBundle_GetExtensionVersion(t *testing.T) {
//	os.Setenv("DICEHUB_ADDR", "http://127.0.0.1:10000")
//	bdl := New(WithDiceHub())
//
//	result, err := bdl.GetExtensionVersion(apistructs.ExtensionVersionGetRequest{
//		Name:    "mysql",
//		Version: "0.0.3",
//	})
//	assert.Nil(t, err)
//	t.Log(result)
//
//	os.Unsetenv("DICEHUB_ADDR")
//}
//
//func TestBundle_SearchExtensions(t *testing.T) {
//	os.Setenv("DICEHUB_ADDR", "dicehub.default.svc.cluster.local:10000")
//	bdl := New(WithDiceHub())
//
//	result, err := bdl.SearchExtensions(apistructs.ExtensionSearchRequest{
//		Extensions: []string{"git"},
//	})
//	assert.Nil(t, err)
//	t.Log(result)
//
//	os.Unsetenv("DICEHUB_ADDR")
//}
