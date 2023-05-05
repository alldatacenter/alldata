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

package agenttool

import (
	"fmt"
	"math"
	"os"
	"path/filepath"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/filehelper"
)

const (
	lockFileName                 = "cache.lock"
	cacheLogPrefix               = "[Cache]"
	declineRatio   float64       = 1.5
	declineLimit   time.Duration = 1 * time.Second
)

func LockPath(path string) error {
	return filehelper.CreateFile(filepath.Join(path, lockFileName), "", 0755)
}

func UnLockPath(path string) error {
	lockFile := filepath.Join(path, lockFileName)
	if err := filehelper.CheckExist(lockFile, false); err != nil {
		return nil
	}
	return os.Remove(lockFile)
}

func IsPathBeLocked(path string) bool {
	lockFile := filepath.Join(path, lockFileName)
	if err := filehelper.CheckExist(lockFile, false); err != nil {
		return false
	}
	return true
}

// WaitingPathUnlock check path is locked at first, then check every second until unlocked or time limited
func WaitingPathUnlock(path string, maxLimitSec int) error {
	if !IsPathBeLocked(path) {
		return nil
	}
	var checkedTimes uint64
	limitTimer := time.NewTimer(time.Duration(float64(maxLimitSec) * float64(time.Second)))
	checkTimer := time.NewTimer(calculateNextCheckTimeDuration(checkedTimes))
	defer limitTimer.Stop()
	defer checkTimer.Stop()

	for {
		select {
		case <-limitTimer.C:
			return fmt.Errorf("%s after %d seconds, the path: %s is still not unlocked", cacheLogPrefix, maxLimitSec, path)
		case <-checkTimer.C:
			if IsPathBeLocked(path) {
				logrus.Warnf("%s the path: %s is still unlocked, continue check", cacheLogPrefix, path)
				checkedTimes++
				checkTimer.Reset(calculateNextCheckTimeDuration(checkedTimes))
				continue
			}

			return nil
		}
	}
}

func calculateNextCheckTimeDuration(checkedTimes uint64) time.Duration {
	lastCheckInterval := 100 * time.Millisecond
	lastCheckInterval = time.Duration(float64(lastCheckInterval) * math.Pow(declineRatio, float64(checkedTimes)))
	if lastCheckInterval > declineLimit {
		return declineLimit
	}
	return lastCheckInterval
}
