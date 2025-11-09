import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "../../lib/utils";

const badgeVariants = cva(
  "inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium transition-colors",
  {
    variants: {
      variant: {
        default:
          "border-slate-200 bg-slate-100 text-slate-900 dark:border-transparent dark:bg-slate-800 dark:text-slate-100",
        success:
          "border-transparent bg-emerald-500/10 text-emerald-600 dark:bg-emerald-600/20 dark:text-emerald-200",
        warning:
          "border-transparent bg-amber-400/20 text-amber-700 dark:bg-amber-600/20 dark:text-amber-100",
        destructive:
          "border-transparent bg-rose-500/10 text-rose-600 dark:bg-rose-600/20 dark:text-rose-200",
        outline:
          "border-slate-300 text-slate-700 dark:border-slate-700 dark:text-slate-100"
      }
    },
    defaultVariants: {
      variant: "default"
    }
  }
);

export interface BadgeProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof badgeVariants> {}

export const Badge = ({ className, variant, ...props }: BadgeProps) => (
  <div className={cn(badgeVariants({ variant }), className)} {...props} />
);
