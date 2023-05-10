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

package istioctl

//import (
//	"context"
//	"fmt"
//	"testing"
//
//	"gotest.tools/assert"
//	"k8s.io/apimachinery/pkg/api/errors"
//	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
//
//	"github.com/erda-project/erda/pkg/clientgo"
//	"github.com/erda-project/erda/pkg/clientgo/restclient"
//)
//
//func TestNewIstioClient(t *testing.T) {
//	restclient.SetInetAddr("netportal.default.svc.cluster.local")
//	client, _ := clientgo.New("inet://dev.terminus.io/kubernetes.default.svc.cluster.local")
//	// if err != nil {
//	// 	panic(err)
//	// }
//	ns := "foo"
//	var dr interface{}
//	err := client.CustomClient.NetworkingV1alpha3().DestinationRules(ns).Delete(context.TODO(), "httpbin", metav1.DeleteOptions{})
//	if err != nil {
//		assert.Equal(t, errors.IsNotFound(err), true)
//	}
//	fmt.Printf("%+v\n, err:%+v", dr, err)
//}
