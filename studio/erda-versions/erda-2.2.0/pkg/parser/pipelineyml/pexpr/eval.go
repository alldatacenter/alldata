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
	"strings"

	"gopkg.in/Knetic/govaluate.v3"

	"github.com/erda-project/erda/pkg/strutil"
)

// Eval 递归渲染统一表达式语法里的占位符，再计算表达式结果
// exprStr: 表达式
// placeholderParams: 占位符参数
func Eval(exprStr string, placeholderParams map[string]string) (interface{}, error) {
	// 校验表达式
	invalidPhs := FindInvalidPlaceholders(exprStr)
	if len(invalidPhs) > 0 {
		return nil, fmt.Errorf("invalid expression, found invalid placeholders: %s (must match: %s)", strings.Join(invalidPhs, ", "), PhRe.String())
	}

	// 递归渲染表达式中的占位符
	// 直到渲染完毕或找到不存在的占位符
	for {
		var notFoundPlaceholders []string
		exprStr = strutil.ReplaceAllStringSubmatchFunc(PhRe, exprStr, func(subs []string) string {
			ph := subs[0]    // ${{ configs.key }}
			inner := subs[1] // configs.key

			v, ok := placeholderParams[inner]
			if ok {
				return v
			}
			notFoundPlaceholders = append(notFoundPlaceholders, ph)
			return exprStr
		})
		if len(notFoundPlaceholders) > 0 {
			return nil, fmt.Errorf("invalid expression, not found placeholders: %s", strings.Join(notFoundPlaceholders, ", "))
		}
		// 没有需要替换的占位符，则退出渲染
		if !PhRe.MatchString(exprStr) {
			break
		}
	}

	// 计算表达式
	expr, err := govaluate.NewEvaluableExpression(exprStr)
	if err != nil {
		return nil, fmt.Errorf("invalid expression: %s, err: %v", exprStr, err)
	}
	strParams := make(map[string]interface{}, len(placeholderParams))
	for k, v := range placeholderParams {
		strParams[k] = v
	}
	result, err := expr.Evaluate(strParams)
	if err != nil {
		return nil, fmt.Errorf("failed to evaluate expr, expr: %s, err: %v", exprStr, err)
	}

	return result, nil
}
