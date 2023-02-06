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

package bundle

//import (
//	"os"
//	"testing"
//
//	"github.com/aliyun/alibaba-cloud-sdk-go/services/cloudapi"
//	"github.com/sirupsen/logrus"
//	"github.com/stretchr/testify/require"
//)
//
//var (
//	clusterName = "terminus-dev"
//)
//
//func TestBundle_DoRemoteAliyunAction(t *testing.T) {
//	os.Setenv("OPS_ADDR", "ops.default.svc.cluster.local:9027")
//	defer func() {
//		os.Unsetenv("OPS_ADDR")
//	}()
//	logrus.SetOutput(os.Stdout)
//	b := New(WithCMP())
//	resp := cloudapi.CreateDescribeZonesResponse()
//	err := b.DoRemoteAliyunAction("1", "terminus-dev", cloudapi.GetEndpointType(),
//		cloudapi.GetEndpointMap(), cloudapi.CreateDescribeZonesRequest(), resp)
//	logrus.Error(resp)
//	require.NoError(t, err)
//}
