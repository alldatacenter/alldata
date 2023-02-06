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

package errors

import (
	"encoding/json"
	"errors"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestBackendErrMarshal(t *testing.T) {
	var b BackendErrs = make(map[string][]error)
	b["asd"] = []error{errors.New("EEE")}
	v, err := json.Marshal(&b)
	assert.Nil(t, err)
	assert.Equal(t, "{\"asd\":[\"EEE\"]}", string(v))
}

func TestMarshal(t *testing.T) {
	d := New()
	d.BackendErrs = map[string][]error{
		"AA": {errors.New("EEE")},
	}
	v, err := json.Marshal(d)
	assert.Nil(t, err)
	assert.Equal(t, "{\"BackendErrs\":{\"AA\":[\"EEE\"]},\"FilterInfo\":\"\",\"FilterErr\":\"\"}", string(v))
}
