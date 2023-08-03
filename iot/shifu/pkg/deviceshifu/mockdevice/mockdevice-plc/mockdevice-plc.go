package main

import (
	"fmt"
	"net/http"
	"strconv"
	"time"

	"github.com/edgenesis/shifu/pkg/deviceshifu/mockdevice/mockdevice"
	"github.com/edgenesis/shifu/pkg/logger"

	"k8s.io/apimachinery/pkg/util/rand"
)

var (
	dataStorage       map[string]string
	memoryArea        = []string{"M", "Q", "T", "C"}
	originalCharacter = "0b0000000000000000"
)

const (
	rootAddress = "rootaddress"
	address     = "address"
	digit       = "digit"
	value       = "value"
)

func main() {
	dataStorage = make(map[string]string)
	for _, v := range memoryArea {
		dataStorage[v] = originalCharacter
	}

	availableFuncs := []string{
		"getcontent",
		"sendsinglebit",
		"get_status",
	}
	mockdevice.StartMockDevice(availableFuncs, instructionHandler)
}

func instructionHandler(functionName string) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		logger.Infof("Handling: %v", functionName)
		switch functionName {
		case "getcontent":
			query := r.URL.Query()
			rootaddress := query.Get(rootAddress)
			if _, ok := dataStorage[rootaddress]; !ok {
				logger.Warnf("Nonexistent memory area: %v", rootaddress)
				w.WriteHeader(http.StatusBadRequest)
				fmt.Fprintf(w, "Nonexistent memory area")
				return
			}

			w.WriteHeader(http.StatusOK)
			fmt.Fprintf(w, dataStorage[rootaddress])
		case "sendsinglebit":
			query := r.URL.Query()
			rootaddress := query.Get(rootAddress)
			addressValue, err := strconv.Atoi(query.Get(address))
			if err != nil {
				logger.Fatalf("%v", err)
			}

			digitsValue, err := strconv.Atoi(query.Get(digit))
			if err != nil {
				logger.Fatalf("%v", err)
			}

			if _, ok := dataStorage[rootaddress]; !ok {
				logger.Warnf("Nonexistent memory area: %v", rootaddress)
				w.WriteHeader(http.StatusBadRequest)
				fmt.Fprintf(w, "Nonexistent memory area")
				return
			}

			requestValue := query.Get(value)
			responseValue := []byte(dataStorage[rootaddress])
			valueModifier := []byte(requestValue)
			responseValue[len(dataStorage[rootaddress])-1-
				addressValue-digitsValue] = valueModifier[0]
			dataStorage[rootaddress] = string(responseValue)
			logger.Infof("%v", responseValue)
			w.WriteHeader(http.StatusOK)
			fmt.Fprintf(w, dataStorage[rootaddress])
		case "get_status":
			rand.Seed(time.Now().UnixNano())
			w.WriteHeader(http.StatusOK)
			fmt.Fprintf(w, mockdevice.StatusSetList[(rand.Intn(len(mockdevice.StatusSetList)))])
		}
	}
}
