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
	"net/http"
)

//                    request       response       response error
type RetryFunc func(*http.Request, *http.Response, error) bool

type RetryOption struct {
	MaxTime  int         // default 3
	Interval int         // second, default: 2s
	Fns      []RetryFunc // fn1 || fn2 || ...
}

var (
	Retry5XX = RetryOption{3, 2, []RetryFunc{
		func(req *http.Request, resp *http.Response, respErr error) bool {
			return resp.StatusCode/100 == 5
		},
	}}

	RetryErrResp = RetryOption{3, 2, []RetryFunc{
		func(req *http.Request, resp *http.Response, respErr error) bool {
			return respErr != nil
		},
	}}

	NoRetry = RetryOption{}
)

func mergeRetryOptions(rs []RetryOption) RetryOption {
	r := RetryOption{3, 2, nil}
	for _, op := range rs {
		if op.MaxTime == 0 && op.Interval == 0 && len(op.Fns) == 0 {
			r = NoRetry
			r.MaxTime = 1 // run only once
			break
		}
		if op.MaxTime > r.MaxTime {
			r.MaxTime = op.MaxTime
		}
		if op.Interval > r.Interval {
			r.Interval = op.Interval
		}
		r.Fns = append(r.Fns, op.Fns...)
	}
	return r
}
