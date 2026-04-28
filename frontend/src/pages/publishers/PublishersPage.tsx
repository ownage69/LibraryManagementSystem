import { Building2, MapPinned, PencilLine, Plus, Search, Trash2 } from 'lucide-react';
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
import { bookService, publisherService } from '../../services/libraryService';
import type { Publisher } from '../../types/entities';
import { getErrorMessage } from '../../utils/errors';
import { countBooksForPublisher, getBooksForPublisher, matchesText } from '../../utils/library';
import { paginateItems } from '../../utils/pagination';

export function PublishersPage() {
  const { canManageLibrary } = usePermissions();
  const { data, loading, error, reload } = useAsyncValue(async () => {
    const [publishers, books] = await Promise.all([publisherService.list(), bookService.list()]);
    return { publishers, books };
  }, []);
  const [query, setQuery] = useState('');
  const [publisherToDelete, setPublisherToDelete] = useState<Publisher | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  const debouncedQuery = useDebouncedValue(query);

  useEffect(() => {
    setPage(1);
  }, [debouncedQuery]);

  if (loading) {
    return <LoadingState title="Loading publishers..." />;
  }

  if (error || !data) {
    return <ErrorState description={error || 'Publisher records could not be loaded.'} onRetry={reload} />;
  }

  const filteredPublishers = data.publishers.filter(
    (publisher) =>
      matchesText(publisher.name, debouncedQuery) || matchesText(publisher.country, debouncedQuery),
  );
  const pagination = paginateItems(filteredPublishers, page, runtimeConfig.pagination.pageSize);

  async function handleDelete() {
    if (!publisherToDelete) {
      return;
    }

    try {
      await publisherService.remove(publisherToDelete.id);
      setFeedback(`Publisher "${publisherToDelete.name}" was removed.`);
      setPublisherToDelete(null);
      reload();
    } catch (deleteError) {
      setFeedback(getErrorMessage(deleteError));
    }
  }

  return (
    <div className="page-layout">
      <PageHeader
        eyebrow="Publishers"
        title="Publishers"
        description="Browse publishers and the books linked to each record."
        actions={
          canManageLibrary ? (
            <Link to="/publishers/new" className="button button--primary">
              <Plus size={16} />
              Add publisher
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
              placeholder="Search by name or country"
              onChange={(event) => setQuery(event.target.value)}
            />
          </div>

          <div className="toolbar-note">
            <strong>{filteredPublishers.length}</strong>
            <span>publishers found</span>
          </div>
        </div>
      </SurfaceCard>

      {feedback ? <AlertBanner title="Publisher update" description={feedback} /> : null}

      {filteredPublishers.length ? (
        <>
          <div className="entity-grid">
            {pagination.items.map((publisher) => {
              const relatedBooks = getBooksForPublisher(data.books, publisher.id);

              return (
                <SurfaceCard key={publisher.id} className="entity-card">
                  <div className="entity-card__header">
                    <div className="entity-card__icon">
                      <Building2 size={18} />
                    </div>
                    <div>
                      <h3>{publisher.name}</h3>
                      <p>{countBooksForPublisher(data.books, publisher)} books linked</p>
                    </div>
                  </div>

                  <p className="entity-card__summary entity-card__summary--inline">
                    <MapPinned size={14} />
                    {publisher.country}
                  </p>

                  <div className="entity-card__summary">
                    {relatedBooks.length
                      ? relatedBooks.slice(0, 3).map((book) => book.title).join(', ')
                      : 'No books linked yet.'}
                  </div>

                  <div className="card-actions">
                    <Link to={`/publishers/${publisher.id}`} className="button button--secondary">
                      Details
                    </Link>
                    {canManageLibrary ? (
                      <>
                        <Link
                          to={`/publishers/${publisher.id}/edit`}
                          className="button button--ghost button--icon-only"
                          aria-label={`Edit ${publisher.name}`}
                          title="Edit"
                        >
                          <PencilLine size={16} />
                        </Link>
                        <button
                          type="button"
                          className="button button--ghost-danger button--icon-only"
                          onClick={() => setPublisherToDelete(publisher)}
                          aria-label={`Delete ${publisher.name}`}
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
          icon={Building2}
          title="No publishers found"
          description={
            canManageLibrary
              ? 'Try another search term or add a publisher record.'
              : 'Try another search term and browse a different publisher profile.'
          }
          action={
            canManageLibrary ? (
              <Link to="/publishers/new" className="button button--primary">
                Add publisher
              </Link>
            ) : null
          }
        />
      )}

      <ConfirmDialog
        open={Boolean(publisherToDelete)}
        title="Delete publisher"
        description="Deleting a publisher can affect book references across the catalog."
        confirmLabel="Delete publisher"
        onCancel={() => setPublisherToDelete(null)}
        onConfirm={handleDelete}
      />
    </div>
  );
}
