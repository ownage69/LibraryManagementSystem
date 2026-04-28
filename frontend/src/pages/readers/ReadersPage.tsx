import { Mail, PencilLine, Plus, Search, Trash2, UserRound } from 'lucide-react';
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
import { loanService, readerService } from '../../services/libraryService';
import type { Reader } from '../../types/entities';
import { formatDate, getReaderFullName } from '../../utils/format';
import { getErrorMessage } from '../../utils/errors';
import { countActiveLoansForReader, getLoansForReader, matchesText } from '../../utils/library';
import { paginateItems } from '../../utils/pagination';

export function ReadersPage() {
  const { data, loading, error, reload } = useAsyncValue(async () => {
    const [readers, loans] = await Promise.all([readerService.list(), loanService.list()]);
    return { readers, loans };
  }, []);
  const [query, setQuery] = useState('');
  const [readerToDelete, setReaderToDelete] = useState<Reader | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  const debouncedQuery = useDebouncedValue(query);

  useEffect(() => {
    setPage(1);
  }, [debouncedQuery]);

  if (loading) {
    return <LoadingState title="Loading readers..." />;
  }

  if (error || !data) {
    return <ErrorState description={error || 'Reader records could not be loaded.'} onRetry={reload} />;
  }

  const filteredReaders = data.readers.filter((reader) => {
    const fullName = getReaderFullName(reader);
    return matchesText(fullName, debouncedQuery) || matchesText(reader.email, debouncedQuery);
  });
  const pagination = paginateItems(filteredReaders, page, runtimeConfig.pagination.pageSize);

  async function handleDelete() {
    if (!readerToDelete) {
      return;
    }

    try {
      await readerService.remove(readerToDelete.id);
      setFeedback(`Reader "${getReaderFullName(readerToDelete)}" was removed.`);
      setReaderToDelete(null);
      reload();
    } catch (deleteError) {
      setFeedback(getErrorMessage(deleteError));
    }
  }

  return (
    <div className="page-layout">
      <PageHeader
        eyebrow="Readers"
        title="Readers"
        description="Search reader records and review recent borrowing activity."
        actions={
          <Link to="/readers/new" className="button button--primary">
            <Plus size={16} />
            Add reader
          </Link>
        }
      />

      <SurfaceCard className="toolbar-card">
        <div className="toolbar-grid">
          <div className="search-field">
            <Search size={16} />
            <TextInput
              type="search"
              value={query}
              placeholder="Search by name or email"
              onChange={(event) => setQuery(event.target.value)}
            />
          </div>

          <div className="toolbar-note">
            <strong>{filteredReaders.length}</strong>
            <span>readers found</span>
          </div>
        </div>
      </SurfaceCard>

      {feedback ? <AlertBanner title="Reader update" description={feedback} /> : null}

      {filteredReaders.length ? (
        <>
          <div className="entity-grid">
            {pagination.items.map((reader) => {
              const readerLoans = getLoansForReader(data.loans, reader.id);
              const latestLoan = readerLoans[0];

              return (
                <SurfaceCard key={reader.id} className="entity-card">
                  <div className="entity-card__header">
                    <div className="entity-card__icon">
                      <UserRound size={18} />
                    </div>
                    <div>
                      <h3>{getReaderFullName(reader)}</h3>
                      <p>{countActiveLoansForReader(data.loans, reader)} active loans</p>
                    </div>
                  </div>

                  <p className="entity-card__summary entity-card__summary--inline">
                    <Mail size={14} />
                    {reader.email}
                  </p>

                  <p className="entity-card__summary">
                    {latestLoan
                      ? `Latest activity: ${latestLoan.bookTitle} on ${formatDate(latestLoan.loanDate)}`
                      : 'No loan history yet.'}
                  </p>

                  <div className="card-actions">
                    <Link to={`/readers/${reader.id}`} className="button button--secondary">
                      Details
                    </Link>
                    <Link
                      to={`/readers/${reader.id}/edit`}
                      className="button button--ghost button--icon-only"
                      aria-label={`Edit ${getReaderFullName(reader)}`}
                      title="Edit"
                    >
                      <PencilLine size={16} />
                    </Link>
                    <button
                      type="button"
                      className="button button--ghost-danger button--icon-only"
                      onClick={() => setReaderToDelete(reader)}
                      aria-label={`Delete ${getReaderFullName(reader)}`}
                      title="Delete"
                    >
                      <Trash2 size={16} />
                    </button>
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
          title="No readers found"
          description="Try another search term or register a new reader."
          action={
            <Link to="/readers/new" className="button button--primary">
              Add reader
            </Link>
          }
        />
      )}

      <ConfirmDialog
        open={Boolean(readerToDelete)}
        title="Delete reader"
        description="This action removes the reader record. Check the loan history first if you need to keep reporting context."
        confirmLabel="Delete reader"
        onCancel={() => setReaderToDelete(null)}
        onConfirm={handleDelete}
      />
    </div>
  );
}
