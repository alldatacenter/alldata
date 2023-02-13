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

package strutil_test

import (
	"testing"

	"github.com/erda-project/erda/pkg/strutil"
)

func TestSnakeToUpCamel(t *testing.T) {
	var names = map[string]string{
		"this_is_a_snake_name": "ThisIsASnakeName",
		"This_Is_A_Snake_name": "ThisIsASnakeName",
	}
	for snake, camel := range names {
		if s := strutil.SnakeToUpCamel(snake); s != camel {
			t.Fatalf("snake: %s, s: %s, camel: %s", snake, snake, camel)
		}
	}
}
