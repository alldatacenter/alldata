from crawlab import save_item
from crawlab.config import get_task_id
from crawlab.entity.result import Result


class CrawlabPipeline(object):
    def process_item(self, item, spider):
        result = Result(item)
        result.set_task_id(get_task_id())
        save_item(result)

        return item
