import logging
from typing import List

from ruamel.yaml import YAML
from ruamel.yaml.error import MarkedYAMLError
from soda.common.logs import Logs
from soda.sodacl.location import Location

logger = logging.getLogger(__name__)


class PathElement:
    def __init__(self, location: Location, value=None):
        self.location: Location = location
        self.value = value


class PathStack:
    def __init__(self, file_path: str):
        self.file_path = file_path
        self.path_elements: List[PathElement] = [PathElement(location=Location(file_path))]

    def set_antlr_collection_in_file_path_element(self, file_dict: dict):
        self.path_elements[0].value = file_dict

    def push_path_element(self, key, value) -> Location:
        location = self.create_location(key)
        self.path_elements.append(PathElement(location=location, value=value))
        return location

    def pop_path_element(self):
        self.path_elements.pop()
        return self.path_elements[-1].location

    def create_location(self, key):
        last_path_element = self.path_elements[-1]
        collection = last_path_element.value
        if isinstance(collection, dict) and isinstance(key, str):
            item_data = collection.lc.data.get(key)
        elif isinstance(collection, list) and isinstance(key, int):
            item_data = collection.lc.data[key]
        else:
            return last_path_element.location
        line = item_data[0] + 1
        col = item_data[1] + 1
        # path_str should only be the nested keys inside the yaml, not the file_path
        # first path element is the file path so we skip that in the path_str
        return Location(self.file_path, line, col)

    def get_value(self, key):
        return self.path_elements[-1].value.get(key)


class Parser:
    def __init__(self, file_path: str, logs: Logs):
        self.path_stack: PathStack = PathStack(file_path)
        self.location = self.path_stack.path_elements[0].location
        self.logs = logs

    def _parse_yaml_str(self, yaml_str: str) -> dict:
        try:
            yaml = YAML()
            yaml.preserve_quotes = True
            file_dict = yaml.load(yaml_str)
            self.path_stack.set_antlr_collection_in_file_path_element(file_dict)
            return file_dict
        except MarkedYAMLError as e:
            location = self._get_location_from_yaml_error(e)
            self.logs.error(f"YAML syntax error", location=location, exception=e)

    def _get_location_from_yaml_error(self, e):
        mark = e.context_mark if e.context_mark else e.problem_mark
        return Location(
            file_path=self.path_stack.file_path,
            line=mark.line + 1,
            col=mark.column + 1,
        )

    def _push_path_element(self, key, value):
        self.location = self.path_stack.push_path_element(key, value)

    def _pop_path_element(self):
        self.location = self.path_stack.pop_path_element()

    def _get_required(self, key: str, value_type: type):
        return self.__get_value(key, required=True, value_type=value_type)

    def _get_optional(self, key: str, value_type: type):
        return self.__get_value(key, required=False, value_type=value_type)

    def __get_value(self, key: str, required: bool, value_type: type):
        value = self.path_stack.get_value(key)
        if value is None:
            if required:
                self.logs.error(f"{key} is required", location=self.location)
        elif not isinstance(value, value_type):
            self.logs.error(
                f'Invalid YAML structure: key "{key}" must contain a {value_type.__name__}, but was {type(value).__name__}',
                location=self.location,
            )
            return None
        return value

    def _resolve_jinja(self, value: str, variables: dict = None):
        from soda.common.jinja import Jinja

        """
        Resolves the given string with Jinja templating and then parses the resolved string as a configuration yaml
        """
        if isinstance(value, str) and "$" in value:
            try:
                return Jinja.resolve(template=value, variables=variables)
            except BaseException as e:
                self.logs.error(
                    f"Could not resolve {value} with Jinja: {e}",
                    e,
                    location=self.location,
                )
        return value
