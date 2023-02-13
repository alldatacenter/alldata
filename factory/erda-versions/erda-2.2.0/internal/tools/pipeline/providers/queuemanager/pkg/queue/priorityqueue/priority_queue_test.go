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
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestPriorityQueue_Get(t *testing.T) {
	pq := NewPriorityQueue()

	get := pq.Get("k1")
	assert.Nil(t, get, "no BaseItem now")

	pq.Add(NewItem("k1", 0, time.Time{}))
	get = pq.Get("k1")
	assert.NotNil(t, get, "only k1 now")
	assert.Equal(t, "k1", get.Key(), "only k1 now")
}

func TestPriorityQueue_Peek(t *testing.T) {
	pq := NewPriorityQueue()

	now := time.Now()

	pq.Add(NewItem("k1", 1, time.Now()))
	peeked := pq.Peek()
	assert.NotNil(t, peeked, "only k1 now")
	assert.Equal(t, peeked.Key(), "k1", "only k1 now")

	pq.Add(NewItem("k1", 1, now))
	pq.Add(NewItem("k2", 1, now.Add(time.Second)))
	peeked = pq.Peek()
	assert.NotNil(t, peeked, "k1 and k2")
	assert.Equal(t, peeked.Key(), "k1", "k1,k2 have same priority, but k1 is earlier")

	pq.Add(NewItem("k3", 1, now.Add(-time.Second)))
	pq.Add(NewItem("k3", 1, now.Add(-time.Second)))
	peeked = pq.Peek()
	assert.NotNil(t, peeked, "k1, k2, k3")
	assert.Equal(t, peeked.Key(), "k3", "k1,k2,k3 have same priority, but k3 is the earliest")

	pq.Add(NewItem("k4", 2, now.Add(time.Hour)))
	peeked = pq.Peek()
	assert.NotNil(t, peeked, "k1, k2, k3, k4")
	assert.Equal(t, peeked.Key(), "k4", "k4's priority is highest")

	// priority: k4 > k3 > k1 > k2
	assert.Equal(t, "k4", pq.data.items[0].Key())
	assert.Equal(t, "k3", pq.data.items[1].Key())
	assert.Equal(t, "k1", pq.data.items[2].Key())
	assert.Equal(t, "k2", pq.data.items[3].Key())
}

func TestPriorityQueue_Pop(t *testing.T) {
	pq := NewPriorityQueue()
	popped := pq.Pop()
	assert.Nil(t, popped, "no BaseItem now")

	now := time.Now()

	pq.Add(NewItem("k1", 1, now))
	popped = pq.Pop()
	assert.NotNil(t, popped)
	assert.Equal(t, popped.Key(), "k1", "only k1 now")

	pq.Add(NewItem("k2", 1, now))
	pq.Add(NewItem("k3", 2, now))
	popped = pq.Pop()
	assert.NotNil(t, popped)
	assert.Equal(t, popped.Key(), "k3", "k3's priority is higher than k2")
	popped = pq.Pop()
	assert.NotNil(t, popped)
	assert.Equal(t, popped.Key(), "k2", "only k2 now")
	popped = pq.Pop()
	assert.Nil(t, popped, "no BaseItem now, all popped out")
	popped = pq.Pop()
	assert.Nil(t, popped, "no BaseItem now, all popped out")

	pq.Add(NewItem("k4", 1, now))
	pq.Add(NewItem("k5", 1, now.Add(-time.Second)))
	popped = pq.Pop()
	assert.NotNil(t, popped)
	assert.Equal(t, popped.Key(), "k5", "k5 is earlier than p4")
}

func TestPriorityQueue_Add(t *testing.T) {
	pq := NewPriorityQueue()
	assert.Equal(t, 0, len(pq.data.items), "no BaseItem now")

	now := time.Now()
	pq.Add(NewItem("k1", 1, now))
	assert.Equal(t, 1, len(pq.data.items), "only k1 now")
	get := pq.Get("k1")
	assert.Equal(t, "k1", get.Key(), "only k1 now")
	assert.Equal(t, int64(1), get.Priority())

	pq.Add(NewItem("k1", 2, now))
	assert.Equal(t, 1, len(pq.data.itemByKey), "still only k1 now")
	get = pq.Get("k1")
	assert.Equal(t, "k1", get.Key(), "still only k1 now")
	assert.Equal(t, int64(2), get.Priority(), "k1's priority updated to 2")
}

type obj struct {
	name      string
	createdAt time.Time
	index     int
	priority  int64
}

func (o obj) Key() string             { return o.name }
func (o obj) Priority() int64         { return 1 }
func (o obj) SetPriority(i int64)     { o.priority = i }
func (o obj) CreationTime() time.Time { return o.createdAt }
func (o obj) Index() int              { return o.index }
func (o obj) SetIndex(i int)          { o.index = i }

func TestPriorityQueue_Add2(t *testing.T) {
	pq := NewPriorityQueue()

	pq.Add(obj{name: "k1", createdAt: time.Now(), priority: 1})
	pq.Add(obj{name: "k2", createdAt: time.Now(), priority: 1})
	popped := pq.Pop()
	assert.NotNil(t, popped)
	assert.Equal(t, "k1", popped.Key())
}

func TestPriorityQueue_Remove(t *testing.T) {
	pq := NewPriorityQueue()

	removed := pq.Remove("k1")
	assert.Nil(t, removed, "no BaseItem now")

	pq.Add(NewItem("k1", 0, time.Time{}))
	removed = pq.Remove("k2")
	assert.Nil(t, removed, "k2 not exist")
	removed = pq.Remove("k1")
	assert.NotNil(t, removed, "k1 exist and removed")
	removed = pq.Remove("k1")
	assert.Nil(t, removed, "k1 already been removed")
}

func TestPriorityQueue_LeftHasHigherOrder(t *testing.T) {
	type args struct {
		left  Item
		right Item
	}
	tests := []struct {
		name string
		args args
		want bool
	}{
		{
			name: "left right both not exist",
			args: args{
				left:  nil,
				right: nil,
			},
			want: false,
		},
		{
			name: "left exist, right not exist",
			args: args{
				left:  NewItem("left", 1, time.Now()),
				right: nil,
			},
			want: true,
		},
		{
			name: "left not exist, right exist",
			args: args{
				left:  nil,
				right: NewItem("right", 1, time.Now()),
			},
			want: false,
		},
		{
			name: "left right both exist, left's priority is higher than right",
			args: args{
				left:  NewItem("left", 2, time.Now()),
				right: NewItem("right", 1, time.Now()),
			},
			want: true,
		},
		{
			name: "left right both exist, right's priority is higher than left",
			args: args{
				left:  NewItem("left", 1, time.Now()),
				right: NewItem("right", 2, time.Now()),
			},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			pq := NewPriorityQueue()
			if tt.args.left != nil {
				pq.Add(tt.args.left)
			}
			if tt.args.right != nil {
				pq.Add(tt.args.right)
			}
			if got := pq.LeftHasHigherOrder("left", "right"); got != tt.want {
				t.Errorf("LeftHasHigherOrder() = %v, want %v", got, tt.want)
			}
		})
	}
}
