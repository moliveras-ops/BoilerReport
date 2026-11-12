// Corrector ortográfico (español) — corre aparte para no congelar la app
importScripts('nspell.js');
const TEC = 'sobrecalentador sobrecalentadores economizador economizadores precalentador precalentadores desaireador desaireadores espesorimetría termografía gamagrafía manifold manifolds sootblower sootblowers socavación socavaciones ovalamiento ovalamientos pitting deflector deflectores ducto ductos sulfuración atomizador atomizadores spout spouts Arauco Valdivia Licancel Celco Biobío recuperadora recuperadoras niple niples mm cm kg psi bar mpa MPa Hz ok';
let sp = null, err = null;
const ready = Promise.all(['es.aff', 'es.dic'].map(f => fetch(f).then(r => { if (!r.ok) throw new Error(f); return r.text(); })))
  .then(([aff, dic]) => { sp = nspell(aff, dic); TEC.split(' ').forEach(w => sp.add(w)); })
  .catch(e => { err = String(e && e.message || e); });
const ok = w => sp.correct(w) || sp.correct(w.toLowerCase());
self.onmessage = async e => {
  const { id, words = [], add = [], rem = [] } = e.data || {};
  await ready;
  if (err) { postMessage({ id, err }); return; }
  rem.forEach(w => sp.remove(w));
  add.forEach(w => sp.add(w));
  const bad = {};
  words.forEach(w => { if (!(w in bad) && !ok(w)) bad[w] = sp.suggest(w).slice(0, 4); });
  postMessage({ id, bad });
};
