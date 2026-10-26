package com.example.budgetai

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetai.data.model.SavingsState
import com.example.budgetai.ui.savings.SavingsViewModel

class PinActivity : AppCompatActivity() {

    private val viewModel: SavingsViewModel by viewModels()

    private lateinit var tvTitulo: TextView
    private lateinit var tvSubtitulo: TextView
    private lateinit var tvDescripcion: TextView
    private lateinit var tvError: TextView
    private lateinit var dots: List<View>

    private var modo = "configurar" // "configurar" | "depositar" | "retirar"
    private var goalId: String = ""
    private var operacionPendiente: String = ""
    private var monto: Double = 0.0
    private var pinIngresado = StringBuilder()
    private var primerPinConfiguracion: String? = null // para pedir "confirma tu PIN" en modo configurar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)

        tvTitulo = findViewById(R.id.tvTitulo)
        tvSubtitulo = findViewById(R.id.tvSubtitulo)
        tvDescripcion = findViewById(R.id.tvDescripcion)
        tvError = findViewById(R.id.tvError)
        dots = listOf(
            findViewById(R.id.dot1), findViewById(R.id.dot2),
            findViewById(R.id.dot3), findViewById(R.id.dot4)
        )

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        modo = intent.getStringExtra("modo") ?: "configurar"
        goalId = intent.getStringExtra("goalId") ?: ""
        monto = intent.getDoubleExtra("monto", 0.0)
        operacionPendiente = intent.getStringExtra("operacionPendiente") ?: ""

