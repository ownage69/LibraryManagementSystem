import { ArrowLeft, BookOpen, Building2, MapPinned, PencilLine } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import { EmptyState } from '../../components/common/EmptyState';
import { ErrorState } from '../../components/common/ErrorState';
import { LoadingState } from '../../components/common/LoadingState';
import { PageHeader } from '../../components/common/PageHeader';
import { SurfaceCard } from '../../components/common/SurfaceCard';
import { usePermissions } from '../../hooks/usePermissions';
import { useAsyncValue } from '../../hooks/useAsyncValue';
import { bookService, publisherService } from '../../services/libraryService';
import { getBooksForPublisher } from '../../utils/library';

export function PublisherDetailsPage() {
  const { canManageLibrary } = usePermissions();
  const id = Number(useParams().id);
  const { data, loading, error, reload } = useAsyncValue(async () => {
    const [publisher, books] = await Promise.all([publisherService.getById(id), bookService.list()]);
    return { publisher, books };
  }, [id]);

  if (loading) {
    return <LoadingState title="Loading publisher details..." />;
  }

  if (error || !data) {
    return <ErrorState description={error || 'Publisher details could not be loaded.'} onRetry={reload} />;
  }

  const relatedBooks = getBooksForPublisher(data.books, data.publisher.id);

  return (
    <div className="page-layout">
      <PageHeader
        breadcrumbs={[
          { label: 'Publishers', to: '/publishers' },
          { label: data.publisher.name },
        ]}
        eyebrow="Publisher details"
        title={data.publisher.name}
        description="Review this publisher's country and the books currently tied to it."
        actions={
          <div className="button-row">
            <Link to="/publishers" className="button button--ghost">
              <ArrowLeft size={16} />
              Back
            </Link>
            {canManageLibrary ? (
              <Link
                to={`/publishers/${data.publisher.id}/edit`}
                className="button button--primary button--icon-only"
                aria-label={`Edit ${data.publisher.name}`}
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
            <Building2 size={18} />
          </div>
          <h3>{data.publisher.name}</h3>
          <p className="entity-card__summary entity-card__summary--inline">
            <MapPinned size={14} />
            {data.publisher.country}
          </p>
          <p>{relatedBooks.length} books are currently linked to this publisher.</p>
        </SurfaceCard>

        <SurfaceCard
          header={
            <div>
              <p className="section-eyebrow">Related books</p>
              <h3 className="section-title">Publisher catalog presence</h3>
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
                    <p>{book.authorNames.join(', ')}</p>
                  </div>
                </Link>
              ))}
            </div>
          ) : (
            <EmptyState
              icon={BookOpen}
              title="No books linked yet"
              description="Once books reference this publisher, they will appear here."
            />
          )}
        </SurfaceCard>
      </div>
    </div>
  );
}
