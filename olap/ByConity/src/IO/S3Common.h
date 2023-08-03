#pragma once

#if !defined(ARCADIA_BUILD)
#include <Common/config.h>
#endif

#if USE_AWS_S3

#include <common/types.h>
#include <aws/core/Aws.h>  // Y_IGNORE
#include <aws/core/client/ClientConfiguration.h> // Y_IGNORE
#include <aws/s3/S3Errors.h>
#include <aws/s3/model/HeadObjectResult.h>
#include <aws/s3/model/GetObjectResult.h>
#include <IO/S3/PocoHTTPClient.h>
#include <IO/BufferBase.h>
#include <Poco/URI.h>

namespace Aws::S3
{
    class S3Client;
}

namespace DB
{
    class RemoteHostFilter;
    struct HttpHeader;
    using HeaderCollection = std::vector<HttpHeader>;
}

namespace DB::S3
{

/// For s3 request exception, contains s3 request's error code
class S3Exception: public Exception
{
public:
    explicit S3Exception(const Aws::S3::S3Error& s3_err, const String& extra_msg = "");

    const char* name() const throw() override { return "DB::S3::S3Exception"; }

    Aws::S3::S3Errors s3Err() const { return error_type; }

    static String formatS3Error(const Aws::S3::S3Error& err, const String& extra);

private:
    const char* className() const throw() override { return "DB::S3::S3Exception"; }

    Aws::S3::S3Errors error_type;
};

class ClientFactory
{
public:
    ~ClientFactory();

    static ClientFactory & instance();

    std::shared_ptr<Aws::S3::S3Client> create(
        const PocoHTTPClientConfiguration & cfg,
        bool is_virtual_hosted_style,
        const String & access_key_id,
        const String & secret_access_key,
        const String & server_side_encryption_customer_key_base64,
        HeaderCollection headers,
        bool use_environment_credentials,
        bool use_insecure_imds_request);

    PocoHTTPClientConfiguration createClientConfiguration(
        const String & force_region,
        const RemoteHostFilter & remote_host_filter,
        unsigned int s3_max_redirects);

private:
    ClientFactory();

private:
    Aws::SDKOptions aws_options;
};

/**
 * Represents S3 URI.
 *
 * The following patterns are allowed:
 * s3://bucket/key
 * http(s)://endpoint/bucket/key
 */
struct URI
{
    Poco::URI uri;
    // Custom endpoint if URI scheme is not S3.
    String endpoint;
    String bucket;
    String key;
    String storage_name;

    bool is_virtual_hosted_style;

    explicit URI(const Poco::URI & uri_);
};

class S3Config
{
public:
    explicit S3Config(const String& ini_file_path);

    S3Config(const String& endpoint_, const String& region_, const String& bucket_,
        const String& ak_id_, const String& ak_secret_, const String& root_prefix_,
        int connect_timeout_ms_ = 10000, int request_timeout_ms_ = 30000,
        int max_redirects_ = 10, int max_connections_ = 100):
            max_redirects(max_redirects_), connect_timeout_ms(connect_timeout_ms_),
            request_timeout_ms(request_timeout_ms_), max_connections(max_connections_),
            endpoint(endpoint_), region(region_), bucket(bucket_), ak_id(ak_id_),
            ak_secret(ak_secret_), root_prefix(root_prefix_) {}

    S3Config(const Poco::Util::AbstractConfiguration& cfg, const String& cfg_prefix);

    void collectCredentialsFromEnv();

    std::shared_ptr<Aws::S3::S3Client> create() const;

    int max_redirects;
    int connect_timeout_ms;
    int request_timeout_ms;
    int max_connections;
    String endpoint;
    String region;
    String bucket;
    String ak_id;
    String ak_secret;
    String root_prefix;
};

class S3Util
{
public:
    S3Util(const std::shared_ptr<Aws::S3::S3Client>& client_, const String& bucket_):
        client(client_), bucket(bucket_) {}

    // Access object metadata
    // NOTE(wsy) Interface using head method won't throw exact exception
    size_t getObjectSize(const String& key) const;
    std::map<String, String> getObjectMeta(const String& key) const;
    bool exists(const String& key) const;

    // Read object
    bool read(const String& key, size_t offset, size_t size, BufferBase::Buffer& buffer) const;

    std::tuple<bool, String, std::vector<String>> listObjectsWithPrefix(
        const String& prefix, const std::optional<String>& token, int limit = 1000) const;

    // Write object
    String createMultipartUpload(const String& key,
        const std::optional<std::map<String, String>>& meta = std::nullopt,
        const std::optional<std::map<String, String>>& tags = std::nullopt) const;
    void completeMultipartUpload(const String& key, const String& upload_id,
        const std::vector<String>& etags) const;
    void abortMultipartUpload(const String& key, const String& upload_id) const;
    String uploadPart(const String& key, const String& upload_id, size_t part_number,
        size_t size, const std::shared_ptr<Aws::StringStream>& stream) const;

    void upload(const String& key, size_t size, const std::shared_ptr<Aws::StringStream>& stream,
        const std::optional<std::map<String, String>>& metadata = std::nullopt,
        const std::optional<std::map<String, String>>& tags = std::nullopt) const;

    // Delete object
    void deleteObject(const String& key, bool check_existence = true) const;
    void deleteObjects(const std::vector<String>& keys) const;
    void deleteObjectsInBatch(const std::vector<String>& key, size_t batch_size = 1000) const;
    void deleteObjectsWithPrefix(const String& prefix,
        const std::function<bool(const S3Util&, const String&)>& filter,
        size_t batch_size = 1000) const;

    // Internal client and info
    const std::shared_ptr<Aws::S3::S3Client>& getClient() const { return client; }
    const String& getBucket() const { return bucket; }

private:
    static String urlEncodeMap(const std::map<String, String>& mp);

    Aws::S3::Model::HeadObjectResult headObject(const String& key) const;
    Aws::S3::Model::GetObjectResult headObjectByGet(const String& key) const;

    std::shared_ptr<Aws::S3::S3Client> client;
    const String bucket;
};

}

#endif
