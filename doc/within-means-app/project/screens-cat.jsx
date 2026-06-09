/* screens-cat.jsx — categories list + category editor */
(function () {
  const { useState } = React;
  const { CatIcon } = window.UI;

  function Cats({ cats, txns, onEdit, onAdd }) {
    const W = window.WM;
    const [tab, setTab] = useState("gasto");
    const list = cats.filter((c) => c.type === tab);
    // spend per category this month
    const spend = {};
    txns.forEach((t) => { spend[t.cat] = (spend[t.cat] || 0) + t.amt; });

    return (
      <div className="viewport">
        <div className="topbar" style={{ flexDirection: "column", alignItems: "stretch", gap: 12 }}>
          <div className="spread">
            <span className="title">Categorías</span>
            <button className="iconbtn" onClick={onAdd} aria-label="Nueva categoría"><Icon name="plus" /></button>
          </div>
          <div className="segmented">
            {[["gasto", "Gastos"], ["ingreso", "Ingresos"], ["transferencia", "Ahorro"]].map(([v, l]) => (
              <button key={v} className={tab === v ? "on" : ""} onClick={() => setTab(v)}>{l}</button>
            ))}
          </div>
        </div>

        <div className="pad" style={{ paddingTop: 4 }}>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
            {list.map((c) => (
              <button key={c.id} className="card card-pad" onClick={() => onEdit(c)}
                style={{ textAlign: "left", padding: 16, display: "flex", flexDirection: "column", gap: 12 }}>
                <div className="spread">
                  <CatIcon cat={c} />
                  {c.nat && <span style={{ fontSize: 9.5, fontWeight: 700, letterSpacing: ".06em", color: "var(--muted)", border: "1px solid var(--border)", padding: "2px 7px", borderRadius: 999 }}>{c.nat}</span>}
                </div>
                <div>
                  <div className="h3" style={{ fontSize: 15 }}>{c.name}</div>
                  <div className="small tnum" style={{ marginTop: 2 }}>
                    {spend[c.name] ? W.fmtMoney(spend[c.name], "USD", { dp: 0 }) + " este mes" : "Sin movimientos"}
                  </div>
                </div>
              </button>
            ))}
            <button onClick={onAdd} className="card" style={{ display: "grid", placeItems: "center", minHeight: 118, border: "1.5px dashed var(--border-strong)", background: "transparent", color: "var(--muted)", gap: 6 }}>
              <Icon name="plus" size={22} />
              <span className="small" style={{ fontWeight: 600 }}>Nueva</span>
            </button>
          </div>
        </div>
      </div>
    );
  }

  function CatEditor({ cat, onClose, onSave, onDelete }) {
    const W = window.WM;
    const isNew = !cat;
    const [name, setName] = useState(cat?.name || "");
    const [type, setType] = useState(cat?.type || "gasto");
    const [color, setColor] = useState(cat?.color || W.PALETTE[0]);
    const [icon, setIcon] = useState(cat?.icon || W.ICONS[0]);
    const [nat, setNat] = useState(cat?.nat || "VARIABLE");
    const [ess, setEss] = useState(cat?.ess || "ESENCIAL");
    const [confirmDel, setConfirmDel] = useState(false);
    const canSave = name.trim().length > 0;

    const preview = { name: name || "Categoría", color, icon };

    return (
      <div className="fullover">
        <window.UI.StatusBar />
        <div className="topbar" style={{ background: "transparent" }}>
          <button className="iconbtn ghost" onClick={onClose} aria-label="Cerrar"><Icon name="close" /></button>
          <span className="title" style={{ fontSize: 18 }}>{isNew ? "Nueva categoría" : "Editar categoría"}</span>
          {!isNew && <button className="iconbtn ghost" onClick={() => setConfirmDel(true)} style={{ color: "var(--neg)" }} aria-label="Eliminar"><Icon name="trash" /></button>}
        </div>

        <div className="viewport">
          <div className="pad" style={{ paddingTop: 4 }}>
            {/* live preview */}
            <div className="center" style={{ marginBottom: 22 }}>
              <div className="catico" style={{ background: color, width: 72, height: 72, borderRadius: 22, margin: "0 auto" }}>
                <Icon name={icon} size={34} />
              </div>
              <div className="h2" style={{ marginTop: 12 }}>{preview.name}</div>
            </div>

            <div className="stack">
              <div className="field">
                <label>Nombre</label>
                <input className="input" placeholder="Ej. Mercado" value={name} onChange={(e) => setName(e.target.value)} autoFocus />
              </div>

              <div className="field">
                <label>Tipo</label>
                <div className="segmented">
                  {[["gasto", "Gasto"], ["ingreso", "Ingreso"], ["transferencia", "Ahorro"]].map(([v, l]) => (
                    <button key={v} className={type === v ? "on" : ""} onClick={() => setType(v)}>{l}</button>
                  ))}
                </div>
              </div>

              <div className="field">
                <label>Color</label>
                <div className="swatch-grid">
                  {W.PALETTE.map((p) => (
                    <button key={p} className={"swatch" + (color === p ? " on" : "")} style={{ background: p }} onClick={() => setColor(p)}>
                      {color === p && <Icon name="check" size={16} stroke={3} style={{ color: "#fff" }} />}
                    </button>
                  ))}
                </div>
              </div>

              <div className="field">
                <label>Icono</label>
                <div className="icon-grid">
                  {W.ICONS.map((ic) => (
                    <button key={ic} className={"icon-pick" + (icon === ic ? " on" : "")} onClick={() => setIcon(ic)}>
                      <Icon name={ic} />
                    </button>
                  ))}
                </div>
              </div>

              {type === "gasto" && (
                <>
                  <div className="field">
                    <label>Naturaleza</label>
                    <div className="segmented">
                      {[["FIJO", "Fijo"], ["VARIABLE", "Variable"]].map(([v, l]) => (
                        <button key={v} className={nat === v ? "on" : ""} onClick={() => setNat(v)}>{l}</button>
                      ))}
                    </div>
                  </div>
                  <div className="field">
                    <label>Prioridad</label>
                    <div className="segmented">
                      {[["ESENCIAL", "Esencial"], ["DISCRECIONAL", "Discrecional"]].map(([v, l]) => (
                        <button key={v} className={ess === v ? "on" : ""} onClick={() => setEss(v)}>{l}</button>
                      ))}
                    </div>
                  </div>
                </>
              )}
            </div>
          </div>
        </div>

        <div style={{ padding: "12px 20px max(20px, env(safe-area-inset-bottom))", borderTop: "1px solid var(--border)", background: "var(--bg)" }}>
          <button className="btn btn-primary btn-block" disabled={!canSave}
            onClick={() => onSave({ ...(cat || {}), name: name.trim(), type, color, icon, nat: type === "gasto" ? nat : undefined, ess: type === "gasto" ? ess : undefined })}>
            {isNew ? "Crear categoría" : "Guardar cambios"}
          </button>
        </div>

        {confirmDel && (
          <div className="scrim dlg" onClick={() => setConfirmDel(false)}>
            <div className="dialog" onClick={(e) => e.stopPropagation()}>
              <div className="h2" style={{ marginBottom: 8 }}>¿Eliminar categoría?</div>
              <div className="body" style={{ marginBottom: 20 }}>Los movimientos asociados no se borrarán.</div>
              <div style={{ display: "flex", gap: 10 }}>
                <button className="btn btn-ghost grow" onClick={() => setConfirmDel(false)}>Cancelar</button>
                <button className="btn grow" style={{ background: "var(--neg)", color: "#fff" }} onClick={() => onDelete(cat)}>Eliminar</button>
              </div>
            </div>
          </div>
        )}
      </div>
    );
  }

  window.Screens = window.Screens || {};
  window.Screens.Cats = Cats;
  window.Screens.CatEditor = CatEditor;
})();
