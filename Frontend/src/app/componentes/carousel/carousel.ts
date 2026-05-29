import { Component, OnInit, OnDestroy, ChangeDetectorRef, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-carousel',
  standalone: true,
  imports: [
    CommonModule
  ],
  templateUrl: './carousel.html',
  styleUrls: ['./carousel.css']
})
export class Carousel implements OnInit, OnDestroy {
  images: string[] = [
    'combo1.webp',
    'combo2.webp',
    'combo3.webp',
    'combo4.webp',
    'combo5.webp'
  ];
  currentIndex = signal(0);
  prevIndex = signal(0);
  private intervalId: any;

  constructor(private cdr: ChangeDetectorRef) {}

  ngOnInit() {
    this.intervalId = setInterval(() => {
      this.prevIndex.set(this.currentIndex());
      this.currentIndex.set((this.currentIndex() + 1) % this.images.length);
      this.cdr.markForCheck();
    }, 5000);
  }

  ngOnDestroy() {
    clearInterval(this.intervalId);
  }
}
