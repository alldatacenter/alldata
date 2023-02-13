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

package diceyml

import (
	"path"
	"path/filepath"
	"regexp"
	"strings"

	"github.com/pkg/errors"
)

type BasicValidateVisitor struct {
	DefaultVisitor
	currentAddOn  string
	collectErrors ValidateError
}

func NewBasicValidateVisitor() DiceYmlVisitor {
	return &BasicValidateVisitor{
		collectErrors: ValidateError{},
	}
}

func (o *BasicValidateVisitor) VisitObject(v DiceYmlVisitor, obj *Object) {
	if len(obj.Services)+len(obj.Jobs) == 0 {
		o.collectErrors[yamlHeaderRegex("_0")] = errors.Wrap(emptyServiceJobList, "got empty 'services' and 'jobs' fields")
	}
}

func (o *BasicValidateVisitor) isEndpointValid(endpoint Endpoint) bool {
	if endpoint.Domain == "" {
		o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentService}, "endpoints")] = errors.Wrap(emptyEndpointDomain, o.currentService)
		return false
	}

	// replace all ${}
	rePlaceholder := regexp.MustCompile("\\$\\{(.+?)\\}")
	if ok := rePlaceholder.MatchString(endpoint.Domain); ok {
		endpoint.Domain = string(rePlaceholder.ReplaceAllString(endpoint.Domain, ""))
		// e.g. ${platform.DICE_PROJECT_NAME}.*.
		if strings.TrimSuffix(endpoint.Domain, ".*") == "" {
			return true
		}
	}

	if strings.HasSuffix(endpoint.Domain, ".*") {
		if ok, _ := regexp.MatchString(`^[0-9a-zA-z-_]+$`, strings.TrimSuffix(endpoint.Domain, ".*")); !ok {
			o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentService, "endpoints"}, endpoint.Domain)] = errors.Wrap(invalidEndpointDomain, o.currentService)
			return false
		}
		return true
	}
	if ok, _ := regexp.MatchString(`^[0-9a-zA-z-_\.:]+$`, endpoint.Domain); !ok {
		o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentService, "endpoints"}, endpoint.Domain)] = errors.Wrap(invalidEndpointDomain, o.currentService)
		return false
	}
	if endpoint.Path != "" && !strings.HasPrefix(endpoint.Path, "/") {
		o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentService, "endpoints", endpoint.Domain}, endpoint.Path)] = errors.Wrap(invalidEndpointPath, o.currentService)
		return false
	}
	return true
}

func (o *BasicValidateVisitor) VisitService(v DiceYmlVisitor, obj *Service) {
	if o.currentService == "" {
		panic("should not be empty")
	}
	// 可能在parse之后，由insertimage插入这个字段
	// if obj.Image == "" {
	// 	o.collectErrors = append(o.collectErrors, errors.Wrap(invalidImage, o.currentService))
	// }

	res := obj.Resources

	if res.MaxCPU <= 0 && res.CPU <= 0 {
		o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentService}, "resources")] = errors.Wrap(invalidCPU, o.currentService)
	}

	if res.MaxMem <= 0 && res.Mem <= 0 {
		o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentService}, "resources")] = errors.Wrap(invalidMem, o.currentService)
	}

	// check whether max_cpu set but smaller than cpu
	if res.MaxCPU > 0 && res.MaxCPU < res.CPU {
		o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentService}, "resources")] = errors.Wrap(invalidMaxCPU, o.currentService)
	}
	// check whether max_mem set but smaller than mem
	if res.MaxMem > 0 && res.MaxMem < res.Mem {
		o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentService}, "resources")] = errors.Wrap(invalidMaxMem, o.currentService)
	}

	for _, port := range obj.Ports {
		if port.Port <= 0 {
			o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentService}, "ports")] = errors.Wrap(invalidPort, o.currentService)
			break
		}
	}
	for _, port := range obj.Expose {
		if port <= 0 {
			o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentService}, "expose")] = errors.Wrap(invalidExpose, o.currentService)
			break
		}
	}
	for _, vol := range obj.Volumes {
		if !path.IsAbs(vol.Path) && !path.IsAbs(vol.TargetPath) {
			o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentService}, "volumes")] = errors.Wrap(invalidVolume, o.currentService)
			break
		}
	}

	switch obj.TrafficSecurity.Mode {
	case "", "https":
	default:
		o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentService}, "traffic_security")] = errors.Wrap(invalidTrafficSecurityMode, o.currentService)
	}
	for _, endpoint := range obj.Endpoints {
		if !o.isEndpointValid(endpoint) {
			break
		}
	}
}
func (o *BasicValidateVisitor) VisitBinds(v DiceYmlVisitor, obj *Binds) {
	for _, bind := range *obj {
		parts := strings.SplitN(bind, ":", 3)
		if len(parts) == 3 {
			if !filepath.IsAbs(parts[0]) {
				o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentService}, "binds")] = errors.Wrap(invalidBindHostPath, o.currentService+":["+parts[0]+"]")
			}
			if !filepath.IsAbs(parts[1]) {
				o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentService}, "binds")] = errors.Wrap(invalidBindContainerPath, o.currentService+":["+parts[1]+"]")
			}
			if parts[2] != "rw" && parts[2] != "ro" {
				o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentService}, "binds")] = errors.Wrap(invalidBindType, o.currentService+":["+parts[2]+"]")
			}
		}
	}
}

