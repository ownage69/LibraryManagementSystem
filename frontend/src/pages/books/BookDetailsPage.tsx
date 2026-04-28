import { ArrowLeft, BookOpen, CalendarDays, Library, PencilLine, Users } from 'lucide-react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { BookCover } from '../../components/books/BookCover';
import { EmptyState } from '../../components/common/EmptyState';
import { ErrorState } from '../../components/common/ErrorState';
import { LoadingState } from '../../components/common/LoadingState';
import { PageHeader } from '../../components/common/PageHeader';
import { StatusBadge } from '../../components/common/StatusBadge';
import { SurfaceCard } from '../../components/common/SurfaceCard';
import { useAsyncValue } from '../../hooks/useAsyncValue';
import { usePermissions } from '../../hooks/usePermissions';
import { useStoredBookCover } from '../../hooks/useStoredBookCover';
import {
  authorService,
  bookService,
  categoryService,
  loanService,
  publisherService,
} from '../../services/libraryService';
import { findById, getAvailableCopies, getBookAvailability } from '../../utils/library';

interface BookDetailsPageProps {
  variant?: 'app' | 'public';
}

export function BookDetailsPage({ variant = 'app' }: BookDetailsPageProps) {
  const { canManageLibrary } = usePermissions();
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);
  const fromParam = searchParams.get('from');
  const id = Number(useParams().id);
  const { data, loading, error, reload } = useAsyncValue(async () => {
    const [book, authors, categories, publishers, loans] = await Promise.all([
      bookService.getById(id),
      authorService.list(),
      categoryService.list(),
      publisherService.list(),
      loanService.list(),
    ]);

    return { book, authors, categories, publishers, loans };
  }, [id]);
  const coverUrl = useStoredBookCover(data?.book.id ?? null);

  if (loading) {
    return <LoadingState title="Loading book details..." />;
  }

  if (error || !data) {
    return <ErrorState description={error || 'Book details could not be loaded.'} onRetry={reload} />;
  }

  const availability = getBookAvailability(data.book, data.loans);
  const availableCopies = getAvailableCopies(data.book, data.loans);
  const totalCopies = data.book.totalCopies || 3;
  const loanedCopies = Math.max(totalCopies - availableCopies, 0);
  const publisher = findById(data.publishers, data.book.publisherId);
  const linkedAuthors = data.authors.filter((author) => data.book.authorIds.includes(author.id));
  const linkedCategories = data.categories.filter((category) =>
    data.book.categoryIds.includes(category.id),
  );
  const listPath = variant === 'public' ? '/catalog' : '/books';
  const returnTo = fromParam || listPath;
  const editPath = `/books/${data.book.id}/edit${fromParam ? `?from=${encodeURIComponent(fromParam)}` : ''}`;

  return (
    <div className="page-layout">
      <PageHeader
        breadcrumbs={[
          { label: variant === 'public' ? 'Catalog' : 'Books', to: returnTo },
          { label: data.book.title },
        ]}
        eyebrow={variant === 'public' ? 'Catalog details' : 'Book details'}
        title={data.book.title}
        description="Book details, related records, and current copy availability."
        actions={
          <div className="button-row">
            <Link to={returnTo} className="button button--ghost">
              <ArrowLeft size={16} />
              Back
            </Link>
            {canManageLibrary && variant !== 'public' ? (
              <Link
                to={editPath}
                className="button button--primary button--icon-only"
                aria-label={`Edit ${data.book.title}`}
                title="Edit"
              >
                <PencilLine size={16} />
              </Link>
            ) : null}
          </div>
        }
      />

      <div className="split-layout">
        <SurfaceCard className="book-detail-card">
          <div className="book-detail-card__cover">
            <BookCover title={data.book.title} coverUrl={coverUrl} />
          </div>

          <div className="book-detail-card__content">
            <div className="book-detail-card__header">
              <div>
                <p className="section-eyebrow">
                  {publisher?.name || data.book.publisherName || 'Publisher pending'}
                </p>
                <h3>{data.book.title}</h3>
              </div>
              <StatusBadge
                label={availability === 'available' ? 'Available' : 'On loan'}
                tone={availability === 'available' ? 'success' : 'warning'}
              />
            </div>

            <p className="book-detail-card__description">
              {data.book.description?.trim() ||
                'No description yet. Add a summary to make the catalog more informative.'}
            </p>

            <div className="detail-grid">
              <div className="detail-item">
                <BookOpen size={16} />
                <div>
                  <small>Available now</small>
                  <strong>{availableCopies}</strong>
                </div>
              </div>

              <div className="detail-item">
                <Library size={16} />
                <div>
                  <small>Total copies</small>
                  <strong>{totalCopies}</strong>
                </div>
              </div>

              <div className="detail-item">
                <CalendarDays size={16} />
                <div>
                  <small>Publication year</small>
                  <strong>{data.book.publishYear || 'Not specified'}</strong>
                </div>
              </div>

              <div className="detail-item">
                <Library size={16} />
                <div>
                  <small>Publisher</small>
                  <strong>{publisher?.name || data.book.publisherName || 'Not specified'}</strong>
                </div>
              </div>

              <div className="detail-item">
                <Users size={16} />
                <div>
                  <small>Authors</small>
                  <strong>{data.book.authorNames.join(', ')}</strong>
                </div>
              </div>
            </div>

            <div className="inline-note">
              {availability === 'available'
                ? `${availableCopies} of ${totalCopies} copies can be borrowed right now.`
                : `All ${totalCopies} copies are currently on loan.`}
              {loanedCopies && availability === 'available'
                ? ` ${loanedCopies} copies are already borrowed.`
                : ''}
            </div>
          </div>
        </SurfaceCard>

        <SurfaceCard
          header={
            <div>
              <p className="section-eyebrow">Relationships</p>
              <h3 className="section-title">Connected entities</h3>
            </div>
          }
        >
          <div className="stacked-links">
            <div>
              <small className="section-label">Authors</small>
              <div className="link-chip-row">
                {linkedAuthors.map((author) =>
                  variant === 'public' ? (
                    <span key={author.id} className="link-chip">
                      {author.firstName} {author.lastName}
                    </span>
                  ) : (
                    <Link key={author.id} to={`/authors/${author.id}`} className="link-chip">
                      {author.firstName} {author.lastName}
                    </Link>
                  ),
                )}
              </div>
            </div>

            <div>
              <small className="section-label">Categories</small>
              <div className="link-chip-row">
                {linkedCategories.map((category) =>
                  variant === 'public' ? (
                    <span key={category.id} className="link-chip">
                      {category.name}
                    </span>
                  ) : (
                    <Link key={category.id} to={`/categories/${category.id}`} className="link-chip">
                      {category.name}
                    </Link>
                  ),
                )}
              </div>
            </div>

            <div>
              <small className="section-label">Publisher</small>
              {publisher ? (
                variant === 'public' ? (
                  <span className="link-chip">{publisher.name}</span>
                ) : (
                  <Link to={`/publishers/${publisher.id}`} className="link-chip">
                    {publisher.name}
                  </Link>
                )
              ) : (
                <span className="link-chip link-chip--muted">Publisher details unavailable</span>
              )}
            </div>
          </div>
        </SurfaceCard>
      </div>

      {linkedCategories.length ? null : (
        <EmptyState
          icon={BookOpen}
          title="No categories linked"
          description="Assign at least one category so the book becomes easier to browse."
        />
      )}
    </div>
  );
}
