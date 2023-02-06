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

package conf

import (
	"os"
	"testing"

	"bou.ke/monkey"
	"github.com/alecthomas/assert"

	"github.com/erda-project/erda/pkg/discover"
)

func TestGetDomain(t *testing.T) {
	host := "www.sfsfsf.erda.cloud"
	confDomain := ".terminus.io,.erda.cloud"
	domain, err := GetDomain(host, confDomain)
	assert.NoError(t, err)
	assert.Equal(t, ".erda.cloud", domain)
}

func TestGetUCRedirectHost(t *testing.T) {
	referer := "https://erda.cloud"
	guard := monkey.Patch(UCRedirectHost, func() string {
		return "openapi.dev.terminus.io,openapi.erda.cloud"
	})
	defer guard.Unpatch()
	host := GetUCRedirectHost(referer)
	assert.Equal(t, "openapi.erda.cloud", host)
}

func Test_getSvcHostPortFromAddr(t *testing.T) {
	host, port, ok := getSvcHostPortFromAddr("fdp.project-865-dev.svc.cluster.local:8080")
	assert.True(t, ok)
	assert.Equal(t, host, "fdp.project-865-dev.svc.cluster.local")
	assert.Equal(t, port, uint16(8080))

	host, port, ok = getSvcHostPortFromAddr("fdp")
	assert.True(t, ok)
	assert.Equal(t, host, "fdp")
	assert.Equal(t, port, uint16(80))
}

func TestLoad(t *testing.T) {
	os.Setenv(discover.EnvFDPMaster, "fdp-master")
	os.Setenv(discover.EnvPipeline, "pipeline:3081")
	os.Setenv(discover.EnvDOP, "dop.project-387-dev.svc.cluster.local:9527")
	defer os.Unsetenv(discover.EnvDOP)
	defer os.Unsetenv(discover.EnvPipeline)
	defer os.Unsetenv(discover.EnvFDPMaster)

	mapping := initCustomSvcHostPortMapping()
	assert.Equal(t, 3, len(mapping))

	assert.Equal(t, discover.SvcFDPMaster, mapping[discover.SvcFDPMaster].Host)
	assert.Equal(t, uint16(80), mapping[discover.SvcFDPMaster].Port)

	assert.Equal(t, discover.SvcPipeline, mapping[discover.SvcPipeline].Host)
	assert.Equal(t, uint16(3081), mapping[discover.SvcPipeline].Port)

	assert.Equal(t, "dop.project-387-dev.svc.cluster.local", mapping[discover.SvcDOP].Host)
	assert.Equal(t, uint16(9527), mapping[discover.SvcDOP].Port)
}
