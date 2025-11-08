import * as React from "react";
import { Check } from "lucide-react";
import { cn } from "../../lib/utils";

export interface CheckboxProps
  extends Omit<React.InputHTMLAttributes<HTMLInputElement>, "type"> {}

export const Checkbox = React.forwardRef<HTMLInputElement, CheckboxProps>(
  ({ className, checked, ...props }, ref) => (
    <label className={cn("inline-flex items-center gap-2 text-sm", className)}>
      <span className="relative inline-flex h-5 w-5 items-center justify-center rounded border border-slate-700 bg-slate-900 transition-colors">
        <input
          ref={ref}
          type="checkbox"
          className="peer absolute inset-0 h-full w-full cursor-pointer opacity-0"
          checked={!!checked}
          {...props}
        />
        <Check
          className={cn(
            "h-3.5 w-3.5 text-brand opacity-0 transition-opacity peer-checked:opacity-100"
          )}
        />
      </span>
      {props.title && <span>{props.title}</span>}
    </label>
  )
);

Checkbox.displayName = "Checkbox";
