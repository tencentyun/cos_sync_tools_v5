package com.qcloud.cos_sync_tools_xml.meta;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用于统计各类任务的数量，并将每次执行的任务统计结果打印到本机, 并写入数据库
 * 
 * @author chengwu
 *
 */
public class TaskStatics {
    public static final TaskStatics instance = new TaskStatics();
    private static final Logger log = LoggerFactory.getLogger(TaskStatics.class);

    private DateFormat timeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private long startTime = 0; // 同步开始时间 UNIX时间戳
    private long endTime = 0; // 同步结束时间 UNIX时间戳

    // 上传文件的总量, 成功量, 失败量
    private AtomicLong uploadFileSumCnt = new AtomicLong(0L);
    private AtomicLong uploadFileOkCnt = new AtomicLong(0L);
    private AtomicLong uploadFileFailCnt = new AtomicLong(0L);
    private AtomicLong uploadFileSkipCnt = new AtomicLong(0L);

    private TaskStatics() {}

    // 更新记录
    private void printStatics() {
        String opStatus = "";
        if (this.uploadFileSumCnt.get() == this.uploadFileOkCnt.get()) {
            opStatus = "ALL_OK";
        } else if (this.uploadFileSumCnt.get() == this.uploadFileFailCnt.get()) {
            opStatus = "ALL_FAIL";
        } else {
            opStatus = "PART_OK";
        }

        String printStr = "\n\nsync over! op statistics:";
        System.out.println(printStr);
        log.info(printStr);
        printStr = String.format("%30s : %s", "op_status", opStatus);
        System.out.println(printStr);
        log.info(printStr);
        printStr = String.format("%30s : %d", "upload_file_ok", this.uploadFileOkCnt.get());
        System.out.println(printStr);
        log.info(printStr);
        printStr = String.format("%30s : %d", "upload_file_fail", this.uploadFileFailCnt.get());
        System.out.println(printStr);
        log.info(printStr);
        printStr = String.format("%30s : %d", "upload_file_skip", this.uploadFileSkipCnt.get());
        System.out.println(printStr);
        log.info(printStr);
        printStr = String.format("%30s : %s", "start_time",
                this.timeFormatter.format(new Date(this.startTime)));
        System.out.println(printStr);
        log.info(printStr);
        printStr = String.format("%30s : %s", "end_time",
                this.timeFormatter.format(new Date(this.endTime)));
        System.out.println(printStr);
        log.info(printStr);
        printStr =
                String.format("%30s : %d s", "used_time", (this.endTime - this.startTime) / 1000);
        System.out.println(printStr);
        log.info(printStr);

    }

    public void addUploadFileOk() {
        this.uploadFileSumCnt.incrementAndGet();
        this.uploadFileOkCnt.incrementAndGet();
    }

    public void addUploadFileFail() {
        this.uploadFileSumCnt.incrementAndGet();
        this.uploadFileFailCnt.incrementAndGet();
    }
    
    public void addSkipFile() {
        this.uploadFileSkipCnt.incrementAndGet();
    }


    // 用于在每轮任务开始时初始化统计数据
    public void beginCollectStatics() {
        startTime = System.currentTimeMillis();
        this.uploadFileSumCnt.set(0L);
        this.uploadFileOkCnt.set(0L);
        this.uploadFileFailCnt.set(0L);
    }

    // 结束数据统计,将数据刷入表中
    public void endCollectStatics() {
        this.endTime = System.currentTimeMillis();
        printStatics();
    }
}
