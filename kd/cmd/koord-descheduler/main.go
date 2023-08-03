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

package main

import (
	"fmt"
	"math/rand"
	"os"
	"time"

	"github.com/spf13/pflag"
	cliflag "k8s.io/component-base/cli/flag"
	"k8s.io/component-base/logs"

	"github.com/koordinator-sh/koordinator/cmd/koord-descheduler/app"
)

func main() {
	if err := runDeschedulerCmd(); err != nil {
		fmt.Fprintf(os.Stderr, "%v\n", err)
		os.Exit(1)
	}
}

func runDeschedulerCmd() error {
	rand.Seed(time.Now().UnixNano())

	pflag.CommandLine.SetNormalizeFunc(cliflag.WordSepNormalizeFunc)

	command := app.NewDeschedulerCommand()

	logs.InitLogs()
	defer logs.FlushLogs()

	err := command.ParseFlags(os.Args[1:])
	if err != nil {
		// when fail to parse flags, return error with the usage message.
		return fmt.Errorf("%v\n%s", err, command.UsageString())
	}

	return command.Execute()
}
