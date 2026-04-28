#!/usr/bin/env sh
set -eu

LOCAL_DATABASE_URL="${LOCAL_DATABASE_URL:-postgresql://postgres:190817@localhost:5432/library}"

if [ -z "${RENDER_DATABASE_URL:-}" ]; then
    echo "Set RENDER_DATABASE_URL to the Render PostgreSQL External Database URL."
    echo "Example:"
    echo "  RENDER_DATABASE_URL='postgresql://user:password@host:5432/library?sslmode=require' bash scripts/migrate-local-postgres-to-render.sh"
    exit 1
fi

if [ "${MIGRATION_MODE:-append}" = "replace" ]; then
    echo "Clearing existing Render rows before import..."
    psql "${RENDER_DATABASE_URL}" -v ON_ERROR_STOP=1 -c "
        truncate table
            loans,
            book_authors,
            book_categories,
            books,
            authors,
            categories,
            publishers,
            readers
        restart identity cascade;
    "
fi

echo "Exporting existing local data and importing it into Render PostgreSQL..."
pg_dump \
    --data-only \
    --no-owner \
    --no-acl \
    "${LOCAL_DATABASE_URL}" \
    | psql "${RENDER_DATABASE_URL}" -v ON_ERROR_STOP=1

echo "Done. Redeploy or refresh the site, then open /dashboard again."
