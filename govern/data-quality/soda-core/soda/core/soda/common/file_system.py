#  Copyright 2020 Soda
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#   http://www.apache.org/licenses/LICENSE-2.0
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
import logging
import os
from pathlib import Path

logger = logging.getLogger(__name__)


class FileSystem:
    def user_home_dir(self):
        return str(Path.home())

    def expand_user(self, path: str):
        return os.path.expanduser(path)

    def exists(self, path: str):
        return Path(self.expand_user(path)).exists()

    def is_dir(self, path: str):
        return Path(self.expand_user(path)).is_dir()

    def is_file(self, path: str):
        return Path(self.expand_user(path)).is_file()

    def file_read_as_str(self, path: str) -> str:
        with open(self.expand_user(path), encoding="utf-8") as f:
            return f.read()

    def file_write_from_str(self, path: str, file_content_str):
        path = self.expand_user(path)
        path_path: Path = Path(path)

        is_new = not path_path.exists()
        with open(path_path, "w+", encoding="utf-8") as f:
            f.write(file_content_str)
        if is_new:
            os.chmod(path, 0o666)

    def scan_dir(self, dir_path: str):
        return os.scandir(self.expand_user(dir_path))

    def dirname(self, path: str):
        return os.path.dirname(self.expand_user(path))

    def mkdirs(self, path: str, absolute: bool = False):
        Path(self.expand_user(path)).mkdir(parents=True, exist_ok=True)

    def join(self, a: str, *paths: str):
        return os.path.join(a, *paths)


class FileSystemSingleton:
    INSTANCE: FileSystem = FileSystem()


def file_system() -> FileSystem:
    return FileSystemSingleton.INSTANCE
