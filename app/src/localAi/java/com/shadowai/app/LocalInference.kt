package com.shadowai.app

import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import kotlinx.coroutines.runBlocking

object LocalInference {
    @Volatile private var engine: Engine? = null

    @Synchronized
    @JvmStatic
    fun load(modelPath: String, threads: Int, contextSize: Int, temperature: Float) {
        release()
        engine = Engine(
            modelPath,
            threads.coerceIn(1, 3),
            contextSize.coerceIn(1024, 4096),
            temperature.coerceIn(0.1f, 2.0f)
        )
    }

    @JvmStatic
    fun load(modelPath: String, threads: Int, contextSize: Int) {
        load(modelPath, threads, contextSize, 0.7f)
    }

    @JvmStatic
    fun load(modelPath: String, threads: Int) {
        load(modelPath, threads, 2048, 0.7f)
    }

    @JvmStatic
    fun isLoaded(): Boolean = engine != null

    @Synchronized
    @JvmStatic
    fun generate(prompt: String, systemPrompt: String, maxTokens: Int): String {
        val active = engine ?: throw IllegalStateException(
            "Noch kein GGUF-Modell geladen. Bitte zuerst eine Modelldatei importieren."
        )
        return active.complete(prompt, systemPrompt, maxTokens.coerceIn(32, 192))
    }

    @Synchronized
    @JvmStatic
    fun release() {
        val old = engine
        engine = null
        old?.close()
    }

    private class Engine(
        path: String,
        threads: Int,
        contextSize: Int,
        temperature: Float
    ) {
        private val model = runBlocking {
            Llama.loadModel(
                modelPath = path,
                config = LlamaConfig(
                    contextSize = contextSize,
                    threads = threads,
                    temperature = temperature
                )
            )
        }

        private val promptLimit = (contextSize * 3).coerceAtLeast(3000)

        fun complete(prompt: String, system: String, maxTokens: Int): String =
            runBlocking {
                Llama.complete(
                    model = model,
                    prompt = prompt.takeLast(promptLimit),
                    systemPrompt = system.take(1500),
                    maxTokens = maxTokens
                ).text
            }

        fun close() {
            runBlocking { Llama.releaseModel(model) }
        }
    }
}
