// Genera respuestas.html: una tarjeta por pregunta, con la respuesta de cada modelo lado a lado.
const esc = (s) => String(s ?? "").replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));

function generarHtml(contexto, respuestas) {
  const preguntas = [...new Set(respuestas.map((r) => r.pregunta))];
  const modelos = [...new Set(respuestas.map((r) => r.modelo))];
  const tipos = { dato: "Dato", consejo: "Consejo", registro: "Registro", rechazo: "Rechazo" };
  const totales = modelos.map((m) => {
    const rs = respuestas.filter((r) => r.modelo === m && r.validacion);
    return `<li><b>${esc(m)}</b>: ${rs.filter((r) => r.validacion.ok).length} de ${rs.length} correctas${rs.some((r) => r.error) ? `, ${respuestas.filter((r) => r.modelo === m && r.error).length} con error` : ""}</li>`;
  }).join("");
  const tarjetas = preguntas.map((q) => {
    const fila = respuestas.filter((r) => r.pregunta === q);
    const cols = modelos.map((m) => {
      const r = fila.find((x) => x.modelo === m);
      if (!r) return `<div class="col"><h4>${esc(m)}</h4><p class="vacio">Sin datos</p></div>`;
      if (r.error) return `<div class="col err"><h4>${esc(m)}</h4><span class="tag mal">Error</span><p class="vacio">${esc(r.error)}</p></div>`;
      const v = r.validacion;
      const sello = v.ok ? '<span class="tag bien">✓ Correcta</span>' : '<span class="tag mal">✗ Falla</span>';
      const aviso = v.revisar ? `<span class="tag rev">⚠ Revisar cifras: ${esc(v.noRespaldadas.join(", "))}</span>` : "";
      const motivos = v.motivos.length ? `<ul class="motivos">${v.motivos.map((x) => `<li>${esc(x)}</li>`).join("")}</ul>` : "";
      const meta = r.tin ? `<p class="meta">${r.tin} tokens entrada · ${r.tout} salida · ${r.ms} ms</p>` : "";
      return `<div class="col ${v.ok ? "" : "falla"}"><h4>${esc(m)}</h4>${sello}${aviso}<pre>${esc(r.texto)}</pre>${motivos}${meta}</div>`;
    }).join("");
    const tipo = tipos[fila[0].tipo] || fila[0].tipo;
    return `<section><header><span class="tipo">${esc(tipo)}</span><h3>${esc(q)}</h3></header><div class="cols">${cols}</div></section>`;
  }).join("");
  return `<!doctype html><html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Respuestas de los modelos</title><style>
:root{--bg:#f6f4ee;--card:#fff;--tx:#10202b;--mut:#52606b;--line:#ddd9ce;--ok:#1f7a70;--okbg:#e2f1ee;--mal:#b3541e;--malbg:#fbe9df;--rev:#8a6410;--revbg:#fbf0d4}
@media(prefers-color-scheme:dark){:root{--bg:#10202b;--card:#17303f;--tx:#f6f4ee;--mut:#a9bcc6;--line:#2a4656;--ok:#5cc4b6;--okbg:#14433f;--mal:#f0a070;--malbg:#4a2b1a;--rev:#e9a23b;--revbg:#44371a}}
*{box-sizing:border-box}body{margin:0;padding:24px 16px 48px;background:var(--bg);color:var(--tx);font:16px/1.5 system-ui,sans-serif}
main{max-width:1200px;margin:0 auto}h1{font-size:28px;margin:0 0 4px}.sub{color:var(--mut);margin:0 0 20px}
details{background:var(--card);border:1px solid var(--line);border-radius:12px;padding:12px 16px;margin-bottom:16px}summary{cursor:pointer;font-weight:600}
.res{background:var(--card);border:1px solid var(--line);border-radius:12px;padding:12px 16px 12px 36px;margin:0 0 24px}
section{background:var(--card);border:1px solid var(--line);border-radius:14px;padding:16px;margin-bottom:20px}
header{display:flex;gap:12px;align-items:center;flex-wrap:wrap;margin-bottom:12px}h3{margin:0;font-size:20px}
.tipo{font-size:12px;letter-spacing:.08em;text-transform:uppercase;color:var(--mut);border:1px solid var(--line);border-radius:999px;padding:2px 10px}
.cols{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:12px}
.col{border:1px solid var(--line);border-radius:10px;padding:12px;min-width:0}.col.falla{border-color:var(--mal)}h4{margin:0 0 8px;font-size:15px;word-break:break-all}
pre{white-space:pre-wrap;word-wrap:break-word;font:15px/1.5 inherit;font-family:inherit;margin:10px 0 0}
.tag{display:inline-block;font-size:13px;font-weight:600;border-radius:6px;padding:2px 8px;margin:0 6px 4px 0}
.bien{color:var(--ok);background:var(--okbg)}.mal{color:var(--mal);background:var(--malbg)}.rev{color:var(--rev);background:var(--revbg)}
.motivos{margin:8px 0 0;padding-left:18px;color:var(--mal);font-size:14px}.meta{color:var(--mut);font-size:13px;margin:10px 0 0}.vacio{color:var(--mut)}
code{white-space:pre-wrap}</style></head><body><main>
<h1>Qué responde cada modelo</h1><p class="sub">Mismas preguntas, mismo prompt y mismo contexto. Cada respuesta se validó con reglas automáticas.</p>
<ul class="res">${totales}</ul>
<details><summary>Contexto financiero entregado a los modelos</summary><pre>${esc(contexto)}</pre></details>
${tarjetas}</main></body></html>`;
}
module.exports = { generarHtml };