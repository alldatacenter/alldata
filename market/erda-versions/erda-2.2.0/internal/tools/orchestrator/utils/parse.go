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
	"net"
	"net/http"
	"strings"

	"github.com/erda-project/erda/apistructs"
)

func GetRealIP(request *http.Request) string {
	ra := request.RemoteAddr
	if ip := request.Header.Get("X-Forwarded-For"); ip != "" {
		ra = strings.Split(ip, ", ")[0]
	} else if ip := request.Header.Get("X-Real-IP"); ip != "" {
		ra = ip
	} else {
		ra, _, _ = net.SplitHostPort(ra)
	}
	return ra
}

func ParseOrderName(uuid string) string {
	if len(uuid) >= 6 {
		return uuid[:6]
	}
	return uuid
}

func ParseDeploymentStatus(status apistructs.DeploymentStatus) apistructs.DeploymentStatus {
	switch status {
	case apistructs.DeploymentStatusWaitApprove, apistructs.DeploymentStatusInit,
		apistructs.DeploymentStatusWaiting, apistructs.DeploymentStatusDeploying:
		return apistructs.DeploymentStatusDeploying
	case apistructs.DeploymentStatusCanceling, apistructs.DeploymentStatusCanceled:
		return apistructs.DeploymentStatusCanceled
	case apistructs.DeploymentStatusFailed, apistructs.DeploymentStatusOK:
		return status
	default:
		return apistructs.DeployStatusWaitDeploy
	}
}

func ParseDeploymentOrderStatus(appStatus apistructs.DeploymentOrderStatusMap) apistructs.DeploymentOrderStatus {
	if appStatus == nil || len(appStatus) == 0 {
		return apistructs.DeployStatusWaitDeploy
	}

	status := make([]apistructs.DeploymentStatus, 0)
	for _, a := range appStatus {
		if a.DeploymentStatus == apistructs.DeploymentStatusWaitApprove ||
			a.DeploymentStatus == apistructs.DeploymentStatusInit ||
			a.DeploymentStatus == apistructs.DeploymentStatusWaiting ||
			a.DeploymentStatus == apistructs.DeploymentStatusDeploying {
			return apistructs.DeploymentOrderStatus(apistructs.DeploymentStatusDeploying)
		}
		status = append(status, a.DeploymentStatus)
	}

	var (
		isFailed   bool
		isAllEmpty = true
	)

	for _, s := range status {
		if s == apistructs.DeploymentStatusCanceling ||
			s == apistructs.DeploymentStatusCanceled {
			return apistructs.DeploymentOrderStatus(apistructs.DeploymentStatusCanceled)
		}
		if s == apistructs.DeploymentStatusFailed {
			isFailed = true
		}
		if s != "" {
			isAllEmpty = false
		}
	}

	if isFailed {
		return apistructs.DeploymentOrderStatus(apistructs.DeploymentStatusFailed)
	}

	if isAllEmpty {
		return apistructs.DeploymentOrderStatus(apistructs.DeploymentStatusDeploying)
	}

	return apistructs.DeploymentOrderStatus(apistructs.DeploymentStatusOK)
}
