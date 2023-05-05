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

import "sync"

// encapsulate sync.WaitGroup
// realize a controllable number of WaitGroup
type limitSyncGroup struct {
	c  chan struct{}
	wg *sync.WaitGroup
}

func NewSemaphore(maxSize int) *limitSyncGroup {
	return &limitSyncGroup{
		c:  make(chan struct{}, maxSize),
		wg: new(sync.WaitGroup),
	}
}
func (s *limitSyncGroup) Add(delta int) {
	s.wg.Add(delta)
	for i := 0; i < delta; i++ {
		s.c <- struct{}{}
	}
}
func (s *limitSyncGroup) Done() {
	<-s.c
	s.wg.Done()
}
func (s *limitSyncGroup) Wait() {
	s.wg.Wait()
}
