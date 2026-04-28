import { ArrowLeft, BookOpen, Bookmark, PencilLine } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import { EmptyState } from '../../components/common/EmptyState';
import { ErrorState } from '../../components/common/ErrorState';
import { LoadingState } from '../../components/common/LoadingState';
import { PageHeader } from '../../components/common/PageHeader';
import { SurfaceCard } from '../../components/common/SurfaceCard';
import { usePermissions } from '../../hooks/usePermissions';
import { useAsyncValue } from '../../hooks/useAsyncValue';
import { bookService, categoryService } from '../../services/libraryService';
import { getBooksForCategory } from '../../utils/library';

export function CategoryDetailsPage() {
  const { canManageLibrary } = usePermissions();
  const id = Number(useParams().id);
  const { data, loading, error, reload } = useAsyncValue(async () => {
    const [category, books] = await Promise.all([categoryService.getById(id), bookService.list()]);
    return { category, books };
  }, [id]);

  if (loading) {
    return <LoadingState title="Loading category details..." />;
  }

  if (error || !data) {
    return <ErrorState description={error || 'Category details could not be loaded.'} onRetry={reload} />;
  }

  const relatedBooks = getBooksForCategory(data.books, data.category.id);
  const authorCount = new Set(relatedBooks.flatMap((book) => book.authorNames)).size;

  return (
    <div className="page-layout">
      <PageHeader
        breadcrumbs={[
          { label: 'Categories', to: '/categories' },
          { label: data.category.name },
        ]}
        eyebrow="Category details"
        title={data.category.name}
        actions={
          <div className="button-row">
            <Link to="/categories" className="button button--ghost">
              <ArrowLeft size={16} />
              Back
            </Link>
            {canManageLibrary ? (
              <Link
                to={`/categories/${data.category.id}/edit`}
                className="button button--primary button--icon-only"
                aria-label={`Edit ${data.category.name}`}
                title="Edit"
              >
                <PencilLine size={16} />
              </Link>
            ) : null}
          </div>
        }
      />

      <div className="category-detail-layout">
        <SurfaceCard className="category-profile-card">
          <div className="category-profile-card__mast">
            <div className="category-profile-card__icon">
              <Bookmark size={22} />
            </div>
            <div>
              <p className="section-eyebrow">Shelf subject</p>
              <h3>{data.category.name}</h3>
            </div>
          </div>

          <div className="category-profile-card__stats">
            <div className="category-stat">
              <strong>{relatedBooks.length}</strong>
              <span>titles</span>
            </div>
            <div className="category-stat">
              <strong>{authorCount}</strong>
              <span>authors</span>
            </div>
          </div>
        </SurfaceCard>

        <SurfaceCard
          className="category-books-card"
          header={
            <div className="category-books-card__header">
              <div>
                <p className="section-eyebrow">Related books</p>
                <h3 className="section-title">Titles grouped under this category</h3>
              </div>
              <span className="status-badge status-badge--neutral">{relatedBooks.length} total</span>
            </div>
          }
        >
          {relatedBooks.length ? (
            <div className="category-book-list">
              {relatedBooks.map((book) => (
                <Link key={book.id} to={`/books/${book.id}`} className="category-book-item">
                  <span className="category-book-item__icon">
                    <BookOpen size={16} />
                  </span>
                  <span className="category-book-item__copy">
                    <strong>{book.title}</strong>
                    <span>{book.authorNames.join(', ')}</span>
                  </span>
                </Link>
              ))}
            </div>
          ) : (
            <EmptyState
              icon={BookOpen}
              title="No books in this category"
              description="Books assigned to this category will appear here."
            />
          )}
        </SurfaceCard>
      </div>
    </div>
  );
}
