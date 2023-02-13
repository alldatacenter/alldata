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

package http

import (
	"math/rand"
	"net/http"
	"time"

	uuid "github.com/satori/go.uuid"
)

var random = rand.New(rand.NewSource(time.Now().UnixNano()))

func (p *provider) shouldSample() bool {
	if p.Cfg.Trace.Enable && p.Cfg.Trace.Rate > 0 {
		if random.Intn(100) < p.Cfg.Trace.Rate {
			return true
		}
	}
	return false
}

func (p *provider) setupTrace(req *http.Request, tags map[string]string, fields map[string]interface{}) {
	requestID := uuid.NewV4().String()
	tags["request_id"] = requestID
	req.Header.Set("terminus-request-id", requestID)
	req.Header.Set("terminus-request-sampled", "true")
}
