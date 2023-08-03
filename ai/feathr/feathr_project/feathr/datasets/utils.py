"""Dataset utilities
"""
import logging
import math
from pathlib import Path
import requests
from urllib.parse import urlparse

from tqdm import tqdm


log = logging.getLogger(__name__)


def maybe_download(src_url: str, dst_filepath: str, expected_bytes=None) -> bool:
    """Check if file exists. If not, download and return True. Else, return False.

    Refs:
        https://github.com/microsoft/recommenders/blob/main/recommenders/datasets/download_utils.py

    Args:
        src_url: Source file URL.
        dst_filepath: Destination file path.
        expected_bytes (optional): Expected bytes of the file to verify.

    Returns:
        bool: Whether the file was downloaded or not
    """
    dst_filepath = Path(dst_filepath)

    if dst_filepath.is_file():
        log.info(f"File {str(dst_filepath)} already exists")
        return False

    # Check dir if exists. If not, create one
    dst_filepath.parent.mkdir(parents=True, exist_ok=True)

    response = requests.get(src_url, stream=True)
    if response.status_code == 200:
        log.info(f"Downloading {src_url}")
        total_size = int(response.headers.get("content-length", 0))
        block_size = 1024
        num_iterables = math.ceil(total_size / block_size)
        with open(str(dst_filepath.resolve()), "wb") as file:
            for data in tqdm(
                response.iter_content(block_size),
                total=num_iterables,
                unit="KB",
                unit_scale=True,
            ):
                file.write(data)

        # Verify the file size
        if expected_bytes is not None and expected_bytes != dst_filepath.stat().st_size:
            # Delete the file since the size is not the same as the expected one.
            dst_filepath.unlink()
            raise IOError(f"Failed to verify {str(dst_filepath)}. Maybe interrupted while downloading?")
        else:
            return True

    else:
        response.raise_for_status()
        # If not HTTPError yet still cannot download
        raise Exception(f"Problem downloading {src_url}")
