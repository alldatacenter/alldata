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

package validation

import (
	"testing"

	"github.com/stretchr/testify/assert"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/utils/pointer"

	deschedulerconfig "github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config/v1alpha2"
)

func TestValidateDeschedulerConfiguration(t *testing.T) {
	tests := []struct {
		name    string
		args    *v1alpha2.DeschedulerConfiguration
		wantErr bool
	}{
		{
			name:    "default args",
			args:    &v1alpha2.DeschedulerConfiguration{},
			wantErr: false,
		},
		{
			name: "duplicate profile name",
			args: &v1alpha2.DeschedulerConfiguration{
				Profiles: []v1alpha2.DeschedulerProfile{
					{
						Name: "profile-1",
					},
					{
						Name: "profile-1",
					},
				},
			},
			wantErr: true,
		},
		{
			name: "invalid healthzBindAddress",
			args: &v1alpha2.DeschedulerConfiguration{
				HealthzBindAddress: pointer.String("0.0.0.0:111:222"),
			},
			wantErr: true,
		},
		{
			name: "invalid metricsBindAddress",
			args: &v1alpha2.DeschedulerConfiguration{
				MetricsBindAddress: pointer.String("0.0.0.0:111:222"),
			},
			wantErr: true,
		},
		{
			name: "invalid nodeSelector",
			args: &v1alpha2.DeschedulerConfiguration{
				NodeSelector: &metav1.LabelSelector{
					MatchLabels: map[string]string{
						"test/a/b/c/d": "test",
					},
				},
			},
			wantErr: true,
		},
		{
			name: "duplicate plugin config",
			args: &v1alpha2.DeschedulerConfiguration{
				Profiles: []v1alpha2.DeschedulerProfile{
					{
						Name: "test",
						PluginConfig: []v1alpha2.PluginConfig{
							{
								Name: "test",
								Args: runtime.RawExtension{
									Raw: []byte(`{}`),
								},
							},
							{
								Name: "test",
								Args: runtime.RawExtension{
									Raw: []byte(`{}`),
								},
							},
						},
					},
				},
			},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			v1alpha2.SetDefaults_DeschedulerConfiguration(tt.args)
			args := &deschedulerconfig.DeschedulerConfiguration{}
			assert.NoError(t, v1alpha2.Convert_v1alpha2_DeschedulerConfiguration_To_config_DeschedulerConfiguration(tt.args, args, nil))
			if err := ValidateDeschedulerConfiguration(args); (err != nil) != tt.wantErr {
				t.Errorf("ValidateDeschedulerConfiguration() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
