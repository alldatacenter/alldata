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
	"encoding/json"
	"fmt"
	"regexp"
	"strconv"
	"strings"

	apiv1 "k8s.io/api/core/v1"
	"sigs.k8s.io/yaml"

	"github.com/erda-project/erda/pkg/strutil"
)

var (
	selectorNotExpr = regexp.MustCompile("^NOT[[:blank:]]+([a-zA-Z0-9-]*)$")
	selectorOrExpr  = regexp.MustCompile("^[a-zA-Z0-9-]*([[:blank:]]+OR[[:blank:]]+[a-zA-Z0-9-]*)*$")
)

type WorkspaceStr string

const (
	WS_DEV     WorkspaceStr = "development"
	WS_TEST                 = "test"
	WS_STAGING              = "staging"
	WS_PROD                 = "production"

	// Addon new options for create labels
	// disk type: ‘SSD’、'NAS'、'OSS'、''
	AddonDiskType = "addonDiskType"
	// volume capacity: at least 20GB
	AddonVolumeSize = "addonVolumeSize"
	//historical snapshot count
	AddonSnapMaxHistory = "addonSnapMaxHistory"
	// addon image private registry
	AddonImageRegistry = "k8s.aliyun.com/insecure-registry"
	// Erda addon images registry
	AddonPublicRegistry = "registry.erda.cloud"
	// Env for orchestrator
	ErdaImageRegistry = "DICE_IMAGE_REGISTRY"

	// 容量最大限制 32TB
	AddonVolumeSizeMax = int32(32768)
	// 容量最小限制 20GiB
	AddonVolumeSizeMin = int32(20)
	// 历史快照数量限制 100 个
	AddonVolumeSnapshotMax = int32(100)
	// 项目级部署 ECI 默认卷容量
	ProjectECIVolumeDefaultSize = int32(20)
)

type Object struct {
	Version      string            `yaml:"version" json:"version"`
	Meta         map[string]string `yaml:"meta" json:"meta"`
	Envs         EnvMap            `yaml:"envs" json:"envs,omitempty"`
	Services     Services          `yaml:"services" json:"services,omitempty"`
	Jobs         Jobs              `yaml:"jobs,omitempty" json:"jobs,omitempty"`
	AddOns       AddOns            `yaml:"addons" json:"addons,omitempty"`
	Environments EnvObjects        `yaml:"environments,omitempty" json:"environments,omitempty"`
	Values       ValueObjects      `yaml:"values" json:"values,omitempty"`
}
type EnvMap map[string]string
type EnvObjects map[string]*EnvObject
type AddOns map[string]*AddOn
type Services map[string]*Service
type Jobs map[string]*Job
type Binds []string
type ValueMap map[string]string
type ValueObjects map[WorkspaceStr]*ValueMap

// Selector value struct of Selectors
// Selectors: map[key]value
// key: [a-zA-Z0-9-]*
// value: NOT_VALUE
//      | NORMAL_VALUE { "|" NORMAL_VALUE }
// NOT_VALUE: "!" NORMAL_VALUE
// NORMAL_VALUE: [a-zA-Z0-9-]*
type Selector struct {
	Not bool `json:"not"`

	// 由上面的定义可见，Not=true时，len(Values) = 1
	Values []string `json:"values"`
}
type Selectors map[string]Selector

type EnvObject struct {
	Envs     EnvMap   `yaml:"envs,omitempty" json:"envs,omitempty"`
	Services Services `yaml:"services,omitempty" json:"services,omitempty"`
	AddOns   AddOns   `yaml:"addons,omitempty" json:"addons,omitempty"`
}

type AddOn struct {
	Plan    string                 `yaml:"plan,omitempty" json:"plan"`
	As      string                 `yaml:"as,omitempty" json:"as,omitempty"`
	Options map[string]string      `yaml:"options,omitempty" json:"options,omitempty"`
	Actions map[string]interface{} `yaml:"actions,omitempty" json:"actions,omitempty"`
	Image   string                 `yaml:"image,omitempty" json:"image,omitempty"`
}

