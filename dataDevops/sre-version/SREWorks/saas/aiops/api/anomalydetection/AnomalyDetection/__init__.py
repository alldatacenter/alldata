import os
import sys
import logging

from bentoml import saved_bundle, configure_logging
from bentoml.cli.bento_service import create_bento_service_cli

# By default, ignore warnings when loading BentoService installed as PyPI distribution
# CLI will change back to default log level in config(info), and by adding --quiet or
# --verbose CLI option, user can change the CLI output behavior
configure_logging(logging_level=logging.ERROR)

__VERSION__ = "20220228111909_6E4D91"

__module_path = os.path.abspath(os.path.dirname(__file__))

AnomalyDetection = saved_bundle.load_bento_service_class(__module_path)

cli=create_bento_service_cli(__module_path)


def load():
    return saved_bundle.load_from_dir(__module_path)


__all__ = ['__version__', 'AnomalyDetection', 'load']
