/* screens-home.jsx — Home dashboard + QuickAdd hero sheet */
(function () {
  const { useState, useEffect, useRef, useMemo } = React;
  const { CatIcon, Donut, useCountUp } = window.UI;

  /* ---------- QuickAdd: the hero fast-entry sheet ---------- */
  function QuickAdd({ onClose, onSave, cats }) {
    const [type, setType] = useState("gasto");
    const [raw, setRaw] = useState(""); // string of digits incl optional dot
    const [catId, setCatId] = useState(null);
    const [note, setNote] = useState("");
    const [expanded, setExpanded] = useState(false);
    const [when, setWhen] = useState("Hoy");

    const visibleCats = cats.filter((c) => c.type === type);
    const cents = useMemo(() => {
      if (!raw) return 0;
      const n = parseFloat(raw);
      return Math.round((isNaN(n) ? 0 : n) * 100);
    }, [raw]);

    const press = (k) => {
      setRaw((r) => {
        if (k === "del") return r.slice(0, -1);
        if (k === ".") return r.includes(".") ? r : (r === "" ? "0." : r + ".");
        // limit 2 decimals
        if (r.includes(".") && r.split(".")[1].length >= 2) return r;
        if (r === "0") return k; // replace leading zero
        if (r.replace(".", "").length >= 9) return r;
        return r + k;
      });
    };

    const display = raw === "" ? "0" : raw;
    const canSave = cents > 0 && catId;
    const accent = type === "ingreso" ? "var(--pos)" : type === "transferencia" ? "var(--brand)" : "var(--neg)";

    const save = () => {
      if (!canSave) return;
      const c = cats.find((x) => x.id === catId);
      onSave({ amt: cents, cat: c.name, desc: note || c.name, when, type });
    };

    const KEYS = ["1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "del"];

    return (
      <div className="scrim" onClick={onClose}>
        <div className="sheet" style={{ height: "auto" }} onClick={(e) => e.stopPropagation()}>
          <div className="sheet-grip" />
          {/* type segmented */}
          <div style={{ padding: "4px 20px 0" }}>
            <div className="segmented">
              {[["gasto", "Gasto"], ["ingreso", "Ingreso"], ["transferencia", "Ahorro"]].map(([v, l]) => (
                <button key={v} className={type === v ? "on" : ""} onClick={() => { setType(v); setCatId(null); }}>{l}</button>
              ))}
            </div>
          </div>

          {/* amount display */}
          <div className="center" style={{ padding: "26px 20px 12px" }}>
            <div className="eyebrow" style={{ marginBottom: 6 }}>{type === "ingreso" ? "Entra" : type === "transferencia" ? "A ahorro" : "Gastas"}</div>
            <div className="amount" style={{ fontSize: 52, color: accent, lineHeight: 1 }}>
              <span style={{ fontSize: 30, verticalAlign: "8px", marginRight: 2, opacity: .7 }}>$</span>
              {display}
            </div>
            {/* selected category preview */}
            <div style={{ height: 22, marginTop: 10 }}>
              {catId && (
                <span className="small enter" style={{ fontWeight: 600, color: "var(--ink-2)" }}>
                  en {cats.find((c) => c.id === catId)?.name}{when !== "Hoy" ? ` · ${when}` : ""}
                </span>
              )}
            </div>
          </div>

          {/* quick category chips — tap to assign instantly */}
          <div style={{ display: "flex", gap: 8, overflowX: "auto", padding: "4px 20px 14px", scrollbarWidth: "none" }}>
            {visibleCats.map((c) => (
              <button key={c.id} onClick={() => setCatId(c.id)}
                style={{
                  flex: "none", display: "flex", flexDirection: "column", alignItems: "center", gap: 6,
                  width: 62, padding: "8px 4px", borderRadius: 16,
                  border: "1px solid " + (catId === c.id ? c.color : "var(--border)"),
                  background: catId === c.id ? c.color + "1a" : "var(--surface)",
                  transition: "all .14s",
                }}>
                <CatIcon cat={c} size="sm" />
                <span style={{ fontSize: 10.5, fontWeight: 600, color: "var(--ink-2)", whiteSpace: "nowrap", maxWidth: 56, overflow: "hidden", textOverflow: "ellipsis" }}>{c.name}</span>
              </button>
            ))}
          </div>

          {/* expandable extras */}
          {expanded && (
            <div className="enter" style={{ padding: "0 20px 8px", display: "flex", flexDirection: "column", gap: 10 }}>
              <input className="input" placeholder="Nota (opcional)" value={note} onChange={(e) => setNote(e.target.value)} />
              <div style={{ display: "flex", gap: 8 }}>
                {["Hoy", "Ayer", "Otra fecha"].map((w) => (
                  <button key={w} className={"chip" + (when === w ? " on" : "")} onClick={() => setWhen(w)} style={{ flex: 1, justifyContent: "center" }}>{w}</button>
                ))}
              </div>
            </div>
          )}

          {/* keypad */}
          <div style={{ padding: "6px 20px 0" }}>
            <div className="keypad">
              {KEYS.map((k) => (
                <button key={k} className="key" onClick={() => press(k)}>
                  {k === "del" ? <Icon name="backspace" size={24} /> : k}
                </button>
              ))}
            </div>
          </div>

          {/* footer actions */}
          <div style={{ display: "flex", gap: 10, padding: "12px 20px max(20px, env(safe-area-inset-bottom))" }}>
            <button className="btn btn-ghost" onClick={() => setExpanded((e) => !e)} style={{ flex: "none", width: 54 }} aria-label="Más opciones">
              <Icon name={expanded ? "down" : "chevron"} style={{ transform: expanded ? "rotate(-90deg)" : "rotate(90deg)" }} />
            </button>
            <button className="btn btn-primary btn-block" disabled={!canSave} onClick={save}
              style={canSave ? { background: accent, boxShadow: `0 6px 16px ${accent}44` } : null}>
              <Icon name="check" size={20} stroke={2.4} />
              {canSave ? `Guardar ${window.WM.fmtMoney(cents)}` : "Elige importe y categoría"}
            </button>
          </div>
        </div>
      </div>
    );
  }

  /* ---------- Home dashboard ---------- */
  function Home({ txns, cats, onOpenTxn, onSeeAll, onAdd, onSettings, budget }) {
    const WM = window.WM;
    // month totals
    const spent = txns.filter((t) => WM.typeOf(t.cat) === "gasto").reduce((s, t) => s + t.amt, 0);
    const income = txns.filter((t) => WM.typeOf(t.cat) === "ingreso").reduce((s, t) => s + t.amt, 0);
    const remaining = budget - spent;
    const pct = Math.min(1, spent / budget);
    const animSpent = useCountUp(spent);

    // top categories for donut
    const byCat = {};
    txns.filter((t) => WM.typeOf(t.cat) === "gasto").forEach((t) => {
      byCat[t.cat] = (byCat[t.cat] || 0) + t.amt;
    });
    const donutData = Object.entries(byCat)
      .map(([name, value]) => ({ name, value, color: WM.catBy(name).color }))
      .sort((a, b) => b.value - a.value);

    const recent = [...txns].sort((a, b) => b.date - a.date).slice(0, 4);
    const daysLeft = 30 - WM.today.getDate() + 8; // playful: ~21 left
    const perDay = remaining > 0 ? remaining / daysLeft : 0;

    const onTrack = remaining >= 0;

    return (
      <div className="viewport">
        <div className="pad">
          {/* greeting */}
          <div className="spread enter" style={{ padding: "10px 0 18px" }}>
            <div>
              <div className="eyebrow">Junio · {WM.user.name}</div>
              <div className="h2" style={{ marginTop: 3 }}>Buenas tardes 👋</div>
            </div>
            <button onClick={onSettings} aria-label="Ajustes" style={{ width: 44, height: 44, borderRadius: 999, background: "var(--brand)", color: "var(--on-brand)", display: "grid", placeItems: "center", fontWeight: 700, fontSize: 18 }}>
              {WM.user.initials}
            </button>
          </div>

          {/* hero balance card */}
          <div className="card card-pad enter" style={{ animationDelay: ".04s", background: "linear-gradient(160deg, var(--brand) 0%, var(--brand-strong) 100%)", border: "none", color: "var(--on-brand)", borderRadius: 28 }}>
            <div className="spread" style={{ gap: 10 }}>
              <span style={{ fontFamily: "var(--font-mono)", fontSize: 11, letterSpacing: ".1em", textTransform: "uppercase", opacity: .8 }}>Disponible</span>
              <span style={{ flex: "none", whiteSpace: "nowrap", display: "inline-flex", alignItems: "center", gap: 5, fontSize: 12, fontWeight: 600, background: "rgba(255,255,255,.18)", padding: "4px 10px", borderRadius: 999 }}>
                <Icon name={onTrack ? "check" : "bell"} size={13} stroke={2.5} /> {onTrack ? "Dentro del plan" : "Atención"}
              </span>
            </div>
            <div className="amount" style={{ fontSize: 42, marginTop: 12 }}>
              {WM.fmtMoney(remaining)}
            </div>
            <div style={{ marginTop: 16 }}>
              <div className="bar-track" style={{ background: "rgba(255,255,255,.22)" }}>
                <div className="bar-fill" style={{ width: `${pct * 100}%`, background: "var(--on-brand)", transition: "width .8s ease" }} />
              </div>
              <div className="spread" style={{ marginTop: 9, fontSize: 12.5, opacity: .9 }}>
                <span>Gastado {WM.fmtMoney(animSpent, "USD", { dp: 0 })}</span>
                <span>Plan {WM.fmtMoney(budget, "USD", { dp: 0 })}</span>
              </div>
            </div>
          </div>

          {/* daily allowance — novel: "puedes gastar X/día" */}
          <div className="card card-pad enter spread" style={{ animationDelay: ".08s", marginTop: 14 }}>
            <div className="row">
              <div style={{ width: 42, height: 42, borderRadius: 13, background: "var(--brand-soft)", color: "var(--brand-strong)", display: "grid", placeItems: "center", flex: "none" }}>
                <Icon name="spark" size={22} />
              </div>
              <div>
                <div className="small">Ritmo sugerido · {daysLeft} días restantes</div>
                <div className="h3" style={{ marginTop: 1 }}>{WM.fmtMoney(perDay, "USD", { dp: 0 })} / día</div>
              </div>
            </div>
            <Icon name="chevron" style={{ color: "var(--faint)" }} />
          </div>

          {/* spend breakdown donut */}
          <div className="card card-pad enter" style={{ animationDelay: ".12s", marginTop: 14 }}>
            <div className="spread" style={{ marginBottom: 6 }}>
              <span className="h3">En qué va el mes</span>
              <button className="small" style={{ fontWeight: 600, color: "var(--brand-strong)" }} onClick={onSeeAll}>Ver todo</button>
            </div>
            <div className="row" style={{ gap: 18, marginTop: 6 }}>
              <Donut data={donutData} size={128} thickness={18}>
                <div>
                  <div className="amount" style={{ fontSize: 19 }}>{WM.fmtMoney(spent, "USD", { dp: 0 })}</div>
                  <div className="small" style={{ fontSize: 10.5 }}>en gastos</div>
                </div>
              </Donut>
              <div className="grow" style={{ display: "flex", flexDirection: "column", gap: 9 }}>
                {donutData.slice(0, 4).map((d) => (
                  <div key={d.name} className="spread" style={{ fontSize: 13 }}>
                    <span className="row" style={{ gap: 8 }}>
                      <span style={{ width: 9, height: 9, borderRadius: 3, background: d.color }} />
                      <span style={{ fontWeight: 600, color: "var(--ink-2)" }}>{d.name}</span>
                    </span>
                    <span className="tnum" style={{ fontWeight: 700 }}>{Math.round(d.value / spent * 100)}%</span>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* recent activity */}
          <div className="enter" style={{ animationDelay: ".16s", marginTop: 22 }}>
            <div className="spread" style={{ marginBottom: 4 }}>
              <span className="h3">Reciente</span>
              <button className="small" style={{ fontWeight: 600, color: "var(--brand-strong)" }} onClick={onSeeAll}>Ver todo</button>
            </div>
            <div className="card" style={{ padding: "4px 14px" }}>
              {recent.map((t) => {
                const c = WM.catBy(t.cat);
                const inc = WM.typeOf(t.cat) === "ingreso";
                return (
                  <div className="txn" key={t.id} onClick={() => onOpenTxn(t)}>
                    <CatIcon cat={c} size="sm" />
                    <div className="meta">
                      <div className="t-name">{t.desc}</div>
                      <div className="t-sub">{c.name} · {WM.relLabel(t.date)}</div>
                    </div>
                    <div className="t-amt tnum" style={{ color: inc ? "var(--pos)" : "var(--ink)" }}>
                      {WM.fmtMoney(WM.signedAmt(t), "USD", { signed: true })}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </div>
    );
  }

  window.Screens = window.Screens || {};
  window.Screens.Home = Home;
  window.Screens.QuickAdd = QuickAdd;
})();
