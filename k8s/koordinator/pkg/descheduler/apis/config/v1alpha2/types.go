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

package v1alpha2

import (
	"bytes"
	"fmt"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/component-base/config/v1alpha1"
	"sigs.k8s.io/yaml"
)

// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object

// DeschedulerConfiguration configures a descheduler
type DeschedulerConfiguration struct {
	metav1.TypeMeta

	// LeaderElection defines the configuration of leader election client.
	LeaderElection v1alpha1.LeaderElectionConfiguration `json:"leaderElection"`

	// ClientConnection specifies the kubeconfig file and client connection
	// settings for the proxy server to use when communicating with the apiserver.
	ClientConnection v1alpha1.ClientConnectionConfiguration `json:"clientConnection"`

	// DebuggingConfiguration holds configuration for Debugging related features
	// TODO: We might wanna make this a substruct like Debugging componentbaseconfigv1alpha1.DebuggingConfiguration
	v1alpha1.DebuggingConfiguration `json:",inline"`

	// Note: Both HealthzBindAddress and MetricsBindAddress fields are deprecated.
	// Only empty address or port 0 is allowed. Anything else will fail validation.
	// HealthzBindAddress is the IP address and port for the health check server to serve on.
	HealthzBindAddress *string `json:"healthzBindAddress,omitempty"`
	// MetricsBindAddress is the IP address and port for the metrics server to serve on.
	MetricsBindAddress *string `json:"metricsBindAddress,omitempty"`

	// Time interval for descheduler to run
	DeschedulingInterval metav1.Duration `json:"deschedulingInterval,omitempty"`

	// Dry run
	DryRun bool `json:"dryRun,omitempty"`

	// Profiles
	Profiles []DeschedulerProfile `json:"profiles,omitempty"`

	// NodeSelector for a set of nodes to operate over
	NodeSelector *metav1.LabelSelector `json:"nodeSelector,omitempty"`
}

// DecodeNestedObjects decodes plugin args for known types.
func (c *DeschedulerConfiguration) DecodeNestedObjects(d runtime.Decoder) error {
	for i := range c.Profiles {
		prof := &c.Profiles[i]
		for j := range prof.PluginConfig {
			err := prof.PluginConfig[j].decodeNestedObjects(d)
			if err != nil {
				return fmt.Errorf("decoding .profiles[%d].pluginConfig[%d]: %w", i, j, err)
			}
		}
	}
	return nil
}

// EncodeNestedObjects encodes plugin args.
func (c *DeschedulerConfiguration) EncodeNestedObjects(e runtime.Encoder) error {
	for i := range c.Profiles {
		prof := &c.Profiles[i]
		for j := range prof.PluginConfig {
			err := prof.PluginConfig[j].encodeNestedObjects(e)
			if err != nil {
				return fmt.Errorf("encoding .profiles[%d].pluginConfig[%d]: %w", i, j, err)
			}
		}
	}
	return nil
}

// DeschedulerProfile is a descheduling profile.
type DeschedulerProfile struct {
	Name         string         `json:"name,omitempty"`
	PluginConfig []PluginConfig `json:"pluginConfig,omitempty"`
	Plugins      *Plugins       `json:"plugins,omitempty"`
}

type Plugins struct {
	Deschedule PluginSet `json:"deschedule,omitempty"`
	Balance    PluginSet `json:"balance,omitempty"`
	Evictor    PluginSet `json:"evict,omitempty"`
}

type PluginSet struct {
	Enabled  []Plugin `json:"enabled,omitempty"`
	Disabled []Plugin `json:"disabled,omitempty"`
}

type Plugin struct {
	// Name defines the name of plugin
	Name string `json:"name,omitempty"`
}

type PluginConfig struct {
	Name string               `json:"name"`
	Args runtime.RawExtension `json:"args,omitempty"`
}

type (
	Percentage         float64
	ResourceThresholds map[corev1.ResourceName]Percentage
)

func (c *PluginConfig) decodeNestedObjects(d runtime.Decoder) error {
	gvk := SchemeGroupVersion.WithKind(c.Name + "Args")
	// dry-run to detect and skip out-of-tree plugin args.
	if _, _, err := d.Decode(nil, &gvk, nil); runtime.IsNotRegisteredError(err) {
		return nil
	}

	obj, parsedGvk, err := d.Decode(c.Args.Raw, &gvk, nil)
	if err != nil {
		return fmt.Errorf("decoding args for plugin %s: %w", c.Name, err)
	}
	if parsedGvk.GroupKind() != gvk.GroupKind() {
		return fmt.Errorf("args for plugin %s were not of type %s, got %s", c.Name, gvk.GroupKind(), parsedGvk.GroupKind())
	}
	c.Args.Object = obj
	return nil
}

func (c *PluginConfig) encodeNestedObjects(e runtime.Encoder) error {
	if c.Args.Object == nil {
		return nil
	}
	var buf bytes.Buffer
	err := e.Encode(c.Args.Object, &buf)
	if err != nil {
		return err
	}
	// The <e> encoder might be a YAML encoder, but the parent encoder expects
	// JSON output, so we convert YAML back to JSON.
	// This is a no-op if <e> produces JSON.
	json, err := yaml.YAMLToJSON(buf.Bytes())
	if err != nil {
		return err
	}
	c.Args.Raw = json
	return nil
}