type Volume struct {
	// TODO: DEPRECATED IN FUTURE
	ID *string `yaml:"id,omitempty" json:"id,omitempty"`
	// TODO: DEPRECATED IN FUTURE
	// nfs, local
	Storage string `yaml:"storage,omitempty" json:"storage,omitempty"`
	// TODO: DEPRECATED IN FUTURE
	Path string `yaml:"path,omitempty" json:"path,omitempty"`

	// Type is the type of volume, it will be supported DICE-NAS, DICE-LOCAL, SSD, NAS, OSS...
	// only support DICE-NAS, DICE-LOCAL, SSD currently
	Type string `yaml:"type,omitempty" json:"type,omitempty"`
	// Capacity is the capacity of volume and the default unit is 'GB'
	Capacity int32 `yaml:"size,omitempty" json:"size,omitempty"`
	// SourcePath is the volume source path that is used in the local PV or host path
	// Default is empty
	//SourcePath string `yaml:"sourcePath,omitempty" json:"sourcePath,omitempty"`
	// TargetPath indicates will mount the file or directory in the volume to the
	// specified location of the container. Default is '/'
	TargetPath string `yaml:"targetPath,omitempty" json:"targetPath,omitempty"`
	// ReadOnly set the file in the volume allow to be read-only
	// Default is false
	ReadOnly bool `yaml:"readOnly,omitempty" json:"readOnly,omitempty"`
	// Snapshot indicates use can create snapshots of this volume
	// if Snapshot field isn't null and the default time interval is 3600 second
	// Note: Now, only for Alibaba disk ssd storageclass
	Snapshot *VolumeSnapshot `yaml:"snapshot,omitempty" json:"snapshot,omitempty"`
}

type VolumeSnapshot struct {
	// MaxHistory indicates the max count of the snapshot can be created
	// if the number of snapshots is beyond the max, the earliest one will be deleted
	MaxHistory int32 `yaml:"maxHistory,omitempty" json:"maxHistory,omitempty"`
}

type SnapshotAnnotations struct {
	Snapshot map[string]VolumeSnapshot
}
type Volumes []Volume

type Job struct {
	Image     string                   `yaml:"image,omitempty" json:"image"`
	Cmd       string                   `yaml:"cmd,omitempty" json:"cmd"`
	Envs      EnvMap                   `yaml:"envs,omitempty" json:"envs,omitempty"`
	Resources Resources                `yaml:"resources,omitempty" json:"resources"`
	Labels    map[string]string        `yaml:"labels,omitempty" json:"labels,omitempty"`
	Binds     Binds                    `yaml:"binds,omitempty" json:"binds,omitempty"`
	Volumes   Volumes                  `yaml:"volumes,omitempty" json:"volumes,omitempty"`
	Init      map[string]InitContainer `yaml:"init,omitempty" json:"init,omitempty"`
	Hosts     []string                 `yaml:"hosts,omitempty" json:"hosts,omitempty"`
}

type InitContainer struct {
	Image      string      `yaml:"image,omitempty" json:"image"`
	SharedDirs []SharedDir `yaml:"shared_dir,omitempty" json:"shared_dir,omitempty"`
	Cmd        string      `yaml:"cmd,omitempty" json:"cmd"`
	Resources  Resources   `yaml:"resources,omitempty" json:"resources"`
}

type Service struct {
	Image           string                   `yaml:"image,omitempty" json:"image"`
	ImageUsername   string                   `yaml:"image_username,omitempty" json:"image_username"`
	ImagePassword   string                   `yaml:"image_password,omitempty" json:"image_password"`
	Cmd             string                   `yaml:"cmd,omitempty" json:"cmd"`
	Ports           []ServicePort            `yaml:"ports,omitempty" json:"ports,omitempty"`
	Envs            EnvMap                   `yaml:"envs,omitempty" json:"envs,omitempty"`
	Hosts           []string                 `yaml:"hosts,omitempty" json:"hosts,omitempty"`
	Resources       Resources                `yaml:"resources,omitempty" json:"resources"`
	Labels          map[string]string        `yaml:"labels,omitempty" json:"labels,omitempty"`
	Annotations     map[string]string        `yaml:"annotations,omitempty" json:"annotations,omitempty"`
	Binds           Binds                    `yaml:"binds,omitempty" json:"binds,omitempty"`
	Volumes         Volumes                  `yaml:"volumes,omitempty" json:"volumes,omitempty"`
	Deployments     Deployments              `yaml:"deployments,omitempty" json:"deployments"`
	DependsOn       []string                 `yaml:"depends_on,omitempty" json:"depends_on,omitempty"`
	Expose          []int                    `yaml:"expose,omitempty" json:"expose,omitempty"`
	HealthCheck     HealthCheck              `yaml:"health_check,omitempty" json:"health_check"`
	SideCars        map[string]*SideCar      `yaml:"sidecars,omitempty" json:"sidecars,omitempty"`
	Init            map[string]InitContainer `yaml:"init,omitempty" json:"init,omitempty"`
	MeshEnable      *bool                    `yaml:"mesh_enable,omitempty" json:"mesh_enable,omitempty"`
	TrafficSecurity TrafficSecurity          `yaml:"traffic_security,omitempty" json:"traffic_security,omitempty"`
	Endpoints       []Endpoint               `yaml:"endpoints,omitempty" json:"endpoints,omitempty"`
	K8SSnippet      *K8SSnippet              `yaml:"k8s_snippet,omitempty" json:"k8s_snippet,omitempty"`
}

