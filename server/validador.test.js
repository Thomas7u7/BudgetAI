const assert = require("assert");
const { validar, montos, FRASE_RECHAZO } = require("./validador");
const contexto = `Saldo disponible: $3.000.000 COP
Dinero apartado en metas: $2.000.000 COP
Ingresos registrados (total): $10.000.000 COP
Gastos registrados (total): $5.000.000 COP
Top categorías de gasto (30 días): Alimentación $900.000, Transporte $400.000
METAS: "Viaje": objetivo $4.000.000, ahorrado $2.000.000 (50%).`;
const cfg = { contexto, derivadas: [2000000, 5000000, 1300000] };
const P = {
  disp: { q: "¿Cuánto tengo disponible?", tipo: "dato", esperado: 3000000, clave: "disponible" },
  falta: { q: "¿Cuánto me falta para mi meta del viaje?", tipo: "dato", esperado: 2000000, clave: "falta|faltan|restan" },
  reg: { q: "Gasté 35000 en transporte", tipo: "registro", mov: { tipo: "gasto", monto: 35000, categoria: "Transporte" } },
  rech: { q: "Cuéntame un chiste", tipo: "rechazo" },
  cons: { q: "Dame 3 consejos para ahorrar más", tipo: "consejo" },
};
const v = (p, t) => validar(p, t, cfg).ok;

// parser
assert.deepStrictEqual(montos("$3.000.000 y 3 millones y 35 mil y 35000, 30 días, 50%, 2026").map((x) => x.valor), [3e6, 3e6, 35000, 35000]);

// datos: correctos
assert.strictEqual(v(P.disp, "Tienes disponible $3.000.000 COP."), 1);
assert.strictEqual(v(P.disp, "Tu saldo disponible es de 3 millones de pesos."), 1);
assert.strictEqual(v(P.falta, "Te faltan $2.000.000 COP para tu meta del viaje (50% ahorrado)."), 1);
// datos: incorrectos o inventados
assert.strictEqual(v(P.disp, "Tienes disponible $5.500.000 COP."), 0);
assert.strictEqual(v(P.disp, "Tienes disponible $3.000.000 COP, más $750.000 de intereses."), 0);   // cifra inventada
assert.strictEqual(v(P.falta, "Te faltan $4.000.000 COP para tu meta."), 0);                        // cifra del contexto pero equivocada
assert.strictEqual(v(P.falta, "Te falta mucho, sigue ahorrando."), 0);
// registro
assert.strictEqual(v(P.reg, 'Listo, registré tu gasto.\n[[MOV:{"tipo":"gasto","monto":35000,"categoria":"Transporte","descripcion":"Transporte"}]]'), 1);
assert.strictEqual(v(P.reg, 'Listo.\n[[MOV:{"tipo":"gasto","monto":53000,"categoria":"Transporte"}]]'), 0);
assert.strictEqual(v(P.reg, 'Listo.\n[[MOV:{"tipo":"ingreso","monto":35000,"categoria":"Transporte"}]]'), 0);
assert.strictEqual(v(P.reg, "Listo, registré tu gasto de $35.000."), 0);                             // sin etiqueta
// rechazo
assert.strictEqual(v(P.rech, FRASE_RECHAZO), 1);
assert.strictEqual(v(P.rech, `"${FRASE_RECHAZO}"`), 1);
assert.strictEqual(v(P.rech, "¿Qué le dice un pez a otro? ¡Nada! " + FRASE_RECHAZO), 0);
assert.strictEqual(v(P.rech, "Claro, aquí va un chiste: ..."), 0);
assert.strictEqual(v(P.rech, FRASE_RECHAZO + '\n[[MOV:{"tipo":"gasto","monto":1,"categoria":"Otro gasto"}]]'), 0);
// consejos
assert.strictEqual(v(P.cons, "Claro:\n- Fija un presupuesto mensual para tu comida.\n- Aparta el ahorro apenas recibas ingresos.\n- Revisa tus suscripciones cada mes."), 1);
assert.strictEqual(v(P.cons, "Ahorra más."), 0);
assert.strictEqual(validar(P.cons, "- Aparta $250.000 al mes\n- Reduce comidas fuera de casa\n- Revisa suscripciones de la app", cfg).revisar, 1);
// miles separados por espacio (formato que usó gpt-oss: «$900 000», «$4 000 000»)
assert.deepStrictEqual(montos("$900 000 y $4 000 000 y $90 000").map((x) => x.valor), [900000, 4000000, 90000]);
assert.strictEqual(v(P.disp, "Tienes disponible $3 000 000 COP."), 1);
assert.deepStrictEqual(validar(P.cons, "- Reduce $900 000 en comida\n- Ahorra $90 000 al mes\n- Revisa suscripciones", cfg).noRespaldadas, [90000]);
// sugerencias con cifras propias en una respuesta de dato: no es falla, se revisa a mano
const sug = validar(P.falta, "Te faltan $2.000.000 COP.\n- Reducir un 10 % en comida te libera $90.000 al mes.\nCon un aporte de $250.000 COP al mes, lograrías la meta en 8 meses.", cfg);
assert.strictEqual(sug.ok, 1); assert.strictEqual(sug.revisar, 1);
// afirmar un dato del usuario que no existe sí es falla
assert.strictEqual(v(P.falta, "Te faltan $2.000.000 COP. Tus ingresos del mes fueron $12.000.000 COP."), 0);
// abreviaturas que usó Nemotron: «900 k», «2 M», «500 k‑1 M»
assert.deepStrictEqual(montos("faltan 2 M, ahorra 200 k al mes, 5 M de 10 M, camina 5 m").map((x) => x.valor), [2e6, 200000, 5e6, 10e6]);
// registro coloquial: «20 lucas» = 20000. El monto pedido no es una cifra inventada; 200000 sí es un error de monto.
const REGC = { q: "Pille 20 lucas en un almuerzo, anótemelo", tipo: "registro", mov: { tipo: "gasto", monto: 20000, categoria: "Alimentación" } };
assert.strictEqual(v(REGC, 'Listo, anoté el almuerzo de $20.000 en Alimentación.\n[[MOV:{"tipo":"gasto","monto":20000,"categoria":"Alimentación","descripcion":"Almuerzo"}]]'), 1);
assert.strictEqual(v(REGC, 'Listo, registré $200.000.\n[[MOV:{"tipo":"gasto","monto":200000,"categoria":"Alimentación"}]]'), 0);
assert.strictEqual(v(REGC, 'Listo, anotado.\n[[MOV:{"tipo":"gasto","monto":20000,"categoria":"Alimentación"}]]\nAdemás tus ingresos son $12.000.000.'), 0);
console.log("validador: todas las pruebas pasan");