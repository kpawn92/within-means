/* ui.jsx — shared presentational components */
(function () {
  const { useState, useEffect, useRef } = React;

  function StatusBar() {
    return (
      <div className="statusbar">
        <span>9:41</span>
        <div className="sb-icons">
          <svg width="18" height="12" viewBox="0 0 18 12" fill="currentColor"><rect x="0" y="6" width="3" height="6" rx="1" /><rect x="5" y="3.5" width="3" height="8.5" rx="1" /><rect x="10" y="1" width="3" height="11" rx="1" opacity="0.9" /><rect x="15" y="0" width="3" height="12" rx="1" opacity="0.35" /></svg>
          <svg width="17" height="12" viewBox="0 0 17 12" fill="none" stroke="currentColor" strokeWidth="1.4"><path d="M1 4.5C4 1.5 13 1.5 16 4.5M3.2 7C5 5.2 12 5.2 13.8 7M5.5 9.5c1.5-1.3 4.5-1.3 6 0" strokeLinecap="round" /></svg>
          <svg width="26" height="13" viewBox="0 0 26 13" fill="none"><rect x="0.5" y="0.5" width="22" height="12" rx="3.5" stroke="currentColor" opacity="0.4" /><rect x="2" y="2" width="17" height="9" rx="2" fill="currentColor" /><rect x="24" y="4" width="2" height="5" rx="1" fill="currentColor" opacity="0.5" /></svg>
        </div>
      </div>
    );
  }

  function CatIcon({ cat, size = "", style }) {
    if (!cat) return null;
    return (
      <div className={"catico " + size} style={{ background: cat.color, ...style }}>
        <Icon name={cat.icon} />
      </div>
    );
  }

  // SVG donut chart. data = [{value, color}], renders ring with gap
  function Donut({ data, size = 150, thickness = 20, children, gap = 0.012 }) {
    const total = data.reduce((s, d) => s + d.value, 0) || 1;
    const r = (size - thickness) / 2;
    const c = 2 * Math.PI * r;
    let offset = 0;
    return (
      <div style={{ position: "relative", width: size, height: size }}>
        <svg width={size} height={size} className="donut">
          {data.map((d, i) => {
            const frac = d.value / total;
            const len = Math.max(0, frac - gap) * c;
            const seg = (
              <circle key={i} cx={size / 2} cy={size / 2} r={r} fill="none"
                stroke={d.color} strokeWidth={thickness} strokeLinecap="round"
                strokeDasharray={`${len} ${c - len}`} strokeDashoffset={-offset * c}
                style={{ transition: "stroke-dasharray .6s ease, stroke-dashoffset .6s ease" }} />
            );
            offset += frac;
            return seg;
          })}
        </svg>
        {children && (
          <div style={{ position: "absolute", inset: 0, display: "grid", placeItems: "center", textAlign: "center" }}>
            {children}
          </div>
        )}
      </div>
    );
  }

  function BottomNav({ tab, onTab, onAdd }) {
    const items = [
      { id: "home", icon: "home", label: "Inicio" },
      { id: "txns", icon: "list", label: "Movimientos" },
      { id: null, icon: "plus", label: "" },
      { id: "stats", icon: "stats", label: "Análisis" },
      { id: "cats", icon: "grid", label: "Categorías" },
    ];
    return (
      <nav className="bottomnav">
        {items.map((it, i) =>
          it.id === null ? (
            <button key="add" className="navadd" onClick={onAdd} aria-label="Añadir">
              <Icon name="plus" stroke={2.4} />
            </button>
          ) : (
            <button key={it.id} className={"navitem" + (tab === it.id ? " on" : "")} onClick={() => onTab(it.id)}>
              <Icon name={it.icon} stroke={tab === it.id ? 2.3 : 2} />
              <span>{it.label}</span>
            </button>
          )
        )}
      </nav>
    );
  }

  function TopBar({ title, onBack, right, large }) {
    return (
      <div className="topbar">
        {onBack && (
          <button className="iconbtn ghost" onClick={onBack} aria-label="Atrás">
            <Icon name="back" />
          </button>
        )}
        <span className="title" style={large ? { fontSize: 25 } : null}>{title}</span>
        {right}
      </div>
    );
  }

  // animated number count-up
  function useCountUp(target, dur = 700) {
    const [v, setV] = useState(target);
    const prev = useRef(target);
    useEffect(() => {
      const from = prev.current, to = target, start = performance.now();
      let raf;
      const tick = (now) => {
        const t = Math.min(1, (now - start) / dur);
        const e = 1 - Math.pow(1 - t, 3);
        setV(from + (to - from) * e);
        if (t < 1) raf = requestAnimationFrame(tick);
        else prev.current = to;
      };
      raf = requestAnimationFrame(tick);
      return () => cancelAnimationFrame(raf);
    }, [target]);
    return v;
  }

  function Toast({ msg, icon = "check", onDone }) {
    useEffect(() => {
      const t = setTimeout(onDone, 2200);
      return () => clearTimeout(t);
    }, []);
    return (
      <div className="toast">
        <Icon name={icon} size={18} stroke={2.4} style={{ color: "var(--brand)" }} />
        {msg}
      </div>
    );
  }

  window.UI = { StatusBar, CatIcon, Donut, BottomNav, TopBar, useCountUp, Toast };
})();
