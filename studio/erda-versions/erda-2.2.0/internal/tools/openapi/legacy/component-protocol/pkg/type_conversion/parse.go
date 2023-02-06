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

package type_conversion

import (
	"fmt"
	"strconv"
)

func InterfaceToUint64(value interface{}) (uint64, error) {
	if value == nil {
		return 0, fmt.Errorf("can not parse value")
	}

	switch value.(type) {
	case int:
		return uint64(value.(int)), nil
	case float64:
		return uint64(value.(float64)), nil
	case string:
		intValue, err := strconv.ParseInt(value.(string), 10, 64)
		if err != nil {
			return 0, err
		}
		return uint64(intValue), nil
	}
	return 0, fmt.Errorf("can not parse value")
}
