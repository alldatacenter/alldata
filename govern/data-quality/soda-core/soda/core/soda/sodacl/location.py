from __future__ import annotations


class Location:
    def __init__(
        self,
        file_path: str | None = None,
        line: int | None = None,
        col: int | None = None,
    ):
        self.file_path = file_path
        self.line = line
        self.col = col

    def __str__(self):
        return f"line={self.line},col={self.col} in {self.file_path}"

    def __hash__(self) -> int:
        return hash((self.file_path, self.line, self.col))

    def get_identity_parts(self) -> list:
        return [self.file_path, self.line]

    def get_cloud_dict(self):
        return {
            "filePath": self.file_path,
            "line": self.line,
            "col": self.col,
        }

    def get_dict(self):
        return {
            "filePath": self.file_path,
            "line": self.line,
            "col": self.col,
        }
