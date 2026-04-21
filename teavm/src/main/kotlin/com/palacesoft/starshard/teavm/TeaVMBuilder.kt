package com.palacesoft.starshard.teavm

import com.github.xpenatan.gdx.teavm.backends.shared.config.AssetFileHandle
import com.github.xpenatan.gdx.teavm.backends.shared.config.compiler.TeaCompiler
import com.github.xpenatan.gdx.teavm.backends.web.config.backend.WebBackend
import org.teavm.tooling.TeaVMSourceFilePolicy
import org.teavm.tooling.sources.DirectorySourceFileProvider
import org.teavm.vm.TeaVMOptimizationLevel
import java.io.File

object TeaVMBuilder {
    @JvmStatic
    fun main(arguments: Array<String>) {
        val debug = "debug" in arguments
        val startJetty = "run" in arguments

        val webBackend = WebBackend()
            .setHtmlTitle("Starshard 79")
            .setHtmlWidth(1600)
            .setHtmlHeight(900)
            .setStartJettyAfterBuild(startJetty)
            .setJettyPort(8080)

        TeaCompiler(webBackend)
            .addAssets(AssetFileHandle("../assets"))
            .setOptimizationLevel(
                if (debug) TeaVMOptimizationLevel.SIMPLE
                else TeaVMOptimizationLevel.ADVANCED
            )
            .setMainClass("com.palacesoft.starshard.teavm.TeaVMLauncher")
            .setObfuscated(!debug)
            .setDebugInformationGenerated(debug)
            .setSourceMapsFileGenerated(debug)
            .setSourceFilePolicy(
                if (debug) TeaVMSourceFilePolicy.COPY
                else TeaVMSourceFilePolicy.DO_NOTHING
            )
            .addSourceFileProvider(
                DirectorySourceFileProvider(File("../core/src/main/kotlin"))
            )
            .build(File("build/dist"))
    }
}
