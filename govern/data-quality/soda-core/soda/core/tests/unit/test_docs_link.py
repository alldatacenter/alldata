from helpers.test_scan import TestScan


def test_docs_link():
    scan = TestScan()
    scan.add_sodacl_yaml_str(
        f"""
      checks forrrr FREE:
        - row_count > 0
    """
    )
