package com.netease.arctic.server.exception;

import com.netease.arctic.ams.api.TableIdentifier;
import com.netease.arctic.server.table.ServerTableIdentifier;

public class ObjectNotExistsException extends ArcticRuntimeException {
  public ObjectNotExistsException(String object) {
    super(object + " not exists");
  }

  public ObjectNotExistsException(TableIdentifier tableIdentifier) {
    super(getObjectName(tableIdentifier) + " not exists");
  }

  public ObjectNotExistsException(ServerTableIdentifier tableIdentifier) {
    super(getObjectName(tableIdentifier) + " not exists");
  }
}
