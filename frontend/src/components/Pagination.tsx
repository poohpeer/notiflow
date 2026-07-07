import type { Page } from '../api/types';

interface PaginationProps {
  page: Page<unknown>;
  onPageChange: (page: number) => void;
}

export function Pagination({ page, onPageChange }: PaginationProps) {
  const from = page.totalElements === 0 ? 0 : page.number * page.size + 1;
  const to = Math.min((page.number + 1) * page.size, page.totalElements);

  return (
    <div className="flex items-center justify-between border-t border-slate-200 px-2 py-3 text-sm text-slate-600">
      <span>
        {from}–{to} of {page.totalElements}
      </span>
      <div className="flex items-center gap-2">
        <button
          className="rounded-md border border-slate-300 bg-white px-3 py-1 disabled:cursor-not-allowed disabled:opacity-50 hover:bg-slate-50"
          onClick={() => onPageChange(page.number - 1)}
          disabled={page.first}
        >
          Previous
        </button>
        <span className="px-1">
          Page {page.totalPages === 0 ? 0 : page.number + 1} of {page.totalPages}
        </span>
        <button
          className="rounded-md border border-slate-300 bg-white px-3 py-1 disabled:cursor-not-allowed disabled:opacity-50 hover:bg-slate-50"
          onClick={() => onPageChange(page.number + 1)}
          disabled={page.last}
        >
          Next
        </button>
      </div>
    </div>
  );
}
