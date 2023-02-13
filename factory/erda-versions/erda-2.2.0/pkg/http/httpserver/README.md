# httpserver

## server
TODO

## errorresp

统一的错误定义，避免代码中出现大量的 plain string

### 定义错误

注意的是定义抽象的大类错误（比如：创建应用失败、部署失败）:

`internal/demo/services/apierrors/errors.go`

```go
var (
	ErrCreateDemo = errorresp.New(errorresp.WithInitMessage("ErrCreateDemo", "创建失败例子"))
)
```

### 返回错误

services 中返回 error：

`internal/demo/services/foobar/foobar.go`

```go
func (f *Foobar) CreateDemo() error {
	return apierrors.ErrCreateDemo.InvalidState("not ready for creation")
}
```

endpoints 中将 services 返回的错误封装成 ErrResp:

`internal/demo/endpoints/info.go`

```go
err := e.foobar.CreateDemo()
if err != nil {
    return errorresp.ErrResp(err)
}
```

endpoints 中直接返回错误:

`internal/demo/endpoints/info.go`

```go
return apierrors.ErrCreateDemo.InvalidParameter("demo is error").ToResp(), nil
```
