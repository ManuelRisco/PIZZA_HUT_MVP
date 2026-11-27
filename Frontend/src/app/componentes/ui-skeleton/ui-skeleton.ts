import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-ui-skeleton',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="w-full" role="status" aria-busy="true" [attr.aria-label]="label">
      <span class="sr-only">{{ label }}</span>

      @if (variant === 'cards') {
        <div class="grid gap-6" [ngClass]="gridClass">
          @for (item of cardItems; track item) {
            <div class="eco-card p-5 space-y-4">
              <div class="eco-skeleton h-44 w-full rounded-xl"></div>
              <div class="space-y-2">
                <div class="eco-skeleton h-5 w-2/3"></div>
                <div class="eco-skeleton h-4 w-full"></div>
                <div class="eco-skeleton h-4 w-4/5"></div>
              </div>
              <div class="flex items-center justify-between pt-2">
                <div class="eco-skeleton h-6 w-20"></div>
                <div class="eco-skeleton h-10 w-28 rounded-lg"></div>
              </div>
            </div>
          }
        </div>
      } @else if (variant === 'kpis') {
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          @for (item of cardItems; track item) {
            <div class="eco-card p-5 space-y-3">
              <div class="flex items-center justify-between">
                <div class="eco-skeleton h-3.5 w-24"></div>
                <div class="eco-skeleton h-8 w-8 rounded-lg"></div>
              </div>
              <div class="eco-skeleton h-8 w-32"></div>
              <div class="eco-skeleton h-3 w-20"></div>
            </div>
          }
        </div>
      } @else if (variant === 'dashboard') {
        <div class="space-y-6">
          <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            @for (item of fourItems; track item) {
              <div class="eco-card p-5 space-y-3">
                <div class="flex items-center justify-between">
                  <div class="eco-skeleton h-3.5 w-24"></div>
                  <div class="eco-skeleton h-8 w-8 rounded-lg"></div>
                </div>
                <div class="eco-skeleton h-8 w-28"></div>
                <div class="eco-skeleton h-3 w-20"></div>
              </div>
            }
          </div>
          <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div class="eco-card p-6 space-y-4">
              <div class="eco-skeleton h-5 w-44"></div>
              <div class="eco-skeleton h-64 w-full rounded-xl"></div>
            </div>
            <div class="eco-card p-6 space-y-4">
              <div class="eco-skeleton h-5 w-44"></div>
              <div class="eco-skeleton h-64 w-full rounded-xl"></div>
            </div>
          </div>
          <div class="eco-card p-6 space-y-3">
            <div class="eco-skeleton h-5 w-48 mb-2"></div>
            @for (row of rowItems; track row) {
              <div class="flex items-center justify-between gap-4 py-2">
                <div class="eco-skeleton h-4 w-32"></div>
                <div class="eco-skeleton h-4 w-48"></div>
                <div class="eco-skeleton h-6 w-20 rounded-full"></div>
              </div>
            }
          </div>
        </div>
      } @else if (variant === 'table') {
        <div class="eco-card overflow-hidden">
          <div class="px-5 py-4 border-b border-[var(--border-subtle)] bg-[var(--surface-subtle)] flex items-center gap-4">
            @for (col of colItems; track col) {
              <div class="eco-skeleton h-4 flex-1"></div>
            }
          </div>
          <div class="divide-y divide-[var(--border-subtle)]">
            @for (row of rowItems; track row) {
              <div class="px-5 py-4 flex items-center gap-4">
                @for (col of colItems; track col) {
                  <div class="eco-skeleton h-4 flex-1"></div>
                }
              </div>
            }
          </div>
        </div>
      } @else if (variant === 'form') {
        <div class="eco-card p-6 space-y-5">
          <div class="eco-skeleton h-6 w-48"></div>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
            @for (item of cardItems; track item) {
              <div class="space-y-2">
                <div class="eco-skeleton h-4 w-28"></div>
                <div class="eco-skeleton h-10 w-full rounded-lg"></div>
              </div>
            }
          </div>
          <div class="flex justify-end pt-2">
            <div class="eco-skeleton h-10 w-40 rounded-lg"></div>
          </div>
        </div>
      } @else if (variant === 'list') {
        <div class="space-y-3">
          @for (row of rowItems; track row) {
            <div class="eco-card p-4 flex items-center justify-between gap-4">
              <div class="flex items-center gap-4 flex-1 min-w-0">
                <div class="eco-skeleton h-12 w-12 rounded-xl shrink-0"></div>
                <div class="space-y-2 flex-1 max-w-md">
                  <div class="eco-skeleton h-4 w-40"></div>
                  <div class="eco-skeleton h-3.5 w-64 max-w-full"></div>
                </div>
              </div>
              <div class="flex items-center gap-3 shrink-0">
                <div class="eco-skeleton h-6 w-20 rounded-full"></div>
                <div class="eco-skeleton h-9 w-24 rounded-lg"></div>
              </div>
            </div>
          }
        </div>
      } @else if (variant === 'map') {
        <div class="eco-card p-6 grid grid-cols-1 lg:grid-cols-2 gap-6 items-center">
          <div class="space-y-4">
            <div class="eco-skeleton h-7 w-56"></div>
            <div class="eco-skeleton h-4 w-full"></div>
            <div class="eco-skeleton h-4 w-5/6"></div>
            <div class="space-y-3 pt-2">
              <div class="eco-skeleton h-12 w-full rounded-lg"></div>
              <div class="eco-skeleton h-12 w-full rounded-lg"></div>
            </div>
            <div class="eco-skeleton h-11 w-44 rounded-lg"></div>
          </div>
          <div class="eco-skeleton h-80 w-full rounded-xl"></div>
        </div>
      }
    </div>
  `
})
export class UiSkeletonComponent {
  @Input() variant: 'cards' | 'table' | 'kpis' | 'dashboard' | 'form' | 'list' | 'map' = 'cards';
  @Input() count = 6;
  @Input() columns: 1 | 2 | 3 | 4 = 3;
  @Input() rows = 6;
  @Input() cols = 5;
  @Input() label = 'Cargando contenido...';

  readonly fourItems = [0, 1, 2, 3];

  get cardItems(): number[] {
    const safeCount = Math.max(1, Math.min(12, Number(this.count) || 6));
    return Array.from({ length: safeCount }, (_, i) => i);
  }

  get rowItems(): number[] {
    const safeRows = Math.max(1, Math.min(12, Number(this.rows) || 6));
    return Array.from({ length: safeRows }, (_, i) => i);
  }

  get colItems(): number[] {
    const safeCols = Math.max(1, Math.min(8, Number(this.cols) || 5));
    return Array.from({ length: safeCols }, (_, i) => i);
  }

  get gridClass(): string {
    switch (this.columns) {
      case 1:
        return 'grid-cols-1';
      case 2:
        return 'grid-cols-1 md:grid-cols-2';
      case 4:
        return 'grid-cols-1 sm:grid-cols-2 lg:grid-cols-4';
      default:
        return 'grid-cols-1 md:grid-cols-2 lg:grid-cols-3';
    }
  }
}
