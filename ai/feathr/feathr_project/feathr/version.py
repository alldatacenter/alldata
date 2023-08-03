__version__ = "1.0.0"

def get_version():
    return __version__

# Decouple Feathr MAVEN Version from Feathr Python SDK Version
import os
def get_maven_artifact_fullname():
    maven_artifact_version = os.environ.get("MAVEN_ARTIFACT_VERSION", __version__)
    return f"com.linkedin.feathr:feathr_2.12:{maven_artifact_version}"