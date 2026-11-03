package com.example.budgetai.data.remote

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Llama al proxy gratis (Vercel). La clave de Gemini vive en las variables de entorno
 * del servidor, nunca en la app. El proxy valida el ID token de Firebase.
 */
object GeminiProxy {

    suspend fun responder(
        url: String,
        pregunta: String,
        contexto: String,
        historial: String
    ): String = withContext(Dispatchers.IO) {
        val usuario = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("Inicia sesión para usar BudgetIA.")
        val token = usuario.getIdToken(false).await().token
            ?: throw IllegalStateException("No pude validar tu sesión. Inicia sesión de nuevo.")

        val cuerpo = JSONObject().apply {
            put("pregunta", pregunta)
            put("contexto", contexto)
            put("historial", historial)
        }.toString()

        val conexion = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 50_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer $token")
        }

        try {
            conexion.outputStream.use { it.write(cuerpo.toByteArray(Charsets.UTF_8)) }
            val codigo = conexion.responseCode
            val stream = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
            val respuesta = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (codigo !in 200..299) {
                throw IllegalStateException(mensajeError(codigo, respuesta))
            }

            val texto = runCatching { JSONObject(respuesta).optString("texto") }.getOrNull()
            if (texto.isNullOrBlank()) {
                throw IllegalStateException("El servidor no devolvió una recomendación.")
            }
            texto
        } finally {
            conexion.disconnect()
        }
    }

    private fun mensajeError(codigo: Int, cuerpo: String): String {
        val detalle = runCatching { JSONObject(cuerpo).optString("error") }
            .getOrNull()
            .orEmpty()

        return when {
            codigo == 401 || codigo == 403 ->
                detalle.ifBlank { "Sesión no válida. Inicia sesión de nuevo." }
            codigo == 429 -> "Demasiadas solicitudes. Espera un momento e intenta de nuevo."
            codigo >= 500 -> "El servicio no está disponible. Intenta de nuevo."
            detalle.isNotBlank() -> detalle
            else -> "No pude conectar con BudgetIA ($codigo)."
        }
    }
}
