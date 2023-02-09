package com.qcloud.cos.auth;

import com.qcloud.cos.exception.CosClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class InstanceMetadataCredentialsEndpointProvider extends CredentialsEndpointProvider {
    private static final Logger LOG = LoggerFactory.getLogger(InstanceMetadataCredentialsEndpointProvider.class);

    public enum Instance {
        CPM("http://bm.metadata.tencentyun.com", "/meta-data/cam/security-credentials"),
        CVM("http://metadata.tencentyun.com", "/meta-data/cam/security-credentials"),
        EMR("http://localhost:2888", "/accesskey");

        final String METADATA_SERVICE_URL;
        final String METADATA_CREDENTIALS_RESOURCE;

        Instance(String METADATA_SERVICE_URL, String METADATA_CREDENTIALS_RESOURCE) {
            this.METADATA_SERVICE_URL = METADATA_SERVICE_URL;
            this.METADATA_CREDENTIALS_RESOURCE = METADATA_CREDENTIALS_RESOURCE;
        }
    }

    private final Instance instance;
    private final String roleName;

    public InstanceMetadataCredentialsEndpointProvider(Instance instance) {
        this(instance, null);
    }

    public InstanceMetadataCredentialsEndpointProvider(Instance instance, String roleName) {
        this.instance = instance;
        this.roleName = roleName;
    }

    @Override
    public URI getCredentialsEndpoint() throws URISyntaxException, IOException {
        if (null != this.roleName && !this.roleName.isEmpty()) {
            return new URI(this.instance.METADATA_SERVICE_URL + this.instance.METADATA_CREDENTIALS_RESOURCE + "/" + this.roleName);
        }

        // Try to get a valid role.
        LOG.debug("The role name is not specified. Trying to get a valid role name from the instance.");
        String roles =
                InstanceCredentialsUtils.getInstance().readResource(
                        new URI(this.instance.METADATA_SERVICE_URL + this.instance.METADATA_CREDENTIALS_RESOURCE));
        String[] roleList = roles.trim().split("\n");
        if (0 == roleList.length) {
            throw new CosClientException("Unable to load the credentials path. No valid cam role was found.");
        }

        LOG.info("Use the role [{}] to obtain the credentials.", roleList[0]);
        return new URI(this.instance.METADATA_SERVICE_URL + this.instance.METADATA_CREDENTIALS_RESOURCE + "/" + roleList[0]);
    }
}
