#!/usr/bin/env python
# encoding: utf-8
""" """
__author__ = 'adonis'


def tesla_loadhook(h, *args, **kwargs):
    """
    Converts a load hook into an application processor.

        >>> app = auto_application()
        >>> def f(*args, **kwargs): "something done before handling request"
        ...
        >>> app.add_processor(loadhook(f, *args, **kwargs))
    """
    def processor(handler):
        h(*args, **kwargs)
        return handler()

    return processor

