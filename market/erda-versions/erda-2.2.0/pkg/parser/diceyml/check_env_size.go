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

package diceyml

import (
	"fmt"
)

type CheckEnvSizeVisitor struct {
	DefaultVisitor
	collectErrors ValidateError
}

func NewCheckEnvSizeVisitor() DiceYmlVisitor {
	return &CheckEnvSizeVisitor{
		collectErrors: ValidateError{},
	}
}

func (o *CheckEnvSizeVisitor) VisitObject(v DiceYmlVisitor, obj *Object) {
	for k, v := range obj.Envs {
		if toolong(v) {
			o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{"envs"}, k)] = fmt.Errorf("global env too long(more than 20000 chars, ~20kb): [%s]", k)
		}
	}
}

func (o *CheckEnvSizeVisitor) VisitService(v DiceYmlVisitor, obj *Service) {
	for k, v := range obj.Envs {
		if toolong(v) {
			o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentService, "envs"}, k)] = fmt.Errorf("env too long(more than 20000 chars, ~20kb): %s", k)
		}
	}
}

func toolong(s string) bool {
	return len(s) > 20000 // ~20kb
}

func CheckEnvSize(obj *Object) ValidateError {
	visitor := NewCheckEnvSizeVisitor()
	obj.Accept(visitor)
	return visitor.(*CheckEnvSizeVisitor).collectErrors
}
