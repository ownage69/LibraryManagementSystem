import { ArrowLeft, BookOpen, Mail, PencilLine, UserRound } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import { EmptyState } from '../../components/common/EmptyState';
import { ErrorState } from '../../components/common/ErrorState';
import { LoadingState } from '../../components/common/LoadingState';
import { PageHeader } from '../../components/common/PageHeader';
import { StatusBadge } from '../../components/common/StatusBadge';
import { SurfaceCard } from '../../components/common/SurfaceCard';
import { useAsyncValue } from '../../hooks/useAsyncValue';
import { loanService, readerService } from '../../services/libraryService';
import { formatDate, formatLoanStatusLabel, getReaderFullName } from '../../utils/format';
import { getLoansForReader } from '../../utils/library';

function resolveTone(label: string): 'success' | 'warning' | 'danger' {
  if (label === 'Returned') {
    return 'success';
  }

  if (label === 'Overdue') {
    return 'danger';
  }

  return 'warning';
}

export function ReaderDetailsPage() {
  const id = Number(useParams().id);
  const { data, loading, error, reload } = useAsyncValue(async () => {
    const [reader, loans] = await Promise.all([readerService.getById(id), loanService.list()]);
    return { reader, loans };
  }, [id]);

  if (loading) {
    return <LoadingState title="Loading reader profile..." />;
  }

  if (error || !data) {
    return <ErrorState description={error || 'Reader details could not be loaded.'} onRetry={reload} />;
  }

  const allLoans = getLoansForReader(data.loans, data.reader.id);
  const currentLoans = allLoans.filter((loan) => !loan.returned);
  const pastLoans = allLoans.filter((loan) => loan.returned);

  return (
    <div className="page-layout">
      <PageHeader
        breadcrumbs={[
          { label: 'Readers', to: '/readers' },
          { label: getReaderFullName(data.reader) },
        ]}
        eyebrow="Reader details"
        title={getReaderFullName(data.reader)}
        description="Review contact data and split current versus past borrowing activity."
        actions={
          <div className="button-row">
            <Link to="/readers" className="button button--ghost">
              <ArrowLeft size={16} />
              Back
            </Link>
            <Link
              to={`/readers/${data.reader.id}/edit`}
              className="button button--primary button--icon-only"
              aria-label={`Edit ${getReaderFullName(data.reader)}`}
              title="Edit"
            >
              <PencilLine size={16} />
            </Link>
          </div>
        }
      />

      <div className="split-layout">
        <SurfaceCard className="profile-card">
          <div className="entity-card__icon">
            <UserRound size={18} />
          </div>
          <h3>{getReaderFullName(data.reader)}</h3>
          <p className="entity-card__summary entity-card__summary--inline">
            <Mail size={14} />
            {data.reader.email}
          </p>
          <p>{currentLoans.length} active loans and {pastLoans.length} returned items.</p>
        </SurfaceCard>

        <SurfaceCard
          header={
            <div>
              <p className="section-eyebrow">Current loans</p>
              <h3 className="section-title">Books still with the reader</h3>
            </div>
          }
        >
          {currentLoans.length ? (
            <div className="simple-list">
              {currentLoans.map((loan) => (
                <Link key={loan.id} to={`/loans/${loan.id}`} className="simple-list__item">
                  <div className="simple-list__icon">
                    <BookOpen size={16} />
                  </div>
                  <div>
                    <strong>{loan.bookTitle}</strong>
                    <p>
                      Borrowed {formatDate(loan.loanDate)} • Due {formatDate(loan.dueDate)}
                    </p>
                  </div>
                  <StatusBadge label={formatLoanStatusLabel(loan)} tone={resolveTone(formatLoanStatusLabel(loan))} />
                </Link>
              ))}
            </div>
          ) : (
            <EmptyState
              icon={BookOpen}
              title="No active loans"
              description="Books currently borrowed by this reader will appear here."
            />
          )}
        </SurfaceCard>
      </div>

      <SurfaceCard
        header={
          <div>
            <p className="section-eyebrow">History</p>
            <h3 className="section-title">Returned loans</h3>
          </div>
        }
      >
        {pastLoans.length ? (
          <div className="simple-list">
            {pastLoans.map((loan) => (
              <Link key={loan.id} to={`/loans/${loan.id}`} className="simple-list__item">
                <div className="simple-list__icon">
                  <BookOpen size={16} />
                </div>
                <div>
                  <strong>{loan.bookTitle}</strong>
                  <p>
                    Borrowed {formatDate(loan.loanDate)} • Due {formatDate(loan.dueDate)}
                  </p>
                </div>
                <StatusBadge label="Returned" tone="success" />
              </Link>
            ))}
          </div>
        ) : (
          <EmptyState
            icon={BookOpen}
            title="No returned loans yet"
            description="Completed loan history will appear here after items are returned."
          />
        )}
      </SurfaceCard>
    </div>
  );
}
