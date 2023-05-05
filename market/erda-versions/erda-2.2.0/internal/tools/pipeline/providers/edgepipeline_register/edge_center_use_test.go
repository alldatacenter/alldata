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

package edgepipeline_register

import (
	"context"
	"testing"
	"time"
)

func Test_mustStarted(t *testing.T) {
	p := &provider{}
	t.Run("not started", func(t *testing.T) {
		defer func() {
			if r := recover(); r == nil {
				t.Error("should panic")
			}
		}()
		p.mustStarted()
	})
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()
	p.startEdgeCenterUse(ctx)
	t.Run("started", func(t *testing.T) {
		defer func() {
			if r := recover(); r != nil {
				t.Error("should not panic")
			}
		}()
		p.mustStarted()
	})
}

func TestOnEdge(t *testing.T) {
	p := &provider{
		Cfg: &Config{
			IsEdge: true,
		},
	}
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()
	p.forEdgeUse.handlersOnEdge = make(chan func(context.Context), 0)
	p.forCenterUse.handlersOnCenter = make(chan func(context.Context), 0)
	p.startEdgeCenterUse(ctx)
	t.Run("edge", func(t *testing.T) {
		val := 5
		p.OnEdge(func(ctx context.Context) {
			p.Lock()
			val = 6
			p.Unlock()
		})
		time.Sleep(1 * time.Second)
		p.Lock()
		if val != 6 {
			p.Unlock()
			t.Errorf("val should be 6, but %d", val)
			return
		}
		p.Unlock()
	})

	t.Run("center", func(t *testing.T) {
		val := 5
		p.OnCenter(func(ctx context.Context) {
			p.Lock()
			val = 6
			p.Unlock()
		})
		time.Sleep(1 * time.Second)
		p.Lock()
		if val != 5 {
			p.Unlock()
			t.Errorf("val should be 5, but %d", val)
			return
		}
		p.Unlock()
	})
}

func TestOnCenter(t *testing.T) {
	p := &provider{
		Cfg: &Config{
			IsEdge: false,
		},
	}
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()
	p.forEdgeUse.handlersOnEdge = make(chan func(context.Context), 0)
	p.forCenterUse.handlersOnCenter = make(chan func(context.Context), 0)
	p.startEdgeCenterUse(ctx)
	t.Run("center", func(t *testing.T) {
		val := 5
		p.OnCenter(func(ctx context.Context) {
			p.Lock()
			val = 6
			p.Unlock()
		})
		time.Sleep(1 * time.Second)
		p.Lock()
		if val != 6 {
			p.Unlock()
			t.Errorf("val should be 6, but %d", val)
			return
		}
		p.Unlock()
	})

	t.Run("edge", func(t *testing.T) {
		val := 5
		p.OnEdge(func(ctx context.Context) {
			p.Lock()
			val = 6
			p.Unlock()
		})
		time.Sleep(1 * time.Second)
		p.Lock()
		if val != 5 {
			p.Unlock()
			t.Errorf("val should be 5, but %d", val)
			return
		}
		p.Unlock()
	})
}
