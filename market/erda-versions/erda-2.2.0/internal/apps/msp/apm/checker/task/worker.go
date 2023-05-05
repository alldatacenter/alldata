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

package task

import (
	"context"
	"fmt"
	"io"
	"time"

	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-proto-go/msp/apm/checker/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/checker/plugins"
)

// Worker .
type Worker interface {
	Run(ctx context.Context)
}

var (
	defaultStrategy         = "periodic"
	defaultPeriodicInterval = 30 * time.Second
)

func NewWorker(log logs.Logger, c *pb.Checker, ctx plugins.Context, handler plugins.Handler) (Worker, error) {
	strategy := c.Config["strategy"].GetStringValue()
	if len(strategy) == 0 {
		strategy = defaultStrategy
	}
	switch strategy {
	case "periodic":
		return newPeriodicWorker(log, c.Config, ctx, handler)
	case "cron":
		// TODO .
	}
	return nil, fmt.Errorf("strategy %q not support", strategy)
}

type periodicWorker struct {
	log      logs.Logger
	ctx      plugins.Context
	handler  plugins.Handler
	interval time.Duration
	closeCh  chan struct{}
}

func newPeriodicWorker(log logs.Logger, cfg map[string]*structpb.Value, ctx plugins.Context, handler plugins.Handler) (*periodicWorker, error) {
	interval := defaultPeriodicInterval
	if i := cfg["interval"].GetNumberValue(); i > 0 {
		interval = time.Duration(i) * time.Second
		if i <= 0 {
			return nil, fmt.Errorf("invalid interval %d", int64(i))
		}
	}
	return &periodicWorker{
		log:      log,
		ctx:      ctx,
		handler:  handler,
		interval: interval,
		closeCh:  make(chan struct{}),
	}, nil
}

func (w *periodicWorker) Run(ctx context.Context) {
	for {
		err := w.handler.Do(w.ctx)
		if err != nil {
			w.log.Error(err)
		}
		select {
		case <-time.After(w.interval):
		case <-w.closeCh:
			return
		case <-ctx.Done():
			return
		}
	}
}

func (w *periodicWorker) Close() error {
	close(w.closeCh)
	if c, ok := w.handler.(io.Closer); ok {
		return c.Close()
	}
	return nil
}

// cronWorker TODO
type cronWorker struct {
}
