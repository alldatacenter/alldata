package main

import (
	"net/http"
	"os"
)

func main() {
	http.HandleFunc("/get_file_mp4", func(w http.ResponseWriter, r *http.Request) {
		// you need copy a mp4 file to here first
		fileContent, _ := os.ReadFile("test.mp4")
		w.Write(fileContent)
	})

	http.ListenAndServe(":12345", nil)
}
