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

import (
	"fmt"
	"reflect"

	"github.com/erda-project/erda/pkg/strutil"
)

// NodeType Node 类型
type NodeType int

const (
	// Bool BoolNode
	Bool NodeType = iota
	// Int IntNode
	Int
	// Int64 Int64Node
	Int64
	// Float FloatNode
	Float
	// Slice SliceNode
	Slice
	// Map MapNode
	Map
	// Ptr PtrNode
	Ptr
	// String StringNode
	String
	// Struct StructNode
	Struct
)

// Node 所有 Node struct 的公共方法
type Node interface {
	// Name node 的 field name
	Name() string

	// Comment node 的 field comment
	Comment() string

	// Type node 所属 NodeType
	Type() NodeType

	// TypeName node 类型名，e.g. int, time.Time
	TypeName() string

	// Tag node 的 field tag
	Tag() reflect.StructTag

	// String impl fmt.Stringer
	String() string

	// Extra node 中用于放置额外信息的地方，可以用于 Walk 时存放信息
	Extra() *interface{}

	// Compress 压缩 node 中的 Ptr,
	// e.g.
	// ptr(ptr(int)) -> int
	Compress() Node

	basenode() *baseNode
}

type baseNode struct {
	// tag struct field tag
	tag reflect.StructTag

	// comment struct field comment
	comment string

	// isStructElem 是否为 struct field
	isStructElem bool

	// anonymous 是否为匿名 field ，在 isStructElem=true 时有意义
	anonymous bool

	// name struct field name
	name string

	// typename
	// e.g. 'time.Time'
	typename string

	// extra 字段用来给 structparser 的使用者来存放额外信息
	extra interface{}
}

// BoolNode bool node
type BoolNode struct{ baseNode }

// IntNode int node
type IntNode struct{ baseNode }

// Int64Node int64 node
type Int64Node struct{ baseNode }

// FloatNode float node
type FloatNode struct{ baseNode }

// SliceNode slice node, include slice & array
type SliceNode struct {
	baseNode
	value Node
}

// MapNode map node
type MapNode struct {
	baseNode
	key   Node
	value Node
}

// PtrNode pointer node
type PtrNode struct {
	baseNode
	inner Node
}

// StringNode string node
type StringNode struct{ baseNode }

// StructNode struct node
type StructNode struct {
	baseNode
	fields []Node
}

func (n *baseNode) Tag() reflect.StructTag { return n.tag }
func (n *baseNode) Comment() string        { return n.comment }
func (n *baseNode) Name() string           { return n.name }
func (n *baseNode) Extra() *interface{}    { return &n.extra }
func (n *baseNode) TypeName() string       { return n.typename }
func (n *baseNode) basenode() *baseNode    { return n }

func (*BoolNode) Type() NodeType   { return Bool }
func (*IntNode) Type() NodeType    { return Int }
func (*Int64Node) Type() NodeType  { return Int64 }
func (*FloatNode) Type() NodeType  { return Float }
func (*SliceNode) Type() NodeType  { return Slice }
func (*MapNode) Type() NodeType    { return Map }
func (*PtrNode) Type() NodeType    { return Ptr }
func (*StringNode) Type() NodeType { return String }
func (*StructNode) Type() NodeType { return Struct }

func (n *BoolNode) String() string {
	return fmt.Sprintf("BoolNode<%s>", n.Name())
}
func (n *IntNode) String() string {
	return fmt.Sprintf("IntNode<%s>", n.Name())
}
func (n *Int64Node) String() string {
	return fmt.Sprintf("Int64Node<%s>", n.Name())
}
func (n *FloatNode) String() string {
	return fmt.Sprintf("FloatNode<%s>", n.Name())
}
func (n *SliceNode) String() string {
	return strutil.Concat("[ ", n.value.String(), " ]", fmt.Sprintf("<%s>", n.Name()))
}
func (n *MapNode) String() string {
	return strutil.Concat("map[", n.key.String(), "]", n.value.String(), fmt.Sprintf("<%s>", n.Name()))
}
func (n *PtrNode) String() string {
	return strutil.Concat("*(", n.inner.String(), ")", fmt.Sprintf("<%s>", n.Name()))
}
func (n *StringNode) String() string {
	return fmt.Sprintf("StringNode<%s>", n.Name())
}
func (n *StructNode) String() string {
	fieldsstr := []string{}
	for i := range n.fields {
		fieldsstr = append(fieldsstr, n.fields[i].String())
	}
	return strutil.Concat(n.Name(), "{ ", strutil.Join(fieldsstr, ", "), " }")
}

