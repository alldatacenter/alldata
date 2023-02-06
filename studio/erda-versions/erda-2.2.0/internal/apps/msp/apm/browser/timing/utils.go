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

package timing

import (
	"strconv"
)

func parseInt64(value string, def int64) int64 {
	if num, err := strconv.ParseInt(value, 10, 64); err == nil {
		return num
	}
	return def
}

func parseInt64WithRadix(value string, def int64, radix int) int64 {
	if num, err := strconv.ParseInt(value, radix, 64); err == nil {
		return num
	}
	return def
}
