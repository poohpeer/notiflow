type LogoProps = {
  className?: string;
  title?: string;
};

// Brand lockup (mark + wordmark + tagline) from the Notiflow brand assets.
// Source file: frontend/public/notiflow-logo.svg — the favicon is generated
// from the same mark (frontend/public/favicon.ico).
export function Logo({ className, title = 'Notiflow' }: LogoProps) {
  return <img src="/notiflow-logo.svg" alt={title} className={className} />;
}
