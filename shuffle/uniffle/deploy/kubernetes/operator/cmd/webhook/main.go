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

	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/webhook"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/webhook/config"
)

func main() {
	klog.InitFlags(nil)
	cfg := &config.Config{}
	cfg.AddFlags()
	flag.Parse()

	cfg.Complete()
	klog.Infof("run config: %+v", cfg)

	// create an admission webhook manager.
	am := webhook.NewAdmissionManager(cfg)
	// start the admission webhook manager.
	if err := am.Start(cfg.RunCtx); err != nil {
		klog.Fatalf("start admission webhook failed: %v", err)
	}
}
