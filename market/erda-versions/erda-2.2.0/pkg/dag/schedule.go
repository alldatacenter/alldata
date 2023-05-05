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

package dag

import (
	"sort"

	"github.com/pkg/errors"
)

// GetSchedulable 根据 已完成的节点名 返回可执行的 节点 ([]Node).
func (g *DAG) GetSchedulable(finishes ...string) (map[string]Node, error) {
	roots := g.getRoots()
	finishesMap, err := g.toMap(finishes...)
	if err != nil {
		return nil, err
	}

	result := make(map[string]Node)

	visited := map[string]Node{}

	// 遍历每个顶点
	for _, root := range roots {
		schedulable := findSchedulable(root, visited, finishesMap)
		for _, n := range schedulable {
			result[n] = g.Nodes[n]
		}
	}

	if !g.allowMarkArbitraryNodesAsDone {
		notVisited := checkNotVisited(finishesMap, visited)
		if len(notVisited) > 0 {
			return nil, errors.Errorf("some done nodes not visited: %v", notVisited)
		}
	}

	return result, nil
}

// GetSchedulableNodeNames 根据 已完成的节点名 返回可调度的 节点名 ([]string).
func (g *DAG) GetSchedulableNodeNames(finishes ...string) ([]string, error) {
	m, err := g.GetSchedulable(finishes...)
	if err != nil {
		return nil, err
	}
	result := make([]string, 0, len(m))
	for nodeName := range m {
		result = append(result, nodeName)
	}
	sort.Sort(sort.StringSlice(result))
	return result, nil
}

// getRoots 返回 DAG 的根节点 (不存在 prev) 列表.
func (g *DAG) getRoots() []Node {
	var roots []Node
	for _, node := range g.Nodes {
		if len(node.PrevNodes()) == 0 {
			roots = append(roots, node)
		}
	}
	return roots
}

func checkNotVisited(doneTaskMap, visited map[string]Node) []string {
	var notVisited []string
	for done := range doneTaskMap {
		if _, ok := visited[done]; !ok {
			notVisited = append(notVisited, done)
		}
	}
	return notVisited
}

func findSchedulable(n Node, visited, doneNodes map[string]Node) []string {
	// 如果已经遍历过，则返回
	if _, ok := visited[n.NodeName()]; ok {
		return nil
	}

	visited[n.NodeName()] = n

	// 该 node 已完成，则递归寻找 node.next 并返回
	if _, ok := doneNodes[n.NodeName()]; ok {
		var schedulable []string
		for _, next := range n.NextNodes() {
			if _, ok := visited[next.NodeName()]; !ok {
				schedulable = append(schedulable, findSchedulable(next, visited, doneNodes)...)
			}
		}
		return schedulable
	}

	// 该 node 未完成且可调度，返回该 node
	if isSchedulable(n, doneNodes) {
		return []string{n.NodeName()}
	}

	// 该 node 未完成且不可调度，返回空
	return nil
}

func isSchedulable(n Node, doneNodes map[string]Node) bool {
	if len(n.PrevNodes()) == 0 {
		return true
	}
	var collected []string
	for _, prev := range n.PrevNodes() {
		if _, ok := doneNodes[prev.NodeName()]; ok {
			collected = append(collected, prev.NodeName())
		}
	}
	return len(collected) == len(n.PrevNodes())
}

func (g *DAG) toMap(nodeNames ...string) (map[string]Node, error) {
	m := make(map[string]Node, len(nodeNames))
	for _, name := range nodeNames {
		n, ok := g.Nodes[name]
		if !ok {
			return nil, errors.Errorf("node %q not found in DAG", name)
		}
		m[name] = n
	}
	return m, nil
}
