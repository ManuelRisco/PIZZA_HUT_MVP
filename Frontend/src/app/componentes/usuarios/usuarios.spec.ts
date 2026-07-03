import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Usuarios } from './usuarios';

describe('Usuarios', () => {
  let component: Usuarios;
  let fixture: ComponentFixture<Usuarios>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideRouter([])],
      imports: [Usuarios]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Usuarios);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});


