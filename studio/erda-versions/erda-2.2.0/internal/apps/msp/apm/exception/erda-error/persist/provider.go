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

package persist

import (
	"context"
	"fmt"
	"time"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/kafka"
	"github.com/erda-project/erda/internal/apps/msp/apm/exception/erda-error/storage"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit"
)

type (
	config struct {
		Input             kafka.BatchReaderConfig `file:"input"`
		Parallelism       int                     `file:"parallelism" default:"1"`
		BufferSize        int                     `file:"buffer_size" default:"1024"`
		ReadTimeout       time.Duration           `file:"read_timeout" default:"5s"`
		IDKeys            []string                `file:"id_keys"`
		PrintInvalidError bool                    `file:"print_invalid_error" default:"false"`
	}
	provider struct {
		Cfg           *config
		Log           logs.Logger
		Kafka         kafka.Interface `autowired:"kafka"`
		StorageWriter storage.Storage `autowired:"error-storage-writer"`

		storage   storage.Storage
		stats     Statistics
		validator Validator
		metadata  MetadataProcessor
	}
)

func (p *provider) Init(ctx servicehub.Context) (err error) {

	p.validator = newValidator(p.Cfg)
	if runner, ok := p.validator.(servicehub.ProviderRunnerWithContext); ok {
		ctx.AddTask(runner.Run, servicehub.WithTaskName("error validator"))
	}

	p.metadata = newMetadataProcessor(p.Cfg)
	if runner, ok := p.metadata.(servicehub.ProviderRunnerWithContext); ok {
		ctx.AddTask(runner.Run, servicehub.WithTaskName("error metadata processor"))
	}

	p.stats = sharedStatistics

	// add consumer task
	for i := 0; i < p.Cfg.Parallelism; i++ {
		//spot
		ctx.AddTask(func(ctx context.Context) error {
			r, err := p.Kafka.NewBatchReader(&p.Cfg.Input, kafka.WithReaderDecoder(p.decodeError))
			if err != nil {
				return err
			}
			defer r.Close()

			w, err := p.StorageWriter.NewWriter(ctx)
			if err != nil {
				return err
			}
			defer w.Close()
			return storekit.BatchConsume(ctx, r, w, &storekit.BatchConsumeOptions{
				BufferSize:          p.Cfg.BufferSize,
				ReadTimeout:         p.Cfg.ReadTimeout,
				ReadErrorHandler:    p.handleReadError,
				WriteErrorHandler:   p.handleWriteError,
				ConfirmErrorHandler: p.confirmErrorHandler,
				Statistics:          p.stats,
			})
		}, servicehub.WithTaskName(fmt.Sprintf("spot-error-consumer(%d)", i)))
	}
	return nil
}

func init() {
	servicehub.Register("error-persist", &servicehub.Spec{
		Dependencies: []string{"kafka.topic.initializer"},
		ConfigFunc:   func() interface{} { return &config{} },
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
