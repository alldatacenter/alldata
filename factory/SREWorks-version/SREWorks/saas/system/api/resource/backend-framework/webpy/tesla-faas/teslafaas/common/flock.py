# coding: utf-8
import fcntl


class FLock(object):
    """
    Lock base on file lock.

    Usage:
    try:
        lock = FLock("/tmp/lock_name.tmp")
        lock.acquire()
        # Do important stuff that needs to be synchronized
        .
        .
    finally:
        lock.release()

    try:
        lock = FLock("/tmp/lock_name.tmp")
        if (lock.acquire_non_block()):
            # Get lock succeed, do your stuff
            .
            .
        else:
            # Get lock failed, do you processing
            .
            .
    finally:
        lock.release()
    """

    def __init__(self, filename):
        self.filename = filename
        # This will create it if it does not exist already
        self.handle = open(filename, 'w+')

    def acquire(self):
        fcntl.flock(self.handle, fcntl.LOCK_EX)

    def acquire_non_block(self):
        """ non-blocking lock """
        try:
            fcntl.flock(self.handle, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except IOError:  # Get lock failed
            return False
        else:
            return True

    def release(self):
        fcntl.flock(self.handle, fcntl.LOCK_UN)

    def __del__(self):
        """close file descriptor when this object is destroyed"""
        self.handle.close()
