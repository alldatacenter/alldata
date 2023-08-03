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

package nodemetric

import (
	"reflect"
	"testing"

	"k8s.io/utils/pointer"

	"github.com/koordinator-sh/koordinator/apis/extension"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
)

func Test_getNodeMetricCollectPolicy(t *testing.T) {
	tests := []struct {
		name    string
		config  *extension.ColocationStrategy
		want    *slov1alpha1.NodeMetricCollectPolicy
		wantErr bool
	}{
		{
			name:    "empty config",
			want:    nil,
			wantErr: true,
		},
		{
			name: "config disabled",
			config: &extension.ColocationStrategy{
				Enable: pointer.Bool(false),
			},
			want:    nil,
			wantErr: true,
		},
		{
			name: "config enabled",
			config: &extension.ColocationStrategy{
				Enable:                         pointer.Bool(true),
				MetricAggregateDurationSeconds: pointer.Int64(60),
				MetricReportIntervalSeconds:    pointer.Int64(180),
			},
			want: &slov1alpha1.NodeMetricCollectPolicy{
				AggregateDurationSeconds: pointer.Int64(60),
				ReportIntervalSeconds:    pointer.Int64(180),
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := getNodeMetricCollectPolicy(tt.config)
			if (err != nil) != tt.wantErr {
				t.Errorf("getNodeMetricCollectPolicy() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("getNodeMetricCollectPolicy() got = %v, want %v", got, tt.want)
			}
		})
	}
}
