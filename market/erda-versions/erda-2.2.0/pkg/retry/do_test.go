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
	"fmt"
	"testing"
	"time"

	"github.com/davecgh/go-spew/spew"
	"github.com/pkg/errors"
)

func testF(text string) error {
	return errors.New(text)
}

func testFF() {
	fmt.Println(time.Now())
}

//func TestDoWithInterval(t *testing.T) {
//	err := DoWithInterval(func() error {
//		testFF()
//		return nil
//	}, 2, time.Second*3)
//	require.Error(t, err)
//}

func TestDo(t *testing.T) {
	var i = 0
	err := DoWithInterval(func() error {
		if i == 0 {
			i++
			return fmt.Errorf("1")
		}
		i++
		return nil
	}, 1, 1*time.Second)
	spew.Dump(err)
}
