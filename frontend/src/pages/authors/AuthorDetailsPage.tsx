import { ArrowLeft, BookOpen, PencilLine, UserRound } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import { EmptyState } from '../../components/common/EmptyState';
import { ErrorState } from '../../components/common/ErrorState';
import { LoadingState } from '../../components/common/LoadingState';
import { PageHeader } from '../../components/common/PageHeader';
import { SurfaceCard } from '../../components/common/SurfaceCard';
import { usePermissions } from '../../hooks/usePermissions';
import { useAsyncValue } from '../../hooks/useAsyncValue';
import { authorService, bookService } from '../../services/libraryService';
import { getAuthorFullName } from '../../utils/format';
import { getBooksForAuthor } from '../../utils/library';

export function AuthorDetailsPage() {
  const { canManageLibrary } = usePermissions();
  const id = Number(useParams().id);
  const { data, loading, error, reload } = useAsyncValue(async () => {
    const [author, books] = await Promise.all([authorService.getById(id), bookService.list()]);
    return { author, books };
  }, [id]);

  if (loading) {
    return <LoadingState title="Loading author profile..." />;
  }

  if (error || !data) {
    return <ErrorState description={error || 'Author details could not be loaded.'} onRetry={reload} />;
  }

  const relatedBooks = getBooksForAuthor(data.books, data.author.id);

  return (
    <div className="page-layout">
      <PageHeader
        breadcrumbs={[
          { label: 'Authors', to: '/authors' },
          { label: getAuthorFullName(data.author) },
        ]}
        eyebrow="Author details"
        title={getAuthorFullName(data.author)}
        description="View this author's profile and the books currently connected to it."
        actions={
          <div className="button-row">
            <Link to="/authors" className="button button--ghost">
              <ArrowLeft size={16} />
              Back
            </Link>
            {canManageLibrary ? (
              <Link
                to={`/authors/${data.author.id}/edit`}
                className="button button--primary button--icon-only"
                aria-label={`Edit ${getAuthorFullName(data.author)}`}
                title="Edit"
              >
                <PencilLine size={16} />
              </Link>
            ) : null}
          </div>
        }
      />

      <div className="split-layout">
        <SurfaceCard className="profile-card">
          <div className="entity-card__icon">
            <UserRound size={18} />
          </div>
          <h3>{getAuthorFullName(data.author)}</h3>
          <p>This record is currently linked to {relatedBooks.length} books in the catalog.</p>
        </SurfaceCard>

        <SurfaceCard
          header={
            <div>
              <p className="section-eyebrow">Related books</p>
              <h3 className="section-title">Author bibliography in the system</h3>
            </div>
          }
        >
          {relatedBooks.length ? (
            <div className="simple-list">
              {relatedBooks.map((book) => (
                <Link key={book.id} to={`/books/${book.id}`} className="simple-list__item">
                  <div className="simple-list__icon">
                    <BookOpen size={16} />
                  </div>
                  <div>
                    <strong>{book.title}</strong>
                    <p>
                      {book.publisherName || 'Publisher pending'} • {book.publishYear || 'Year not set'}
                    </p>
                  </div>
                </Link>
              ))}
            </div>
          ) : (
            <EmptyState
              icon={BookOpen}
              title="No books linked yet"
              description="Once books reference this author, they will appear here."
            />
          )}
        </SurfaceCard>
      </div>
    </div>
  );
}
