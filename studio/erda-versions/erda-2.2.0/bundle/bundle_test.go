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

import (
	"os"
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/pkg/http/httpclient"
)

func TestBundleOption(t *testing.T) {
	os.Setenv("CMDB_ADDR", "http://a.com")
	os.Setenv("DICEHUB_ADDR", "http://a.com")
	os.Setenv("CMP_ADDR", "http://a.com")
	os.Setenv("ORCHESTRATOR_ADDR", "http://a.com")
	os.Setenv("SCHEDULER_ADDR", "http://a.com")
	os.Setenv("ADDON_PLATFORM_ADDR", "http://a.com")

	defer func() {
		os.Unsetenv("CMDB_ADDR")
		os.Unsetenv("DICEHUB_ADDR")
		os.Unsetenv("EVENTBOX_ADDR")
		os.Unsetenv("CMP_ADDR")
		os.Unsetenv("ORCHESTRATOR_ADDR")
		os.Unsetenv("SCHEDULER_ADDR")
		os.Unsetenv("ADDON_PLATFORM_ADDR")
	}()

	hc := httpclient.New()

	options := []Option{
		WithCMDB(),
		WithAddOnPlatform(),
		WithDiceHub(),
		WithCoreServices(),
		WithCMP(),
		WithOrchestrator(),
		WithScheduler(),
		WithHTTPClient(hc),
	}

	b := New(options...)

	var (
		v   string
		err error
	)

	v, err = b.urls.CMDB()
	assert.Equal(t, v, "http://a.com")
	assert.Nil(t, err)

	v, err = b.urls.DiceHub()
	assert.Equal(t, v, "http://a.com")
	assert.Nil(t, err)

	v, err = b.urls.CMP()
	assert.Equal(t, v, "http://a.com")
	assert.Nil(t, err)

	v, err = b.urls.Orchestrator()
	assert.Equal(t, v, "http://a.com")
	assert.Nil(t, err)
}