type ContainerSnippet apiv1.Container

type K8SSnippet struct {
	Container *ContainerSnippet `yaml:"container,omitempty" json:"container,omitempty"`
}

type ServicePort struct {
	Port       int            `yaml:"port" json:"port"`
	Protocol   string         `yaml:"protocol" json:"protocol"`
	L4Protocol apiv1.Protocol `yaml:"l4_protocol" json:"l4_protocol"`
	Expose     bool           `yaml:"expose" json:"expose"`
	Default    bool           `yaml:"default" json:"default"`
}

type MarshalableContainerSnippet struct {
	WorkingDir               string                         `json:"workingDir,omitempty" yaml:"workingDir,omitempty"`
	EnvFrom                  []apiv1.EnvFromSource          `json:"envFrom,omitempty" yaml:"envFrom,omitempty"`
	Env                      []apiv1.EnvVar                 `json:"env,omitempty" yaml:"env,omitempty"`
	LivenessProbe            *apiv1.Probe                   `json:"livenessProbe,omitempty" yaml:"livenessProbe,omitempty"`
	ReadinessProbe           *apiv1.Probe                   `json:"readinessProbe,omitempty" yaml:"readinessProbe,omitempty"`
	StartupProbe             *apiv1.Probe                   `json:"startupProbe,omitempty" yaml:"startupProbe,omitempty"`
	Lifecycle                *apiv1.Lifecycle               `json:"lifecycle,omitempty" yaml:"lifecycle,omitempty"`
	TerminationMessagePath   string                         `json:"terminationMessagePath,omitempty" yaml:"terminationMessagePath,omitempty"`
	TerminationMessagePolicy apiv1.TerminationMessagePolicy `json:"terminationMessagePolicy,omitempty" yaml:"terminationMessagePolicy,omitempty"`
	ImagePullPolicy          apiv1.PullPolicy               `json:"imagePullPolicy,omitempty" yaml:"imagePullPolicy,omitempty"`
	SecurityContext          *apiv1.SecurityContext         `json:"securityContext,omitempty" yaml:"securityContext,omitempty"`
	Stdin                    bool                           `json:"stdin,omitempty" yaml:"stdin,omitempty"`
	StdinOnce                bool                           `json:"stdinOnce,omitempty" yaml:"stdinOnce,omitempty"`
	TTY                      bool                           `json:"tty,omitempty" yaml:"tty,omitempty"`
}

func (cs *ContainerSnippet) ConvertToMarshalable() *MarshalableContainerSnippet {
	return &MarshalableContainerSnippet{
		WorkingDir:               cs.WorkingDir,
		EnvFrom:                  cs.EnvFrom,
		Env:                      cs.Env,
		LivenessProbe:            cs.LivenessProbe,
		ReadinessProbe:           cs.ReadinessProbe,
		StartupProbe:             cs.StartupProbe,
		Lifecycle:                cs.Lifecycle,
		TerminationMessagePath:   cs.TerminationMessagePath,
		TerminationMessagePolicy: cs.TerminationMessagePolicy,
		ImagePullPolicy:          cs.ImagePullPolicy,
		SecurityContext:          cs.SecurityContext,
		Stdin:                    cs.Stdin,
		StdinOnce:                cs.StdinOnce,
		TTY:                      cs.TTY,
	}
}

func (cs *ContainerSnippet) MarshalYAML() (interface{}, error) {
	return yaml.Marshal(cs.ConvertToMarshalable())
}

func (cs *ContainerSnippet) MarshalJSON() ([]byte, error) {
	return json.Marshal(cs.ConvertToMarshalable())
}

type MarshalableServiceProt struct {
	Port       int             `json:"port" yaml:"port"`
	Protocol   string          `json:"protocol,omitempty" yaml:"protocol,omitempty"`
	L4Protocol *apiv1.Protocol `json:"l4_protocol,omitempty" yaml:"l4_protocol,omitempty"`
	Expose     bool            `json:"expose,omitempty" yaml:"expose,omitempty"`
	Default    bool            `json:"default,omitempty" yaml:"default,omitempty"`
}

