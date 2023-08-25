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
	"time"

	corev1 "k8s.io/api/core/v1"
)

type Config struct {
	KubeletPreferredAddressType string
	KubeletSyncInterval         time.Duration
	KubeletSyncTimeout          time.Duration
	InsecureKubeletTLS          bool
	KubeletReadOnlyPort         uint
	NodeTopologySyncInterval    time.Duration
	DisableQueryKubeletConfig   bool
	EnableNodeMetricReport      bool
	MetricReportInterval        time.Duration // Deprecated
}

func NewDefaultConfig() *Config {
	return &Config{
		KubeletPreferredAddressType: string(corev1.NodeInternalIP),
		KubeletSyncInterval:         10 * time.Second,
		KubeletSyncTimeout:          3 * time.Second,
		InsecureKubeletTLS:          false,
		KubeletReadOnlyPort:         10255,
		NodeTopologySyncInterval:    3 * time.Second,
		DisableQueryKubeletConfig:   false,
		EnableNodeMetricReport:      true,
	}
}

func (c *Config) InitFlags(fs *flag.FlagSet) {
	fs.StringVar(&c.KubeletPreferredAddressType, "kubelet-preferred-address-type", c.KubeletPreferredAddressType, "The node address types to use when determining which address to use to connect to a particular node.")
	fs.DurationVar(&c.KubeletSyncInterval, "kubelet-sync-interval", c.KubeletSyncInterval, "The interval at which Koordlet will retain data from Kubelet. Non-zero values should contain a corresponding time unit (e.g. 1s, 2m, 3h).")
	fs.DurationVar(&c.KubeletSyncTimeout, "kubelet-sync-timeout", c.KubeletSyncTimeout, "The length of time to wait before giving up on a single request to Kubelet. Non-zero values should contain a corresponding time unit (e.g. 1s, 2m, 3h).")
	fs.BoolVar(&c.InsecureKubeletTLS, "kubelet-insecure-tls", c.InsecureKubeletTLS, "Using read-only port to communicate with Kubelet. For testing purposes only, not recommended for production use.")
	fs.UintVar(&c.KubeletReadOnlyPort, "kubelet-read-only-port", c.KubeletReadOnlyPort, "The read-only port for the kubelet to serve on with no authentication/authorization. Default: 10255.")
	fs.DurationVar(&c.NodeTopologySyncInterval, "node-topology-sync-interval", c.NodeTopologySyncInterval, "The interval which Koordlet will report the node topology info, include cpu and gpu")
	fs.BoolVar(&c.DisableQueryKubeletConfig, "disable-query-kubelet-config", c.DisableQueryKubeletConfig, "Disables querying the kubelet configuration from kubelet. Flag must be set to true if kubelet-insecure-tls=true is configured")
	fs.DurationVar(&c.MetricReportInterval, "report-interval", c.MetricReportInterval, "Deprecated since v1.1, use ColocationStrategy.MetricReportIntervalSeconds in config map of slo-controller")
	fs.BoolVar(&c.EnableNodeMetricReport, "enable-node-metric-report", c.EnableNodeMetricReport, "Enable status update of node metric crd.")
}
