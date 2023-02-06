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

package retry

import (
	"time"

	"github.com/hashicorp/go-multierror"
)

func Do(fn func() error, n int) (err error) {
	return DoWithInterval(fn, n, 0)
}

func DoWithInterval(fn func() error, n int, interval time.Duration) error {
	var me *multierror.Error
	if n <= 0 {
		n = 1
	}
	for i := 0; i < n; i++ {
		err := fn()
		if err == nil {
			me = nil
			break
		}
		me = multierror.Append(me, err)
		time.Sleep(interval)
	}
	if me != nil {
		return me.ErrorOrNil()
	}
	return nil
}
