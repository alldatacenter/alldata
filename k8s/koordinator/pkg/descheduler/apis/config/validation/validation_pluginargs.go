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
	"strconv"

	metav1validation "k8s.io/apimachinery/pkg/apis/meta/v1/validation"
	"k8s.io/apimachinery/pkg/util/intstr"
	"k8s.io/apimachinery/pkg/util/validation/field"

	sev1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	deschedulerconfig "github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config"
)

func ValidateRemovePodsViolatingNodeAffinityArgs(path *field.Path, args *deschedulerconfig.RemovePodsViolatingNodeAffinityArgs) error {
	var allErrs field.ErrorList

	if len(args.NodeAffinityType) == 0 {
		allErrs = append(allErrs, field.Invalid(path.Child("nodeAffinityType"), args.NodeAffinityType, "nodeAffinityType should not be empty"))
	}
	// At most one of include/exclude can be set
	if args.Namespaces != nil && len(args.Namespaces.Include) > 0 && len(args.Namespaces.Exclude) > 0 {
		allErrs = append(allErrs, field.Invalid(path.Child("namespaces"), args.Namespaces, "only one of Include/Exclude namespaces can be set"))
	}

	if len(allErrs) == 0 {
		return nil
	}
	return allErrs.ToAggregate()
}

func ValidateMigrationControllerArgs(path *field.Path, args *deschedulerconfig.MigrationControllerArgs) error {
	var allErrs field.ErrorList

	if args.MaxMigratingPerNamespace != nil && *args.MaxMigratingPerNamespace < 0 {
		allErrs = append(allErrs, field.Invalid(path.Child("maxMigratingPerNamespace"), *args.MaxMigratingPerNamespace, "maxMigratingPerNamespace should be greater or equal 0"))
	}

	if args.MaxMigratingPerNode != nil && *args.MaxMigratingPerNode < 0 {
		allErrs = append(allErrs, field.Invalid(path.Child("maxMigratingPerNode"), *args.MaxMigratingPerNode, "maxMigratingPerNode should be greater or equal 0"))
	}

	if args.MaxMigratingPerWorkload != nil {
		if _, err := intstr.GetScaledValueFromIntOrPercent(args.MaxMigratingPerWorkload, 100, true); err != nil {
			allErrs = append(allErrs, field.Invalid(path.Child("maxMigratingPerWorkload"), *args.MaxMigratingPerWorkload, fmt.Sprintf("maxMigratingPerWorkload is invalid, err: %v ", err)))
		}
	}

	if args.MaxUnavailablePerWorkload != nil {
		if _, err := intstr.GetScaledValueFromIntOrPercent(args.MaxUnavailablePerWorkload, 100, true); err != nil {
			allErrs = append(allErrs, field.Invalid(path.Child("maxUnavailablePerWorkload"), *args.MaxUnavailablePerWorkload, fmt.Sprintf("maxUnavailablePerWorkload is invalid, err: %v ", err)))
		}
	}

	if args.EvictQPS != "" {
		evictQPS, err := strconv.ParseFloat(args.EvictQPS, 64)
		if err != nil {
			allErrs = append(allErrs, field.Invalid(path.Child("evictQPS"), args.EvictQPS, "evictQPS should be float number"))
		} else if evictQPS > 0 && args.EvictBurst <= 0 {
			allErrs = append(allErrs, field.Invalid(path.Child("evictBurst"), args.EvictBurst, "evictBurst is required to be greater than 0 when set evictQPS"))
		}
	}

	if args.LabelSelector != nil {
		allErrs = append(allErrs, metav1validation.ValidateLabelSelector(args.LabelSelector, field.NewPath("labelSelector"))...)
	}

	// At most one of include/exclude can be set
	if args.Namespaces != nil && len(args.Namespaces.Include) > 0 && len(args.Namespaces.Exclude) > 0 {
		allErrs = append(allErrs, field.Invalid(path.Child("namespaces"), args.Namespaces, "only one of Include/Exclude namespaces can be set"))
	}

	if args.MaxConcurrentReconciles < 1 {
		allErrs = append(allErrs, field.Invalid(path.Child("maxConcurrentReconciles"), args.MaxConcurrentReconciles, "maxConcurrentReconciles should be greater than or equal to 1"))
	}

	if args.DefaultJobMode != string(sev1alpha1.PodMigrationJobModeReservationFirst) && args.DefaultJobMode != string(sev1alpha1.PodMigrationJobModeEvictionDirectly) {
		allErrs = append(allErrs, field.Invalid(path.Child("defaultJobMode"), args.DefaultJobMode, fmt.Sprintf("defaultJobMode must be %s or %s", sev1alpha1.PodMigrationJobModeReservationFirst, sev1alpha1.PodMigrationJobModeEvictionDirectly)))
	}

	if args.DefaultJobTTL.Duration < 0 {
		allErrs = append(allErrs, field.Invalid(path.Child("defaultJobTTL"), args.DefaultJobTTL, "defaultJobTTL should be positive or zero"))
	}

	if len(allErrs) == 0 {
		return nil
	}
	return allErrs.ToAggregate()
}
