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

package k8s

import (
	apiv1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/util/intstr"

	"github.com/erda-project/erda/apistructs"
)

func (k *Kubernetes) NewHealthcheckProbe(service *apistructs.Service) *apiv1.Probe {
	return FillHealthCheckProbe(service)
}

func SetHealthCheck(container *apiv1.Container, service *apistructs.Service) {

	probe := FillHealthCheckProbe(service)

	container.LivenessProbe = probe
	readinessprobe := probe.DeepCopy()
	if readinessprobe != nil {
		readinessprobe.FailureThreshold = 3
		readinessprobe.PeriodSeconds = 10
		readinessprobe.InitialDelaySeconds = 10
	}
	container.ReadinessProbe = readinessprobe

}

// FillHealthCheckProbe Fill out k8s probe based on service
func FillHealthCheckProbe(service *apistructs.Service) *apiv1.Probe {
	var (
		probe *apiv1.Probe
		newHC = service.NewHealthCheck
		oldHC = service.HealthCheck
	)

	if newHC != nil && (newHC.ExecHealthCheck != nil || newHC.HttpHealthCheck != nil) {
		probe = NewHealthCheck(newHC)
	} else if oldHC != nil {
		probe = OldHealthCheck(oldHC)
	} else {
		// Default health check
		probe = DefaultHealthCheck(service)
	}

	return probe
}

// NewCheckProbe Create k8s probe default object
func NewCheckProbe() *apiv1.Probe {
	return &apiv1.Probe{
		InitialDelaySeconds: 0,
		// Timeout of each health check
		TimeoutSeconds: 10,
		// Health check detection interval
		PeriodSeconds:    15,
		FailureThreshold: int32(apistructs.HealthCheckDuration) / 15,
	}
}

// DefaultHealthCheck The user has not configured any health check, and the first port is checked by layer 4 tcp by default
func DefaultHealthCheck(service *apistructs.Service) *apiv1.Probe {
	if len(service.Ports) == 0 {
		return nil
	}

	probe := NewCheckProbe()
	probe.TCPSocket = &apiv1.TCPSocketAction{
		Port: intstr.FromInt(service.Ports[0].Port),
	}
	return probe
}

// NewHealthCheck Configure the new version of Dice health check
func NewHealthCheck(hc *apistructs.NewHealthCheck) *apiv1.Probe {
	if hc == nil || (hc.HttpHealthCheck == nil && hc.ExecHealthCheck == nil) {
		return nil
	}

	probe := NewCheckProbe()
	if hc.HttpHealthCheck != nil {
		httpCheck := hc.HttpHealthCheck
		probe.HTTPGet = &apiv1.HTTPGetAction{
			Path:   httpCheck.Path,
			Port:   intstr.FromInt(httpCheck.Port),
			Scheme: apiv1.URIScheme("HTTP"),
		}

		if times := int32(httpCheck.Duration) / 15; times > probe.FailureThreshold {
			probe.FailureThreshold = times
		}
	} else if hc.ExecHealthCheck != nil {
		execCheck := hc.ExecHealthCheck
		probe.Exec = &apiv1.ExecAction{
			Command: []string{"sh", "-c", execCheck.Cmd},
		}
		if times := int32(execCheck.Duration) / 15; times > probe.FailureThreshold {
			probe.FailureThreshold = times
		}
	}
	return probe
}

// OldHealthCheck Compatible with Dice old version health detection
func OldHealthCheck(hc *apistructs.HealthCheck) *apiv1.Probe {
	if hc == nil {
		return nil
	}

	probe := NewCheckProbe()
	switch hc.Kind {
	case "COMMAND":
		probe.Exec = &apiv1.ExecAction{
			Command: []string{"sh", "-c", hc.Command},
		}
	case "TCP":
		probe.TCPSocket = &apiv1.TCPSocketAction{
			Port: intstr.FromInt(hc.Port),
		}
	case "HTTP", "https":
		probe.HTTPGet = &apiv1.HTTPGetAction{
			Path:   hc.Path,
			Port:   intstr.FromInt(hc.Port),
			Scheme: apiv1.URIScheme(hc.Kind),
		}
	}
	// The default duration of the old health check is 5 minutes (the container will be killed if all health checks fail within 5 minutes), the same as the dcos configuration
	probe.FailureThreshold = 300 / 15
	return probe
}
