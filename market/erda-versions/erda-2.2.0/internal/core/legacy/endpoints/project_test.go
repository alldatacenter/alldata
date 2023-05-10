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

package endpoints

import (
	"net/http"
	"net/url"
	"testing"

	"github.com/gorilla/schema"
	"github.com/stretchr/testify/assert"
)

func TestGetProjParams(t *testing.T) {
	// init Endpoints with queryStringDecoder
	queryStringDecoder := schema.NewDecoder()
	queryStringDecoder.IgnoreUnknownKeys(true)

	req, err := http.NewRequest("GET", "https://baidu.com", nil)
	if err != nil {
		panic(err)
	}

	params := make(url.Values)
	params.Add("keepMsp", "true")
	params.Add("orgId", "1")
	req.URL.RawQuery = params.Encode()

	parsedReq, err := getListProjectsParam(req)
	assert.NoError(t, err)
	assert.Equal(t, parsedReq.KeepMsp, true)
}