func (sp *ServicePort) ConvertToMarshalable() *MarshalableServiceProt {
	spObj := &MarshalableServiceProt{
		Port:       sp.Port,
		Protocol:   sp.Protocol,
		L4Protocol: &sp.L4Protocol,
		Expose:     sp.Expose,
		Default:    sp.Default,
	}
	if strings.EqualFold(sp.Protocol, "tcp") {
		spObj.Protocol = ""
	}
	if sp.L4Protocol == apiv1.ProtocolTCP {
		spObj.L4Protocol = nil
	}
	return spObj
}

// simplify marshal yaml
func (sp *ServicePort) MarshalYAML() (interface{}, error) {
	return yaml.Marshal(sp.ConvertToMarshalable())
}

// simplify marshal json
func (sp *ServicePort) MarshalJSON() ([]byte, error) {
	return json.Marshal(sp.ConvertToMarshalable())
}

// The ServicePort UnmarshalYAML unmarshal custom yaml definition
// which support more protocol and adapted previous version
func (sp *ServicePort) UnmarshalYAML(unmarshal func(interface{}) error) error {
	var portInt int
	var err error

	spObj := struct {
		Port       int            `yaml:"port"`
		Protocol   string         `yaml:"protocol"`
		L4Protocol apiv1.Protocol `yaml:"l4_protocol"`
		Expose     bool           `yaml:"expose"`
		Default    bool           `yaml:"default"`
	}{}

	if err = unmarshal(&portInt); err == nil {
		sp.Port = portInt
		sp.Protocol = "TCP"
		sp.L4Protocol = apiv1.ProtocolTCP
		sp.Expose = false
		sp.Default = false
		return nil
	} else if err = unmarshal(&spObj); err == nil {
		sp.Port = spObj.Port
		sp.Protocol = spObj.Protocol
		sp.L4Protocol = spObj.L4Protocol
		sp.Expose = spObj.Expose
		sp.Default = spObj.Default
		if sp.Protocol == "" {
			sp.Protocol = "TCP"
		}
		if sp.L4Protocol == "" {
			if sp.Protocol == "UDP" {
				sp.L4Protocol = apiv1.ProtocolUDP
			} else {
				sp.L4Protocol = apiv1.ProtocolTCP
			}
		}
		return nil
	}

	return err
}

// The ServicePort UnmarshalJSON unmarshal custom json definition
// which support more protocol and adapted previous version
func (sp *ServicePort) UnmarshalJSON(b []byte) error {
	var portInt int
	var err error

	spObj := struct {
		Port       int            `json:"port"`
		Protocol   string         `json:"protocol"`
		L4Protocol apiv1.Protocol `json:"l4_protocol"`
		Expose     bool           `json:"expose"`
		Default    bool           `json:"default"`
	}{}

	if err = json.Unmarshal(b, &portInt); err == nil {
		sp.Protocol = "TCP"
		sp.L4Protocol = apiv1.ProtocolTCP
		sp.Port = portInt
		sp.Expose = false
		sp.Default = false
		return nil
	} else if err = json.Unmarshal(b, &spObj); err == nil {
		sp.Protocol = spObj.Protocol
		sp.L4Protocol = spObj.L4Protocol
		sp.Port = spObj.Port
		sp.Expose = spObj.Expose
		sp.Default = spObj.Default
		if sp.Protocol == "" {
			sp.Protocol = "TCP"
		}
		if sp.L4Protocol == "" {
			if sp.Protocol == "UDP" {
				sp.L4Protocol = apiv1.ProtocolUDP
			} else {
				sp.L4Protocol = apiv1.ProtocolTCP
			}
		}
		return nil
	}
	return err
}

func (e *EnvMap) UnmarshalJSON(b []byte) error {
	if *e == nil {
		*e = map[string]string{}
	}
	envs := map[string]interface{}{}
	if err := json.Unmarshal(b, &envs); err != nil {
		return err
	}
	for k, v := range envs {
		switch t := v.(type) {
		case string:
			(*e)[k] = t
		case float64:
			(*e)[k] = strconv.FormatFloat(t, 'f', -1, 64)
		default:
			(*e)[k] = fmt.Sprintf("%v", t)
		}
	}
	return nil
}

func (v *Volume) UnmarshalJSON(data []byte) error {
	return unmarshalVolume(v, func(i interface{}) (err error) {
		if s, ok := i.(*string); ok {
			if *s, err = strconv.Unquote(string(data)); err != nil {
				*s = string(data)
			}
			return nil
		}
		return json.Unmarshal(data, i)
	})
}

