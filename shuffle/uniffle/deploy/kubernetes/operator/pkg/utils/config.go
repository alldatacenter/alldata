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

package utils

import (
	"context"
	"flag"

	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
	"k8s.io/client-go/tools/clientcmd"
	"k8s.io/klog/v2"
	"sigs.k8s.io/controller-runtime/pkg/manager/signals"

	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/generated/clientset/versioned"
)

const (
	flagKubeConfig = "kubeconfig"
)

// GenericConfig stores the basic configuration of admission webhook server.
type GenericConfig struct {
	KubeConfig string
	RESTConfig *rest.Config
	KubeClient kubernetes.Interface
	RSSClient  versioned.Interface
	RunCtx     context.Context
}

// AddFlags adds all configurations to the global flags.
func (c *GenericConfig) AddFlags() {
	if flag.Lookup(flagKubeConfig) == nil {
		flag.StringVar(&c.KubeConfig, flagKubeConfig, "",
			"Paths to a kubeconfig. Only required if out-of-cluster.")
	}
}

// Complete is called before the component runs.
func (c *GenericConfig) Complete() {
	c.KubeConfig = flag.Lookup(flagKubeConfig).Value.String()

	restConfig, err := clientcmd.BuildConfigFromFlags("", c.KubeConfig)
	if err != nil {
		klog.Fatalf("create *rest.Config failed: %v", err)
	}
	c.RESTConfig = restConfig

	c.KubeClient, err = kubernetes.NewForConfig(restConfig)
	if err != nil {
		klog.Fatalf("create kubeClient failed: %v", err)
	}

	c.RSSClient, err = versioned.NewForConfig(restConfig)
	if err != nil {
		klog.Fatalf("create rssClient failed: %v", err)
	}

	c.RunCtx = signals.SetupSignalHandler()
}
