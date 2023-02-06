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

package util

import (
	"encoding/base64"
	"io"
	"net/http"

	"github.com/erda-project/erda/pkg/strutil"
)

const Base64EncodedRequestBody = "base64-encoded-request-body"

func HandleRequest(r *http.Request) {
	// base64 decode request body if declared in header
	if strutil.Equal(r.Header.Get(Base64EncodedRequestBody), "true", true) {
		r.Body = io.NopCloser(base64.NewDecoder(base64.StdEncoding, r.Body))
	}
}
