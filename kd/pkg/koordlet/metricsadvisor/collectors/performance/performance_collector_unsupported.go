//go:build !linux
// +build !linux

/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package performance

import (
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metricsadvisor/framework"
)

type performanceCollector struct{}

func New(opt *framework.Options) framework.Collector {
	return &performanceCollector{}
}

func (p *performanceCollector) Enabled() bool {
	return false
}

func (p *performanceCollector) Setup(c *framework.Context) {}

func (p *performanceCollector) Run(stopCh <-chan struct{}) {}

func (p *performanceCollector) Started() bool {
	return false
}
