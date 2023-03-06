from __future__ import annotations


class TestColumn:
    __test__ = False

    def __init__(self, name: str, data_type: str):
        self.name: str = name
        self.data_type: str = data_type

    def __hash__(self):
        return

    def to_hashable_json(self):
        """
        Used to create the unique hash part in the table name to determine if the table already exists.
        """
        return [self.name, self.data_type]
