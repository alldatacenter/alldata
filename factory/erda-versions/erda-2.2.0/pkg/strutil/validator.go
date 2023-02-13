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
	"fmt"
	"regexp"
	"unicode"
)

// Validator defines a validator function.
// User can extend validator in their own packages.
type Validator func(s string) error

// Validate validate `s` with composed validators and return error if have
func Validate(s string, validators ...Validator) error {
	for _, v := range validators {
		if err := v(s); err != nil {
			return err
		}
	}
	return nil
}

// MinLenValidator 校验 `s` 是否符合最小长度要求
func MinLenValidator(minLen int) Validator {
	return func(s string) error {
		if len(s) < minLen {
			if minLen == 1 {
				return fmt.Errorf("cannot be empty")
			}
			return fmt.Errorf("less than min length: %d", minLen)
		}
		return nil
	}
}

// MaxLenValidator 校验 `s` 是否超过最大长度
func MaxLenValidator(maxLen int) Validator {
	return func(s string) error {
		if len(s) > maxLen {
			return fmt.Errorf("over max length: %d", maxLen)
		}
		return nil
	}
}

func MaxRuneCountValidator(maxLen int) Validator {
	return func(s string) error {
		if len([]rune(s)) > maxLen {
			return fmt.Errorf("over max rune count: %d", maxLen)
		}
		return nil
	}
}

var envKeyRegexp = regexp.MustCompilePOSIX(`^[a-zA-Z_]+[a-zA-Z0-9_]*$`)

// EnvKeyValidator 检验 `s` 是否符合 linux env key 规范
var EnvKeyValidator Validator = func(s string) error {
	valid := envKeyRegexp.MatchString(s)
	if !valid {
		return fmt.Errorf("illegal env key, validated by regexp: %s", envKeyRegexp.String())
	}
	return nil
}

// EnvValueLenValidator 校验 `s` 是否超过 linux env value 最大长度
var EnvValueLenValidator = MaxLenValidator(128 * 1024)

// NoChineseValidator 校验 `s` 是否包含中文字符
var NoChineseValidator Validator = func(s string) error {
	var chineseCharacters []string
	for _, runeValue := range s {
		if unicode.Is(unicode.Han, runeValue) {
			chineseCharacters = append(chineseCharacters, string(runeValue))
		}
	}
	if len(chineseCharacters) > 0 {
		return fmt.Errorf("found %d chinese characters: %s", len(chineseCharacters),
			Join(chineseCharacters, " ", true))
	}
	return nil
}

// AlphaNumericDashUnderscoreValidator 正则表达式校验，只能以大小写字母或数字开头，支持大小写字母、数字、中划线、下划线、点
var AlphaNumericDashUnderscoreValidator Validator = func(s string) error {
	exp := `^([A-Za-z0-9][-A-Za-z0-9_.]*)?[A-Za-z0-9]$`
	valid := regexp.MustCompile(exp).MatchString(s)
	if !valid {
		return fmt.Errorf("valid regexp: %s", exp)
	}
	return nil
}

// K8sNodeSelectorKeyValidator k8s 节点选择正则表达式校验
var K8sNodeSelectorMatchValidator = AlphaNumericDashUnderscoreValidator
