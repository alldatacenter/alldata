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

package pipelineyml

import (
	"regexp"
	"strings"

	"github.com/pkg/errors"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/pkg/expression"
	"github.com/erda-project/erda/pkg/strutil"
)

// SecretVisitor 占位符统一在 yaml 中进行文本渲染，不渲染结构体，保证引号统一处理
type SecretVisitor struct {
	data                 []byte
	secrets              map[string]string
	recursiveRenderTimes int
}

func NewSecretVisitor(data []byte, secrets map[string]string, recursiveRenderTimes int) *SecretVisitor {
	v := SecretVisitor{}
	v.data = data
	v.secrets = secrets
	v.recursiveRenderTimes = recursiveRenderTimes
	if v.recursiveRenderTimes < 1 {
		// set default value
		v.recursiveRenderTimes = 2
	}
	return &v
}

const looseSecretRegExpr = `\(\(\s*([^()]*)\s*\)\)`
const validSecretRegExpr = `\(\(([^()\s]+)\)\)`

var looseSecretRegexp = regexp.MustCompile(looseSecretRegExpr)
var validSecretRegexp = regexp.MustCompile(validSecretRegExpr)

// yaml 全局文本替换
func (v *SecretVisitor) Visit(s *Spec) {
	var (
		replaced = v.data
	)
	for i := 0; i < v.recursiveRenderTimes; i++ {
		// only render existing secrets, validate together later
		replaced, _ = RenderSecrets(v.data, v.secrets)
		v.data = replaced
	}
	// 使用渲染后的 data 反序列化，保证不丢失 hint，例如: !!str
	if err := yaml.Unmarshal(replaced, s); err != nil {
		s.appendError(err)
	}
}

func unwrapSecret(wrappedSecret string) string {
	return strutil.TrimSuffixes(strutil.TrimPrefixes(wrappedSecret, "(("), "))")
}

func unwrapSecretV1(wrappedSecret string) string {
	return strutil.TrimSuffixes(strutil.TrimPrefixes(wrappedSecret, expression.LeftPlaceholder), expression.RightPlaceholder)
}

// RenderSecrets 将 ((xxx)) 替换为 secrets 中的值
//
// input:   ((a))((b))((c))
// secrets: a=1,b=2
// result:  12((c)) err: secret not found: ((c))
func RenderSecrets(input []byte, secrets map[string]string) ([]byte, error) {
	var tmpS Spec

	// find invalid secret
	looseSecrets := make(map[string]struct{})
	for _, sec := range looseSecretRegexp.FindAllString(string(input), -1) {
		looseSecrets[sec] = struct{}{}
	}
	validSecrets := make(map[string]struct{})
	for _, sec := range validSecretRegexp.FindAllString(string(input), -1) {
		validSecrets[sec] = struct{}{}
	}
	for sec := range looseSecrets {
		if _, ok := validSecrets[sec]; !ok {
			tmpS.appendError(errors.Errorf("invalid secret: %q (must match regexp `%s`)", sec, validSecretRegExpr))
		}
	}

	// replace (())
	replaced := validSecretRegexp.ReplaceAllStringFunc(string(input), func(wrappedSec string) string {
		value, ok := secrets[unwrapSecret(wrappedSec)]
		if !ok {
			tmpS.appendError(errors.Errorf("secret not found: %s", wrappedSec))
			return wrappedSec
		}
		return value
	})

	// replace ${{ configs.key }}
	for k, v := range secrets {
		replaced = strings.ReplaceAll(replaced, expression.LeftPlaceholder+" "+expression.Configs+"."+k+" "+expression.RightPlaceholder, expression.ReplaceRandomParams(v))
	}

	return []byte(replaced), nil
}
