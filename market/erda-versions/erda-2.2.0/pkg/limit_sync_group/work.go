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

package limit_sync_group

import (
	"sync"
)

type function struct {
	params   []interface{}
	function func(*Locker, ...interface{}) error
}

type Worker struct {
	wait      *limitSyncGroup
	functions []function
	errInfo   error
	lock      *Locker
}

type Locker struct {
	sync.RWMutex
}

func NewWorker(limit int) *Worker {
	group := NewSemaphore(limit)
	return &Worker{
		wait: group,
		lock: &Locker{
			RWMutex: sync.RWMutex{},
		},
	}
}

func (that *Worker) AddFunc(fun func(*Locker, ...interface{}) error, params ...interface{}) *Worker {
	that.functions = append(that.functions, function{
		function: fun,
		params:   params,
	})
	return that
}

func (that *Worker) Do() *Worker {
	for index := range that.functions {
		that.wait.Add(1)
		go func(index int) {
			defer func() {
				that.wait.Done()
			}()
			err := that.functions[index].function(that.lock, that.functions[index].params...)
			if err != nil {
				that.errInfo = err
			}
		}(index)
	}
	that.wait.Wait()
	that.functions = nil
	return that
}

func (that *Worker) Error() error {
	return that.errInfo
}
