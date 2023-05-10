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

package desensitize

import (
	"strings"
	"unicode/utf8"
)

func Mobile(str string) string {
	return desensitize(str, 3, 4)
}

func Email(str string) string {
	parts := strings.SplitN(str, "@", 2)
	if len(parts) == 2 {
		return desensitize(parts[0], 3, 1) + "@" + parts[1]
	}
	return desensitize(str, 3, 1)
}

func Name(str string) string {
	return desensitize(str, 1, 1)
}

func desensitize(str string, before, end int) string {
	l := utf8.RuneCountInString(str)
	r := []rune(str)
	switch l {
	case 0:
		return ""
	case 1:
		return "*"
	case 2:
		return string(r[0:1]) + "*"
	}
	k := l / 3
	// make sure before+end <= l-k (at least k '*')
	if before+k+1 > l {
		before = l - k - 1
	}
	if before+end+k > l {
		end = l - before - k
	}
	var sb strings.Builder
	for i := 0; i < l; i++ {
		if i < before || i >= l-end {
			sb.WriteRune(r[i])
		} else {
			// replace to '*'
			sb.WriteString("*")
		}
	}
	return sb.String()
}
