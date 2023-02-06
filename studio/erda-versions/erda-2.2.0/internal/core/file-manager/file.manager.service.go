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

package file_manager

import (
	"bytes"
	"context"
	"encoding/base64"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"path/filepath"
	"strings"
	"time"

	"github.com/erda-project/erda-infra/providers/httpserver"
	"github.com/erda-project/erda-proto-go/core/services/filemanager/pb"
	"github.com/erda-project/erda/pkg/common/errors"
	httpapi "github.com/erda-project/erda/pkg/common/httpapi"
	perm "github.com/erda-project/erda/pkg/common/permission"
)

type fileManagerService struct {
	p *provider
}

func (s *fileManagerService) ListFiles(ctx context.Context, req *pb.ListFilesRequest) (*pb.ListFilesResponse, error) {
	var command string
	if len(req.Path) > 0 {
		if !strings.HasPrefix(req.Path, "/") {
			return nil, errors.NewInvalidParameterError("path", "must be absolute path")
		}
		command = fmt.Sprintf("cd %q && pwd && TIME_STYLE='+%%Y-%%m-%%dT%%H:%%M:%%S.%%N' ls -la", noExpandPath(req.Path))
	} else {
		command = fmt.Sprintf("cd ~ && pwd && TIME_STYLE='+%%Y-%%m-%%dT%%H:%%M:%%S.%%N' ls -la")
	}
	instance, err := s.getInstanceInfo(ctx, req.ContainerID, req.HostIP)
	if err != nil {
		return nil, err
	}
	stdout := &strings.Builder{}
	err = s.execInPod(ctx, instance.ClusterName, instance.Namespace, instance.PodName, instance.ContainerName,
		[]string{
			"sh", "-c", command,
		},
		strings.NewReader(""),
		stdout,
	)
	if err != nil {
		return nil, err
	}
	dir, err := parseFileList(stdout.String())
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	return &pb.ListFilesResponse{
		Data: dir,
	}, nil
}

func parseFileList(text string) (*pb.FileDirectory, error) {
	dir := &pb.FileDirectory{}
	lines := strings.Split(strings.Trim(text, "\n"), "\n")
	if len(lines) < 2 {
		return nil, fmt.Errorf("invalid command output")
	}
	dir.Directory = lines[0] // output of pwd
	lines = lines[2:]        // skip "total <num>" line
	for _, line := range lines {
		entry, err := parseFileInfo(line)
		if err != nil {
			continue
		}
		dir.Files = append(dir.Files, entry)
	}
	return dir, nil
}

func parseFileInfo(line string) (*pb.FileInfo, error) {
	fi := &pb.FileInfo{}
	var modTime string
	reader := strings.NewReader(line)
	_, err := fmt.Fscanf(reader, "%s %d %s %s %d %s ", &fi.Mode, &fi.HardLinks, &fi.User, &fi.UserGroup, &fi.Size, &modTime)
	if err != nil {
		return nil, err
	}
	// parse filename, filename maybe contains white space " "
	fi.Name = line[len(line)-reader.Len():]
	if strings.HasPrefix(fi.Mode, "l") {
		idx := strings.Index(fi.Name, " -> ")
		if idx > 0 {
			fi.Name = fi.Name[:idx]
		}
	}
	if strings.Contains(fi.Mode, "d") {
		fi.IsDir = true
	}
	// parse modTime
	t, err := time.ParseInLocation("2006-01-02T15:04:05.000000000", modTime, time.Local)
	if err != nil {
		return nil, err
	}
	fi.ModTime = t.UnixNano() / int64(time.Millisecond)
	return fi, nil
}

const maxFileSizeToRead = 10 * 1024 * 1024 // 10 MB

