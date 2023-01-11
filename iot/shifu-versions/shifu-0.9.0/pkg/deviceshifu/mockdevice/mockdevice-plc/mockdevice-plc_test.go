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
	dataStorage = make(map[string]string)
	for _, v := range memoryArea {
		dataStorage[v] = originalCharacter
	}

	availableFuncs := []string{
		"getcontent",
		"sendsinglebit",
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
			"case 1 getcontent rootaddress nil",
			"http://localhost:12345/getcontent",
			400,
			"Nonexistent memory area",
		},
		{
			"case 2 getcontent rootaddress=Q",
			"http://localhost:12345/getcontent?rootaddress=Q",
			200,
			dataStorage["Q"],
		},
		{
			"case 3 sendsinglebit rootaddress nil address=0 digit=1",
			"http://localhost:12345/sendsinglebit?digit=1&address=0",
			400,
			"Nonexistent memory area",
		},
		{
			"case 4 sendsinglebit rootaddress=Q address=0 digit=1",
			"http://localhost:12345/sendsinglebit?rootaddress=Q&digit=1&address=0&value=1",
			200,
			"0b0000000000000010",
		},
		{
			"case 5 get_status",
			"http://localhost:12345/get_status",
			200,
			[]string{"Running", "Idle", "Busy", "Error"},
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
			case strings.Contains(c.url, "/getcontent"):
				assert.Equal(t, c.expResult, string(body))
			case strings.Contains(c.url, "/get_status"):
				assert.Contains(t, c.expResult, string(body))
			}

		})
	}
}
