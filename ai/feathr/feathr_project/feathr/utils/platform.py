import os


def is_jupyter() -> bool:
    """Check if the module is running on Jupyter notebook/console.
    Note - there might be better way to check if the code is running on a jupyter notebook or not,
    but this hacky way still works.

    Ref:
        https://stackoverflow.com/questions/15411967/how-can-i-check-if-code-is-executed-in-the-ipython-notebook

    Returns:
        bool: True if the module is running on Jupyter notebook or Jupyter console, False otherwise.
    """
    # Should check is_databricks() and is_synapse() first since they also use the same ZMQ interactive shell.
    if is_databricks() or is_synapse():
        return False

    try:
        # Pre-loaded module `get_ipython()` tells you whether you are running inside IPython or not.
        shell_name = get_ipython().__class__.__name__
        # `ZMQInteractiveShell` tells you if this is an interactive mode (notebook).
        if shell_name == "ZMQInteractiveShell":
            return True
        else:
            return False
    except NameError:
        return False


def is_databricks() -> bool:
    """Check if the module is running on Databricks.

    Returns:
        bool: True if the module is running on Databricks notebook, False otherwise.
    """
    # Note, this is a hacky way to check if the code is running on Databricks.
    if "DATABRICKS_RUNTIME_VERSION" in os.environ:
        return True
    else:
        return False


def is_synapse() -> bool:
    """Check if the module is running on Azure Synapse.

    Returns:
        bool: True if the module is running on Azure Synapse notebook, False otherwise.
    """
    # Note, this is a hacky way to check if the code is running on Synapse.
    if "SYNAPSE_ENABLE_CONFIG_MERGE_RULE" in os.environ:
        return True
    else:
        return False
