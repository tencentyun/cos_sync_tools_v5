package com.qcloud.cos_sync_tools_xml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.StorageClass;
import com.qcloud.cos.model.UploadResult;
import com.qcloud.cos.transfer.TransferManager;
import com.qcloud.cos.transfer.Upload;
import com.qcloud.cos_sync_tools_xml.meta.DbRecord;
import com.qcloud.cos_sync_tools_xml.meta.TaskStatics;

public class UploadFileTask implements Runnable {

    private static Logger log = LoggerFactory.getLogger(UploadFileTask.class);

    private File localFile;
    private String bucketName;
    private String key;
    private String storageClass;
    private TransferManager transferManager;
    private Semaphore semaphore;


    public UploadFileTask(File localFile, String bucketName, String key, String storageClass,
            TransferManager transferManager, Semaphore semaphore) {
        super();
        this.localFile = localFile;
        this.bucketName = bucketName;
        this.key = key;
        this.storageClass = storageClass;
        this.transferManager = transferManager;
        this.semaphore = semaphore;
    }

    public void run() {
        try {
            doExecuteUploadTask();
        } finally {
            this.semaphore.release();
        }
    }

    private void doExecuteUploadTask() {
        PutObjectRequest putObjectRequest = null;
        if (localFile.isDirectory()) {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(0L);
            putObjectRequest = new PutObjectRequest(bucketName, key,
                    new ByteArrayInputStream(new byte[0]), objectMetadata);
        } else {
            putObjectRequest = new PutObjectRequest(bucketName, key, localFile);
        }
        
        if (storageClass.equals("standard")) {
            putObjectRequest.setStorageClass(StorageClass.Standard);
        } else if (storageClass.equals("standard_ia")) {
            putObjectRequest.setStorageClass(StorageClass.Standard_IA);
        } else if (storageClass.equals("nearline")) {
            putObjectRequest.setStorageClass(StorageClass.NearLine);
        }

        int maxRetryTime = 5;
        int retryIndex = 0;
        while (retryIndex <= maxRetryTime) {
            ++retryIndex;
            try {
                Upload upload = transferManager.upload(putObjectRequest);
                UploadResult uploadResult = upload.waitForUploadResult();
                DbRecord.instance.update(localFile);
                TaskStatics.instance.addUploadFileOk();
                String printMsg =
                        String.format("[ok] [localpath: %s]", localFile.getAbsolutePath());
                System.out.println(printMsg);
                String infoMsg = String.format(
                        "[ok] [localpath: %s] [cosPath: %s] [length: %d] [ETag: %s]",
                        localFile.getAbsolutePath(), key,
                        localFile.isDirectory() ? 0 : localFile.length(), uploadResult.getETag());
                log.info(infoMsg);
                return;
            } catch (CosClientException e) {
                if (retryIndex > maxRetryTime) {
                    String printMsg =
                            String.format("[fail] [localpath: %s]", localFile.getAbsolutePath());
                    System.out.println(printMsg);
                    String errMsg = String.format(
                            "[fail] [localpath: %s] [cosPath: %s] [length: %d] [Exception: %s]",
                            localFile.getAbsolutePath(), key,
                            localFile.isDirectory() ? 0 : localFile.length(), e.toString());
                    log.error(errMsg);
                    TaskStatics.instance.addUploadFileFail();
                    return;
                } else {
                    try {
                        Thread.sleep(200L);
                    } catch (InterruptedException e1) {
                        log.error("upload localFile occur a interrupted exception. localFilePath: "
                                + localFile.getAbsolutePath());
                    }
                }
            } catch (Exception e) {
                log.error("upload localFile occur a unknown exception. localFilePath: "
                        + localFile.getAbsolutePath() + ", exception: " + e.toString());
                return;
            }
        }
    }
}
