import type { ReactNode, SelectHTMLAttributes, InputHTMLAttributes, TextareaHTMLAttributes } from 'react';

interface FieldWrapProps {
  label: string;
  htmlFor?: string;
  error?: string;
  hint?: ReactNode;
  required?: boolean;
  children: ReactNode;
}

const baseControl =
  'w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500';

function FieldWrap({ label, htmlFor, error, hint, required, children }: FieldWrapProps) {
  return (
    <label className="block" htmlFor={htmlFor}>
      <span className="mb-1 flex items-center gap-1 text-sm font-medium text-slate-700">
        {label}
        {required && <span className="text-red-500">*</span>}
      </span>
      {children}
      {error ? (
        <span className="mt-1 block text-xs text-red-600">{error}</span>
      ) : hint ? (
        <span className="mt-1 block text-xs text-slate-500">{hint}</span>
      ) : null}
    </label>
  );
}

type WrapProps = Omit<FieldWrapProps, 'children'>;

type InputFieldProps = WrapProps & InputHTMLAttributes<HTMLInputElement>;
export function InputField({ label, error, hint, required, htmlFor, ...rest }: InputFieldProps) {
  return (
    <FieldWrap label={label} error={error} hint={hint} required={required} htmlFor={htmlFor ?? rest.id}>
      <input {...rest} className={`${baseControl} ${error ? 'border-red-400' : ''}`} />
    </FieldWrap>
  );
}

type TextareaFieldProps = WrapProps & TextareaHTMLAttributes<HTMLTextAreaElement>;
export function TextareaField({ label, error, hint, required, htmlFor, ...rest }: TextareaFieldProps) {
  return (
    <FieldWrap label={label} error={error} hint={hint} required={required} htmlFor={htmlFor ?? rest.id}>
      <textarea {...rest} className={`${baseControl} ${error ? 'border-red-400' : ''}`} />
    </FieldWrap>
  );
}

type SelectFieldProps = FieldWrapProps & SelectHTMLAttributes<HTMLSelectElement>;
export function SelectField({
  label,
  error,
  hint,
  required,
  htmlFor,
  children,
  ...rest
}: SelectFieldProps) {
  return (
    <FieldWrap label={label} error={error} hint={hint} required={required} htmlFor={htmlFor ?? rest.id}>
      <select {...rest} className={`${baseControl} ${error ? 'border-red-400' : ''}`}>
        {children}
      </select>
    </FieldWrap>
  );
}
