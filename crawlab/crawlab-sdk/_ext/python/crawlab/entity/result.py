class Result(dict):
    def get_task_id(self) -> str:
        return self.get('_tid')

    def set_task_id(self, tid: str):
        self['_tid'] = tid

    def to_dict(self) -> dict:
        return dict(self)
