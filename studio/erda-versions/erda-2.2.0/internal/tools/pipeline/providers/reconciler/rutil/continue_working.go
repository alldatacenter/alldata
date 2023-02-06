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

package rutil

import (
	"context"
	"fmt"
	"time"

	"github.com/erda-project/erda-infra/base/logs"
)

type WaitDuration time.Duration

var (
	ContinueWorkingAbort               WaitDuration = -3 // negative numbers are reserved for loop control.
	ContinueWorkingImmediately         WaitDuration = -2
	ContinueWorkingWithDefaultInterval WaitDuration = -1
)

type ContinueWorkingOption struct {
	DefaultRetryInterval time.Duration
}

type ContinueWorkerOptionFunc func(*ContinueWorkingOption)

func WithContinueWorkingDefaultRetryInterval(interval time.Duration) ContinueWorkerOptionFunc {
	return func(option *ContinueWorkingOption) {
		if option.DefaultRetryInterval <= 0 {
			panic(fmt.Errorf("default duration must > 0"))
		}
		option.DefaultRetryInterval = interval
	}
}

func ContinueWorkingWithCustomInterval(d time.Duration) WaitDuration { return WaitDuration(d) }

func ContinueWorking(ctx context.Context, logger logs.Logger, f func(ctx context.Context) WaitDuration, opFuncs ...ContinueWorkerOptionFunc) {
	opt := ContinueWorkingOption{
		DefaultRetryInterval: time.Second * 5,
	}
	for _, opFunc := range opFuncs {
		opFunc(&opt)
	}
	for {
		select {
		case <-ctx.Done():
			logger.Warnf("cancel continuous working, context done, reason: %v", ctx.Err())
			return
		default:
			waitDuration := f(ctx)
			switch waitDuration {
			case ContinueWorkingAbort:
				return
			case ContinueWorkingImmediately:
				continue
			case ContinueWorkingWithDefaultInterval:
				time.Sleep(opt.DefaultRetryInterval)
				continue
			default:
				time.Sleep(time.Duration(waitDuration))
				continue
			}
		}
	}
}
