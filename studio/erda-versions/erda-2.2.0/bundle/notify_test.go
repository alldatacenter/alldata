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
//	"github.com/stretchr/testify/assert"
//
//	"github.com/erda-project/erda/apistructs"
//)
//
//func TestBundle_GetNotifyConfigMS(t *testing.T) {
//	os.Setenv("CMDB_ADDR", "cmdb.default.svc.cluster.local:9093")
//	defer func() {
//		os.Unsetenv("CMDB_ADDR")
//	}()
//	bdl := New(WithCMDB())
//	cfg, err := bdl.GetNotifyConfigMS("2", "1")
//	assert.NoError(t, err)
//	assert.Equal(t, false, cfg)
//}
//
//func TestBundle_NotifyList(t *testing.T) {
//	os.Setenv("MONITOR_ADDR", "monitor.default.svc.cluster.local:7096")
//	defer func() {
//		os.Unsetenv("MONITOR_ADDR")
//	}()
//	bdl := New(WithMonitor())
//	req := apistructs.NotifyPageRequest{
//		ScopeId: "18",
//		Scope:   "app",
//		UserId:  "2",
//		OrgId:   "1",
//	}
//	cfg, err := bdl.NotifyList(req)
//	assert.NoError(t, err)
//	fmt.Printf("%+v", cfg)
//}
