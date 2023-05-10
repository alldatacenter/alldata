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

package queuemanager

import (
	"time"
)

type config struct {
	IncomingPipelineCfg IncomingPipelineCfg `file:"incoming_pipeline"`
}

type IncomingPipelineCfg struct {
	EtcdKeyPrefixWithSlash         string        `file:"etcd_key_prefix_with_slash"`
	RetryInterval                  time.Duration `file:"retry_interval" default:"10s"`
	IntervalOfLoadRunningPipelines time.Duration `file:"interval_of_load_running_pipelines" env:"INTERVAL_OF_LOAD_RUNNING_PIPELINES" default:"30s"`
}
