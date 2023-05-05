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
//	"strconv"
//	"testing"
//	"time"
//
//	"github.com/stretchr/testify/require"
//
//	"github.com/erda-project/erda/apistructs"
//)
//
//func TestBundle_CreateEvent(t *testing.T) {
//	os.Setenv("CMDB_ADDR", "cmdb.marathon.l4lb.thisdcos.directory:9093")
//	defer func() {
//		os.Unsetenv("CMDB_ADDR")
//	}()
//	b := New(WithCMDB())
//	tm := strconv.FormatInt(time.Now().Unix(), 10)
//	audit := apistructs.Audit{
//		UserID:       "1000008",
//		ScopeType:    "app",
//		ScopeID:      2,
//		AppID:        4,
//		ProjectID:    2,
//		OrgID:        2,
//		Context:      map[string]interface{}{"appId": "4", "projectId": "2"},
//		TemplateName: "createApp",
//		AuditLevel:   "p3",
//		Result:       "success",
//		ErrorMsg:     "",
//		StartTime:    tm,
//		EndTime:      tm,
//		ClientIP:     "1.1.1.1",
//		UserAgent:    "chrom",
//	}
//	err := b.CreateAuditEvent(&apistructs.AuditCreateRequest{Audit: audit})
//	require.NoError(t, err)
//}
