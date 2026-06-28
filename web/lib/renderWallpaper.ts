import sharp from 'sharp';
import type { MagicConfig, WikiImageResult } from './types';

export async function renderWallpaper(params: {
  config: MagicConfig;
  revelation: string;
  wiki: WikiImageResult;
  image?: Buffer;
}): Promise<Buffer> {
  const { config, image } = params;

  const width = config.width || 1080;
  const height = config.height || 2400;

  // Mode final : photo uniquement, plein écran, sans texte.
  if (image && image.length > 500) {
    return sharp(image)
      .resize(width, height, {
        fit: 'cover',
        position: 'center'
      })
      .png({
        quality: 95,
        compressionLevel: 8
      })
      .toBuffer();
  }

  // Si aucune photo Wikipédia n'est trouvée, on met un fond neutre.
  // Aucun texte volontairement.
  return sharp({
    create: {
      width,
      height,
      channels: 3,
      background: '#020617'
    }
  })
    .png({
      quality: 95,
      compressionLevel: 8
    })
    .toBuffer();
}