/*
volumes:
  - name~storage:/container/path
  - name:/container/path	# 没有 ~storage 表示使用默认本地默认存储，就是本地磁盘
  - /container/path		# 没有指定 volume name，默认生成 uuid
  - log-volume~nas:/var/logs	# ~nas 表示使用 nas 存储
  - data-volume~nas:/home/admin/data
*/
func (v *Volume) UnmarshalYAML(unmarshal func(interface{}) error) error {
	return unmarshalVolume(v, unmarshal)
}

func unmarshalVolume(v *Volume, unmarshal func(interface{}) error) error {
	var s string
	volobj := struct {
		ID *string `json:"id"`
		// nfs, local
		Storage  string `json:"storage"`
		Path     string `json:"path"`
		Type     string `json:"type,omitempty"`
		Capacity int32  `json:"size,omitempty"`
		//SourcePath string          `json:"sourcePath,omitempty"`
		TargetPath string          `json:"targetPath,omitempty"`
		ReadOnly   bool            `json:"readOnly,omitempty"`
		Snapshot   *VolumeSnapshot `json:"snapshot,omitempty"`
	}{}
	if err := unmarshal(&volobj); err == nil {
		v.ID = volobj.ID
		v.Storage = volobj.Storage
		v.Path = volobj.Path
		v.Type = volobj.Type
		v.Capacity = volobj.Capacity
		//v.SourcePath = volobj.SourcePath
		v.TargetPath = volobj.TargetPath
		v.ReadOnly = volobj.ReadOnly
		v.Snapshot = volobj.Snapshot
		return nil
	}
	if err := unmarshal(&s); err != nil {
		return err
	}
	splitted := strings.SplitN(s, ":", 2)
	var newv Volume
	switch len(splitted) {
	case 0:
		return fmt.Errorf("illegal volume path: %v", s)
	case 1:
		if splitted[0] == "" {
			return fmt.Errorf("illegal empty volume path")
		}
		newv.Path = splitted[0]
	case 2:
		nameAndStorage := strings.SplitN(splitted[0], "~", 2)
		if len(nameAndStorage) == 1 {
			newv.ID = &(nameAndStorage[0])
		} else if len(nameAndStorage) == 2 {
			newv.ID = &(nameAndStorage[0])
			newv.Storage = nameAndStorage[1]
		}
		newv.Path = splitted[1]
	}
	*v = newv
	return nil
}

// func (v Volume) MarshalYAML() (interface{}, error) {
// 	var r string
// 	r += v.Name
// 	if v.Storage != "" {
// 		r += "~" + v.Storage
// 	}
// 	if r != "" {
// 		r += ":"
// 	}
// 	r += v.Path
// 	return r, nil
// }

func (sl *Selector) UnmarshalJSON(data []byte) error {
	return unmarshalSelector(sl, func(i interface{}) (err error) {
		if s, ok := i.(*string); ok {
			if *s, err = strconv.Unquote(string(data)); err != nil {
				*s = string(data)
			}
			return nil
		}
		return json.Unmarshal(data, i)
	})
}

func (sl Selector) MarshalJSON() ([]byte, error) {
	data, err := marshalSelector(sl)
	if err != nil {
		return nil, err
	}
	return []byte(strconv.Quote(data.(string))), nil
}

func (sl *Selector) UnmarshalYAML(unmarshal func(interface{}) error) error {
	return unmarshalSelector(sl, unmarshal)
}

func (sl Selector) MarshalYAML() (interface{}, error) {
	return marshalSelector(sl)
}

func unmarshalSelector(selector *Selector, unmarshal func(interface{}) error) error {
	obj := struct {
		Not    bool     `json:"not"`
		Values []string `json:"values"`
	}{}
	if err := unmarshal(&obj); err == nil {
		selector.Not = obj.Not
		selector.Values = obj.Values
		return nil
	}
	var s string
	if err := unmarshal(&s); err != nil {
		return err
	}
	s = strutil.Trim(s)
	matches := selectorNotExpr.FindStringSubmatch(s)
	if matches != nil && len(matches) > 1 && matches[1] != "" {
		selector.Not = true
		selector.Values = []string{matches[1]}
		return nil
	}
	if selectorOrExpr.MatchString(s) {
		identifies := strutil.Split(s, "OR", true)
		selector.Values = strutil.TrimSlice(identifies)
		return nil
	}
	return fmt.Errorf("failed to unmarshal {Selector}: %s", s)
}

