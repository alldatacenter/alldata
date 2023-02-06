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

package structparser

// BottomUpWalk 自底向上遍历 `node`
func BottomUpWalk(node Node, f func(curr Node, children []Node)) {
	switch v := node.(type) {
	case *BoolNode:
		f(node, nil)
	case *IntNode:
		f(node, nil)
	case *Int64Node:
		f(node, nil)
	case *FloatNode:
		f(node, nil)
	case *StringNode:
		f(node, nil)
	case *SliceNode:
		BottomUpWalk(v.value, f)
		f(v, []Node{v.value})
	case *MapNode:
		BottomUpWalk(v.key, f)
		BottomUpWalk(v.value, f)
		f(v, []Node{v.key, v.value})
	case *PtrNode:
		BottomUpWalk(v.inner, f)
		f(v, []Node{v.inner})
	case *StructNode:
		for _, field := range v.fields {
			BottomUpWalk(field, f)
		}
		f(v, v.fields)
	default:
		panic("unreachable")
	}
}
