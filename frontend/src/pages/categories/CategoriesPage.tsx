import { Bookmark, PencilLine, Plus, Search, Trash2 } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { AlertBanner } from '../../components/common/AlertBanner';
import { ConfirmDialog } from '../../components/common/ConfirmDialog';
import { EmptyState } from '../../components/common/EmptyState';
import { ErrorState } from '../../components/common/ErrorState';
import { LoadingState } from '../../components/common/LoadingState';
import { PageHeader } from '../../components/common/PageHeader';
import { PaginationControls } from '../../components/common/PaginationControls';
import { SurfaceCard } from '../../components/common/SurfaceCard';
import { TextInput } from '../../components/forms/TextInput';
import { runtimeConfig } from '../../config/runtime';
import { useAsyncValue } from '../../hooks/useAsyncValue';
import { useDebouncedValue } from '../../hooks/useDebouncedValue';
import { usePermissions } from '../../hooks/usePermissions';
import { bookService, categoryService } from '../../services/libraryService';
import type { Category } from '../../types/entities';
import { getErrorMessage } from '../../utils/errors';
import { countBooksForCategory, getBooksForCategory, matchesText } from '../../utils/library';
import { paginateItems } from '../../utils/pagination';

export function CategoriesPage() {
  const { canManageLibrary } = usePermissions();
  const { data, loading, error, reload } = useAsyncValue(async () => {
    const [categories, books] = await Promise.all([categoryService.list(), bookService.list()]);
    return { categories, books };
  }, []);
  const [query, setQuery] = useState('');
  const [categoryToDelete, setCategoryToDelete] = useState<Category | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  const debouncedQuery = useDebouncedValue(query);

  useEffect(() => {
    setPage(1);
  }, [debouncedQuery]);

  if (loading) {
    return <LoadingState title="Loading categories..." />;
  }

  if (error || !data) {
    return <ErrorState description={error || 'Category records could not be loaded.'} onRetry={reload} />;
  }

  const filteredCategories = data.categories.filter((category) =>
    matchesText(category.name, debouncedQuery),
  );
  const pagination = paginateItems(filteredCategories, page, runtimeConfig.pagination.pageSize);

  async function handleDelete() {
    if (!categoryToDelete) {
      return;
    }

    try {
      await categoryService.remove(categoryToDelete.id);
      setFeedback(`Category "${categoryToDelete.name}" was removed.`);
      setCategoryToDelete(null);
      reload();
    } catch (deleteError) {
      setFeedback(getErrorMessage(deleteError));
    }
  }

  return (
    <div className="page-layout">
      <PageHeader
        eyebrow="Categories"
        title="Categories"
        description="Browse shelf categories and the books assigned to them."
        actions={
          canManageLibrary ? (
            <Link to="/categories/new" className="button button--primary">
              <Plus size={16} />
              Add category
            </Link>
          ) : null
        }
      />

      <SurfaceCard className="toolbar-card">
        <div className="toolbar-grid">
          <div className="search-field">
            <Search size={16} />
            <TextInput
              type="search"
              value={query}
              placeholder="Search by category name"
              onChange={(event) => setQuery(event.target.value)}
            />
          </div>

          <div className="toolbar-note">
            <strong>{filteredCategories.length}</strong>
            <span>categories found</span>
          </div>
        </div>
      </SurfaceCard>

      {feedback ? <AlertBanner title="Category update" description={feedback} /> : null}

      {filteredCategories.length ? (
        <>
          <div className="entity-grid">
            {pagination.items.map((category) => {
              const relatedBooks = getBooksForCategory(data.books, category.id);

              return (
                <SurfaceCard key={category.id} className="entity-card">
                  <div className="entity-card__header">
                    <div className="entity-card__icon">
                      <Bookmark size={18} />
                    </div>
                    <div>
                      <h3>{category.name}</h3>
                      <p>{countBooksForCategory(data.books, category)} books assigned</p>
                    </div>
                  </div>

                  <p className="entity-card__summary">
                    {relatedBooks.length
                      ? relatedBooks.slice(0, 3).map((book) => book.title).join(', ')
                      : 'No books assigned yet.'}
                  </p>

                  <div className="card-actions">
                    <Link to={`/categories/${category.id}`} className="button button--secondary">
                      Details
                    </Link>
                    {canManageLibrary ? (
                      <>
                        <Link
                          to={`/categories/${category.id}/edit`}
                          className="button button--ghost button--icon-only"
                          aria-label={`Edit ${category.name}`}
                          title="Edit"
                        >
                          <PencilLine size={16} />
                        </Link>
                        <button
                          type="button"
                          className="button button--ghost-danger button--icon-only"
                          onClick={() => setCategoryToDelete(category)}
                          aria-label={`Delete ${category.name}`}
                          title="Delete"
                        >
                          <Trash2 size={16} />
                        </button>
                      </>
                    ) : null}
                  </div>
                </SurfaceCard>
              );
            })}
          </div>

          <PaginationControls pagination={pagination} onPageChange={setPage} />
        </>
      ) : (
        <EmptyState
          icon={Bookmark}
          title="No categories found"
          description={
            canManageLibrary
              ? 'Try another search term or create a category for your catalog.'
              : 'Try another search term and return to the taxonomy later.'
          }
          action={
            canManageLibrary ? (
              <Link to="/categories/new" className="button button--primary">
                Add category
              </Link>
            ) : null
          }
        />
      )}

      <ConfirmDialog
        open={Boolean(categoryToDelete)}
        title="Delete category"
        description="Deleting a category can affect how books are organized across the catalog."
        confirmLabel="Delete category"
        onCancel={() => setCategoryToDelete(null)}
        onConfirm={handleDelete}
      />
    </div>
  );
}
