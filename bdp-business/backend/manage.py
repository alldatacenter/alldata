#!/usr/bin/env python
"""Django's command-line utility for administrative tasks."""
import os
import sys


def main():
    os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'main.settings')
    try:
        from django.core.management import execute_from_command_line
    except Exception:
        raise Exception("Couldn't import Django...")
    execute_from_command_line(sys.argv)


if __name__ == '__main__':
    main()
