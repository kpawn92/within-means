# Catálogo de KPIs financieros profesionales

Inventario de los indicadores que la arquitectura debe poder soportar. Cada KPI se mapea al contexto que lo habilita y a la fase del roadmap donde entra. **Ninguno se implementa en el MVP**; el MVP solo garantiza que los datos necesarios se capturan y que la arquitectura admite añadirlos después.

Fuente: indicadores de análisis financiero personal de nivel profesional/empresarial.

---

## 1. Rentabilidad y eficiencia

| KPI | Fórmula | Contexto habilitante | Decisión que dispara |
|---|---|---|---|
| **Margen de ahorro neto** | (Ingresos − Gastos) / Ingresos | `analytics` (MVP) | Cuánto invertir / cuánto recortar. <10% fragilidad; >20% saludable. |
| **Ratio gastos fijos / ingresos netos** | ΣFixed / NetIncome | `analytics` + `categories.nature` (MVP) | Refinanciar, mudarse, cancelar suscripciones. >50-60% poca maniobra. |
| **Coeficiente de Engel** | Gasto alimentación / Gasto total | `analytics` + `categories.engelGroup` (MVP) | Indicador clásico de bienestar; menor es mejor. |
| **ROAS personal** | Ingresos atribuibles / Gasto categoría productiva | `analytics` + `categories.productive` (MVP) | Qué gastos generan ingresos vs solo consumen. |

---

## 2. Liquidez y solvencia

| KPI | Fórmula | Contexto habilitante | Decisión que dispara |
|---|---|---|---|
| **Ratio de liquidez** | Activos líquidos / Gastos mensuales fijos | `assets` + `analytics` (MVP+3) | Cuándo dejar de ahorrar en cash y empezar a invertir. Estándar: 3-6 meses. |
| **DTI** (Debt-to-Income) | Pagos deuda / Ingreso bruto mensual | `liabilities` + `analytics` (MVP+3) | Capacidad de endeudamiento. >36% alto riesgo; >43% no hipoteca. |
| **DSCR** (Debt Service Coverage) | Ingreso operativo / Servicio de deuda | `liabilities` + `analytics` (MVP+3) | <1 = generas menos de lo que debes pagar. |

---

## 3. Variabilidad y riesgo

| KPI | Fórmula | Contexto habilitante | Decisión que dispara |
|---|---|---|---|
| **Coef. variación ingresos** | σ(ingresos) / μ(ingresos) | `analytics` snapshots (MVP+4) | Crítico para freelancers. >0.3 necesita colchón mayor. |
| **Índice de concentración (Herfindahl)** | Σ(share_i²) | `analytics` + `transactions.originRef` (MVP+4) | Diversificar si una fuente >40-50%. |
| **Varianza presupuestaria** | (Real − Presupuestado) / Presupuestado | `budgets` (MVP+2) | Identificar categorías donde sistemáticamente fallas. |
| **Stress test personal** | Simular caídas 20/40/100% × 1/3/6 meses | `forecasts` (MVP+4) | Cuánto resistes ante caídas. |

---

## 4. Tiempo y eficiencia operativa

| KPI | Fórmula | Contexto habilitante | Decisión que dispara |
|---|---|---|---|
| **Burn rate mensual** | Promedio gastos últimos 3 meses | `analytics` + `recurring` (MVP+1) | Métrica startup, igual de útil personalmente. |
| **Runway** | Activos líquidos / Burn rate | `assets` + `analytics` (MVP+3) | Cuándo aceptar trabajo peor pagado, cuándo emprender. |
| **Días de gasto cubiertos / día de ingreso** | Ratio gasto/ingreso invertido | `analytics` (MVP) | Cuántos días al mes "trabajas para ti". |
| **CAGR ingresos / patrimonio** | (Final/Initial)^(1/n) − 1 | `analytics` snapshots + `assets` (MVP+3) | Si avanzas o solo te mueves. |

---

## 5. Patrimonio y construcción de riqueza

