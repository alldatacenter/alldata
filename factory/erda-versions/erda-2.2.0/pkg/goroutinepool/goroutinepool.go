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

// Usage:
// poolSize := 10
// pool := goroutinepool.New(poolSize)
// pool.Start()
// pool.Go(func(){...}) // `Go' will return `NoMoreWorkerErr' if no more worker available
// pool.MustGo(func(){...}) // `MustGo' will block until any worker available
//
// pool.Stop() // `Stop' will block to wait all workers done
//
// pool.Start() // and the same pool can be reused
package goroutinepool

import (
	"errors"
	"runtime/debug"
	"sync"
	"time"

	"github.com/sirupsen/logrus"
)

var (
	NoMoreWorkerErr = errors.New("no more worker, pool is full")
	TimeoutErr      = errors.New("time out")
)

type worker struct {
	pool *GoroutinePool
	job  chan func()
	stop chan struct{}
}

func (w *worker) run() {
	for {
		w.pool.addIdleWorker(w)
		select {
		case job := <-w.job:
			func() {
				defer func() {
					if err := recover(); err != nil {
						debug.PrintStack()
						logrus.Errorf("[alert] GoroutinePool: a job panic: %v", err)
					}
				}()
				job()

			}()
		case <-w.stop:
			w.pool.Done()
			return
		}
	}

}

type GoroutinePool struct {
	allWorkers     []*worker
	cap            int
	sync.WaitGroup // wait all workers stopped

	sync.RWMutex // protect 'running' and 'workers'
	workers      chan *worker
	running      bool
}

func New(cap int) *GoroutinePool {
	pool := &GoroutinePool{
		workers: make(chan *worker, cap),
		cap:     cap,
		running: false,
	}
	return pool
}

func (p *GoroutinePool) Start() {
	p.Lock()
	defer p.Unlock()

	if p.running {
		return
	}
	if p.allWorkers == nil {
		for i := 0; i < p.cap; i++ {
			w := &worker{pool: p, job: make(chan func()), stop: make(chan struct{}, 1)}
			p.allWorkers = append(p.allWorkers, w)
			w.pool.Add(1)
			go w.run()
		}
	} else {
		p.workers = make(chan *worker, p.cap)
		for _, w := range p.allWorkers {
			w.pool.Add(1)
			go w.run()
		}
	}
	p.running = true
}

// block until all workers stopped
func (p *GoroutinePool) Stop() {
	p.Lock()
	defer p.Unlock()

	if !p.running {
		return
	}
	for _, w := range p.allWorkers {
		w.stop <- struct{}{}
	}

	p.Wait()

	close(p.workers)
	p.running = false
}

func (p *GoroutinePool) Go(f func()) error {
	p.RLock()
	defer p.RUnlock()

	if !p.running {
		panic("not running yet")
	}
	select {
	case worker := <-p.workers:
		worker.job <- f
	default:
		return NoMoreWorkerErr
	}
	return nil
}

func (p *GoroutinePool) GoWithTimeout(f func(), timeout time.Duration) error {
	p.RLock()
	defer p.RUnlock()

	if !p.running {
		panic("not running yet")
	}
	select {
	case worker := <-p.workers:
		worker.job <- f
	case <-time.After(timeout):
		return TimeoutErr
	}
	return nil
}

func (p *GoroutinePool) MustGo(f func()) {
	p.RLock()
	defer p.RUnlock()

	if !p.running {
		panic("not running yet")
	}
	select {
	case worker := <-p.workers:
		worker.job <- f
	}

}

// return [<IDLE-worker-num>, <total-worker-num>]
func (p *GoroutinePool) Statistics() [2]int {
	p.Lock()
	defer p.Unlock()

	if !p.running {
		return [2]int{0, 0}
	}
	return [2]int{len(p.workers), len(p.allWorkers)}
}

func (p *GoroutinePool) addIdleWorker(w *worker) {
	defer func() {
		if r := recover(); r != nil {
			return
		}
	}()
	p.workers <- w
}
