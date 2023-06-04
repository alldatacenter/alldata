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
import datart.core.base.consts.FileOwner;
import datart.core.base.exception.Exceptions;
import datart.core.common.Application;
import datart.core.common.FileUtils;
import datart.core.entity.*;
import datart.server.service.BaseService;
import datart.server.service.FileService;
import datart.server.service.OrgService;
import datart.server.service.UserService;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Service
public class FileServiceImpl extends BaseService implements FileService {

    @Override
    public String uploadFile(FileOwner fileOwner, String ownerId, MultipartFile file, String fileName) throws IOException {
        switch (fileOwner) {
            case DASHBOARD:
            case DATACHART:
                return saveVizImage(fileOwner, ownerId, file, fileName);
            case USER_AVATAR:
                return updateUserAvatar(ownerId, file);
            case ORG_AVATAR:
                return updateOrgAvatar(ownerId, file);
            case DATA_SOURCE:
                return saveAsDatasource(fileOwner, ownerId, file);
            default:
                Exceptions.msg("unknown file type " + fileOwner);
        }
        return null;
    }


    @Override
    public boolean deleteFiles(FileOwner fileOwner, String ownerId) {
        try {
            switch (fileOwner) {
                case ORG_AVATAR:
                    securityManager.requireOrgOwner(ownerId);
                    OrgService orgService = Application.getBean(OrgService.class);
                    orgService.updateAvatar(ownerId, "");
                    break;
                case USER_AVATAR:
                    requireExists(ownerId, User.class);
                    UserService userService = Application.getBean(UserService.class);
                    userService.updateAvatar("");
                    break;
                default:
                    break;
            }
            String path = FileUtils.concatPath(Application.getFileBasePath(), fileOwner.getPath(), ownerId);
            return FileSystemUtils.deleteRecursively(new File(path));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getBasePath(FileOwner owner, String id) {
        return FileUtils.concatPath(Application.getFileBasePath(), owner.getPath(), id);
    }

    private String saveVizImage(FileOwner fileOwner, String ownerId, MultipartFile file, String fileName) throws IOException {
        switch (fileOwner) {
            case DASHBOARD:
                requireExists(ownerId, Dashboard.class);
                break;
            case DATACHART:
                requireExists(ownerId, Datachart.class);
                break;
        }
        String filePath = FileUtils.concatPath(fileOwner.getPath(), ownerId, StringUtils.isBlank(fileName) ? file.getOriginalFilename() : fileName);
        String fullPath = FileUtils.withBasePath(filePath);
        FileUtils.mkdirParentIfNotExist(fullPath);
        file.transferTo(new File(fullPath));
        return filePath;
    }

    private String updateUserAvatar(String userId, MultipartFile file) throws IOException {

        requireExists(userId, User.class);

        String filePath = FileUtils.concatPath(FileOwner.USER_AVATAR.getPath(), userId, file.getOriginalFilename());

        String fullPath = FileUtils.withBasePath(filePath);

        FileUtils.mkdirParentIfNotExist(fullPath);

        Thumbnails.of(file.getInputStream())
                .size(Const.IMAGE_WIDTH, Const.IMAGE_HEIGHT)
                .toFile(fullPath);

        UserService userService = Application.getBean(UserService.class);

        userService.updateAvatar(filePath);

        return filePath;
    }

    private String updateOrgAvatar(String orgId, MultipartFile file) throws IOException {

        requireExists(orgId, Organization.class);

        String filePath = FileUtils.concatPath(FileOwner.ORG_AVATAR.getPath(), orgId, file.getOriginalFilename());

        String fullPath = FileUtils.withBasePath(filePath);


        FileUtils.mkdirParentIfNotExist(fullPath);

        Thumbnails.of(file.getInputStream())
                .size(Const.IMAGE_WIDTH, Const.IMAGE_HEIGHT)
                .toFile(fullPath);

        OrgService orgService = Application.getBean(OrgService.class);

        orgService.updateAvatar(orgId, filePath);

        return filePath;
    }

    public String saveAsDatasource(FileOwner fileOwner, String ownerId, MultipartFile file) throws IOException {

        requireExists(ownerId, Source.class);

        String filePath = FileUtils.concatPath(fileOwner.getPath(), ownerId, System.currentTimeMillis() + "-" + file.getOriginalFilename());

        String fullPath = FileUtils.withBasePath(filePath);

        FileUtils.mkdirParentIfNotExist(fullPath);

        file.transferTo(new File(fullPath));

        return filePath;
    }

}
