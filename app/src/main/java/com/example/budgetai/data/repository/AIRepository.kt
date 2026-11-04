package com.example.budgetai.data.repository

import com.example.budgetai.BuildConfig
import com.example.budgetai.data.remote.GeminiClient
import com.example.budgetai.data.remote.GeminiProxy
import com.example.budgetai.data.model.Movement
import com.example.budgetai.data.model.SavingsGoal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Arma el contexto financiero real del usuario (movimientos, saldo, metas y ahorros)
 * y lo envía a Gemini junto con la pregunta del chat.
 */
class AIRepository {

    private val movimientosRepo = MovementRepository()
    private val metasRepo = GoalRepository()
    private val ahorrosRepo = SavingsRepository()
    private val usuarioRepo = UserRepository()

    private val moneda = NumberFormat.getInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }
    private val fecha = SimpleDateFormat("dd/MM/yyyy", Locale("es", "CO"))

    suspend fun responder(pregunta: String, historial: String = ""): String {
        val contexto = construirContexto()

        // Con AI_PROXY_URL la clave no viaja en el APK. Si falla el proxy, hace fallback al cliente directo.
        // Con proxy configurado no se usa el cliente directo: el proxy ya tiene respaldo entre proveedores.
        if (BuildConfig.AI_PROXY_URL.isNotBlank()) {
            return GeminiProxy.responder(BuildConfig.AI_PROXY_URL, pregunta, contexto, historial)
        }

        val prompt = buildString {
            appendLine("Fecha de hoy: ${fecha.format(Date())}")
            appendLine("CONTEXTO FINANCIERO REAL DEL USUARIO:")
            appendLine(contexto)
            if (historial.isNotBlank()) {
                appendLine()
                appendLine("CONVERSACIÓN PREVIA (para dar continuidad):")
                appendLine(historial)
            }
            appendLine()
            appendLine("PREGUNTA DEL USUARIO:")
            append(pregunta)
        }
        return GeminiClient.generar(prompt)
    }

    private suspend fun construirContexto(): String {
        val movimientos = runCatching { movimientosRepo.obtenerMovimientos() }.getOrDefault(emptyList())
        val ahorros = runCatching { ahorrosRepo.obtenerMetas() }.getOrDefault(emptyList())
        val metas = runCatching { metasRepo.obtenerMetas() }.getOrDefault(emptyList())
        val perfil = runCatching { usuarioRepo.obtenerPerfil() }.getOrNull()

        val totalIngresos = movimientos.filter { it.tipo.equals("ingreso", ignoreCase = true) }.sumOf { it.monto }
        val ingresoPerfil = perfil?.informacionFinanciera?.ingresoMensual ?: 0.0
        val ingresos = if (totalIngresos > 0) totalIngresos else ingresoPerfil
        val gastos = movimientos.filter { it.tipo.equals("gasto", ignoreCase = true) }.sumOf { it.monto }

// Mismo saldo que ve el usuario en el Home (ya descuenta lo apartado en metas)
        val enMetas = metas.sumOf { it.montoAhorrado }
        val saldo = runCatching { movimientosRepo.calcularSaldo() }
            .getOrElse { (ingresos - gastos - enMetas).coerceAtLeast(0.0) }

        val hace30Dias = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        val recientes = movimientos.filter { it.fecha.toDate().time >= hace30Dias }
        val ingresos30 = recientes.filter { it.tipo == "ingreso" }.sumOf { it.monto }
        val gastos30 = recientes.filter { it.tipo != "ingreso" }.sumOf { it.monto }

        val topCategorias = recientes.filter { it.tipo != "ingreso" }
            .groupBy { it.categoria.ifBlank { "Sin categoría" } }
            .mapValues { (_, lista) -> lista.sumOf { it.monto } }
            .entries.sortedByDescending { it.value }
            .take(5)

        return buildString {
            if (perfil != null) {
                appendLine("Usuario: ${perfil.nombreCompleto.ifBlank { "sin nombre" }}")
                appendLine("Moneda: ${perfil.moneda}")
                perfil.informacionFinanciera?.let { info ->
                    appendLine("Ingreso mensual declarado: ${dinero(info.ingresoMensual)} (${info.frecuenciaIngresos})")
                    if (info.ahorroMensualEsperado > 0) {
                        appendLine("Ahorro mensual esperado: ${dinero(info.ahorroMensualEsperado)}")
                    }
                    if (info.objetivoFinanciero.isNotBlank()) {
                        appendLine("Objetivo financiero declarado: ${info.objetivoFinanciero}")
                    }
                }
            } else {
                appendLine("Moneda: COP")
            }
            appendLine("Saldo disponible: ${dinero(saldo)}")
            appendLine("Saldo disponible: ${dinero(saldo)} (ya descontado el dinero apartado en metas)")
            appendLine("Dinero apartado en metas: ${dinero(enMetas)}")
            appendLine("Ingresos registrados (total): ${dinero(ingresos)}")
            appendLine("Gastos registrados (total): ${dinero(gastos)}")
            appendLine("Últimos 30 días: ingresos ${dinero(ingresos30)}, gastos ${dinero(gastos30)}")
            appendLine("Movimientos registrados: ${movimientos.size}")

            if (topCategorias.isNotEmpty()) {
                appendLine("Top categorías de gasto (30 días): " +
                    topCategorias.joinToString { "${it.key} ${dinero(it.value)}" })
            }

            appendLine()
            appendLine("METAS DE AHORRO (savingsGoals):")
            if (ahorros.isEmpty()) {
                appendLine("- Sin metas de ahorro registradas.")
            } else {
                ahorros.take(10).forEach { meta -> appendLine(describirAhorro(meta)) }
            }

            appendLine()
            appendLine("METAS DE GASTO/OBJETIVOS (goals):")
            if (metas.isEmpty()) {
                appendLine("- Sin metas de gasto registradas.")
            } else {
                metas.take(10).forEach { meta ->
                    appendLine(
                        "- \"${meta.nombre}\" (${meta.categoria}): objetivo ${dinero(meta.montoObjetivo)}, " +
                            "ahorrado ${dinero(meta.montoAhorrado)} (${meta.progreso()}%), " +
                            "fecha ${fecha.format(meta.fechaObjetivo.toDate())}, estado ${meta.estado}."
                    )
                }
            }

            appendLine()
            appendLine("ÚLTIMOS MOVIMIENTOS:")
            if (movimientos.isEmpty()) {
                appendLine("- Sin movimientos registrados.")
            } else {
                movimientos.take(20).forEach { mov -> appendLine(describirMovimiento(mov)) }
            }
        }
    }

    private fun describirAhorro(meta: SavingsGoal): String =
        "- \"${meta.nombre}\": objetivo ${dinero(meta.montoObjetivo)}, ahorrado ${dinero(meta.montoAhorrado)} " +
            "(${meta.progreso()}%), faltan ${dinero(meta.montoFaltante())}, " +
            "fecha límite ${fecha.format(meta.fechaLimite.toDate())}, " +
            "frecuencia ${meta.frecuencia}, recomendado ${dinero(meta.montoRecomendado)} por periodo, " +
            "activa=${meta.activa}, completada=${meta.completada}."

    private fun describirMovimiento(mov: Movement): String =
        "- ${fecha.format(mov.fecha.toDate())} | ${mov.tipo} | ${mov.categoria.ifBlank { "Sin categoría" }} | " +
            "${mov.descripcion.ifBlank { "sin descripción" }} | ${dinero(mov.monto)}."

    private fun dinero(valor: Double): String = "$" + moneda.format(valor) + " COP"
}
