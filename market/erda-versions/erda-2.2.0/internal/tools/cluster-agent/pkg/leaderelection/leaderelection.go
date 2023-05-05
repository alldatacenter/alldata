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

package leaderelection

import (
	"context"
	"fmt"
	"io/ioutil"
	"os"
	"strings"
	"time"

	"github.com/google/uuid"
	"github.com/pkg/errors"
	coordinationv1client "k8s.io/client-go/kubernetes/typed/coordination/v1"
	corev1client "k8s.io/client-go/kubernetes/typed/core/v1"
	"k8s.io/client-go/rest"
	"k8s.io/client-go/tools/leaderelection"
	"k8s.io/client-go/tools/leaderelection/resourcelock"
)

type Options struct {
	Identity                   string
	LeaderElectionResourceLock string
	LeaderElectionNamespace    string
	LeaderElectionID           string

	LeaseDuration time.Duration
	RenewDeadline time.Duration
	RetryPeriod   time.Duration

	EventRecorder resourcelock.EventRecorder

	OnNewLeaderFun   func(identity string)
	OnStartedLeading func(ctx context.Context)
	OnStoppedLeading func()
}

// Start leader election loop
func Start(ctx context.Context, rc *rest.Config, options Options) error {
	rl, err := NewResourceLock(rc, options)
	if err != nil {
		return err
	}

	leaderelection.RunOrDie(ctx, leaderelection.LeaderElectionConfig{
		Lock:          rl,
		LeaseDuration: options.LeaseDuration,
		RenewDeadline: options.RenewDeadline,
		RetryPeriod:   options.RetryPeriod,
		Callbacks: leaderelection.LeaderCallbacks{
			OnStartedLeading: options.OnStartedLeading,
			OnStoppedLeading: options.OnStoppedLeading,
			OnNewLeader:      options.OnNewLeaderFun,
		},
		ReleaseOnCancel: true,
	})

	return nil
}

func NewResourceLock(rc *rest.Config, options Options) (resourcelock.Interface, error) {
	if rc == nil {
		return nil, errors.New("rest.Config is nil")
	} else {
		rest.AddUserAgent(rc, "leader-election")
	}

	if options.LeaderElectionID == "" {
		return nil, errors.New("leader election ID is required")
	}

	if options.LeaderElectionResourceLock == "" {
		options.LeaderElectionResourceLock = resourcelock.LeasesResourceLock
	}

	if options.LeaderElectionNamespace == "" {
		var err error
		options.LeaderElectionNamespace, err = getNamespace()
		if err != nil {
			return nil, err
		}
	}

	if options.Identity == "" {
		var err error
		options.Identity, err = GenIdentity()
		if err != nil {
			return nil, err
		}
	}

	corev1Client, err := corev1client.NewForConfig(rc)
	if err != nil {
		return nil, err
	}
	coordinationClient, err := coordinationv1client.NewForConfig(rc)
	if err != nil {
		return nil, err
	}

	rlc := resourcelock.ResourceLockConfig{
		Identity: options.Identity,
	}

	if options.EventRecorder != nil {
		rlc.EventRecorder = options.EventRecorder
	}

	return resourcelock.New(
		options.LeaderElectionResourceLock,
		options.LeaderElectionNamespace,
		options.LeaderElectionID,
		corev1Client, coordinationClient, rlc)
}

func getNamespace() (string, error) {
	if ns := os.Getenv("POD_NAMESPACE"); ns != "" {
		return ns, nil
	}

	if data, err := ioutil.ReadFile("/var/run/secrets/kubernetes.io/serviceaccount/namespace"); err == nil {
		if ns := strings.TrimSpace(string(data)); len(ns) > 0 {
			return ns, nil
		}
	}
	return "", errors.New("unable to determine namespace")
}

func GenIdentity() (string, error) {
	hostname, err := os.Hostname()
	if err != nil {
		return "", err
	}
	return fmt.Sprintf("%s_%s", hostname, uuid.New().String()), nil
}
