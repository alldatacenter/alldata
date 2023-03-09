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
	"path/filepath"

	"k8s.io/klog/v2"
)

var HostSystemInfo = collectVersionInfo()

func collectVersionInfo() VersionInfo {
	return VersionInfo{
		IsAnolisOS: isAnolisOS(),
	}
}

type VersionInfo struct {
	// Open Anolis OS (kernel): https://github.com/alibaba/cloud-kernel
	IsAnolisOS bool
}

func isAnolisOS() bool {
	return isSupportBvtOrWmarRatio()
}

func isSupportBvtOrWmarRatio() bool {
	bvtFilePath := filepath.Join(Conf.CgroupRootDir, CgroupCPUDir, CPUBVTWarpNsName)
	exists, err := PathExists(bvtFilePath)
	klog.V(2).Infof("[%v] PathExists exists %v, err: %v", bvtFilePath, exists, err)
	if err == nil && exists {
		return true
	}

	wmarkRatioPath := filepath.Join(Conf.CgroupRootDir, CgroupMemDir, "*", MemoryWmarkRatioName)
	matches, err := filepath.Glob(wmarkRatioPath)
	klog.V(2).Infof("[%v] PathExists wmark_ratio exists %v, err: %v", wmarkRatioPath, matches, err)
	if err == nil && len(matches) > 0 {
		return true
	}

	return false
}
