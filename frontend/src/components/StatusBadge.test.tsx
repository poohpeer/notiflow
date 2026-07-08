import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { StatusBadge, statusColor } from './StatusBadge';

describe('StatusBadge', () => {
  it('renders the humanized status label', () => {
    render(<StatusBadge status="FAILED_RETRYABLE" />);
    expect(screen.getByText('Failed Retryable')).toBeInTheDocument();
  });

  it('applies the status color classes', () => {
    render(<StatusBadge status="SENT" />);
    expect(screen.getByText('Sent').className).toContain('bg-green-100');
  });
});

describe('statusColor', () => {
  it('maps terminal failures to red', () => {
    expect(statusColor('DEAD_LETTERED')).toContain('red');
    expect(statusColor('FAILED_PERMANENT')).toContain('red');
  });

  it('maps success to green', () => {
    expect(statusColor('SENT')).toContain('green');
  });
});