func (s *fileManagerService) ReadFile(ctx context.Context, req *pb.ReadFileRequest) (*pb.ReadFileResponse, error) {
	if !strings.HasPrefix(req.Path, "/") {
		return nil, errors.NewInvalidParameterError("path", "must be absolute path")
	}
	instance, err := s.getInstanceInfo(ctx, req.ContainerID, req.HostIP)
	if err != nil {
		return nil, err
	}

	checkFile := func() (*pb.FileInfo, error) {
		stdout := &strings.Builder{}
		err = s.execInPod(ctx, instance.ClusterName, instance.Namespace, instance.PodName, instance.ContainerName,
			[]string{
				"sh", "-c", fmt.Sprintf("TIME_STYLE='+%%Y-%%m-%%dT%%H:%%M:%%S.%%N' ls -la %q", noExpandPath(req.Path)),
			},
			strings.NewReader(""),
			stdout,
		)
		if err != nil {
			return nil, errors.NewInternalServerError(err)
		}
		content := strings.Trim(stdout.String(), "\n")
		lines := strings.Split(content, "\n")
		if len(lines) <= 0 {
			return nil, errors.NewNotFoundError(req.Path)
		} else if len(lines) > 1 {
			return nil, errors.NewInvalidParameterError("path="+req.Path, "not a regular file")
		}
		fi, err := parseFileInfo(lines[0])
		if err != nil {
			return nil, errors.NewInternalServerError(err)
		}
		if !strings.HasPrefix(fi.Mode, "-") {
			return nil, errors.NewInvalidParameterError("path="+req.Path, "not a regular file")
		}
		if fi.Size > maxFileSizeToRead {
			return nil, errors.NewInvalidParameterError("path="+req.Path, "file is too large")
		}
		return fi, nil
	}
	fi, err := checkFile()
	if err != nil {
		return nil, err
	}

	// read file
	stdout := &bytes.Buffer{}
	err = s.execInPod(ctx, instance.ClusterName, instance.Namespace, instance.PodName, instance.ContainerName,
		[]string{
			"sh", "-c", fmt.Sprintf("dd if=%q", noExpandPath(req.Path)),
		},
		strings.NewReader(""),
		stdout,
	)
	if err != nil {
		return nil, err
	}
	content := stdout.Bytes()
	return &pb.ReadFileResponse{
		Data: &pb.FileData{
			Path:     req.Path,
			Mode:     fi.Mode,
			Encoding: "base64",
			Content:  base64.StdEncoding.EncodeToString(content),
			Size:     int64(len(content)),
		},
	}, nil
}

func (s *fileManagerService) WriteFile(ctx context.Context, req *pb.WriteFileRequest) (*pb.WriteFileResponse, error) {
	if !strings.HasPrefix(req.Path, "/") {
		return nil, errors.NewInvalidParameterError("path", "must be absolute path")
	}
	path := noExpandPath(req.Path)
	var command string
	switch req.Action {
	case "create", "":
		command = fmt.Sprintf("dd of=%q", path)
	case "save":
		command = fmt.Sprintf("[ -f %q ] && dd of=%q", path, path)
	default:
		return nil, errors.NewInvalidParameterError("action", "must be create or save")
	}

	var stdin io.Reader
	if len(req.Content) > 0 {
		switch req.Encoding {
		case "base64":
			byts, err := base64.StdEncoding.DecodeString(req.Content)
			if err != nil {
				return nil, errors.NewInvalidParameterError("content", "invalid base64 content")
			}
			stdin = bytes.NewReader(byts)
		case "":
			stdin = strings.NewReader(req.Content)
		default:
			return nil, errors.NewInvalidParameterError("encoding", "must be base64 or \"\"")
		}
	} else {
		stdin = strings.NewReader("")
	}

	instance, err := s.getInstanceInfo(ctx, req.ContainerID, req.HostIP)
	if err != nil {
		return nil, err
	}
	stdout := &bytes.Buffer{}
	err = s.execInPod(ctx, instance.ClusterName, instance.Namespace, instance.PodName, instance.ContainerName,
		[]string{
			"sh", "-c", command,
		},
		stdin,
		stdout,
	)
	if err != nil {
		if strings.Contains(err.Error(), "exit code 1") {
			return nil, errors.NewNotFoundError(fmt.Sprintf("file %q not exist", req.Path))
		}
		return nil, err
	}
	return &pb.WriteFileResponse{
		Data: stdout.String(),
	}, nil
}

