import os
from typing import Optional

from jinja2 import Environment
from jinja2.runtime import Context


class OsContext(Context):
    def resolve_or_missing(self, key):
        # Note: ordering here must be the same as in scan.get_variable(variable_name): First env vars, then scan vars
        if key in os.environ:
            return os.environ[key]
        elif key in self.vars:
            return self.vars[key]
        elif key == "env_var":
            return Jinja.env_var
        return Context.resolve_or_missing(self, key)


def create_os_environment():
    environment = Environment(variable_start_string="${", variable_end_string="}")
    environment.context_class = OsContext
    return environment


class Jinja:
    environment = create_os_environment()

    @staticmethod
    def resolve(template: str, variables: dict = None) -> str:
        """
        Convenience method that funnels Jinja exceptions into parselog errors.
        This method throws no exceptions.  Returns None in case of Jinja exceptions.
        """
        if not isinstance(variables, dict):
            variables = {}
        jinja_template = Jinja.environment.from_string(template)
        rendered_value = jinja_template.render(variables)
        return rendered_value

    @staticmethod
    def env_var(variable_name: str, default_value: Optional[str] = None) -> str:
        """
        Function that can be used in the variables in resolve.  Usage:
        variables = {
            'env_var': Jinja.env_var
        }
        Jinja.resolve(template, variables, parse)
        """
        value = os.getenv(variable_name, default_value)
        return value if isinstance(value, str) else ""
