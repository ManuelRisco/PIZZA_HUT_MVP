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

  constructor(private readonly cdr: ChangeDetectorRef) {}

  ngOnInit() {
    this.startAutoPlay();
  }

  ngOnDestroy() {
    this.stopAutoPlay();
  }

  startAutoPlay() {
    this.intervalId = setInterval(() => {
      this.next();
    }, 5000);
  }

  stopAutoPlay() {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }

  next() {
    this.prevIndex.set(this.currentIndex());
    this.currentIndex.set((this.currentIndex() + 1) % this.images.length);
    this.cdr.markForCheck();
  }

  prev() {
    this.prevIndex.set(this.currentIndex());
    this.currentIndex.set((this.currentIndex() - 1 + this.images.length) % this.images.length);
    this.cdr.markForCheck();
  }

  goTo(index: number) {
    if (this.currentIndex() !== index) {
      this.prevIndex.set(this.currentIndex());
      this.currentIndex.set(index);
      this.cdr.markForCheck();
      
      // Reiniciar el timer al hacer click manual
      this.stopAutoPlay();
      this.startAutoPlay();
    }
  }
}

