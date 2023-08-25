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

package audit

import (
	"flag"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func Test_NewDefaultConfig(t *testing.T) {
	expectConfig := &Config{
		LogDir:               "/var/log/koordlet",
		Verbose:              3,
		MaxDiskSpaceMB:       16,
		MaxConcurrentReaders: 4,
		ActiveReaderTTL:      time.Minute * 10,
		DefaultEventsLimit:   256,
		MaxEventsLimit:       2048,
		TickerDuration:       time.Minute,
	}
	defaultConfig := NewDefaultConfig()
	assert.Equal(t, expectConfig, defaultConfig)
}

func Test_InitFlags(t *testing.T) {
	cmdArgs := []string{
		"",
		"--audit-log-dir=/tmp/log/koordlet",
		"--audit-verbose=4",
		"--audit-max-disk-space-mb=32",
	}
	fs := flag.NewFlagSet(cmdArgs[0], flag.ExitOnError)

	type fields struct {
		LogDir         string
		Verbose        int
		MaxDiskSpaceMB int
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
				LogDir:         "/tmp/log/koordlet",
				Verbose:        4,
				MaxDiskSpaceMB: 32,
			},
			args: args{fs: fs},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			raw := &Config{
				LogDir:               tt.fields.LogDir,
				Verbose:              tt.fields.Verbose,
				MaxDiskSpaceMB:       tt.fields.MaxDiskSpaceMB,
				MaxConcurrentReaders: 4,
				ActiveReaderTTL:      time.Minute * 10,
				DefaultEventsLimit:   256,
				MaxEventsLimit:       2048,
				TickerDuration:       time.Minute,
			}
			c := NewDefaultConfig()
			c.InitFlags(tt.args.fs)
			tt.args.fs.Parse(cmdArgs[1:])
			assert.Equal(t, raw, c)
		})
	}
}
