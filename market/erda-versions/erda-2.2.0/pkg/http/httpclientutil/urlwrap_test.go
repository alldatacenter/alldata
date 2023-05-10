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

package httpclientutil

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/require"
	"gopkg.in/stretchr/testify.v1/assert"
)

func TestRmProto(t *testing.T) {
	var expected = "dice.terminus.io"

	var http = "http://dice.terminus.io"
	require.Equal(t, RmProto(http), expected)

	var https = "https://dice.terminus.io"
	require.Equal(t, RmProto(https), expected)

	var noPrefix = "dice.terminus.io"
	require.Equal(t, RmProto(noPrefix), expected)
}

func TestGetPath(t *testing.T) {
	assert.Equal(t,
		"/repository/docker-hosted-platform/",
		GetPath("http://addon-nexus.default.svc.cluster.local:8081/repository/docker-hosted-platform/"))
}

func TestCombineHostAndPath(t *testing.T) {
	fmt.Println(CombineHostAndPath("https://baidu.com/", "/repository/platform/"))
}

func TestGetPort(t *testing.T) {
	//assert.Equal(t, 8081, GetPort("http://addon-nexus.default.svc.cluster.local:8081/repository/docker-hosted-platform/"))
	//assert.Equal(t, 80, GetPort("addon-nexus"))
	assert.Equal(t, 8081, GetPort("addon-nexus.default.svc.cluster.local:8081"))
}

func TestGetHost(t *testing.T) {
	assert.Equal(t, "nexus-sys.dev.terminus.io", GetHost("http://nexus-sys.dev.terminus.io/repository/platform/"))
}
