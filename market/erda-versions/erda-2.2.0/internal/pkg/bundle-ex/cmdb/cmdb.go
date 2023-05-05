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

package cmdb

import (
	"fmt"
	"os"
	"strings"
	"time"

	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

// Cmdb .
type Cmdb struct {
	url        string
	operatorID string
	hc         *httpclient.HTTPClient
}

// Option .
type Option func(cmdb *Cmdb)

// New .
func New(options ...Option) *Cmdb {
	addr := strings.TrimRight(discover.CoreServices(), "/")
	opid := os.Getenv("DICE_OPERATOR_ID")
	if len(opid) <= 0 {
		opid = "1100"
	}
	cmdb := &Cmdb{
		url:        fmt.Sprintf("http://%s", addr),
		operatorID: opid,
	}
	for _, op := range options {
		op(cmdb)
	}
	if cmdb.hc == nil {
		cmdb.hc = httpclient.New(httpclient.WithTimeout(time.Second, time.Second*60))
	}
	return cmdb
}

// WithURL .
func WithURL(url string) Option {
	return func(e *Cmdb) {
		e.url = strings.TrimRight(url, "/")
	}
}

// WithOperatorID .
func WithOperatorID(operatorID string) Option {
	return func(e *Cmdb) {
		e.operatorID = operatorID
	}
}

// WithHTTPClient .
func WithHTTPClient(hc *httpclient.HTTPClient) Option {
	return func(e *Cmdb) {
		e.hc = hc
	}
}
