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

package router

import (
	"fmt"
	"sort"
	"strings"
)

type (
	// Router .
	Router struct {
		tree *node
	}
	// KeyValue .
	KeyValue struct {
		Key   string `json:"key"`
		Value string `json:"value"`
	}
	node struct {
		parent      *node
		children    []*node
		childrenMap map[string]*node
		kind        kind
		key         string // name prefix 、key、value
		target      interface{}
	}
	kind uint8
)

const (
	skind kind = iota
	akind
	kkind
	vkind
)

// New returns a new Router instance.
func New() *Router {
	return &Router{
		tree: &node{},
	}
}

// Add .
func (r *Router) Add(name string, kvs []*KeyValue, target interface{}) error {
	if len(name) == 0 || name == "*" {
		r.insertName("*", akind, kvs, target)
		return nil
	}
	r.insertName("*", akind, nil, nil)
	if name[0] != '*' {
		name = "*" + name
	}
	for i, l := 1, len(name); i < l; i++ {
		if name[i] == '*' {
			r.insertName(name[:i], skind, nil, nil)
			r.insertName(name[:i+1], akind, nil, nil)
		}
	}
	r.insertName(name, skind, kvs, target)
	return nil
}

// TODO check conflict
func (r *Router) insertName(name string, t kind, kvs []*KeyValue, target interface{}) error {
	cn := r.tree // cuerrent node
	search := name
	for {
		sl, pl, l := len(search), len(cn.key), 0

		max := pl
		if sl < max {
			max = sl
		}
		for ; l < max && search[l] == cn.key[l]; l++ {
		}
		if l == 0 {
			// At root node
			cn.key = search
			cn.kind = t
			cn.addKeyValues(kvs, target)
		} else if l < pl {
			// Split node
			n := newNode(cn.kind, cn.key[l:], cn, cn.children, cn.target)

			// Update parent path for all children to new node
			for _, child := range cn.children {
				child.parent = n
			}

			// Reset parent node
			cn.kind = skind
			cn.key = cn.key[:l]
			cn.children = nil
			cn.target = nil

			cn.addChild(n)

			if l == sl {
				cn.kind = t
				cn.addKeyValues(kvs, target)
			} else {
				// Create child node
				n = newNode(t, search[l:], cn, nil, nil)
				n.addKeyValues(kvs, target)
				cn.addChild(n)
			}
		} else if l < sl {
			search = search[l:]
			c := cn.findChildWithKey0(search[0])
			if c != nil {
				// Go deeper
				cn = c
				continue
			}
			// Create child node
			n := newNode(t, search, cn, nil, nil)
			n.addKeyValues(kvs, target)
			cn.addChild(n)
		} else {
			cn.addKeyValues(kvs, target)
		}
		return nil
	}
}

func newNode(t kind, key string, p *node, c []*node, target interface{}) *node {
	return &node{
		parent:   p,
		children: c,
		kind:     t,
		key:      key,
		target:   target,
	}
}

func (n *node) addChild(c *node) {
	n.children = append(n.children, c)
}

func (n *node) findChildWithKey0(l byte) *node {
	for _, c := range n.children {
		if c.key[0] == l {
			return c
		}
	}
	return nil
}

func (n *node) addKeyValues(kvs []*KeyValue, target interface{}) {
	if len(kvs) <= 0 {
		if n.target == nil {
			n.target = target
		}
	} else {
		n.insertKeyValues(kvs, target)
	}
}

func (n *node) insertKeyValues(kvs []*KeyValue, target interface{}) {
	cn := n
	for i, kv := range kvs {
		kn := cn.findChildWithKey(kv.Key)
		if kn == nil {
			kn = newNode(kkind, kv.Key, cn, nil, nil)
			if cn.childrenMap == nil {
				cn.childrenMap = make(map[string]*node)
			}
			cn.childrenMap[kv.Key] = kn
			if i == 0 {
				cn.children = append(cn.children, kn)
			}
		}
		cn = kn.findChildWithKey(kv.Value)
		if cn == nil {
			cn = newNode(vkind, kv.Value, kn, nil, nil)
			if kn.childrenMap == nil {
				kn.childrenMap = make(map[string]*node)
			}
			kn.childrenMap[kv.Value] = cn
		}
	}
	if cn.target == nil {
		cn.target = target
	}
}

func (n *node) findChildWithKey(key string) *node {
	if n.childrenMap != nil {
		return n.childrenMap[key]
	}
	return nil
}

