/* screens-txn.jsx — Transactions list + full transaction editor */
(function () {
  const { useState, useMemo } = React;
  const { CatIcon } = window.UI;
  const WM = () => window.WM;

  /* ---------- Transactions list ---------- */
  function Txns({ txns, onOpenTxn, onAdd }) {
    const W = WM();
    const [q, setQ] = useState("");
    const [filter, setFilter] = useState("todos");

    const filtered = txns
      .filter((t) => filter === "todos" || W.typeOf(t.cat) === filter)
      .filter((t) => {
        if (!q) return true;
        const s = q.toLowerCase();
        return t.desc.toLowerCase().includes(s) || t.cat.toLowerCase().includes(s);
      })
      .sort((a, b) => b.date - a.date);

    // group by relative day label
    const groups = [];
    filtered.forEach((t) => {
      const label = W.relLabel(t.date);
      let g = groups.find((x) => x.label === label);
      if (!g) { g = { label, items: [], total: 0 }; groups.push(g); }
      g.items.push(t);
      g.total += W.signedAmt(t);
    });

    const monthSpent = txns.filter((t) => W.typeOf(t.cat) === "gasto").reduce((s, t) => s + t.amt, 0);

    return (
      <div className="viewport">
        <div className="topbar" style={{ flexDirection: "column", alignItems: "stretch", gap: 12, paddingBottom: 10 }}>
          <div className="spread">
            <span className="title">Movimientos</span>
            <span className="small tnum" style={{ fontWeight: 600 }}>{W.fmtMoney(monthSpent, "USD", { dp: 0 })} en junio</span>
          </div>
          <div style={{ position: "relative" }}>
            <Icon name="eye" size={18} style={{ position: "absolute", left: 14, top: 13, color: "var(--faint)", display: "none" }} />
            <input className="input" placeholder="Buscar movimiento…" value={q} onChange={(e) => setQ(e.target.value)} style={{ background: "var(--surface)" }} />
          </div>
          <div style={{ display: "flex", gap: 8, overflowX: "auto", scrollbarWidth: "none", margin: "0 -20px", padding: "0 20px" }}>
            {[["todos", "Todos"], ["gasto", "Gastos"], ["ingreso", "Ingresos"], ["transferencia", "Ahorro"]].map(([v, l]) => (
              <button key={v} className={"chip" + (filter === v ? " on" : "")} onClick={() => setFilter(v)}>{l}</button>
            ))}
          </div>
        </div>

        <div className="pad" style={{ paddingTop: 4 }}>
          {groups.length === 0 && (
            <div className="center" style={{ padding: "60px 20px", color: "var(--muted)" }}>
              <Icon name="list" size={36} style={{ opacity: .4 }} />
              <div className="body" style={{ marginTop: 12 }}>Sin resultados</div>
            </div>
          )}
          {groups.map((g) => (
            <div key={g.label} style={{ marginBottom: 18 }}>
              <div className="spread" style={{ padding: "0 4px 6px" }}>
                <span className="eyebrow">{g.label}</span>
                <span className="small tnum" style={{ fontWeight: 600, color: g.total >= 0 ? "var(--pos)" : "var(--muted)" }}>
                  {W.fmtMoney(g.total, "USD", { signed: true, dp: 0 })}
                </span>
              </div>
              <div className="card" style={{ padding: "2px 14px" }}>
                {g.items.map((t) => {
                  const c = W.catBy(t.cat);
                  const inc = W.typeOf(t.cat) === "ingreso";
                  return (
                    <div className="txn" key={t.id} onClick={() => onOpenTxn(t)}>
                      <CatIcon cat={c} size="sm" />
                      <div className="meta">
                        <div className="t-name">{t.desc}</div>
                        <div className="t-sub">{c.name}{t.source ? " · " + t.source : ""}</div>
                      </div>
                      <div className="t-amt tnum" style={{ color: inc ? "var(--pos)" : "var(--ink)" }}>
                        {W.fmtMoney(W.signedAmt(t), "USD", { signed: true })}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  /* ---------- Transaction detail / editor ---------- */
  function TxnEditor({ txn, cats, onClose, onSave, onDelete }) {
    const W = WM();
    const isNew = !txn;
    const init = txn || { cat: cats.find((c) => c.type === "gasto").name, amt: 0, desc: "", date: W.today };
    const [type, setType] = useState(W.typeOf(init.cat));
    const [amt, setAmt] = useState(init.amt ? (init.amt / 100).toString() : "");
    const [catName, setCatName] = useState(init.cat);
    const [desc, setDesc] = useState(init.desc);
    const [confirmDel, setConfirmDel] = useState(false);

    const visibleCats = cats.filter((c) => c.type === type);
    const c = W.catBy(catName) || visibleCats[0];
    const cents = Math.round((parseFloat(amt) || 0) * 100);
    const canSave = cents > 0 && catName;
    const dateStr = `${init.date.getDate()} ${W.MONTHS_LONG[init.date.getMonth()]} ${init.date.getFullYear()}`;

    return (
      <div className="fullover">
        <window.UI.StatusBar />
        <div className="topbar" style={{ background: "transparent" }}>
          <button className="iconbtn ghost" onClick={onClose} aria-label="Cerrar"><Icon name="close" /></button>
          <span className="title" style={{ fontSize: 18 }}>{isNew ? "Nuevo movimiento" : "Editar movimiento"}</span>
          {!isNew && (
            <button className="iconbtn ghost" onClick={() => setConfirmDel(true)} aria-label="Eliminar" style={{ color: "var(--neg)" }}>
              <Icon name="trash" />
            </button>
          )}
        </div>

        <div className="viewport">
          <div className="pad" style={{ paddingTop: 4 }}>
            {/* type segmented */}
            <div className="segmented" style={{ marginBottom: 20 }}>
              {[["gasto", "Gasto"], ["ingreso", "Ingreso"], ["transferencia", "Ahorro"]].map(([v, l]) => (
                <button key={v} className={type === v ? "on" : ""} onClick={() => { setType(v); if (W.typeOf(catName) !== v) setCatName(cats.find((x) => x.type === v).name); }}>{l}</button>
              ))}
            </div>

            {/* amount input big */}
            <div className="center" style={{ marginBottom: 24 }}>
              <div className="eyebrow" style={{ marginBottom: 8 }}>Importe</div>
              <div className="row" style={{ justifyContent: "center", gap: 2 }}>
                <span className="amount" style={{ fontSize: 28, color: "var(--muted)", alignSelf: "flex-start", marginTop: 6 }}>$</span>
                <input value={amt} onChange={(e) => setAmt(e.target.value.replace(/[^0-9.]/g, ""))}
                  inputMode="decimal" placeholder="0"
                  className="amount" style={{ fontSize: 46, width: 200, textAlign: "center", background: "transparent", border: "none", outline: "none", color: c.color }} />
              </div>
              <div style={{ height: 2, width: 120, background: "var(--border)", margin: "4px auto 0" }} />
            </div>

            <div className="stack">
              {/* category picker */}
              <div className="field">
                <label>Categoría</label>
                <div style={{ display: "flex", gap: 9, overflowX: "auto", scrollbarWidth: "none", margin: "0 -20px", padding: "2px 20px 6px" }}>
                  {visibleCats.map((cc) => (
                    <button key={cc.id} onClick={() => setCatName(cc.name)}
                      style={{
                        flex: "none", display: "flex", flexDirection: "column", alignItems: "center", gap: 6,
                        width: 66, padding: "9px 4px", borderRadius: 16,
                        border: "1.5px solid " + (catName === cc.name ? cc.color : "var(--border)"),
                        background: catName === cc.name ? cc.color + "1a" : "var(--surface)", transition: "all .14s",
                      }}>
                      <CatIcon cat={cc} size="sm" />
                      <span style={{ fontSize: 10.5, fontWeight: 600, color: "var(--ink-2)", whiteSpace: "nowrap", maxWidth: 58, overflow: "hidden", textOverflow: "ellipsis" }}>{cc.name}</span>
                    </button>
                  ))}
                </div>
              </div>

              {/* description */}
              <div className="field">
                <label>Descripción</label>
                <input className="input" placeholder="Ej. Compra semanal" value={desc} onChange={(e) => setDesc(e.target.value)} />
              </div>

              {/* date row */}
              <div className="field">
                <label>Fecha</label>
                <button className="input" style={{ display: "flex", alignItems: "center", justifyContent: "space-between", textAlign: "left" }}>
                  <span className="row" style={{ gap: 10 }}><Icon name="calendar" size={18} style={{ color: "var(--muted)" }} />{dateStr}</span>
                  <Icon name="chevron" size={18} style={{ color: "var(--faint)" }} />
                </button>
              </div>

              {/* recurring toggle (novel touch) */}
              <div className="card card-pad spread">
                <span className="row" style={{ gap: 12 }}>
                  <Icon name="repeat" size={20} style={{ color: "var(--muted)" }} />
                  <span><span className="h3" style={{ fontSize: 15 }}>Recurrente</span><div className="small">Repetir cada mes</div></span>
                </span>
                <Toggle />
              </div>
            </div>
          </div>
        </div>

        {/* save bar */}
        <div style={{ padding: "12px 20px max(20px, env(safe-area-inset-bottom))", borderTop: "1px solid var(--border)", background: "var(--bg)" }}>
          <button className="btn btn-primary btn-block" disabled={!canSave}
            onClick={() => onSave({ ...(txn || {}), amt: cents, cat: catName, desc: desc || c.name, date: init.date })}>
            {isNew ? "Añadir movimiento" : "Guardar cambios"}
          </button>
        </div>

        {confirmDel && (
          <div className="scrim dlg" onClick={() => setConfirmDel(false)}>
            <div className="dialog" onClick={(e) => e.stopPropagation()}>
              <div className="h2" style={{ marginBottom: 8 }}>¿Eliminar movimiento?</div>
              <div className="body" style={{ marginBottom: 20 }}>Esta acción no se puede deshacer.</div>
              <div style={{ display: "flex", gap: 10 }}>
                <button className="btn btn-ghost grow" onClick={() => setConfirmDel(false)}>Cancelar</button>
                <button className="btn grow" style={{ background: "var(--neg)", color: "#fff" }} onClick={() => onDelete(txn)}>Eliminar</button>
              </div>
            </div>
          </div>
        )}
      </div>
    );
  }

  function Toggle({ on: initOn = false, onChange }) {
    const [on, setOn] = useState(initOn);
    return (
      <button onClick={() => { setOn(!on); onChange && onChange(!on); }}
        style={{ width: 48, height: 28, borderRadius: 999, background: on ? "var(--brand)" : "var(--surface-3)", position: "relative", transition: "background .2s", flex: "none" }}>
        <span style={{ position: "absolute", top: 3, left: on ? 23 : 3, width: 22, height: 22, borderRadius: 999, background: "#fff", boxShadow: "0 1px 3px rgba(0,0,0,.3)", transition: "left .2s" }} />
      </button>
    );
  }

  window.Screens = window.Screens || {};
  window.Screens.Txns = Txns;
  window.Screens.TxnEditor = TxnEditor;
  window.Screens.Toggle = Toggle;
})();
