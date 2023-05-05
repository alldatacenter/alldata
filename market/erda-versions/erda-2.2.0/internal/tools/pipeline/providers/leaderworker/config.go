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

package leaderworker

import (
	"time"
)

type config struct {
	Leader leaderConfig `file:"leader"`
	Worker workerConfig `file:"worker"`
}

type leaderConfig struct {
	IsWorker               bool          `file:"is_worker" env:"LEADER_IS_WORKER" default:"true"`
	CleanupInterval        time.Duration `file:"cleanup_interval" default:"1m"`
	RetryInterval          time.Duration `file:"retry_interval" default:"5s"`
	EtcdKeyPrefixWithSlash string        `file:"etcd_key_prefix_with_slash"`
}

type workerConfig struct {
	Candidate              candidateWorkerConfig `file:"candidate"`
	EtcdKeyPrefixWithSlash string                `file:"etcd_key_prefix_with_slash"`
	LivenessProbeInterval  time.Duration         `file:"liveness_probe_interval" default:"10s"`
	Task                   workerTaskConfig      `file:"task"`
	Heartbeat              workerHeartbeatConfig `file:"heartbeat"`
	RetryInterval          time.Duration         `file:"retry_interval" default:"10s"`
}

type workerTaskConfig struct {
	RetryDeleteTaskInterval time.Duration `file:"retry_delete_task_interval" default:"30s"`
}

type workerHeartbeatConfig struct {
	ReportInterval                     time.Duration `file:"report_interval" default:"5s"`
	AllowedMaxContinueLostContactTimes int           `file:"allowed_max_continue_lost_contact_times" default:"3"`
}

type candidateWorkerConfig struct {
	ThresholdToBecomeOfficial time.Duration `file:"threshold_to_become_official" env:"THRESHOLD_TO_BECOME_OFFICIAL_WORKER" default:"15s"`
}
