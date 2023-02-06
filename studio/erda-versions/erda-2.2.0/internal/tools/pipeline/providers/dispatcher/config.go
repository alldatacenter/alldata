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

package dispatcher

import (
	"time"
)

type config struct {
	Concurrency   int           `file:"concurrency" default:"100"`
	RetryInterval time.Duration `file:"retry_interval" env:"DISPATCH_RETRY_INTERVAL" default:"5s"`
	Consistent    consistentConfig
}

type consistentConfig struct {
	PartitionCount    int     `file:"partition_count" env:"DISPATCHER_CONSISTENT_PARTITION_COUNT" default:"7"`
	ReplicationFactor int     `file:"replication_factor" env:"DISPATCHER_CONSISTENT_REPLICATION_FACTOR" default:"20"`
	Load              float64 `file:"load" env:"DISPATCHER_CONSISTENT_LOAD" default:"1.25"`
}