func (bs *Binds) UnmarshalYAML(unmarshal func(interface{}) error) error {
	binds := []Bind{}
	if err := unmarshal(&binds); err != nil {
		bindsstr := []string{}
		if err := unmarshal(&bindsstr); err != nil {
			return err
		}
		*bs = bindsstr
		return nil
	}
	r := []string{}
	for _, bind := range binds {
		tp := bind.Type
		if tp == "" {
			tp = "rw"
		}
		r = append(r, strutil.Join([]string{bind.HostPath, bind.ContainerPath, tp}, ":", true))
	}
	*bs = r
	return nil
}

func marshalSelector(selector Selector) (interface{}, error) {
	if selector.Not && len(selector.Values) > 0 {
		return strutil.Concat("NOT ", selector.Values[0]), nil
	}
	if selector.Not {
		return nil, fmt.Errorf("failed to marshal {Selector}")
	}
	return strutil.Join(selector.Values, " OR ", true), nil
}

func (bs Binds) MarshalJSON() ([]byte, error) {
	binds, err := ParseBinds(bs)
	if err != nil {
		return nil, err
	}
	return json.Marshal(binds)
}

func (bs *Binds) UnmarshalJSON(b []byte) error {
	binds := []Bind{}
	if err := json.Unmarshal(b, &binds); err != nil {
		bindsstr := []string{}
		if err := json.Unmarshal(b, &bindsstr); err != nil {
			return err
		}
		*bs = bindsstr
		return nil
	}
	r := []string{}
	for _, bind := range binds {
		tp := bind.Type
		if tp == "" {
			tp = "rw"
		}
		r = append(r, strutil.Join([]string{bind.HostPath, bind.ContainerPath, tp}, ":", true))
	}
	*bs = r
	return nil
}

func ParseBinds(binds []string) ([]Bind, error) {
	r := []Bind{}
	for _, bind := range binds {
		var host, container, tp string
		parts := strings.SplitN(bind, ":", 3)
		if len(parts) == 3 {
			host = parts[0]
			container = parts[1]
			tp = parts[2]
		} else if len(parts) == 2 {
			host = parts[0]
			container = parts[1]
			tp = "rw"
		} else {
			return nil, fmt.Errorf("illegal `binds` value")
		}
		r = append(r, Bind{host, container, tp})
	}
	return r, nil
}

type Bind struct {
	HostPath      string `yaml:"host,omitempty" json:"host"`
	ContainerPath string `yaml:"container" json:"container"`
	Type          string `yaml:"type" json:"type"`
}

type SharedDir struct {
	Main    string `yaml:"main" json:"main"`
	SideCar string `yaml:"sidecar" json:"sidecar"`
}

type SideCar struct {
	Image      string      `yaml:"image,omitempty" json:"image"`
	Cmd        string      `yaml:"cmd,omitempty" json:"cmd"`
	Envs       EnvMap      `yaml:"envs,omitempty" json:"envs,omitempty"`
	SharedDirs []SharedDir `yaml:"shared_dir,omitempty" json:"shared_dir,omitempty"`
	Resources  Resources   `yaml:"resources,omitempty" json:"resources"`
}

type HealthCheck struct {
	HTTP *HTTPCheck `yaml:"http,omitempty" json:"http,omitempty"`
	Exec *ExecCheck `yaml:"exec,omitempty" json:"exec,omitempty"`
}

type HTTPCheck struct {
	Port     int    `yaml:"port,omitempty" json:"port,omitempty"`
	Path     string `yaml:"path,omitempty" json:"path,omitempty"`
	Duration int    `yaml:"duration,omitempty" json:"duration,omitempty"`
}

type ExecCheck struct {
	Cmd      string `yaml:"cmd,omitempty" json:"cmd,omitempty"`
	Duration int    `yaml:"duration,omitempty" json:"duration,omitempty"`
}

type Resources struct {
	CPU                      float64           `yaml:"cpu,omitempty" json:"cpu"`
	Mem                      int               `yaml:"mem,omitempty" json:"mem"`
	MaxCPU                   float64           `yaml:"max_cpu,omitempty" json:"max_cpu"`
	MaxMem                   int               `yaml:"max_mem,omitempty" json:"max_mem"`
	Disk                     int               `yaml:"disk,omitempty" json:"disk"`
	Network                  map[string]string `yaml:"network,omitempty" json:"network"`
	EmptyDirCapacity         int               `yaml:"emptydir_size,omitempty" json:"emptydir_size"`
	EphemeralStorageCapacity int               `yaml:"ephemeral_storage_size,omitempty" json:"ephemeral_storage_size"`
}

