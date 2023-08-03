import os
import traceback
from typing import Optional, Dict, List
from uuid import UUID
from fastapi import APIRouter, FastAPI, HTTPException
from fastapi.responses import JSONResponse
from starlette.middleware.cors import CORSMiddleware
from registry import *
from registry.db_registry import DbRegistry, ConflictError
from registry.models import AnchorDef, AnchorFeatureDef, DerivedFeatureDef, EntityType, ProjectDef, SourceDef, to_snake

rp = "/"
try:
    rp = os.environ["API_BASE"]
    if rp[0] != '/':
        rp = '/' + rp
except:
    pass
print("Using API BASE: ", rp)

registry = DbRegistry()
app = FastAPI()
router = APIRouter()

# Enables CORS
app.add_middleware(CORSMiddleware,
                   allow_origins=["*"],
                   allow_credentials=True,
                   allow_methods=["*"],
                   allow_headers=["*"],
                   )

def exc_to_content(e: Exception) -> Dict:
    content={"message": str(e)}
    if os.environ.get("REGISTRY_DEBUGGING"):
        content["traceback"] = "".join(traceback.TracebackException.from_exception(e).format())
    return content

@app.exception_handler(ConflictError)
async def conflict_error_handler(_, exc: ConflictError):
    return JSONResponse(
        status_code=409,
        content=exc_to_content(exc),
    )


@app.exception_handler(ValueError)
async def value_error_handler(_, exc: ValueError):
    return JSONResponse(
        status_code=400,
        content=exc_to_content(exc),
    )

@app.exception_handler(TypeError)
async def type_error_handler(_, exc: ValueError):
    return JSONResponse(
        status_code=400,
        content=exc_to_content(exc),
    )


@app.exception_handler(KeyError)
async def key_error_handler(_, exc: KeyError):
    return JSONResponse(
        status_code=404,
        content=exc_to_content(exc),
    )

@app.exception_handler(IndexError)
async def index_error_handler(_, exc: IndexError):
    return JSONResponse(
        status_code=404,
        content=exc_to_content(exc),
    )


@router.get("/projects")
def get_projects() -> List[str]:
    return registry.get_projects()

@router.get("/projects-ids")
def get_projects_ids() -> Dict:
    return registry.get_projects_ids()

@router.get("/projects/{project}")
def get_projects(project: str) -> Dict:
    return registry.get_project(project).to_dict()

@router.get("/dependent/{entity}")
def get_dependent_entities(entity: str) -> List:
    entity_id = registry.get_entity_id(entity)
    downstream_entities = registry.get_dependent_entities(entity_id)
    return list([e.to_dict() for e in downstream_entities])

@router.delete("/entity/{entity}")
def delete_entity(entity: str):
    entity_id = registry.get_entity_id(entity)
    downstream_entities = registry.get_dependent_entities(entity_id)
    if len(downstream_entities) > 0:
        registry.delete_empty_entities(downstream_entities)
        if len(registry.get_dependent_entities(entity_id)) > 0:
            raise HTTPException(
                status_code=412, detail=f"""Entity cannot be deleted as it has downstream/dependent entities.
                Entities: {list([e.qualified_name for e in downstream_entities])}"""
            )
    registry.delete_entity(entity_id)

@router.get("/projects/{project}/datasources")
def get_project_datasources(project: str) -> List:
    p = registry.get_entity(project)
    source_ids = [s.id for s in p.attributes.sources]
    sources = registry.get_entities(source_ids)
    return list([e.to_dict() for e in sources])


@router.get("/projects/{project}/datasources/{datasource}")
def get_datasource(project: str, datasource: str) -> Dict:
    p = registry.get_entity(project)
    for s in p.attributes.sources:
        if str(s.id) == datasource:
            return s
    # If datasource is not found, raise 404 error
    raise HTTPException(
        status_code=404, detail=f"Data Source {datasource} not found")


@router.get("/projects/{project}/features")
def get_project_features(project: str, keyword: Optional[str] = None, page: Optional[int] = None, limit: Optional[int] = None) -> List:
    if keyword:
        start =  None
        size = None
        if page is not None and limit is not None:
            start = (page - 1) * limit
            size = limit
        efs = registry.search_entity(
            keyword, [EntityType.AnchorFeature, EntityType.DerivedFeature], project=project, start=start, size=size)
        feature_ids = [ef.id for ef in efs]
        features = registry.get_entities(feature_ids)
        return list([e.to_dict() for e in features])
    else:
        p = registry.get_entity(project)
        feature_ids = [s.id for s in p.attributes.anchor_features] + \
            [s.id for s in p.attributes.derived_features]
        features = registry.get_entities(feature_ids)
        return list([e.to_dict() for e in features])


@router.get("/features/{feature}")
def get_feature(feature: str) -> Dict:
    e = registry.get_entity(feature)
    if e.entity_type not in [EntityType.DerivedFeature, EntityType.AnchorFeature]:
        raise HTTPException(
            status_code=404, detail=f"Feature {feature} not found")
    return e.to_dict()

@router.get("/features/{feature}/lineage")
def get_feature_lineage(feature: str) -> Dict:
    lineage = registry.get_lineage(feature)
    return lineage.to_dict()

@router.post("/projects")
def new_project(definition: Dict) -> Dict:
    id = registry.create_project(ProjectDef(**to_snake(definition)))
    return {"guid": str(id)}


@router.post("/projects/{project}/datasources")
def new_project_datasource(project: str, definition: Dict) -> Dict:
    project_id = registry.get_entity_id(project)
    id = registry.create_project_datasource(project_id, SourceDef(**to_snake(definition)))
    return {"guid": str(id)}


@router.post("/projects/{project}/anchors")
def new_project_anchor(project: str, definition: Dict) -> Dict:
    project_id = registry.get_entity_id(project)
    id = registry.create_project_anchor(project_id, AnchorDef(**to_snake(definition)))
    return {"guid": str(id)}


@router.post("/projects/{project}/anchors/{anchor}/features")
def new_project_anchor_feature(project: str, anchor: str, definition: Dict) -> Dict:
    project_id = registry.get_entity_id(project)
    anchor_id = registry.get_entity_id(anchor)
    id = registry.create_project_anchor_feature(project_id, anchor_id, AnchorFeatureDef(**to_snake(definition)))
    return {"guid": str(id)}


@router.post("/projects/{project}/derivedfeatures")
def new_project_derived_feature(project: str, definition: Dict) -> Dict:
    project_id = registry.get_entity_id(project)
    id = registry.create_project_derived_feature(project_id, DerivedFeatureDef(**to_snake(definition)))
    return {"guid": str(id)}


app.include_router(prefix=rp, router=router)
