'use client';

import { useMemo, useState } from 'react';

type PathItem = { path: string; preview: string };
type ScanResult = {
  ok: boolean;
  contentType: 'json' | 'text';
  paths: PathItem[];
  rawPreview: string;
  error?: string;
};

type ConfigResult = {
  ok: boolean;
  token: string;
  valueUrl: string;
  wallpaperUrl: string;
  dynamicValueUrl: string;
  dynamicWallpaperUrl: string;
  error?: string;
};

export default function HomePage() {
  const [sourceUrl, setSourceUrl] = useState('');
  const [wikiLang, setWikiLang] = useState('fr');
  const [fallbackLang, setFallbackLang] = useState('en');
  const [width, setWidth] = useState(1080);
  const [height, setHeight] = useState(2400);
  const [template, setTemplate] = useState<'wiki-photo' | 'text-only'>('wiki-photo');
  const [selectedPath, setSelectedPath] = useState('');
  const [scan, setScan] = useState<ScanResult | null>(null);
  const [config, setConfig] = useState<ConfigResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const selectedPreview = useMemo(() => {
    return scan?.paths.find(p => p.path === selectedPath)?.preview || '';
  }, [scan, selectedPath]);

  async function scanSource() {
    setLoading(true);
    setError('');
    setMessage('Scan en cours...');
    setConfig(null);
    try {
      const res = await fetch('/api/scan', {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ sourceUrl })
      });
      const data = await res.json();
      if (!res.ok || !data.ok) throw new Error(data.error || 'Scan impossible');
      setScan(data);
      const firstPath = data.paths?.[0]?.path || '';
      setSelectedPath(firstPath);
      setMessage(`Scan OK. ${data.paths?.length || 0} champ(s) détecté(s).`);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur inconnue');
      setScan(null);
    } finally {
      setLoading(false);
    }
  }

  async function createConfig() {
    setLoading(true);
    setError('');
    setMessage('Création du token...');
    try {
      const res = await fetch('/api/config', {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({
          sourceUrl,
          valuePath: selectedPath,
          wikiLang,
          fallbackLang,
          width,
          height,
          template
        })
      });
      const data = await res.json();
      if (!res.ok || !data.ok) throw new Error(data.error || 'Configuration impossible');
      setConfig(data);
      setMessage('Configuration créée. Copie le token dans l’app Android.');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur inconnue');
      setConfig(null);
    } finally {
      setLoading(false);
    }
  }

  function useMock() {
    const origin = window.location.origin;
    setSourceUrl(`${origin}/api/mock?selection=Tom%20Cruise`);
    setScan(null);
    setConfig(null);
    setSelectedPath('');
    setMessage('URL de test ajoutée. Clique sur Scanner.');
  }

  async function copy(text: string) {
    await navigator.clipboard.writeText(text);
    setMessage('Copié.');
  }

  return (
    <main className="page">
      <section className="hero">
        <div className="card">
          <h1>Magic Lockscreen Android</h1>
          <p>
            Colle ton URL Inject / WikiTest, choisis la valeur à écouter, puis récupère un token pour l’app Android.
            Le backend génère ensuite un fond d’écran avec image Wikipédia et texte de révélation.
          </p>

          <label>URL source Inject / WikiTest</label>
          <input
            value={sourceUrl}
            onChange={e => setSourceUrl(e.target.value)}
            placeholder="https://11z.co/_w/xxxxx/selection"
          />

          <div className="actions">
            <button onClick={scanSource} disabled={loading || !sourceUrl}>Scanner</button>
            <button className="secondary" onClick={useMock} disabled={loading}>Utiliser une source test</button>
          </div>

          {message && <div className="status">{message}</div>}
          {error && <div className="status error">{error}</div>}
        </div>

        <div className="preview-phone">
          <div>
            <strong>{selectedPreview || 'Révélation'}</strong>
            <span>Fond d’écran généré pour Android</span>
          </div>
        </div>
      </section>

      {scan && (
        <section className="card" style={{ marginTop: 24 }}>
          <h2>1. Choisis la donnée à écouter</h2>
          <p>Type détecté : <strong>{scan.contentType}</strong>. Clique sur le champ qui contient la révélation.</p>

          <div className="path-list">
            {scan.paths.map(item => (
              <button
                key={item.path}
                className={`path-button ${selectedPath === item.path ? 'selected' : ''}`}
                onClick={() => setSelectedPath(item.path)}
              >
                <strong>{item.path}</strong><br />
                <span>{item.preview}</span>
              </button>
            ))}
          </div>

          <label>Aperçu brut</label>
          <pre className="code">{scan.rawPreview}</pre>
        </section>
      )}

      {scan && (
        <section className="card" style={{ marginTop: 24 }}>
          <h2>2. Réglages image Android</h2>
          <div className="row">
            <div>
              <label>Largeur image</label>
              <input type="number" value={width} onChange={e => setWidth(Number(e.target.value))} />
            </div>
            <div>
              <label>Hauteur image</label>
              <input type="number" value={height} onChange={e => setHeight(Number(e.target.value))} />
            </div>
          </div>

          <div className="row">
            <div>
              <label>Langue Wikipédia principale</label>
              <input value={wikiLang} onChange={e => setWikiLang(e.target.value)} placeholder="fr" />
            </div>
            <div>
              <label>Fallback Wikipédia</label>
              <input value={fallbackLang} onChange={e => setFallbackLang(e.target.value)} placeholder="en" />
            </div>
          </div>

          <label>Template</label>
          <select value={template} onChange={e => setTemplate(e.target.value as 'wiki-photo' | 'text-only')}>
            <option value="wiki-photo">Photo Wikipédia + texte</option>
            <option value="text-only">Texte seul</option>
          </select>

          <div className="actions">
            <button onClick={createConfig} disabled={loading || !selectedPath}>Créer la configuration Android</button>
          </div>
        </section>
      )}

      {config && (
        <section className="card" style={{ marginTop: 24 }}>
          <h2>3. Configuration Android</h2>
          <p>Copie le token dans l’app Android. Ne partage pas ce token publiquement.</p>

          <label>Backend base URL</label>
          <pre className="code">{new URL(config.valueUrl).origin}</pre>
          <button className="secondary" onClick={() => copy(new URL(config.valueUrl).origin)}>Copier le domaine</button>

          <label>Token</label>
          <pre className="code">{config.token}</pre>
          <button className="secondary" onClick={() => copy(config.token)}>Copier le token</button>

          <label>URL valeur</label>
          <pre className="code">{config.valueUrl}</pre>

          <label>URL wallpaper</label>
          <pre className="code">{config.wallpaperUrl}</pre>
          <div className="actions">
            <a href={config.wallpaperUrl} target="_blank" rel="noreferrer"><button>Voir le wallpaper</button></a>
            <button className="secondary" onClick={() => copy(config.wallpaperUrl)}>Copier l’URL wallpaper</button>
          </div>
        </section>
      )}
    </main>
  );
}
