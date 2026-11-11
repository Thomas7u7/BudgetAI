// Benchmark interno: ¿cuál modelo es mejor PARA BudgetChat?
// Lee respuestas.json (lo genera comparar.js) y calcula un puntaje de 0 a 100 con pesos explícitos.
// Uso:  node benchmark.js            (no llama a ninguna API ni necesita claves)
// Salidas: benchmark.csv y benchmark.md
const fs = require("fs");

const ARCHIVO = process.argv[2] || __dirname + "/respuestas.json";
const { respuestas } = JSON.parse(fs.readFileSync(ARCHIVO, "utf8"));

// Preguntas coloquiales (por si el archivo es anterior y no trae el campo «grupo»).
const COLOQUIALES = new Set([
  "Parce, ¿cuántas lucas me quedan pa gastar?",
  "Pille 20 lucas en un almuerzo, anótemelo",
  "Ey, mejor cuénteme un chistecito pa no aburrirme",
]);
const grupoDe = (r) => r.grupo || (COLOQUIALES.has(r.pregunta) ? "coloquial" : "base");

// Pesos del puntaje (suman 1). Se pueden cambiar; abajo se prueba qué pasa con otros pesos.
const PESOS = { correccion: 0.50, coloquial: 0.15, cifras: 0.15, velocidad: 0.10, costo: 0.10 };
const ESCENARIOS = {
  "Pesos base": PESOS,
  "Solo calidad": { correccion: 0.6, coloquial: 0.2, cifras: 0.2, velocidad: 0, costo: 0 },
  "Costo y velocidad": { correccion: 0.2, coloquial: 0.1, cifras: 0.1, velocidad: 0.3, costo: 0.3 },
  "Todo igual": { correccion: 0.2, coloquial: 0.2, cifras: 0.2, velocidad: 0.2, costo: 0.2 },
};

// Costo por llamada: usa los precios guardados en el archivo si existen; si no, lo recalcula con los de comparar.js.
const PRECIOS = { "gemini-3.6-flash": [0.75, 3.75], "openai/gpt-oss-120b": [0.15, 0.60], "qwen/qwen3.8-27b": [0.80, 4.00], "nvidia/nemotron-3-ultra-550b-a55b:free": [0, 0] };
const costo = (r) => { const p = PRECIOS[r.modelo] || [0, 0]; return (r.tin * p[0] + r.tout * p[1]) / 1e6; };
const mediana = (a) => { const s = [...a].sort((x, y) => x - y); const m = s.length >> 1; return s.length % 2 ? s[m] : (s[m - 1] + s[m]) / 2; };

const modelos = [...new Set(respuestas.map((r) => r.modelo))];
// Solo cuentan las preguntas que respondieron TODOS los modelos (igualdad de condiciones).
const preguntas = [...new Set(respuestas.map((r) => r.pregunta))].filter((q) =>
  modelos.every((m) => respuestas.some((r) => r.modelo === m && r.pregunta === q && !r.error && r.texto)));
const filas = respuestas.filter((r) => preguntas.includes(r.pregunta) && !r.error && r.texto);

const medidas = modelos.map((m) => {
  const f = filas.filter((r) => r.modelo === m);
  const base = f.filter((r) => grupoDe(r) === "base"), col = f.filter((r) => grupoDe(r) === "coloquial");
  const ok = (a) => a.filter((r) => r.validacion?.ok).length;
  return {
    modelo: m,
    n: f.length, n_base: base.length, n_col: col.length,
    correctas: ok(f), correctas_base: ok(base), correctas_col: ok(col),
    revisar: f.filter((r) => r.validacion?.revisar).length,
    lat_mediana_ms: mediana(f.map((r) => r.ms)),
    tokens_out: f.reduce((a, r) => a + r.tout, 0) / (f.length || 1),
    costo_1000: (f.reduce((a, r) => a + costo(r), 0) / (f.length || 1)) * 1000,
  };
});

const latMin = Math.min(...medidas.map((x) => x.lat_mediana_ms));
const costoMax = Math.max(...medidas.map((x) => x.costo_1000));
for (const x of medidas) {
  x.p = {
    correccion: x.correctas / (x.n || 1),
    // Si no hay preguntas coloquiales en el archivo, ese criterio se apaga (no se inventa).
    coloquial: x.n_col ? x.correctas_col / x.n_col : null,
    cifras: 1 - x.revisar / (x.n || 1),
    velocidad: latMin / x.lat_mediana_ms,
    costo: costoMax === 0 ? 1 : 1 - x.costo_1000 / costoMax,
  };
}
function puntaje(x, pesos) {
  let suma = 0, total = 0;
  for (const [k, w] of Object.entries(pesos)) { if (x.p[k] == null || w === 0) continue; suma += w * x.p[k]; total += w; }
  return total ? (100 * suma) / total : 0;   // se reescala si algún criterio no aplica
}