type Deployments struct {
	Replicas int               `yaml:"replicas,omitempty" json:"replicas"`
	Policies string            `yaml:"policies,omitempty" json:"policies"`
	Labels   map[string]string `yaml:"labels,omitempty" json:"labels,omitempty"`
	// Type indicates the type of Deployments, per-node,stateful and stateless are supported
	Workload string `yaml:"workload,omitempty" json:"workload,omitempty"`
	// Selectors available selectors:
	// [location]
	Selectors Selectors `yaml:"selectors,omitempty" json:"selectors,omitempty"`
}

type TrafficSecurity struct {
	Mode string `yaml:"mode,omitempty" json:"mode,omitempty"`
}

type Endpoint struct {
	Domain      string           `yaml:"domain,omitempty" json:"domain,omitempty"`
	Path        string           `yaml:"path,omitempty" json:"path,omitempty"`
	BackendPath string           `yaml:"backend_path,omitempty" json:"backend_path,omitempty"`
	Policies    EndpointPolicies `yaml:"policies,omitempty" json:"policies,omitempty"`
}

type EndpointPolicies struct {
	Cors      *map[string]interface{} `yaml:"cors,omitempty" json:"cors,omitempty"`
	RateLimit *map[string]interface{} `yaml:"rate_limit,omitempty" json:"rate_limit,omitempty"`
}

func convert(i interface{}) interface{} {
	switch x := i.(type) {
	case map[interface{}]interface{}:
		m2 := map[string]interface{}{}
		for k, v := range x {
			ks := fmt.Sprintf("%v", k)
			m2[ks] = convert(v)
		}
		return m2
	case []interface{}:
		for i, v := range x {
			x[i] = convert(v)
		}
	}
	return i
}

func (a *AddOn) MarshalJSON() ([]byte, error) {
	for k, v := range a.Actions {
		a.Actions[k] = convert(v)
	}
	new := struct {
		Plan    string                 `json:"plan"`
		As      string                 `json:"as,omitempty"`
		Options map[string]string      `json:"options,omitempty"`
		Actions map[string]interface{} `json:"actions,omitempty"`
		Image   string                 `json:"image,omitempty"`
	}{
		Plan:    a.Plan,
		As:      a.As,
		Options: a.Options,
		Actions: a.Actions,
		Image:   a.Image,
	}
	return json.Marshal(new)
}

// =========================== default visitor =====================================
type DiceYmlVisitor interface {
	VisitObject(v DiceYmlVisitor, obj *Object)
	VisitEnvObject(v DiceYmlVisitor, obj *EnvObject)
	VisitEnvObjects(v DiceYmlVisitor, obj *EnvObjects)
	VisitService(v DiceYmlVisitor, obj *Service)
	VisitServices(v DiceYmlVisitor, obj *Services)
	VisitJobs(v DiceYmlVisitor, obj *Jobs)
	VisitJob(v DiceYmlVisitor, obj *Job)
	VisitAddOn(v DiceYmlVisitor, obj *AddOn)
	VisitAddOns(v DiceYmlVisitor, obj *AddOns)
	VisitResources(v DiceYmlVisitor, obj *Resources)
	VisitHealthCheck(v DiceYmlVisitor, obj *HealthCheck)
	VisitHTTPCheck(v DiceYmlVisitor, obj *HTTPCheck)
	VisitExecCheck(v DiceYmlVisitor, obj *ExecCheck)
	VisitDeployments(v DiceYmlVisitor, obj *Deployments)
	VisitBinds(v DiceYmlVisitor, obj *Binds)
	VisitK8SSnippet(v DiceYmlVisitor, obj *K8SSnippet)
	VisitContainerSnippet(v DiceYmlVisitor, obj *ContainerSnippet)
}

func (obj *Object) Accept(v DiceYmlVisitor) {
	v.VisitObject(v, obj)

	if obj.Services == nil {
		obj.Services = Services{}
	}
	obj.Services.Accept(v)
	if obj.Jobs == nil {
		obj.Jobs = Jobs{}
	}
	obj.Jobs.Accept(v)
	if obj.AddOns == nil {
		obj.AddOns = AddOns{}
	}
	obj.AddOns.Accept(v)
	if obj.Environments == nil {
		obj.Environments = EnvObjects{}
	}
	obj.Environments.Accept(v)
}

