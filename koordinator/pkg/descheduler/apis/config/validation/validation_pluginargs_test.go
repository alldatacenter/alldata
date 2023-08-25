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
	"time"

	"github.com/stretchr/testify/assert"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/utils/pointer"

	deschedulerconfig "github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config/v1alpha2"
)

func TestValidateMigrationControllerArgs(t *testing.T) {
	tests := []struct {
		name    string
		args    *v1alpha2.MigrationControllerArgs
		wantErr bool
	}{
		{
			name:    "default args",
			args:    &v1alpha2.MigrationControllerArgs{},
			wantErr: false,
		},
		{
			name: "invalid evictQPS",
			args: &v1alpha2.MigrationControllerArgs{
				EvictQPS: &deschedulerconfig.Float64OrString{
					Type:   deschedulerconfig.String,
					StrVal: "xxxx",
				},
			},
			wantErr: true,
		},
		{
			name: "invalid evictBurst",
			args: &v1alpha2.MigrationControllerArgs{
				EvictQPS: &deschedulerconfig.Float64OrString{
					Type:     deschedulerconfig.Float,
					FloatVal: 11,
				},
				EvictBurst: pointer.Int32(0),
			},
			wantErr: true,
		},
		{
			name: "invalid labelSelector",
			args: &v1alpha2.MigrationControllerArgs{
				LabelSelector: &metav1.LabelSelector{
					MatchLabels: map[string]string{
						"test/a/b/c": "123",
					},
				},
			},
			wantErr: true,
		},
		{
			name: "invalid maxConcurrentReconciles",
			args: &v1alpha2.MigrationControllerArgs{
				MaxConcurrentReconciles: pointer.Int32(-1),
			},
			wantErr: true,
		},
		{
			name: "invalid defaultJobMode",
			args: &v1alpha2.MigrationControllerArgs{
				DefaultJobMode: "unsupportedMode",
			},
			wantErr: true,
		},
		{
			name: "invalid defaultJobTTL",
			args: &v1alpha2.MigrationControllerArgs{
				DefaultJobTTL: &metav1.Duration{Duration: -10 * time.Minute},
			},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			v1alpha2.SetDefaults_MigrationControllerArgs(tt.args)
			args := &deschedulerconfig.MigrationControllerArgs{}
			assert.NoError(t, v1alpha2.Convert_v1alpha2_MigrationControllerArgs_To_config_MigrationControllerArgs(tt.args, args, nil))
			if err := ValidateMigrationControllerArgs(nil, args); (err != nil) != tt.wantErr {
				t.Errorf("ValidateMigrationControllerArgs() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
