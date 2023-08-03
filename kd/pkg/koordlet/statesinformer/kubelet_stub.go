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
	"fmt"
	"io"
	"net"
	"net/http"
	"net/url"
	"strconv"
	"time"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/client-go/rest"
	kubeletconfigv1beta1 "k8s.io/kubelet/config/v1beta1"
	"k8s.io/kubernetes/cmd/kubelet/app/options"
	kubeletconfiginternal "k8s.io/kubernetes/pkg/kubelet/apis/config"
	kubeletscheme "k8s.io/kubernetes/pkg/kubelet/apis/config/scheme"
)

type KubeletStub interface {
	GetAllPods() (corev1.PodList, error)
	GetKubeletConfiguration() (*kubeletconfiginternal.KubeletConfiguration, error)
}

type kubeletStub struct {
	addr       string
	port       int
	scheme     string
	httpClient *http.Client
}

func NewKubeletStub(addr string, port int, scheme string, timeout time.Duration, cfg *rest.Config) (KubeletStub, error) {
	client := &http.Client{
		Timeout: timeout,
	}
	if cfg != nil && rest.IsConfigTransportTLS(*cfg) {
		transport, err := rest.TransportFor(cfg)
		if err != nil {
			return nil, err
		}
		client.Transport = transport
	}

	return &kubeletStub{
		httpClient: client,
		addr:       addr,
		port:       port,
		scheme:     scheme,
	}, nil
}

func (k *kubeletStub) GetAllPods() (corev1.PodList, error) {
	url := url.URL{
		Scheme: k.scheme,
		Host:   net.JoinHostPort(k.addr, strconv.Itoa(k.port)),
		Path:   "/pods/",
	}
	podList := corev1.PodList{}
	rsp, err := k.httpClient.Get(url.String())
	if err != nil {
		return podList, err
	}
	defer rsp.Body.Close()
	if rsp.StatusCode != http.StatusOK {
		return podList, fmt.Errorf("request %s failed, code %d", url.String(), rsp.StatusCode)
	}

	body, err := io.ReadAll(rsp.Body)
	if err != nil {
		return podList, err
	}

	// parse json data
	err = json.Unmarshal(body, &podList)
	if err != nil {
		return podList, fmt.Errorf("parse kubelet pod list failed, err: %v", err)
	}
	return podList, nil
}

type kubeletConfigz struct {
	ComponentConfig kubeletconfigv1beta1.KubeletConfiguration `json:"kubeletconfig"`
}

func (k *kubeletStub) GetKubeletConfiguration() (*kubeletconfiginternal.KubeletConfiguration, error) {
	configzURL := url.URL{
		Scheme: k.scheme,
		Host:   net.JoinHostPort(k.addr, strconv.Itoa(k.port)),
		Path:   "/configz",
	}
	rsp, err := k.httpClient.Get(configzURL.String())
	if err != nil {
		return nil, err
	}
	defer rsp.Body.Close()

	if rsp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("request %s failed, code %d", configzURL.String(), rsp.StatusCode)
	}

	body, err := io.ReadAll(rsp.Body)
	if err != nil {
		return nil, err
	}

	var configz kubeletConfigz
	if err = json.Unmarshal(body, &configz); err != nil {
		return nil, fmt.Errorf("failed to unmarshal kubeletConfigz: %v", err)
	}

	kubeletConfiguration, err := options.NewKubeletConfiguration()
	if err != nil {
		return nil, err
	}

	scheme, _, err := kubeletscheme.NewSchemeAndCodecs()
	if err != nil {
		return nil, err
	}
	if err = scheme.Convert(&configz.ComponentConfig, kubeletConfiguration, nil); err != nil {
		return nil, err
	}
	return kubeletConfiguration, nil
}
