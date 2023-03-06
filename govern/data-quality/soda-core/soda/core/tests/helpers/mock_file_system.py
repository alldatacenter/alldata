import os

from soda.common.file_system import FileSystem, FileSystemSingleton


class MockFileSystem(FileSystem):
    current_dir = "/Users/johndoe"
    dirs = set("/")
    files = {}

    def __init__(self):
        FileSystemSingleton.INSTANCE = self

    def user_home_dir(self):
        return "/Users/johndoe"

    def mkdirs(self, path: str, absolute: bool = False):
        normalized_path = self.normalize(path)
        self.dirs.add(normalized_path)

    def file_read_as_str(self, path: str) -> str:
        normalized_path = self.normalize(path)
        return self.files.get(normalized_path)

    def file_write_from_str(self, path: str, file_content_str):
        normalized_path = self.normalize(path)
        self.files[normalized_path] = file_content_str

    def exists(self, path: str):
        normalized_path = self.normalize(path)
        return normalized_path in self.files

    def is_file(self, path: str):
        normalized_path = self.normalize(path)
        return normalized_path in self.files

    def is_dir(self, path: str):
        normalized_path = self.normalize(path)
        return normalized_path in self.dirs

    def expand_user(self, path: str):
        if path.startswith("~"):
            return self.user_home_dir() + path[1:]
        return path

    def scan_dir(self, dir_path: str):
        return super().scan_dir(dir_path)

    def dirname(self, path: str):
        return super().dirname(path)

    def list_dir(self, dir_path):
        normalized_dir_path = self.normalize(dir_path)
        return {k: v for k, v in self.files.items() if k.startswith(normalized_dir_path)}

    def normalize(self, path: str):
        if path.startswith("~"):
            path = self.user_home_dir() + path[1:]
        if path.startswith("./"):
            path = path[2:]
        if not path.startswith("/"):
            path = f"{self.current_dir}/{path}"
        return os.path.normpath(path)
