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

// 找出依赖却不存在的 service
package diceyml

import (
	"fmt"
)

type FindLackServiceVisitor struct {
	DefaultVisitor
	lack ValidateError
}

func NewFindLackServiceVisitor() DiceYmlVisitor {
	return &FindLackServiceVisitor{
		lack: ValidateError{},
	}
}

func (o *FindLackServiceVisitor) VisitServices(v DiceYmlVisitor, obj *Services) {
	for srvName, srv := range *obj {
		for _, name := range srv.DependsOn {
			if _, ok := (*obj)[name]; !ok {
				o.lack[yamlHeaderRegexWithUpperHeader([]string{"services", srvName}, "depends_on")] = fmt.Errorf("found lacked services: %v", name)
			}
		}
	}
}

func FindLackService(obj *Object) ValidateError {
	visitor := NewFindLackServiceVisitor()
	obj.Accept(visitor)
	return visitor.(*FindLackServiceVisitor).lack
}
