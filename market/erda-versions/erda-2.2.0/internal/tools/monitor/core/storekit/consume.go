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

package storekit

import (
	"context"
	"errors"
	"time"
)

// BatchReader .
type BatchReader interface {
	ReadN(buf []Data, timeout time.Duration) (int, error)
	Confirm() error
	Close() error
}

// BatchConsumeOptions .
type BatchConsumeOptions struct {
	BufferSize  int
	ReadTimeout time.Duration
	// ReadErrorHandler return err to exit, return nil to continue
	ReadErrorHandler func(err error) error
	// WriteErrorHandler return err to retry write, return nil to continue
	WriteErrorHandler func(list []Data, err error) error
	// ConfirmErrorHandler return err to exit, return nil to continue
	ConfirmErrorHandler func(err error) error
	Backoff             TimerBackoff
	Statistics          ConsumeStatistics
}

// ErrExitConsume .
var ErrExitConsume = errors.New("exit consume")

// BatchConsume .
func BatchConsume(ctx context.Context, r BatchReader, w BatchWriter, opts *BatchConsumeOptions) error {
	// timer backoff
	backoff := opts.Backoff
	if backoff == nil {
		backoff = &MultiplierBackoff{
			Base:   opts.ReadTimeout,
			Max:    16 * opts.ReadTimeout,
			Factor: 2,
		}
	}
	backoff.Reset()

	// statistics
	stats := opts.Statistics
	if stats == nil {
		stats = NopConsumeStatistics
	}

	// failed handlers
	readErrorHandler := opts.ReadErrorHandler
	writeErrorHandler := opts.WriteErrorHandler
	confirmErrorHandler := opts.ConfirmErrorHandler

	// check flusher
	flusher, flush := w.(Flusher)

	// init buffer
	buf := make([]Data, opts.BufferSize, opts.BufferSize)

consumeLoop:
	for {
		// check exit
		select {
		case <-ctx.Done():
			return nil
		default:
		}

		// read with timeout
		start := time.Now()
		n, err := r.ReadN(buf, opts.ReadTimeout)
		if err != nil {
			if err == ErrExitConsume {
				return nil
			}
			stats.ReadError(err)
			if err := readErrorHandler(err); err != nil {
				return err
			}
			select {
			case <-ctx.Done():
				return nil
			case <-backoff.Wait():
			}
			continue
		}
		stats.ObserveReadLatency(start)

		if n <= 0 {
			continue
		}
		list := buf[0:n]

		// write data
	retryLoop:
		for {
			start := time.Now()
			_, err := w.WriteN(list...)
			if err == nil && flush {
				err = flusher.Flush()
			}
			if err != nil {
				if err == ErrExitConsume {
					return nil
				}
				stats.WriteError(list, err)
				if err := writeErrorHandler(list, err); err == nil {
					// skip data
					continue consumeLoop
				}
				select {
				case <-ctx.Done():
					return nil
				case <-backoff.Wait():
				}
				continue retryLoop
			}
			stats.ObserveWriteLatency(start)

			err = r.Confirm()
			if err != nil {
				stats.ConfirmError(list, err)
				if err = confirmErrorHandler(err); err != nil {
					return err
				}
			}
			stats.Success(list)
			backoff.Reset()
			break
		}
	}
}

// NewMockBatchReader .
func NewMockBatchReader(ctx context.Context, interval time.Duration, batchSize int, creator func() interface{}) BatchReader {
	ctx, cancel := context.WithCancel(ctx)
	return &mockBatchReader{
		ctx:     ctx,
		cancel:  cancel,
		timer:   time.NewTimer(0),
		size:    batchSize,
		creator: creator,
	}
}

// mockBatchReader .
type mockBatchReader struct {
	ctx      context.Context
	cancel   func()
	interval time.Duration
	timer    *time.Timer
	size     int
	creator  func() interface{}
}

func (r *mockBatchReader) ReadN(buf []Data, timeout time.Duration) (int, error) {
	size := r.size
	if len(buf) < size {
		size = len(buf)
	}
	select {
	case <-r.ctx.Done():
		return 0, nil
	case <-r.timer.C:
		for i := 0; i < size; i++ {
			buf[i] = r.creator()
		}
	}
	r.timer.Reset(r.interval)
	return size, nil
}

func (r *mockBatchReader) Confirm() error { return nil }
func (r *mockBatchReader) Close() error {
	r.cancel()
	return nil
}
