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

package statesinformer

import (
	"encoding/json"
	"log"
	"net"
	"net/http"
	"net/http/httptest"
	"net/url"
	"strconv"
	"strings"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/client-go/rest"
)

var (
	token              string
	kubeletConfigzData = []byte(`
		{
		    "kubeletconfig": {
		        "enableServer": true,
		        "cpuManagerPolicy": "static",
		        "cpuManagerReconcilePeriod": "10s",
		        "evictionHard": {
		            "imagefs.available": "15%",
		            "memory.available": "222Mi",
		            "nodefs.available": "10%",
		            "nodefs.inodesFree": "5%"
		        },
		        "systemReserved": {
		            "cpu": "200m",
		            "memory": "1111Mi",
		            "pid": "1000"
		        },
		        "kubeReserved": {
		            "cpu": "200m",
		            "memory": "6666Mi",
		            "pid": "1000"
		        }
		    }
		}`,
	)
)

func validateAuth(r *http.Request) bool {
	bear := r.Header.Get("Authorization")
	if bear == "" {
		return false
	}
	parts := strings.Split(bear, "Bearer")
	if len(parts) != 2 {
		return false
	}

	httpToken := strings.TrimSpace(parts[1])
	if len(httpToken) < 1 {
		return false
	}
	if httpToken != token {
		return false
	}
	return true
}

func mockPodsList(w http.ResponseWriter, r *http.Request) {
	if !validateAuth(r) {
		w.WriteHeader(http.StatusUnauthorized)
		return
	}
	podList := new(corev1.PodList)
	b, err := json.Marshal(podList)
	if err != nil {
		log.Printf("codec error %+v", err.Error())
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	w.WriteHeader(http.StatusOK)
	w.Header().Set("Content-Type", "application/json")
	w.Write(b)
}

func mockGetKubeletConfiguration(w http.ResponseWriter, r *http.Request) {
	if !validateAuth(r) {
		w.WriteHeader(http.StatusUnauthorized)
		return
	}
	w.WriteHeader(http.StatusOK)
	w.Header().Set("Content-Type", "application/json")
	w.Write(kubeletConfigzData)
}

func parseHostAndPort(rawURL string) (string, string, error) {
	u, err := url.Parse(rawURL)
	if err != nil {
		return "", "0", err
	}
	return net.SplitHostPort(u.Host)
}

func Test_kubeletStub_GetAllPods(t *testing.T) {
	t.Run("test not panic", func(t *testing.T) {
		token = "token"

		server := httptest.NewTLSServer(http.HandlerFunc(mockPodsList))
		defer server.Close()

		address, portStr, err := parseHostAndPort(server.URL)
		if err != nil {
			t.Fatal(err)
		}

		port, _ := strconv.Atoi(portStr)
		cfg := &rest.Config{
			Host:        net.JoinHostPort(address, portStr),
			BearerToken: token,
			TLSClientConfig: rest.TLSClientConfig{
				Insecure: true,
			},
		}

		client, err := NewKubeletStub(address, port, "https", 10*time.Second, cfg)
		if err != nil {
			t.Fatal(err)
		}
		ps, err := client.GetAllPods()
		if err != nil {
			t.Fatal(err)
		}
		t.Logf("podList %+v\n", ps)
	})
}

func Test_kubeletStub_GetKubeletConfiguration(t *testing.T) {
	token = "token"

	server := httptest.NewTLSServer(http.HandlerFunc(mockGetKubeletConfiguration))
	defer server.Close()

	address, portStr, err := parseHostAndPort(server.URL)
	if err != nil {
		t.Fatal(err)
	}

	port, _ := strconv.Atoi(portStr)
	cfg := &rest.Config{
		Host:        net.JoinHostPort(address, portStr),
		BearerToken: token,
		TLSClientConfig: rest.TLSClientConfig{
			Insecure: true,
		},
	}

	client, err := NewKubeletStub(address, port, "https", 10*time.Second, cfg)
	if err != nil {
		t.Fatal(err)
	}
	kubeletConfiguration, err := client.GetKubeletConfiguration()
	if err != nil {
		t.Fatal(err)
	}
	assert.Equal(t, "static", kubeletConfiguration.CPUManagerPolicy)
	expectedEvictionHard := map[string]string{
		"imagefs.available": "15%",
		"memory.available":  "222Mi",
		"nodefs.available":  "10%",
		"nodefs.inodesFree": "5%",
	}
	expectedSystemReserved := map[string]string{
		"cpu":    "200m",
		"memory": "1111Mi",
		"pid":    "1000",
	}
	expectedKubeReserved := map[string]string{
		"cpu":    "200m",
		"memory": "6666Mi",
		"pid":    "1000",
	}
	assert.Equal(t, expectedEvictionHard, kubeletConfiguration.EvictionHard)
	assert.Equal(t, expectedSystemReserved, kubeletConfiguration.SystemReserved)
	assert.Equal(t, expectedKubeReserved, kubeletConfiguration.KubeReserved)
}

func TestNewKubeletStub(t *testing.T) {
	type args struct {
		addr    string
		port    int
		scheme  string
		timeout time.Duration
		config  *rest.Config
	}

	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			name: "127.0.0.1",
			args: args{
				addr:    "127.0.0.1",
				port:    10250,
				scheme:  "https",
				timeout: 10 * time.Second,
				config: &rest.Config{
					BearerToken: token,
					TLSClientConfig: rest.TLSClientConfig{
						Insecure: true,
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := NewKubeletStub(tt.args.addr, tt.args.port, tt.args.scheme, tt.args.timeout, tt.args.config)
			if (err != nil) != tt.wantErr {
				t.Errorf("NewKubeletStub() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			assert.NotNil(t, got)
		})
	}
}
