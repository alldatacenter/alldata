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

package pexpr

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestFindInvalidPlaceholders(t *testing.T) {
	invalidExprs := []string{
		"${{configs.key}}",    // 前后均缺失空格
		"${{ configs.key}}",   // 后面缺失空格
		"${{configs.key }}",   // 前面缺失空格
		"${{  configs.key }}", // 前面有两个空格
		"${{ configs.key  }}", // 后面有两个空格
	}

	for _, expr := range invalidExprs {
		fmt.Println(expr)
		v := FindInvalidPlaceholders(expr)
		assert.True(t, len(v) == 1)
		assert.True(t, v[0] == expr)
	}

	// 正确
	v := FindInvalidPlaceholders("${{ configs.key }}")
	assert.True(t, len(v) == 0)
}
