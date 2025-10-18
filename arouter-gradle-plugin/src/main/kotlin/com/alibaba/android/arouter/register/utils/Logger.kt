package com.alibaba.android.arouter.register.utils

import org.gradle.api.Project

/**
 * Format log
 *
 * @author zhiLong <a href="mailto:zhiLong.lzl@alibaba-inc.com">Contact me.</a>
 * @version 1.0
 * @since 2017/12/18 下午2:43
 */
class Logger {
    companion object {
        var logger: org.gradle.api.logging.Logger? = null

        fun make(project: Project) {
            logger = project.logger
        }

        fun i(info: String) {
            if (info != null && logger != null) {
                logger!!.info("ARouter::Register >>> $info")
            }
        }

        fun e(error: String) {
            if (error != null && logger != null) {
                logger!!.error("ARouter::Register >>> $error")
            }
        }

        fun w(warning: String) {
            if (warning != null && logger != null) {
                logger!!.warn("ARouter::Register >>> $warning")
            }
        }
    }
}