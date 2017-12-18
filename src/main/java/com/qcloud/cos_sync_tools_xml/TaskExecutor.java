package com.qcloud.cos_sync_tools_xml;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import com.qcloud.cos.transfer.TransferManager;
import com.qcloud.cos_sync_tools_xml.meta.Config;

public class TaskExecutor {
    private static final Logger log = LoggerFactory.getLogger(TaskExecutor.class);

    public static final TaskExecutor instance = new TaskExecutor();
    private COSClient cosClient = null;
    private TransferManager transferManagerForBigFile = null;
    private TransferManager transferManagerForSmallFile = null;
    private static final int maxBigFileUploadExecutorNum = 4;
    private static final int maxSmallFileUploadExecutorNum = 64;
    private Semaphore semaphore = new Semaphore(maxBigFileUploadExecutorNum + maxSmallFileUploadExecutorNum);
    private ExecutorService pollTaskExecutor = Executors.newFixedThreadPool((maxBigFileUploadExecutorNum + maxSmallFileUploadExecutorNum) * 2) ;
    public TaskExecutor() {
        super();
        COSCredentials cred = new BasicCOSCredentials(Config.instance.getSecretId(), Config.instance.getSecretKey());
        ClientConfig config = new ClientConfig(new Region(Config.instance.getRegion()));
        config.setUserAgent("cos-sync-tools-v5.1");
        if (Config.instance.getEnableHttps() != 0) {
            config.setHttpProtocol(HttpProtocol.https);
        }
        cosClient = new COSClient(cred, config);
        transferManagerForBigFile = new TransferManager(cosClient, Executors.newFixedThreadPool(maxBigFileUploadExecutorNum));
        transferManagerForSmallFile = new TransferManager(cosClient, Executors.newFixedThreadPool(maxSmallFileUploadExecutorNum));        
    }

    private String convertLocalPathToCosPath(File localFile, String localPathFolder) {
        String localPath = localFile.getAbsolutePath();
        if (Config.instance.isWindowsSystem()) {
            localPath = localPath.replace('\\', '/');
        }
        if (localFile.isDirectory()) {
            localPath += "/";
        }
        String cosTargetDir = Config.instance.getCosPath();
        String cosPath = cosTargetDir + localPath.substring(localPathFolder.length());
        return cosPath;
    }

    public void AddLocalFileTask(File localFile, String localPathFolder) {
        String bucketName = Config.instance.getBucket();
        String key = convertLocalPathToCosPath(localFile, localPathFolder);
        String storageClass = Config.instance.getStorageClass();
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            String errMsg = String.format(
                    "semaphore acquire failed. localFilePath: %s, bucket: %s, key: %s, exception: %s",
                    localFile.getAbsolutePath(), bucketName, key, e.toString());
            log.error(errMsg);
            return;
        }

        try {
            UploadFileTask task = null;
            final long bigFileThreshold = 8 * 1024 * 1024;
            if (localFile.length() >= bigFileThreshold) {
                task = new UploadFileTask(localFile, bucketName, key, storageClass, transferManagerForBigFile, semaphore);
            } else {
                task = new UploadFileTask(localFile, bucketName, key, storageClass, transferManagerForSmallFile, semaphore);
            }
            pollTaskExecutor.submit(task);
        } catch (RejectedExecutionException e) {
            String errMsg = String.format(
                    "upload result to queue failed. localFilePath: %s, bucket: %s, key: %s, exception: %s",
                    localFile.getAbsolutePath(), bucketName, key, e.toString());
            log.error(errMsg);
        }
    }

    public void WaitForTaskOver() {
        pollTaskExecutor.shutdown();
        try {
            pollTaskExecutor.awaitTermination(1000, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            log.error(e.toString());
        }
        transferManagerForBigFile.shutdownNow();
        transferManagerForSmallFile.shutdownNow();
        cosClient.shutdown();
    }
}
