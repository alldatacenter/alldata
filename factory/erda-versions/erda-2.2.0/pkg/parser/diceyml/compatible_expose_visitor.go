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

type CompatibleExposeVisitor struct {
	DefaultVisitor
}

func NewCompatibleExposeVisitor() DiceYmlVisitor {
	return &CompatibleExposeVisitor{}
}

func (o *CompatibleExposeVisitor) VisitServices(v DiceYmlVisitor, obj *Services) {
	for _, v := range *obj {
		if len(v.Expose) > 0 && len(v.Ports) > 0 {
			v.Ports[0].Expose = true
		}
	}
}

func CompatibleExpose(obj *Object) {
	visitor := NewCompatibleExposeVisitor()
	obj.Accept(visitor)
}
