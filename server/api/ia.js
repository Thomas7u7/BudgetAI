const { createRemoteJWKSet, jwtVerify } = require("jose");

const CLAVES_FIREBASE = createRemoteJWKSet(
  new URL(
    "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"
  )
);

const PROYECTO = process.env.FIREBASE_PROJECT_ID || "budgetai-9106c";
const MODELO = process.env.GEMINI_MODEL || "gemini-3.5-flash";
const URL_GEMINI =
  `https://generativelanguage.googleapis.com/v1beta/models/${MODELO}:generateContent`;

const INSTRUCCIONES = `
Eres BudgetIA, asesor financiero personal dentro de una app de finanzas.
Responde siempre en español claro, cercano y breve (máximo 180 palabras).
Usa montos en pesos colombianos (COP) y viñetas cuando ayude a la lectura.
Básate únicamente en el contexto financiero entregado; no inventes cifras.
Si faltan datos, dilo y sugiere cómo obtenerlos dentro de la app.
Da recomendaciones concretas, realistas y accionables sobre ahorro,
reducción de gastos y cumplimiento de metas. No des asesoría de inversión
de alto riesgo ni solicites datos sensibles.
`.trim();

function limpiar(valor, maximo) {
  if (typeof valor !== "string") return "";
  return valor.trim().slice(0, maximo);
}

async function verificarUsuario(req) {
  const cabecera = req.headers.authorization || "";
  const token = cabecera.startsWith("Bearer ") ? cabecera.slice(7).trim() : "";
  if (!token) return null;

  try {
    const { payload } = await jwtVerify(token, CLAVES_FIREBASE, {
      issuer: `https://securetoken.google.com/${PROYECTO}`,
      audience: PROYECTO,
    });
    return payload.sub || null;
  } catch {
    return null;
  }
}

module.exports = async (req, res) => {
  res.setHeader("Cache-Control", "no-store");

  if (req.method !== "POST") {
    return res.status(405).json({ error: "Método no permitido." });
  }

  const uid = await verificarUsuario(req);
  if (!uid) {
    return res.status(401).json({ error: "Sesión no válida. Inicia sesión de nuevo." });
  }

  let datos = req.body;
  if (typeof datos === "string") {
    try {
      datos = JSON.parse(datos);
    } catch {
      return res.status(400).json({ error: "Solicitud inválida." });
    }
  }

  const pregunta = limpiar(datos?.pregunta, 2000);
  const contexto = limpiar(datos?.contexto, 20000);
  const historial = limpiar(datos?.historial, 4000);

  if (!pregunta) {
    return res.status(400).json({ error: "Escribe una pregunta." });
  }
  if (!contexto) {
    return res.status(400).json({ error: "No se recibió contexto financiero." });
  }

  const clave = process.env.GEMINI_API_KEY;
  if (!clave) {
    console.error("Falta GEMINI_API_KEY en las variables de entorno");
    return res.status(500).json({ error: "Servicio mal configurado." });
  }

  const prompt = [
    `Fecha de hoy: ${new Date().toISOString().slice(0, 10)}`,
    "CONTEXTO FINANCIERO REAL DEL USUARIO:",
    contexto,
    historial ? `\nCONVERSACIÓN PREVIA:\n${historial}` : "",
    "\nPREGUNTA DEL USUARIO:",
    pregunta,
  ].join("\n");

  const cuerpoPeticion = {
    systemInstruction: { parts: [{ text: INSTRUCCIONES }] },
    contents: [{ role: "user", parts: [{ text: prompt }] }],
    generationConfig: {
      temperature: 0.6,
      maxOutputTokens: 700,
      thinkingConfig: { thinkingBudget: 0 },
    },
  };

  let respuesta;
  try {
    respuesta = await fetch(URL_GEMINI, {
      method: "POST",
      headers: {
        "Content-Type": "application/json; charset=utf-8",
        "x-goog-api-key": clave,
      },
      body: JSON.stringify(cuerpoPeticion),
      signal: AbortSignal.timeout(45000),
    });
  } catch (error) {
    console.error("Gemini no respondió", error);
    return res.status(503).json({ error: "Gemini no está disponible. Intenta de nuevo." });
  }

  const cuerpo = await respuesta.text();
  if (!respuesta.ok) {
    console.error("Gemini devolvió error", respuesta.status);
    if (respuesta.status === 429) {
      return res.status(429).json({ error: "Demasiadas solicitudes. Espera un momento." });
    }
    return res.status(503).json({ error: "Gemini no está disponible. Intenta de nuevo." });
  }

  let texto = "";
  try {
    const resultado = JSON.parse(cuerpo);
    texto = (resultado.candidates?.[0]?.content?.parts ?? [])
      .map((parte) => parte.text ?? "")
      .join("")
      .trim();
  } catch (error) {
    console.error("Respuesta ilegible de Gemini", error);
  }

  if (!texto) {
    return res.status(502).json({ error: "No pude generar una recomendación. Intenta de nuevo." });
  }

  return res.status(200).json({ texto });
};
