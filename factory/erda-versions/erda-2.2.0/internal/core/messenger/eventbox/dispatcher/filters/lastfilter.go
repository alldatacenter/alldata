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

package filters

import (
	"encoding/json"
	"math/rand"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/core/messenger/eventbox/dispatcher/errors"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/subscriber"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/types"
	"github.com/erda-project/erda/pkg/goroutinepool"
)

// Filter的最后一个，实际上用来做后续对 message 的操作
type LastFilter struct {
	subscribers map[string]subscriber.Subscriber
	pools       map[string]*goroutinepool.GoroutinePool
}

func NewLastFilter(pools map[string]*goroutinepool.GoroutinePool, subscribers map[string]subscriber.Subscriber) Filter {
	return &LastFilter{
		subscribers: subscribers,
		pools:       pools,
	}
}

func (l *LastFilter) Name() string {
	return "LastFilter"
}

func (l *LastFilter) Filter(m *types.Message) *errors.DispatchError {
	derr := errors.New()
	publishErrM := make(map[string](chan []error))
	for name, sub := range l.subscribers {
		for k, v := range m.Labels {
			if k.Equal(name) {
				publishErrCh, poolErr := throttlePublish(m, l.pools[name], sub, v)
				if poolErr != nil {
					derr.BackendErrs[name] = []error{poolErr}
					continue
				} else {
					publishErrM[name] = publishErrCh
				}
			}
		}
	}
	for name, publishErr := range publishErrM {
		errs := <-publishErr
		if len(errs) > 0 {
			derr.BackendErrs[name] = errs
		}
	}
	return derr
}

func throttlePublish(m *types.Message, pool *goroutinepool.GoroutinePool, sub subscriber.Subscriber, labelV interface{}) (chan []error, error) {
	errsCh := make(chan []error, 1)
	f := func() {
		defer func() {
			close(errsCh)
		}()
		content_, err := json.Marshal(m.Content)
		if err != nil {
			errsCh <- []error{err}
			return
		}
		labelV_, err := json.Marshal(labelV)
		if err != nil {
			errsCh <- []error{err}
			return
		}
		errs := sub.Publish(string(labelV_), string(content_), m.Time, m)
		if len(errs) > 0 {
			logrus.Errorf("publish message: %v, err: %v", m, errs)
			errsCh <- errs
		} else {
			errsCh <- []error{}
		}
	}

	if err := pool.Go(f); err != nil {
		if err == goroutinepool.NoMoreWorkerErr {
			rand.Seed(time.Now().Unix())
			time.Sleep(time.Duration(rand.Intn(1000)) * time.Millisecond) // retry after random time
			if err = pool.Go(f); err != nil {
				close(errsCh)
				return nil, err
			}
		} else {
			close(errsCh)
			return nil, err
		}

	}
	return errsCh, nil
}
