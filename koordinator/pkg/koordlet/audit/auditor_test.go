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

package audit

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"
)

func makeRequestUrl(size int, serverUrl, pageToken string) string {
	url := fmt.Sprintf("%s?size=%d", serverUrl, size)
	if pageToken != "" {
		url += fmt.Sprintf("&pageToken=%s", pageToken)
	}
	return url
}

func TestAuditorLogger(t *testing.T) {
	tempDir := t.TempDir()

	c := NewDefaultConfig()
	c.LogDir = tempDir
	auditor := NewAuditor(c)
	logger := auditor.LoggerWriter()
	blocks := make([][]byte, 26)
	for i := 0; i < len(blocks); i++ {
		blocks[i] = makeBlock(63, 'a'+byte(i%26))
		logger.V(0).Node().Message(string(blocks[i])).Do()
	}
	logger.Flush()

	server := httptest.NewServer(http.HandlerFunc(auditor.HttpHandler()))
	defer server.Close()

	client := http.Client{}
	req, _ := http.NewRequest("GET", makeRequestUrl(10, server.URL, ""), nil)
	req.Header.Add("Accept", "application/json")
	resp, err := client.Do(req)

	if err != nil {
		t.Fatalf("failed to get events: %v", err)
	}

	defer resp.Body.Close()
	body, err := io.ReadAll(resp.Body)
	response := &JsonResponse{}
	if err := json.Unmarshal(body, response); err != nil {
		t.Fatal(err)
	}

	if len(response.Events) != 10 {
		t.Errorf("failed to load events, expected %d actual %d", 10, len(response.Events))
	}

	// continue read logs
	req, _ = http.NewRequest("GET", makeRequestUrl(1, server.URL, response.NextPageToken), nil)
	req.Header.Add("Accept", "application/json")
	resp, err = client.Do(req)
	if err != nil {
		t.Fatalf("failed to get events: %v", err)
	}
	defer resp.Body.Close()
	body, err = io.ReadAll(resp.Body)
	response = &JsonResponse{}
	if err := json.Unmarshal(body, response); err != nil {
		t.Fatal(err)
	}
	if len(response.Events) != 1 {
		t.Errorf("failed to load events, expected %d actual %d", 1, len(response.Events))
	}
	if !bytes.Equal(blocks[len(blocks)-11], []byte(response.Events[0].Message)) {
		t.Errorf("failed to load events, expected %s actual %s", blocks[len(blocks)-11], response.Events[0].Message)
	}

	// continue to the end
	func() {
		count := 0
		stepSize := 5
		for {
			req, _ = http.NewRequest("GET", makeRequestUrl(stepSize, server.URL, response.NextPageToken), nil)
			req.Header.Add("Accept", "application/json")
			resp, err = client.Do(req)
			if err != nil {
				t.Fatalf("failed to get events: %v", err)
			}
			defer resp.Body.Close()
			body, err = io.ReadAll(resp.Body)
			response = &JsonResponse{}
			if err := json.Unmarshal(body, response); err != nil {
				t.Fatal(err)
			}
			count += len(response.Events)
			if len(response.Events) < stepSize {
				break
			}
		}

		if count != len(blocks)-11 {
			t.Errorf("failed to read to the end, expected %v actual %v", len(blocks)-11, count)
		}
	}()
}

func TestAuditorLoggerTxtOutput(t *testing.T) {
	tempDir := t.TempDir()

	c := NewDefaultConfig()
	c.LogDir = tempDir
	auditor := NewAuditor(c)
	logger := auditor.LoggerWriter()
	blocks := make([][]byte, 26)
	for i := 0; i < len(blocks); i++ {
		blocks[i] = makeBlock(63, 'a'+byte(i%26))
		logger.V(0).Node().Message(string(blocks[i])).Do()
	}
	logger.Flush()

	server := httptest.NewServer(http.HandlerFunc(auditor.HttpHandler()))
	defer server.Close()

	client := http.Client{}
	req, _ := http.NewRequest("GET", makeRequestUrl(10, server.URL, ""), nil)
	resp, err := client.Do(req)
	if err != nil {
		t.Fatalf("failed to get events: %v", err)
	}
	defer resp.Body.Close()
	body, _ := io.ReadAll(resp.Body)
	lines := bytes.Split(bytes.TrimSpace(body), []byte{'\n'})
	if len(lines) != 10 {
		t.Errorf("failed to load events, expected %d actual %d", 10, len(lines))
	}

	nextPageTokens := resp.Header["Next-Page-Token"]
	if len(nextPageTokens) < 1 || nextPageTokens[0] == "" {
		t.Errorf("missing header Next-Page-Token: %v", nextPageTokens)
	}
}

