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
	"fmt"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	"k8s.io/apimachinery/pkg/util/validation/field"

	"github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config"
)

// ValidateLoadAwareSchedulingArgs validates that LoadAwareSchedulingArgs are correct.
func ValidateLoadAwareSchedulingArgs(args *config.LoadAwareSchedulingArgs) error {
	var allErrs field.ErrorList

	if args.NodeMetricExpirationSeconds != nil && *args.NodeMetricExpirationSeconds <= 0 {
		allErrs = append(allErrs, field.Invalid(field.NewPath("nodeMetricExpiredSeconds"), *args.NodeMetricExpirationSeconds, "nodeMetricExpiredSeconds should be a positive value"))
	}

	if err := validateResourceWeights(args.ResourceWeights); err != nil {
		allErrs = append(allErrs, field.Invalid(field.NewPath("resourceWeights"), args.ResourceWeights, err.Error()))
	}
	if err := validateResourceThresholds(args.UsageThresholds); err != nil {
		allErrs = append(allErrs, field.Invalid(field.NewPath("usageThresholds"), args.UsageThresholds, err.Error()))
	}
	if err := validateEstimatedResourceThresholds(args.EstimatedScalingFactors); err != nil {
		allErrs = append(allErrs, field.Invalid(field.NewPath("estimatedScalingFactors"), args.EstimatedScalingFactors, err.Error()))
	}

	for resourceName := range args.ResourceWeights {
		if _, ok := args.EstimatedScalingFactors[resourceName]; !ok {
			allErrs = append(allErrs, field.NotFound(field.NewPath("estimatedScalingFactors"), resourceName))
			break
		}
	}

	if len(allErrs) == 0 {
		return nil
	}
	return allErrs.ToAggregate()
}

func validateResourceWeights(resources map[corev1.ResourceName]int64) error {
	for resourceName, weight := range resources {
		if weight <= 0 {
			return fmt.Errorf("resource Weight of %v should be a positive value, got %v", resourceName, weight)
		}
		if weight > 100 {
			return fmt.Errorf("resource Weight of %v should be less than 100, got %v", resourceName, weight)
		}
	}
	return nil
}

func validateResourceThresholds(thresholds map[corev1.ResourceName]int64) error {
	for resourceName, thresholdPercent := range thresholds {
		if thresholdPercent < 0 {
			return fmt.Errorf("resource Threshold of %v should be a positive value, got %v", resourceName, thresholdPercent)
		}
		if thresholdPercent > 100 {
			return fmt.Errorf("resource Threshold of %v should be less than 100, got %v", resourceName, thresholdPercent)
		}
	}
	return nil
}

func validateEstimatedResourceThresholds(thresholds map[corev1.ResourceName]int64) error {
	for resourceName, thresholdPercent := range thresholds {
		if thresholdPercent <= 0 {
			return fmt.Errorf("estimated resource Threshold of %v should be a positive value, got %v", resourceName, thresholdPercent)
		}
		if thresholdPercent > 100 {
			return fmt.Errorf("estimated  resource Threshold of %v should be less than 100, got %v", resourceName, thresholdPercent)
		}
	}
	return nil
}

func ValidateElasticQuotaArgs(elasticArgs *config.ElasticQuotaArgs) error {
	for resName, q := range elasticArgs.DefaultQuotaGroupMax {
		if q.Cmp(*resource.NewQuantity(0, resource.DecimalSI)) == -1 {
			return fmt.Errorf("elasticQuotaArgs error, defaultQuotaGroupMax should be a positive value, resourceName:%v, got %v",
				resName, q)
		}
	}

	for resName, q := range elasticArgs.SystemQuotaGroupMax {
		if q.Cmp(*resource.NewQuantity(0, resource.DecimalSI)) == -1 {
			return fmt.Errorf("elasticQuotaArgs error, systemQuotaGroupMax should be a positive value, resourceName:%v, got %v",
				resName, q)
		}
	}

	if elasticArgs.DelayEvictTime != nil && elasticArgs.DelayEvictTime.Duration < 0 {
		return fmt.Errorf("elasticQuotaArgs error, DelayEvictTime should be a positive value")
	}

	if elasticArgs.RevokePodInterval != nil && elasticArgs.RevokePodInterval.Duration < 0 {
		return fmt.Errorf("elasticQuotaArgs error, RevokePodCycle should be a positive value")
	}

	return nil
}

func ValidateCoschedulingArgs(coeSchedulingArgs *config.CoschedulingArgs) error {
	if coeSchedulingArgs.DefaultTimeout != nil && coeSchedulingArgs.DefaultTimeout.Duration < 0 {
		return fmt.Errorf("coeSchedulingArgs DefaultTimeoutSeconds invalid")
	}
	if coeSchedulingArgs.ControllerWorkers != nil && *coeSchedulingArgs.ControllerWorkers < 1 {
		return fmt.Errorf("coeSchedulingArgs ControllerWorkers invalid")
	}
	return nil
}