const salida = [];
const log = (s = "") => { console.log(s); salida.push(s); };
log(`# Benchmark interno de BudgetChat`);
log(`\nArchivo: ${ARCHIVO}`);
log(`Preguntas comparadas en igualdad de condiciones: ${preguntas.length} (${preguntas.filter((q) => COLOQUIALES.has(q)).length} coloquiales). Una corrida por modelo.\n`);
log("## Medidas por modelo\n");
log("| Modelo | Correctas | Formales | Coloquiales | Cifras por revisar | Latencia mediana (ms) | Tokens de salida | Costo por 1.000 llamadas (USD) |");
log("|---|---|---|---|---|---|---|---|");
for (const x of medidas)
  log(`| ${x.modelo} | ${x.correctas}/${x.n} | ${x.correctas_base}/${x.n_base} | ${x.n_col ? `${x.correctas_col}/${x.n_col}` : "sin datos"} | ${x.revisar} | ${Math.round(x.lat_mediana_ms)} | ${x.tokens_out.toFixed(0)} | ${x.costo_1000.toFixed(2)} |`);

log("\n## Puntaje (0 a 100) con los pesos base\n");
log(`Pesos: corrección ${PESOS.correccion * 100} %, lenguaje coloquial ${PESOS.coloquial * 100} %, cifras sin revisar ${PESOS.cifras * 100} %, velocidad ${PESOS.velocidad * 100} %, costo ${PESOS.costo * 100} %.\n`);
const ranking = [...medidas].sort((a, b) => puntaje(b, PESOS) - puntaje(a, PESOS));
log("| Puesto | Modelo | Puntaje |");
log("|---|---|---|");
ranking.forEach((x, i) => log(`| ${i + 1} | ${x.modelo} | ${puntaje(x, PESOS).toFixed(1)} |`));

log("\n## ¿Cambia el ganador si cambian los pesos?\n");
log("| Escenario | " + ranking.map((x) => x.modelo.split("/").pop()).join(" | ") + " | Ganador |");
log("|---|" + ranking.map(() => "---").join("|") + "|---|");
for (const [nombre, pesos] of Object.entries(ESCENARIOS)) {
  const pts = ranking.map((x) => puntaje(x, pesos));
  const orden = pts.map((v, i) => [v, i]).sort((p, q) => q[0] - p[0]);
  const empate = orden.length > 1 && orden[0][0] - orden[1][0] < 1;   // menos de 1 punto: no es una diferencia que se pueda defender
  const g = empate ? `empate (${ranking[orden[0][1]].modelo.split("/").pop()} y ${ranking[orden[1][1]].modelo.split("/").pop()})` : ranking[orden[0][1]].modelo.split("/").pop();
  log(`| ${nombre} | ${pts.map((v) => v.toFixed(1)).join(" | ")} | ${g} |`);
}

const col = preguntas.filter((q) => COLOQUIALES.has(q));
if (col.length) {
  log("\n## ¿Responden igual con lenguaje coloquial?\n");
  log("| Modelo | Pregunta coloquial | ¿Pasa la validación? | Resultado |");
  log("|---|---|---|---|");
  for (const x of medidas) for (const q of col) {
    const r = filas.find((y) => y.modelo === x.modelo && y.pregunta === q);
    log(`| ${x.modelo.split("/").pop()} | ${q} | ${r.validacion?.ok ? "sí" : "NO"} | ${r.validacion?.ok ? "" : (r.validacion?.motivos || []).join("; ")} |`);
  }
}

log("\n## Cómo leerlo (límites)\n");
log("- Son pocas preguntas y una sola corrida por modelo: el puntaje es un indicio para decidir, no una estadística.");
log("- Los pesos son una decisión del equipo; por eso se prueban otros escenarios. Si el ganador cambia, la conclusión depende de lo que se priorice.");
log("- La corrección usa el validador de `validador.js` (cifras, etiqueta de registro y frase de rechazo). No mide si los consejos son sensatos.");
log("- No mide disponibilidad: los cupos gratuitos son bajos (por ejemplo, los modelos `:free` de OpenRouter permiten unas 50 solicitudes al día sin créditos), y eso pesa en producción aunque el puntaje no lo vea.");
log("- El costo y la velocidad se puntúan en relación con el mejor y el peor del grupo; con otros modelos en la comparación, los puntajes cambian.");
log("- La latencia depende de la red y de la carga del proveedor en ese momento.");
fs.writeFileSync(__dirname + "/benchmark.md", salida.join("\n"));
fs.writeFileSync(__dirname + "/benchmark.csv",
  ["modelo,correctas,n,correctas_coloquiales,n_coloquiales,cifras_por_revisar,lat_mediana_ms,tokens_out,costo_por_1000_usd,puntaje_base"]
    .concat(medidas.map((x) => [x.modelo, x.correctas, x.n, x.correctas_col, x.n_col, x.revisar, Math.round(x.lat_mediana_ms), x.tokens_out.toFixed(0), x.costo_1000.toFixed(4), puntaje(x, PESOS).toFixed(1)].join(","))).join("\n"));
console.log("\nGuardado: benchmark.md y benchmark.csv");