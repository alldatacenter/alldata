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

package manager

import (
	"fmt"
	"net/http"
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/pkg/http/httputil"
)

func TestGetOrgIDStrFromHeader(t *testing.T) {
	req, err := http.NewRequest(http.MethodGet, "https://demo.io", nil)
	assert.NoError(t, err)
	req.Header.Set(httputil.OrgHeader, "1")
	id, err := GetOrgIDStr(req)
	assert.NoError(t, err)
	assert.Equal(t, id, "1")
}

func TestGetOrgIDStrFromUrl(t *testing.T) {
	req, err := http.NewRequest(http.MethodGet, "https://demo.io?orgID=1", nil)
	assert.NoError(t, err)
	id, err := GetOrgIDStr(req)
	assert.NoError(t, err)
	assert.Equal(t, id, "1")
}

func TestGetOrgIDStrEmpty(t *testing.T) {
	req, err := http.NewRequest(http.MethodGet, "https://demo.io", nil)
	assert.NoError(t, err)
	_, err = GetOrgIDStr(req)
	assert.Error(t, fmt.Errorf("invalid param, orgID is empty"), err)
}

func TestGetOrgIDFromHeader(t *testing.T) {
	req, err := http.NewRequest(http.MethodGet, "https://demo.io", nil)
	assert.NoError(t, err)
	req.Header.Set(httputil.OrgHeader, "1")
	id, err := GetOrgID(req)
	assert.NoError(t, err)
	assert.Equal(t, id, uint64(1))
}

func TestGetOrgIDFromUrl(t *testing.T) {
	req, err := http.NewRequest(http.MethodGet, "https://demo.io?orgID=1", nil)
	assert.NoError(t, err)
	id, err := GetOrgID(req)
	assert.NoError(t, err)
	assert.Equal(t, id, uint64(1))
}

func TestGetOrgIDEmpty(t *testing.T) {
	req, err := http.NewRequest(http.MethodGet, "https://demo.io", nil)
	assert.NoError(t, err)
	_, err = GetOrgID(req)
	assert.Error(t, fmt.Errorf("invalid param, orgID is empty"), err)
}

func TestGetOrgIDInvalid(t *testing.T) {
	req, err := http.NewRequest(http.MethodGet, "https://demo.io?orgID=invalid", nil)
	assert.NoError(t, err)
	_, err = GetOrgID(req)
	assert.Error(t, fmt.Errorf("invalid param, orgID is invalid"), err)
}
