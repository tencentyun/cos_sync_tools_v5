package com.qcloud.cos_sync_tools_xml.meta;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * 同步工具的配置类, 从文件中读取配置, 并进行检查, 配置为JSON格式
 * 
 * @author chengwu
 *
 */
public class Config {
    public static final Config instance = new Config();
    private static final String configPath = "./conf/config.ini";
    private boolean initConfigFlag = true;
    private String initConfigErr = "";

    private Properties prop;

    private Config() {}

    public void init() {
        File configFile = new File(configPath);
        if (!configFile.exists()) {
            this.initConfigFlag = false;
            this.initConfigErr = String.format("config file %s not exist", configPath);
            return;
        }

        if (!configFile.isFile()) {
            this.initConfigFlag = false;
            this.initConfigErr = String.format("config file %s is not regular file", configPath);
            return;
        }

        if (!configFile.canRead()) {
            this.initConfigFlag = false;
            this.initConfigErr = String.format("config file %s is not readable", configPath);
            return;
        }

        FileInputStream configIn = null;
        this.prop = new Properties();
        try {
            configIn = new FileInputStream(configFile);
            this.prop.load(configIn);
        } catch (Exception e) {
            this.initConfigFlag = false;
            this.initConfigErr = String.format("read config file get exception:%s", e.getMessage());
            return;
        } finally {
            try {
                if (configIn != null) {
                    configIn.close();
                }
            } catch (Exception e) {
            }
        }

        String[] validConfigKeyArry = {"secret_id", "secret_key", "bucket", "region",
                "local_path", "cos_path", "storage_class", "enable_https"};

        for (String validKey : validConfigKeyArry) {
            if (this.prop.getProperty(validKey) == null) {
                this.initConfigFlag = false;
                this.initConfigErr = String.format("config file not contain %s", validKey);
                return;
            }
        }

        if (!checkBucket() || !checkLocalPath() || !checkCosPath() || !checkStorageClass()
                || !checkEnableHttps()) {
            return;
        }

        formatLocalPath();
        formatCosPath();
        this.initConfigFlag = true;
    }


    /**
     * check bucket是否正确
     * 
     * @return 配置正确返回True, 否则False
     */
    private boolean checkBucket() {
        String bucketName = this.prop.getProperty("bucket").trim();
        String parrtern = ".*-(125|100)[0-9]{3,}$";
        if (Pattern.matches(parrtern, bucketName)) {
            return true;
        } else {
            this.initConfigFlag = false;
            this.initConfigErr =
                    "wrong config, bucket name must contain legal appid. example: music-1251122334";
            return false;
        }
        
    }

    private boolean checkStorageClass() {
        String storageClass = this.prop.getProperty("storage_class").trim().toLowerCase();
        if (storageClass.equals("standard") || storageClass.equals("standard_ia")
                || storageClass.equals("nearline")) {
            return true;
        } else {
            this.initConfigFlag = false;
            this.initConfigErr =
                    "wrong config, storage_class only support standard, standard_ia, nearline";
            return false;
        }
    }

    private boolean checkEnableHttps() {
        return checkValueIntStr("enable_https");
    }

    private boolean checkValueIntStr(String key) {
        try {
            Integer.valueOf(this.prop.getProperty(key).trim());
            return true;
        } catch (NumberFormatException e) {
            this.initConfigFlag = false;
            this.initConfigErr = String.format("wrong config, %s is illegal", key);
            return false;
        }
    }

    private boolean checkLocalPath() {
        String localPath = this.prop.getProperty("local_path").trim();
        File localPathDir = new File(localPath);
        if (!localPathDir.exists()) {
            this.initConfigFlag = false;
            this.initConfigErr = String.format("wrong config, %s not exist!", localPath);
            return false;
        }

        if (!localPathDir.isDirectory()) {
            this.initConfigFlag = false;
            this.initConfigErr = String.format("wrong config, %s is not dir!", localPath);
            return false;
        }

        if (!localPathDir.canRead() || !localPathDir.canExecute()) {
            this.initConfigFlag = false;
            this.initConfigErr = String
                    .format("wrong config, the dir %s must have the r+w authority!", localPath);
            return false;
        }
        return true;
    }

    private boolean checkCosPath() {
        String cosPath = this.prop.getProperty("cos_path").trim();
        if (!cosPath.startsWith("/")) {
            this.initConfigFlag = false;
            this.initConfigErr = "wrong config, cos_path must start with bucket root /";
            return false;
        }
        return true;
    }


    // 格式化cos path
    private void formatCosPath() {
        String cosPath = this.prop.getProperty("cos_path").trim();
        if (!cosPath.endsWith("/")) {
            cosPath += "/";
        }
        this.prop.setProperty("cos_path", cosPath);
    }

    // 格式化local_path
    private void formatLocalPath() {
        String localPathInconfig = this.prop.getProperty("local_path").trim();
        String[] localPathArray = localPathInconfig.split(";");
        boolean seaone = false;
        StringBuilder stringBuilder = new StringBuilder();
        for (String localPath : localPathArray) {
            String formatPath = new File(localPath).getAbsolutePath();
            if (isWindowsSystem()) {
                formatPath = formatPath.replace("\\", "/");
            }
            if (!formatPath.endsWith("/")) {
                formatPath += "/";
            }
            if (seaone) {
                stringBuilder.append(";");
                stringBuilder.append(formatPath);
            } else {
                seaone = true;
                stringBuilder.append(formatPath);
            }
        }
        this.prop.setProperty("local_path", stringBuilder.toString());
    }

    public boolean isValidConfig() {
        return initConfigFlag;
    }

    public String getInitConfigErr() {
        return initConfigErr;
    }

    public String getSecretId() {
        return this.prop.getProperty("secret_id").trim();
    }

    public String getSecretKey() {
        return this.prop.getProperty("secret_key").trim();
    }

    public String getBucket() {
        return this.prop.getProperty("bucket").trim();
    }

    public String[] getLocalPath() {
        String localPathConfig = this.prop.getProperty("local_path").trim();
        return localPathConfig.split(";");
    }

    public String getCosPath() {
        return this.prop.getProperty("cos_path").trim();
    }

    public int getEnableHttps() {
        return getIntValue("enable_https");
    }

    public String getRegion() {
        return this.prop.getProperty("region").trim();
    }

    public String getStorageClass() {
        return this.prop.getProperty("storage_class").trim().toLowerCase();
    }

    private int getIntValue(String key) {
        int keyValue = new Integer(this.prop.getProperty(key).trim()).intValue();
        return keyValue;
    }
    
    public boolean isWindowsSystem() {
        String osSystemName = System.getProperty("os.name").toLowerCase();
        return osSystemName.startsWith("win");
    }
}
