# Crawlab 的 Python SDK

中文 | [English](https://github.com/crawlab-team/crawlab-sdk/blob/master/python/README.md)

Crawlab 的 Python SDK 主要由 2 部分构成:
1. CLI 命令行工具
2. Utility 工具

## CLI 命令行工具

CLI 命令行工具主要是为比较习惯用命令行的开发者设计的，他们可以利用这个命令行工具与 Crawlab 进行交互。

CLI 命令行工具的安装很简单：

```bash
pip install crawlab-sdk
```

然后，您就可以用 `crawlab` 这个命令在命令行中与 Crawlab 交互了。

可以利用下方命令来查找帮助，或者您可以参考 
[官方文档](https://docs.crawlab.cn/SDK/CLI.html)。

```
crawlab --help
```

## Utility 工具

Utility 工具主要提供一些 `Helper` 方法来让您的爬虫更好的集成到 Crawlab 中，例如保存结果数据到 Crawlab 中等等。

下面介绍 Scrapy 和一般 Python 爬虫与 Crawlab 集成的方式。

⚠️注意：请确保您已经通过 pip 安装了 `crawlab-sdk`。

##### Scrapy 集成

在 `settings.py` 中找到 `ITEM_PIPELINES`（`dict` 类型的变量），在其中添加如下内容。

```python
ITEM_PIPELINES = {
    'crawlab.pipelines.CrawlabMongoPipeline': 888,
}
```

然后，启动 Scrapy 爬虫，运行完成之后，您就应该能看到抓取结果出现在 [任务详情-结果](../Task/View.md) 里。

##### 通用 Python 爬虫集成

将下列代码加入到您爬虫中的结果保存部分。

```python
# 引入保存结果方法
from crawlab import save_item

# 这是一个结果，需要为 dict 类型
result = {'name': 'crawlab'}

# 调用保存结果方法
save_item(result)
```

然后，启动爬虫，运行完成之后，您就应该能看到抓取结果出现在 [任务详情-结果](../Task/View.md) 里。
