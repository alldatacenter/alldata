package datart.server.service;

import datart.core.entity.Folder;
import datart.core.mappers.FolderMapper;
import datart.server.base.transfer.model.FolderTransferModel;

import java.util.List;

public interface FolderService extends BaseCRUDService<Folder, FolderMapper>, ResourceTransferService<Folder, FolderTransferModel, FolderTransferModel, Folder> {

    List<Folder> listOrgFolders(String orgId);

    boolean checkUnique(String orgId, String parentId, String name);

    Folder getVizFolder(String vizId, String relType);

    List<Folder> getAllParents(String folderId);

}