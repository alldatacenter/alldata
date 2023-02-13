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

package goroutine_context

import (
	"context"
	"runtime"
	"sync"

	"github.com/go-eden/routine"
)

const LocaleNameContextKey = "locale_name_context_key"

const bucketsSize = 128

const arm64 = "arm64"

type (
	contextBucket struct {
		lock sync.RWMutex
		data map[int64]context.Context
	}
	contextBuckets struct {
		buckets [bucketsSize]*contextBucket
	}
)

var goroutineContext contextBuckets

func init() {
	for i := range goroutineContext.buckets {
		goroutineContext.buckets[i] = &contextBucket{
			data: make(map[int64]context.Context),
		}
	}
}

// GetContext .
func GetContext() context.Context {
	// mac system use goroutine_context.GetContext() will panic
	if runtime.GOARCH == arm64 {
		return context.Background()
	}

	goid := routine.Goid()
	idx := goid % bucketsSize
	bucket := goroutineContext.buckets[idx]
	bucket.lock.RLock()
	ctx := bucket.data[goid]
	bucket.lock.RUnlock()
	return ctx
}

// SetContext .
func SetContext(ctx context.Context) {
	// mac system use goroutine_context.GetContext() will panic
	if runtime.GOARCH == arm64 {
		return
	}

	goid := routine.Goid()
	idx := goid % bucketsSize
	bucket := goroutineContext.buckets[idx]
	bucket.lock.Lock()
	defer bucket.lock.Unlock()
	bucket.data[goid] = ctx
}

// ClearContext .
func ClearContext() {
	// mac system use goroutine_context.GetContext() will panic
	if runtime.GOARCH == arm64 {
		return
	}

	goid := routine.Goid()
	idx := goid % bucketsSize
	bucket := goroutineContext.buckets[idx]
	bucket.lock.Lock()
	defer bucket.lock.Unlock()
	delete(bucket.data, goid)
}
