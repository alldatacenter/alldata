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

// 检查 service name 合法性
package diceyml

import (
	"fmt"
	"regexp"
)

var serviceNameRegex = regexp.MustCompile("^((([a-z0-9]|[a-z0-9][a-z0-9\\-]*[a-z0-9])\\.)*([a-z0-9]|[a-z0-9][a-z0-9\\-]*[a-z0-9]))$")

// var serviceNameMaxLen = 14 // service name 最长长度

var envNameRegex = regexp.MustCompile("^([A-Za-z_][A-Za-z0-9_]*)$")

type ServiceNameVisitor struct {
	DefaultVisitor
	collectErrors ValidateError
}

func NewServiceNameVisitor() DiceYmlVisitor {
	return &ServiceNameVisitor{
		collectErrors: ValidateError{},
	}
}

func (o *ServiceNameVisitor) VisitService(v DiceYmlVisitor, obj *Service) {
	if !serviceNameRegex.MatchString(o.currentService) {
		o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{"services"}, o.currentService)] = fmt.Errorf(
			"service name[%s] not match regex:\n %s, \n e.g: aa.bb-cc.dd-ee", o.currentService, serviceNameRegex)
	}

	for k := range obj.Envs {
		if !envNameRegex.MatchString(k) {
			o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{"services", o.currentService, "envs"}, k)] = fmt.Errorf("env name [%s] not match regex:\n %s", k, envNameRegex)
		}
	}
}

func (o *ServiceNameVisitor) VisitObject(v DiceYmlVisitor, obj *Object) {
	for k := range obj.Envs {
		if !envNameRegex.MatchString(k) {
			o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{"envs"}, k)] = fmt.Errorf("env name [%s] not match regex:\n %s", k, envNameRegex)
		}
	}
}

func ServiceNameCheck(obj *Object) ValidateError {
	visitor := NewServiceNameVisitor()
	obj.Accept(visitor)
	return visitor.(*ServiceNameVisitor).collectErrors
}
