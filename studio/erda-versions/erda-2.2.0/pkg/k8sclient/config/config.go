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

package config

import (
	"encoding/base64"
	"fmt"

	"k8s.io/client-go/kubernetes/scheme"
	"k8s.io/client-go/rest"
	"k8s.io/client-go/util/flowcontrol"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/clusterdialer"
)

// ParseManageConfig parse manage config to rest.Config
func ParseManageConfig(clusterName string, c *apistructs.ManageConfig) (*rest.Config, error) {
	if c == nil {
		return nil, fmt.Errorf("empty manage config")
	}

	switch c.Type {
	case apistructs.ManageToken, apistructs.ManageCert:
		return GetRestConfig(c)
	case apistructs.ManageProxy:
		fallthrough
	default:
		return GetDialerRestConfig(clusterName, c)
	}
}

// GetDialerRestConfig get cluster dialer rest.Config
func GetDialerRestConfig(clusterName string, c *apistructs.ManageConfig) (*rest.Config, error) {
	if c.Address == "" {
		return nil, fmt.Errorf("proxy must spcified address")
	}

	rc, err := GetRestConfig(c)
	if err != nil {
		return nil, err
	}

	rc.TLSClientConfig.NextProtos = []string{"http/1.1"}
	rc.UserAgent = rest.DefaultKubernetesUserAgent() + " cluster " + clusterName
	rc.Dial = clusterdialer.DialContext(clusterName)

	return rc, nil
}

// GetRestConfig get rest.Config with manage config
func GetRestConfig(c *apistructs.ManageConfig) (*rest.Config, error) {
	// If not provide api-server address, use in-cluster rest config
	if c.Address == "" {
		return rest.InClusterConfig()
	}

	rc := &rest.Config{
		Host:    c.Address,
		APIPath: "/apis",
		QPS:     1000,
		Burst:   100,
		ContentConfig: rest.ContentConfig{
			NegotiatedSerializer: scheme.Codecs.WithoutConversion(),
		},
		TLSClientConfig: rest.TLSClientConfig{},
		UserAgent:       rest.DefaultKubernetesUserAgent(),
		RateLimiter:     flowcontrol.NewTokenBucketRateLimiter(1000, 100),
	}

	// If ca data is empty, the certificate is not validated
	if c.CaData == "" {
		rc.TLSClientConfig.Insecure = true
	} else {
		caBytes, err := base64.StdEncoding.DecodeString(c.CaData)
		if err != nil {
			return nil, err
		}

		rc.TLSClientConfig.CAData = caBytes
	}

	// If token is not empty, use token firstly, else use certificate
	if c.Token != "" {
		rc.BearerToken = c.Token
	} else {
		if c.CertData == "" || c.KeyData == "" {
			return nil, fmt.Errorf("must provide available cert data and key data")
		}

		certBytes, err := base64.StdEncoding.DecodeString(c.CertData)
		if err != nil {
			return nil, err
		}

		keyBytes, err := base64.StdEncoding.DecodeString(c.KeyData)
		if err != nil {
			return nil, err
		}

		rc.TLSClientConfig.CertData = certBytes
		rc.TLSClientConfig.KeyData = keyBytes
	}

	return rc, nil
}

// GetInClusterRestConfig get in cluster rest config
func GetInClusterRestConfig() (*rest.Config, error) {
	rc, err := rest.InClusterConfig()
	if err != nil {
		return nil, err
	}

	rc.QPS = 1000
	rc.Burst = 100

	return rc, nil
}
