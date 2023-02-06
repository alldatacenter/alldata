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

package apis

import (
	"os"
	"testing"

	"github.com/stretchr/testify/assert"

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
