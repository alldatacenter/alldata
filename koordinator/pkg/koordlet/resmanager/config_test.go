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

package resmanager

import (
	"flag"
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/resmanager/plugins"
)

func Test_NewDefaultConfig(t *testing.T) {
	expectConfig := &Config{
		ReconcileIntervalSeconds:   1,
		CPUSuppressIntervalSeconds: 1,
		CPUEvictIntervalSeconds:    1,
		MemoryEvictIntervalSeconds: 1,
		MemoryEvictCoolTimeSeconds: 4,
		CPUEvictCoolTimeSeconds:    20,
		QOSExtensionCfg:            &plugins.QOSExtensionConfig{FeatureGates: map[string]bool{}},
	}
	defaultConfig := NewDefaultConfig()
	assert.Equal(t, expectConfig, defaultConfig)
}

func Test_InitFlags(t *testing.T) {
	cmdArgs := []string{
		"",
		"--reconcile-interval-seconds=2",
		"--cpu-suppress-interval-seconds=2",
		"--cpu-evict-interval-seconds=2",
		"--memory-evict-interval-seconds=2",
		"--memory-evict-cool-time-seconds=8",
		"--cpu-evict-cool-time-seconds=40",
		"--qos-extension-plugins=test-plugin=true",
	}
	fs := flag.NewFlagSet(cmdArgs[0], flag.ExitOnError)

	type fields struct {
		ReconcileIntervalSeconds   int
		CPUSuppressIntervalSeconds int
		CPUEvictIntervalSeconds    int
		MemoryEvictIntervalSeconds int
		MemoryEvictCoolTimeSeconds int
		CPUEvictCoolTimeSeconds    int
		QOSExtensionCfg            *plugins.QOSExtensionConfig
	}
	type args struct {
		fs *flag.FlagSet
	}
	tests := []struct {
		name   string
		fields fields
		args   args
	}{
		{
			name: "not default",
			fields: fields{
				ReconcileIntervalSeconds:   2,
				CPUSuppressIntervalSeconds: 2,
				CPUEvictIntervalSeconds:    2,
				MemoryEvictIntervalSeconds: 2,
				MemoryEvictCoolTimeSeconds: 8,
				CPUEvictCoolTimeSeconds:    40,
				QOSExtensionCfg:            &plugins.QOSExtensionConfig{FeatureGates: map[string]bool{"test-plugin": true}},
			},
			args: args{fs: fs},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			raw := &Config{
				ReconcileIntervalSeconds:   tt.fields.ReconcileIntervalSeconds,
				CPUSuppressIntervalSeconds: tt.fields.CPUSuppressIntervalSeconds,
				CPUEvictIntervalSeconds:    tt.fields.CPUEvictIntervalSeconds,
				MemoryEvictIntervalSeconds: tt.fields.MemoryEvictIntervalSeconds,
				MemoryEvictCoolTimeSeconds: tt.fields.MemoryEvictCoolTimeSeconds,
				CPUEvictCoolTimeSeconds:    tt.fields.CPUEvictCoolTimeSeconds,
				QOSExtensionCfg:            tt.fields.QOSExtensionCfg,
			}
			c := NewDefaultConfig()
			c.InitFlags(tt.args.fs)
			tt.args.fs.Parse(cmdArgs[1:])
			assert.Equal(t, raw, c)
		})
	}
}
