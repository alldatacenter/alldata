package main

import (
	"io"
	"net/http"
	"strings"
	"testing"
	"time"

	"github.com/edgenesis/shifu/pkg/deviceshifu/mockdevice/mockdevice"
	"github.com/stretchr/testify/assert"
)

func TestInstructionHandler(t *testing.T) {
	availableFuncs := []string{
		"get_position",
		"get_status",
	}
	t.Setenv("MOCKDEVICE_NAME", "mockdevice_test")
	t.Setenv("MOCKDEVICE_PORT", "12345")
	mocks := []struct {
		name       string
		url        string
		StatusCode int
		expResult  interface{}
	}{
		{
			"case 1 port 12345 get_status",
			"http://localhost:12345/get_status",
			200,
			[]string{"Running", "Idle", "Busy", "Error"},
		},
		{
			"case 2 port 12345 get_position",
			"http://localhost:12345/get_position",
			200,
			true,
		},
	}

	go mockdevice.StartMockDevice(availableFuncs, instructionHandler)

	time.Sleep(100 * time.Microsecond)

	for _, c := range mocks {
		t.Run(c.name, func(t *testing.T) {
			resp, err := http.Get(c.url)
			assert.Nil(t, err)
			defer resp.Body.Close()
			body, _ := io.ReadAll(resp.Body)

			switch {
			case strings.Contains(c.url, "/get_position"):
				assert.Equal(t, c.expResult, check(string(body)))
			case strings.Contains(c.url, "/get_status"):
				assert.Contains(t, c.expResult, string(body))
			}
		})
	}
}

func check(Result string) bool {
	res := true
	expResult := []string{"xpos", "ypos"}
	for _, v := range expResult {
		if !strings.Contains(Result, v) {
			res = false
			break
		}
	}
	return res
}
