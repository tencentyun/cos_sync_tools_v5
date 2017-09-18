package com.qcloud.cos_sync_tools_xml.meta;

import org.apache.commons.codec.digest.DigestUtils;
import org.iq80.leveldb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.iq80.leveldb.impl.Iq80DBFactory.*;
import java.io.*;

/**
 * DbRecord 里存储已经上传的记录, key是本地路径和COS目录的一个拼接, value是mtime
 * 
 * @author chengwu
 *
 */

public class DbRecord {

    public static final Logger log = LoggerFactory.getLogger(DbRecord.class);

    public static final DbRecord instance = new DbRecord();
    private static final int CACHE_SIZE = 128 << 20;

    private DB db;

    private DbRecord() {}

    public boolean init() {
        Options options = new Options();
        options.cacheSize(CACHE_SIZE);
        options.createIfMissing(true);
        try {
            db = factory.open(new File("db"), options);
            return true;
        } catch (IOException e) {
            log.error(e.toString());
            return false;
        }
    }

    // key是本地绝对路径和COS要同步的本地folder路径的拼接
    private String buildDbKey(File localFile) {
        String cosPath = Config.instance.getCosPath();
        String bucket = Config.instance.getBucket();
        String region = Config.instance.getRegion();
        String fullPath = new StringBuilder("localPath: ").append(localFile.getAbsolutePath())
                .append("bucket: ").append(bucket).append(", region: ").append(region)
                .append(", cospath: ").append(cosPath).toString();
        return DigestUtils.md5Hex(fullPath);
    }

    // 更新已上传的记录
    public boolean update(File localFile) {
        String key = buildDbKey(localFile);
        String value = String.valueOf(localFile.lastModified());
        try {
            db.put(key.getBytes(), value.getBytes());
            return true;
        } catch (DBException e) {
            log.error("db put key/value failed. key: " + key + ", value: " + value, e);
            return false;
        }
    }

    // query 用于查询是否需要进行上传, 如果需要则返回true, 否则返回false
    public boolean queryIfNeedToUpload(File localFile) {
        String key = buildDbKey(localFile);
        byte[] valueByte = db.get(key.getBytes());
        if (valueByte == null) {
            return true;
        }

        // 对于目录来说，如果之前已经上传过, 则不进行上传，因为目录下面如果更新了文件，则目录的mtime一定会发生改变
        if (localFile.isDirectory()) {
            return false;
        }
        String value = new String(valueByte);

        long mtime = 0;
        try {
            mtime = Long.valueOf(value);
        } catch (NumberFormatException e) {
            log.error("value in invalid num str. key: " + key + ", value: " + value);
            return true;
        }
        return mtime != localFile.lastModified();
    }

    public void shutdown() {
        if (db != null) {
            try {
                db.close();
            } catch (IOException e) {
                log.error("close db occur a exception: " + e.toString());
            }
        }
    }
}
