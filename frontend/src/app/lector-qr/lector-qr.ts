import { Component, ElementRef, OnDestroy, OnInit, output, signal, viewChild } from '@angular/core';
import jsQR from 'jsqr';

/** Abre la cámara trasera y avisa (una sola vez) cuando lee un código QR. */
@Component({
  selector: 'app-lector-qr',
  template: `
    <div class="lector">
      <video #video playsinline muted></video>
      <canvas #lienzo hidden></canvas>
      <div class="lector-marco" aria-hidden="true"></div>
    </div>
    @if (error()) {
      <p class="aviso error" role="alert">{{ error() }}</p>
    }
  `
})
export class LectorQr implements OnInit, OnDestroy {
  readonly leido = output<string>();
  protected readonly error = signal<string | null>(null);

  private readonly video = viewChild.required<ElementRef<HTMLVideoElement>>('video');
  private readonly lienzo = viewChild.required<ElementRef<HTMLCanvasElement>>('lienzo');
  private flujo: MediaStream | null = null;
  private cuadro = 0;
  private activo = true;

  async ngOnInit(): Promise<void> {
    if (!navigator.mediaDevices?.getUserMedia) {
      this.error.set('Este navegador no permite usar la cámara (necesita HTTPS). Escribe el código a mano.');
      return;
    }
    try {
      this.flujo = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' }, audio: false });
      if (!this.activo) {
        this.apagar();
        return;
      }
      const elemento = this.video().nativeElement;
      elemento.srcObject = this.flujo;
      await elemento.play();
      this.cuadro = requestAnimationFrame(() => this.mirar());
    } catch {
      this.error.set('No se pudo abrir la cámara. Revisa el permiso del navegador o escribe el código a mano.');
    }
  }

  ngOnDestroy(): void {
    this.activo = false;
    this.apagar();
  }

  private mirar(): void {
    if (!this.activo) {
      return;
    }
    const video = this.video().nativeElement;
    if (video.readyState === video.HAVE_ENOUGH_DATA && video.videoWidth > 0) {
      const lienzo = this.lienzo().nativeElement;
      lienzo.width = video.videoWidth;
      lienzo.height = video.videoHeight;
      const ctx = lienzo.getContext('2d', { willReadFrequently: true });
      if (ctx) {
        ctx.drawImage(video, 0, 0);
        const imagen = ctx.getImageData(0, 0, lienzo.width, lienzo.height);
        const qr = jsQR(imagen.data, imagen.width, imagen.height, { inversionAttempts: 'dontInvert' });
        if (qr?.data) {
          this.activo = false;
          this.apagar();
          this.leido.emit(qr.data.trim());
          return;
        }
      }
    }
    this.cuadro = requestAnimationFrame(() => this.mirar());
  }

  private apagar(): void {
    cancelAnimationFrame(this.cuadro);
    this.flujo?.getTracks().forEach(t => t.stop());
    this.flujo = null;
  }
}
