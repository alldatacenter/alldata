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

package aliyun_resources

import (
	"fmt"
	"net/url"
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/http/httpclient"
)

func GetProjectClusterName(ctx Context, projid string, workspace string) (clusterName, projectName string, err error) {
	// parse project id
	projID, err := strconv.ParseUint(projid, 10, 64)
	if err != nil {
		logrus.Errorf("parse project id failed, project id: %s, error:%v", projid, err)
		return
	}

	// get project info
	proj, err := ctx.Bdl.GetProjectWithSetter(projID, httpclient.SetParams(url.Values{"withQuota": {"true"}}))
	if err != nil {
		logrus.Errorf("get project info failed, project id: %s, error:%v", projid, err)
		return
	}
	projectName = proj.Name

	// get cluster name
	clusterName = proj.ClusterConfig[workspace]
	if clusterName == "" && workspace != "" {
		err = fmt.Errorf("get cluster name failed, empty clustr name, project id: %s, workspace:%s", projid, workspace)
		logrus.Errorf("%s, project cluster config:%+v", err.Error(), proj.ClusterConfig)
		return
	}
	return
}
