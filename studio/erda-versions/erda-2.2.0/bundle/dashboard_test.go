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
//	"time"
//
//	"github.com/erda-project/erda/apistructs"
//)
//
//func TestBundle_GetLog(*testing.T) {
//	os.Setenv("MONITOR_ADDR", "monitor.default.svc.cluster.local:7096")
//	b := New(WithMonitor())
//
//	fmt.Println(b.GetLog(apistructs.DashboardSpotLogRequest{
//		ID:     "pipeline-task-244",
//		Source: apistructs.DashboardSpotLogSourceJob,
//		Stream: apistructs.DashboardSpotLogStreamStdout,
//		Count:  -50,
//		Start:  0,
//		End:    time.Duration(1590047806647571944),
//	}))
//}
