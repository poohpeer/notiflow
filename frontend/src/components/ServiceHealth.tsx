import type { ServiceStatus } from '../hooks/useMetrics';

export function ServiceHealth({ services }: { services: ServiceStatus[] }) {
  if (!services.length) {
    return <div className="text-sm text-slate-400">No scrape targets reporting.</div>;
  }
  return (
    <div className="flex flex-wrap gap-2">
      {services.map((s) => (
        <span
          key={s.job}
          className={`inline-flex items-center gap-2 rounded-full px-3 py-1 text-sm font-medium ring-1 ring-inset ${
            s.up
              ? 'bg-green-50 text-green-800 ring-green-600/20'
              : 'bg-red-50 text-red-800 ring-red-600/20'
          }`}
        >
          <span
            className={`h-2 w-2 rounded-full ${s.up ? 'bg-green-500' : 'bg-red-500'}`}
            aria-hidden
          />
          {s.job}
          <span className="text-xs opacity-70">{s.up ? 'UP' : 'DOWN'}</span>
        </span>
      ))}
    </div>
  );
}
