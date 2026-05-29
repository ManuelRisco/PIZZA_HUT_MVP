import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MetodosPagoComponent } from './metodos-pago';

describe('MetodosPagoComponent', () => {
  let component: MetodosPagoComponent;
  let fixture: ComponentFixture<MetodosPagoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MetodosPagoComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MetodosPagoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
