/* Currency — the web build.
 *
 * One screen of state, three views, no framework. Everything that decides a number lives in this
 * file's top third: the rate table, the direction of conversion, and the expression parser. The
 * views below only read those. That is deliberate — the Android build had a bug where a second
 * screen multiplied by a table that is meant to be divided, and the only cure is one implementation.
 */
(() => {
  "use strict";

  /* ── catalogue ───────────────────────────────────────────────────────────── */
  const CAT = {
    USD:["United States Dollar","$"],   EUR:["Euro","€"],              GBP:["British Pound","£"],
    LBP:["Lebanese Pound","L£"],        SYP:["Syrian Pound","S£"],     EGP:["Egyptian Pound","E£"],
    TRY:["Turkish Lira","₺"],           SAR:["Saudi Riyal","SAR"],      AED:["UAE Dirham","AED"],
    QAR:["Qatari Riyal","QAR"],         KWD:["Kuwaiti Dinar","KWD"],    BHD:["Bahraini Dinar","BHD"],
    OMR:["Omani Rial","OMR"],           JOD:["Jordanian Dinar","JOD"],  ILS:["Israeli Shekel","₪"],
    IQD:["Iraqi Dinar","IQD"],          IRR:["Iranian Rial","IRR"],     MAD:["Moroccan Dirham","MAD"],
    DZD:["Algerian Dinar","DZD"],       TND:["Tunisian Dinar","TND"],   LYD:["Libyan Dinar","LYD"],
    SDG:["Sudanese Pound","SDG"],       YER:["Yemeni Rial","YER"],      INR:["Indian Rupee","₹"],
    PKR:["Pakistani Rupee","PKR"],      BDT:["Bangladeshi Taka","BDT"], LKR:["Sri Lankan Rupee","LKR"],
    CNY:["Chinese Yuan","¥"],           JPY:["Japanese Yen","¥"],       KRW:["South Korean Won","₩"],
    HKD:["Hong Kong Dollar","HK$"],     SGD:["Singapore Dollar","S$"],  MYR:["Malaysian Ringgit","RM"],
    THB:["Thai Baht","฿"],              IDR:["Indonesian Rupiah","IDR"],PHP:["Philippine Peso","₱"],
    VND:["Vietnamese Dong","₫"],        AUD:["Australian Dollar","A$"], NZD:["New Zealand Dollar","NZ$"],
    CAD:["Canadian Dollar","C$"],       BRL:["Brazilian Real","R$"],    MXN:["Mexican Peso","MX$"],
    ARS:["Argentine Peso","AR$"],       CLP:["Chilean Peso","CLP"],     COP:["Colombian Peso","COP"],
    PEN:["Peruvian Sol","S/"],          ZAR:["South African Rand","R"], NGN:["Nigerian Naira","₦"],
    KES:["Kenyan Shilling","KSh"],      GHS:["Ghanaian Cedi","GH₵"],    RUB:["Russian Ruble","₽"],
    UAH:["Ukrainian Hryvnia","₴"],      PLN:["Polish Zloty","zł"],      CZK:["Czech Koruna","Kč"],
    HUF:["Hungarian Forint","Ft"],      RON:["Romanian Leu","lei"],     SEK:["Swedish Krona","kr"],
    NOK:["Norwegian Krone","kr"],       DKK:["Danish Krone","kr"],      CHF:["Swiss Franc","CHF"],
    ISK:["Icelandic Krona","kr"],       BTC:["Bitcoin","₿"],            ETH:["Ethereum","Ξ"],
    XAU:["Gold · troy ounce",null],     XAG:["Silver · troy ounce",null],
  };

  const FLAG = { USD:"US", EUR:"EU", GBP:"GB", LBP:"LB", SYP:"SY", EGP:"EG", TRY:"TR", SAR:"SA",
    AED:"AE", QAR:"QA", KWD:"KW", BHD:"BH", OMR:"OM", JOD:"JO", ILS:"IL", IQD:"IQ", IRR:"IR",
    MAD:"MA", DZD:"DZ", TND:"TN", LYD:"LY", SDG:"SD", YER:"YE", INR:"IN", PKR:"PK", BDT:"BD",
    LKR:"LK", CNY:"CN", JPY:"JP", KRW:"KR", HKD:"HK", SGD:"SG", MYR:"MY", THB:"TH", IDR:"ID",
    PHP:"PH", VND:"VN", AUD:"AU", NZD:"NZ", CAD:"CA", BRL:"BR", MXN:"MX", ARS:"AR", CLP:"CL",
    COP:"CO", PEN:"PE", ZAR:"ZA", NGN:"NG", KES:"KE", GHS:"GH", RUB:"RU", UAH:"UA", PLN:"PL",
    CZK:"CZ", HUF:"HU", RON:"RO", SEK:"SE", NOK:"NO", DKK:"DK", CHF:"CH", ISK:"IS" };

  /* Notes only — a cash desk does not count the loose change twice. */
  const DENOMS = {
    USD:[100,50,20,10,5,1],
    LBP:[100000,50000,20000,10000,5000,1000],
    EUR:[500,200,100,50,20,10,5],
    GBP:[50,20,10,5],
    SYP:[5000,2000,1000,500,200,100],
    TRY:[200,100,50,20,10,5],
    EGP:[200,100,50,20,10,5],
    AED:[1000,500,200,100,50,5],
    SAR:[500,100,50,10,5],
  };

  const FALLBACK = { USD:1, EUR:0.8905, GBP:0.742, LBP:89550.64, SYP:13002, EGP:53.899,
    TRY:50.063, SAR:3.75, AED:3.6725, QAR:3.64, KWD:0.3067, BHD:0.376, OMR:0.3845,
    JOD:0.709, ILS:3.34, IQD:1310, IRR:42000, MAD:9.24, DZD:129.5, TND:2.93, LYD:4.85,
    SDG:601, YER:250, INR:94.469, PKR:283.6, BDT:122.3, LKR:296.5, CNY:7.03, JPY:147.2,
    KRW:1370, HKD:7.81, SGD:1.28, MYR:4.14, THB:32.2, IDR:16250, PHP:57.6, VND:25400,
    AUD:1.54, NZD:1.68, CAD:1.37, BRL:5.35, MXN:18.4, ARS:1250, CLP:945, COP:3900,
    PEN:3.55, ZAR:17.3, NGN:1450, KES:129, GHS:11.7, RUB:79.5, UAH:41.5, PLN:3.62,
    CZK:20.8, HUF:335, RON:4.43, SEK:9.35, NOK:9.98, DKK:6.65, CHF:0.798, ISK:122,
    BTC:0.0000091, ETH:0.00023 };

  const DEFAULT_ORDER = ["LBP","EUR","GBP","EGP","TRY","INR","AED","SAR","XAU","XAG","BTC","JPY","CNY","CHF","CAD","AUD"];
  const STORE = "currency.web.v1";

  const state = {
    base:"USD", amount:100,
    rates:{ ...FALLBACK }, metals:{}, updated:0, live:false,
    order:[...DEFAULT_ORDER], filter:"", tab:"convert",
    chart:{ base:"EUR", quote:"USD", period:"3M" },
    count:{ bag:["USD"], counts:{}, expected:null, history:[], collapsed:[] },
    cached:false,
  };

  /* ── helpers ─────────────────────────────────────────────────────────────── */
  const $ = (id) => document.getElementById(id);
  const el = (tag, cls, text) => { const n = document.createElement(tag); if (cls) n.className = cls; if (text != null) n.textContent = text; return n; };
  const flagOf = (code) => {
    const cc = FLAG[code];
    if (!cc) return null;
    return String.fromCodePoint(...[...cc].map((c) => 0x1F1E6 + c.charCodeAt(0) - 65));
  };
  const nameOf = (code) => (CAT[code] || [code])[0];
  const symOf = (code) => (CAT[code] || [, ""])[1] || "";

  const grouped = (v) => {
    const abs = Math.abs(v);
    const dec = abs >= 100000 ? 0 : abs >= 1 ? 2 : 4;
    return v.toLocaleString("en-US", { minimumFractionDigits: dec, maximumFractionDigits: dec });
  };
  const money2 = (v) => v.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

  /* ── the one money convention ────────────────────────────────────────────── */
  const perUsd = (code) => (code === "XAU" || code === "XAG") ? state.metals[code] : state.rates[code];

  /** amount of `from` → amount of `to`. Divide into USD, then multiply out of it. */
  const convert = (value, from, to) => {
    const f = perUsd(from), t = perUsd(to);
    if (!f || !t) return 0;
    return (value / f) * t;
  };
  const toUsd = (value, code) => {
    const p = perUsd(code);
    return p ? value / p : 0;
  };

  /* ── expression parser: × ÷ before + −, parentheses, null when unparsable ── */
  function evaluate(raw) {
    const src = String(raw).replace(/×/g, "*").replace(/÷/g, "/").replace(/−/g, "-").replace(/,/g, "").trim();
    if (!src) return null;
    const tokens = src.match(/\d*\.?\d+|[+\-*/()]/g);
    if (!tokens || tokens.join("") !== src.replace(/\s+/g, "")) return null;
    let i = 0;
    const peek = () => tokens[i];
    function primary() {
      const t = tokens[i];
      if (t === "(") { i++; const v = expr(); if (tokens[i] !== ")") throw 0; i++; return v; }
      if (t === "-") { i++; return -primary(); }
      if (t === undefined || !/^\d/.test(t)) throw 0;
      i++; const v = Number(t); if (!Number.isFinite(v)) throw 0; return v;
    }
    function term() {
      let v = primary();
      while (peek() === "*" || peek() === "/") {
        const op = tokens[i++]; const r = primary();
        if (op === "/" && r === 0) throw 0;
        v = op === "*" ? v * r : v / r;
      }
      return v;
    }
    const expr = () => { let v = term(); while (peek() === "+" || peek() === "-") { const op = tokens[i++]; const r = term(); v = op === "+" ? v + r : v - r; } return v; };
    try { const v = expr(); return i === tokens.length && Number.isFinite(v) ? v : null; } catch { return null; }
  }

  /* ── persistence ─────────────────────────────────────────────────────────── */
  function saveState() {
    try {
      localStorage.setItem(STORE, JSON.stringify({
        base: state.base, amount: state.amount, order: state.order, tab: state.tab,
        chart: state.chart, count: state.count, rates: state.rates, metals: state.metals,
        updated: state.updated,
      }));
    } catch (e) { /* private mode: the app still works, it just forgets */ }
  }
  function loadState() {
    try {
      const raw = localStorage.getItem(STORE);
      if (!raw) return;
      const s = JSON.parse(raw);
      if (s.base && CAT[s.base]) state.base = s.base;
      if (typeof s.amount === "number" && s.amount > 0) state.amount = s.amount;
      if (Array.isArray(s.order) && s.order.length) state.order = s.order.filter((c) => CAT[c]);
      if (s.tab) state.tab = s.tab;
      if (s.chart) state.chart = { ...state.chart, ...s.chart };
      if (s.count) {
        state.count = {
          bag: Array.isArray(s.count.bag) && s.count.bag.length ? s.count.bag : ["USD"],
          counts: s.count.counts || {}, expected: s.count.expected ?? null,
          history: Array.isArray(s.count.history) ? s.count.history : [],
          collapsed: Array.isArray(s.count.collapsed) ? s.count.collapsed : [],
        };
      }
      if (s.rates) state.rates = { ...state.rates, ...s.rates };
      if (s.metals) state.metals = s.metals;
      if (s.updated) state.updated = s.updated;
      state.cached = Boolean(s.updated);
    } catch (e) { /* a corrupt key must not brick the app */ }
  }

  /* ── toast ───────────────────────────────────────────────────────────────── */
  let toastTimer;
  function toast(msg) {
    const t = $("toast");
    t.textContent = msg;
    t.classList.add("show");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => t.classList.remove("show"), 2000);
  }

  /* ── sheets ──────────────────────────────────────────────────────────────── */
  const openSheets = new Set();
  function sheetOpen(id) {
    openSheets.add(id);
    $("scrim").classList.add("open");
    $(id).classList.add("open");
  }
  function sheetClose(id) {
    openSheets.delete(id);
    if (!openSheets.size) $("scrim").classList.remove("open");
    $(id).classList.remove("open");
  }
  function closeAllSheets() { [...openSheets].forEach(sheetClose); }
  $("scrim").addEventListener("click", closeAllSheets);

  /* ── keypad (amount and quantity share it) ───────────────────────────────── */
  let entryRaw = "", entryFresh = false, keypadMode = "amount", keypadCommit = null;

  function paintEntry() {
    const entry = $("entry");
    if (!entryRaw) { entry.classList.add("idle"); entry.textContent = "Tap digits — arithmetic allowed"; return; }
    entry.classList.remove("idle");
    const preview = evaluate(entryRaw);
    entry.innerHTML = "";
    entry.appendChild(el("span", null, entryRaw.replace(/\*/g, " × ").replace(/\//g, " ÷ ").replace(/-/g, " − ")));
    if (preview != null) entry.appendChild(el("span", "tail", "  = " + grouped(preview)));
  }

  function openKeypad(opts) {
    keypadMode = opts.mode || "amount";
    keypadCommit = opts.onCommit;
    $("sheetTitle").textContent = opts.title || "Convert from";
    $("sheetCur").textContent = opts.subtitle || "";
    document.querySelector("#sheet .key.go").textContent = opts.confirm || "CONVERT";
    entryRaw = opts.initial != null ? String(opts.initial) : "";
    entryFresh = Boolean(entryRaw);
    paintEntry();
    sheetOpen("sheet");
  }

  function press(key) {
    if (key === "C") { entryRaw = ""; entryFresh = false; paintEntry(); return; }
    if (key === "back") { entryRaw = entryRaw.slice(0, -1); entryFresh = false; paintEntry(); return; }
    if (entryFresh && (/^[0-9.]$/.test(key) || key === "(")) entryRaw = "";
    entryFresh = false;
    if (key === "go") {
      const v = evaluate(entryRaw);
      if (v == null || v < 0) { toast("That is not a number I can read"); return; }
      if (keypadCommit) keypadCommit(v);
      sheetClose("sheet");
      return;
    }
    if (/[+\-*/]/.test(key) && /[+\-*/]$/.test(entryRaw)) entryRaw = entryRaw.slice(0, -1);
    entryRaw += key;
    paintEntry();
  }

  document.querySelectorAll("#sheet [data-key]").forEach((b) => b.addEventListener("click", () => press(b.dataset.key)));
  document.querySelectorAll("#sheet [data-chip]").forEach((b) => b.addEventListener("click", () => {
    const v = evaluate(entryRaw || "0") || 0;
    entryRaw = String(v + Number(b.dataset.chip));
    entryFresh = false;
    paintEntry();
  }));

  /* ── picker ──────────────────────────────────────────────────────────────── */
  function openPicker({ title, codes, onPick, filter }) {
    $("pickerTitle").textContent = title || "Choose a currency";
    $("pickerSearch").value = "";
    const list = $("pickerList");
    const draw = (q) => {
      list.innerHTML = "";
      const needle = (q || "").toLowerCase().trim();
      codes
        .filter((c) => !needle || c.toLowerCase().includes(needle) || nameOf(c).toLowerCase().includes(needle))
        .forEach((c) => {
          const row = el("div", "picker-item");
          const b = el("div", "badge flag");
          const f = flagOf(c);
          if (f) b.textContent = f; else { b.classList.remove("flag"); b.classList.add("fiat"); b.textContent = symOf(c) || c.slice(0, 1); }
          row.appendChild(b);
          const col = el("div");
          col.appendChild(el("div", "pi1", c));
          col.appendChild(el("div", "pi2", nameOf(c)));
          row.appendChild(col);
          row.addEventListener("click", () => { sheetClose("picker"); onPick(c); });
          list.appendChild(row);
        });
      if (!list.children.length) list.appendChild(el("div", "empty", "Nothing matches that."));
    };
    draw(filter || "");
    $("pickerSearch").oninput = (e) => draw(e.target.value);
    sheetOpen("picker");
  }
  $("pickerClose").addEventListener("click", () => sheetClose("picker"));

  /* ── rates ───────────────────────────────────────────────────────────────── */
  function offlineNotice() {
    state.live = false;
    renderStatus();
    $("footLeft").textContent = state.updated
      ? "Offline — rates from " + new Date(state.updated).toLocaleString("en-GB", { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" })
      : "Offline — built-in reference rates, not live";
  }

  async function loadRates(force) {
    const started = performance.now();
    // A hard offline is known before asking: skip the request and say so, rather than waiting out
    // a timeout and then blaming the feed.
    if (navigator.onLine === false) { offlineNotice(); return; }
    $("statusText").textContent = "…";
    try {
      const [fiat, gold, silver] = await Promise.all([
        fetch("https://open.er-api.com/v6/latest/USD", { cache: "no-store" }).then((r) => r.json()),
        fetch("https://api.gold-api.com/price/XAU", { cache: "no-store" }).then((r) => r.json()).catch(() => null),
        fetch("https://api.gold-api.com/price/XAG", { cache: "no-store" }).then((r) => r.json()).catch(() => null),
      ]);
      if (fiat && fiat.rates) state.rates = { ...state.rates, ...fiat.rates };
      if (gold && gold.price) state.metals.XAU = gold.price;
      if (silver && silver.price) state.metals.XAG = silver.price;
      state.live = true;
      state.cached = false;
      state.updated = Date.now();
      saveState();
      renderAll();
      const ms = Math.round(performance.now() - started);
      $("footLeft").textContent = "Rates by open.er-api.com · " + ms + " ms";
      if (window.ChartView) window.ChartView.onRates();
      if (window.CountView) window.CountView.render();
    } catch (err) {
      offlineNotice();
    }
  }

  function renderStatus() {
    const pill = $("status");
    if (!state.live) {
      pill.classList.add("stale");
      $("statusText").textContent = state.updated ? "Cached " + new Date(state.updated).toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit" }) : "Offline";
      return;
    }
    pill.classList.remove("stale");
    $("statusText").textContent = "Updated " + new Date(state.updated).toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit" });
  }

  /* ── convert view ────────────────────────────────────────────────────────── */
  function renderBase() {
    $("baseCode").textContent = state.base;
    $("baseFlag").textContent = flagOf(state.base) || "¤";
    const sym = symOf(state.base);
    $("baseSymbol").textContent = sym && !/^[A-Z]{3}$/.test(sym) && sym.length <= 3 ? sym : "";
    const parts = money2(state.amount).split(".");
    $("amountInt").textContent = Number(parts[0].replace(/,/g, "")).toLocaleString("en-US");
    $("amountCents").textContent = "." + parts[1];
    $("baseName").textContent = nameOf(state.base);
  }

  function rowFor(code) {
    const li = el("li", "row");
    if (code === "LBP") li.classList.add("pinned");
    const b = el("div", "badge");
    if (code === "XAU") { b.classList.add("metal-gold"); b.textContent = "Au"; }
    else if (code === "XAG") { b.classList.add("metal-silver"); b.textContent = "Ag"; }
    else {
      const f = flagOf(code);
      if (f) { b.classList.add("flag"); b.textContent = f; }
      else { b.classList.add("fiat"); b.textContent = symOf(code) || code; }
    }
    li.appendChild(b);

    const main = el("div", "row-main");
    const top = el("div", "code");
    top.appendChild(el("span", null, code));
    if (code === "LBP") top.appendChild(el("span", "pin", "★"));
    main.appendChild(top);
    main.appendChild(el("div", "name-sm", nameOf(code)));
    li.appendChild(main);

    const val = el("div", "value");
    const p = perUsd(code);
    if (p && (code === "XAU" || code === "XAG")) {
      val.classList.add("metal");
      val.appendChild(el("div", "num", "$" + money2(p) + " / oz"));
      val.appendChild(el("div", "sub", "24k $" + money2(p / 31.1034768) + "/g"));
    } else if (p) {
      const sym = symOf(code);
      const prefix = sym && !/^[A-Z]{3}$/.test(sym) && sym.length <= 2 ? sym + " " : "";
      val.appendChild(el("div", "num", prefix + grouped(convert(state.amount, state.base, code))));
    } else {
      val.appendChild(el("div", "num", "—"));
      val.appendChild(el("div", "sub", "no rate"));
    }
    li.appendChild(val);
    li.addEventListener("click", () => { if (code !== state.base) setBase(code); });
    return li;
  }

  function renderList() {
    const list = $("list");
    list.innerHTML = "";
    const q = state.filter.trim().toLowerCase();
    let codes = q
      ? Object.keys(CAT).filter((c) => c.toLowerCase().includes(q) || nameOf(c).toLowerCase().includes(q))
      : [...state.order];
    codes = codes.filter((c) => c !== state.base).slice(0, 80);
    codes.forEach((c) => list.appendChild(rowFor(c)));
    if (!codes.length) {
      const empty = el("li", "foot", 'Nothing matches "' + state.filter + '"');
      empty.style.justifyContent = "center";
      list.appendChild(empty);
    }
  }

  function setBase(code) {
    state.base = code;
    state.filter = "";
    $("search").value = "";
    saveState();
    renderAll();
    toast("Base set to " + code);
  }

  function renderAll() {
    renderStatus();
    renderBase();
    renderList();
  }

  /* ── nav ─────────────────────────────────────────────────────────────────── */
  function setTab(tab) {
    state.tab = tab;
    document.querySelectorAll(".view").forEach((v) => v.classList.toggle("active", v.id === "view-" + tab));
    document.querySelectorAll(".tab").forEach((t) => t.classList.toggle("active", t.dataset.view === tab));
    window.scrollTo({ top: 0 });
    if (tab === "chart" && window.ChartView) window.ChartView.show();
    if (tab === "count" && window.CountView) window.CountView.render();
    saveState();
  }
  document.querySelectorAll(".tab").forEach((b) => b.addEventListener("click", () => setTab(b.dataset.view)));

  /* ── wiring ──────────────────────────────────────────────────────────────── */
  $("baseCard").addEventListener("click", () => openKeypad({
    title: "Convert from", subtitle: state.base + " · " + nameOf(state.base),
    initial: state.amount, confirm: "CONVERT",
    onCommit: (v) => { if (v <= 0) { toast("Amount has to be more than zero"); return; } state.amount = v; saveState(); renderAll(); },
  }));
  $("search").addEventListener("input", (e) => { state.filter = e.target.value; renderList(); });
  $("resetOrder").addEventListener("click", () => {
    state.order = [...DEFAULT_ORDER]; state.filter = ""; $("search").value = "";
    saveState(); renderList(); toast("Order reset");
  });
  $("status").addEventListener("click", () => loadRates(true));

  document.addEventListener("keydown", (e) => {
    if (openSheets.size === 0) return;
    if (e.key === "Escape") { closeAllSheets(); return; }
    if (!$("sheet").classList.contains("open")) return;
    if (/^[0-9.]$/.test(e.key)) press(e.key);
    else if (e.key === "Backspace") press("back");
    else if (e.key === "Enter") press("go");
    else if (e.key === "*") press("*");
    else if (e.key === "/") press("/");
    else if (e.key === "+") press("+");
    else if (e.key === "-") press("-");
  });

  window.addEventListener("online", () => { toast("Back online — refreshing"); loadRates(true); });
  window.addEventListener("offline", () => { state.live = false; renderStatus(); toast("Offline — showing the last known rates"); });

  document.addEventListener("visibilitychange", () => {
    if (!document.hidden && Date.now() - state.updated > 5 * 60 * 1000) loadRates(false);
  });

  /* ── install + offline shell ─────────────────────────────────────────────── */
  let installEvent = null;
  window.addEventListener("beforeinstallprompt", (e) => {
    e.preventDefault();
    installEvent = e;
    $("installBanner").hidden = false;
  });
  $("installBanner").addEventListener("click", async () => {
    if (!installEvent) { toast("Use your browser menu → Add to Home screen"); return; }
    installEvent.prompt();
    const choice = await installEvent.userChoice;
    if (choice.outcome === "accepted") { $("installBanner").hidden = true; toast("Installed"); }
    installEvent = null;
  });
  window.addEventListener("appinstalled", () => { $("installBanner").hidden = true; });

  if ("serviceWorker" in navigator) {
    window.addEventListener("load", () => navigator.serviceWorker.register("sw.js").catch(() => {}));
  }

  /* ── expose + boot ───────────────────────────────────────────────────────── */
  window.App = {
    state, CAT, DENOMS, FLAG, flagOf, nameOf, symOf, grouped, money2,
    perUsd, convert, toUsd, evaluate, toast, saveState,
    sheetOpen, sheetClose, closeAllSheets, openKeypad, openPicker, setTab, renderAll, loadRates,
  };

  loadState();
  renderAll();
  setTab(state.tab || "convert");
  loadRates(false);
})();
/* Chart view — daily ECB reference rates.
 *
 * The ECB publishes ~30 currencies; it does not publish LBP, SYP or gold. Rather than draw a
 * straight line and call it history, an unsupported pair says so and points at the Convert tab,
 * where the live rate does exist. A chart that lies is worse than no chart.
 */
(() => {
  const A = window.App;
  const W = 360, H = 190, PAD = { l: 4, r: 4, t: 10, b: 18 };

  const s = () => A.state.chart;

  let supported = null;      // codes the ECB actually publishes
  let series = [];           // [{ at:Date, value:number }]
  let loading = false;

  const nf = (v) => {
    const abs = Math.abs(v);
    const dec = abs >= 1000 ? 0 : abs >= 10 ? 2 : 4;
    return v.toLocaleString("en-US", { minimumFractionDigits: dec, maximumFractionDigits: dec });
  };

  const startFor = (period) => {
    const d = new Date();
    const back = { "1M": 31, "3M": 92, "6M": 183, "1Y": 366 }[period] || 92;
    d.setDate(d.getDate() - back);
    return d.toISOString().slice(0, 10);
  };

  async function ensureSupported() {
    if (supported) return supported;
    const res = await fetch("https://api.frankfurter.dev/v1/currencies");
    const map = await res.json();
    supported = Object.keys(map).sort();
    // EUR is the API's anchor currency but arrives in the map too; keep it, it is a valid base.
    return supported;
  }

  async function load() {
    const { base, quote, period } = s();
    if (base === quote) { showMessage("Pick two different currencies — a currency against itself is a straight line."); return; }
    loading = true;
    $status("…");
    try {
      await ensureSupported();
      const unsupported = [base, quote].filter((c) => !supported.includes(c));
      if (unsupported.length) {
        showMessage(
          unsupported.join(" and ") + (unsupported.length > 1 ? " have" : " has") +
          " no daily history at the ECB, which is the only keyless free source. The live rate is on the " +
          "Convert tab — this screen deliberately does not invent a past for it."
        );
        return;
      }
      const end = new Date().toISOString().slice(0, 10);
      const url = "https://api.frankfurter.dev/v1/" + startFor(period) + ".." + end +
        "?base=" + encodeURIComponent(base) + "&symbols=" + encodeURIComponent(quote);
      const res = await fetch(url);
      if (!res.ok) throw new Error("HTTP " + res.status);
      const data = await res.json();
      const rates = data.rates || {};
      series = Object.keys(rates).sort().map((day) => ({ at: day, value: rates[day][quote] }))
        .filter((p) => Number.isFinite(p.value));
      if (!series.length) { showMessage("The ECB returned no quotes for this pair in the selected range."); return; }
      hideMessage();
      draw();
      $status(new Date(series[series.length - 1].at + "T00:00:00").toLocaleDateString("en-GB", { day: "2-digit", month: "short" }));
      A.state.chart.lastUpdate = Date.now();
    } catch (err) {
      showMessage("Could not reach the ECB feed. The last chart you loaded stays on screen when the network blinks; this build keeps no copy of the series.");
      $status("offline");
    } finally {
      loading = false;
    }
  }

  const $status = (t) => { document.getElementById("chartStatusText").textContent = t; };

  function showMessage(text) {
    const box = document.getElementById("chartEmpty");
    box.hidden = false;
    box.textContent = text;
    document.getElementById("chartWrap").style.opacity = "0.15";
    document.getElementById("chartLast").textContent = "—";
    document.getElementById("chartLastLabel").textContent = "no series";
    document.getElementById("chartDelta").textContent = "—";
    document.getElementById("chartDelta").className = "delta";
    ["chartHigh", "chartLow", "chartChange"].forEach((id) => { document.getElementById(id).textContent = "—"; });
    series = [];
  }
  function hideMessage() {
    document.getElementById("chartEmpty").hidden = true;
    document.getElementById("chartWrap").style.opacity = "1";
  }

  function draw() {
    const svg = document.getElementById("chart");
    const { base, quote } = s();
    const values = series.map((p) => p.value);
    const min = Math.min(...values), max = Math.max(...values);
    const span = max - min || Math.max(max * 0.01, 0.0001);
    const lo = min - span * 0.12, hi = max + span * 0.12;
    const x = (i) => PAD.l + (i / Math.max(1, series.length - 1)) * (W - PAD.l - PAD.r);
    const y = (v) => PAD.t + (1 - (v - lo) / (hi - lo)) * (H - PAD.t - PAD.b);

    const pts = series.map((p, i) => [x(i), y(p.value)]);
    const line = pts.map((p, i) => (i ? "L" : "M") + p[0].toFixed(2) + " " + p[1].toFixed(2)).join(" ");
    const area = line + " L" + pts[pts.length - 1][0].toFixed(2) + " " + (H - PAD.b) + " L" + pts[0][0].toFixed(2) + " " + (H - PAD.b) + " Z";
    const last = series[series.length - 1];
    const first = series[0];
    const up = last.value >= first.value;

    svg.innerHTML = `
      <defs>
        <linearGradient id="fillUp" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#00D632" stop-opacity=".28"/>
          <stop offset="100%" stop-color="#00D632" stop-opacity="0"/>
        </linearGradient>
        <linearGradient id="fillDown" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#FF453A" stop-opacity=".24"/>
          <stop offset="100%" stop-color="#FF453A" stop-opacity="0"/>
        </linearGradient>
      </defs>
      <line x1="0" y1="${(H - PAD.b).toFixed(1)}" x2="${W}" y2="${(H - PAD.b).toFixed(1)}" stroke="#26262A" stroke-width="1"/>
      <path d="${area}" fill="url(#${up ? "fillUp" : "fillDown"})"/>
      <path d="${line}" fill="none" stroke="${up ? "#00D632" : "#FF453A"}" stroke-width="2"
            stroke-linejoin="round" stroke-linecap="round" vector-effect="non-scaling-stroke"/>
      <circle cx="${pts[pts.length - 1][0].toFixed(2)}" cy="${pts[pts.length - 1][1].toFixed(2)}" r="3.4" fill="${up ? "#00D632" : "#FF453A"}"/>
      <line id="scrubLine" x1="0" y1="${PAD.t}" x2="0" y2="${H - PAD.b}" stroke="#5C5C63" stroke-width="1" stroke-dasharray="3 3" opacity="0"/>
      <circle id="scrubDot" cx="0" cy="0" r="4" fill="#fff" opacity="0"/>
    `;

    const lastStr = nf(last.value);
    document.getElementById("chartLast").innerHTML = lastStr.replace(
      /(\.\d+)$/,
      '<span class="cents" style="color:' + (up ? "var(--green)" : "#FF8A80") + '">$1</span>'
    );
    document.getElementById("chartLastLabel").textContent =
      "1 " + base + " = " + nf(last.value) + " " + quote + " · " + new Date(last.at + "T00:00:00").toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" });

    const change = ((last.value - first.value) / first.value) * 100;
    const chip = document.getElementById("chartDelta");
    chip.textContent = (change >= 0 ? "▲ " : "▼ ") + Math.abs(change).toFixed(2) + "%";
    chip.className = "delta " + (change >= 0 ? "up" : "down");

    document.getElementById("chartHigh").textContent = nf(max);
    document.getElementById("chartLow").textContent = nf(min);
    document.getElementById("chartChange").textContent = (change >= 0 ? "+" : "") + change.toFixed(2) + "%";
    document.getElementById("chartChange").style.color = change >= 0 ? "var(--green)" : "#FF8A80";
    attachScrub(x, y);
  }

  function attachScrub(x, y) {
    const wrap = document.getElementById("chartWrap");
    const svg = document.getElementById("chart");
    const tip = document.getElementById("scrubTip");
    const line = () => document.getElementById("scrubLine");
    const dot = () => document.getElementById("scrubDot");

    const move = (clientX) => {
      if (!series.length) return;
      const rect = svg.getBoundingClientRect();
      const rel = Math.min(1, Math.max(0, (clientX - rect.left) / rect.width));
      const i = Math.round(rel * (series.length - 1));
      const point = series[i];
      const px = x(i), py = y(point.value);
      line().setAttribute("x1", px); line().setAttribute("x2", px); line().setAttribute("opacity", "1");
      dot().setAttribute("cx", px); dot().setAttribute("cy", py); dot().setAttribute("opacity", "1");
      tip.classList.add("show");
      tip.style.left = Math.min(rect.width - 74, Math.max(74, (px / W) * rect.width)) + "px";
      tip.textContent = new Date(point.at + "T00:00:00").toLocaleDateString("en-GB", { day: "2-digit", month: "short" }) + " · " + nf(point.value);
    };
    const clear = () => {
      tip.classList.remove("show");
      const l = line(), d = dot();
      if (l) l.setAttribute("opacity", "0");
      if (d) d.setAttribute("opacity", "0");
    };

    wrap.onpointerdown = (e) => { wrap.setPointerCapture(e.pointerId); move(e.clientX); };
    wrap.onpointermove = (e) => { if (e.buttons || e.pointerType === "touch") move(e.clientX); };
    wrap.onpointerup = (e) => { move(e.clientX); setTimeout(clear, 1400); };
    wrap.onpointerleave = clear;
  }

  /* ── pair controls ───────────────────────────────────────────────────────── */
  function paintPair() {
    const { base, quote } = s();
    document.getElementById("chartBase").textContent = base;
    document.getElementById("chartQuote").textContent = quote;
    document.getElementById("chartBaseFlag").textContent = A.flagOf(base) || "¤";
    document.getElementById("chartQuoteFlag").textContent = A.flagOf(quote) || "¤";
    document.querySelectorAll(".period").forEach((b) => b.classList.toggle("active", b.dataset.period === s().period));
  }

  async function pickPair(which) {
    await ensureSupported();
    A.openPicker({
      title: which === "base" ? "Chart from" : "Chart to",
      codes: supported,
      onPick: (code) => {
        s()[which] = code;
        if (s().base === s().quote) s()[which === "base" ? "quote" : "base"] = code === "USD" ? "EUR" : "USD";
        A.saveState();
        paintPair();
        load();
      },
    });
  }

  document.getElementById("pickBase").addEventListener("click", () => pickPair("base"));
  document.getElementById("pickQuote").addEventListener("click", () => pickPair("quote"));
  document.getElementById("swapPair").addEventListener("click", () => {
    const c = s(); const t = c.base; c.base = c.quote; c.quote = t;
    A.saveState(); paintPair(); load();
  });
  document.querySelectorAll(".period").forEach((b) => b.addEventListener("click", () => {
    s().period = b.dataset.period;
    A.saveState();
    paintPair();
    load();
  }));
  document.getElementById("chartStatus").addEventListener("click", () => load());

  window.ChartView = {
    show() { paintPair(); if (!series.length && !loading) load(); },
    onRates() { /* the chart is ECB-only; er-api rates do not feed it */ },
  };
})();
/* Count view — a mixed bag of banknotes, totalled in USD.
 *
 * Same rules as the Android build: notes only, one collapsible section per currency, a running
 * subtotal in the local currency with its USD equivalent underneath, and a grand total in USD.
 * Every currency converts through the same helper as the Convert tab, so 50,000 LBP can never be
 * worth four billion dollars again.
 */
(() => {
  const A = window.App;
  const c = () => A.state.count;

  const lineValue = (code, denom) => (c().counts[code]?.[denom] || 0) * denom;

  const localTotal = (code) => (A.DENOMS[code] || []).reduce((sum, d) => sum + lineValue(code, d), 0);
  const noteCount = (code) => (A.DENOMS[code] || []).reduce((sum, d) => sum + (c().counts[code]?.[d] || 0), 0);

  function totals() {
    let usd = 0, notes = 0, withMoney = 0;
    c().bag.forEach((code) => {
      const local = localTotal(code);
      if (local > 0) withMoney += 1;
      usd += A.toUsd(local, code);
      notes += noteCount(code);
    });
    return { usd, notes, currencies: withMoney };
  }

  const setCount = (code, denom, n) => {
    const bag = c().counts;
    bag[code] = bag[code] || {};
    if (n <= 0) delete bag[code][denom];
    else bag[code][denom] = n;
    A.saveState();
    render();
  };

  /* ── cards ───────────────────────────────────────────────────────────────── */
  function badgeFor(code) {
    const b = document.createElement("div");
    b.className = "badge";
    const f = A.flagOf(code);
    if (f) { b.classList.add("flag"); b.textContent = f; }
    else { b.classList.add("fiat"); b.textContent = A.symOf(code) || code; }
    return b;
  }

  function cardFor(code) {
    const card = document.createElement("div");
    card.className = "cur-card" + (c().collapsed.includes(code) ? " collapsed" : "");

    const head = document.createElement("div");
    head.className = "cur-head";
    head.appendChild(badgeFor(code));

    const titles = document.createElement("div");
    titles.className = "cur-titles";
    const c1 = document.createElement("div");
    c1.className = "c1";
    c1.appendChild(document.createElement("span")).textContent = code;
    titles.appendChild(c1);
    const c2 = document.createElement("div");
    c2.className = "c2";
    const notes = noteCount(code);
    c2.textContent = A.nameOf(code) + " · " + notes + (notes === 1 ? " note" : " notes");
    titles.appendChild(c2);
    head.appendChild(titles);

    const total = document.createElement("div");
    total.className = "cur-total";
    const local = localTotal(code);
    const sym = A.symOf(code);
    const prefix = sym && !/^[A-Z]{3}$/.test(sym) && sym.length <= 2 ? sym : "";
    total.appendChild(Object.assign(document.createElement("div"), { className: "t1", textContent: prefix + A.grouped(local) }));
    if (code !== "USD") {
      total.appendChild(Object.assign(document.createElement("div"), { className: "t2", textContent: "$" + A.money2(A.toUsd(local, code)) }));
    }
    head.appendChild(total);

    const chev = document.createElement("div");
    chev.className = "chev";
    chev.textContent = "▾";
    head.appendChild(chev);
    head.addEventListener("click", () => {
      const i = c().collapsed.indexOf(code);
      if (i >= 0) c().collapsed.splice(i, 1); else c().collapsed.push(code);
      A.saveState();
      render();
    });
    card.appendChild(head);

    const body = document.createElement("div");
    body.className = "cur-body";
    (A.DENOMS[code] || []).forEach((denom) => body.appendChild(rowFor(code, denom)));
    card.appendChild(body);

    const foot = document.createElement("div");
    foot.className = "cur-foot";
    const rate = A.perUsd(code);
    foot.appendChild(Object.assign(document.createElement("span"), {
      textContent: code === "USD" ? "Base currency" : rate ? "1 USD = " + A.grouped(rate) + " " + code : "no rate",
    }));
    const remove = document.createElement("button");
    remove.type = "button";
    remove.textContent = "Remove";
    remove.addEventListener("click", () => {
      if (c().bag.length === 1) { A.toast("Keep at least one currency in the bag"); return; }
      c().bag = c().bag.filter((x) => x !== code);
      delete c().counts[code];
      A.saveState();
      render();
    });
    foot.appendChild(remove);
    card.appendChild(foot);
    return card;
  }

  function rowFor(code, denom) {
    const row = document.createElement("div");
    row.className = "note-row";

    const label = document.createElement("div");
    label.className = "denom";
    const sym = A.symOf(code);
    const prefix = sym && !/^[A-Z]{3}$/.test(sym) && sym.length <= 2 ? '<span class="sym">' + sym + "</span>" : "";
    label.innerHTML = prefix + denom.toLocaleString("en-US");

    const n = c().counts[code]?.[denom] || 0;
    const stepper = document.createElement("div");
    stepper.className = "stepper";

    const minus = document.createElement("button");
    minus.type = "button";
    minus.className = "step";
    minus.textContent = "−";
    minus.disabled = n === 0;
    minus.addEventListener("click", () => setCount(code, denom, n - 1));

    const value = document.createElement("div");
    value.className = "count-val";
    value.textContent = n;
    value.title = "Tap to type";
    value.addEventListener("click", () => A.openKeypad({
      title: "How many notes?",
      subtitle: prefix.replace(/<[^>]+>/g, "") + denom.toLocaleString("en-US") + " · " + code,
      initial: n, confirm: "SET",
      onCommit: (v) => setCount(code, denom, Math.round(v)),
    }));

    const plus = document.createElement("button");
    plus.type = "button";
    plus.className = "step plus";
    plus.textContent = "+";
    // Long press: type how many notes at once, the way the Android build does. `longFired` stops the
    // click that always follows a long press from adding one more on top of what was just typed.
    let hold, longFired = false;
    const startHold = () => {
      longFired = false;
      hold = setTimeout(() => {
        hold = null;
        longFired = true;
        A.openKeypad({
          title: "How many notes?",
          subtitle: prefix.replace(/<[^>]+>/g, "") + denom.toLocaleString("en-US") + " · " + code,
          initial: n, confirm: "SET",
          onCommit: (v) => setCount(code, denom, Math.round(v)),
        });
      }, 450);
    };
    const cancelHold = () => { if (hold) { clearTimeout(hold); hold = null; } };
    plus.addEventListener("pointerdown", startHold);
    plus.addEventListener("pointerup", cancelHold);
    plus.addEventListener("pointerleave", cancelHold);
    plus.addEventListener("click", () => {
      if (longFired) { longFired = false; return; }
      setCount(code, denom, n + 1);
    });

    stepper.append(minus, value, plus);
    row.append(label, stepper);

    const line = document.createElement("div");
    const amount = lineValue(code, denom);
    line.className = "line-total" + (amount ? "" : " zero");
    const s2 = A.symOf(code);
    const p2 = s2 && !/^[A-Z]{3}$/.test(s2) && s2.length <= 2 ? s2 + " " : "";
    line.textContent = amount ? p2 + A.grouped(amount) : "—";
    row.appendChild(line);
    return row;
  }

  /* ── total card ──────────────────────────────────────────────────────────── */
  function renderTotal() {
    const t = totals();
    const cents = Math.round(t.usd * 100);
    document.getElementById("countInt").textContent = Math.floor(cents / 100).toLocaleString("en-US");
    document.getElementById("countCents").textContent = "." + String(cents % 100).padStart(2, "0");
    document.getElementById("countMeta").textContent =
      t.notes + (t.notes === 1 ? " note" : " notes") + " · " + t.currencies + (t.currencies === 1 ? " currency" : " currencies");

    const chip = document.getElementById("verdictChip");
    const exp = c().expected;
    if (!exp || exp <= 0) { chip.textContent = "—"; chip.className = "chip-v"; return; }
    const diff = t.usd - exp;
    if (Math.abs(diff) < 0.005) { chip.textContent = "✓ Match"; chip.className = "chip-v match"; }
    else if (diff < 0) { chip.textContent = "Short $" + A.money2(-diff); chip.className = "chip-v short"; }
    else { chip.textContent = "Over $" + A.money2(diff); chip.className = "chip-v over"; }

    document.getElementById("countSave").disabled = t.usd <= 0;
  }

  function render() {
    const host = document.getElementById("curCards");
    host.innerHTML = "";
    c().bag.forEach((code) => host.appendChild(cardFor(code)));
    renderTotal();
    const exp = document.getElementById("expected");
    if (document.activeElement !== exp) exp.value = c().expected ? String(c().expected) : "";
    const newest = c().history[0];
    document.getElementById("countFoot").textContent = newest
      ? "Last saved " + new Date(newest.at).toLocaleString("en-GB", { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" })
      : "Nothing saved yet";
  }

  /* ── actions ─────────────────────────────────────────────────────────────── */
  document.getElementById("countClear").addEventListener("click", () => {
    c().counts = {};
    A.saveState();
    render();
    A.toast("Bag cleared");
  });

  document.getElementById("countSave").addEventListener("click", () => {
    const t = totals();
    if (t.usd <= 0) { A.toast("Count something first"); return; }
    c().history.unshift({
      at: Date.now(),
      totalUsd: t.usd,
      notes: t.notes,
      bag: [...c().bag],
      counts: JSON.parse(JSON.stringify(c().counts)),
      rates: { LBP: A.perUsd("LBP"), EUR: A.perUsd("EUR"), GBP: A.perUsd("GBP") },
    });
    c().history = c().history.slice(0, 40);
    A.saveState();
    render();
    A.toast("Saved · $" + A.money2(t.usd));
  });

  const historySheet = document.getElementById("history");
  document.getElementById("countHistory").addEventListener("click", () => {
    const list = document.getElementById("historyList");
    list.innerHTML = "";
    if (!c().history.length) {
      list.appendChild(Object.assign(document.createElement("div"), { className: "empty", textContent: "No saved counts yet. Count a bag and press Save — the stacks come back exactly as you left them." }));
    }
    c().history.forEach((entry, index) => {
      const row = document.createElement("div");
      row.className = "hist";
      const left = document.createElement("div");
      left.appendChild(Object.assign(document.createElement("div"), { className: "h1", textContent: "$" + A.money2(entry.totalUsd) }));
      left.appendChild(Object.assign(document.createElement("div"), {
        className: "h2",
        textContent: new Date(entry.at).toLocaleString("en-GB", { day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" }) + " · " + entry.notes + " notes · " + entry.bag.join(" "),
      }));
      row.appendChild(left);
      const right = document.createElement("div");
      right.className = "h3";
      const restore = document.createElement("button");
      restore.type = "button";
      restore.className = "mini";
      restore.textContent = "Restore";
      restore.addEventListener("click", () => {
        c().bag = [...entry.bag];
        c().counts = JSON.parse(JSON.stringify(entry.counts));
        A.saveState();
        render();
        A.sheetClose("history");
        A.toast("Restored · $" + A.money2(entry.totalUsd));
      });
      const del = document.createElement("button");
      del.type = "button";
      del.className = "mini";
      del.style.marginLeft = "6px";
      del.textContent = "✕";
      del.addEventListener("click", () => {
        c().history.splice(index, 1);
        A.saveState();
        A.sheetClose("history");
        document.getElementById("countHistory").click();
        render();
      });
      right.append(restore, del);
      row.appendChild(right);
      list.appendChild(row);
    });
    A.sheetOpen("history");
  });
  document.getElementById("historyClose").addEventListener("click", () => A.sheetClose("history"));

  document.getElementById("addCurrency").addEventListener("click", () => {
    A.openPicker({
      title: "Add to the bag",
      codes: Object.keys(A.DENOMS).filter((code) => !c().bag.includes(code)),
      onPick: (code) => {
        c().bag.push(code);
        A.saveState();
        render();
        A.toast(code + " added to the bag");
      },
    });
  });

  const exp = document.getElementById("expected");
  exp.addEventListener("input", () => {
    const v = parseFloat(String(exp.value).replace(/,/g, ""));
    c().expected = Number.isFinite(v) && v > 0 ? v : null;
    A.saveState();
    renderTotal();
  });
  exp.addEventListener("blur", () => { exp.value = c().expected ? String(c().expected) : ""; });

  window.CountView = { render };
})();
