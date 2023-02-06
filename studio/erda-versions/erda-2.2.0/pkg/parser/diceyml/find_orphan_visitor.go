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

// 找出没有被其他service依赖的后端service， 前端service不考虑（也就是有expose的service）
package diceyml

type FindOrphanVisitor struct {
	DefaultVisitor
	orphan []string
}

func NewFindOrphanVisitor() DiceYmlVisitor {
	return &FindOrphanVisitor{}
}

func (o *FindOrphanVisitor) VisitServices(v DiceYmlVisitor, obj *Services) {
	orphan := []string{}
	depends := map[string]struct{}{}
	for _, srv := range *obj {
		for _, name := range srv.DependsOn {
			depends[name] = struct{}{}
		}
	}
	for name, srv := range *obj {
		_, ok := depends[name]
		if !ok && srv.Expose == nil {
			orphan = append(orphan, name)
		}
	}
	o.orphan = orphan
}

func FindOrphan(obj *Object) []string {
	visitor := NewFindOrphanVisitor()
	obj.Accept(visitor)
	return visitor.(*FindOrphanVisitor).orphan
}
