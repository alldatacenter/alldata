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
	"path"

	"k8s.io/utils/pointer"
)

const (
	ProcSysVmRelativePath   = "sys/vm/"
	MemcgReaperRelativePath = "kernel/mm/memcg_reaper/"

	MinFreeKbytesFileName        = "min_free_kbytes"
	WatermarkScaleFactorFileName = "watermark_scale_factor"
	MemcgReapBackGroundFileName  = "reap_background"
)

var (
	MinFreeKbytesValidator        = &RangeValidator{min: 10 * 1024, max: 10 * 1024 * 1024}
	WatermarkScaleFactorValidator = &RangeValidator{min: 10, max: 400}
	MemcgReapBackGroundValidator  = &RangeValidator{min: 0, max: 1}
)

var (
	MinFreeKbytes        = NewCommonSystemResource(ProcSysVmRelativePath, MinFreeKbytesFileName, GetProcRootDir).WithValidator(MinFreeKbytesValidator)
	WatermarkScaleFactor = NewCommonSystemResource(ProcSysVmRelativePath, WatermarkScaleFactorFileName, GetProcRootDir).WithValidator(WatermarkScaleFactorValidator)
	MemcgReapBackGround  = NewCommonSystemResource(MemcgReaperRelativePath, MemcgReapBackGroundFileName, GetSysRootDir).WithValidator(MemcgReapBackGroundValidator).WithCheckSupported(SupportedIfFileExists)
)

var _ Resource = &SystemResource{}

type SystemResource struct {
	Type         ResourceType
	RootDir      func() string
	RelativePath string
	FileName     string
	Validator    ResourceValidator

	Supported      *bool
	SupportMsg     string
	CheckSupported func(r Resource, dynamicPath string) (isSupported bool, msg string)
}

func (c *SystemResource) ResourceType() ResourceType {
	if len(c.Type) > 0 {
		return c.Type
	}
	return ResourceType(c.FileName)
}

func (c *SystemResource) Path(dynamicPath string) string {
	return path.Join(c.RootDir(), c.RelativePath, c.FileName)
}

func (c *SystemResource) IsSupported(dynamicPath string) (bool, string) {
	if c.Supported == nil {
		if c.CheckSupported == nil {
			return false, "unknown support status"
		}
		return c.CheckSupported(c, dynamicPath)
	}
	return *c.Supported, c.SupportMsg
}

func (c *SystemResource) IsValid(v string) (bool, string) {
	if c.Validator == nil {
		return true, ""
	}
	return c.Validator.Validate(v)
}

func (c *SystemResource) WithValidator(validator ResourceValidator) Resource {
	c.Validator = validator
	return c
}

func (c *SystemResource) WithSupported(isSupported bool, msg string) Resource {
	c.Supported = pointer.BoolPtr(isSupported)
	c.SupportMsg = msg
	return c
}

func (c *SystemResource) WithCheckSupported(checkSupportedFn func(r Resource, parentDir string) (isSupported bool, msg string)) Resource {
	c.Supported = nil
	c.CheckSupported = checkSupportedFn
	return c
}

func NewCommonSystemResource(relativePath, fileName string, Rootdir func() string) Resource {
	return &SystemResource{Type: ResourceType(fileName), FileName: fileName, RelativePath: relativePath, RootDir: Rootdir, Supported: pointer.Bool(true)}
}
