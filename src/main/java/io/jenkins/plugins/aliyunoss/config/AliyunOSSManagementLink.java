package io.jenkins.plugins.aliyunoss.config;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.ManagementLink;
import hudson.util.FormApply;
import java.io.IOException;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.verb.POST;

@Extension(ordinal = Double.MAX_VALUE)
public class AliyunOSSManagementLink extends ManagementLink {
    @Override
    public String getIconFileName() {
        return "/plugin/jenkins-aliyun-oss-plugin/images/logo.svg";
    }

    @Override
    public String getDisplayName() {
        return "Aliyun OSS";
    }

    @Override
    public String getUrlName() {
        return "aliyunoss";
    }

    @Override
    public String getDescription() {
        return "Aliyun OSS plugin";
    }

    @POST
    public void doConfigure(StaplerRequest req, StaplerResponse res)
            throws ServletException, Descriptor.FormException, IOException {
        if (Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
            getAliyunOSSGlobalConfigDescriptor().configure(req, req.getSubmittedForm());
            FormApply.success(req.getContextPath() + "/manage").generateResponse(req, res, null);
        }
    }

    public Descriptor<AliyunOSSGlobalConfig> getAliyunOSSGlobalConfigDescriptor() {
        return Jenkins.get().getDescriptorByType(AliyunOSSGlobalConfig.class);
    }
}
