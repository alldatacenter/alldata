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

package utils

import (
	"bufio"
	"bytes"
	"context"
	"fmt"
	"io"
	"io/ioutil"
	"net/url"
	"os"
	"os/exec"
	"regexp"
	"strings"
	"sync"
	"time"

	"github.com/pkg/errors"
)

var InvalidErdaRepo = errors.New("Invalid Erda git repository!")

func GetWorkspaceBranch(dir string) (string, error) {
	branchCmd := exec.Command("git", "rev-parse", "--abbrev-ref", "HEAD")
	branchCmd.Dir = dir
	out, err := branchCmd.CombinedOutput()
	if err != nil {
		return "", errors.WithMessage(err, strings.TrimSpace(string(out)))
	}
	re := regexp.MustCompile(`\r?\n`)
	branch := re.ReplaceAllString(string(out), "")

	return branch, nil
}

func GetOriginRepo() string {
	return GetRepo("origin")
}

func GetRepo(remote string) string {
	out, _ := exec.Command("git", "config", "--get", "remote."+remote+".url").CombinedOutput()
	return string(out)
}

func IsWorkspaceDirty(dir string) (bool, error) {
	statusCmd := exec.Command("git", "status", "-s")
	statusCmd.Dir = dir
	wcCmd := exec.Command("wc", "-l")

	rs, err := PipeCmds(statusCmd, wcCmd)
	if err != nil {
		return true, errors.WithMessage(err, strings.TrimSpace(rs))
	}
	return strings.TrimSpace(rs) != "0", nil
}

func GetWorkspacePipelines(dir string) ([]string, error) {
	var ymls []string
	fs, err := ioutil.ReadDir(dir)
	if err != nil && !os.IsNotExist(err) {
		return nil, err
	}

	for _, path := range fs {
		if !path.IsDir() && strings.HasSuffix(path.Name(), ".yml") {
			ymls = append(ymls, path.Name())
		}
	}

	return ymls, nil
}

func IsDir(path string) bool {
	s, err := os.Stat(path)
	if err != nil {
		return false
	}
	return s.IsDir()
}

func GetWorkspaceInfo(dir, remoteName string) (GitterURLInfo, error) {
	remoteCmd := exec.Command("git", "remote", "get-url", remoteName)
	remoteCmd.Dir = dir
	out, err := remoteCmd.CombinedOutput()
	if err != nil {
		return GitterURLInfo{}, errors.WithMessage(err, strings.TrimSpace(string(out)))
	}

	re := regexp.MustCompile(`\r?\n`)
	newStr := re.ReplaceAllString(string(out), "")

	u, err := url.Parse(newStr)
	if err != nil {
		return GitterURLInfo{}, err
	}
	t, paths, err := ClassifyURL(u.Path)
	if err != nil || t != GittarURL {
		return GitterURLInfo{}, InvalidErdaRepo
	}

	// /<org>/dop/<project>/<app>
	return GitterURLInfo{
		OrganizationURLInfo{u.Scheme, u.Host, paths[1]}, paths[3], paths[4],
	}, nil
}

func InputPWD(prompt string) string {
	cmd := exec.Command("stty", "-echo")
	cmd.Stdin = os.Stdin
	if err := cmd.Run(); err != nil {
		panic(err)
	}
	defer func() {
		cmd := exec.Command("stty", "echo")
		cmd.Stdin = os.Stdin
		if err := cmd.Run(); err != nil {
			panic(err)
		}
		fmt.Println("")
	}()
	return InputNormal(prompt)
}

func InputNormal(prompt string) string {
	fmt.Printf(prompt)
	r := bufio.NewReader(os.Stdin)
	input, err := r.ReadString('\n')
	if err != nil {
		panic(err)
	}
	return input[:len(input)-1]
}

func InputAndChoose(prompt, yes, no string) string {
	var ans string
	for {
		ans = strings.ToUpper(InputNormal(fmt.Sprintf("%s[%s/%s]", prompt, yes, no)))
		if ans == yes || ans == no {
			break
		}
	}
	return ans
}

type pagingList func(pageNo, pageSize int) (bool, error)

func PagingView(p pagingList, choose string, pageSize int, interactive bool) error {
	pageNo := 1
	num := 0
	for {
		more, err := p(pageNo, pageSize)
		if err != nil {
			return err
		}
		num += pageSize
		if more {
			if interactive {
				ans := InputAndChoose(choose, "Y", "N")
				if ans == "Y" {
					pageNo += 1
				} else {
					break
				}
			} else {
				pageNo += 1
			}
		} else {
			break
		}
	}

	return nil
}

func PagingAll(p pagingList, pageSize int) error {
	pageNo := 1
	for {
		more, err := p(pageNo, pageSize)
		if err != nil {
			return err
		}
		if more {
			pageNo += 1
		} else {
			break
		}
	}
	return nil
}

type TaskRunner func() bool

func DoTaskListWithTimeout(timeout time.Duration, rs []TaskRunner) error {
	wg := sync.WaitGroup{}
	timeoutCtx, _ := context.WithTimeout(context.Background(), timeout)

	for _, r := range rs {
		wg.Add(1)
		go func(r TaskRunner) {
			defer wg.Done()
			timeTicker := time.NewTicker(2 * time.Second)
			for {
				select {
				case <-timeTicker.C:
					if r() {
						return
					}
				case <-timeoutCtx.Done():
					return
				}
			}
		}(r)
	}
	wg.Wait()
	if timeoutCtx.Err() != nil {
		return timeoutCtx.Err()
	}
	return nil
}

type TaskRunnerE func() (bool, error)

func DoTaskWithTimeout(c TaskRunnerE, timeout time.Duration) error {
	wg := sync.WaitGroup{}
	timeoutCtx, _ := context.WithTimeout(context.Background(), timeout)
	wg.Add(1)

	var err error
	go func() {
		defer wg.Done()
		timeTicker := time.NewTicker(2 * time.Second)
		for {
			select {
			case <-timeTicker.C:
				var rs bool
				rs, err = c()
				if err != nil {
					return
				}
				if rs {
					return
				}
			case <-timeoutCtx.Done():
				return
			}
		}
	}()
	wg.Wait()

	if err != nil {
		return err
	}
	if timeoutCtx.Err() != nil {
		return timeoutCtx.Err()
	}

	return nil
}

func PipeCmds(cur, next *exec.Cmd) (string, error) {
	var buf bytes.Buffer

	r, w := io.Pipe()
	cur.Stdout = w
	next.Stdin = r
	next.Stdout = &buf

	err := cur.Start()
	if err != nil {
		return "", err
	}
	err = next.Start()
	if err != nil {
		return "", err
	}

	err = cur.Wait()
	if err != nil {
		return "", err
	}
	err = w.Close()
	if err != nil {
		return "", err
	}
	err = next.Wait()
	if err != nil {
		return "", err
	}

	return buf.String(), nil
}
