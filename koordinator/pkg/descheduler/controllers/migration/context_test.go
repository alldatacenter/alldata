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

package migration

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	sev1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
)

func TestJobContext(t *testing.T) {
	timeout := 1 * time.Minute
	expectJobCtx := &JobContext{
		Labels: map[string]string{
			"test-labels": "123",
		},
		Annotations: map[string]string{
			"test-annotations": "456",
		},
		Mode:    sev1alpha1.PodMigrationJobModeEvictionDirectly,
		Timeout: &timeout,
	}

	ctx := WithContext(context.TODO(), expectJobCtx)
	jobCtx := FromContext(ctx)
	assert.Equal(t, expectJobCtx, jobCtx)
	job := &sev1alpha1.PodMigrationJob{}
	err := jobCtx.ApplyTo(job)
	assert.NoError(t, err)
	expectJob := &sev1alpha1.PodMigrationJob{
		ObjectMeta: metav1.ObjectMeta{
			Labels: map[string]string{
				"test-labels": "123",
			},
			Annotations: map[string]string{
				"test-annotations": "456",
			},
		},
		Spec: sev1alpha1.PodMigrationJobSpec{
			Mode: sev1alpha1.PodMigrationJobModeEvictionDirectly,
			TTL:  &metav1.Duration{Duration: timeout},
		},
	}
	assert.Equal(t, expectJob, job)
}
