import { Component, ElementRef, EventEmitter, OnInit, Output, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { api, ErrorApi, Ficha, medidaValida, Retazo } from './api';

const fichaVacia = (): Ficha => ({ material: '', color: '#315875', ancho: null, largo: null, ubicacion: '' });

@Component({
  selector: 'app-inventario',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './inventario.html'
})
export class InventarioComponent implements OnInit {
  @Output() sesionCaducada = new EventEmitter<void>();
  @ViewChild('editor', { static: true }) editor!: ElementRef<HTMLDialogElement>;
  @ViewChild('confirmacion', { static: true }) confirmacion!: ElementRef<HTMLDialogElement>;
  retazos: Retazo[] = [];
  filtro: { ancho: number; largo: number } | null = null;
  anchoBuscado: number | null = null;
  largoBuscado: number | null = null;
  ficha = fichaVacia();
  seleccionado: Retazo | null = null;
  ocupado = false;
  error = '';
  errorModal = '';
  aviso = '';
  cargado = false;

  ngOnInit() { void this.cargar(); }

  alCancelar(evento: Event): void {
    if (this.ocupado) evento.preventDefault();
  }

  private fallo(error: unknown, mensaje: string, modal = false) {
    if (error instanceof ErrorApi && error.estado === 401) {
      this.sesionCaducada.emit();
    } else if (modal) {
      this.errorModal = mensaje;
    } else {
      this.error = mensaje;
    }
  }

  async cargar(filtro = this.filtro) {
    this.ocupado = true;
    this.error = '';
    try {
      const parametros = filtro ? '?anchoMin=' + filtro.ancho + '&largoMin=' + filtro.largo : '';
      this.retazos = await api<Retazo[]>('retazos' + parametros);
      this.filtro = filtro;
      this.cargado = true;
    } catch (error) {
      this.fallo(error, 'No se pudo cargar el inventario. Vuelve a intentarlo.');
    } finally {
      this.ocupado = false;
    }
  }

  buscar() {
    if (this.ocupado) return;
    this.aviso = '';
    if (!medidaValida(this.anchoBuscado) || !medidaValida(this.largoBuscado)) {
      this.error = 'Escribe el ancho y el largo en centímetros enteros, entre 1 y 10 000.';
      return;
    }
    void this.cargar({ ancho: this.anchoBuscado!, largo: this.largoBuscado! });
  }

  async quitarFiltro() {
    this.aviso = '';
    await this.cargar(null);
    if (!this.filtro) {
      this.anchoBuscado = null;
      this.largoBuscado = null;
    }
  }

  abrirEditor(retazo: Retazo | null = null) {
    this.seleccionado = retazo;
    this.ficha = retazo
      ? { material: retazo.material, color: retazo.color, ancho: retazo.ancho, largo: retazo.largo, ubicacion: retazo.ubicacion }
      : fichaVacia();
    this.errorModal = '';
    this.editor.nativeElement.showModal();
  }

  async guardar() {
    if (this.ocupado) return;
    const datos = { ...this.ficha, material: this.ficha.material.trim(), ubicacion: this.ficha.ubicacion.trim() };
    if (!datos.material || !datos.ubicacion || !medidaValida(datos.ancho) || !medidaValida(datos.largo)) {
      this.errorModal = 'Completa el material y la ubicación. Las medidas deben ser enteros entre 1 y 10 000 cm.';
      return;
    }
    this.ocupado = true;
    this.errorModal = '';
    try {
      const id = this.seleccionado?.id;
      await api(id ? 'retazos/' + id : 'retazos', id ? 'PUT' : 'POST', datos);
      this.editor.nativeElement.close();
      this.aviso = id ? 'Retazo actualizado.' : 'Retazo añadido al inventario.';
      await this.cargar();
    } catch (error) {
      this.fallo(error, 'No se pudo guardar el retazo. Revisa los datos y vuelve a intentarlo.', true);
    } finally {
      this.ocupado = false;
    }
  }

  confirmar(retazo: Retazo) {
    this.seleccionado = retazo;
    this.errorModal = '';
    this.confirmacion.nativeElement.showModal();
  }

  async eliminar() {
    if (!this.seleccionado || this.ocupado) return;
    this.ocupado = true;
    this.errorModal = '';
    try {
      await api('retazos/' + this.seleccionado.id, 'DELETE');
      this.confirmacion.nativeElement.close();
      this.aviso = 'Retazo eliminado.';
      await this.cargar();
    } catch (error) {
      this.fallo(error, 'No se pudo eliminar el retazo. Vuelve a intentarlo.', true);
    } finally {
      this.ocupado = false;
    }
  }
}
