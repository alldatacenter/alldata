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

package scheduled

import (
	"context"
	"reflect"
	"time"

	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-proto-go/msp/apm/checker/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/checker/task/fetcher"
)

// Dispatcher
type Dispatcher struct {
	log logs.Logger

	checkers map[int64]*pb.Checker
	lister   func() (map[int64]*pb.Checker, error)
	reload   time.Duration

	putCh    chan *pb.Checker
	removeCh chan int64
	watchers []chan *fetcher.Event
}

func NewDispatcher(lister func() (map[int64]*pb.Checker, error), reload time.Duration, log logs.Logger) *Dispatcher {
	return &Dispatcher{
		log:      log,
		checkers: make(map[int64]*pb.Checker),
		lister:   lister,
		reload:   reload,
		putCh:    make(chan *pb.Checker, 8),
		removeCh: make(chan int64, 8),
	}
}

func (p *Dispatcher) Run(ctx context.Context) error {
	load := func() error {
		checkers, err := p.lister()
		if err != nil {
			return err
		}
		for _, c := range checkers {
			if exist, ok := p.checkers[c.Id]; ok {
				if compareChecker(c, exist) {
					continue
				}
				p.notify(&fetcher.Event{
					Action: fetcher.ActionUpdate,
					Data:   copyChecker(c),
				})
				p.log.Debugf("update checker %v to %v", c, exist)
			} else {
				p.notify(&fetcher.Event{
					Action: fetcher.ActionAdd,
					Data:   copyChecker(c),
				})
			}
			p.checkers[c.Id] = c
		}
		for id, c := range p.checkers {
			if _, ok := checkers[c.Id]; !ok {
				p.notify(&fetcher.Event{
					Action: fetcher.ActionDelete,
					Data:   copyChecker(c),
				})
				delete(p.checkers, id)
			}
		}
		return nil
	}

	err := load()
	if err != nil {
		return err
	}
	for {
		select {
		case <-ctx.Done():
			return nil
		case c := <-p.putCh:
			if exist, ok := p.checkers[c.Id]; ok {
				if compareChecker(c, exist) {
					continue
				}
				p.notify(&fetcher.Event{
					Action: fetcher.ActionUpdate,
					Data:   copyChecker(c),
				})
				p.log.Debugf("update checker %v to %v", c, exist)
			} else {
				p.notify(&fetcher.Event{
					Action: fetcher.ActionAdd,
					Data:   copyChecker(c),
				})
			}
			p.checkers[c.Id] = c
		case id := <-p.removeCh:
			if c, ok := p.checkers[id]; ok {
				p.notify(&fetcher.Event{
					Action: fetcher.ActionDelete,
					Data:   copyChecker(c),
				})
				delete(p.checkers, id)
			}
		case <-time.After(p.reload):
		}

		err := load()
		if err != nil {
			p.log.Errorf("fail to load checkers: %s", err)
		}
	}
}

func (p *Dispatcher) Watch() <-chan *fetcher.Event {
	ch := make(chan *fetcher.Event, 16)
	p.watchers = append(p.watchers, ch)
	return ch
}

func (p *Dispatcher) notify(e *fetcher.Event) {
	for _, w := range p.watchers {
		w <- e
	}
}

func (p *Dispatcher) Put(c *pb.Checker) {
	p.putCh <- c
}

func (p *Dispatcher) Remove(id int64) {
	p.removeCh <- id
}

func compareChecker(a, b *pb.Checker) bool {
	if a.Id != b.Id || a.Name != b.Name || a.Type != b.Type {
		return false
	}
	if !reflect.DeepEqual(a.Config, b.Config) || !reflect.DeepEqual(a.Tags, b.Tags) {
		return false
	}
	return true
}

func copyChecker(c *pb.Checker) *pb.Checker {
	ck := &pb.Checker{
		Id:   c.Id,
		Name: c.Name,
		Type: c.Type,
	}
	if c.Config != nil {
		ck.Config = make(map[string]*structpb.Value)
		for k, v := range c.Config {
			ck.Config[k] = v
		}
	}
	if c.Tags != nil {
		ck.Tags = make(map[string]string)
		for k, v := range c.Tags {
			ck.Tags[k] = v
		}
	}
	return ck
}
