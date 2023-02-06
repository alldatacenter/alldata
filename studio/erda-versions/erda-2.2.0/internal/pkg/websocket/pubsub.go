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

package websocket

import (
	"context"
	"encoding/json"
	"fmt"
	"path/filepath"
	"sync"
	"time"

	"github.com/coreos/etcd/clientv3"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/crypto/uuid"
	"github.com/erda-project/erda/pkg/jsonstore"
	"github.com/erda-project/erda/pkg/jsonstore/etcd"
	"github.com/erda-project/erda/pkg/jsonstore/storetypes"
)

const (
	eDir = "/websocket/events"
	ttl  = 60
)

type Subscriber struct {
	js        jsonstore.JSONStoreWithWatch
	eventC    chan *Event
	stopC     chan struct{}
	runningWg sync.WaitGroup
}

func NewSubscriber(ec chan *Event) (*Subscriber, error) {
	js, err := jsonstore.New()
	if err != nil {
		return nil, errors.Wrap(err, "failed to create json storage instance")
	}

	js_ := js.(jsonstore.JSONStoreWithWatch)

	return &Subscriber{
		js:     js_,
		eventC: ec,
		stopC:  make(chan struct{}),
	}, nil
}

func (s *Subscriber) retry(f func() error) {
	err := f()
	if err != nil {
		s.retry(f)
	}
}

func (s *Subscriber) Start() {
	ctx, cancel := context.WithCancel(context.Background())

	go func() {
		<-s.stopC
		cancel()
	}()

	f := func() error {
		err := s.js.Watch(ctx, eDir, true, true, false, Event{},
			func(key string, value interface{}, _ storetypes.ChangeType) error {
				event := value.(*Event)
				go func() {
					logrus.Debugf("watch event %v", event)
					s.eventC <- event
				}()
				return nil
			})
		return err
	}

	s.retry(f)
}

func (s *Subscriber) Stop() {
	s.stopC <- struct{}{}
	s.runningWg.Wait()
	logrus.Warn("stop subscriber")
}

type Publisher struct {
	es *etcd.Store
}

func NewPublisher() (*Publisher, error) {
	es, err := etcd.New()
	if err != nil {
		return nil, errors.Wrap(err, "failed to create etcd store instance")
	}
	return &Publisher{es: es}, nil
}

func (p *Publisher) EmitEvent(ctx context.Context, e Event) error {
	path := generatePath()
	lease := clientv3.NewLease(p.es.GetClient())
	r, err := lease.Grant(ctx, ttl)
	if err != nil {
		return errors.Wrap(err, "failed to grant etcd lease")
	}
	b, err := json.Marshal(e)
	if err != nil {
		return errors.Wrapf(err, "failed to marshal event %v", e)
	}
	if _, err := p.es.PutWithOption(ctx, path, string(b), []interface{}{clientv3.WithLease(r.ID)}); err != nil {
		return err
	}
	logrus.Debugf("emit ws event %+v", e)
	return nil
}

func generatePath() string {
	uuid := fmt.Sprintf("%d-%s", time.Now().UnixNano(), uuid.UUID())
	return filepath.Join(eDir, uuid)
}
