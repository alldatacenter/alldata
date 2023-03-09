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

package runtimehooks

import (
	"flag"
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

func Test_NewDefaultConfig(t *testing.T) {
	expectConfig := &Config{
		RuntimeHooksNetwork:       "unix",
		RuntimeHooksAddr:          "/host-var-run-koordlet/koordlet.sock",
		RuntimeHooksFailurePolicy: "Ignore",
		RuntimeHookConfigFilePath: system.Conf.RuntimeHooksConfigDir,
		RuntimeHookHostEndpoint:   "/var/run/koordlet/koordlet.sock",
		RuntimeHookDisableStages:  []string{},
		FeatureGates:              map[string]bool{},
	}
	defaultConfig := NewDefaultConfig()
	assert.Equal(t, expectConfig, defaultConfig)
}

func Test_InitFlags(t *testing.T) {
	cfg := NewDefaultConfig()
	cfg.InitFlags(flag.CommandLine)
	flag.Parse()
}
