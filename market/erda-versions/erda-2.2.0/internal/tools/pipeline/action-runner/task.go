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

package actionrunner

import (
	"fmt"
	"io"
	"net/http"
	"os"
	"os/exec"
	"path"
	"strings"
	"time"

	"github.com/erda-project/erda/pkg/retry"
	"github.com/erda-project/erda/pkg/template"
)

// Task .
type Task struct {
	ID          int      `json:"id"`
	JobID       string   `json:"job_id"`
	Status      string   `json:"status"`
	DownloadURL string   `json:"context_data_url"`
	Token       string   `json:"openapi_token"`
	Workdir     string   `json:"workdir"`
	Commands    []string `json:"commands"`
	Targets     []string `json:"targets"`
}

type worker struct {
	*Task
	r           *Runner
	conf        *Conf
	contextPath string
	log         Logger
	closeCh     chan bool
	cancelCh    chan bool
}

func (w *worker) Execute() (err error) {
	w.closeCh = make(chan bool)
	w.cancelCh = make(chan bool)
	go w.checkTask()
	var url string
	defer func() {
		if err != nil {
			w.log.Error(err)
			err := w.taskResultCallback(w.ID, "failed", url)
			if err != nil {
				w.log.Error(err)
			}
		} else {
			w.log.Infof("execute task success: id: %d, job id: %s", w.ID, w.JobID)
			err := w.taskResultCallback(w.ID, "success", url)
			if err != nil {
				w.log.Error(err)
			}
		}
		w.log.Flush()
		if err != nil {
			err := os.Rename(w.contextPath, w.contextPath+"_failed")
			if err != nil {
				w.log.Error("fail to rename context dir: %s", err)
			}
		} else {
			err = os.RemoveAll(w.contextPath)
			if err != nil {
				w.log.Errorf("fail to remove context dir: %s", err)
			}
		}
	}()
	os.RemoveAll(w.contextPath)
	err = os.MkdirAll(w.contextPath, os.ModePerm)
	if err != nil {
		return err
	}

	// retry context download
	err = retry.DoWithInterval(
		func() error {
			return w.downloadContext()
		}, 3, time.Second*5,
	)
	if err != nil {
		return err
	}

	workdir := path.Join(w.contextPath, w.Workdir)
	err = os.Chdir(workdir)
	if err != nil {
		return err
	}
	err = w.runCommands(workdir)
	if err != nil {
		return err
	}

	// retry upload target
	err = retry.DoWithInterval(
		func() error {
			url, err = w.uploadTargets(workdir)
			return err
		}, 3, time.Second*5,
	)

	return err
}

func (w *worker) downloadContext() error {
	res, err := http.Get(w.DownloadURL)
	if err != nil {
		return fmt.Errorf("fail to download %s: %s", w.DownloadURL, err)
	}
	file := path.Join(os.TempDir(), w.JobID+".tar")
	f, err := os.Create(file)
	if err != nil {
		return fmt.Errorf("fail to create download file %s: %s", w.JobID+".tar", err)
	}
	defer func() {
		err := f.Close()
		if err != nil {
			w.log.Error(err)
		}
		err = os.Remove(file)
		if err != nil {
			w.log.Error(err)
		}
	}()
	w.log.Infof("downloading %s", file)
	_, err = io.Copy(f, res.Body)
	if err != nil {
		return err
	}
	w.log.Infof("download %s", file)

	return w.unTar(file, w.contextPath)
}

func (w *worker) runCommands(workdir string) error {
	for _, cmdTemplate := range w.Commands {
		cmdStr := template.Render(cmdTemplate, w.conf.Params)
		ch := make(chan error)
		cmd := exec.Command("/bin/bash", "-c", cmdStr)
		go func() {
			cmd.Stdout = w.log.Stdout()
			cmd.Stderr = w.log.Stderr()
			if err := cmd.Run(); err != nil {
				ch <- err
				return
			}
			ch <- nil
		}()
		select {
		case err := <-ch:
			close(ch)
			if err != nil {
				return err
			}
		case <-w.cancelCh:
			if cmd.Process != nil {
				cmd.Process.Kill()
			}
			return fmt.Errorf("cmd `%s` canceled", cmd)
		}
	}
	return nil
}

func (w *worker) uploadTargets(workdir string) (string, error) {
	err := os.Chdir(workdir)
	if err != nil {
		return "", err
	}
	resultDir := path.Join(w.contextPath, "__action_result")
	err = os.MkdirAll(resultDir, os.ModePerm)
	if err != nil {
		return "", err
	}
	for _, target := range w.Targets {
		err := w.move(target, resultDir)
		if err != nil {
			return "", err
		}
	}
	resultFile := path.Join(w.contextPath, "result.tar")
	err = os.Chdir(resultDir)
	if err != nil {
		return "", err
	}
	w.log.Infof("upload: %s -> %v", resultFile, w.Targets)
	err = w.tar(resultFile, w.Targets...)
	if err != nil {
		return "", err
	}
	return w.uploadFile(resultFile, w.Token)
}

func (w *worker) unTar(tarAbsPath string, output string) error {
	err := os.MkdirAll(output, os.ModePerm)
	if err != nil {
		return fmt.Errorf("fail to create dir: %s", err)
	}
	cmd := exec.Command("tar", "-xf", tarAbsPath, "-C", output)
	cmd.Stdout = w.log.Stdout()
	cmd.Stderr = w.log.Stderr()
	return cmd.Run()
}

func (w *worker) tar(output string, sources ...string) error {
	args := append([]string{"-cf", output}, sources...)
	cmd := exec.Command("tar", args...)
	cmd.Stdout = w.log.Stdout()
	cmd.Stderr = w.log.Stderr()
	err := cmd.Run()
	return err
}

func (w *worker) move(src, dir string) error {
	if strings.Contains(src, "/") {
		dir := path.Join(dir, path.Dir(src))
		err := os.MkdirAll(dir, os.ModePerm)
		if err != nil {
			return err
		}
	}
	return os.Rename(src, path.Join(dir, src))
}

func (w *worker) checkTask() {
	tick := time.Tick(3 * time.Second)
	for {
		select {
		case <-tick:
			ok, err := w.getTaskStatus(w.ID, w.Token)
			if err != nil {
				w.log.Error(err)
				continue
			}
			if !ok {
				close(w.cancelCh)
				return
			}
		case <-w.closeCh:
			return
		}
	}
}

func (w *worker) Close() error {
	return nil
}
