package log

import (
	"bufio"
	"bytes"
	"errors"
	"github.com/apex/log"
	"github.com/crawlab-team/crawlab-core/utils"
	"github.com/crawlab-team/go-trace"
	"io"
	"os"
	"path/filepath"
	"sync"
	"time"
)

type FileLogDriver struct {
	// settings
	opts *FileLogDriverOptions // options

	// internals
	mu          sync.Mutex
	logFileName string
}

type FileLogDriverOptions struct {
	BaseDir string
	Ttl     time.Duration
}

func (d *FileLogDriver) Init() (err error) {
	go d.cleanup()

	return nil
}

func (d *FileLogDriver) Close() (err error) {
	return nil
}

func (d *FileLogDriver) WriteLine(id string, line string) (err error) {
	d.initDir(id)

	d.mu.Lock()
	defer d.mu.Unlock()

	f, err := os.OpenFile(d.getLogFilePath(id, d.logFileName), os.O_WRONLY|os.O_APPEND|os.O_CREATE, os.FileMode(0760))
	if err != nil {
		return trace.TraceError(err)
	}
	defer f.Close()

	_, err = f.WriteString(line + "\n")
	if err != nil {
		return trace.TraceError(err)
	}

	return nil
}

func (d *FileLogDriver) WriteLines(id string, lines []string) (err error) {
	for _, l := range lines {
		if err := d.WriteLine(id, l); err != nil {
			return err
		}
	}
	return nil
}

func (d *FileLogDriver) Find(id string, pattern string, skip int, limit int) (lines []string, err error) {
	if pattern != "" {
		return lines, errors.New("not implemented")
	}
	if !utils.Exists(d.getLogFilePath(id, d.logFileName)) {
		return nil, nil
	}

	f, err := os.Open(d.getLogFilePath(id, d.logFileName))
	if err != nil {
		return nil, trace.TraceError(err)
	}
	defer f.Close()

	sc := bufio.NewScanner(f)

	i := -1
	for sc.Scan() {
		i++

		if i < skip {
			continue
		}

		if i >= skip+limit {
			break
		}

		line := sc.Text()
		lines = append(lines, line)
	}
	if err := sc.Err(); err != nil {
		return nil, trace.TraceError(err)
	}

	return lines, nil
}

func (d *FileLogDriver) Count(id string, pattern string) (n int, err error) {
	if pattern != "" {
		return n, errors.New("not implemented")
	}
	if !utils.Exists(d.getLogFilePath(id, d.logFileName)) {
		return 0, nil
	}

	f, err := os.Open(d.getLogFilePath(id, d.logFileName))
	if err != nil {
		return n, trace.TraceError(err)
	}
	return d.lineCounter(f)
}

func (d *FileLogDriver) Flush() (err error) {
	return nil
}

func (d *FileLogDriver) getBasePath(id string) (filePath string) {
	return filepath.Join(d.opts.BaseDir, id)
}

func (d *FileLogDriver) getMetadataPath(id string) (filePath string) {
	return filepath.Join(d.opts.BaseDir, id, MetadataName)
}

func (d *FileLogDriver) getLogFilePath(id, fileName string) (filePath string) {
	return filepath.Join(d.opts.BaseDir, id, fileName)
}

func (d *FileLogDriver) getLogFiles(id string) (files []os.FileInfo) {
	return utils.ListDir(d.getBasePath(id))
}

func (d *FileLogDriver) initDir(id string) {
	if !utils.Exists(d.getBasePath(id)) {
		if err := os.MkdirAll(d.getBasePath(id), os.FileMode(0770)); err != nil {
			trace.PrintError(err)
		}
	}
}

func (d *FileLogDriver) lineCounter(r io.Reader) (n int, err error) {
	buf := make([]byte, 32*1024)
	count := 0
	lineSep := []byte{'\n'}

	for {
		c, err := r.Read(buf)
		count += bytes.Count(buf[:c], lineSep)

		switch {
		case err == io.EOF:
			return count, nil

		case err != nil:
			return count, err
		}
	}
}

func (d *FileLogDriver) cleanup() {
	for {
		dirs := utils.ListDir(d.opts.BaseDir)
		for _, dir := range dirs {
			if time.Now().After(dir.ModTime().Add(d.opts.Ttl)) {
				if err := os.RemoveAll(d.getBasePath(dir.Name())); err != nil {
					trace.PrintError(err)
					continue
				}
				log.Infof("removed outdated log directory: %s", d.getBasePath(dir.Name()))
			}
		}

		time.Sleep(10 * time.Minute)
	}
}

var logDriver Driver

func newFileLogDriver(options *FileLogDriverOptions) (driver Driver, err error) {
	if options == nil {
		options = &FileLogDriverOptions{}
	}

	// normalize BaseDir
	baseDir := options.BaseDir
	if baseDir == "" {
		baseDir = "/var/log/crawlab"
	}
	options.BaseDir = baseDir

	// normalize Ttl
	ttl := options.Ttl
	if ttl == 0 {
		ttl = 30 * 24 * time.Hour
	}
	options.Ttl = ttl

	// driver
	driver = &FileLogDriver{
		opts:        options,
		logFileName: "log.txt",
		mu:          sync.Mutex{},
	}

	// init
	if err := driver.Init(); err != nil {
		return nil, err
	}

	return driver, nil
}

func GetFileLogDriver(options *FileLogDriverOptions) (driver Driver, err error) {
	if logDriver != nil {
		return logDriver, nil
	}
	logDriver, err = newFileLogDriver(options)
	if err != nil {
		return nil, err
	}
	return logDriver, nil
}
