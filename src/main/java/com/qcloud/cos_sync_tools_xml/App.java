package com.qcloud.cos_sync_tools_xml;

import com.qcloud.cos_sync_tools_xml.meta.Config;
import com.qcloud.cos_sync_tools_xml.meta.DbRecord;
import com.qcloud.cos_sync_tools_xml.meta.TaskStatics;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) {
        // 初始化配置
        Config.instance.init();
        if (!Config.instance.isValidConfig()) {
            System.err.println(Config.instance.getInitConfigErr());
            return;
        }


        if (!DbRecord.instance.init()) {
            System.err.println("init db record failed. please check error log!");
            return;
        }

        TaskStatics.instance.beginCollectStatics();
        String[] localPathArray = Config.instance.getLocalPath();
        for (String localFolder : localPathArray) {
            LocalFolderScan localFolderScan = new LocalFolderScan(localFolder);
            localFolderScan.ScanLocalFolder();
        }
        TaskExecutor.instance.WaitForTaskOver();
        DbRecord.instance.shutdown();
        TaskStatics.instance.endCollectStatics();
    }
}
