# Documentación de `within-means`

Índice maestro. Cada sección agrupa los .md por tema. Orden de lectura recomendado para alguien nuevo en el proyecto:

1. [`architecture/overview.md`](architecture/overview.md) — qué arquitectura usamos y por qué.
2. [`contexts/README.md`](contexts/README.md) — mapa de bounded contexts.
3. [`persistence/overview.md`](persistence/overview.md) — cómo persistimos datos.
4. [`scalability/strategy.md`](scalability/strategy.md) — cómo crece sin reescribirse.
5. [`roadmap/mvp.md`](roadmap/mvp.md) — qué construimos primero.

## Mapa de la documentación

```
doc/
├── README.md                       (estás aquí)
├── architecture/
│   ├── overview.md                 Hexagonal + DDD + CQRS, kernel, buses, Event Store
│   ├── conventions.md              Naming, capas, estilo Kotlin, tests
│   └── module-structure.md         Estructura Gradle, version catalog, targets KMP
├── contexts/
│   ├── README.md                   Mapa: contextos MVP vs futuros, relaciones, eventos
│   ├── mvp.md                      Contextos del MVP: users, categories, transactions, analytics
│   └── future.md                   Contextos post-MVP: accounts, budgets, recurring, assets, liabilities, forecasts, goals, dashboard
├── persistence/
│   └── overview.md                 SQLDelight, SQLCipher, UUID v4, mapping, Criteria → SQL, Event Store, snapshots
├── scalability/
│   ├── strategy.md                 Cómo escala la arquitectura: patrones, decisiones tempranas, plan de crecimiento
│   └── kpi-catalog.md              Catálogo de KPIs financieros profesionales mapeados a contextos
└── roadmap/
    ├── mvp.md                      Fases 1-6: bootstrap, kernel, categories, transactions, analytics básico, Android
    └── post-mvp.md                 Fases 7+: recurring, accounts, assets, liabilities, forecasts, goals, dashboard, Desktop
```

## Mapa por intención

| Si quieres... | Lee... |
|---|---|
| Entender la arquitectura | [`architecture/overview.md`](architecture/overview.md) |
| Saber qué contextos hay | [`contexts/README.md`](contexts/README.md) |
| Ver el modelo de un contexto MVP | [`contexts/mvp.md`](contexts/mvp.md) |
| Saber qué llega después del MVP | [`contexts/future.md`](contexts/future.md) |
| Configurar Gradle / módulos | [`architecture/module-structure.md`](architecture/module-structure.md) |
| Aplicar convenciones de naming | [`architecture/conventions.md`](architecture/conventions.md) |
| Modelar persistencia | [`persistence/overview.md`](persistence/overview.md) |
| Entender cómo escala el sistema | [`scalability/strategy.md`](scalability/strategy.md) |
| Ver los KPIs profesionales objetivo | [`scalability/kpi-catalog.md`](scalability/kpi-catalog.md) |
| Saber qué se construye y cuándo | [`roadmap/mvp.md`](roadmap/mvp.md) / [`roadmap/post-mvp.md`](roadmap/post-mvp.md) |

## Convenciones de la documentación

- **Idioma:** español para la prosa, inglés para nombres técnicos y código.
- **Enlaces relativos:** entre archivos en `doc/`, usar paths relativos (`../persistence/overview.md`). Desde el README raíz, usar `doc/...`.
- **Frescura:** si una decisión cambia, actualizar el .md correspondiente en el mismo PR. Doc obsoleta es peor que doc inexistente.
- **No duplicar:** cada hecho vive en un único sitio canónico; el resto enlaza.
