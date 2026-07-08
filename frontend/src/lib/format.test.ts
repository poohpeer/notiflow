import { describe, expect, it } from 'vitest';
import { PROMQL, channelLabel, formatDateTime, formatNumber, statusLabel } from './format';

describe('statusLabel', () => {
  it('title-cases a single word', () => {
    expect(statusLabel('SENT')).toBe('Sent');
  });

  it('normalizes SNAKE_CASE into words', () => {
    expect(statusLabel('FAILED_RETRYABLE')).toBe('Failed Retryable');
    expect(statusLabel('DEAD_LETTERED')).toBe('Dead Lettered');
  });
});

describe('channelLabel', () => {
  it('keeps the first letter and lowercases the rest', () => {
    expect(channelLabel('EMAIL')).toBe('Email');
    expect(channelLabel('SMS')).toBe('Sms');
  });
});

describe('formatNumber', () => {
  it('renders a locale string', () => {
    expect(formatNumber(5)).toBe('5');
    expect(typeof formatNumber(1000)).toBe('string');
  });
});

describe('formatDateTime', () => {
  it('returns a dash for empty input', () => {
    expect(formatDateTime(null)).toBe('—');
    expect(formatDateTime(undefined)).toBe('—');
  });

  it('returns the raw input when it is not a date', () => {
    expect(formatDateTime('not-a-date')).toBe('not-a-date');
  });

  it('formats a valid ISO timestamp', () => {
    const out = formatDateTime('2026-07-08T10:00:00Z');
    expect(out).not.toBe('—');
    expect(out).toContain('2026');
  });
});

describe('PROMQL', () => {
  it('scopes the service-up query to the three backend jobs', () => {
    expect(PROMQL.serviceUp).toContain('notiflow-api');
    expect(PROMQL.serviceUp).toContain('notiflow-relay');
    expect(PROMQL.serviceUp).toContain('notiflow-worker');
  });
});
