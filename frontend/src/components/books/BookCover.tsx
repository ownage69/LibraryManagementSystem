import { useEffect, useMemo, useState } from 'react';

const coverTones = ['forest', 'burgundy', 'ocean', 'gold'] as const;
const staticCoverExtensions = ['jpg', 'jpeg', 'webp', 'png'] as const;

function resolveTone(title: string) {
  const index = [...title].reduce((sum, character) => sum + character.charCodeAt(0), 0);
  return coverTones[index % coverTones.length];
}

function resolveStaticCoverUrl(title: string, extensionIndex: number) {
  const normalizedTitle = title.trim();

  if (!normalizedTitle || extensionIndex >= staticCoverExtensions.length) {
    return null;
  }

  return `/book-covers/${encodeURIComponent(normalizedTitle)}.${staticCoverExtensions[extensionIndex]}`;
}

interface BookCoverProps {
  title: string;
  coverUrl?: string | null;
}

export function BookCover({ title, coverUrl }: BookCoverProps) {
  const [staticExtensionIndex, setStaticExtensionIndex] = useState(0);
  const [storedCoverFailed, setStoredCoverFailed] = useState(false);
  const staticCoverUrl = useMemo(
    () => resolveStaticCoverUrl(title, staticExtensionIndex),
    [staticExtensionIndex, title],
  );
  const imageUrl = storedCoverFailed ? staticCoverUrl : coverUrl || staticCoverUrl;
  const initials = title
    .split(' ')
    .slice(0, 2)
    .map((chunk) => chunk[0] || '')
    .join('')
    .toUpperCase();

  useEffect(() => {
    setStaticExtensionIndex(0);
    setStoredCoverFailed(false);
  }, [coverUrl, title]);

  if (imageUrl) {
    return (
      <div className="book-cover book-cover--image">
        <img
          src={imageUrl}
          alt={`${title} cover`}
          className="book-cover__image"
          onError={() => {
            if (coverUrl && !storedCoverFailed) {
              setStoredCoverFailed(true);
              return;
            }

            setStaticExtensionIndex((currentIndex) =>
              Math.min(currentIndex + 1, staticCoverExtensions.length),
            );
          }}
        />
      </div>
    );
  }

  return (
    <div className={`book-cover book-cover--${resolveTone(title)}`}>
      <small>Catalog edition</small>
      <strong>{initials}</strong>
      <span>{title}</span>
    </div>
  );
}
