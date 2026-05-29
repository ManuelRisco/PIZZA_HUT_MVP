import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Carousel } from '../carousel/carousel';
import { SobreNosotros } from "../sobre-nosotros/sobre-nosotros";
import { MenuComponent } from "../menu/menu";

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, Carousel, SobreNosotros, MenuComponent],
  templateUrl: './home.html',
  styleUrls: ['./home.css']
})
export class Home {
  
}