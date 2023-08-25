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
	"time"

	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	sev1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
)

var (
	ctxKey = new(int)
)

type JobContext struct {
	Labels      map[string]string
	Annotations map[string]string
	Timeout     *time.Duration
	Mode        sev1alpha1.PodMigrationJobMode
}

func WithContext(ctx context.Context, jobCtx *JobContext) context.Context {
	return context.WithValue(ctx, ctxKey, jobCtx)
}

func FromContext(ctx context.Context) *JobContext {
	jobCtx, _ := ctx.Value(ctxKey).(*JobContext)
	return jobCtx
}

func (c *JobContext) ApplyTo(job *sev1alpha1.PodMigrationJob) error {
	if c == nil {
		return nil
	}
	if len(c.Labels) > 0 {
		if job.Labels == nil {
			job.Labels = make(map[string]string)
		}
		for k, v := range c.Labels {
			job.Labels[k] = v
		}
	}
	if len(c.Annotations) > 0 {
		if job.Annotations == nil {
			job.Annotations = make(map[string]string)
		}
		for k, v := range c.Annotations {
			job.Annotations[k] = v
		}
	}
	if c.Timeout != nil {
		job.Spec.TTL = &metav1.Duration{
			Duration: *c.Timeout,
		}
	}
	if c.Mode != "" {
		job.Spec.Mode = c.Mode
	}
	return nil
}
