from soda.scan import Scan


def test_variables():
    scan = Scan()
    scan.add_variables({"now": "2022-10-22 11:12:13"})
    scan.add_sodacl_yaml_str(
        f"""
          variables:
            hello: world
            sometime_later: ${{now}}
        """
    )

    assert scan._variables["hello"] == "world"
    assert scan._variables["sometime_later"] == "2022-10-22 11:12:13"
