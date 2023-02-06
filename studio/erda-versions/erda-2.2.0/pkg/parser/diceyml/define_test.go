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
	"reflect"
	"testing"

	"github.com/stretchr/testify/assert"
	"gopkg.in/yaml.v2"
	apiv1 "k8s.io/api/core/v1"
)

func TestUnmarshalBinds(t *testing.T) {
	s := `
- /aaa:/dddd:ro
- /bbb:/ccc:rw
`
	binds := Binds{}
	assert.Nil(t, yaml.Unmarshal([]byte(s), &binds))

	bindsjson, err := json.Marshal(binds)
	assert.Nil(t, err)
	bindsresult := Binds{}
	assert.Nil(t, json.Unmarshal(bindsjson, &bindsresult))
	assert.Equal(t, binds, bindsresult)
}

func TestUnmarshalVolumes(t *testing.T) {
	s := `
- name~st:/ded/de/de
- /deded
- eee:/ddd
`
	vols := Volumes{}
	assert.Nil(t, yaml.Unmarshal([]byte(s), &vols))

}

func TestContainerSnippet_MarshalYAML(t *testing.T) {
	tests := []struct {
		name    string
		cs      *ContainerSnippet
		want    interface{}
		wantErr bool
	}{
		{
			"case1",
			&ContainerSnippet{
				Name: "test",
				TTY:  true,
				SecurityContext: &apiv1.SecurityContext{
					Privileged: &[]bool{true}[0],
				},
			},
			[]byte(`securityContext:
  privileged: true
tty: true
`),
			false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := tt.cs.MarshalYAML()
			if (err != nil) != tt.wantErr {
				t.Errorf("ContainerSnippet.MarshalYAML() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("ContainerSnippet.MarshalYAML() = %s, want %v", got, tt.want)
			}
		})
	}
}

func TestServicePort_MarshalYAML(t *testing.T) {
	type fields struct {
		Port       int
		Protocol   string
		L4Protocol apiv1.Protocol
		Expose     bool
		Default    bool
	}
	tests := []struct {
		name    string
		fields  fields
		want    interface{}
		wantErr bool
	}{
		{
			"case1",
			fields{
				Port:       80,
				Protocol:   "TCP",
				L4Protocol: apiv1.ProtocolTCP,
			},
			[]byte(`port: 80
`),
			false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			sp := &ServicePort{
				Port:       tt.fields.Port,
				Protocol:   tt.fields.Protocol,
				L4Protocol: tt.fields.L4Protocol,
				Expose:     tt.fields.Expose,
				Default:    tt.fields.Default,
			}
			got, err := sp.MarshalYAML()
			if (err != nil) != tt.wantErr {
				t.Errorf("ServicePort.MarshalYAML() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("ServicePort.MarshalYAML() = %s, want %v", got, tt.want)
			}
		})
	}
}
