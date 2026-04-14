import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

/**
 * Bypasses Angular's DomSanitizer for iframe src URLs so that trusted
 * embed URLs (e.g. VidSrc) can be rendered inside an <iframe>.
 *
 * Usage: <iframe [src]="url | trustUrl" ...>
 */
@Pipe({
  name: 'trustUrl',
  standalone: true
})
export class TrustUrlPipe implements PipeTransform {
  constructor(private sanitizer: DomSanitizer) {}

  transform(url: string | null | undefined): SafeResourceUrl {
    if (!url) return '';
    return this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }
}
