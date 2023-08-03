package main

import (
	"fmt"
	"math/rand"
	"net/http"
	"strconv"

	"github.com/edgenesis/shifu/pkg/deviceshifu/mockdevice/mockdevice"
	"github.com/edgenesis/shifu/pkg/logger"
)

func main() {
	availableFuncs := []string{
		"get_coordinate",
		"get_status",
	}
	mockdevice.StartMockDevice(availableFuncs, instructionHandler)
}

func instructionHandler(functionName string) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		logger.Infof("Handling: %v", functionName)
		switch functionName {
		case "get_coordinate":
			xrange := 100
			yrange := 200
			zrange := 300
			xpos := strconv.Itoa(rand.Intn(xrange))
			ypos := strconv.Itoa(rand.Intn(yrange))
			zpos := strconv.Itoa(rand.Intn(zrange))
			fmt.Fprintf(w, "xpos: %v, ypos: %v, zpos: %v", xpos, ypos, zpos)
		case "get_status":
			fmt.Fprintf(w, mockdevice.StatusSetList[(rand.Intn(len(mockdevice.StatusSetList)))])
		}
	}
}
