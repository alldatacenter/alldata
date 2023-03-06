class Lazy:
    def __init__(self):
        self.object = None

    def is_set(self) -> bool:
        return self.object is not None

    def get(self):
        return self.object

    def set(self, o):
        self.object = o
