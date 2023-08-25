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

package system

import (
	"fmt"
	"strings"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/klog/v2"
)

type CgroupDriverType string

const (
	Cgroupfs CgroupDriverType = "cgroupfs"
	Systemd  CgroupDriverType = "systemd"

	kubeletDefaultCgroupDriver = Cgroupfs

	KubeRootNameSystemd       = "kubepods.slice/"
	KubeBurstableNameSystemd  = "kubepods-burstable.slice/"
	KubeBesteffortNameSystemd = "kubepods-besteffort.slice/"

	KubeRootNameCgroupfs       = "kubepods/"
	KubeBurstableNameCgroupfs  = "burstable/"
	KubeBesteffortNameCgroupfs = "besteffort/"
)

func (c CgroupDriverType) Validate() bool {
	s := string(c)
	return s == string(Cgroupfs) || s == string(Systemd)
}

type formatter struct {
	ParentDir string
	QOSDirFn  func(qos corev1.PodQOSClass) string
	PodDirFn  func(qos corev1.PodQOSClass, podUID string) string
	// containerID format: "containerd://..." or "docker://..."
	ContainerDirFn func(id string) (string, error)

	PodIDParser       func(basename string) (string, error)
	ContainerIDParser func(basename string) (string, error)
}

var cgroupPathFormatterInSystemd = formatter{
	ParentDir: KubeRootNameSystemd,
	QOSDirFn: func(qos corev1.PodQOSClass) string {
		switch qos {
		case corev1.PodQOSBurstable:
			return KubeBurstableNameSystemd
		case corev1.PodQOSBestEffort:
			return KubeBesteffortNameSystemd
		case corev1.PodQOSGuaranteed:
			return "/"
		}
		return "/"
	},
	PodDirFn: func(qos corev1.PodQOSClass, podUID string) string {
		id := strings.ReplaceAll(podUID, "-", "_")
		switch qos {
		case corev1.PodQOSBurstable:
			return fmt.Sprintf("kubepods-burstable-pod%s.slice/", id)
		case corev1.PodQOSBestEffort:
			return fmt.Sprintf("kubepods-besteffort-pod%s.slice/", id)
		case corev1.PodQOSGuaranteed:
			return fmt.Sprintf("kubepods-pod%s.slice/", id)
		}
		return "/"
	},
	ContainerDirFn: func(id string) (string, error) {
		hashID := strings.Split(id, "://")
		if len(hashID) < 2 {
			return "", fmt.Errorf("parse container id %s failed", id)
		}

		switch hashID[0] {
		case "docker":
			return fmt.Sprintf("docker-%s.scope/", hashID[1]), nil
		case "containerd":
			return fmt.Sprintf("cri-containerd-%s.scope/", hashID[1]), nil
		default:
			return "", fmt.Errorf("unknown container protocol %s", id)
		}
	},
	PodIDParser: func(basename string) (string, error) {
		patterns := []struct {
			prefix string
			suffix string
		}{
			{
				prefix: "kubepods-besteffort-pod",
				suffix: ".slice",
			},
			{
				prefix: "kubepods-burstable-pod",
				suffix: ".slice",
			},

			{
				prefix: "kubepods-pod",
				suffix: ".slice",
			},
		}

		for i := range patterns {
			if strings.HasPrefix(basename, patterns[i].prefix) && strings.HasSuffix(basename, patterns[i].suffix) {
				return basename[len(patterns[i].prefix) : len(basename)-len(patterns[i].suffix)], nil
			}
		}
		return "", fmt.Errorf("fail to parse pod id: %v", basename)
	},
	ContainerIDParser: func(basename string) (string, error) {
		patterns := []struct {
			prefix string
			suffix string
		}{
			{
				prefix: "docker-",
				suffix: ".scope",
			},
			{
				prefix: "cri-containerd-",
				suffix: ".scope",
			},
		}

		for i := range patterns {
			if strings.HasPrefix(basename, patterns[i].prefix) && strings.HasSuffix(basename, patterns[i].suffix) {
				return basename[len(patterns[i].prefix) : len(basename)-len(patterns[i].suffix)], nil
			}
		}
		return "", fmt.Errorf("fail to parse pod id: %v", basename)
	},
}

var cgroupPathFormatterInCgroupfs = formatter{
	ParentDir: KubeRootNameCgroupfs,
	QOSDirFn: func(qos corev1.PodQOSClass) string {
		switch qos {
		case corev1.PodQOSBurstable:
			return KubeBurstableNameCgroupfs
		case corev1.PodQOSBestEffort:
			return KubeBesteffortNameCgroupfs
		case corev1.PodQOSGuaranteed:
			return "/"
		}
		return "/"
	},
	PodDirFn: func(qos corev1.PodQOSClass, podUID string) string {
		return fmt.Sprintf("pod%s/", podUID)
	},
	ContainerDirFn: func(id string) (string, error) {
		hashID := strings.Split(id, "://")
		if len(hashID) < 2 {
			return "", fmt.Errorf("parse container id %s failed", id)
		}
		if hashID[0] == "docker" || hashID[0] == "containerd" {
			return fmt.Sprintf("%s/", hashID[1]), nil
		} else {
			return "", fmt.Errorf("unknown container protocol %s", id)
		}
	},
	PodIDParser: func(basename string) (string, error) {
		if strings.HasPrefix(basename, "pod") {
			return basename[len("pod"):], nil
		}
		return "", fmt.Errorf("fail to parse pod id: %v", basename)
	},
	ContainerIDParser: func(basename string) (string, error) {
		return basename, nil
	},
}

// default use Systemd cgroup path format
var CgroupPathFormatter = cgroupPathFormatterInSystemd

func SetupCgroupPathFormatter(driver CgroupDriverType) {
	switch driver {
	case Systemd:
		CgroupPathFormatter = cgroupPathFormatterInSystemd
	case Cgroupfs:
		CgroupPathFormatter = cgroupPathFormatterInCgroupfs
	default:
		klog.Warningf("cgroup driver formatter not supported: '%s'", string(driver))
	}
}
