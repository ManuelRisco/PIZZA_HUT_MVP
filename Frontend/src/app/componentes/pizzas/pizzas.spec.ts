import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Pizzas } from './pizzas';

describe('Pizzas', () => {
  let component: Pizzas;
  let fixture: ComponentFixture<Pizzas>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideRouter([])],
      imports: [Pizzas]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Pizzas);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});


