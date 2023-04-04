package datart.server.service;

import datart.core.base.consts.FileOwner;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileService {

    String uploadFile(FileOwner fileOwner, String relId, MultipartFile file, String fileName) throws IOException;

    boolean deleteFiles(FileOwner fileOwner, String relId);

    String getBasePath(FileOwner owner,String id);

}