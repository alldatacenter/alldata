import os
from pathlib import Path


def write_to_file(content: str, full_file_name: str):
    """Write content to a file.
    Attributes:
        content: content to write into the file
        full_file_name: full file path
    """
    dir_name = os.path.dirname(full_file_name)
    Path(dir_name).mkdir(parents=True, exist_ok=True)
    with open(full_file_name, "w") as handle:
        print(content, file=handle)