import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Ingredientes } from './ingredientes';

describe('Ingredientes', () => {
  let component: Ingredientes;
  let fixture: ComponentFixture<Ingredientes>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideRouter([])],
      imports: [Ingredientes]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Ingredientes);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});


