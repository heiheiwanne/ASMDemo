package com.demo.plugin


/**
 * 作用描述: 日志记录
 * @author : xmq
 * @date : 2018/12/21 下午5:54
 */
class Logger {
    static org.gradle.api.logging.Logger logger

    static void setLogger(org.gradle.api.logging.Logger logger) {
        this.logger = logger
    }

    static void i(String info) {
        if (null != info && null != logger) {
            logger.info("MyPlugin >>> " + info)
        }
    }

    static void e(String error) {
        if (null != error && null != logger) {
            logger.error("MyPlugin >>> " + error)
        }
    }

    static void w(String warning) {
        if (null != warning && null != logger) {
            logger.warn("MyPlugin >>> " + warning)
        }
    }
}
