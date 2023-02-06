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

package ticker_test

import (
	"fmt"
	"testing"
	"time"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/pkg/time/ticker"
)

func TestExitError_Error(t *testing.T) {
	var e = ticker.ExitError{Message: "something wrong"}
	t.Log(e.Error())
}

func TestTicker_Close(t *testing.T) {
	var times = 0
	ti := ticker.New(time.Millisecond*200, func() (bool, error) {
		times++
		fmt.Println("times:", times)
		if times > 5 {
			return true, &ticker.ExitError{Message: "time over"}
		}
		if times > 3 {
			return false, errors.New("normal error")
		}
		return false, nil
	})
	ti.Run()
}
