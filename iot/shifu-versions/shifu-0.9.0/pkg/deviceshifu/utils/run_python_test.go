package utils

import (
	"os"
	"strings"
	"testing"

	"github.com/stretchr/testify/require"
)

func TestCustomizedDataProcessing(t *testing.T) {
	rawData, _ := os.ReadFile("testdata/raw_data")
	expectedProcessed, _ := os.ReadFile("testdata/expected_data")
	processed := ProcessInstruction("customized_handlers", "humidity", string(rawData), "testdata/pythoncustomizedhandlersfortest")
	processed = strings.ReplaceAll(processed, "'", "\"")
	require.JSONEqf(t, string(expectedProcessed), processed, "Processed data is not the same as expected!")
}
