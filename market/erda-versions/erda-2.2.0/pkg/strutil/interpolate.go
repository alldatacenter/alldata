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
	"strings"

	"github.com/pkg/errors"
)

// FirstCustomPlaceholder 找出字符串 s 中的占位符.
// left 是占位符标志的左部, right 是占位符标志的右部,
// 如 ${expression}, left 为 '${', right 为 '}',
// ${{ expression }}, left 为 '${{', right 为 '}}'.
// 注意占位符表达式中不能出现 '\n', '\r' 等字符.
// 返回值 expression 为占位符表达式, indexStart 为占位符起始位置索引,
// indexEnd 为占位符结束位置索引.
// 当 err==nil && indexStart==0 && indexEnd==0 时, 表示 s 中不存在任何给定的占位符.
func FirstCustomPlaceholder(s, left, right string) (expr string, indexStart, indexEnd int, err error) {
	leftLen := len(left)
	rightLen := len(right)
	if leftLen == 0 {
		return "", 0, 0, errors.New("left is invalid")
	}
	if rightLen == 0 {
		return "", 0, 0, errors.New("right is invalid")
	}
	if len(s) <= leftLen+rightLen {
		return "", 0, 0, nil
	}

	indexStart = strings.Index(s, left)
	if indexStart < 0 {
		return "", 0, 0, nil
	}
	indexEnd = strings.Index(s[indexStart+leftLen:], right)
	if indexEnd < 0 {
		return "", 0, 0, nil
	}
	expr = s[indexStart+leftLen : indexStart+leftLen+indexEnd]
	if bytes.ContainsRune([]byte(expr), '\n') ||
		bytes.ContainsRune([]byte(expr), '\r') {
		return "", 0, 0, errors.New("invalid literal: '\\n' or '\\r' in string")
	}

	return strings.TrimSpace(expr), indexStart, indexStart + leftLen + indexEnd + rightLen, nil
}

func FirstCustomExpression(s, left, right string, f func(string) bool) (expr string, indexStart, indexEnd int, err error) {
	leftLen := len(left)
	rightLen := len(right)
	if leftLen == 0 {
		return "", 0, 0, errors.New("left is invalid")
	}
	if rightLen == 0 {
		return "", 0, 0, errors.New("right is invalid")
	}
	if len(s) <= leftLen+rightLen {
		return "", 0, 0, nil
	}

	for i := 0; i < len(s)-leftLen-rightLen+1; i++ {
		if s[i:i+leftLen] == left {
			for j := i + leftLen; j < len(s)-rightLen+1; j++ {
				if s[j:j+rightLen] == right {
					placeholder := s[i+leftLen : j]
					placeholder = strings.TrimPrefix(placeholder, " ")
					placeholder = strings.TrimSuffix(placeholder, " ")
					if f(placeholder) {
						if bytes.ContainsRune([]byte(placeholder), '\n') ||
							bytes.ContainsRune([]byte(placeholder), '\r') {
							return "", 0, 0, errors.New("invalid literal: '\\n' or '\\r' in string")
						}
						return placeholder, i, j + rightLen, nil
					}
					break
				}
			}
		}
	}
	return "", 0, 0, nil
}

func Replace(s string, new string, indexStart, indexEnd int) string {
	if len(s) <= indexStart {
		return s
	}
	if len(s) <= indexEnd {
		return s[:indexStart] + new
	}
	return s[:indexStart] + new + s[indexEnd:]
}

// Interpolate 对 s 中的 ${PLACEHOLDER} 或 ${PLACEHOLDER:DEFAULT} 占位符进行插值
func Interpolate(s string, values map[string]string, defaultPrecedence bool, left, right string) (string, error) {
	if values == nil {
		values = make(map[string]string)
	}
	var valuesCopy = make(map[string]string)
	for k, v := range values {
		valuesCopy[k] = v
	}

	if err := InterpolationDereference(valuesCopy, left, right); err != nil {
		return s, err
	}

	for {
		placeholder, indexStart, indexEnd, err := FirstCustomPlaceholder(s, left, right)
		if err != nil {
			return s, err
		}
		if indexStart == indexEnd {
			return s, nil
		}
		kv := strings.Split(placeholder, ":")
		placeholder = strings.TrimSpace(kv[0])
		value, ok := valuesCopy[placeholder]
		if len(kv) > 1 && (!ok || defaultPrecedence) {
			value = strings.TrimSpace(strings.Join(kv[1:], ":"))
		}
		s = s[:indexStart] + value + s[indexEnd:]
	}
}

// InterpolationDereference 渲染出 values 值之间的互相引用.
// 注意 key 中不能出现占位符; 不能出现循环引用.
func InterpolationDereference(values map[string]string, left, right string) error {
	for k := range values {
		// validate the key
		placeholder, indexStart, indexEnd, err := FirstCustomPlaceholder(k, left, right)
		if err != nil {
			return err
		}
		if indexStart != indexEnd {
			return errors.Errorf("placeholder %s in the values' key %s", placeholder, k)
		}

		for {
			placeholder, indexStart, indexEnd, err = FirstCustomPlaceholder(values[k], left, right)
			if err != nil {
				return err
			}
			if indexStart == indexEnd {
				break
			}
			if placeholder == k {
				return errors.Errorf("loop reference in key %s", k)
			}
			values[k] = values[k][:indexStart] + values[placeholder] + values[k][indexEnd:]
		}
	}
	return nil
}
