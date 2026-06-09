/* icons.jsx — line icon set. <Icon name="..." /> renders 24x24 stroke icons. */
(function () {
  const P = {
    // nav
    home: <><path d="M3 10.5 12 4l9 6.5" /><path d="M5 9.5V20h14V9.5" /></>,
    list: <><path d="M8 6h12M8 12h12M8 18h12" /><circle cx="4" cy="6" r="1" /><circle cx="4" cy="12" r="1" /><circle cx="4" cy="18" r="1" /></>,
    stats: <><path d="M4 20V10M10 20V4M16 20v-6M22 20H2" /></>,
    grid: <><rect x="4" y="4" width="6.5" height="6.5" rx="2" /><rect x="13.5" y="4" width="6.5" height="6.5" rx="2" /><rect x="4" y="13.5" width="6.5" height="6.5" rx="2" /><rect x="13.5" y="13.5" width="6.5" height="6.5" rx="2" /></>,
    settings: <><circle cx="12" cy="12" r="3" /><path d="M12 2v3M12 19v3M2 12h3M19 12h3M5 5l2 2M17 17l2 2M19 5l-2 2M7 17l-2 2" /></>,
    plus: <><path d="M12 5v14M5 12h14" /></>,
    // controls
    close: <><path d="M6 6l12 12M18 6 6 18" /></>,
    back: <><path d="M15 5l-7 7 7 7" /></>,
    chevron: <><path d="M9 6l6 6-6 6" /></>,
    down: <><path d="M6 9l6 6 6-6" /></>,
    check: <><path d="M5 12.5l4.5 4.5L19 7" /></>,
    trash: <><path d="M4 7h16M9 7V5h6v2M6 7l1 13h10l1-13" /></>,
    calendar: <><rect x="4" y="5" width="16" height="16" rx="3" /><path d="M4 9h16M9 3v4M15 3v4" /></>,
    lock: <><rect x="5" y="10" width="14" height="11" rx="3" /><path d="M8 10V7a4 4 0 0 1 8 0v3" /></>,
    arrowup: <><path d="M12 19V5M6 11l6-6 6 6" /></>,
    arrowdown: <><path d="M12 5v14M6 13l6 6 6-6" /></>,
    eye: <><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" /><circle cx="12" cy="12" r="2.5" /></>,
    bell: <><path d="M6 9a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6" /><path d="M10 20a2 2 0 0 0 4 0" /></>,
    sun: <><circle cx="12" cy="12" r="4" /><path d="M12 2v2M12 20v2M4 12H2M22 12h-2M5 5l1.5 1.5M17.5 17.5 19 19M19 5l-1.5 1.5M6.5 17.5 5 19" /></>,
    moon: <><path d="M20 14a8 8 0 1 1-9-11 6 6 0 0 0 9 11Z" /></>,
    globe: <><circle cx="12" cy="12" r="9" /><path d="M3 12h18M12 3c2.5 2.5 2.5 15 0 18M12 3c-2.5 2.5-2.5 15 0 18" /></>,
    wallet: <><rect x="3" y="6" width="18" height="14" rx="3" /><path d="M3 10h18M16 14h2" /></>,
    backspace: <><path d="M9 5h11a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H9l-6-7 6-7Z" /><path d="M13 9l4 6M17 9l-4 6" /></>,
    flag: <><path d="M5 21V4M5 4h11l-2 4 2 4H5" /></>,
    repeat: <><path d="M4 9a5 5 0 0 1 5-5h9M20 9l-2-2 2-2M20 15a5 5 0 0 1-5 5H6M4 15l2 2-2 2" /></>,
    spark: <><path d="M12 3v4M12 17v4M3 12h4M17 12h4M6 6l2.5 2.5M15.5 15.5 18 18M18 6l-2.5 2.5M8.5 15.5 6 18" /></>,
    // category icons (13)
    food: <><path d="M5 3v8a3 3 0 0 0 6 0V3M8 3v18M16 3c-1.5 1-2 3-2 5s.5 3 2 3v10" /></>,
    house: <><path d="M4 11 12 5l8 6M6 10v9h12v-9" /><path d="M10 19v-5h4v5" /></>,
    car: <><path d="M4 16v-4l2-5h12l2 5v4M4 16h16M4 16v2M20 16v2" /><circle cx="8" cy="16" r="1.4" /><circle cx="16" cy="16" r="1.4" /></>,
    health: <><path d="M12 21s-8-4.5-8-10a4.5 4.5 0 0 1 8-3 4.5 4.5 0 0 1 8 3c0 5.5-8 10-8 10Z" /></>,
    book: <><path d="M4 5a2 2 0 0 1 2-2h6v17H6a2 2 0 0 0-2 2V5Z" /><path d="M20 5a2 2 0 0 0-2-2h-6v17h6a2 2 0 0 1 2 2V5Z" /></>,
    leisure: <><circle cx="12" cy="12" r="9" /><path d="M9 9l6 6M15 9l-6 6" /></>,
    shop: <><path d="M5 8h14l-1 12H6L5 8Z" /><path d="M9 8V6a3 3 0 0 1 6 0v2" /></>,
    coffee: <><path d="M4 9h13v5a4 4 0 0 1-4 4H8a4 4 0 0 1-4-4V9Z" /><path d="M17 10h2a2 2 0 0 1 0 4h-2M7 4v2M11 4v2" /></>,
    gift: <><rect x="4" y="9" width="16" height="11" rx="1.5" /><path d="M4 13h16M12 9v11M12 9c-1-3-5-3-5-1s4 1 5 1Zm0 0c1-3 5-3 5-1s-4 1-5 1Z" /></>,
    salary: <><circle cx="12" cy="12" r="8" /><path d="M12 8v8M9.5 10h3.5a1.5 1.5 0 0 1 0 3H10a1.5 1.5 0 0 0 0 3h3.5" /></>,
    phone: <><rect x="7" y="3" width="10" height="18" rx="2.5" /><path d="M11 18h2" /></>,
    pet: <><circle cx="8" cy="9" r="1.6" /><circle cx="16" cy="9" r="1.6" /><circle cx="5.5" cy="13" r="1.5" /><circle cx="18.5" cy="13" r="1.5" /><path d="M12 13c-2.5 0-4 2-4 4a3 3 0 0 0 8 0c0-2-1.5-4-4-4Z" /></>,
    plane: <><path d="M10 3 21 12 10 21l-1-7-6-2 6-2 1-7Z" /></>,
  };

  function Icon({ name, size, stroke = 2, fill = false, style }) {
    const node = P[name] || P.spark;
    return (
      <svg viewBox="0 0 24 24" width={size || 24} height={size || 24}
        fill={fill ? "currentColor" : "none"} stroke="currentColor"
        strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round" style={style}>
        {node}
      </svg>
    );
  }
  Icon.names = Object.keys(P);
  window.Icon = Icon;
})();
