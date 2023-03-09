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
	"net"
	"strconv"
	"time"

	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/util/sets"
	componentbaseconfigv1alpha1 "k8s.io/component-base/config/v1alpha1"
	"k8s.io/utils/pointer"

	sev1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config"
	migrationevictor "github.com/koordinator-sh/koordinator/pkg/descheduler/controllers/migration/evictor"
)

const (
	defaultMigrationControllerMaxConcurrentReconciles = 1

	defaultMigrationJobMode           = sev1alpha1.PodMigrationJobModeReservationFirst
	defaultMigrationJobTTL            = 5 * time.Minute
	defaultMigrationJobEvictionPolicy = migrationevictor.NativeEvictorName
)

var (
	defaultObjectLimiters = map[MigrationLimitObjectType]MigrationObjectLimiter{
		MigrationLimitObjectWorkload: {
			Duration: metav1.Duration{Duration: 5 * time.Minute},
		},
	}
)

func addDefaultingFuncs(scheme *runtime.Scheme) error {
	return RegisterDefaults(scheme)
}

func pluginsNames(p *Plugins) []string {
	if p == nil {
		return nil
	}
	extensions := []PluginSet{
		p.Deschedule,
		p.Balance,
		p.Evictor,
	}
	n := sets.NewString()
	for _, e := range extensions {
		for _, pg := range e.Enabled {
			n.Insert(pg.Name)
		}
	}
	return n.List()
}

func setDefaults_Profile(prof *DeschedulerProfile) {
	// Set default plugins.
	prof.Plugins = mergePlugins(getDefaultPlugins(), prof.Plugins)

	// Set default plugin configs.
	scheme := GetPluginArgConversionScheme()
	existingConfigs := sets.NewString()
	for j := range prof.PluginConfig {
		existingConfigs.Insert(prof.PluginConfig[j].Name)
		args := prof.PluginConfig[j].Args.Object
		if _, isUnknown := args.(*runtime.Unknown); isUnknown {
			continue
		}
		scheme.Default(args)
	}

	// Append default configs for plugins that didn't have one explicitly set.
	for _, name := range pluginsNames(prof.Plugins) {
		if existingConfigs.Has(name) {
			continue
		}
		gvk := SchemeGroupVersion.WithKind(name + "Args")
		args, err := scheme.New(gvk)
		if err != nil {
			// This plugin is out-of-tree or doesn't require configuration.
			continue
		}
		scheme.Default(args)
		args.GetObjectKind().SetGroupVersionKind(gvk)
		prof.PluginConfig = append(prof.PluginConfig, PluginConfig{
			Name: name,
			Args: runtime.RawExtension{Object: args},
		})
	}
}

