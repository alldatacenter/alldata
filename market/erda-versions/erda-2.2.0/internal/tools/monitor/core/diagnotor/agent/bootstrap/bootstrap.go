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

package bootstrap

import (
	"context"
	"log"
	"os"
	"os/exec"
	"os/signal"
	"sync"
	"syscall"

	containerruntime "github.com/erda-project/erda/internal/tools/monitor/core/diagnotor/agent/container-runtime"
)

// Run .
func Run(fn func()) {
	if os.Getenv("DIAGNOTOR_AGENT_RUN_MODE") == "program" {
		if os.Getenv("DIAGNOTOR_AGENT_DISABLE_INIT_NS") != "true" {
			err := initNamespace()
			if err != nil {
				log.Fatalf("failed to initNamespace: %s", err)
				return
			}
		}
		fn()
	} else { // DIAGNOTOR_AGENT_RUN_MODE=bootstrap
		bootstrap()
	}
}

func bootstrap() {
	targetPodUID := os.Getenv("TARGET_POD_UID")
	targetContainerID := os.Getenv("TARGET_CONTAINER_ID")
	if len(targetPodUID) <= 0 || len(targetContainerID) <= 0 {
		log.Fatalf("TARGET_POD_UID or TARGET_CONTAINER_ID is empty")
		return
	}
	targetPID, err := containerruntime.FindPidByPodContainer(targetPodUID, targetContainerID)
	if err != nil {
		log.Printf("failed to find pid by pod + container: %s", err)
		log.Printf("It is possible that the target container has exited")
		return
	}
	runInTargetNamespace(targetPID)
}

func runInTargetNamespace(targetPID string) {
	sc := make(chan os.Signal, 1)
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	args := []string{
		"--target", targetPID, "--uts", "--ipc", "--net", "--pid", // "--user", // nsenter: setns(): can't reassociate to namespace 'user': Invalid argument
	}
	args = append(args, os.Args...)
	cmd := exec.CommandContext(ctx, "nsenter", args...)
	cmd.Env = append(os.Environ(), "DIAGNOTOR_AGENT_RUN_MODE=program")
	cmd.Stdin = os.Stdin
	cmd.Stderr = os.Stderr
	cmd.Stdout = os.Stdout
	var wg sync.WaitGroup
	wg.Add(1)
	go func() {
		defer wg.Done()
		err := cmd.Run()
		if err != nil {
			log.Fatalf("failed to run program with namespaces: %s", err)
		}
		select {
		case sc <- syscall.SIGQUIT:
		default:
		}
	}()

	log.Println("wait to exit ...")
	signal.Notify(sc, syscall.SIGHUP, syscall.SIGINT, syscall.SIGTERM, syscall.SIGQUIT)
	<-sc

	cmd.Process.Kill()
	wg.Wait()
}

func initNamespace() error {
	cmd := exec.Command("mount", "-t", "proc", "/proc", "/proc")
	cmd.Stdin = os.Stdin
	cmd.Stderr = os.Stderr
	cmd.Stdout = os.Stdout
	err := cmd.Run()
	listProcesses()
	return err
}

func listProcesses() {
	cmd := exec.Command("ps", "-ef")
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	err := cmd.Run()
	if err != nil {
		log.Printf("[ERROR] failed to ps -ef: %s", err)
	}
}
