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

package spec

import "github.com/erda-project/erda/pkg/parser/diceyml"

type LegacyDice struct {
	Name         string                 `json:"name"`
	Endpoints    map[string]*Service    `json:"endpoints,omitempty"`
	Services     map[string]*Service    `json:"services,omitempty"`
	Addons       map[string]*Addon      `json:"addons,omitempty"`
	AddonActions map[string]interface{} `json:"addonActions,omitempty"`
	Branch       string                 `json:"branch"`
	GlobalEnv    map[string]string      `json:"globalEnv,omitempty"`
}

type Service struct {
	Scale       *int                        `json:"scale,omitempty"`
	Ports       []int                       `json:"ports,omitempty"`
	Depends     []string                    `json:"depends,omitempty"`
	Environment map[string]string           `json:"environment,omitempty"`
	Resources   *Resources                  `json:"resources,omitempty"`
	Cmd         string                      `json:"cmd"`
	Hosts       []string                    `json:"hosts"`
	Volumes     []string                    `json:"volumes"`
	Image       string                      `json:"image"`
	HealthCheck HealthCheck                 `json:"health_check"`
	SideCars    map[string]*diceyml.SideCar `yaml:"sidecars,omitempty" json:"sidecars,omitempty"`
}

type HealthCheck struct {
	HTTP *HTTPCheck `json:"http,omitempty"`
	Exec *ExecCheck `json:"exec,omitempty"`
}

type HTTPCheck struct {
	Port     int    `json:"port,string"`
	Path     string `json:"path"`
	Duration int    `json:"duration,string"`
}

type ExecCheck struct {
	Cmd      string `json:"cmd"`
	Duration int    `json:"duration,string"`
}

type Resources struct {
	CPU  float64 `json:"cpu"`
	Mem  float64 `json:"mem"`
	Disk float64 `json:"disk"`

	MaxMem                   float64 `json:"max_mem"`
	MaxCPU                   float64 `json:"max_cpu"`
	EmptyDirCapacity         float64 `json:"emptydir_size"`
	EphemeralStorageCapacity float64 `json:"ephemeral_storage_size"`
}

type Addon struct {
	Id      string                 `json:"id"`
	Name    string                 `json:"name"`
	Type    string                 `json:"type"`
	Plan    string                 `json:"plan"`
	As      string                 `json:"as"`
	Options map[string]string      `json:"options"`
	Actions map[string]interface{} `json:"actions"`
}
