//
// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

//
// note: this file is copied and modified from https://github.com/apache/pulsar-client-go/blob/master/pulsar/internal/semaphore.go
//

package syncx

import (
	"context"
	"sync/atomic"
)

// Semaphore is the interface of a semaphore, this semaphore can acquire one permit each time
type Semaphore interface {
	// Acquire a permit, if one is available and returns immediately,
	// reducing the number of available permits by one.
	Acquire(ctx context.Context) bool

	// TryAcquire Try to acquire a permit. The method will return immediately
	// with a `true` if it was possible to acquire a permit and
	// `false` otherwise.
	TryAcquire() bool

	// Release a permit, returning it to the semaphore.
	// Release a permit, increasing the number of available permits by
	// one.  If any threads are trying to acquire a permit, then one is
	// selected and given the permit that was just released.  That thread
	// is (re)enabled for thread scheduling purposes.
	// There is no requirement that a thread that releases a permit must
	// have acquired that permit by calling Acquire().
	// Correct usage of a semaphore is established by programming convention
	// in the application.
	Release()

	// Cap gets the max permits of a semaphore
	Cap() int32

	// Available gets the available permits of a semaphore, it just a snapshot, not an
	// accurate value.
	Available() int32
}

type semaphore struct {
	maxPermits int32
	permits    int32
	ch         chan bool
}

// NewSemaphore news a Semaphore
func NewSemaphore(maxPermits int32) Semaphore {
	if maxPermits <= 0 {
		panic("max permits for semaphore needs to be > 0")
	}

	return &semaphore{
		maxPermits: maxPermits,
		permits:    0,
		ch:         make(chan bool),
	}
}

func (s *semaphore) Acquire(ctx context.Context) bool {
	permits := atomic.AddInt32(&s.permits, 1)
	if permits <= s.maxPermits {
		return true
	}

	// Block on the channel until a new permit is available
	// or the context expires
	select {
	case <-s.ch:
		return true
	case <-ctx.Done():
		atomic.AddInt32(&s.permits, -1)
		return false
	}
}

func (s *semaphore) TryAcquire() bool {
	for {
		currentPermits := atomic.LoadInt32(&s.permits)
		if currentPermits >= s.maxPermits {
			// All the permits are already exhausted
			return false
		}

		if atomic.CompareAndSwapInt32(&s.permits, currentPermits, currentPermits+1) {
			// Successfully incremented counter
			return true
		}
	}
}

func (s *semaphore) Release() {
	permits := atomic.AddInt32(&s.permits, -1)
	if permits >= s.maxPermits {
		// Unblock the next in line to acquire the semaphore
		s.ch <- true
	}
}

func (s *semaphore) Cap() int32 {
	return s.maxPermits
}

func (s *semaphore) Available() int32 {
	currentPermits := atomic.LoadInt32(&s.permits)
	return s.maxPermits - currentPermits
}
