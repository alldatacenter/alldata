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

package statesinformer

import (
	"flag"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
)

func TestNewDefaultConfig(t *testing.T) {
	tests := []struct {
		name string
		want *Config
	}{
		{
			name: "config",
			want: &Config{
				KubeletPreferredAddressType: string(corev1.NodeInternalIP),
				KubeletSyncInterval:         10 * time.Second,
				KubeletSyncTimeout:          3 * time.Second,
				InsecureKubeletTLS:          false,
				KubeletReadOnlyPort:         10255,
				NodeTopologySyncInterval:    3 * time.Second,
				DisableQueryKubeletConfig:   false,
				EnableNodeMetricReport:      true,
				MetricReportInterval:        0,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := NewDefaultConfig()
			assert.Equal(t, tt.want, got)
		})
	}
}

func TestConfig_InitFlags(t *testing.T) {
	cmdArgs := []string{
		"",
		"--kubelet-preferred-address-type=Hostname",
		"--kubelet-sync-interval=30s",
		"--kubelet-sync-timeout=10s",
		"--kubelet-insecure-tls=true",
		"--kubelet-read-only-port=10258",
		"--node-topology-sync-interval=10s",
		"--disable-query-kubelet-config=true",
		"--enable-node-metric-report=false",
	}
	fs := flag.NewFlagSet(cmdArgs[0], flag.ExitOnError)

	type fields struct {
		KubeletPreferredAddressType string
		KubeletSyncInterval         time.Duration
		KubeletSyncTimeout          time.Duration
		InsecureKubeletTLS          bool
		KubeletReadOnlyPort         uint
		NodeTopologySyncInterval    time.Duration
		DisableQueryKubeletConfig   bool
		EnableNodeMetricReport      bool
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
				KubeletPreferredAddressType: "Hostname",
				KubeletSyncInterval:         30 * time.Second,
				KubeletSyncTimeout:          10 * time.Second,
				InsecureKubeletTLS:          true,
				KubeletReadOnlyPort:         10258,
				NodeTopologySyncInterval:    10 * time.Second,
				DisableQueryKubeletConfig:   true,
				EnableNodeMetricReport:      false,
			},
			args: args{fs: fs},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			raw := &Config{
				KubeletPreferredAddressType: tt.fields.KubeletPreferredAddressType,
				KubeletSyncInterval:         tt.fields.KubeletSyncInterval,
				KubeletSyncTimeout:          tt.fields.KubeletSyncTimeout,
				InsecureKubeletTLS:          tt.fields.InsecureKubeletTLS,
				KubeletReadOnlyPort:         tt.fields.KubeletReadOnlyPort,
				NodeTopologySyncInterval:    tt.fields.NodeTopologySyncInterval,
				DisableQueryKubeletConfig:   tt.fields.DisableQueryKubeletConfig,
				EnableNodeMetricReport:      tt.fields.EnableNodeMetricReport,
			}
			c := NewDefaultConfig()
			c.InitFlags(tt.args.fs)
			err := tt.args.fs.Parse(cmdArgs[1:])
			assert.NoError(t, err)
			assert.Equal(t, raw, c)
		})
	}
}
