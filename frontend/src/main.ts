import { Component, OnInit, provideZoneChangeDetection } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { bootstrapApplication } from '@angular/platform-browser';
import { api, ErrorApi, Usuario } from './api';
import { InventarioComponent } from './inventario';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [FormsModule, InventarioComponent],
  templateUrl: './principal.html'
})
class AppComponent implements OnInit {
  usuario: Usuario | null = null;
  correo = '';
  clave = '';
  error = '';
  ocupado = false;
  verificando = true;

  async ngOnInit() {
    try {
      this.usuario = await api<Usuario>('auth/me');
    } catch (error) {
      if (!(error instanceof ErrorApi && error.estado === 401)) {
        this.error = 'No se pudo conectar con el taller. Comprueba que el backend está en marcha.';
      }
    } finally {
      this.verificando = false;
    }
  }

  async entrar(form: NgForm, elemento: HTMLFormElement) {
    if (this.ocupado) return;
    if (form.invalid) {
      elemento.querySelector<HTMLInputElement>('input.ng-invalid')?.focus();
      return;
    }
    this.ocupado = true;
    this.error = '';
    try {
      this.usuario = await api<Usuario>('auth/login', 'POST', { email: this.correo.trim(), password: this.clave });
      this.clave = '';
    } catch (error) {
      if (error instanceof ErrorApi && error.estado === 400) {
        this.error = 'Revisa el formato del correo y la longitud de la contraseña.';
      } else if (error instanceof ErrorApi && error.estado === 401) {
        this.error = 'El correo o la contraseña son incorrectos.';
      } else {
        this.error = 'No se pudo iniciar sesión. Comprueba la conexión e inténtalo de nuevo.';
      }
    } finally {
      this.ocupado = false;
    }
  }

  async salir() {
    this.ocupado = true;
    try {
      await api<void>('auth/logout', 'POST');
      this.finSesion();
    } catch (error) {
      if (error instanceof ErrorApi && error.estado === 401) this.finSesion();
      else this.error = 'No se pudo cerrar la sesión. Inténtalo de nuevo.';
    } finally {
      this.ocupado = false;
    }
  }

  finSesion(caducada = false) {
    this.usuario = null;
    this.clave = '';
    this.error = caducada ? 'Tu sesión ha caducado. Vuelve a ingresar para continuar.' : '';
  }
}

bootstrapApplication(AppComponent, {
  providers: [provideZoneChangeDetection()]
}).catch(error => console.error(error));
