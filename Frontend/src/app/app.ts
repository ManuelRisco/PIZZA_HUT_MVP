import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './componentes/navbar/navbar';
import { FooterComponent } from "./componentes/footer/footer";

@Component({    
  selector: 'app-root',
  standalone: true,
  imports: [
    NavbarComponent,
    FooterComponent,
    RouterOutlet,
],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class App { 
  protected readonly title = signal('pizzahut');
}