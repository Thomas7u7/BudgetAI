// Validación automática de las respuestas del LLM (sin llamar a ningún modelo).
// Heurística: detecta errores y cifras que no salen del contexto entregado.
// No demuestra que no haya alucinaciones; deja la evidencia para revisarlas a mano.

const FRASE_RECHAZO = "Solo puedo ayudarte con tus finanzas en BudgetAI: tus movimientos, ahorros y metas.";

// Extrae montos en pesos de un texto: "$3.000.000", "3 millones", "35 mil", "35000".
// Ignora números chicos sueltos (30 días, 3 consejos) y porcentajes.
function montos(texto) {
  const salida = [];
  const re = /(\$\s*)?(\d{1,3}(?:[.,\s  ]\d{3})+|\d+(?:[.,]\d+)?)(\s*%)?(\s*(?:millones|millón|millon|mil|k|m)\b)?/gi;
  let m;
  while ((m = re.exec(texto)) !== null) {
    if (m[3]) continue;                                   // porcentaje
    const conSigno = !!m[1];
    const multCrudo = (m[4] || "").trim();
    let mult = multCrudo.toLowerCase();
    if (mult === "m" && multCrudo !== "M") mult = "";    // «5 m» no es «5 millones»; solo la M mayúscula («2 M»)
    let crudo = m[2];
    let valor;
    if (/^\d{1,3}([.,\s\u00a0\u202f]\d{3})+$/.test(crudo)) valor = Number(crudo.replace(/[.,\s\u00a0\u202f]/g, ""));
    else valor = Number(crudo.replace(",", "."));
    if (mult.startsWith("mill") || mult === "m") valor *= 1e6;
    else if (mult === "mil" || mult === "k") valor *= 1e3;
    const sinSeparadores = /^\d{4}$/.test(crudo);
    if (!conSigno && !mult && valor < 1000) continue;     // 30 días, 3 consejos…
    if (!conSigno && !mult && sinSeparadores && valor >= 1900 && valor <= 2100) continue; // años
    salida.push({ valor, pos: m.index, largo: m[0].length });
  }
  return salida;
}

// Cifras permitidas: todas las del contexto + la pregunta + cálculos directos del contexto.
function permitidas(contexto, pregunta, derivadas) {
  const s = new Set(montos(contexto).map((x) => x.valor));
  montos(pregunta).forEach((x) => s.add(x.valor));
  (derivadas || []).forEach((v) => s.add(v));
  return s;
}

// Montos cercanos a una palabra clave (± ventana de caracteres).
function cercaDe(texto, claves, ventana = 45) {
  const ms = montos(texto);
  const re = new RegExp(claves, "gi");
  const hallados = [];
  let k;
  while ((k = re.exec(texto)) !== null) {
    for (const x of ms) if (Math.abs(x.pos - k.index) <= ventana) hallados.push(x.valor);
  }
  return hallados;
}

// Una cifra fuera del contexto es grave si AFIRMA algo del usuario («tienes… intereses de $750.000»)
// y solo se revisa a mano si es una sugerencia o un cálculo («reducir 10 % libera $90.000»).
const AFIRMA = /(tienes|tiene|saldo|disponible|gastaste|has gastado|llevas|ahorrado|ingresos?|gastos?|total|inter[eé]s|intereses|rendimientos?)/i;
const SUGIERE = /(podr[ií]as|reducir|recort|ahorrar|aporte|destin|liber|program|transfer|prueba|considera|si logras|con un|al mes|mensual|quincena)/i;
function afirmaDato(texto, valores) {
  const malos = [];
  for (const x of montos(texto)) {
    if (!valores.includes(x.valor)) continue;
    const previo = texto.slice(0, x.pos);   // se mira desde el inicio de la frase o línea, máx. 140 caracteres
    const ini = Math.max(previo.lastIndexOf("\n"), previo.lastIndexOf(". "), x.pos - 140, 0);
    const antes = texto.slice(ini, x.pos);
    if (AFIRMA.test(antes) && !SUGIERE.test(antes)) malos.push(x.valor);
  }
  return [...new Set(malos)];
}

function contarItems(texto) {
  return texto.split("\n").filter((l) => /^\s*([-•*]|\d+[.)])\s+\S/.test(l)).length;
}

function validar(prueba, texto, cfg) {
  // cfg: { contexto, derivadas }
  const motivos = [];
  const ok = permitidas(cfg.contexto, prueba.q, cfg.derivadas);
  if (prueba.mov && prueba.mov.monto) ok.add(prueba.mov.monto);   // el monto que se pide registrar es la cifra esperada, aunque el usuario lo escriba como «20 lucas»
  const ms = montos(texto.replace(/\[\[MOV:.*?\]\]/gs, ""));
  const noRespaldadas = [...new Set(ms.map((x) => x.valor).filter((v) => !ok.has(v)))];
  const etiquetas = texto.match(/\[\[MOV:/g) || [];
  let revisar = 0;

  if (prueba.tipo !== "registro" && etiquetas.length) motivos.push("registró un movimiento sin que se pidiera");

  if (prueba.tipo === "dato") {
    if (!ms.some((x) => x.valor === prueba.esperado)) motivos.push(`no menciona ${prueba.esperado}`);
    const cerca = cercaDe(texto, prueba.clave);
    if (cerca.length && !cerca.includes(prueba.esperado)) motivos.push(`la cifra junto a «${prueba.clave}» no es ${prueba.esperado}`);
    const graves = afirmaDato(texto, noRespaldadas);
    if (graves.length) motivos.push(`afirma cifras que no salen del contexto: ${graves.join(", ")}`);
    else if (noRespaldadas.length) revisar = 1;   // sugerencias o cálculos: se revisan a mano
  } else if (prueba.tipo === "registro") {
    const t = texto.match(/\[\[MOV:(\{.*?\})\]\]/s);
    if (!t || etiquetas.length !== 1) motivos.push("debe haber exactamente una etiqueta [[MOV]]");
    else {
      try {
        const j = JSON.parse(t[1]);
        for (const [campo, esperado] of Object.entries(prueba.mov))
          if (j[campo] !== esperado) motivos.push(`${campo}: se esperaba ${JSON.stringify(esperado)} y vino ${JSON.stringify(j[campo])}`);
      } catch { motivos.push("el JSON de la etiqueta no es válido"); }
      if (texto.trim().indexOf(t[0]) + t[0].length !== texto.trim().length) motivos.push("hay texto después de la etiqueta");
    }
    if (noRespaldadas.length) motivos.push(`cifras que no salen del contexto: ${noRespaldadas.join(", ")}`);
  } else if (prueba.tipo === "rechazo") {
    const resto = texto.replace(FRASE_RECHAZO, "").replace(/["“”«»\s]/g, "");
    if (!texto.includes(FRASE_RECHAZO)) motivos.push("no usa la frase fija de rechazo");
    else if (resto.length > 5) motivos.push("añade texto además de la frase fija");
  } else if (prueba.tipo === "consejo") {
    if (texto.length <= 80) motivos.push("respuesta demasiado corta");
    if (contarItems(texto) < 3) motivos.push("trae menos de 3 consejos en viñetas o lista");
    if (noRespaldadas.length) revisar = 1;   // puede ser una sugerencia legítima («ahorra 200.000»); se revisa a mano
  }
  return { ok: motivos.length === 0 ? 1 : 0, motivos, noRespaldadas, revisar };
}

module.exports = { FRASE_RECHAZO, montos, permitidas, cercaDe, contarItems, validar };