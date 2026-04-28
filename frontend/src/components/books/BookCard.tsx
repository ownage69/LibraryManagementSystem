import { PencilLine, Trash2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import { StatusBadge } from '../common/StatusBadge';
import { useStoredBookCover } from '../../hooks/useStoredBookCover';
import type { Book } from '../../types/entities';
import { BookCover } from './BookCover';

interface BookCardProps {
  book: Book;
  availability: 'available' | 'loaned';
  onDelete?: (book: Book) => void;
  basePath?: string;
  returnTo?: string;
  canManage?: boolean;
}

export function BookCard({
  book,
  availability,
  onDelete,
  basePath = '/books',
  returnTo,
  canManage = false,
}: BookCardProps) {
  const coverUrl = useStoredBookCover(book.id);
  const totalCopies = book.totalCopies || 3;
  const availableCopies =
    typeof book.availableCopies === 'number'
      ? book.availableCopies
      : availability === 'available'
        ? 1
        : 0;
  const loanedCopies = Math.max(totalCopies - availableCopies, 0);
  const detailsPath = `${basePath}/${book.id}${returnTo ? `?from=${encodeURIComponent(returnTo)}` : ''}`;
  const editPath = `${basePath}/${book.id}/edit${returnTo ? `?from=${encodeURIComponent(returnTo)}` : ''}`;

  return (
    <article className="book-card">
      <BookCover title={book.title} coverUrl={coverUrl} />

      <div className="book-card__body">
        <div className="book-card__heading">
          <p className="book-card__eyebrow">{book.publisherName || 'Publisher pending'}</p>
          <StatusBadge
            label={availability === 'available' ? 'Available' : 'On loan'}
            tone={availability === 'available' ? 'success' : 'warning'}
          />
        </div>

        <h3>{book.title}</h3>

        <p className="book-card__meta">{book.authorNames.join(', ')} • {book.publishYear || 'Year not set'}</p>

        <p className="book-card__copies">
          {availableCopies} of {totalCopies} copies available
          {loanedCopies ? ` • ${loanedCopies} on loan` : ''}
        </p>

        <div className="tag-list">
          {book.categoryNames.map((category) => (
            <span key={category} className="tag">
              {category}
            </span>
          ))}
        </div>

        <p className="book-card__description">
          {book.description?.trim() || 'No description yet. Add a short summary to enrich the catalog.'}
        </p>

        <div className="card-actions">
          <Link to={detailsPath} className="button button--secondary">
            Details
          </Link>
          {canManage ? (
            <>
              <Link
                to={editPath}
                className="button button--ghost button--icon-only"
                aria-label={`Edit ${book.title}`}
                title="Edit"
              >
                <PencilLine size={16} />
              </Link>
              <button
                type="button"
                className="button button--ghost-danger button--icon-only"
                onClick={() => onDelete?.(book)}
                aria-label={`Delete ${book.title}`}
                title="Delete"
              >
                <Trash2 size={16} />
              </button>
            </>
          ) : null}
        </div>
      </div>
    </article>
  );
}
