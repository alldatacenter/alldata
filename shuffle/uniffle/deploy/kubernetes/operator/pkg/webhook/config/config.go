/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package config

import (
	"crypto/tls"
	"flag"
	"fmt"
	"io/ioutil"

	"k8s.io/klog/v2"

	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/constants"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/utils"
	webhookconstants "github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/webhook/constants"
)

const (
	flagIgnoreLastApps       = "ignore-last-apps"
	flagIgnoreRSS            = "ignore-rss"
	flagPort                 = "port"
	flagExternalService      = "external-service"
	flagServerCertFile       = "server-cert-file"
	flagServerPrivateKeyFile = "server-private-key-file"
	flagCACertFile           = "ca-cert-file"
)

// Config contains all configurations.
type Config struct {
	IgnoreLastApps bool
	IgnoreRSS      bool
	HTTPConfig
	utils.GenericConfig
}

// HTTPConfig stores all the http-related configurations.
type HTTPConfig struct {
	Port            int
	ExternalService string
	ServerCertFile  string
	ServerKeyFile   string
	CACertFile      string
}

// AddFlags stores http-related configurations.
func (c *HTTPConfig) AddFlags() {
	flag.IntVar(&c.Port, flagPort, 9876, "Listening port of admission webhook server.")
	flag.StringVar(&c.ExternalService, flagExternalService, webhookconstants.ComponentName,
		"Service name which provides external access to admission webhook server.")
	flag.StringVar(&c.ServerCertFile, flagServerCertFile, "",
		"File containing the default x509 Certificate for HTTPS. (CA cert, if any, concatenated after server cert). "+
			"If HTTPS serving is enabled, and --server-cert-file and --server-private-key-file are not provided, "+
			"a self-signed certificate and key will be generated.")
	flag.StringVar(&c.ServerKeyFile, flagServerPrivateKeyFile, "",
		"File containing the default x509 private key matching --server-cert-file.")
	flag.StringVar(&c.CACertFile, flagCACertFile, "", "File containing the ca certificate.")
}

// NeedLoadCertsFromSecret returns whether we need to load certs from specify secret.
func (c *HTTPConfig) NeedLoadCertsFromSecret() bool {
	return len(c.CACertFile) == 0 || len(c.ServerCertFile) == 0 || len(c.ServerKeyFile) == 0
}

// Addr returns the webhook server's listening address.
func (c *HTTPConfig) Addr() string {
	return fmt.Sprintf(":%v", c.Port)
}

// TLSConfig returns the TLS config.
func (c *HTTPConfig) TLSConfig() *tls.Config {
	cert, err := tls.LoadX509KeyPair(c.ServerCertFile, c.ServerKeyFile)
	if err != nil {
		klog.Fatal(err)
	}
	tlsConfig := &tls.Config{
		Certificates: []tls.Certificate{cert},
	}
	return tlsConfig
}

// GetCaCert return contents of ca cert.
func (c *HTTPConfig) GetCaCert() []byte {
	caCertBody, err := ioutil.ReadFile(c.CACertFile)
	if err != nil {
		klog.Fatalf("read ca cert file %v failed: %v", c.CACertFile, err)
	}
	return caCertBody
}

// LeaderElectionID returns leader election ID.
func (c *Config) LeaderElectionID() string {
	return "rss-webhook-" + constants.LeaderIDSuffix
}

// AddFlags adds all configurations to the global flags.
func (c *Config) AddFlags() {
	flag.BoolVar(&c.IgnoreLastApps, flagIgnoreLastApps, false,
		"Used when debugging, it means we will ignore checking last apps.")
	flag.BoolVar(&c.IgnoreRSS, flagIgnoreRSS, false,
		"Used when debugging, it means we will ignore checking rss objects.")
	c.HTTPConfig.AddFlags()
	c.GenericConfig.AddFlags()
}

// Complete is called before rss-webhook runs.
func (c *Config) Complete() {
	c.GenericConfig.Complete()
}
