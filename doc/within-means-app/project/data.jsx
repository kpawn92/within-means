/* data.jsx — mock domain data + helpers (assigned to window) */
(function () {
  const PALETTE = [
    "#3F8F6B", "#C8783C", "#C0504D", "#5B7FB4", "#8A6BB1",
    "#D4A12E", "#4FA3A3", "#B5556E", "#6E8B3D", "#A36A4F",
    "#717A8C", "#C77FA6", "#3E7A8C",
  ];
  const ICONS = ["food", "house", "car", "health", "book", "leisure", "shop",
    "coffee", "gift", "salary", "phone", "pet", "plane"];

  let cid = 0;
  const cat = (o) => ({ id: "c" + (++cid), ...o });
  const CATEGORIES = [
    cat({ name: "Mercado", type: "gasto", color: "#3F8F6B", icon: "food", nat: "VARIABLE", ess: "ESENCIAL" }),
    cat({ name: "Alquiler", type: "gasto", color: "#5B7FB4", icon: "house", nat: "FIJO", ess: "ESENCIAL" }),
    cat({ name: "Transporte", type: "gasto", color: "#C8783C", icon: "car", nat: "VARIABLE", ess: "ESENCIAL" }),
    cat({ name: "Salud", type: "gasto", color: "#C0504D", icon: "health", nat: "VARIABLE", ess: "ESENCIAL" }),
    cat({ name: "Formación", type: "gasto", color: "#8A6BB1", icon: "book", nat: "VARIABLE", ess: "DISCRECIONAL" }),
    cat({ name: "Ocio", type: "gasto", color: "#D4A12E", icon: "leisure", nat: "VARIABLE", ess: "DISCRECIONAL" }),
    cat({ name: "Compras", type: "gasto", color: "#C77FA6", icon: "shop", nat: "VARIABLE", ess: "DISCRECIONAL" }),
    cat({ name: "Café & bar", type: "gasto", color: "#A36A4F", icon: "coffee", nat: "VARIABLE", ess: "DISCRECIONAL" }),
    cat({ name: "Servicios", type: "gasto", color: "#717A8C", icon: "phone", nat: "FIJO", ess: "ESENCIAL" }),
    cat({ name: "Nómina", type: "ingreso", color: "#3F8F6B", icon: "salary" }),
    cat({ name: "Freelance", type: "ingreso", color: "#4FA3A3", icon: "spark" }),
    cat({ name: "Regalos", type: "ingreso", color: "#C77FA6", icon: "gift" }),
    cat({ name: "Ahorro", type: "transferencia", color: "#6E8B3D", icon: "flag" }),
  ];

  const today = new Date(2026, 5, 9);
  const day = (d) => { const x = new Date(today); x.setDate(x.getDate() - d); return x; };

  let tid = 0;
  const tx = (o) => ({ id: "t" + (++tid), ...o });
  const TXNS = [
    tx({ cat: "Mercado", amt: 4280, date: day(0), desc: "Compra semanal" }),
    tx({ cat: "Café & bar", amt: 650, date: day(0), desc: "Café con Ana" }),
    tx({ cat: "Transporte", amt: 1200, date: day(1), desc: "Recarga metro" }),
    tx({ cat: "Nómina", amt: 285000, date: day(1), desc: "Salario junio", source: "Acme S.A." }),
    tx({ cat: "Ocio", amt: 2400, date: day(2), desc: "Cine" }),
    tx({ cat: "Servicios", amt: 5500, date: day(3), desc: "Internet + móvil" }),
    tx({ cat: "Mercado", amt: 1875, date: day(4), desc: "Frutería" }),
    tx({ cat: "Salud", amt: 3200, date: day(5), desc: "Farmacia" }),
    tx({ cat: "Freelance", amt: 60000, date: day(6), desc: "Proyecto web", source: "Cliente B" }),
    tx({ cat: "Alquiler", amt: 95000, date: day(7), desc: "Renta junio" }),
    tx({ cat: "Compras", amt: 4990, date: day(8), desc: "Zapatillas" }),
    tx({ cat: "Café & bar", amt: 480, date: day(9), desc: "Desayuno" }),
    tx({ cat: "Transporte", amt: 2600, date: day(10), desc: "Gasolina" }),
    tx({ cat: "Formación", amt: 1500, date: day(12), desc: "Curso online" }),
    tx({ cat: "Ocio", amt: 3600, date: day(14), desc: "Concierto" }),
    tx({ cat: "Mercado", amt: 3950, date: day(16), desc: "Compra semanal" }),
    tx({ cat: "Regalos", amt: 5000, date: day(18), desc: "Reembolso" }),
    tx({ cat: "Salud", amt: 8000, date: day(20), desc: "Dentista" }),
  ];

  const CUR = { USD: { sym: "$" }, EUR: { sym: "€" }, GBP: { sym: "£" } };

  function fmtMoney(cents, cur = "USD", opts = {}) {
    const neg = cents < 0;
    const v = Math.abs(cents) / 100;
    const dp = opts.dp === 0 ? 0 : 2;
    const s = v.toLocaleString("es-ES", { minimumFractionDigits: dp, maximumFractionDigits: dp });
    const sym = (CUR[cur] || CUR.USD).sym;
    const sign = opts.signed ? (neg ? "−" : "+") : (neg ? "−" : "");
    return `${sign}${sym}${s}`;
  }

  const MONTHS = ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"];
  const MONTHS_LONG = ["enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"];
  const DAYS = ["dom", "lun", "mar", "mié", "jue", "vie", "sáb"];

  function relLabel(date) {
    const t = new Date(2026, 5, 9);
    const diff = Math.round((t - date) / 86400000);
    if (diff === 0) return "Hoy";
    if (diff === 1) return "Ayer";
    if (diff < 7) return DAYS[date.getDay()] + " " + date.getDate();
    return date.getDate() + " " + MONTHS[date.getMonth()];
  }

  function typeOf(catName) {
    const c = CATEGORIES.find((c) => c.name === catName);
    return c ? c.type : "gasto";
  }
  function signedAmt(t) {
    const ty = typeOf(t.cat);
    return ty === "ingreso" ? t.amt : -t.amt;
  }

  window.WM = {
    PALETTE, ICONS, CATEGORIES, TXNS, CUR,
    fmtMoney, MONTHS, MONTHS_LONG, DAYS, relLabel, typeOf, signedAmt,
    catBy: (name) => CATEGORIES.find((c) => c.name === name),
    today: new Date(2026, 5, 9),
    user: { name: "Lucía", initials: "L", currency: "USD", monthlyBudget: 180000 },
  };
})();