// SetDefaults_DeschedulerConfiguration sets additional defaults
func SetDefaults_DeschedulerConfiguration(obj *DeschedulerConfiguration) {
	if len(obj.Profiles) == 0 {
		obj.Profiles = append(obj.Profiles, DeschedulerProfile{})
	}
	// Only apply a default scheduler name when there is a single profile.
	// Validation will ensure that every profile has a non-empty unique name.
	if len(obj.Profiles) == 1 && obj.Profiles[0].Name == "" {
		obj.Profiles[0].Name = "koord-descheduler"
	}

	// Add the default set of plugins and apply the configuration.
	for i := range obj.Profiles {
		prof := &obj.Profiles[i]
		setDefaults_Profile(prof)
	}

	if len(obj.LeaderElection.ResourceLock) == 0 {
		// Use lease-based leader election to reduce cost.
		// We migrated for EndpointsLease lock in 1.17 and starting in 1.20 we
		// migrated to Lease lock.
		obj.LeaderElection.ResourceLock = "leases"
	}
	if len(obj.LeaderElection.ResourceNamespace) == 0 {
		obj.LeaderElection.ResourceNamespace = "koordinator-system"
	}
	if len(obj.LeaderElection.ResourceName) == 0 {
		obj.LeaderElection.ResourceName = "koord-descheduler"
	}
	// Use the default LeaderElectionConfiguration options
	componentbaseconfigv1alpha1.RecommendedDefaultLeaderElectionConfiguration(&obj.LeaderElection)

	if len(obj.ClientConnection.ContentType) == 0 {
		obj.ClientConnection.ContentType = "application/vnd.kubernetes.protobuf"
	}
	// Scheduler has an opinion about QPS/Burst, setting specific defaults for itself, instead of generic settings.
	if obj.ClientConnection.QPS == 0.0 {
		obj.ClientConnection.QPS = 50.0
	}
	if obj.ClientConnection.Burst == 0 {
		obj.ClientConnection.Burst = 100
	}

	// Enable profiling by default in the scheduler
	if obj.EnableProfiling == nil {
		enableProfiling := true
		obj.EnableProfiling = &enableProfiling
	}

	// Enable contention profiling by default if profiling is enabled
	if *obj.EnableProfiling && obj.EnableContentionProfiling == nil {
		enableContentionProfiling := true
		obj.EnableContentionProfiling = &enableContentionProfiling
	}

	defaultBindAddress := net.JoinHostPort("0.0.0.0", strconv.Itoa(config.DefaultDeschedulerPort))
	if obj.HealthzBindAddress == nil {
		obj.HealthzBindAddress = &defaultBindAddress
	} else {
		if host, port, err := net.SplitHostPort(*obj.HealthzBindAddress); err == nil {
			if len(host) == 0 {
				host = "0.0.0.0"
			}
			hostPort := net.JoinHostPort(host, port)
			obj.HealthzBindAddress = &hostPort
		} else {
			if host := net.ParseIP(*obj.HealthzBindAddress); host != nil {
				hostPort := net.JoinHostPort(*obj.HealthzBindAddress, strconv.Itoa(config.DefaultDeschedulerPort))
				obj.HealthzBindAddress = &hostPort
			}
		}
	}

	if obj.MetricsBindAddress == nil {
		obj.MetricsBindAddress = &defaultBindAddress
	} else {
		if host, port, err := net.SplitHostPort(*obj.MetricsBindAddress); err == nil {
			if len(host) == 0 {
				host = "0.0.0.0"
			}
			hostPort := net.JoinHostPort(host, port)
			obj.MetricsBindAddress = &hostPort
		} else {
			if host := net.ParseIP(*obj.MetricsBindAddress); host != nil {
				hostPort := net.JoinHostPort(*obj.MetricsBindAddress, strconv.Itoa(config.DefaultDeschedulerPort))
				obj.MetricsBindAddress = &hostPort
			}
		}
	}
}

func SetDefaults_DefaultEvictorArgs(obj *DefaultEvictorArgs) {
	// TODO(joseph): the current version disables the eviction ability by default
	if obj.DryRun == nil {
		obj.DryRun = pointer.Bool(true)
	}
}

func SetDefaults_RemovePodsViolatingNodeAffinityArgs(obj *RemovePodsViolatingNodeAffinityArgs) {
	if len(obj.NodeAffinityType) == 0 {
		obj.NodeAffinityType = append(obj.NodeAffinityType, "requiredDuringSchedulingIgnoredDuringExecution")
	}
}

func SetDefaults_MigrationControllerArgs(obj *MigrationControllerArgs) {
	if obj.MaxConcurrentReconciles == nil {
		obj.MaxConcurrentReconciles = pointer.Int32(defaultMigrationControllerMaxConcurrentReconciles)
	}
	if obj.DefaultJobMode == "" {
		obj.DefaultJobMode = string(defaultMigrationJobMode)
	}
	if obj.DefaultJobTTL == nil {
		obj.DefaultJobTTL = &metav1.Duration{Duration: defaultMigrationJobTTL}
	}
	if obj.EvictionPolicy == "" {
		obj.EvictionPolicy = defaultMigrationJobEvictionPolicy
	}

	if len(obj.ObjectLimiters) == 0 {
		obj.ObjectLimiters = defaultObjectLimiters
	}
}

func SetDefaults_LowNodeLoadArgs(obj *LowNodeLoadArgs) {
	if obj.NodeFit == nil {
		obj.NodeFit = pointer.Bool(true)
	}
}
