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

// Use:
// 1. 调用方实现 dag.NamedNode 接口
// 2. 使用 dag.New(node1, node2, ...) 创建 DAG
// 3. 请看测试用例
package dag

import (
	"strings"

	"github.com/pkg/errors"
)

// DAG 代表 有向无环图.
type DAG struct {
	// Nodes represents map of name to Node in DAG.
	Nodes map[string]*defaultNode
	// allowMarkArbitraryNodesAsDone 允许 DAG 中任意一个节点标记为已完成；
	// 否则，会校验前置路径上所有节点都需要为已完成
	allowMarkArbitraryNodesAsDone bool
	// allowNotCheckCycle 允许不检查环(性能问题)
	allowNotCheckCycle bool
}

// NamedNode 方便用户使用，仅在创建 DAG 时使用
type NamedNode interface {
	// NodeName 需要唯一标识一个节点
	NodeName() string
	// PrevNodeNames 表示与当前节点直接相连的前置节点
	PrevNodeNames() []string
}

// Node 代表 DAG 的节点
type Node interface {
	NamedNode
	PrevNodes() []Node
	NextNodes() []Node
	NextNodeNames() []string
}

type Option func(*DAG)

func WithAllowMarkArbitraryNodesAsDone(allow bool) Option {
	return func(g *DAG) {
		g.allowMarkArbitraryNodesAsDone = allow
	}
}

func WithAllowNotCheckCycle(allow bool) Option {
	return func(g *DAG) {
		g.allowNotCheckCycle = allow
	}
}

// New 返回一个 DAG
// @nodes: map[节点名]NamedNode
func New(nodes []NamedNode, ops ...Option) (*DAG, error) {
	g := DAG{
		Nodes: map[string]*defaultNode{},
	}

	// apply ops
	for _, op := range ops {
		op(&g)
	}

	// 初始化 DAG
	for _, n := range nodes {
		if err := g.addNode(n); err != nil {
			return nil, errors.Errorf("failed to add node %q to DAG, err: %v", n.NodeName(), err)
		}
	}

	// Node 之间增加 Link
	for _, n := range g.Nodes {
		for _, prevNodeName := range n.PrevNodeNames() {
			if err := g.addLink(n, prevNodeName); err != nil {
				return nil, errors.Errorf("failed to add link between %q and %q, err: %v", n.NodeName(), prevNodeName, err)
			}
		}
	}
	return &g, nil
}

func (g *DAG) addNode(n NamedNode) error {
	if _, ok := g.Nodes[n.NodeName()]; ok {
		return errors.Errorf("duplicate node: %s", n.NodeName())
	}
	g.Nodes[n.NodeName()] = &defaultNode{name: n.NodeName(), prevNodeNames: n.PrevNodeNames()}
	return nil
}

func (g *DAG) addLink(n Node, prevNodeName string) error {
	// find prevNode
	prevNode, ok := g.Nodes[prevNodeName]
	if !ok {
		return errors.Errorf("node %q depends on an nonexistent node %q", n.NodeName(), prevNodeName)
	}
	// link two nodes
	if err := g.linkTwoNodes(prevNode, n); err != nil {
		return errors.Errorf("failed to create link from %q to %q, err: %v", prevNode.NodeName(), n.NodeName(), err)
	}
	return nil
}

func (g *DAG) linkTwoNodes(from, to Node) error {
	if !g.allowNotCheckCycle {
		if err := validateNodes(from, to); err != nil {
			return err
		}
	}
	// link node
	to.(*defaultNode).prevNodes = append(to.(*defaultNode).prevNodes, from.(*defaultNode))
	from.(*defaultNode).nextNodes = append(from.(*defaultNode).nextNodes, to.(*defaultNode))
	return nil
}

func validateNodes(from, to Node) error {
	// check for self cycle
	if from.NodeName() == to.NodeName() {
		return errors.Errorf("self cycle detected: node %q depends on itself", from.NodeName())
	}

	// check for cycle
	path := []string{to.NodeName(), from.NodeName()}
	if err := visit(to, from.PrevNodes(), path); err != nil {
		return errors.Errorf("cycle detected: %v", err)
	}

	return nil
}

func visit(startNode Node, prev []Node, visitedPath []string) error {
	for _, n := range prev {
		visitedPath = append(visitedPath, n.NodeName())
		if n.NodeName() == startNode.NodeName() {
			return errors.Errorf(getVisitedPath(visitedPath))
		}
		if err := visit(startNode, n.PrevNodes(), visitedPath); err != nil {
			return err
		}
	}
	return nil
}

func getVisitedPath(path []string) string {
	// reverse the path since we traversed the DAG using prev pointers.
	for i := len(path)/2 - 1; i >= 0; i-- {
		opp := len(path) - 1 - i
		path[i], path[opp] = path[opp], path[i]
	}
	return strings.Join(path, " -> ")
}

type defaultNode struct {
	name          string
	prevNodeNames []string

	prevNodes []*defaultNode
	nextNodes []*defaultNode
}

func (n *defaultNode) NodeName() string {
	return n.name
}

func (n *defaultNode) PrevNodeNames() []string {
	return n.prevNodeNames
}

func (n *defaultNode) PrevNodes() []Node {
	r := make([]Node, 0, len(n.prevNodes))
	for _, prev := range n.prevNodes {
		r = append(r, prev)
	}
	return r
}

func (n *defaultNode) NextNodeNames() []string {
	r := make([]string, 0, len(n.nextNodes))
	for _, next := range n.nextNodes {
		r = append(r, next.name)
	}
	return r
}

func (n *defaultNode) NextNodes() []Node {
	r := make([]Node, 0, len(n.nextNodes))
	for _, next := range n.nextNodes {
		r = append(r, next)
	}
	return r
}
