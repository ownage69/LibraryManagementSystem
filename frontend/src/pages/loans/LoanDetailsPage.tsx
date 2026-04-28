import { ArrowLeft, ArrowLeftRight, BookOpen, CalendarDays, PencilLine, UserRound } from 'lucide-react';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ErrorState } from '../../components/common/ErrorState';
import { LoadingState } from '../../components/common/LoadingState';
import { PageHeader } from '../../components/common/PageHeader';
import { StatusBadge } from '../../components/common/StatusBadge';
import { SurfaceCard } from '../../components/common/SurfaceCard';
import { runtimeConfig } from '../../config/runtime';
import { useAsyncValue } from '../../hooks/useAsyncValue';
import { loanService } from '../../services/libraryService';
import { formatDate, formatLoanStatusLabel } from '../../utils/format';
import { getErrorMessage } from '../../utils/errors';

function resolveTone(label: string): 'success' | 'warning' | 'danger' {
  if (label === 'Returned') {
    return 'success';
  }

  if (label === 'Overdue') {
    return 'danger';
  }

  return 'warning';
}

export function LoanDetailsPage() {
  const id = Number(useParams().id);
  const { data, loading, error, reload } = useAsyncValue(() => loanService.getById(id), [id]);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [isReturning, setIsReturning] = useState(false);

  if (loading) {
    return <LoadingState title="Loading loan details..." />;
  }

  if (error || !data) {
    return <ErrorState description={error || 'Loan details could not be loaded.'} onRetry={reload} />;
  }

  const loan = data;
  const status = formatLoanStatusLabel(loan);

  async function handleReturn() {
    setFeedback(null);
    setIsReturning(true);

    try {
      await loanService.returnBook(loan.id);
      setFeedback(`Loan #${loan.id} was updated with the return action.`);
      reload();
    } catch (returnError) {
      setFeedback(getErrorMessage(returnError));
    } finally {
      setIsReturning(false);
    }
  }

  return (
    <div className="page-layout">
      <PageHeader
        breadcrumbs={[
          { label: 'Loans', to: '/loans' },
          { label: `Loan #${loan.id}` },
        ]}
        eyebrow="Loan details"
        title={`Loan #${loan.id}`}
        description="See the full borrow lifecycle with reader, book, dates, and current status in one focused view."
        actions={
          <div className="button-row">
            <Link to="/loans" className="button button--ghost">
              <ArrowLeft size={16} />
              Back
            </Link>
            <Link
              to={`/loans/${loan.id}/edit`}
              className="button button--ghost button--icon-only"
              aria-label={`Edit loan ${loan.id}`}
              title="Edit"
            >
              <PencilLine size={16} />
            </Link>
            {!loan.returned && runtimeConfig.features.loanReturnEnabled ? (
              <button
                type="button"
                className="button button--primary"
                onClick={handleReturn}
                disabled={isReturning}
              >
                <ArrowLeftRight size={16} />
                {isReturning ? 'Processing...' : 'Return book'}
              </button>
            ) : null}
          </div>
        }
      />

      {feedback ? (
        <div className="alert-banner alert-banner--success">
          <strong>Loan action</strong>
          <p>{feedback}</p>
        </div>
      ) : null}

      <SurfaceCard className="loan-detail-card">
        <div className="loan-detail-card__header">
          <div>
            <p className="section-eyebrow">Loan status</p>
            <h3>{loan.bookTitle}</h3>
          </div>
          <StatusBadge label={status} tone={resolveTone(status)} />
        </div>

        <div className="detail-grid">
          <div className="detail-item">
            <BookOpen size={16} />
            <div>
              <small>Book</small>
              <strong>
                <Link to={`/books/${loan.bookId}`}>{loan.bookTitle}</Link>
              </strong>
            </div>
          </div>

          <div className="detail-item">
            <UserRound size={16} />
            <div>
              <small>Reader</small>
              <strong>
                <Link to={`/readers/${loan.readerId}`}>{loan.readerName}</Link>
              </strong>
            </div>
          </div>

          <div className="detail-item">
            <CalendarDays size={16} />
            <div>
              <small>Borrow date</small>
              <strong>{formatDate(loan.loanDate)}</strong>
            </div>
          </div>

          <div className="detail-item">
            <CalendarDays size={16} />
            <div>
              <small>Due date</small>
              <strong>{formatDate(loan.dueDate)}</strong>
            </div>
          </div>

          <div className="detail-item">
            <CalendarDays size={16} />
            <div>
              <small>Return date</small>
              <strong>
                {formatDate(
                  loan.returnDate,
                  loan.returned ? 'Returned date unavailable' : 'Not returned',
                )}
              </strong>
            </div>
          </div>
        </div>
      </SurfaceCard>
    </div>
  );
}
