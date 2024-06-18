package io.jenkins.plugins.aliyunoss.config;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;
import io.jenkins.plugins.aliyunoss.utils.AliyunOSSClient;
import io.jenkins.plugins.aliyunoss.utils.Constants;
import io.jenkins.plugins.aliyunoss.utils.Utils;
import java.io.IOException;
import java.util.UUID;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

@Setter
@Getter
@ToString
@NoArgsConstructor
public class AliyunOSSConfig implements Describable<AliyunOSSConfig> {

    private String id;
    private String endpoint;
    private String bucket;
    private String accessKey;
    private Secret secretKey;
    private String basePrefix;

    @DataBoundConstructor
    public AliyunOSSConfig(
            String id, String endpoint, String bucket, String accessKey, String secretKey, String basePrefix) {
        this.id = StringUtils.isBlank(id) ? UUID.randomUUID().toString() : id;
        this.endpoint = endpoint;
        this.bucket = bucket;
        this.accessKey = accessKey;
        this.secretKey = Secret.fromString(secretKey);
        this.basePrefix = basePrefix;
    }

    public String getId() {
        if (StringUtils.isBlank(id)) {
            setId(UUID.randomUUID().toString());
        }
        return id;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = Secret.fromString(secretKey);
    }

    public String getSecretKey() {
        if (secretKey == null) {
            return null;
        }
        return secretKey.getPlainText();
    }

    public FormValidation doCheckBucket(@QueryParameter String val) throws IOException, ServletException {
        if (Utils.isNullOrEmpty(val)) {
            return FormValidation.error("Bucket不能为空！");
        }
        try {
            AliyunOSSClient.validateOSSBucket(endpoint, accessKey, secretKey.getPlainText(), val);
        } catch (Exception e) {
            return FormValidation.error(e.getMessage());
        }
        return FormValidation.ok();
    }

    @Override
    public Descriptor<AliyunOSSConfig> getDescriptor() {
        return Jenkins.get().getDescriptorByType(AliyunOSSConfigDescriptor.class);
    }

    @Extension
    public static class AliyunOSSConfigDescriptor extends Descriptor<AliyunOSSConfig> {

        public FormValidation doTest(
                @QueryParameter String endpoint,
                @QueryParameter String bucket,
                @QueryParameter String accessKey,
                @QueryParameter String secretKey,
                @QueryParameter String basePrefix) {
            if (Utils.isNullOrEmpty(endpoint)) {
                return FormValidation.error("Please input endpoint");
            }
            if (Utils.isNullOrEmpty(bucket)) {
                return FormValidation.error("Please input bucket");
            }
            if (Utils.isNullOrEmpty(accessKey)) {
                return FormValidation.error("Please input accessKey");
            }
            if (Utils.isNullOrEmpty(secretKey)) {
                return FormValidation.error("Please input secretKey");
            }
            if (StringUtils.indexOf(basePrefix, Constants.SLASH) == 0) {
                return FormValidation.error("Base Prefix can't begin with '/'");
            }
            try {
                AliyunOSSClient.validateOSSBucket(endpoint, accessKey, secretKey, bucket);
            } catch (Exception e) {
                return FormValidation.error(e.getMessage());
            }
            return FormValidation.ok("Validate Success!");
        }
    }
}
