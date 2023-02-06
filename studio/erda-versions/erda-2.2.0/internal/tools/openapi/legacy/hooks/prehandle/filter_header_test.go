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

package prehandle

import (
	"context"
	"net/http"
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/pkg/http/httputil"
)

func TestFilterHeader(t *testing.T) {
	r := http.Request{
		Header: make(http.Header),
	}

	// add User-ID header
	r.Header.Add(httputil.UserHeader, "1")
	r.Header.Add(httputil.UserHeader, "2")
	assert.True(t, 2 == len(r.Header.Values(httputil.UserHeader)))
	r.Header.Del(httputil.UserHeader)
	assert.True(t, 0 == len(r.Header.Values(httputil.UserHeader)))
	r.Header.Del(httputil.UserHeader)
	assert.True(t, 0 == len(r.Header.Values(httputil.UserHeader)))

	// add Internal-Client header
	r.Header.Add(httputil.InternalHeader, "bundle")
	assert.True(t, 1 == len(r.Header.Values(httputil.InternalHeader)))
	r.Header.Add(httputil.InternalHeader, "true")
	assert.True(t, 2 == len(r.Header.Values(httputil.InternalHeader)))

	// add Client-ID/Client-Name header
	r.Header.Add(httputil.ClientIDHeader, "bundle")
	r.Header.Add(httputil.ClientNameHeader, "bundle")

	// add a valid custom header
	r.Header.Add("Pipeline-ID", "1")

	// filter
	FilterHeader(context.Background(), nil, &r)

	assert.True(t, 1 == len(r.Header))
}
