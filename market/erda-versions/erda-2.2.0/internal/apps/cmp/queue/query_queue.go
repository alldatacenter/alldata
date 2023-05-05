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

package queue

import "sync"

type QueryQueue struct {
	queue map[string]chan struct{}
	size  int
	mtx   sync.Mutex
}

func NewQueryQueue(size int) *QueryQueue {
	if size <= 0 {
		size = 1
	}
	return &QueryQueue{
		queue: map[string]chan struct{}{},
		size:  size,
	}
}

func (p *QueryQueue) Acquire(key string, delta int) {
	var ch chan struct{}
	var ok bool
	p.mtx.Lock()
	if ch, ok = p.queue[key]; !ok {
		ch = make(chan struct{}, p.size)
		p.queue[key] = ch
	}
	p.mtx.Unlock()

	for i := 0; i < delta; i++ {
		ch <- struct{}{}
	}
}

func (p *QueryQueue) Release(key string, delta int) {
	p.mtx.Lock()
	ch := p.queue[key]
	p.mtx.Unlock()
	for i := 0; i < delta; i++ {
		<-ch
	}
}
