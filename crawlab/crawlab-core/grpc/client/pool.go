package client

import (
	"github.com/crawlab-team/crawlab-core/errors"
	"github.com/crawlab-team/crawlab-core/interfaces"
	"github.com/crawlab-team/go-trace"
	"github.com/emirpasic/gods/lists/arraylist"
	"go.uber.org/dig"
	"math/rand"
	"sync"
)

type Pool struct {
	// settings
	size    int
	cfgPath string

	// internals
	clients *arraylist.List
}

func (p *Pool) GetConfigPath() (path string) {
	return p.cfgPath
}

func (p *Pool) SetConfigPath(path string) {
	p.cfgPath = path
}

func (p *Pool) Init() (err error) {
	for i := 0; i < p.size; i++ {
		if err := p.NewClient(); err != nil {
			return err
		}
	}
	return nil
}

func (p *Pool) NewClient() (err error) {
	c, err := NewClient(WithConfigPath(p.cfgPath))
	if err != nil {
		return trace.TraceError(err)
	}
	if err := c.Start(); err != nil {
		return err
	}
	p.clients.Add(c)
	return nil
}

func (p *Pool) GetClient() (c interfaces.GrpcClient, err error) {
	idx := p.getRandomIndex()
	res, ok := p.clients.Get(idx)
	if !ok {
		return nil, trace.TraceError(errors.ErrorGrpcClientNotExists)
	}
	c, ok = res.(interfaces.GrpcClient)
	if !ok {
		return nil, trace.TraceError(errors.ErrorGrpcInvalidType)
	}
	return c, nil
}

func (p *Pool) SetSize(size int) {
	p.size = size
}

func (p *Pool) getRandomIndex() (idx int) {
	return rand.Intn(p.clients.Size())
}

func NewNewPool(opts ...PoolOption) (p interfaces.GrpcClientPool, err error) {
	// pool
	p = &Pool{
		size:    1,
		clients: arraylist.New(),
	}

	// apply options
	for _, opt := range opts {
		opt(p)
	}

	// initialize
	if err := p.Init(); err != nil {
		return nil, err
	}

	return p, nil
}

func ProvideClientPool(path string) func() (interfaces.GrpcClientPool, error) {
	return func() (interfaces.GrpcClientPool, error) {
		return NewNewPool(WithPoolConfigPath(path))
	}
}

var poolStore = sync.Map{}

func GetClientPool(path string) (p interfaces.GrpcClientPool, err error) {
	res, ok := poolStore.Load(path)
	if !ok {
		return createClientPool(path)
	}
	p, ok = res.(interfaces.GrpcClientPool)
	if !ok {
		return createClientPool(path)
	}
	return p, nil
}

func createClientPool(path string) (p2 interfaces.GrpcClientPool, err error) {
	c := dig.New()
	if err := c.Provide(ProvideClientPool(path)); err != nil {
		return nil, trace.TraceError(err)
	}
	if err := c.Invoke(func(p interfaces.GrpcClientPool) {
		p2 = p
	}); err != nil {
		return nil, trace.TraceError(err)
	}
	return p2, nil
}
