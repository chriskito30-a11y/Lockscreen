import './globals.css';
import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Magic Lockscreen Android',
  description: 'Dashboard pour générer un fond d’écran Android dynamique depuis Inject / WikiTest.'
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="fr">
      <body>{children}</body>
    </html>
  );
}
