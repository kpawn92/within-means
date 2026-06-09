/* app.jsx — root: state, routing, theme, tweaks */
(function () {
  const { useState, useEffect, useCallback } = React;
  const S = window.Screens;
  const { BottomNav, Toast } = window.UI;

  function App() {
    const W = window.WM;
    const [theme, setTheme] = useState("light");
    const [tab, setTab] = useState("home");
    const [txns, setTxns] = useState(() => W.TXNS.map((t) => ({ ...t })));
    const [cats, setCats] = useState(() => W.CATEGORIES.map((c) => ({ ...c })));

    // overlays
    const [quickAdd, setQuickAdd] = useState(false);
    const [txnEdit, setTxnEdit] = useState(null); // {txn} or {new:true}
    const [catEdit, setCatEdit] = useState(null);
    const [overlay, setOverlay] = useState(null); // onboarding | unlock | null
    const [toast, setToast] = useState(null);

    // tweaks
    const [tweaks, setTweaks] = useState({
      accent: "#3F8F6B", font: "Hanken Grotesk", density: 1, radius: 24,
    });

    // apply theme
    useEffect(() => {
      document.documentElement.dataset.theme = theme;
    }, [theme]);

    // apply tweaks to :root
    useEffect(() => {
      const r = document.documentElement.style;
      const oklchFromHex = tweaks.accent;
      r.setProperty("--brand", tweaks.accent);
      r.setProperty("--font-sans", `"${tweaks.font}", system-ui, sans-serif`);
      r.setProperty("--d", tweaks.density);
      r.setProperty("--r-lg", tweaks.radius + "px");
      r.setProperty("--pos", tweaks.accent);
      r.setProperty("--brand-soft", tweaks.accent + "22");
    }, [tweaks]);

    const showToast = (msg, icon) => setToast({ msg, icon, k: Date.now() });

    /* ----- txn ops ----- */
    const saveTxn = (data) => {
      if (data.id) {
        setTxns((ts) => ts.map((t) => (t.id === data.id ? { ...t, ...data } : t)));
        showToast("Movimiento actualizado");
      } else {
        const date = data.when === "Ayer" ? new Date(W.today.getTime() - 86400000) : (data.date || W.today);
        const nt = { id: "t" + Date.now(), cat: data.cat, amt: data.amt, desc: data.desc, date };
        setTxns((ts) => [nt, ...ts]);
        showToast(`${W.fmtMoney(data.amt)} en ${data.cat}`, "check");
      }
      setTxnEdit(null);
      setQuickAdd(false);
    };
    const deleteTxn = (t) => {
      setTxns((ts) => ts.filter((x) => x.id !== t.id));
      setTxnEdit(null);
      showToast("Movimiento eliminado", "trash");
    };

    /* ----- cat ops ----- */
    const saveCat = (data) => {
      if (data.id) {
        setCats((cs) => cs.map((c) => (c.id === data.id ? { ...c, ...data } : c)));
        showToast("Categoría actualizada");
      } else {
        setCats((cs) => [...cs, { ...data, id: "c" + Date.now() }]);
        showToast("Categoría creada");
      }
      setCatEdit(null);
    };
    const deleteCat = (c) => {
      setCats((cs) => cs.filter((x) => x.id !== c.id));
      setCatEdit(null);
      showToast("Categoría eliminada", "trash");
    };

    const tabTitle = { home: "Inicio", txns: "Movimientos", stats: "Análisis", cats: "Categorías", settings: "Ajustes" };

    return (
      <div className="screen">
        <window.UI.StatusBar />

        {/* main tab content */}
        {tab === "home" && <S.Home txns={txns} cats={cats} budget={W.user.monthlyBudget}
          onOpenTxn={(t) => setTxnEdit({ txn: t })} onSeeAll={() => setTab("txns")} onAdd={() => setQuickAdd(true)} onSettings={() => setTab("settings")} />}
        {tab === "txns" && <S.Txns txns={txns} onOpenTxn={(t) => setTxnEdit({ txn: t })} onAdd={() => setQuickAdd(true)} />}
        {tab === "stats" && <S.Stats txns={txns} cats={cats} />}
        {tab === "cats" && <S.Cats cats={cats} txns={txns} onEdit={(c) => setCatEdit({ cat: c })} onAdd={() => setCatEdit({ new: true })} />}
        {tab === "settings" && <S.Settings theme={theme} onToggleTheme={() => setTheme((t) => (t === "dark" ? "light" : "dark"))}
          onLock={() => setOverlay("unlock")} onRestart={() => setOverlay("onboarding")} />}

        {/* settings entry: floating top-right on main tabs */}
        {["home", "txns", "stats", "cats"].includes(tab) && (
          <button className="iconbtn" onClick={() => setTab("settings")}
            style={{ position: "absolute", top: 54, right: 16, zIndex: 7, background: "transparent", border: "none", display: tab === "home" ? "none" : "grid" }}>
            <Icon name="settings" size={21} />
          </button>
        )}

        <BottomNav tab={tab} onTab={setTab} onAdd={() => setQuickAdd(true)} />

        {/* overlays */}
        {quickAdd && <S.QuickAdd cats={cats} onClose={() => setQuickAdd(false)} onSave={saveTxn} />}
        {txnEdit && <S.TxnEditor txn={txnEdit.txn} cats={cats} onClose={() => setTxnEdit(null)} onSave={saveTxn} onDelete={deleteTxn} />}
        {catEdit && <S.CatEditor cat={catEdit.cat} onClose={() => setCatEdit(null)} onSave={saveCat} onDelete={deleteCat} />}
        {overlay === "onboarding" && <S.Onboarding onDone={() => setOverlay("unlock")} />}
        {overlay === "unlock" && <S.Unlock onUnlock={() => setOverlay(null)} />}

        {toast && <Toast key={toast.k} msg={toast.msg} icon={toast.icon} onDone={() => setToast(null)} />}

        <Tweaks theme={theme} setTheme={setTheme} tweaks={tweaks} setTweaks={setTweaks} />
      </div>
    );
  }

  /* ---------- Tweaks panel ---------- */
  function Tweaks({ theme, setTheme, tweaks, setTweaks }) {
    const set = (k, v) => setTweaks((t) => ({ ...t, [k]: v }));
    return (
      <TweaksPanel title="Tweaks">
        <TweakSection label="Tema" />
        <TweakRadio label="Apariencia" value={theme} onChange={setTheme}
          options={[{ value: "light", label: "Claro" }, { value: "dark", label: "Oscuro" }]} />
        <TweakSection label="Marca" />
        <TweakColor label="Acento" value={tweaks.accent} onChange={(v) => set("accent", v)}
          options={["#3F8F6B", "#2F6F5B", "#3E7A8C", "#5B6BB4", "#A65D3C", "#7A6BB1"]} />
        <TweakSection label="Tipografía" />
        <TweakSelect label="Fuente" value={tweaks.font} onChange={(v) => set("font", v)}
          options={["Hanken Grotesk", "Inter Tight", "Bricolage Grotesque", "Sora", "Figtree"]} />
        <TweakSection label="Densidad y forma" />
        <TweakSlider label="Densidad" min={0.82} max={1.18} step={0.04} value={tweaks.density} unit="×" onChange={(v) => set("density", v)} />
        <TweakSlider label="Redondeo" min={8} max={32} step={2} value={tweaks.radius} unit="px" onChange={(v) => set("radius", v)} />
      </TweaksPanel>
    );
  }

  ReactDOM.createRoot(document.getElementById("root")).render(<App />);
})();
