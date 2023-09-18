/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package datart.server.service.impl;

import datart.core.base.consts.Const;
import datart.core.base.exception.Exceptions;
import datart.core.base.exception.NotAllowedException;
import datart.core.base.exception.NotFoundException;
import datart.core.base.exception.ParamException;
import datart.core.common.DateUtils;
import datart.core.common.UUIDGenerator;
import datart.core.entity.*;
import datart.core.mappers.ext.*;
import datart.security.base.PermissionInfo;
import datart.security.base.ResourceType;
import datart.security.base.SubjectType;
import datart.security.exception.PermissionDeniedException;
import datart.security.manager.shiro.ShiroSecurityManager;
import datart.security.util.PermissionHelper;
import datart.server.base.params.BaseCreateParam;
import datart.server.base.params.BaseUpdateParam;
import datart.server.base.params.FolderCreateParam;
import datart.server.base.params.FolderUpdateParam;
import datart.server.base.transfer.ImportStrategy;
import datart.server.base.transfer.TransferConfig;
import datart.server.base.transfer.model.FolderTransferModel;
import datart.server.service.BaseService;
import datart.server.service.FolderService;
import datart.server.service.RoleService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FolderServiceImpl extends BaseService implements FolderService {

    private final FolderMapperExt folderMapper;

    private final RoleService roleService;

    private final RelRoleResourceMapperExt rrrMapper;

    private final DashboardMapperExt dashboardMapper;

    private final DatachartMapperExt datachartMapper;

    private final StoryboardMapperExt storyboardMapper;

    public FolderServiceImpl(FolderMapperExt folderMapper,
                             DashboardMapperExt dashboardMapper,
                             DatachartMapperExt datachartMapper,
                             RoleService roleService,
                             RelRoleResourceMapperExt rrrMapper,
                             StoryboardMapperExt storyboardMapper) {
        this.folderMapper = folderMapper;
        this.roleService = roleService;
        this.rrrMapper = rrrMapper;
        this.dashboardMapper = dashboardMapper;
        this.datachartMapper = datachartMapper;
        this.storyboardMapper = storyboardMapper;
    }

    @Override
    public void requirePermission(Folder folder, int permission) {
        List<Role> roles = roleService.listUserRoles(folder.getOrgId(), getCurrentUser().getId());
        boolean hasPermission = roles.stream().anyMatch(role -> hasPermission(role, folder, permission));
        if (!hasPermission) {
            Exceptions.tr(PermissionDeniedException.class, "message.security.permission-denied",
                    folder.getRelType() + ":" + folder.getName() + ":" + ShiroSecurityManager.expand2StringPermissions(permission));
        }
    }

    private boolean hasPermission(Role role, Folder folder, int permission) {
        if (folder.getId() == null || rrrMapper.countRolePermission(folder.getId(), role.getId()) == 0) {
            Folder parent = folderMapper.selectByPrimaryKey(folder.getParentId());
            if (parent == null) {
                return securityManager.hasPermission(PermissionHelper.vizPermission(folder.getOrgId(), role.getId(),
                        ResourceType.FOLDER.name(), permission));
            } else {
                return hasPermission(role, parent, permission);
            }
        } else {
            return securityManager.hasPermission(PermissionHelper.vizPermission(folder.getOrgId(), role.getId(), folder.getId(), permission));
        }
    }

    @Override
    public List<Folder> listOrgFolders(String orgId) {
        List<Folder> folders = folderMapper.selectByOrg(orgId);
        if (securityManager.isOrgOwner(orgId)) {
            return folders;
        }

        Map<String, Folder> filtered = new HashMap<>();

        List<Folder> permitted = folders.stream()
                .filter(folder -> {
                    if (hasReadPermission(folder)) {
                        return true;
                    } else {
                        filtered.put(folder.getId(), folder);
                        return false;
                    }
                })
                .collect(Collectors.toList());
        while (!filtered.isEmpty()) {
            boolean updated = false;
            for (Folder folder : permitted) {
                Folder parent = filtered.remove(folder.getParentId());
                if (parent != null) {
                    permitted.add(parent);
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                break;
            }
        }

        return permitted;
    }

    @Override
    @Transactional
    public boolean delete(String id) {
        List<Folder> folders = folderMapper.selectByParentId(id);
        if (!CollectionUtils.isEmpty(folders)) {
            Exceptions.tr(NotAllowedException.class, "resource.folder.delete");
        }
        return FolderService.super.delete(id);
    }

    @Override
    public boolean checkUnique(String orgId, String parentId, String name) {
        if (!CollectionUtils.isEmpty(folderMapper.checkVizName(orgId, parentId, name))) {
            Exceptions.tr(ParamException.class, "error.param.exists.name");
        }
        return true;
    }

    @Override
    public Folder getVizFolder(String vizId, String relType) {
        return folderMapper.selectByRelTypeAndId(relType, vizId);
    }

    @Override
    public List<Folder> getAllParents(String folderId) {
        List<Folder> parents = new LinkedList<>();
        getParent(parents, folderId);
        return parents;
    }

    @Override
    public FolderTransferModel exportResource(TransferConfig transferConfig, Set<String> ids) {
        FolderTransferModel folderTransferModel = new FolderTransferModel();
        folderTransferModel.setFolders(folderMapper.selectByPrimaryKeys(ids));
        return folderTransferModel;
    }

    @Override
    public boolean importResource(FolderTransferModel model, ImportStrategy strategy, String orgId) {
        securityManager.requireOrgOwner(orgId);
        if (model == null || model.getFolders() == null) {
            return true;
        }
        switch (strategy) {
            case NEW:
            case ROLLBACK:
                importFolders(orgId, model.getFolders(), false);
                break;
            case OVERWRITE:
                importFolders(orgId, model.getFolders(), true);
                break;
        }
        return true;
    }

    private void importFolders(String orgId, List<Folder> folders, boolean deleteFirst) {
        if (deleteFirst) {
            for (Folder folder : folders) {
                try {
                    Folder retrieve = retrieve(folder.getId());
                    if (retrieve == null) {
                        continue;
                    }
                    if (!retrieve.getOrgId().equals(orgId)) {
                        Exceptions.msg("message.viz.import.database.conflict");
                    }
                    try {
                        delete(retrieve.getId(), false, false);
                    } catch (Exception ignore) {
                    }
                } catch (NotFoundException ignored) {
                }
            }
        }

        for (Folder folder : folders) {
            try {
                checkUnique(orgId, folder.getParentId(), folder.getName());
            } catch (Exception e) {
                folder.setName(DateUtils.withTimeString(folder.getName()));
            }
            folder.setOrgId(orgId);
            folderMapper.insert(folder);
        }
//        Folder root = null;
//        for (Folder folder : folders) {
//            if (folder.getParentId() == null) {
//                if (root != null) {
//                    Exceptions.msg("Folder has duplicate root nodes");
//                }
//                root = folder;
//            }
//        }
//        if (root == null) {
//            Exceptions.msg("Folder does not have a root node");
//        }
//        final Folder rootFolder = root;
//        Map<String, List<Folder>> folderMap = folders.stream()
//                .collect(Collectors.groupingBy(Folder::getId));
//        Map<String, List<Folder>> folderTree = folders.stream()
//                .filter(folder -> !folder.getId().equals(rootFolder.getId()))
//                .collect(Collectors.groupingBy(Folder::getParentId));
//
//        //  insert root
//        folderMapper.insert(rootFolder);
//        if (!folderTree.isEmpty()) {
//            for (String key : folderTree.keySet()) {
//                List<Folder> nodes = folderTree.get(key);
//            }
//        }
    }

//    private void insertFolderTree(Map<String, List<Folder>> folderTree, Map<String, Folder> folderMap, String parentId, String orgId) {
//        List<Folder> folders = folderTree.get(parentId);
//        if (CollectionUtils.isEmpty(folders)) {
//            return;
//        }
//        for (Folder folder : folders) {
//            Folder folder0 = folderMapper.selectByParentIdAndName(parentId, folder.getName());
//            if (folder0 != null) {
//                folder.setId(folder0.getId());
//                continue;
//            }
//            folder.setParentId(folderMap.get(folder.getParentId()).getId());
//            folderMapper.insert(folder);
//        }
//    }

    @Override
    public void replaceId(FolderTransferModel model
            , final Map<String, String> sourceIdMapping
            , final Map<String, String> viewIdMapping
            , final Map<String, String> chartIdMapping
            , final Map<String, String> boardIdMapping
            , final Map<String, String> folderIdMapping) {
        if (model != null && model.getFolders() != null) {
            for (Folder folder : model.getFolders()) {
                String newId = UUIDGenerator.generate();
                folderIdMapping.put(folder.getId(), newId);
                folder.setId(newId);
            }
            for (Folder folder : model.getFolders()) {
                folder.setParentId(folderIdMapping.get(folder.getParentId()));
            }
        }

    }

    private void getParent(List<Folder> list, String parentId) {
        Folder folder = folderMapper.selectByPrimaryKey(parentId);
        if (folder != null) {
            if (folder.getParentId() != null) {
                getParent(list, folder.getParentId());
            }
            list.add(folder);
        }
    }

    @Override
    @Transactional
    public boolean update(BaseUpdateParam baseUpdateParam) {

        FolderUpdateParam updateParam = (FolderUpdateParam) baseUpdateParam;
        Folder folder = retrieve(updateParam.getId());
        if (folder == null) {
            Exceptions.notFound("resource.folder");
        }
        requirePermission(folder, Const.MANAGE);
        // check name
        if (!folder.getName().equals(updateParam.getName())) {
            Folder check = new Folder();
            check.setOrgId(folder.getOrgId());
            check.setParentId(updateParam.getParentId());
            check.setName(updateParam.getName());
            checkUnique(check);
        }
        // update folder
        folder.setUpdateBy(getCurrentUser().getId());
        folder.setUpdateTime(new Date());
        folder.setIndex(updateParam.getIndex());
        folder.setName(updateParam.getName());
        folder.setParentId(updateParam.getParentId());
        folderMapper.updateByPrimaryKey(folder);

        // update viz
        switch (ResourceType.valueOf(folder.getRelType())) {
            case DASHBOARD:
                Dashboard dashboard = new Dashboard();
                dashboard.setId(folder.getRelId());
                dashboard.setName(folder.getName());
                dashboard.setUpdateBy(getCurrentUser().getId());
                dashboard.setUpdateTime(new Date());
                dashboardMapper.updateByPrimaryKeySelective(dashboard);
                break;
            case DATACHART:
                Datachart datachart = new Datachart();
                datachart.setUpdateBy(getCurrentUser().getId());
                datachart.setUpdateTime(new Date());
                datachart.setId(folder.getRelId());
                datachart.setName(folder.getName());
                datachart.setDescription(updateParam.getDescription());
                datachartMapper.updateByPrimaryKeySelective(datachart);
                break;
            case STORYBOARD:
                Storyboard storyboard = new Storyboard();
                storyboard.setUpdateBy(getCurrentUser().getId());
                storyboard.setUpdateTime(new Date());
                storyboard.setId(folder.getRelId());
                storyboard.setName(folder.getName());
                storyboardMapper.updateByPrimaryKeySelective(storyboard);
                break;
        }
        return true;
    }

    @Override
    @Transactional
    public Folder create(BaseCreateParam createParam) {
        FolderCreateParam folderCreate = (FolderCreateParam) createParam;
        requireExists(folderCreate.getOrgId(), Organization.class);
        // check name
        Folder check = new Folder();
        check.setOrgId(folderCreate.getOrgId());
        check.setParentId(folderCreate.getParentId());
        check.setName(folderCreate.getName());
        checkUnique(check);
        // insert folder
        Folder folder = new Folder();
        BeanUtils.copyProperties(createParam, folder);
        folder.setCreateBy(getCurrentUser().getId());
        folder.setCreateTime(new Date());
        folder.setId(UUIDGenerator.generate());
        folder.setRelType(ResourceType.FOLDER.name());
        requirePermission(folder, Const.CREATE);
        // insert permissions
        if (!CollectionUtils.isEmpty(folderCreate.getPermissions())) {
            roleService.grantPermission(folderCreate.getPermissions());
        }
        grantDefaultPermission(folder);
        folderMapper.insert(folder);
        return folder;
    }

    private boolean hasReadPermission(Folder folder) {
        try {
            requirePermission(folder, Const.MANAGE);
            return true;
        } catch (Exception ignored) {
        }
        try {
            requirePermission(folder, Const.READ);
            if (ResourceType.DATACHART.name().equals(folder.getRelType())) {
                return retrieve(folder.getRelId(), Datachart.class).getStatus() == Const.VIZ_PUBLISH;
            } else if (ResourceType.DASHBOARD.name().equals(folder.getRelType())) {
                return retrieve(folder.getRelId(), Dashboard.class).getStatus() == Const.VIZ_PUBLISH;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    @Transactional
    public void grantDefaultPermission(Folder folder) {
        if (securityManager.isOrgOwner(folder.getOrgId())) {
            return;
        }
        PermissionInfo permissionInfo = new PermissionInfo();
        permissionInfo.setOrgId(folder.getOrgId());
        permissionInfo.setSubjectType(SubjectType.USER);
        permissionInfo.setSubjectId(getCurrentUser().getId());
        permissionInfo.setResourceType(ResourceType.VIZ);
        permissionInfo.setResourceId(folder.getId());
        permissionInfo.setPermission(Const.CREATE);
        roleService.grantPermission(Collections.singletonList(permissionInfo));
    }

}