// SprintTree .
func (r *Router) SprintTree(verbose bool) string {
	sb := &strings.Builder{}
	r.tree.printTree(sb, "", false, verbose)
	return sb.String()
}

// PrintTree .
func (r *Router) PrintTree(verbose bool) {
	sb := &strings.Builder{}
	r.tree.printTree(sb, "", false, verbose)
	fmt.Print(sb)
}

func (n *node) printTree(sb *strings.Builder, pfx string, tail, verbose bool) {
	p := prefix(tail, pfx, "└── ", "├── ")
	if verbose {
		sb.WriteString(fmt.Sprintf("%s%s: kind=%d, target=%v,  node=%p, parent=%p\n", p, n.key, n.kind, n.target, n, n.parent))
	} else {
		sb.WriteString(fmt.Sprintf("%s%s: kind=%d, target=%v\n", p, n.key, n.kind, n.target))
	}
	p = prefix(tail, pfx, "    ", "│   ")
	if n.children != nil {
		children, l := n.children, len(n.children)
		for i := 0; i < l; i++ {
			children[i].printTree(sb, p, i == l-1, verbose)
		}
	} else {
		var keys []string
		for key := range n.childrenMap {
			keys = append(keys, key)
		}
		sort.Strings(keys)
		i, l := 0, len(n.childrenMap)
		for _, key := range keys {
			node := n.childrenMap[key]
			node.printTree(sb, p, i == l-1, verbose)
			i++
		}
	}
}

func prefix(tail bool, p, on, off string) string {
	if tail {
		return fmt.Sprintf("%s%s", p, on)
	}
	return fmt.Sprintf("%s%s", p, off)
}

// Find lookup target registered for name and key/values.
func (r *Router) Find(name string, kvs map[string]string) interface{} {
	cn := r.tree // Current node as root
	target, _ := cn.find("*"+name, kvs)
	return target
}

func (n *node) find(name string, kvs map[string]string) (interface{}, bool) {
	sl, pl, l := len(name), len(n.key), 0
	max := pl
	if sl < max {
		max = sl
	}
	for ; l < max && name[l] == n.key[l]; l++ {
	}
	if l != pl {
		if n.kind == akind {
			// slow search
			for i := 0; i < sl; i++ {
				if n := n.findChild(name[i], skind); n != nil {
					target, ok := n.find(name[i:], kvs)
					if ok {
						return target, ok
					}
				}
			}
		}
		return nil, false
	}
	name = name[l:]
	if len(name) == 0 {
		if target, ok := n.findKeyValues(kvs); ok {
			return target, ok
		}
		if n := n.findChildByKind(akind); n != nil {
			if target, ok := n.findKeyValues(kvs); ok {
				return target, ok
			}
		}
		return nil, false
	}

	if n := n.findChild(name[0], skind); n != nil {
		if target, ok := n.find(name, kvs); ok {
			return target, ok
		}
	}
	if target, ok := n.findKeyValues(kvs); ok {
		return target, ok
	}

	if n := n.findChildByKind(akind); n != nil {
		if target, ok := n.find(name, kvs); ok {
			return target, ok
		}
		if target, ok := n.findKeyValues(kvs); ok {
			return target, ok
		}
	}
	return nil, false
}

func (n *node) findKeyValues(kvs map[string]string) (interface{}, bool) {
	if n.childrenMap == nil || len(n.childrenMap) <= 0 {
		if /*((kvs == nil || len(kvs) == 0) || n.kind == akind) &&*/ n.target != nil {
			return n.target, true
		}
		return nil, false
	}
	if (kvs == nil || len(kvs) == 0) && n.target != nil {
		return n.target, true
	}
	for _, kn := range n.childrenMap {
		val, ok := kvs[kn.key]
		if !ok {
			continue
		}
		vn := kn.findChildWithKey(val)
		if vn == nil {
			continue
		}
		if target, ok := vn.findKeyValues(kvs); ok {
			return target, ok
		}
		if vn.target != nil {
			return vn.target, true
		}
	}
	if n.kind < kkind && n.target != nil {
		return n.target, true
	}
	return nil, false
}

func (n *node) findChildByKind(t kind) *node {
	for _, c := range n.children {
		if c.kind == t {
			return c
		}
	}
	return nil
}

func (n *node) findChild(k byte, t kind) *node {
	for _, c := range n.children {
		if c.key[0] == k && c.kind == t {
			return c
		}
	}
	return nil
}
