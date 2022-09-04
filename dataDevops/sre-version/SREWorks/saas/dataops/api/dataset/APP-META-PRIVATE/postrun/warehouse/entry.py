# coding: utf-8

from . import sw_entity_init as entity
from . import sw_model_init as model


def init():
    entity.add_sw_entities()
    model.add_sw_models()
    model.add_default_resource_price()
