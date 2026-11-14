package com.shadowai.app

object LocalInference {
    @JvmStatic
    fun load(modelPath: String, threads: Int, contextSize: Int, temperature: Float) {
        throw UnsupportedOperationException(
            "GGUF-KI ist in der Universal-Version nicht enthalten."
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
    fun isLoaded(): Boolean = false

    @JvmStatic
    fun generate(prompt: String, systemPrompt: String, maxTokens: Int): String {
        throw UnsupportedOperationException("GGUF-KI ist nicht verfügbar.")
    }

    @JvmStatic
    fun release() {}
}
