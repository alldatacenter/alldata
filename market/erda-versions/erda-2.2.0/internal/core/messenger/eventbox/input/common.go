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

package input

import (
	"context"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/core/messenger/eventbox/dispatcher/errors"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/types"
	"github.com/erda-project/erda/pkg/dlock"
)

type Handler func(*types.Message) *errors.DispatchError

func OnlyOne(ctx context.Context, lock *dlock.DLock) (func(), error) {
	var isCanceled bool
	var locked bool

	cleanup := func() {
		if isCanceled {
			return
		}
		logrus.Infof("Onlyone: etcdlock: unlock, key: %s", lock.Key())
		if err := lock.Unlock(); err != nil {
			logrus.Errorf("Onlyone: etcdlock unlock err: %v", err)
			return
		}

	}

	go func() {
		time.Sleep(3 * time.Second)
		if !locked && !isCanceled {
			logrus.Warnf("Onlyone: not get lock yet after 3s")
		}
	}()
	if err := lock.Lock(ctx); err != nil {
		if err == context.Canceled {
			isCanceled = true
			logrus.Infof("Onlyone: etcdlock: %v", err)
			return cleanup, nil
		}
		return cleanup, err
	}
	locked = true
	logrus.Infof("Onlyone: etcdlock: lock, key: %s", lock.Key())

	return cleanup, nil
}
