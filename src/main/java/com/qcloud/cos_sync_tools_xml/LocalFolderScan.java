package com.qcloud.cos_sync_tools_xml;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qcloud.cos_sync_tools_xml.meta.Config;
import com.qcloud.cos_sync_tools_xml.meta.DbRecord;
import com.qcloud.cos_sync_tools_xml.meta.TaskStatics;

/**
 * 扫描目录的子成员, 并将文件和目录分别放在对应的list中
 * 
 * @author chengwu
 *
 */
public class LocalFolderScan {

    private static final Logger log = LoggerFactory.getLogger(LocalFolderScan.class);
    private String localFolderPath;

    public LocalFolderScan(String localFolderPath) {
        super();
        this.localFolderPath = localFolderPath;
    }

    public void ScanLocalFolder() {
        SimpleFileVisitor<Path> finder = new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                String dirPath = dir.toString();
                // 格式化dirpath, 确保以/结尾, 对于windows, 替换其中的\分隔符
                /*
                if (Config.instance.isWindowsSystem()) {
                    dirPath = dirPath.replace('\\', '/');
                }
                if (!dirPath.endsWith("/")) {
                    dirPath += "/";
                }
                if (DbRecord.instance.queryIfNeedToUpload(new File(dirPath))) {
                    TaskExecutor.instance.AddLocalFileTask(new File(dirPath), localFolderPath);
                } else {
                    TaskStatics.instance.addSkipFile();
                }
                */
                return super.preVisitDirectory(dir, attrs);
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                String filePath = file.toString();
                // 格式化filepath, 确保以/结尾, 对于windows, 替换其中的\分隔符
                if (Config.instance.isWindowsSystem()) {
                    filePath = filePath.replace('\\', '/');
                }
                if (DbRecord.instance.queryIfNeedToUpload(new File(filePath))) {
                    TaskExecutor.instance.AddLocalFileTask(new File(filePath), localFolderPath);
                } else {
                    TaskStatics.instance.addSkipFile();
                }
                return super.visitFile(file, attrs);
            }
        };

        try {
            java.nio.file.Files.walkFileTree(Paths.get(localFolderPath), finder);
        } catch (IOException e) {
            log.error("walk file tree error", e);
        }
    }

}
