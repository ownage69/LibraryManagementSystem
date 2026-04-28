import { PencilLine, Plus, Search, Trash2, UserRound } from 'lucide-react';
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
import { authorService, bookService } from '../../services/libraryService';
import type { Author } from '../../types/entities';
import { getAuthorFullName } from '../../utils/format';
import { getErrorMessage } from '../../utils/errors';
import { countBooksForAuthor, getBooksForAuthor, matchesText } from '../../utils/library';
import { paginateItems } from '../../utils/pagination';

function getAuthorInitials(author: Author) {
  return `${author.firstName.charAt(0)}${author.lastName.charAt(0)}`.toUpperCase();
}

export function AuthorsPage() {
  const { canManageLibrary } = usePermissions();
  const { data, loading, error, reload } = useAsyncValue(async () => {
    const [authors, books] = await Promise.all([authorService.list(), bookService.list()]);
    return { authors, books };
  }, []);
  const [query, setQuery] = useState('');
  const [authorToDelete, setAuthorToDelete] = useState<Author | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  const debouncedQuery = useDebouncedValue(query);

  useEffect(() => {
    setPage(1);
  }, [debouncedQuery]);

  if (loading) {
    return <LoadingState title="Loading authors..." />;
  }

  if (error || !data) {
    return <ErrorState description={error || 'Author records could not be loaded.'} onRetry={reload} />;
  }

  const filteredAuthors = data.authors.filter((author) =>
    matchesText(getAuthorFullName(author), debouncedQuery),
  );
  const pagination = paginateItems(filteredAuthors, page, runtimeConfig.pagination.pageSize);

  async function handleDelete() {
    if (!authorToDelete) {
      return;
    }

    try {
      await authorService.remove(authorToDelete.id);
      setFeedback(`Author "${getAuthorFullName(authorToDelete)}" was removed.`);
      setAuthorToDelete(null);
      reload();
    } catch (deleteError) {
      setFeedback(getErrorMessage(deleteError));
    }
  }

  return (
    <div className="page-layout">
      <PageHeader
        eyebrow="Authors"
        title="Authors"
        description="Search author records and open related books."
        actions={
          canManageLibrary ? (
            <Link to="/authors/new" className="button button--primary">
              <Plus size={16} />
              Add author
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
              placeholder="Search by first or last name"
              onChange={(event) => setQuery(event.target.value)}
            />
          </div>

          <div className="toolbar-note">
            <strong>{filteredAuthors.length}</strong>
            <span>authors found</span>
          </div>
        </div>
      </SurfaceCard>

      {feedback ? <AlertBanner title="Author update" description={feedback} /> : null}

      {filteredAuthors.length ? (
        <>
          <div className="entity-grid">
            {pagination.items.map((author) => {
              const relatedBooks = getBooksForAuthor(data.books, author.id);

              return (
                <SurfaceCard key={author.id} className="entity-card">
                  <div className="entity-card__header">
                    <div className="entity-card__icon entity-card__icon--avatar">
                      <span className="entity-avatar">{getAuthorInitials(author)}</span>
                    </div>
                    <div>
                      <h3>{getAuthorFullName(author)}</h3>
                      <p>{countBooksForAuthor(data.books, author)} books linked</p>
                    </div>
                  </div>

                  <p className="entity-card__summary">
                    {relatedBooks.length
                      ? relatedBooks.slice(0, 3).map((book) => book.title).join(', ')
                      : 'No books linked yet.'}
                  </p>

                  <div className="card-actions">
                    <Link to={`/authors/${author.id}`} className="button button--secondary">
                      Details
                    </Link>
                    {canManageLibrary ? (
                      <>
                        <Link
                          to={`/authors/${author.id}/edit`}
                          className="button button--ghost button--icon-only"
                          aria-label={`Edit ${getAuthorFullName(author)}`}
                          title="Edit"
                        >
                          <PencilLine size={16} />
                        </Link>
                        <button
                          type="button"
                          className="button button--ghost-danger button--icon-only"
                          onClick={() => setAuthorToDelete(author)}
                          aria-label={`Delete ${getAuthorFullName(author)}`}
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
          icon={UserRound}
          title="No authors found"
          description={
            canManageLibrary
              ? 'Try a different search query or add a new author to the system.'
              : 'Try a different search query and open another author profile.'
          }
          action={
            canManageLibrary ? (
              <Link to="/authors/new" className="button button--primary">
                Add author
              </Link>
            ) : null
          }
        />
      )}

      <ConfirmDialog
        open={Boolean(authorToDelete)}
        title="Delete author"
        description="This action removes the author record. Make sure no important catalog links depend on it."
        confirmLabel="Delete author"
        onCancel={() => setAuthorToDelete(null)}
        onConfirm={handleDelete}
      />
    </div>
  );
}
