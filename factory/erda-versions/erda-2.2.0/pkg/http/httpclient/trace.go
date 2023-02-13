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

package httpclient

import (
	"bytes"
	"fmt"
	"io"
	"io/ioutil"
	"net/http"
)

type Tracer interface {
	TraceRequest(*http.Request)
	// if read response body, you need to set it back
	TraceResponse(*http.Response)
}

type DefaultTracer struct {
	w io.Writer
}

func NewDefaultTracer(w io.Writer) *DefaultTracer {
	return &DefaultTracer{w}
}

func (t *DefaultTracer) TraceRequest(req *http.Request) {
	s := fmt.Sprintf("RequestURL: %s\n", req.URL.String())
	io.WriteString(t.w, s)
}

func (t *DefaultTracer) TraceResponse(r *http.Response) {
	body, err := ioutil.ReadAll(r.Body)
	if err != nil {
		io.WriteString(t.w, fmt.Sprintf("TraceResponse: read response body fail: %v", err))
		return
	}
	r.Body.Close()
	io.WriteString(t.w, fmt.Sprintf("ResponseBody: %s\n", string(body)))
	r.Body = ioutil.NopCloser(bytes.NewReader(body))
}
