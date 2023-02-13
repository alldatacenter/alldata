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

package util

type GPool struct {
	queue chan int
}

func NewGPool(size int) *GPool {
	if size <= 0 {
		size = 1
	}
	return &GPool{
		queue: make(chan int, size),
	}
}

func (p *GPool) Acquire(delta int) {
	for i := 0; i < delta; i++ {
		p.queue <- 1
	}
}

func (p *GPool) Release(delta int) {
	for i := 0; i < delta; i++ {
		<-p.queue
	}
}
