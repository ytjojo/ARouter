package com.alibaba.android.arouter.register.utils

import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.slf4j.LoggerFactory

/**
 * Format log
 *
 * @author zhiLong <a href="mailto:zhiLong.lzl@alibaba-inc.com">Contact me.</a>
 * @version 1.0
 * @since 2017/12/18 下午2:43
 */
class Logger {
    companion object {
        private var logger: org.gradle.api.logging.Logger? = null
            get() {
                if (field == null) {
                    field = Logging.getLogger(Logger::class.java)
                }
                return field!!
            }

        fun make(project: Project) {
            logger = project.logger
        }

        fun i(info: String) {
            logger!!.lifecycle("ARouter::Register >>> $info")
        }

        fun e(error: String) {
            logger!!.error("ARouter::Register >>> $error")
        }

        fun w(warning: String) {
            logger!!.warn("ARouter::Register >>> $warning")
        }
    }
}