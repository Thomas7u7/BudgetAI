# Benchmark interno de BudgetChat

Archivo: C:\BudgetAI\server/respuestas.json
Preguntas comparadas en igualdad de condiciones: 10 (3 coloquiales). Una corrida por modelo.

## Medidas por modelo

| Modelo | Correctas | Formales | Coloquiales | Cifras por revisar | Latencia mediana (ms) | Tokens de salida | Costo por 1.000 llamadas (USD) |
|---|---|---|---|---|---|---|---|
| gemini-3.6-flash | 10/10 | 7/7 | 3/3 | 2 | 1682 | 104 | 0.92 |
| qwen/qwen3.8-27b | 9/10 | 7/7 | 2/3 | 1 | 609 | 84 | 0.90 |
| nvidia/nemotron-3-ultra-550b-a55b:free | 10/10 | 7/7 | 3/3 | 2 | 3596 | 146 | 0.00 |

## Puntaje (0 a 100) con los pesos base

Pesos: corrección 50 %, lenguaje coloquial 15 %, cifras sin revisar 15 %, velocidad 10 %, costo 10 %.

| Puesto | Modelo | Puntaje |
|---|---|---|
| 1 | nvidia/nemotron-3-ultra-550b-a55b:free | 88.7 |
| 2 | gemini-3.6-flash | 80.6 |
| 3 | qwen/qwen3.8-27b | 78.7 |

## ¿Cambia el ganador si cambian los pesos?

| Escenario | nemotron-3-ultra-550b-a55b:free | gemini-3.6-flash | qwen3.8-27b | Ganador |
|---|---|---|---|---|
| Pesos base | 88.7 | 80.6 | 78.7 | nemotron-3-ultra-550b-a55b:free |
| Solo calidad | 96.0 | 96.0 | 85.3 | empate (nemotron-3-ultra-550b-a55b:free y gemini-3.6-flash) |
| Costo y velocidad | 73.1 | 48.9 | 64.3 | nemotron-3-ultra-550b-a55b:free |
| Todo igual | 79.4 | 63.2 | 69.7 | nemotron-3-ultra-550b-a55b:free |

## ¿Responden igual con lenguaje coloquial?

| Modelo | Pregunta coloquial | ¿Pasa la validación? | Resultado |
|---|---|---|---|
| gemini-3.6-flash | Parce, ¿cuántas lucas me quedan pa gastar? | sí |  |
| gemini-3.6-flash | Pille 20 lucas en un almuerzo, anótemelo | sí |  |
| gemini-3.6-flash | Ey, mejor cuénteme un chistecito pa no aburrirme | sí |  |
| qwen3.8-27b | Parce, ¿cuántas lucas me quedan pa gastar? | sí |  |
| qwen3.8-27b | Pille 20 lucas en un almuerzo, anótemelo | NO | monto: se esperaba 20000 y vino 200000; cifras que no salen del contexto: 200000 |
| qwen3.8-27b | Ey, mejor cuénteme un chistecito pa no aburrirme | sí |  |
| nemotron-3-ultra-550b-a55b:free | Parce, ¿cuántas lucas me quedan pa gastar? | sí |  |
| nemotron-3-ultra-550b-a55b:free | Pille 20 lucas en un almuerzo, anótemelo | sí |  |
| nemotron-3-ultra-550b-a55b:free | Ey, mejor cuénteme un chistecito pa no aburrirme | sí |  |

## Cómo leerlo (límites)

- Son pocas preguntas y una sola corrida por modelo: el puntaje es un indicio para decidir, no una estadística.
- Los pesos son una decisión del equipo; por eso se prueban otros escenarios. Si el ganador cambia, la conclusión depende de lo que se priorice.
- La corrección usa el validador de `validador.js` (cifras, etiqueta de registro y frase de rechazo). No mide si los consejos son sensatos.
- No mide disponibilidad: los cupos gratuitos son bajos (por ejemplo, los modelos `:free` de OpenRouter permiten unas 50 solicitudes al día sin créditos), y eso pesa en producción aunque el puntaje no lo vea.
- El costo y la velocidad se puntúan en relación con el mejor y el peor del grupo; con otros modelos en la comparación, los puntajes cambian.
- La latencia depende de la red y de la carga del proveedor en ese momento.