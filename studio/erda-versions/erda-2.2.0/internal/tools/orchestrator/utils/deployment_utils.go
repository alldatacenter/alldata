// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package utils

import (
	"strconv"
	"strings"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/spec"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/strutil"
)

func ConvertToLegacyDice(dice *diceyml.DiceYaml, addonActions map[string]interface{}) *spec.LegacyDice {
	addons := make(map[string]*spec.Addon)
	for name, addon := range dice.Obj().AddOns {
		addons[name] = &spec.Addon{
			Plan:    addon.Plan,
			As:      addon.As,
			Options: addon.Options,
			Actions: addon.Actions,
		}
	}
	services := make(map[string]*spec.Service)
	endpoints := make(map[string]*spec.Service)
	for name, service := range dice.Obj().Services {
		legacyService := spec.Service{
			Cmd:         service.Cmd,
			Depends:     service.DependsOn,
			Environment: service.Envs,
			Image:       service.Image,
			Resources: &spec.Resources{
				CPU:  service.Resources.CPU,
				Mem:  float64(service.Resources.Mem),
				Disk: float64(service.Resources.Disk),
			},
			Ports:       diceyml.ComposeIntPortsFromServicePorts(service.Ports),
			Scale:       &service.Deployments.Replicas,
			Volumes:     convertVolumeToLegacyDice(service.Volumes),
			Hosts:       service.Hosts,
			HealthCheck: convertHealthCheckToLegacyDice(service.HealthCheck),
			SideCars:    service.SideCars,
		}

		if len(service.Expose) > 0 {
			// endpoints
			endpoints[name] = &legacyService
		} else {
			services[name] = &legacyService
		}
	}
	return &spec.LegacyDice{
		Addons:       addons,
		AddonActions: addonActions,
		Endpoints:    endpoints,
		Services:     services,
		GlobalEnv:    dice.Obj().Envs,
	}
}

func convertVolumeToLegacyDice(volumes diceyml.Volumes) []string {
	vs := make([]string, len(volumes))
	for i, volume := range volumes {
		vs[i] = volume.TargetPath
	}
	return vs
}

func convertHealthCheckToLegacyDice(hc diceyml.HealthCheck) spec.HealthCheck {
	var ret spec.HealthCheck
	if hc.HTTP != nil {
		ret.HTTP = &spec.HTTPCheck{
			Port:     hc.HTTP.Port,
			Path:     hc.HTTP.Path,
			Duration: hc.HTTP.Duration,
		}
	}
	if hc.Exec != nil {
		ret.Exec = &spec.ExecCheck{
			Cmd:      hc.Exec.Cmd,
			Duration: hc.Exec.Duration,
		}
	}
	return ret
}

func ApplyOverlay(d *diceyml.Object, overlay *diceyml.Object) {
	if overlay.Envs != nil {
		d.Envs = overlay.Envs
	}
	for name, service := range overlay.Services {
		applyOverlay_(d, name, service)
	}
}

func applyOverlay_(d *diceyml.Object, name string, serviceOverlay *diceyml.Service) {
	if d == nil || serviceOverlay == nil || name == "" {
		return
	}
	applyOverlay__(&(d.Services), name, serviceOverlay)
	//applyOverlayJobs__(&(d.Jobs), name, serviceOverlay)
}

// applyOverlay__ 覆盖service信息
func applyOverlay__(services *diceyml.Services, name string, serviceOverlay *diceyml.Service) {
	if services == nil {
		return
	}
	if service, ok := (*services)[name]; ok {
		if serviceOverlay.Envs != nil {
			service.Envs = serviceOverlay.Envs
		}
		service.Deployments.Replicas = serviceOverlay.Deployments.Replicas

		r := service.Resources
		r.CPU = serviceOverlay.Resources.CPU
		r.Mem = int(serviceOverlay.Resources.Mem)
		r.Disk = int(serviceOverlay.Resources.Disk)
		r.EmptyDirCapacity = int(serviceOverlay.Resources.EmptyDirCapacity)
		r.EphemeralStorageCapacity = int(serviceOverlay.Resources.EmptyDirCapacity)
		service.Resources = r
	}
}

