package com.example.budgetai.data.remote

import com.example.budgetai.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cliente mínimo de la API de Gemini (generateContent).
 * No usa SDK para no sumar dependencias: solo HttpURLConnection + org.json.
 */
object GeminiClient {

    private const val MODELO = "gemini-3.5-flash"
    private const val URL_API =
        "https://generativelanguage.googleapis.com/v1beta/models/$MODELO:generateContent"

    private val INSTRUCCIONES = """
        Eres BudgetIA, asesor financiero personal dentro de una app de finanzas.
        Responde siempre en español claro, cercano y breve (máximo 180 palabras).
        Usa montos en pesos colombianos (COP) y viñetas cuando ayude a la lectura.
        Básate únicamente en el contexto financiero entregado; no inventes cifras.
        Si faltan datos, dilo y sugiere cómo obtenerlos dentro de la app.
        Da recomendaciones concretas, realistas y accionables sobre ahorro,
        reducción de gastos y cumplimiento de metas. No des asesoría de inversión
        de alto riesgo ni solicites datos sensibles.
    """.trimIndent()

    suspend fun generar(prompt: String): String = withContext(Dispatchers.IO) {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            throw IllegalStateException("Falta la clave de Gemini. Configura GEMINI_API_KEY en local.properties.")
        }

        var ultimoError = "No pude conectar con Gemini."
        repeat(3) { intento ->
            val (codigo, cuerpo) = peticion(prompt)
            if (codigo in 200..299) return@withContext extraerTexto(cuerpo)

            ultimoError = mensajeError(codigo, cuerpo)
            val transitorio = codigo == 503 || codigo == 429
            if (transitorio && intento < 2) {
                delay(700L * (intento + 1))
                return@repeat
            }
            throw IllegalStateException(ultimoError)
        }
        throw IllegalStateException(ultimoError)
    }

    private fun peticion(prompt: String): Pair<Int, String> {
        val cuerpo = JSONObject().apply {
            put(
                "systemInstruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", INSTRUCCIONES)))
            )
            put(
                "contents",
                JSONArray().put(
                    JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                    }
                )
            )
            put(
                "generationConfig",
                JSONObject().apply {
                    put("temperature", 0.6)
                    put("maxOutputTokens", 700)
                    // Sin razonamiento interno: respuestas completas, más rápidas y baratas.
                    put("thinkingConfig", JSONObject().put("thinkingBudget", 0))
                }
            )
        }.toString()

        val conexion = (URL(URL_API).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 40_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
        }

        return try {
            conexion.outputStream.use { it.write(cuerpo.toByteArray(Charsets.UTF_8)) }
            val codigo = conexion.responseCode
            val stream = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
            val texto = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            codigo to texto
        } finally {
            conexion.disconnect()
        }
    }

    private fun extraerTexto(cuerpo: String): String {
        val partes = JSONObject(cuerpo)
            .optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?: throw IllegalStateException("Gemini no devolvió una respuesta.")

        val texto = buildString {
            for (i in 0 until partes.length()) {
                append(partes.optJSONObject(i)?.optString("text").orEmpty())
            }
        }.trim()

        return texto.ifBlank { "No pude generar una recomendación esta vez. Intenta de nuevo." }
    }

    private fun mensajeError(codigo: Int, cuerpo: String): String {
        val detalle = runCatching {
            JSONObject(cuerpo).optJSONObject("error")?.optString("message")
        }.getOrNull().orEmpty()

        return when (codigo) {
            400 -> "La solicitud a Gemini no es válida. Intenta reformular tu pregunta."
            401, 403 -> "La clave de Gemini no es válida o no tiene permisos."
            429 -> "Demasiadas solicitudes a Gemini. Espera un momento e intenta de nuevo."
            503 -> "Gemini está saturado en este momento. Intenta de nuevo en unos segundos."
            else -> if (detalle.isBlank()) "Error de Gemini ($codigo)." else "Gemini: $detalle"
        }
    }
}
