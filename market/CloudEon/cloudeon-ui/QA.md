报错：Error: error:0308010C:digital envelope routines::unsupported
处理方式：
```shell
export NODE_OPTIONS=--openssl-legacy-provider
```

报错：.git can't be found 
修改package.json
```
"prepare": "cd .. && husky install superjsonweb/.husky",
```