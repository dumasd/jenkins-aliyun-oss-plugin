package io.jenkins.plugins.aliyunoss;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import io.jenkins.plugins.aliyunoss.config.AliyunOSSConfig;
import io.jenkins.plugins.aliyunoss.config.AliyunOSSGlobalConfig;
import io.jenkins.plugins.aliyunoss.utils.AliyunOSSClient;
import io.jenkins.plugins.aliyunoss.utils.AliyunOSSException;
import io.jenkins.plugins.aliyunoss.utils.Logger;

import java.io.IOException;
import java.util.Optional;
import javax.servlet.ServletException;

import io.jenkins.plugins.aliyunoss.utils.Utils;
import jenkins.tasks.SimpleBuildStep;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

/**
 * @author Bruce.Wu
 * @date 2024-06-18
 */
@Setter
@Getter
public class AliyunOSSPublisher extends Recorder implements SimpleBuildStep {

    private String ossId;
    private String includes;
    private String excludes;
    private String pathPrefix;

    @DataBoundConstructor
    public AliyunOSSPublisher(String ossId, String includes) {
        this.ossId = ossId;
        this.includes = includes;
    }

    @DataBoundSetter
    public void setExcludes(String excludes) {
        this.excludes = excludes;
    }

    @DataBoundSetter
    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    @Override
    public void perform(
            @NonNull Run<?, ?> run,
            @NonNull FilePath workspace,
            @NonNull EnvVars env,
            @NonNull Launcher launcher,
            @NonNull TaskListener listener)
            throws InterruptedException, IOException {
        Logger logger = new Logger(listener);
        if (run.getResult() == Result.FAILURE) {
            logger.log("Job build fail. skip upload");
            return;
        }
        Optional<AliyunOSSConfig> ossConfigOp = AliyunOSSGlobalConfig.getConfig(ossId);
        if (ossConfigOp.isEmpty()) {
            logger.log("Aliyun OSS config not found. please check your config");
            return;
        }
        AliyunOSSConfig ossConfig = ossConfigOp.get();
        String includesFp = env.expand(includes);
        includesFp = StringUtils.trim(includesFp);
        String excludesFp = env.expand(excludes);
        excludesFp = StringUtils.trim(excludesFp);
        String jobName = run.getParent().getFullName();
        int buildId = run.getNumber();
        String ossPath = Utils.splicePath(ossConfig.getBasePrefix(), jobName, String.valueOf(buildId), pathPrefix);
        try {
            AliyunOSSClient.upload(logger, workspace, ossConfig, includesFp, excludesFp, ossPath);
        } catch (AliyunOSSException e) {
            e.printStackTrace(logger.getStream());
            logger.log("Upload to aliyun oss fail, the error message is:");
            logger.log(e.getMessage());
        }
    }

    @Symbol("ossUpload")
    @Extension
    public static class AliyunOSSPublisherDescriptor extends BuildStepDescriptor<Publisher> {

        @NonNull
        @Override
        public String getDisplayName() {
            return "Aliyun OSS Uploader";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @POST
        public FormValidation doCheckOssId(@QueryParameter("ossId") String ossId) throws IOException, ServletException {
            if (StringUtils.isBlank(ossId)) {
                return FormValidation.error("Please set OSS config id");
            }
            Optional<AliyunOSSConfig> ossConfigOp = AliyunOSSGlobalConfig.getConfig(ossId);
            if (ossConfigOp.isEmpty()) {
                return FormValidation.error("OSS config id not found");
            }
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckIncludes(@QueryParameter("includes") String includes)
                throws IOException, ServletException {
            if (StringUtils.isBlank(includes)) {
                return FormValidation.error("Please set includes");
            }
            return FormValidation.ok();
        }
    }
}
