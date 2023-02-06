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
	"regexp"

	"github.com/erda-project/erda/pkg/strutil"
)

// phRe 占位符正则表达式:
//   ${{ configs.key }}
//   ${{ dirs.preTaskName.fileName }}
//   ${{ outputs.preTaskName.key }}
//   ${{ params.key }}
//   ${{ (echo hello world) }}
var PhRe = regexp.MustCompile(`\${{[ ]{1}([^{}\s]+)[ ]{1}}}`) // [ ]{1} 强调前后均有且仅有一个空格

// loosePhRe 宽松的正则表达式:
var LoosePhRe = regexp.MustCompile(`\${{[^{}]+}}`)

// FindInvalidPlaceholders 找到表达式中不合规范的占位符
func FindInvalidPlaceholders(exprStr string) []string {
	// 合法的占位符列表
	validPhs := PhRe.FindAllString(exprStr, -1)
	// 宽松的占位符列表
	loosePhs := LoosePhRe.FindAllString(exprStr, -1)

	// 不在合法占位符列表中的即为非法占位符
	var invalidPhs []string
	for _, loose := range loosePhs {
		if !strutil.Exist(validPhs, loose) {
			invalidPhs = append(invalidPhs, loose)
		}
	}
	return invalidPhs
}
