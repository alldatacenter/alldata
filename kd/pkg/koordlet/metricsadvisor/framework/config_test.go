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

package framework

import (
	"flag"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func Test_NewDefaultConfig(t *testing.T) {
	expectConfig := &Config{
		CollectResUsedInterval:     1 * time.Second,
		CollectNodeCPUInfoInterval: 60 * time.Second,
		CPICollectorInterval:       60 * time.Second,
		PSICollectorInterval:       10 * time.Second,
		CPICollectorTimeWindow:     10 * time.Second,
	}
	defaultConfig := NewDefaultConfig()
	assert.Equal(t, expectConfig, defaultConfig)
}

func Test_InitFlags(t *testing.T) {
	cmdArgs := []string{
		"",
		"--collect-res-used-interval=3s",
		"--collect-node-cpu-info-interval=90s",
		"--cpi-collector-interval=90s",
		"--psi-collector-interval=5s",
		"--collect-cpi-timewindow=15s",
	}
	fs := flag.NewFlagSet(cmdArgs[0], flag.ExitOnError)

	type fields struct {
		CollectResUsedInterval     time.Duration
		CollectNodeCPUInfoInterval time.Duration
		CPICollectorInterval       time.Duration
		PSICollectorInterval       time.Duration
		CPICollectorTimeWindow     time.Duration
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
				CollectResUsedInterval:     3 * time.Second,
				CollectNodeCPUInfoInterval: 90 * time.Second,
				CPICollectorInterval:       90 * time.Second,
				PSICollectorInterval:       5 * time.Second,
				CPICollectorTimeWindow:     15 * time.Second,
			},
			args: args{fs: fs},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			raw := &Config{
				CollectResUsedInterval:     tt.fields.CollectResUsedInterval,
				CollectNodeCPUInfoInterval: tt.fields.CollectNodeCPUInfoInterval,
				CPICollectorInterval:       tt.fields.CPICollectorInterval,
				PSICollectorInterval:       tt.fields.PSICollectorInterval,
				CPICollectorTimeWindow:     tt.fields.CPICollectorTimeWindow,
			}
			c := NewDefaultConfig()
			c.InitFlags(tt.args.fs)
			tt.args.fs.Parse(cmdArgs[1:])
			assert.Equal(t, raw, c)
		})
	}
}
