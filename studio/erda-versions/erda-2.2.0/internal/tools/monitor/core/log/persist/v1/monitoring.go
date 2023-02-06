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

package storage

import (
	"github.com/prometheus/client_golang/prometheus"
)

var (
	logBytesCounter = prometheus.NewCounterVec(
		prometheus.CounterOpts{
			Name:      "log_bytes",
			Subsystem: "log_storage",
			Help:      "the size of consumed log",
		},
		[]string{levelKey, srcKey, srcComponentTypeKey, srcComponentNameKey, srcClusterNameKey, srcOrgNameKey, srcProjectIDKey, srcProjectNameKey, srcApplicationIDKey, srcApplicationNameKey, srcWorkspaceKey})

	logInvalidCounter = prometheus.NewCounterVec(
		prometheus.CounterOpts{
			Name:      "log_invalid_count",
			Subsystem: "log_storage",
			Help:      "the count of invalid log since started",
		},
		[]string{diceOrgNameKey},
	)
)

func init() {
	prometheus.MustRegister(logBytesCounter, logInvalidCounter)
}
