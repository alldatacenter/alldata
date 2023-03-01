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

package main

import (
	"flag"

	"k8s.io/klog/v2"
	ctrl "sigs.k8s.io/controller-runtime"

	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/controller/config"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/controller/controller"
)

func main() {
	klog.InitFlags(nil)
	cfg := &config.Config{}
	cfg.AddFlags()
	flag.Parse()

	cfg.Complete()
	klog.Infof("run config: %+v", cfg)

	// create a manager for leader election.
	mgr, err := ctrl.NewManager(cfg.RESTConfig, ctrl.Options{
		LeaderElection:   true,
		LeaderElectionID: cfg.LeaderElectionID(),
	})
	if err != nil {
		klog.Fatal(err)
	}
	// create a rss controller.
	rc := controller.NewRSSController(cfg)
	if err = mgr.Add(rc); err != nil {
		klog.Fatal(err)
	}
	// start the rss controller.
	if err = mgr.Start(cfg.RunCtx); err != nil {
		klog.Fatal(err)
	}
}