func (o *BasicValidateVisitor) VisitResources(v DiceYmlVisitor, obj *Resources) {
	if o.currentService == "" && o.currentJob == "" {
		panic("should not be empty")
	}
	if obj.CPU < 0 {
		o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentService, "resources"}, "cpu")] = errors.Wrap(invalidCPU, o.currentService)
	}
	if obj.CPU == 0 {
		o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentService}, "resources")] = errors.Wrap(invalidCPU, o.currentService)
	}
	if obj.Mem < 0 {
		o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentService, "resources"}, "mem")] = errors.Wrap(invalidMem, o.currentService)
	}
	if obj.Mem == 0 {
		o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentService}, "resources")] = errors.Wrap(invalidMem, o.currentService)
	}

	if obj.Disk < 0 {
		o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentService, "resources"}, "disk")] = errors.Wrap(invalidDisk, o.currentService)
	}
	if mode, ok := obj.Network["mode"]; ok {
		if mode != "container" && mode != "host" {
			o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentService, "resources", "network"}, "mode")] = errors.Wrap(invalidNetworkMode, o.currentService)
		}
	}

	if obj.EmptyDirCapacity < 0 {
		o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentService, "resources"}, "emptydir_size")] = errors.Wrap(invalidEmptyDir, o.currentService)
	}

	if obj.EphemeralStorageCapacity < 0 {
		o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentService, "resources"}, "ephemeral_storage_size")] = errors.Wrap(invalidEphemeralStorage, o.currentService)
	}
}

func (o *BasicValidateVisitor) VisitDeployments(v DiceYmlVisitor, obj *Deployments) {
	if o.currentService == "" && o.currentJob == "" {
		panic("should not be empty")
	}
	if obj.Replicas < 0 {
		o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentService, "deployments"}, "replicas")] = errors.Wrap(invalidReplicas, o.currentService)
	}

	if obj.Policies != "" && obj.Policies != "shuffle" && obj.Policies != "affinity" && obj.Policies != "unique" {
		o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{o.currentService, "deployments"}, "policies")] = errors.Wrap(invalidPolicy, o.currentService)
	}
}

func (o *BasicValidateVisitor) VisitAddOns(v DiceYmlVisitor, obj *AddOns) {
	for name, v_ := range *obj {
		o.currentAddOn = name
		v_.Accept(v)
	}
	o.currentAddOn = ""
}

func (o *BasicValidateVisitor) VisitAddOn(v DiceYmlVisitor, obj *AddOn) {
	if o.currentAddOn == "" {
		panic("should not be empty")
	}
	if obj.Plan == "" {
		o.collectErrors[yamlHeaderRegexWithUpperHeader([]string{"addons"}, o.currentAddOn)] = errors.Wrap(invalidAddonPlan, o.currentAddOn)
	}
}

func BasicValidate(obj *Object) ValidateError {
	visitor := NewBasicValidateVisitor()
	obj.Accept(visitor)
	return visitor.(*BasicValidateVisitor).collectErrors
}
