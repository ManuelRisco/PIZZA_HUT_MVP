import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { LoginDTO } from '../../models/usuario.interface';

@Component({
  selector: 'app-join',
  standalone: true,
  imports: [
    FormsModule,
    RouterModule
  ],
  templateUrl: './join.html',
  styleUrls: ['./join.css']
})
export class Join { 
    
  usuario: LoginDTO = {
    email: '',
    password: '',
  };

  loginError: string = '';
  loading = false;

  constructor(private authService: AuthService, private router: Router) {}
  
  onLogin(event: Event) {
    event.preventDefault();
    
    if (!this.usuario.email || !this.usuario.password) {
      this.loginError = 'Por favor, complete todos los campos';
      return;
    }

    this.loading = true;
    this.loginError = '';

    this.authService.login(this.usuario.email, this.usuario.password).subscribe({
      next: (response) => {
        this.loading = false;
        
        // Redirigir según el rol del usuario en la respuesta
        if (response.usuario.role === 'ADMIN') {
          this.router.navigate(['/panel-admin']);
        } else {
          this.router.navigate(['/menu']);
        }
      },
      error: (error) => {
        this.loading = false;
        console.error('Error en login:', error);
        this.loginError = error.error?.message || 'Correo o contraseña incorrectos';
      }
    });
  }
}
