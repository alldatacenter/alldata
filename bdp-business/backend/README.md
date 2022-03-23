# BACKEND FOR BDP PLATFORM

> 开源大数据平台

## 环境配置
1.建议在项目根目录创建 `venv` 虚拟环境，通过以下命令进入虚拟环境：

```bash
source venv/bin/activate
```

2.安装项目依赖：

```bash
export PATH="/opt/homebrew/opt/mysql@5.7/bin:$PATH"
export PATH="/opt/homebrew/opt/mysql-client@5.7/bin:$PATH"
pip install -r requirements.txt
```

## 项目配置
把公共变量提到配置项里是一个好的实践，项目的配置项在 `main/settings/*` 目录里，如下：

```
main/settings
├── __init__.py         # 优先使用 local.py ，如果未提供，则应用 development.py
├── default.py          # 公共的默认配置
├── development.py      # 开发环境配置
├── local.py            # 本地开发配置（可选，gitignore）
├── production.py       # 生产环境配置
├── secret.py           # 私密的配置，比如：OBS 的密钥，数据库密钥等（可选，gitignore）
└── testing.py          # 测试环境配置
```

## 常用命令

### 本地开发

1.启动命令

```bash
python manage.py runserver
```

2.生成迁移文件

```bash
python manage.py makemigrations
```

3.执行迁移

```bash
python manage.py migrate
```


### 生产环境 & 测试环境

**注意：环境千万不要用错，否则可能导致不可挽回的局面（比如在测试环境连了生产环境的数据库，然后一顿猛操作...）**

1.启动

```bash
./run.sh start       # prod
./run.sh start:test  # test
```

2.重启

```bash
./run.sh reload       # prod
./run.sh reload:test  # test
```

3.停止

```bash
./run.sh stop       # prod
./run.sh stop       # test
```

更多命令，请执行 `./run.sh --help` 查看，或者直接查看 `run.sh` 脚本代码

4、启动后超级超级用户

```markdown
python3 manage.py createsuperuser
账号密码：admin/admin

```
