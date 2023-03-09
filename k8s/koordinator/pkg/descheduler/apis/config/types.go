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

package config

import (
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/component-base/config"
)

const (
	DefaultDeschedulerPort         = 10258
	DefaultInsecureDeschedulerPort = 10251
)

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// DeschedulerConfiguration configures a descheduler
type DeschedulerConfiguration struct {
	metav1.TypeMeta

	// LeaderElection defines the configuration of leader election client.
	LeaderElection config.LeaderElectionConfiguration

	// ClientConnection specifies the kubeconfig file and client connection
	// settings for the proxy server to use when communicating with the apiserver.
	ClientConnection config.ClientConnectionConfiguration

	// DebuggingConfiguration holds configuration for Debugging related features
	// TODO: We might wanna make this a substruct like Debugging componentbaseconfig.DebuggingConfiguration
	config.DebuggingConfiguration

	// HealthzBindAddress is the IP address and port for the health check server to serve on.
	HealthzBindAddress string
	// MetricsBindAddress is the IP address and port for the metrics server to serve on.
	MetricsBindAddress string

	// Time interval for descheduler to run
	DeschedulingInterval metav1.Duration

	// Dry run
	DryRun bool

	// Profiles are descheduling profiles that koord-descheduler supports.
	Profiles []DeschedulerProfile

	// NodeSelector for a set of nodes to operate over
	NodeSelector *metav1.LabelSelector
}

// DeschedulerProfile is a descheduling profile.
type DeschedulerProfile struct {
	Name         string
	PluginConfig []PluginConfig
	Plugins      *Plugins
}

type Plugins struct {
	Deschedule PluginSet
	Balance    PluginSet
	Evictor    PluginSet
}

type PluginSet struct {
	Enabled  []Plugin
	Disabled []Plugin
}

type Plugin struct {
	// Name defines the name of plugin
	Name string `json:"name"`
}

type PluginConfig struct {
	Name string
	Args runtime.Object
}

type (
	Percentage         float64
	ResourceThresholds map[corev1.ResourceName]Percentage
)