func (s *fileManagerService) MakeDirectory(ctx context.Context, req *pb.MakeDirectoryRequest) (*pb.MakeDirectoryResponse, error) {
	if !strings.HasPrefix(req.Path, "/") {
		return nil, errors.NewInvalidParameterError("path", "must be absolute path")
	}
	var command string
	if req.All {
		command = fmt.Sprintf("mkdir -p %q", noExpandPath(req.Path))
	} else {
		command = fmt.Sprintf("mkdir %q", noExpandPath(req.Path))
	}
	instance, err := s.getInstanceInfo(ctx, req.ContainerID, req.HostIP)
	if err != nil {
		return nil, err
	}
	stdout := &bytes.Buffer{}
	err = s.execInPod(ctx, instance.ClusterName, instance.Namespace, instance.PodName, instance.ContainerName,
		[]string{
			"sh", "-c", command,
		},
		strings.NewReader(""),
		stdout,
	)
	if err != nil {
		return nil, err
	}

	return &pb.MakeDirectoryResponse{
		Data: "OK",
	}, nil
}

func (s *fileManagerService) MoveFile(ctx context.Context, req *pb.MoveFileRequest) (*pb.MoveFileResponse, error) {
	if !strings.HasPrefix(req.Source, "/") || !strings.HasPrefix(req.Destination, "/") {
		return nil, errors.NewInvalidParameterError("path", "must be absolute path")
	}
	instance, err := s.getInstanceInfo(ctx, req.ContainerID, req.HostIP)
	if err != nil {
		return nil, err
	}
	stdout := &bytes.Buffer{}
	err = s.execInPod(ctx, instance.ClusterName, instance.Namespace, instance.PodName, instance.ContainerName,
		[]string{
			"sh", "-c", fmt.Sprintf("mv %q %q", noExpandPath(req.Source), noExpandPath(req.Destination)),
		},
		strings.NewReader(""),
		stdout,
	)
	if err != nil {
		return nil, err
	}
	return &pb.MoveFileResponse{
		Data: "OK",
	}, nil
}

func (s *fileManagerService) DeleteFile(ctx context.Context, req *pb.DeleteFileRequest) (*pb.DeleteFileResponse, error) {
	if !strings.HasPrefix(req.Path, "/") {
		return nil, errors.NewInvalidParameterError("path", "must be absolute path")
	}
	if strings.TrimSpace(req.Path) == "/" {
		return nil, errors.NewInvalidParameterError("path", "can't delete root directory")
	}
	instance, err := s.getInstanceInfo(ctx, req.ContainerID, req.HostIP)
	if err != nil {
		return nil, err
	}
	stdout := &bytes.Buffer{}
	err = s.execInPod(ctx, instance.ClusterName, instance.Namespace, instance.PodName, instance.ContainerName,
		[]string{
			"sh", "-c", fmt.Sprintf("rm -rf %q", noExpandPath(req.Path)),
		},
		strings.NewReader(""),
		stdout,
	)
	if err != nil {
		return nil, err
	}
	return &pb.DeleteFileResponse{
		Data: "OK",
	}, nil
}

