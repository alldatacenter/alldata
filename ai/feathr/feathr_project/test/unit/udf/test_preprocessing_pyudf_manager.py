import pytest

from feathr.udf._preprocessing_pyudf_manager import _PreprocessingPyudfManager


@pytest.mark.parametrize(
    "fn_name, fn_str",
    [
        ("fn_without_type_hint", "def fn_without_type_hint(a):\n  return a + 10\n"),
        ("fn_with_type_hint", "def fn_with_type_hint(a: int) -> int:\n  return a + 10\n"),
        ("fn_with_complex_type_hint", "def fn_with_complex_type_hint(a: Union[int, float]) -> Union[int, float]:\n  return a + 10\n"),
    ]
)
def test__parse_function_str_for_name(fn_name, fn_str):
    assert fn_name == _PreprocessingPyudfManager._parse_function_str_for_name(fn_str)
