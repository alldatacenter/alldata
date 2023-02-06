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

package priorityqueue

import (
	"container/heap"
	"sort"
)

// PriorityQueue 优先队列
type PriorityQueue struct {
	// 使用 data 再次封装，防止 Len() 等方法被暴露
	data *priorityQueue
}

// priorityQueue 优先队列
type priorityQueue struct {
	items     []Item
	itemByKey map[string]Item
}

func NewPriorityQueue() *PriorityQueue {
	return &PriorityQueue{
		data: &priorityQueue{
			items:     make([]Item, 0),
			itemByKey: make(map[string]Item),
		},
	}
}

// Get 根据 key 获取 BaseItem
func (pq *PriorityQueue) Get(key string) Item {
	return pq.data.itemByKey[key]
}

// Peek 获取优先级最高的 BaseItem
func (pq *PriorityQueue) Peek() Item {
	if len(pq.data.items) == 0 {
		return nil
	}
	return pq.data.items[0]
}

// Pop 弹出优先级最高的 BaseItem，并返回
func (pq *PriorityQueue) Pop() Item {
	if len(pq.data.items) == 0 {
		return nil
	}
	return heap.Pop(pq.data).(Item)
}

// Add 新增 BaseItem
func (pq *PriorityQueue) Add(item Item) {
	if res, ok := pq.data.itemByKey[item.Key()]; ok {
		if res.Priority() != item.Priority() {
			res.SetPriority(item.Priority())
			heap.Fix(pq.data, res.Index())
		}
	} else {
		heap.Push(pq.data, convertItem(item))
	}
	sort.Sort(pq.data)
}

// Remove 删除 BaseItem
func (pq *PriorityQueue) Remove(key string) Item {
	if _item, ok := pq.data.itemByKey[key]; ok {
		delete(pq.data.itemByKey, key)
		return heap.Remove(pq.data, _item.Index()).(Item)
	}
	return nil
}

// Len 返回队列长度
func (pq *PriorityQueue) Len() int {
	return pq.data.Len()
}

// Range range items and apply func to item one by one.
func (pq *PriorityQueue) Range(f func(Item) (stopRange bool)) {
	for _, item := range pq.data.items {
		stopRange := f(item)
		if stopRange {
			break
		}
	}
}

// LeftHasHigherOrder judge order of two items.
// return true if left has higher order.
func (pq *PriorityQueue) LeftHasHigherOrder(left, right string) bool {
	leftItem, leftExist := pq.data.itemByKey[left]
	rightItem, rightExist := pq.data.itemByKey[right]
	if !leftExist && !rightExist {
		return false
	}
	if !leftExist {
		return false
	}
	if !rightExist {
		return true
	}
	// both exist, judge by index
	return leftItem.Index() < rightItem.Index()
}
