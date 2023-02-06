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

package strutil

import (
	"bytes"
)

// SnakeToUpCamel make a snake style name to up-camel style
func SnakeToUpCamel(name string) string {
	var (
		buf = bytes.NewBuffer(nil)
		big = true
	)
	for _, s := range name {
		if s == '_' {
			big = true
			continue
		}
		if big && s >= 'a' && s <= 'z' {
			buf.WriteRune(s - 32)
		} else {
			buf.WriteRune(s)
		}
		big = false
	}
	return buf.String()
}
