import * as React from "react";
import { cn } from "../../lib/utils";

export const Card = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
  <div
    ref={ref}
    className={cn(
      "rounded-xl border border-slate-200 bg-white p-6 text-slate-900 shadow-sm shadow-slate-200/60 dark:border-slate-800 dark:bg-slate-900/60 dark:text-slate-100 dark:shadow-slate-950/40",
      className
    )}
    {...props}
  />
));

Card.displayName = "Card";

export const CardHeader = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
  <div
    ref={ref}
    className={cn(
      "mb-4 flex flex-col gap-1.5 text-sm text-slate-600 dark:text-slate-300",
      className
    )}
    {...props}
  />
));

CardHeader.displayName = "CardHeader";

export const CardTitle = React.forwardRef<
  HTMLHeadingElement,
  React.HTMLAttributes<HTMLHeadingElement>
>(({ className, ...props }, ref) => (
  <h3
    ref={ref}
    className={cn("text-lg font-semibold text-slate-900 dark:text-slate-50", className)}
    {...props}
  />
));

CardTitle.displayName = "CardTitle";

export const CardContent = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
  <div
    ref={ref}
    className={cn("text-sm text-slate-700 dark:text-slate-200", className)}
    {...props}
  />
));

CardContent.displayName = "CardContent";

export const CardDescription = React.forwardRef<
  HTMLParagraphElement,
  React.HTMLAttributes<HTMLParagraphElement>
>(({ className, ...props }, ref) => (
  <p
    ref={ref}
    className={cn("text-xs text-slate-500 dark:text-slate-400", className)}
    {...props}
  />
));

CardDescription.displayName = "CardDescription";
