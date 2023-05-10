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
//	"fmt"
//	"os"
//	"testing"
//
//	"github.com/stretchr/testify/require"
//
//	"github.com/erda-project/erda/apistructs"
//)
//
//func TestBundle_GetProject(t *testing.T) {
//	os.Setenv("CMDB_ADDR", "cmdb.marathon.l4lb.thisdcos.directory:9093")
//	defer func() {
//		os.Unsetenv("CMDB_ADDR")
//	}()
//	b := New(WithCMDB())
//	pj, err := b.GetProject(1)
//	require.NoError(t, err)
//	require.True(t, 1 == pj.ID)
//}
//
//func TestBundle_ListProject(t *testing.T) {
//	os.Setenv("CMDB_ADDR", "10.99.186.34:9093")
//	defer func() {
//		os.Unsetenv("CMDB_ADDR")
//	}()
//	b := New(WithCMDB())
//	req := apistructs.ProjectListRequest{}
//	req.OrgID = 1
//	req.Name = "dice-dev"
//	req.PageNo = 1
//	req.PageSize = 10
//	pj, err := b.ListProject("2", req)
//	require.NoError(t, err)
//	require.True(t, pj.List[0].Name == "dice-dev")
//}
//
//func TestBundle_GetProjectNS(t *testing.T) {
//	os.Setenv("CMDB_ADDR", "cmdb.default.svc.cluster.local:9093")
//	defer func() {
//		os.Unsetenv("CMDB_ADDR")
//	}()
//	b := New(WithCMDB())
//	info, err := b.GetProjectNamespaceInfo(1)
//	require.NoError(t, err)
//	fmt.Println(info)
//}
