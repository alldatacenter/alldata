import os
from typing import Optional


def get_task_id() -> Optional[str]:
    try:
        return os.getenv('CRAWLAB_TASK_ID')
    except Exception:
        return None
