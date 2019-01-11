package com.demo.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Java类描述
 *
 * @author : xmq
 * @date : 2019/1/9 上午11:16
 */
class MyPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def isApp = project.plugins.hasPlugin(AppPlugin)

        if (!isApp) return

        Logger.setLogger(project.logger) //设置logger
        project.extensions.getByType(AppExtension).registerTransform(new MyTransform())
        project.extensions.getByType(AppExtension).registerTransform(new CostTransform())
    }
}
