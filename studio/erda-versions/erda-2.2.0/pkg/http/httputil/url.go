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

package httputil

import (
	"net/url"
	"strings"
)

// JoinPath .
func JoinPath(appendRoot bool, segments ...string) string {
	sb := &strings.Builder{}
	if appendRoot {
		sb.WriteString("/")
	}
	last := len(segments) - 1
	for i, s := range segments {
		sb.WriteString(url.PathEscape(s))
		if i < last {
			sb.WriteRune('/')
		}
	}
	return sb.String()
}

// JoinPathR .
func JoinPathR(segments ...string) string {
	return JoinPath(true)
}