func TestAuditorLoggerReaderInvalidPageToken(t *testing.T) {
	tempDir := t.TempDir()

	c := NewDefaultConfig()
	c.LogDir = tempDir
	c.ActiveReaderTTL = time.Millisecond * 100
	c.TickerDuration = time.Millisecond * 100
	auditor := NewAuditor(c)
	logger := auditor.LoggerWriter()
	blocks := make([][]byte, 26)
	for i := 0; i < len(blocks); i++ {
		blocks[i] = makeBlock(63, 'a'+byte(i%26))
		logger.V(0).Node().Message(string(blocks[i])).Do()
	}
	logger.Flush()

	server := httptest.NewServer(http.HandlerFunc(auditor.HttpHandler()))
	defer server.Close()

	client := http.Client{}
	req, _ := http.NewRequest("GET", makeRequestUrl(10, server.URL, ""), nil)
	resp, err := client.Do(req)
	if err != nil {
		t.Fatalf("failed to get events: %v", err)
	}
	defer resp.Body.Close()
	body, _ := io.ReadAll(resp.Body)
	lines := bytes.Split(bytes.TrimSpace(body), []byte{'\n'})
	if len(lines) != 10 {
		t.Errorf("failed to load events, expected %d actual %d", 10, len(lines))
	}

	nextPageTokens := resp.Header["Next-Page-Token"]
	if len(nextPageTokens) < 1 || nextPageTokens[0] == "" {
		t.Errorf("missing header Next-Page-Token: %v", nextPageTokens)
	}

	stopCh := make(chan struct{})
	defer close(stopCh)
	go auditor.Run(stopCh)

	time.Sleep(time.Second)

	// request with expired token
	req, _ = http.NewRequest("GET", makeRequestUrl(10, server.URL, nextPageTokens[0]), nil)
	resp, err = client.Do(req)
	if err != nil {
		t.Fatalf("failed to get events: %v", err)
	}
	if resp.StatusCode != http.StatusConflict {
		t.Errorf("unexpected status code [%v] for expired reader", resp.StatusCode)
	}

	// request with not exists token
	req, _ = http.NewRequest("GET", makeRequestUrl(10, server.URL, "not-exists-token"), nil)
	resp, err = client.Do(req)
	if err != nil {
		t.Fatalf("failed to get events: %v", err)
	}
	if resp.StatusCode != http.StatusConflict {
		t.Errorf("unexpected status code [%v] for invalid reader", resp.StatusCode)
	}
}

func TestAuditorLoggerMaxActiveReaders(t *testing.T) {
	tempDir := t.TempDir()

	c := NewDefaultConfig()
	c.LogDir = tempDir
	ad := NewAuditor(c)
	logger := ad.LoggerWriter()
	blocks := make([][]byte, 26)
	for i := 0; i < len(blocks); i++ {
		blocks[i] = makeBlock(63, 'a'+byte(i%26))
		logger.V(0).Node().Message(string(blocks[i])).Do()
	}
	logger.Flush()

	server := httptest.NewServer(http.HandlerFunc(ad.HttpHandler()))
	defer server.Close()

	client := http.Client{}

	for i := 0; i < c.MaxConcurrentReaders+5; i++ {
		req, _ := http.NewRequest("GET", makeRequestUrl(10, server.URL, ""), nil)
		resp, err := client.Do(req)
		if err != nil {
			t.Fatalf("failed to get events: %v", err)
		}
		resp.Body.Close()
	}

	if ad.(*auditor).activeReaders.Len() != c.MaxConcurrentReaders {
		t.Error("failed to expired reader")
	}
}
