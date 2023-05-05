# cloud-storage
a sdk for ali oss and minio

## Usage
```go
client := NewClient("127.0.0.1:9000", "accesskey", "secretkey")
url, err := client.Upload("bucketName", "objectName", "filepath")
```

## Tips
- 创建的 bucket 权限需要自己配置，一般需要设为公开