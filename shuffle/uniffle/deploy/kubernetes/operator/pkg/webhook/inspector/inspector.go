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

package inspector

import (
	"context"
	"crypto/tls"
	"fmt"
	"net/http"

	"k8s.io/client-go/informers"
	"k8s.io/client-go/kubernetes"
	corelisters "k8s.io/client-go/listers/core/v1"
	"k8s.io/client-go/tools/cache"
	"k8s.io/klog/v2"
	"sigs.k8s.io/controller-runtime/pkg/manager"

	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/generated/clientset/versioned"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/generated/informers/externalversions"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/generated/listers/uniffle/v1alpha1"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/utils"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/webhook/config"
	webhookconstants "github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/webhook/constants"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/webhook/util"
)

var _ Inspector = &inspector{}

// Inspector intercepts the request and checks whether the pod can be deleted.
type Inspector interface {
	manager.Runnable
}

// NewInspector creates an Inspector.
func NewInspector(cfg *config.Config, tlsConfig *tls.Config) Inspector {
	return newInspector(cfg, tlsConfig)
}

// newInspector creates an inspector.
func newInspector(cfg *config.Config, tlsConfig *tls.Config) *inspector {
	rssInformerFactory := externalversions.NewSharedInformerFactory(cfg.RSSClient, 0)
	cmInformerFactory := utils.BuildCoordinatorInformerFactory(cfg.KubeClient)
	i := &inspector{
		ignoreLastApps:     cfg.IgnoreLastApps,
		ignoreRSS:          cfg.IgnoreRSS,
		tlsConfig:          tlsConfig,
		kubeClient:         cfg.KubeClient,
		rssClient:          cfg.RSSClient,
		rssInformerFactory: rssInformerFactory,
		rssInformer:        rssInformerFactory.Uniffle().V1alpha1().RemoteShuffleServices().Informer(),
		rssLister:          rssInformerFactory.Uniffle().V1alpha1().RemoteShuffleServices().Lister(),
		cmInformerFactory:  cmInformerFactory,
		cmInformer:         cmInformerFactory.Core().V1().ConfigMaps().Informer(),
		cmLister:           cmInformerFactory.Core().V1().ConfigMaps().Lister(),
	}

	// register handler functions for admission webhook server.
	mux := http.NewServeMux()
	mux.HandleFunc(webhookconstants.ValidatingPodPath,
		util.WithAdmissionReviewHandler(i.validateDeletingShuffleServer))
	mux.HandleFunc(webhookconstants.ValidatingRssPath,
		util.WithAdmissionReviewHandler(i.validateRSS))
	mux.HandleFunc(webhookconstants.MutatingRssPath,
		util.WithAdmissionReviewHandler(i.mutateRSS))
	i.server = &http.Server{
		Addr:      cfg.Addr(),
		Handler:   mux,
		TLSConfig: tlsConfig,
	}

	return i
}

// inspector implements the Inspector interface.
type inspector struct {
	ignoreLastApps     bool
	ignoreRSS          bool
	tlsConfig          *tls.Config
	server             *http.Server
	kubeClient         kubernetes.Interface
	rssClient          versioned.Interface
	rssInformerFactory externalversions.SharedInformerFactory
	rssInformer        cache.SharedIndexInformer
	rssLister          v1alpha1.RemoteShuffleServiceLister
	cmInformerFactory  informers.SharedInformerFactory
	cmInformer         cache.SharedIndexInformer
	cmLister           corelisters.ConfigMapLister
}

// Start starts the Inspector.
func (i *inspector) Start(ctx context.Context) error {
	i.rssInformerFactory.Start(ctx.Done())
	i.cmInformerFactory.Start(ctx.Done())
	if !cache.WaitForCacheSync(ctx.Done(), i.rssInformer.HasSynced, i.cmInformer.HasSynced) {
		return fmt.Errorf("wait for cache synced failed")
	}
	klog.V(2).Info("inspector started")
	// set up the http server for listening pods and rss objects' validating and mutating requests.
	if err := i.server.ListenAndServeTLS("", ""); err != nil {
		return fmt.Errorf("listen error: %v", err)
	}
	return nil
}