// Compress struct compress
// 如果 field 是 anonymous 的，且其类型为 struct ，那么将这个 field 的子 fields ，
// 扩展到当前 struct
func (n *StructNode) Compress() Node {
	newfields := []Node{}
	for _, field := range n.fields {
		switch v := field.(type) {
		case *StructNode:
			if !v.anonymous {
				newfields = append(newfields, v.Compress())
				continue
			}
			for _, ifield := range v.fields {
				ifieldCompressed := ifield.Compress()
				if structifield, ok := ifieldCompressed.(*StructNode); ok &&
					structifield.anonymous {
					newfields = append(newfields, structifield.fields...)
				} else {
					newfields = append(newfields, ifieldCompressed)
				}
			}
		default:
			newfields = append(newfields, v.Compress())
		}
	}
	return &StructNode{baseNode: n.baseNode, fields: newfields}
}

// Compress 压缩 PtrNode， 并且将 `inner` 的属性设置为 ptrnode 的属性
func (n *PtrNode) Compress() Node {
	inner := n.inner.Compress()
	base := inner.basenode()

	// inner 继承 ptr 的属性
	base.tag = n.tag
	base.comment = n.comment
	base.isStructElem = n.isStructElem
	base.anonymous = n.anonymous
	base.name = n.name
	base.extra = n.extra
	// typename 不需要继承
	return inner
}

func (n *SliceNode) Compress() Node {
	return &SliceNode{
		baseNode: n.baseNode,
		value:    n.value.Compress(),
	}

}
func (n *MapNode) Compress() Node {
	return &MapNode{baseNode: n.baseNode, key: n.key, value: n.value.Compress()}
}
func (n *BoolNode) Compress() Node {
	return n
}
func (n *IntNode) Compress() Node {
	return n
}
func (n *Int64Node) Compress() Node {
	return n
}
func (n *FloatNode) Compress() Node {
	return n
}
func (n *StringNode) Compress() Node {
	return n
}

type constructCtx struct {
	deep         int
	tag          reflect.StructTag
	name         string
	comment      string
	isStructElem bool
	anonymous    bool
	typename     string
}

func (c *constructCtx) basenode() baseNode {
	return baseNode{
		tag:          c.tag,
		comment:      c.comment,
		name:         c.name,
		isStructElem: c.isStructElem,
		anonymous:    c.anonymous,
		typename:     c.typename,
	}
}

func newNode(ctx constructCtx, t reflect.Type) (n Node) {
	defer func() {
		if r := recover(); r != nil {
			n = &StructNode{baseNode: ctx.basenode(), fields: nil}
		}
	}()
	if ctx.deep > 20 {
		return &BoolNode{ctx.basenode()}
	}
	ctx.deep += 1
	value := reflect.New(t).Interface()
	switch t.Kind() {
	case reflect.Bool:
		return &BoolNode{ctx.basenode()}
	case reflect.Uint, reflect.Uint8, reflect.Uint16, reflect.Uint32,
		reflect.Int, reflect.Int8, reflect.Int16, reflect.Int32:
		return &IntNode{ctx.basenode()}
	case reflect.Int64, reflect.Uint64:
		return &Int64Node{ctx.basenode()}
	case reflect.Uintptr:
		panic("not support")
	case reflect.Float32, reflect.Float64:
		return &FloatNode{ctx.basenode()}
	case reflect.Complex64:
		panic("not support")
	case reflect.Complex128:
		panic("not support")
	case reflect.Slice, reflect.Array:
		elem := t.Elem()
		return &SliceNode{ctx.basenode(), newNode(constructCtx{deep: ctx.deep}, elem)}
	case reflect.Chan:
		panic("not support")
	case reflect.Func:
		panic("not support")
	case reflect.Interface:
		return &StructNode{ctx.basenode(), nil}
	case reflect.Map:
		k := newNode(constructCtx{deep: ctx.deep}, t.Key())
		v := newNode(constructCtx{deep: ctx.deep}, t.Elem())
		return &MapNode{ctx.basenode(), k, v}
	case reflect.Ptr:
		inner := newNode(constructCtx{deep: ctx.deep}, t.Elem())
		return &PtrNode{ctx.basenode(), inner}
	case reflect.String:
		return &StringNode{ctx.basenode()}
	case reflect.Struct:
		idx := t.NumField()
		fields := []Node{}
		for i := 0; i < idx; i++ {
			field := t.Field(i)
			newctx := constructCtx{
				deep:         ctx.deep,
				tag:          field.Tag,
				name:         field.Name,
				isStructElem: true,
				anonymous:    field.Anonymous,
				typename:     field.Type.String(),
				comment:      getComment(value, field.Name),
			}
			newnode := newNode(newctx, field.Type)
			fields = append(fields, newnode)
		}
		return &StructNode{ctx.basenode(), fields}
	}
	panic("unreachable")
}
