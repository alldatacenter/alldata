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

package dao

import (
	"time"

	"github.com/sirupsen/logrus"
)

var (
	containerDiffCount = 0
	containerCount     = 0
	hostDiffCount      = 0
	hostCount          = 0
	upTime             = 0
)

// Count 记录cmdb运行时间，获取的容器数量，主机数量
func Count() {
	logrus.Info("start init Container Count....")
	for {
		logrus.Info("Container: ", containerCount)
		logrus.Info("ContainerDiff: ", containerDiffCount)
		logrus.Info("Host: ", hostCount)
		logrus.Info("UpTime: ", upTime)
		upTime++
		time.Sleep(60 * time.Second)
	}
}
