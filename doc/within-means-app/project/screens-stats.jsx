/* screens-stats.jsx — analytics: trend bars, category breakdown, insights */
(function () {
  const { useState, useMemo } = React;
  const { CatIcon, Donut } = window.UI;

  function Stats({ txns, cats }) {
    const W = window.WM;
    const [period, setPeriod] = useState("mes");
    const [lens, setLens] = useState("categoria"); // categoria | esencial

    const expenses = txns.filter((t) => W.typeOf(t.cat) === "gasto");
    const income = txns.filter((t) => W.typeOf(t.cat) === "ingreso").reduce((s, t) => s + t.amt, 0);
    const spent = expenses.reduce((s, t) => s + t.amt, 0);
    const saved = income - spent;
    const savedPct = income ? Math.round(saved / income * 100) : 0;

    // by category
    const byCat = {};
    expenses.forEach((t) => { byCat[t.cat] = (byCat[t.cat] || 0) + t.amt; });
    const catRows = Object.entries(byCat).map(([n, v]) => ({ name: n, value: v, color: W.catBy(n).color }))
      .sort((a, b) => b.value - a.value);

    // by essential/discretionary
    const ess = { ESENCIAL: 0, DISCRECIONAL: 0 };
    expenses.forEach((t) => { const c = W.catBy(t.cat); ess[c.ess || "DISCRECIONAL"] += t.amt; });
    const essRows = [
      { name: "Esencial", value: ess.ESENCIAL, color: "#3F8F6B" },
      { name: "Discrecional", value: ess.DISCRECIONAL, color: "#D4A12E" },
    ];

    const rows = lens === "categoria" ? catRows : essRows;
    const max = Math.max(...rows.map((r) => r.value), 1);

    // weekly trend (mock 6 weeks)
    const weeks = [
      { label: "S1", g: 32000 }, { label: "S2", g: 41000 }, { label: "S3", g: 38500 },
      { label: "S4", g: 29000 }, { label: "S5", g: 44200 }, { label: "S6", g: spent > 90000 ? 36000 : spent },
    ];
    const wmax = Math.max(...weeks.map((w) => w.g));
    const avgWeek = Math.round(weeks.reduce((s, w) => s + w.g, 0) / weeks.length);

    return (
      <div className="viewport">
        <div className="topbar"><span className="title">Análisis</span></div>
        <div className="pad" style={{ paddingTop: 4 }}>
          {/* period segmented */}
          <div className="segmented" style={{ marginBottom: 18 }}>
            {[["semana", "Semana"], ["mes", "Mes"], ["año", "Año"]].map(([v, l]) => (
              <button key={v} className={period === v ? "on" : ""} onClick={() => setPeriod(v)}>{l}</button>
            ))}
          </div>

          {/* summary trio */}
          <div style={{ display: "grid", gridTemplateColumns: "repeat(3,1fr)", gap: 10, marginBottom: 16 }}>
            {[
              { l: "Ingresos", v: income, c: "var(--pos)", ic: "arrowdown" },
              { l: "Gastos", v: spent, c: "var(--neg)", ic: "arrowup" },
              { l: "Ahorro", v: saved, c: "var(--brand-strong)", ic: "flag" },
            ].map((s) => (
              <div key={s.l} className="card card-pad" style={{ padding: 14 }}>
                <Icon name={s.ic} size={17} style={{ color: s.c }} />
                <div className="amount" style={{ fontSize: 16, marginTop: 8 }}>{W.fmtMoney(s.v, "USD", { dp: 0 })}</div>
                <div className="small" style={{ fontSize: 11 }}>{s.l}</div>
              </div>
            ))}
          </div>

          {/* savings rate insight */}
          <div className="card card-pad" style={{ marginBottom: 16, background: "var(--brand-soft)", border: "1px solid transparent" }}>
            <div className="row" style={{ gap: 14 }}>
              <Donut data={[{ value: savedPct, color: "var(--brand)" }, { value: 100 - savedPct, color: "var(--surface-3)" }]} size={64} thickness={10} gap={0}>
                <span className="amount" style={{ fontSize: 15, color: "var(--brand-strong)" }}>{savedPct}%</span>
              </Donut>
              <div className="grow">
                <div className="h3" style={{ color: "var(--brand-strong)" }}>Tasa de ahorro saludable</div>
                <div className="small" style={{ color: "var(--ink-2)", marginTop: 2 }}>Guardaste {W.fmtMoney(saved, "USD", { dp: 0 })} de lo que entró. Vas mejor que el mes pasado.</div>
              </div>
            </div>
          </div>

          {/* weekly trend bars */}
          <div className="card card-pad" style={{ marginBottom: 16 }}>
            <div className="spread" style={{ marginBottom: 16 }}>
              <span className="h3">Evolución del gasto</span>
              <span className="small">media {W.fmtMoney(avgWeek, "USD", { dp: 0 })}</span>
            </div>
            <div style={{ display: "flex", alignItems: "flex-end", gap: 10, height: 130 }}>
              {weeks.map((w, i) => (
                <div key={i} style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", gap: 8, height: "100%", justifyContent: "flex-end" }}>
                  <div style={{ width: "100%", maxWidth: 26, height: `${w.g / wmax * 100}%`, borderRadius: 7, background: i === weeks.length - 1 ? "var(--brand)" : "var(--surface-3)", transition: "height .6s ease", position: "relative" }} />
                  <span className="small" style={{ fontSize: 10.5 }}>{w.label}</span>
                </div>
              ))}
            </div>
          </div>

          {/* breakdown */}
          <div className="card card-pad">
            <div className="spread" style={{ marginBottom: 14 }}>
              <span className="h3">Desglose</span>
              <div className="segmented" style={{ padding: 3, gap: 2 }}>
                {[["categoria", "Categoría"], ["esencial", "Tipo"]].map(([v, l]) => (
                  <button key={v} className={lens === v ? "on" : ""} onClick={() => setLens(v)} style={{ padding: "6px 12px", fontSize: 12 }}>{l}</button>
                ))}
              </div>
            </div>
            <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
              {rows.map((r) => (
                <div key={r.name}>
                  <div className="spread" style={{ marginBottom: 6, fontSize: 13.5 }}>
                    <span style={{ fontWeight: 600, color: "var(--ink-2)" }}>{r.name}</span>
                    <span className="tnum" style={{ fontWeight: 700 }}>{W.fmtMoney(r.value, "USD", { dp: 0 })}</span>
                  </div>
                  <div className="bar-track">
                    <div className="bar-fill" style={{ width: `${r.value / max * 100}%`, background: r.color, transition: "width .6s ease" }} />
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    );
  }

  window.Screens = window.Screens || {};
  window.Screens.Stats = Stats;
})();