// 默认不会像 Object 一样默认去遍历 Services, AddOns
// 比如validate，envobject中的service可能是不全的，但也是正确的
// 如果需要遍历envobject下的service和addon，需要手动去遍历
func (obj *EnvObject) Accept(v DiceYmlVisitor) {
	v.VisitEnvObject(v, obj)
}
func (obj *EnvObjects) Accept(v DiceYmlVisitor) {
	v.VisitEnvObjects(v, obj)
}
func (obj *Service) Accept(v DiceYmlVisitor) {
	v.VisitService(v, obj)

	obj.Resources.Accept(v)
	obj.Deployments.Accept(v)
	obj.HealthCheck.Accept(v)
	obj.Binds.Accept(v)
	if obj.K8SSnippet != nil {
		obj.K8SSnippet.Accept(v)
	}
}
func (obj *Services) Accept(v DiceYmlVisitor) {
	v.VisitServices(v, obj)
}
func (obj *Job) Accept(v DiceYmlVisitor) {
	v.VisitJob(v, obj)

	obj.Resources.Accept(v)
	obj.Binds.Accept(v)
}
func (obj *Jobs) Accept(v DiceYmlVisitor) {
	v.VisitJobs(v, obj)
}
func (obj *AddOn) Accept(v DiceYmlVisitor) {
	v.VisitAddOn(v, obj)
}
func (obj *AddOns) Accept(v DiceYmlVisitor) {
	v.VisitAddOns(v, obj)
}
func (obj *Resources) Accept(v DiceYmlVisitor) {
	v.VisitResources(v, obj)
}
func (obj *HealthCheck) Accept(v DiceYmlVisitor) {
	if obj.HTTP == nil {
		obj.HTTP = new(HTTPCheck)
	}
	obj.HTTP.Accept(v)
	if obj.Exec == nil {
		obj.Exec = new(ExecCheck)
	}
	obj.Exec.Accept(v)

	v.VisitHealthCheck(v, obj)
}
func (obj *HTTPCheck) Accept(v DiceYmlVisitor) {
	v.VisitHTTPCheck(v, obj)
}
func (obj *ExecCheck) Accept(v DiceYmlVisitor) {
	v.VisitExecCheck(v, obj)
}

func (obj *Deployments) Accept(v DiceYmlVisitor) {
	v.VisitDeployments(v, obj)
}

func (obj *Binds) Accept(v DiceYmlVisitor) {
	v.VisitBinds(v, obj)
}

func (obj *K8SSnippet) Accept(v DiceYmlVisitor) {
	v.VisitK8SSnippet(v, obj)
	if obj.Container != nil {
		obj.Container.Accept(v)
	}
}

func (obj *ContainerSnippet) Accept(v DiceYmlVisitor) {
	v.VisitContainerSnippet(v, obj)
}

type DefaultVisitor struct {
	// used when iter on Object.Services
	currentService string
	currentJob     string
}

func (*DefaultVisitor) VisitObject(v DiceYmlVisitor, obj *Object)         {}
func (*DefaultVisitor) VisitEnvObject(v DiceYmlVisitor, obj *EnvObject)   {}
func (*DefaultVisitor) VisitEnvObjects(v DiceYmlVisitor, obj *EnvObjects) {}
func (*DefaultVisitor) VisitService(v DiceYmlVisitor, obj *Service)       {}
func (o *DefaultVisitor) VisitServices(v DiceYmlVisitor, obj *Services) {
	for name, service := range *obj {
		o.currentService = name
		service.Accept(v)
	}
	o.currentService = ""
}
func (o *DefaultVisitor) VisitJobs(v DiceYmlVisitor, obj *Jobs) {
	for name, job := range *obj {
		o.currentJob = name
		job.Accept(v)
	}
	o.currentJob = ""
}
func (o *DefaultVisitor) VisitJob(v DiceYmlVisitor, obj *Job)                         {}
func (*DefaultVisitor) VisitAddOn(v DiceYmlVisitor, obj *AddOn)                       {}
func (*DefaultVisitor) VisitAddOns(v DiceYmlVisitor, obj *AddOns)                     {}
func (*DefaultVisitor) VisitResources(v DiceYmlVisitor, obj *Resources)               {}
func (*DefaultVisitor) VisitHealthCheck(v DiceYmlVisitor, obj *HealthCheck)           {}
func (*DefaultVisitor) VisitHTTPCheck(v DiceYmlVisitor, obj *HTTPCheck)               {}
func (*DefaultVisitor) VisitExecCheck(v DiceYmlVisitor, obj *ExecCheck)               {}
func (*DefaultVisitor) VisitDeployments(v DiceYmlVisitor, obj *Deployments)           {}
func (*DefaultVisitor) VisitBinds(v DiceYmlVisitor, obj *Binds)                       {}
func (*DefaultVisitor) VisitK8SSnippet(v DiceYmlVisitor, obj *K8SSnippet)             {}
func (*DefaultVisitor) VisitContainerSnippet(v DiceYmlVisitor, obj *ContainerSnippet) {}