func (s *fileManagerService) DownloadFile(rw http.ResponseWriter, req *http.Request, c httpserver.Context, params struct {
	ContainerID string `param:"containerID" validate:"required"`
	HostIP      string `query:"hostIP" validate:"required"`
	Path        string `query:"path"`
}) interface{} {
	if !strings.HasPrefix(params.Path, "/") {
		return httpapi.Errors.InvalidParameter(fmt.Errorf("path must be absolute"))
	}
	ctx := perm.WithPermissionDataContext(req.Context())
	perm.SetPermissionDataFromContext(ctx, instanceKey, c.Attribute(instanceKey))
	instance, err := s.getInstanceInfo(ctx, params.ContainerID, params.HostIP)
	if err != nil {
		return convertErrorToResponse(err)
	}
	filename := filepath.Base(params.Path) + ".tar.gz"
	flusher := rw.(http.Flusher)

	setHeader := true // delay to set header
	var count int
	path := noExpandPath(params.Path)
	err = s.execInPod(ctx, instance.ClusterName, instance.Namespace, instance.PodName, instance.ContainerName,
		[]string{
			"sh", "-c", fmt.Sprintf("([ -f %q ] || [ -d %q ]) && tar -zcf - %q", path, path, path),
		},
		strings.NewReader(""),
		WriterFunc(func(p []byte) (n int, err error) {
			if setHeader {
				setHeader = false
				rw.Header().Set("Cache-Control", "no-cache, no-store, must-revalidate")
				rw.Header().Set("Pragma", "no-cache")
				rw.Header().Set("Expires", "0")
				rw.Header().Set("charset", "utf-8")
				rw.Header().Set("Content-Disposition", "attachment;filename="+url.QueryEscape(filename))
				rw.Header().Set("Content-Type", "application/octet-stream")
			}
			n, err = rw.Write(p)
			if count >= 100 {
				flusher.Flush()
				count = 0
			} else {
				count++
			}
			return n, err
		}),
	)
	if err != nil {
		if strings.Contains(err.Error(), "exit code 1") {
			return httpapi.Errors.NotFound("file or directory not exist", params.Path)
		}
		return convertErrorToResponse(err)
	}
	if count > 0 {
		flusher.Flush()
	}
	return nil
}

func (s *fileManagerService) UploadFile(rw http.ResponseWriter, req *http.Request, c httpserver.Context, params struct {
	ContainerID string `param:"containerID" validate:"required"`
	HostIP      string `query:"hostIP" validate:"required"`
	Path        string `query:"path"`
}) interface{} {
	if !strings.HasPrefix(params.Path, "/") {
		return httpapi.Errors.InvalidParameter(fmt.Errorf("path must be absolute"))
	}
	file, _, err := req.FormFile("file")
	if err != nil {
		return httpapi.Errors.InvalidParameter("file")
	}
	defer file.Close()
	ctx := perm.WithPermissionDataContext(req.Context())
	perm.SetPermissionDataFromContext(ctx, instanceKey, c.Attribute(instanceKey))
	instance, err := s.getInstanceInfo(ctx, params.ContainerID, params.HostIP)
	if err != nil {
		return convertErrorToResponse(err)
	}
	path := noExpandPath(params.Path)
	stdout := &bytes.Buffer{}
	err = s.execInPod(ctx, instance.ClusterName, instance.Namespace, instance.PodName, instance.ContainerName,
		[]string{
			"sh", "-c", fmt.Sprintf("([ ! -f %q ] && [ ! -d %q ]) && dd of=%q", path, path, path),
		},
		file,
		stdout,
	)
	if err != nil {
		if strings.Contains(err.Error(), "exit code 1") {
			return httpapi.Errors.NotFound("file or directory already exists", params.Path)
		}
		return convertErrorToResponse(err)
	}
	return httpapi.Success("OK")
}

func convertErrorToResponse(err error) *httpapi.Response {
	switch e := err.(type) {
	case *errors.InvalidParameterError:
		return httpapi.Errors.InvalidParameter(e)
	case *errors.NotFoundError:
		return httpapi.Errors.NotFound(e)
	case *errors.InternalServerError:
		return httpapi.Errors.Internal(e)
	case *errors.ServiceInvokingError:
		return httpapi.Errors.Internal(e)
	}
	return httpapi.Errors.Internal(err)
}
