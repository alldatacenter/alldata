"""
Module to hold shared SQLAlchemy state.

These objects are set by the L{coilmq.store.sa.init_model} function.
"""
engine = None  # : The SA engine
Session = None  # : The SA Session (or Session-like callable)
# : The SA C{sqlalchemy.orm.MetaData} instance bound to the engine.
metadata = None
