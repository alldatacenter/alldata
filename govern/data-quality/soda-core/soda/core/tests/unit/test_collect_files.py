from __future__ import annotations

from helpers.fixtures import project_root_dir
from soda.scan import Scan

basedir = f"{project_root_dir}soda/core/tests/unit/test_collect_files_dir"


def collect_paths(path: str, recursive: bool | None = True, suffixes: list[str] | None = None) -> list[str]:
    scan = Scan()
    return scan._collect_file_paths(path=path, recursive=recursive, suffixes=suffixes)


def assert_paths(actual_paths: iter, expected_file_paths: set):
    prefix_length = len(basedir)
    relative_actual_paths = {path[prefix_length:] for path in actual_paths}
    assert relative_actual_paths == expected_file_paths


def test_collect_files():
    assert_paths(
        collect_paths(f"{basedir}/cfgs"),
        {
            "/cfgs/root_configuration.yml",
            "/cfgs/nested/nested/nested_configuration.yml",
            "/cfgs/nested/nested/nested_configuration2.yml",
        },
    )


def test_collect_files_slash():
    assert_paths(
        collect_paths(f"{basedir}/sodacls/"),
        {
            "/sodacls/root_checks2.yml",
            "/sodacls/nested/nested_checks.yml",
            "/sodacls/root_checks.yaml",
        },
    )


def test_collect_files_yaml():
    assert_paths(
        collect_paths(f"{basedir}/sodacls/root_checks.yaml"),
        {
            "/sodacls/root_checks.yaml",
        },
    )
    assert_paths(
        collect_paths(f"{basedir}/cfgs/file_to_be_ignored.txt"),
        {
            "/cfgs/file_to_be_ignored.txt",
        },
    )


def test_collect_files_empty():
    assert_paths(collect_paths(f"{basedir}/empty"), set())
