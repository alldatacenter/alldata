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

package manager

import "sync"

type TaskContext struct {
	ch     chan int
	wg     *sync.WaitGroup
	result interface{}
}

func (t *TaskContext) Add() {
	t.wg.Add(1)
	t.ch <- 1
}

func (t *TaskContext) Done() {
	t.wg.Done()
	select {
	case <-t.ch:
	default:
	}
}

func (t *TaskContext) Wait() {
	t.wg.Wait()
}

func NewTaskContext(concurrency int, result interface{}) *TaskContext {
	return &TaskContext{
		ch:     make(chan int, concurrency),
		wg:     &sync.WaitGroup{},
		result: result,
	}
}
