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

package reportapisv1

import (
	"os"
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda-proto-go/tools/monitor/dashboard/report/pb"
	"github.com/erda-project/erda/pkg/discover"
)

func Test_provider_createFQDN(t *testing.T) {
	p := &provider{
		Cfg: &config{
			DiceNameSpace: "project-xxx",
		},
	}
	ass := assert.New(t)

	os.Setenv("MONITOR_ADDR", "monitor:7096")
	addr, err := p.createFQDN(discover.Monitor())
	ass.Nil(err)
	assert.Equal(t, "monitor.project-xxx:7096", addr)

	os.Setenv("MONITOR_ADDR", "monitor")
	addr, err = p.createFQDN(discover.Monitor())
	ass.Error(err)

	os.Setenv("MONITOR_ADDR", "http://monitor:7096")
	addr, err = p.createFQDN(discover.Monitor())
	ass.Error(err)
}

func Test_notify2pb(t *testing.T) {
	assert.Equal(t, &pb.Notify{
		Type:        "aaa",
		GroupId:     1024,
		GroupType:   "dingding",
		NotifyGroup: nil,
	}, notify2pb(&notify{
		Type:        "aaa",
		GroupId:     1024,
		GroupType:   "dingding",
		NotifyGroup: nil,
	}))
}

func Test_editReportTaskFields(t *testing.T) {
	report := &reportTask{}
	update := &reportTaskUpdate{
		Name: pointerString("a"),
		NotifyTarget: &pb.Notify{
			Type: "aa",
		},
		DashboardId: pointerString("b"),
	}
	assert.Equal(t, &reportTask{
		Name: "a",
		NotifyTarget: pb2notify(&pb.Notify{
			Type: "aa",
		}),
		DashboardId: "b",
	}, editReportTaskFields(report, update))
}

func pointerString(s string) *string {
	return &s
}

func pointerInt(x int) *int {
	return &x
}

func pointerUint64(x uint64) *uint64 {
	return &x
}
func pointerInt64(x int64) *int64 {
	return &x
}