// applyOverlayJobs__ 覆盖jobs信息
func applyOverlayJobs__(jobs *diceyml.Jobs, name string, serviceOverlay *spec.Service) {
	if jobs == nil {
		return
	}
	if service, ok := (*jobs)[name]; ok {
		if serviceOverlay.Environment != nil {
			service.Envs = serviceOverlay.Environment
		}
		if serviceOverlay.Resources != nil {
			r := service.Resources
			r.CPU = serviceOverlay.Resources.CPU
			r.MaxCPU = serviceOverlay.Resources.MaxCPU
			r.Mem = int(serviceOverlay.Resources.Mem)
			r.MaxMem = int(serviceOverlay.Resources.MaxMem)
			r.Disk = int(serviceOverlay.Resources.Disk)
			r.EmptyDirCapacity = int(serviceOverlay.Resources.EmptyDirCapacity)
			r.EphemeralStorageCapacity = int(serviceOverlay.Resources.EmptyDirCapacity)
			service.Resources = r
		}
	}
}

func AppendEnv(target map[string]string, overlay map[string]string) {
	if target == nil || overlay == nil {
		return
	}
	for k, v := range overlay {
		target[k] = v
	}
}

func ConvertServiceLabels(groupLabels, serviceLabels map[string]string, serviceName string) map[string]string {
	// TODO: we shall not combine groupLabels
	labels := make(map[string]string)
	for k, v := range groupLabels {
		labels[k] = v
	}
	for k, v := range serviceLabels {
		labels[k] = v
	}
	// we do prefer DICE_X_NAME more than DICE_X, but still keep DICE_X for compatibility
	labels["DICE_SERVICE"] = serviceName
	labels["DICE_SERVICE_NAME"] = serviceName
	return labels
}

func ConvertHealthCheck(hc spec.HealthCheck) *apistructs.NewHealthCheck {
	if hc.HTTP != nil && len(hc.HTTP.Path) > 0 && hc.HTTP.Port != 0 {
		return &apistructs.NewHealthCheck{
			HttpHealthCheck: &apistructs.HttpHealthCheck{
				Duration: hc.HTTP.Duration,
				Port:     hc.HTTP.Port,
				Path:     hc.HTTP.Path,
			},
		}
	} else if hc.Exec != nil && len(hc.Exec.Cmd) > 0 {
		return &apistructs.NewHealthCheck{
			ExecHealthCheck: &apistructs.ExecHealthCheck{
				Duration: hc.Exec.Duration,
				Cmd:      hc.Exec.Cmd,
			},
		}
	} else {
		// TODO: should we panic if no healthCheck ?
		// TODO: or just warning
		return nil
	}
}

func ConvertBinds(volumePrefixDir string, vol []string) (binds []apistructs.ServiceBind) {
	if vol == nil {
		return
	}

	for _, v := range vol {
		vList := strings.Split(v, ":")
		var mode = "RW"
		var hostPath string
		var containerPath string
		switch len(vList) {
		case 3:
			hostPath = vList[0]
			containerPath = vList[1]
			mode = vList[2]
		case 2:
			if vList[1] == "RW" || vList[1] == "RO" {
				hostPath = vList[0]
				containerPath = vList[0]
				mode = vList[1]
			} else {
				hostPath = vList[0]
				containerPath = vList[1]
			}
		case 1:
			hostPath = vList[0]
			containerPath = vList[0]
		default:
			// TODO: should we panic ?
			continue
		}

		if !strings.HasPrefix(hostPath, "/") {
			hostPath = "/" + hostPath
		}
		binds = append(binds, apistructs.ServiceBind{
			Bind: apistructs.Bind{
				ContainerPath: containerPath,
				HostPath:      volumePrefixDir + hostPath,
				ReadOnly:      mode == "RO",
			},
		})
	}

	return
}

func BuildDiscoveryConfig(appName string, group *apistructs.ServiceGroup) map[string]string {
	config := make(map[string]string, 0)
	for _, service := range group.Services {
		if len(service.Ports) > 0 {
			keyPrefix := buildEnvKeyPrefix(appName, service.Name)
			config[keyPrefix+"_HOST"] = service.Vip
			for i, port := range service.Ports {
				portStr := strconv.Itoa(port.Port)
				if i == 0 {
					config[keyPrefix+"_PORT"] = portStr
				}
				config[keyPrefix+"_PORT"+strconv.Itoa(i)] = portStr
			}
		}
	}
	return config
}

func buildEnvKeyPrefix(appName, serviceName string) string {
	return strutil.Concat("DICE_DISCOVERY_", convertEnvFormat(appName), "_", convertEnvFormat(serviceName))
}

func convertEnvFormat(s string) string {
	return strings.ToUpper(strings.Replace(s, "-", "_", -1))
}
