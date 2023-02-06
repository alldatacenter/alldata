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

package testplan

import (
	"os"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/alecthomas/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
)

func Test_convertAddr(t *testing.T) {
	err := os.Setenv("DOP_ADDR", "dop:9999")
	assert.NoError(t, err)

	var bdl *bundle.Bundle
	p := &provider{
		bundle: bdl,
	}
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "CreateWebhook",
		func(bdl *bundle.Bundle, r apistructs.CreateHookRequest) error {
			assert.Equal(t, r.URL, "http://dop:9999/api/autotests/actions/plan-execute-callback")
			return nil
		})
	defer monkey.UnpatchAll()
	err = p.registerWebHook()
	assert.NoError(t, err)
}