        configurarTextosSegunModo()
        configurarTeclado()
        observarEstado()
    }

    private fun configurarTextosSegunModo() {
        when (modo) {
            "configurar" -> {
                tvTitulo.text = "Seguridad de ahorro"
                tvSubtitulo.text = "Configura tu PIN de seguridad"
                tvDescripcion.text = "Crea tu código obligatorio de acceso de 4 dígitos para autorizar el ingreso y retiro de fondos."
            }
            "depositar" -> {
                tvTitulo.text = "Confirmación de depósito"
                tvSubtitulo.text = "Ingresa tu PIN de seguridad"
                tvDescripcion.text = "Para autorizar el depósito de fondos"
            }
            "retirar" -> {
                tvTitulo.text = "Confirmación de ahorro"
                tvSubtitulo.text = "Ingresa tu PIN obligatorio"
                tvDescripcion.text = "Para autorizar el movimiento de fondos"
            }
        }
    }

    private fun configurarTeclado() {
        val teclasNumericas = mapOf(
            R.id.key1 to "1", R.id.key2 to "2", R.id.key3 to "3",
            R.id.key4 to "4", R.id.key5 to "5", R.id.key6 to "6",
            R.id.key7 to "7", R.id.key8 to "8", R.id.key9 to "9",
            R.id.key0 to "0"
        )
        teclasNumericas.forEach { (id, digito) ->
            findViewById<TextView>(id).setOnClickListener { agregarDigito(digito) }
        }
        findViewById<TextView>(R.id.keyX).setOnClickListener { borrarDigito() }
        findViewById<TextView>(R.id.keyOK).setOnClickListener { confirmarPin() }
    }

    private fun agregarDigito(digito: String) {
        if (pinIngresado.length >= 4) return
        pinIngresado.append(digito)
        actualizarDots()
        tvError.visibility = View.GONE

        if (pinIngresado.length == 4) {
            confirmarPin()
        }
    }

    private fun borrarDigito() {
        if (pinIngresado.isNotEmpty()) {
            pinIngresado.deleteCharAt(pinIngresado.length - 1)
            actualizarDots()
        }
    }

    private fun actualizarDots() {
        dots.forEachIndexed { index, dot ->
            dot.setBackgroundResource(
                if (index < pinIngresado.length) R.drawable.bg_pin_dot_filled else R.drawable.bg_pin_dot
            )
        }
    }

    private fun confirmarPin() {
        val pin = pinIngresado.toString()
        if (pin.length != 4) return

        when (modo) {
            "configurar" -> manejarConfiguracion(pin)
            else -> viewModel.verificarPin(pin)
        }
    }

    private fun manejarConfiguracion(pin: String) {
        if (primerPinConfiguracion == null) {
            // Primera vez: guarda el PIN y pide confirmarlo
            primerPinConfiguracion = pin
            pinIngresado.clear()
            actualizarDots()
            tvDescripcion.text = "Confirma tu código de 4 dígitos para continuar."
        } else {
            // Segunda vez: compara con el primero
            if (pin == primerPinConfiguracion) {
                viewModel.configurarPin(pin)
            } else {
                mostrarError("Los códigos no coinciden. Intenta de nuevo.")
                primerPinConfiguracion = null
                pinIngresado.clear()
                actualizarDots()
                tvDescripcion.text = "Crea tu código obligatorio de acceso de 4 dígitos para autorizar el ingreso y retiro de fondos."
            }
        }
    }

    private fun mostrarError(mensaje: String) {
        tvError.text = mensaje
        tvError.visibility = View.VISIBLE
    }

    private fun observarEstado() {
        viewModel.savingsState.observe(this) { estado ->
            when (estado) {
                is SavingsState.PinConfigurado -> {
                    if (goalId.isNotEmpty() && monto > 0) {
                        continuarSegunModo()
                    } else {
                        val intent = Intent(this, PinSuccessActivity::class.java)
                        intent.putExtra("tipo", "pin_creado")
                        startActivity(intent)
                        finish()
                    }
                }

                is SavingsState.PinValido -> {
                    continuarSegunModo()
                }

                is SavingsState.PinInvalido -> {
                    mostrarError("Código incorrecto (intento ${3 - estado.intentosRestantes} de 3)")
                    pinIngresado.clear()
                    actualizarDots()
                }

                is SavingsState.PinBloqueado -> {
                    mostrarError("Tu cuenta de ahorros se bloqueó temporalmente por 15 minutos tras varios intentos fallidos.")
                    pinIngresado.clear()
                    actualizarDots()
                }

                is SavingsState.Error -> {
                    mostrarError(estado.mensaje)
                    pinIngresado.clear()
                    actualizarDots()
                }

                else -> Unit
            }
        }
    }

    private fun continuarSegunModo() {
        val accion = if (operacionPendiente.isNotEmpty()) operacionPendiente else modo
        when (accion) {
            "depositar" -> {
                viewModel.depositar(goalId, monto)
                observarResultadoOperacion("deposito")
            }
            "retirar" -> {
                viewModel.retirar(goalId, monto)
                observarResultadoOperacion("retiro")
            }
        }
    }

    private fun observarResultadoOperacion(tipo: String) {
        viewModel.savingsState.observe(this) { estado ->
            when (estado) {
                is SavingsState.DepositoExitoso -> {
                    val intent = Intent(this, OperationResultActivity::class.java)
                    intent.putExtra("tipo", "deposito_exitoso")
                    intent.putExtra("monto", monto)
                    intent.putExtra("goalId", goalId)
                    startActivity(intent)
                    finish()
                }
                is SavingsState.RetiroExitoso -> {
                    val intent = Intent(this, OperationResultActivity::class.java)
                    intent.putExtra("tipo", "retiro_exitoso")
                    intent.putExtra("monto", monto)
                    intent.putExtra("goalId", goalId)
                    startActivity(intent)
                    finish()
                }
                is SavingsState.Error -> {
                    val intent = Intent(this, OperationResultActivity::class.java)
                    intent.putExtra("tipo", "${tipo}_fallido")
                    intent.putExtra("mensaje", estado.mensaje)
                    startActivity(intent)
                    finish()
                }
                else -> Unit
            }
        }
    }
}