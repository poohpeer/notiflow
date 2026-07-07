type LogoProps = {
  className?: string;
  title?: string;
};

// Brand mark: gradient squircle with three white "flow" waves (decreasing
// opacity) and a notification dot. Shared by the header and the favicon
// (frontend/public/favicon.svg — keep the two in sync).
export function Logo({ className, title = 'Notiflow' }: LogoProps) {
  return (
    <svg
      viewBox="0 0 200 200"
      className={className}
      role="img"
      aria-label={title}
      xmlns="http://www.w3.org/2000/svg"
    >
      <defs>
        <linearGradient id="nf-grad" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#5B7CFA" />
          <stop offset="100%" stopColor="#6D4FE0" />
        </linearGradient>
      </defs>
      <path
        fill="url(#nf-grad)"
        d="M100,18 C142,16 178,40 184,82 C190,124 168,170 124,182 C80,194 34,178 18,138 C2,98 10,54 44,32 C60,22 80,19 100,18 Z"
      />
      <path d="M56,84 Q78,64 100,84 Q122,104 144,84" fill="none" stroke="#FFFFFF" strokeWidth="10" strokeLinecap="round" />
      <path d="M56,108 Q78,88 100,108 Q122,128 144,108" fill="none" stroke="#FFFFFF" strokeWidth="10" strokeLinecap="round" opacity="0.75" />
      <path d="M66,132 Q83,116 100,132 Q117,148 134,132" fill="none" stroke="#FFFFFF" strokeWidth="10" strokeLinecap="round" opacity="0.5" />
      <circle cx="168" cy="30" r="10" fill="#FFFFFF" />
    </svg>
  );
}
