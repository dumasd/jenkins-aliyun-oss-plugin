package io.jenkins.plugins.aliyunoss;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.ListObjectsV2Request;
import com.aliyun.oss.model.ListObjectsV2Result;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.OSSObjectSummary;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import io.jenkins.plugins.aliyunoss.config.AliyunOSSConfig;
import io.jenkins.plugins.aliyunoss.config.AliyunOSSGlobalConfig;
import io.jenkins.plugins.aliyunoss.utils.AliyunOSSClient;
import io.jenkins.plugins.aliyunoss.utils.AliyunOSSException;
import io.jenkins.plugins.aliyunoss.utils.Logger;
import io.jenkins.plugins.aliyunoss.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import javax.servlet.ServletException;

import jenkins.MasterToSlaveFileCallable;
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
public class AliyunOSSDownloader extends Builder implements SimpleBuildStep {
    /**
     * OSS ID
     */
    private String ossId;
    /**
     * The path to download
     */
    private String path;
    /**
     * Download location
     */
    private String location;
    /**
     * Overwrite existing file
     */
    private boolean force = true;

    @DataBoundConstructor
    public AliyunOSSDownloader(String ossId, String path) {
        this.ossId = ossId;
        this.path = path;
    }

    @DataBoundSetter
    public void setForce(boolean force) {
        this.force = force;
    }

    @DataBoundSetter
    public void setLocation(String location) {
        this.location = location;
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
        Optional<AliyunOSSConfig> ossConfigOp = AliyunOSSGlobalConfig.getConfig(ossId);
        if (ossConfigOp.isEmpty()) {
            logger.log("Aliyun OSS config not found. please check your config");
            return;
        }
        AliyunOSSConfig ossConfig = ossConfigOp.get();
        String locationFp = env.expand(location);
        final FilePath target = workspace.child(locationFp);
        if (target.exists()) {
            if (force) {
                if (target.isDirectory()) {
                    target.deleteRecursive();
                } else {
                    target.delete();
                }
            } else {
                logger.log("Download failed due to existing target file; set force=true to overwrite target file");
                return;
            }
        }
        String ossPath = Utils.splicePath(ossConfig.getBasePrefix(), env.expand(path));
        target.act(new RemoteDownloader(logger, ossConfig, ossPath));
    }

    private static class RemoteDownloader extends MasterToSlaveFileCallable<Void> {

        private static final long serialVersionUID = 1L;

        private final transient Logger logger;
        private final transient AliyunOSSConfig ossConfig;
        private final String path;

        private RemoteDownloader(Logger logger, AliyunOSSConfig ossConfig, String path) {
            this.logger = logger;
            this.ossConfig = ossConfig;
            this.path = path;
        }

        @Override
        public Void invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            logger.log(
                    "Downloading from aliyun oss. endpoint: %s, bucket: %s, path: %s",
                    ossConfig.getEndpoint(), ossConfig.getBucket(), path);
            OSSClient client;
            try {
                client = AliyunOSSClient.getClient(
                        ossConfig.getEndpoint(),
                        ossConfig.getAccessKey(),
                        ossConfig.getSecretKey(),
                        ossConfig.getBucket());
            } catch (AliyunOSSException e) {
                e.printStackTrace(logger.getStream());
                throw new IOException(e.getMessage());
            }
            if (Objects.isNull(path) || path.endsWith("/")) {
                // 文件夹下所有内容
                ListObjectsV2Request listObjReq = new ListObjectsV2Request(ossConfig.getBucket(), path);
                listObjReq.setMaxKeys(100);
                ListObjectsV2Result listObjectResult = client.listObjectsV2(listObjReq);
                if (listObjectResult.getKeyCount() > 100) {
                    throw new IOException("OSS file count too much > 100, please check your path");
                }
                for (OSSObjectSummary summary : listObjectResult.getObjectSummaries()) {
                    String savePath = Utils.removePrefix(listObjectResult.getPrefix(), summary.getKey());
                    OSSObject object = client.getObject(ossConfig.getBucket(), summary.getKey());
                    File saveFile = new File(f, savePath);
                    saveFile(saveFile, object);
                    logger.log(
                            "Downloaded file and saved. objectKey: %s, savePath: %s",
                            summary.getKey(), saveFile.getPath());
                }
            } else {
                // 下载文件
                OSSObject object = client.getObject(ossConfig.getBucket(), path);
                File saveFile = f;
                if (f.isDirectory()) {
                    String fileName = Utils.getFileName(object.getKey());
                    saveFile = new File(f, fileName);
                    logger.log(
                            "Downloaded file and saved. objectKey: %s, savePath: %s",
                            object.getKey(), saveFile.getPath());
                }
                saveFile(saveFile, object);
            }
            return null;
        }

        private void saveFile(File saveFile, OSSObject object) throws IOException {
            File parent = saveFile.getParentFile();
            if (Objects.nonNull(parent) && !parent.exists()) {
                if (!parent.mkdirs()) {
                    throw new IOException("Make dir error");
                }
            }
            FileOutputStream fos = new FileOutputStream(saveFile);
            try (InputStream is = object.getObjectContent()) {
                is.transferTo(fos);
                fos.flush();
                fos.close();
            }
        }
    }

    @Symbol("ossDownload")
    @Extension
    public static class AliyunOSSDownloaderDescriptor extends BuildStepDescriptor<Builder> {
        @NonNull
        @Override
        public String getDisplayName() {
            return "Aliyun OSS Downloader";
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
        public FormValidation doCheckPath(@QueryParameter("path") String path) throws IOException, ServletException {
            if (StringUtils.isBlank(path)) {
                return FormValidation.error("Please set OSS path");
            }
            return FormValidation.ok();
        }

    }
}
