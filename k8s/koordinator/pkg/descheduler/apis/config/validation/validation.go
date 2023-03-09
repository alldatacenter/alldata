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
	"reflect"

	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	utilerrors "k8s.io/apimachinery/pkg/util/errors"
	"k8s.io/apimachinery/pkg/util/sets"
	"k8s.io/apimachinery/pkg/util/validation"
	"k8s.io/apimachinery/pkg/util/validation/field"
	componentbasevalidation "k8s.io/component-base/config/validation"

	"github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/controllers/names"
)

func ValidateDeschedulerConfiguration(cc *config.DeschedulerConfiguration) utilerrors.Aggregate {
	var errs []error
	errs = append(errs, componentbasevalidation.ValidateClientConnectionConfiguration(&cc.ClientConnection, field.NewPath("clientConnection")).ToAggregate())
	errs = append(errs, componentbasevalidation.ValidateLeaderElectionConfiguration(&cc.LeaderElection, field.NewPath("leaderElection")).ToAggregate())
	profilesPath := field.NewPath("profiles")
	if len(cc.Profiles) == 0 {
		errs = append(errs, field.Required(profilesPath, ""))
	} else {
		existingProfiles := make(map[string]int, len(cc.Profiles))
		for i := range cc.Profiles {
			profile := &cc.Profiles[i]
			path := profilesPath.Index(i)
			errs = append(errs, validateDeschedulerProfile(path, profile)...)
			if idx, ok := existingProfiles[profile.Name]; ok {
				errs = append(errs, field.Duplicate(path.Child("name"), profilesPath.Index(idx).Child("name")))
			}
			existingProfiles[profile.Name] = i
		}
	}
	for _, msg := range validation.IsValidSocketAddr(cc.HealthzBindAddress) {
		errs = append(errs, field.Invalid(field.NewPath("healthzBindAddress"), cc.HealthzBindAddress, msg))
	}
	for _, msg := range validation.IsValidSocketAddr(cc.MetricsBindAddress) {
		errs = append(errs, field.Invalid(field.NewPath("metricsBindAddress"), cc.MetricsBindAddress, msg))
	}

	if cc.NodeSelector != nil {
		_, err := metav1.LabelSelectorAsSelector(cc.NodeSelector)
		if err != nil {
			errs = append(errs, err)
		}
	}

	return utilerrors.Flatten(utilerrors.NewAggregate(errs))
}

func validateDeschedulerProfile(path *field.Path, profile *config.DeschedulerProfile) []error {
	var errs []error
	if len(profile.Name) == 0 {
		errs = append(errs, field.Required(path.Child("name"), ""))
	}
	errs = append(errs, validatePluginConfig(path, profile)...)
	return errs
}

func validatePluginConfig(path *field.Path, profile *config.DeschedulerProfile) []error {
	var errs []error
	m := map[string]interface{}{
		// NOTE: you can add the in-tree plugins configuration validation function
		names.MigrationController:         ValidateMigrationControllerArgs,
		"RemovePodsViolatingNodeAffinity": ValidateRemovePodsViolatingNodeAffinityArgs,
	}

	seenPluginConfig := make(sets.String)

	for i := range profile.PluginConfig {
		pluginConfigPath := path.Child("pluginConfig").Index(i)
		name := profile.PluginConfig[i].Name
		args := profile.PluginConfig[i].Args
		if seenPluginConfig.Has(name) {
			errs = append(errs, field.Duplicate(pluginConfigPath, name))
		} else {
			seenPluginConfig.Insert(name)
		}
		if validateFunc, ok := m[name]; ok {
			// type mismatch, no need to validate the `args`.
			if reflect.TypeOf(args) != reflect.ValueOf(validateFunc).Type().In(1) {
				errs = append(errs, field.Invalid(pluginConfigPath.Child("args"), args, "has to match plugin args"))
			} else {
				in := []reflect.Value{reflect.ValueOf(pluginConfigPath.Child("args")), reflect.ValueOf(args)}
				res := reflect.ValueOf(validateFunc).Call(in)
				// It's possible that validation function return a Aggregate, just append here and it will be flattened at the end of CC validation.
				if res[0].Interface() != nil {
					errs = append(errs, res[0].Interface().(error))
				}
			}
		}
	}
	return errs
}
