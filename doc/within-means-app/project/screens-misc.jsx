/* screens-misc.jsx — Settings, Onboarding, Unlock (PIN lock) */
(function () {
  const { useState, useEffect } = React;
  const { Toggle } = window.Screens;

  /* ---------- Settings ---------- */
  function Settings({ theme, onToggleTheme, onLock, onRestart }) {
    const W = window.WM;
    const Section = ({ title, children }) => (
      <div style={{ marginBottom: 22 }}>
        <div className="eyebrow" style={{ padding: "0 4px 8px" }}>{title}</div>
        <div className="card" style={{ padding: "0 16px" }}>{children}</div>
      </div>
    );
    const Item = ({ icon, label, sub, right, onClick, danger, first }) => (
      <button onClick={onClick} style={{ width: "100%", display: "flex", alignItems: "center", gap: 13, padding: "13px 0", borderTop: first ? "none" : "1px solid var(--border)", textAlign: "left" }}>
        <span style={{ width: 36, height: 36, borderRadius: 11, background: danger ? "var(--neg-soft)" : "var(--surface-2)", color: danger ? "var(--neg)" : "var(--ink-2)", display: "grid", placeItems: "center", flex: "none" }}>
          <Icon name={icon} size={19} />
        </span>
        <span className="grow">
          <span className="h3" style={{ fontSize: 15, color: danger ? "var(--neg)" : "var(--ink)", display: "block" }}>{label}</span>
          {sub && <span className="small" style={{ display: "block" }}>{sub}</span>}
        </span>
        {right || <Icon name="chevron" size={18} style={{ color: "var(--faint)" }} />}
      </button>
    );

    return (
      <div className="viewport">
        <div className="topbar"><span className="title">Ajustes</span></div>
        <div className="pad" style={{ paddingTop: 4 }}>
          <div className="card card-pad row enter" style={{ gap: 14, marginBottom: 22 }}>
            <div style={{ width: 56, height: 56, borderRadius: 999, background: "var(--brand)", color: "var(--on-brand)", display: "grid", placeItems: "center", fontWeight: 700, fontSize: 24, flex: "none" }}>{W.user.initials}</div>
            <div className="grow">
              <div className="h2" style={{ fontSize: 18 }}>{W.user.name} Herrera</div>
              <div className="small">lucia@correo.com</div>
            </div>
            <Icon name="chevron" style={{ color: "var(--faint)" }} />
          </div>

          <Section title="Preferencias">
            <Item first icon={theme === "dark" ? "moon" : "sun"} label="Apariencia" sub={theme === "dark" ? "Oscuro" : "Claro"}
              right={<Toggle on={theme === "dark"} onChange={onToggleTheme} />} onClick={onToggleTheme} />
            <Item icon="wallet" label="Moneda" sub="Dólar (USD · $)" />
            <Item icon="globe" label="Idioma" sub="Español" />
            <Item icon="calendar" label="Inicio del mes" sub="Día 1" />
          </Section>

          <Section title="Presupuesto">
            <Item first icon="spark" label="Plan mensual" sub={W.fmtMoney(W.user.monthlyBudget, "USD", { dp: 0 })} />
            <Item icon="bell" label="Alertas de gasto" sub="Avisar al 80% del plan" right={<Toggle on={true} />} />
            <Item icon="repeat" label="Movimientos recurrentes" sub="3 activos" />
          </Section>

          <Section title="Seguridad">
            <Item first icon="lock" label="Bloqueo con PIN" sub="Activado" right={<Toggle on={true} />} />
            <Item icon="eye" label="Ocultar importes al abrir" right={<Toggle />} />
          </Section>

          <button className="btn btn-ghost btn-block" style={{ marginBottom: 12 }} onClick={onLock}>
            <Icon name="lock" size={18} /> Bloquear ahora
          </button>
          <button className="btn btn-ghost btn-block" style={{ color: "var(--neg)" }} onClick={onRestart}>Ver introducción</button>
          <div className="center small" style={{ marginTop: 18, color: "var(--faint)" }}>Within Means · v2.0</div>
        </div>
      </div>
    );
  }

  /* ---------- Onboarding ---------- */
  function Onboarding({ onDone }) {
    const [step, setStep] = useState(0);
    const slides = [
      { icon: "wallet", title: "Vive dentro de\ntus posibilidades", body: "Within Means te acompaña a gastar con calma — sin culpa y sin hojas de cálculo.", art: "balance" },
      { icon: "spark", title: "Registra en\nsegundos", body: "Un toque, el importe, la categoría. El gesto más rápido que vas a encontrar.", art: "add" },
      { icon: "stats", title: "Entiende a\ndónde va", body: "Descubre en qué se va tu dinero y cuánto puedes gastar hoy sin pasarte.", art: "stats" },
    ];
    const s = slides[step];
    const last = step === slides.length - 1;

    const Art = () => {
      if (s.art === "balance") return (
        <div className="card card-pad" style={{ background: "linear-gradient(160deg, var(--brand), var(--brand-strong))", border: "none", color: "var(--on-brand)", borderRadius: 26, width: 250, transform: "rotate(-3deg)", boxShadow: "var(--shadow-lg)" }}>
          <div className="small" style={{ opacity: .85, color: "var(--on-brand)" }}>Disponible este mes</div>
          <div className="amount" style={{ fontSize: 36, marginTop: 4 }}>$1,240.50</div>
          <div className="bar-track" style={{ background: "rgba(255,255,255,.25)", marginTop: 16 }}><div className="bar-fill" style={{ width: "62%", background: "#fff" }} /></div>
          <div className="small" style={{ color: "var(--on-brand)", opacity: .85, marginTop: 8 }}>Vas dentro del plan ✓</div>
        </div>
      );
      if (s.art === "add") return (
        <div className="card card-pad" style={{ width: 230, transform: "rotate(2deg)", boxShadow: "var(--shadow-lg)" }}>
          <div className="center">
            <div className="eyebrow">Gastas</div>
            <div className="amount" style={{ fontSize: 40, color: "var(--neg)", margin: "4px 0 12px" }}>$42.80</div>
          </div>
          <div className="keypad" style={{ gap: 6 }}>
            {["1", "2", "3", "4", "5", "6"].map((k) => <div key={k} className="key" style={{ height: 40, fontSize: 18, background: "var(--surface-2)" }}>{k}</div>)}
          </div>
        </div>
      );
      return (
        <div className="card card-pad" style={{ width: 240, transform: "rotate(-2deg)", boxShadow: "var(--shadow-lg)" }}>
          <div className="h3" style={{ marginBottom: 14 }}>Evolución</div>
          <div style={{ display: "flex", alignItems: "flex-end", gap: 8, height: 90 }}>
            {[40, 65, 50, 80, 45, 70].map((h, i) => (
              <div key={i} style={{ flex: 1, height: h + "%", borderRadius: 6, background: i === 3 ? "var(--brand)" : "var(--surface-3)" }} />
            ))}
          </div>
        </div>
      );
    };

    return (
      <div className="fullover" style={{ background: "var(--bg)" }}>
        <window.UI.StatusBar />
        <div className="spread" style={{ padding: "8px 20px" }}>
          <div className="eyebrow" style={{ fontSize: 13, letterSpacing: ".02em", textTransform: "none", color: "var(--brand-strong)", fontWeight: 700 }}>Within Means</div>
          {!last && <button className="small" style={{ fontWeight: 600 }} onClick={onDone}>Saltar</button>}
        </div>

        <div className="grow" style={{ display: "flex", flexDirection: "column", justifyContent: "center", padding: "0 32px" }}>
          <div style={{ display: "grid", placeItems: "center", height: 260, marginBottom: 12 }} key={step}>
            <div className="enter"><Art /></div>
          </div>
          <h1 className="h1" style={{ fontSize: 30, whiteSpace: "pre-line", marginBottom: 12 }} key={"t" + step}>{s.title}</h1>
          <p className="body" style={{ fontSize: 16, maxWidth: 320 }}>{s.body}</p>
        </div>

        <div style={{ padding: "0 24px max(28px, env(safe-area-inset-bottom))" }}>
          <div className="hstack" style={{ justifyContent: "center", gap: 7, marginBottom: 22 }}>
            {slides.map((_, i) => (
              <span key={i} style={{ height: 7, borderRadius: 999, width: i === step ? 24 : 7, background: i === step ? "var(--brand)" : "var(--border-strong)", transition: "all .25s" }} />
            ))}
          </div>
          <button className="btn btn-primary btn-block" onClick={() => last ? onDone() : setStep(step + 1)}>
            {last ? "Empezar" : "Siguiente"}
          </button>
        </div>
      </div>
    );
  }

  /* ---------- Unlock (PIN) ---------- */
  function Unlock({ onUnlock }) {
    const [pin, setPin] = useState("");
    const [err, setErr] = useState(false);
    const CODE = "1234";

    useEffect(() => {
      if (pin.length === 4) {
        if (pin === CODE) setTimeout(onUnlock, 180);
        else { setErr(true); setTimeout(() => { setErr(false); setPin(""); }, 500); }
      }
    }, [pin]);

    const press = (k) => {
      if (k === "del") return setPin((p) => p.slice(0, -1));
      if (pin.length < 4) setPin((p) => p + k);
    };

    return (
      <div className="fullover" style={{ background: "var(--bg)" }}>
        <window.UI.StatusBar />
        <div className="grow" style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", padding: "0 32px" }}>
          <div style={{ width: 64, height: 64, borderRadius: 20, background: "var(--brand-soft)", color: "var(--brand-strong)", display: "grid", placeItems: "center", marginBottom: 22 }}>
            <Icon name="lock" size={30} />
          </div>
          <div className="h2">Hola de nuevo, Lucía</div>
          <div className="small" style={{ marginTop: 4 }}>Introduce tu PIN para continuar</div>

          <div className={"hstack"} style={{ gap: 16, margin: "34px 0 8px", animation: err ? "shake .4s" : "none" }}>
            {[0, 1, 2, 3].map((i) => (
              <span key={i} style={{ width: 15, height: 15, borderRadius: 999, background: i < pin.length ? (err ? "var(--neg)" : "var(--brand)") : "transparent", border: "2px solid " + (err ? "var(--neg)" : i < pin.length ? "var(--brand)" : "var(--border-strong)"), transition: "all .15s" }} />
            ))}
          </div>
          <div className="small" style={{ color: "var(--neg)", height: 18, marginTop: 14 }}>{err ? "PIN incorrecto" : ""}</div>
        </div>

        <div style={{ padding: "0 40px max(28px, env(safe-area-inset-bottom))" }}>
          <div className="keypad" style={{ gap: 14 }}>
            {["1", "2", "3", "4", "5", "6", "7", "8", "9"].map((k) => (
              <button key={k} className="key" style={{ height: 64, fontSize: 26, borderRadius: 999 }} onClick={() => press(k)}>{k}</button>
            ))}
            <div />
            <button className="key" style={{ height: 64, fontSize: 26, borderRadius: 999 }} onClick={() => press("0")}>0</button>
            <button className="key" style={{ height: 64, borderRadius: 999 }} onClick={() => press("del")}><Icon name="backspace" size={24} /></button>
          </div>
          <div className="center small" style={{ marginTop: 18, color: "var(--faint)" }}>PIN de demo: 1234</div>
        </div>
      </div>
    );
  }

  window.Screens = window.Screens || {};
  window.Screens.Settings = Settings;
  window.Screens.Onboarding = Onboarding;
  window.Screens.Unlock = Unlock;
})();
