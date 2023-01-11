package main

import (
	"fmt"
	"math/rand"
	"net/http"
	"strconv"
	"time"

	"github.com/edgenesis/shifu/pkg/deviceshifu/mockdevice/mockdevice"
	"github.com/edgenesis/shifu/pkg/logger"
)

func main() {
	availableFuncs := []string{
		"read_value",
		"get_status",
	}
	mockdevice.StartMockDevice(availableFuncs, instructionHandler)
}

func instructionHandler(functionName string) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		logger.Infof("Handling: %v", functionName)
		switch functionName {
		case "read_value":
			rand.Seed(time.Now().UnixNano())
			min := 10
			max := 30
			fmt.Fprint(w, strconv.Itoa(rand.Intn(max-min+1)+min))
		case "get_status":
			rand.Seed(time.Now().UnixNano())
			fmt.Fprint(w, mockdevice.StatusSetList[(rand.Intn(len(mockdevice.StatusSetList)))])
		}
	}
}
