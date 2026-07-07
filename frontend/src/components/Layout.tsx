import { NavLink, Outlet } from 'react-router-dom';
import { Logo } from './Logo';

const navItems = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/notifications', label: 'Notifications', end: false },
  { to: '/notifications/new', label: 'New', end: false },
];

function navClass({ isActive }: { isActive: boolean }): string {
  return `rounded-md px-3 py-2 text-sm font-medium ${
    isActive ? 'bg-slate-900 text-white' : 'text-slate-600 hover:bg-slate-100'
  }`;
}

export function Layout() {
  return (
    <div className="min-h-screen">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3">
          <NavLink to="/" className="flex items-center gap-2 text-lg font-semibold text-slate-900">
            <Logo className="h-8 w-8" />
            Notiflow
          </NavLink>
          <nav className="flex items-center gap-1">
            {navItems.map((item) => (
              <NavLink key={item.to} to={item.to} end={item.end} className={navClass}>
                {item.label}
              </NavLink>
            ))}
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  );
}
