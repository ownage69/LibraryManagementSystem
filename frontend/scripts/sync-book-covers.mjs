import { copyFile, mkdir, readdir, rm } from 'node:fs/promises';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDirectory = dirname(fileURLToPath(import.meta.url));
const frontendDirectory = resolve(scriptDirectory, '..');
const defaultSourceDirectory = resolve(frontendDirectory, '..', 'booksimages');
const sourceDirectory = resolve(process.env.BOOK_COVERS_SOURCE_DIR || defaultSourceDirectory);
const destinationDirectory = resolve(frontendDirectory, 'public', 'book-covers');
const supportedExtensions = new Set(['.jpg', '.jpeg', '.png', '.webp', '.gif']);

async function copyBookCovers() {
  let entries;

  try {
    entries = await readdir(sourceDirectory, { withFileTypes: true });
  } catch (error) {
    if (error?.code === 'ENOENT') {
      console.warn(`Book cover source directory was not found: ${sourceDirectory}`);
      return;
    }

    throw error;
  }

  await rm(destinationDirectory, { recursive: true, force: true });
  await mkdir(destinationDirectory, { recursive: true });

  const files = entries.filter((entry) => {
    const extension = entry.name.slice(entry.name.lastIndexOf('.')).toLowerCase();
    return entry.isFile() && supportedExtensions.has(extension);
  });

  await Promise.all(
    files.map((file) =>
      copyFile(join(sourceDirectory, file.name), join(destinationDirectory, file.name)),
    ),
  );

  console.log(`Copied ${files.length} book cover image(s).`);
}

await copyBookCovers();
