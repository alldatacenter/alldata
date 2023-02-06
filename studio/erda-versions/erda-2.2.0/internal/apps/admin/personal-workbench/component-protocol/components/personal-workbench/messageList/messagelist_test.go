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

package messageList

import (
	"os"
	"testing"

	"github.com/erda-project/erda/bundle"
)

func initBundle() *bundle.Bundle {
	os.Setenv("CORE_SERVICES_ADDR", "http://core-services.project-387-dev.svc.cluster.local:9526")
	os.Setenv("MSP_ADDR", "http://msp.project-387-dev.svc.cluster.local:8080")
	os.Setenv("DOP_ADDR", "http://dop.project-387-dev.svc.cluster.local:9527")
	os.Setenv("ORCHESTRATOR_ADDR", "http://orchestrator.project-387-dev.svc.cluster.local:8081")
	os.Setenv("GITTAR_ADDR", "http://gittar.project-387-dev.svc.cluster.local:5566")
	bdl := bundle.New(
		bundle.WithCoreServices(),
		bundle.WithMSP(),
		bundle.WithDOP(),
		bundle.WithGittar(),
		bundle.WithOrchestrator(),
	)
	return bdl
}

func TestMessage(t *testing.T) {

	//bdl := initBundle()
	//wbsvc := workbench.New(workbench.WithBundle(bdl))
	//l := MessageList{
	//	bdl:   bdl,
	//	wbSvc: wbsvc,
	//	identity: apistructs.Identity{
	//		UserID: "2",
	//		OrgID:  "1",
	//	},
	//	filterReq: apistructs.WorkbenchMsgRequest{
	//		Type: apistructs.WorkbenchItemUnreadMes,
	//		PageRequest: apistructs.PageRequest{
	//			PageNo:   1,
	//			PageSize: 10,
	//		},
	//	},
	//}
	//l.doFilterMsg()
}