| KPI | Fórmula | Contexto habilitante | Decisión que dispara |
|---|---|---|---|
| **Patrimonio neto** | Activos − Pasivos | `assets` + `liabilities` (MVP+3) | Métrica más importante a largo plazo. |
| **Tasa de crecimiento del patrimonio** | ΔPatrimonio / Patrimonio_inicial (MoM, YoY) | `assets` + `liabilities` snapshots (MVP+3) | Si es ≤0 con ingresos altos, algo estructural falla. |
| **Ratio activos productivos / total** | Σ productivos / Σ activos | `assets.productive` (MVP+3) | Indicador clave de independencia financiera. |
| **Número de Wealth** (Stanley & Danko) | (Edad × Ingreso anual) / 10 | `users.age` + `analytics` (MVP+3, requiere edad en perfil) | Patrimonio esperado a tu edad e ingreso. |
| **Ratio de independencia financiera** | Ingresos pasivos / Gastos totales | `transactions.incomeSource` + `analytics` (MVP+4) | =1 → no necesitas trabajar. |

---

## 6. Indicadores predictivos

| KPI | Fórmula / Método | Contexto habilitante | Decisión que dispara |
|---|---|---|---|
| **Proyección flujo de caja 12m** | Recurrencias + tendencia + estacionalidad | `forecasts` + `recurring` (MVP+4) | Anticipar baches de liquidez. |
| **Punto de equilibrio personal** | Ingreso mínimo para no descapitalizarte | `analytics` + `liabilities` (MVP+4) | Negociar salarios, fijar tarifas, aceptar proyectos. |
| **Tiempo a meta** | (Meta − Ahorro actual) / Tasa ahorro, ajustada por rendimiento | `goals` + `analytics` (MVP+4) | Plazos concretos a objetivos abstractos. |
| **Análisis de sensibilidad** | Variación KPIs con shocks (+10% tarifas, −1 cliente, +15% gastos fijos) | `forecasts` (MVP+4) | Decisiones bajo escenarios. |

---

## Mapa KPI ↔ contexto ↔ fase

```
MVP        : margen de ahorro neto, ratio gastos fijos, coef Engel, ROAS personal,
             días gasto/ingreso, descomposiciones básicas por categoría.

MVP+1      : burn rate (mejorado por recurring), separación fijo/variable cierta.

MVP+2      : varianza presupuestaria.

MVP+3      : patrimonio neto, ratio liquidez, DTI, DSCR, runway, CAGR, número de Wealth,
             ratio activos productivos, crecimiento patrimonio MoM/YoY.

MVP+4      : coef variación ingresos, Herfindahl, stress test, proyección 12m,
             punto de equilibrio, tiempo a meta, ratio independencia,
             análisis de sensibilidad.

MVP+5      : dashboard integrado con todos los anteriores + semáforos personalizables.
```

## Cuadro de mando final (visión)

El objetivo último (MVP+5) es un cuadro de mando con 8-12 KPIs clave seleccionables, actualizados mensualmente, con semáforos verde/amarillo/rojo según umbrales configurables por el usuario. Cada KPI en rojo dispara una sugerencia accionable.

Ejemplo de semáforos por defecto:

| KPI | Verde | Amarillo | Rojo |
|---|---|---|---|
| Margen ahorro | >20% | 10-20% | <10% |
| Ratio gastos fijos | <50% | 50-60% | >60% |
| DTI | <30% | 30-43% | >43% |
| DSCR | >1.5 | 1.0-1.5 | <1.0 |
| Ratio liquidez (meses) | >6 | 3-6 | <3 |
| Coef variación ingresos | <0.2 | 0.2-0.3 | >0.3 |
| Herfindahl concentración | <0.25 | 0.25-0.40 | >0.40 |
| Margen vs presupuesto | ±5% | ±10% | >±10% |

Estos umbrales son **propiedad del usuario** (configurables) — el sistema solo provee defaults.

## Próximos pasos

Cuando llegue MVP+5 y se materialice el `dashboard`, este catálogo se convertirá en el contrato funcional de los KPIs implementados. Mientras tanto, sirve como **norte arquitectónico** para validar que cada decisión del MVP no impide soportar estos indicadores después.
