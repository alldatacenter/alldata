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

package k8s

import (
	"encoding/base64"
	"encoding/json"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"
	apiv1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/secret"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

func TestUpdateImageSecret(t *testing.T) {
	k8s := &Kubernetes{
		secret: &secret.Secret{},
	}
	service := &apistructs.Service{
		Name:          "test-service",
		Namespace:     "test",
		Image:         "test",
		ImageUsername: "test",
		ImagePassword: "test",
		Cmd:           "",
		Ports:         nil,
		ProxyPorts:    nil,
		Vip:           "",
		ShortVIP:      "",
		ProxyIp:       "",
		PublicIp:      "",
		Scale:         0,
		Resources: apistructs.Resources{
			Cpu: 0.1,
			Mem: 512,
		},
		Depends:            nil,
		Env:                nil,
		Labels:             nil,
		DeploymentLabels:   nil,
		Selectors:          nil,
		Binds:              nil,
		Volumes:            nil,
		Hosts:              nil,
		HealthCheck:        nil,
		NewHealthCheck:     nil,
		SideCars:           nil,
		InitContainer:      nil,
		InstanceInfos:      nil,
		MeshEnable:         nil,
		TrafficSecurity:    diceyml.TrafficSecurity{},
		WorkLoad:           "",
		ProjectServiceName: "",
		K8SSnippet:         nil,
		StatusDesc:         apistructs.StatusDesc{},
	}
	servicegroup := &apistructs.ServiceGroup{
		Dice: apistructs.Dice{
			ID:                   "test",
			Type:                 "service",
			Labels:               map[string]string{},
			Services:             []apistructs.Service{*service},
			ServiceDiscoveryKind: "",
			ServiceDiscoveryMode: "",
			ProjectNamespace:     "",
		},
	}

	monkey.PatchInstanceMethod(reflect.TypeOf(k8s.secret), "Get", func(sec *secret.Secret, namespace, name string) (*apiv1.Secret, error) {
		b := []byte{}
		return &apiv1.Secret{Data: map[string][]byte{".dockerconfigjson": b}}, nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(k8s.secret), "Update", func(sec *secret.Secret, secret *apiv1.Secret) error {
		return nil
	})
	registryInfos := k8s.composeRegistryInfos(servicegroup)
	_ = k8s.UpdateImageSecret("test", registryInfos)
	assert.Equal(t, nil, nil)

}

func TestParseImageSecret(t *testing.T) {
	infos := []apistructs.RegistryInfo{
		{
			Host:     "fake-registry.erda.cloud",
			UserName: "fake",
			Password: "fake",
		},
	}

	dockerConfigJson := &apistructs.RegistryAuthJson{
		Auths: map[string]apistructs.RegistryUserInfo{},
	}

	authString := base64.StdEncoding.EncodeToString([]byte(infos[0].UserName + ":" + infos[0].Password))
	dockerConfigJson.Auths[infos[0].Host] = apistructs.RegistryUserInfo{Auth: authString}

	ret, err := json.Marshal(dockerConfigJson)
	assert.NoError(t, err)

	type args struct {
		s *apiv1.Secret
	}
	tests := []struct {
		name    string
		args    args
		want    *apiv1.Secret
		wantErr bool
	}{
		{
			name: "standard",
			args: args{
				s: &apiv1.Secret{
					ObjectMeta: metav1.ObjectMeta{
						Name:      AliyunRegistry,
						Namespace: "erda-system",
					},
					Data: map[string][]byte{
						apiv1.DockerConfigJsonKey: []byte("{\"auths\":{}}"),
					}},
			},
			want: &apiv1.Secret{
				ObjectMeta: metav1.ObjectMeta{
					Name:      AliyunRegistry,
					Namespace: "erda-system",
				},
				Data: map[string][]byte{
					apiv1.DockerConfigJsonKey: ret,
				},
			},
			wantErr: false,
		},
		{
			name: "nil",
			args: args{
				s: nil,
			},
			want: &apiv1.Secret{
				ObjectMeta: metav1.ObjectMeta{
					Name:      AliyunRegistry,
					Namespace: apiv1.NamespaceDefault,
				},
				Data: map[string][]byte{
					apiv1.DockerConfigJsonKey: ret,
				},
				Type: apiv1.SecretTypeDockerConfigJson,
			},
			wantErr: false,
		}}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := parseImageSecret(metav1.NamespaceDefault, infos, tt.args.s)
			if (err != nil) != tt.wantErr {
				t.Errorf("parseImageSecret error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			assert.Equal(t, got, tt.want)
		})
	}
}
