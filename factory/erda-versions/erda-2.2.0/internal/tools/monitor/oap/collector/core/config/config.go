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

package config

import (
	"time"
)

// The global common config for pipeline component
type Batch struct {
}

type Config struct {
	Pipelines PipelineWrap `file:"pipelines"`
}

type Pipeline struct {
	Enable        *bool         `file:"_enable" desc:"pipeline enable or not"`
	BatchSize     int           `file:"batch_size" desc:"the batch max size for per exporter"`
	FlushInterval time.Duration `file:"flush_interval"  desc:"the ticker for per exporter"`
	FlushJitter   time.Duration `file:"flush_jitter"  desc:"the ticker jitter for per exporter"`
	Receivers     []string      `file:"receivers"`
	Processors    []string      `file:"processors"`
	Exporters     []string      `file:"exporters"`
}

type PipelineWrap struct {
	Metrics []Pipeline `file:"metrics"`
	Logs    []Pipeline `file:"logs"`
	Spans   []Pipeline `file:"spans"`
	Raws    []Pipeline `file:"raws"`
}
