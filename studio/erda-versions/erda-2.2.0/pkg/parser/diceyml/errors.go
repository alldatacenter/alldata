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

var (
	notfoundJob                = errortype("not found job in yaml")
	notfoundService            = errortype("not found service in yaml")
	invalidService             = errortype("invalid service defined in yaml")
	emptyServiceJobList        = errortype("empty service and job list")
	notfoundVersion            = errortype("not found version in yaml")
	invalidReplicas            = errortype("invalid replicas defined in yaml")
	invalidPolicy              = errortype("invalid policy defined in yaml")
	invalidCPU                 = errortype("invalid cpu defined in yaml")
	invalidMaxCPU              = errortype("invalid max cpu defined in yaml")
	invalidMaxMem              = errortype("invalid max mem defined in yaml")
	invalidMem                 = errortype("invalid memory defined in yaml")
	invalidDisk                = errortype("invalid disk defined in yaml")
	invalidNetworkMode         = errortype("invalid network mode defined in yaml, must be 'container' or 'host'")
	invalidBindHostPath        = errortype("invalid binds hostpath, must be absolute path")
	invalidBindContainerPath   = errortype("invalid binds containerpath, must be absolute path")
	invalidBindType            = errortype("invalid bind type")
	invalidPort                = errortype("invalid port defined in yaml")
	invalidExpose              = errortype("invalid expose defined in yaml")
	invalidVolume              = errortype("invalid volume defined in yaml")
	invalidAddonPlan           = errortype("invalid addon plan in yaml")
	invalidImage               = errortype("invalid image defined in yaml")
	invalidTrafficSecurityMode = errortype("invalid traffic security mode in yaml, must be 'https'")
	emptyEndpointDomain        = errortype("empty domain in endpoints")
	invalidEndpointDomain      = errortype("invalid domain in endpoints")
	invalidEndpointPath        = errortype("invalid path in endpoints, must start with '/'")
	invalidEmptyDir            = errortype("invalid emptydir_size defined in yaml")
	invalidEphemeralStorage    = errortype("invalid ephemeral_storage_size defined in yaml, at least set 1")
)

type errortype string

func (e errortype) Error() string {
	return string(e)
}
