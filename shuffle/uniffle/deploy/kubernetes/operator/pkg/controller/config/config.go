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
	"flag"

	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/constants"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/utils"
)

const (
	flagWorkers = "workers"
)

// Config contains all configurations.
type Config struct {
	Workers int
	utils.GenericConfig
}

// LeaderElectionID returns leader election ID.
func (c *Config) LeaderElectionID() string {
	return "rss-controller-" + constants.LeaderIDSuffix
}

// AddFlags adds all configurations to the global flags.
func (c *Config) AddFlags() {
	flag.IntVar(&c.Workers, flagWorkers, 1, "Concurrency of the rss controller.")
	c.GenericConfig.AddFlags()
}

// Complete is called before rss-controller runs.
func (c *Config) Complete() {
	c.GenericConfig.Complete()
}
