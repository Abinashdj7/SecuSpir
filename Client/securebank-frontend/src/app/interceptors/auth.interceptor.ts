import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { tap, finalize } from 'rxjs/operators';
import { inject } from '@angular/core';
import { Router } from '@angular/router';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('token');
  const router = inject(Router);
  const startTime = Date.now();

  const clonedReq = token
    ? req.clone({ headers: req.headers.set('Authorization', `Bearer ${token}`) })
    : req;

  console.log(`🚀 [${req.method}] ${req.url}`);

  return next(clonedReq).pipe(
    tap({
      next: (event) => {
        if (event instanceof HttpResponse) {
          const duration = Date.now() - startTime;
          const emoji = duration > 1000 ? '🐢' : duration > 500 ? '⚠️' : '✅';
          console.log(`${emoji} [${req.method}] ${req.url} → ${event.status} [${duration}ms]`);
        }
      },
      error: (error) => {
        const duration = Date.now() - startTime;
        console.error(
          `❌ [${req.method}] ${req.url} → ${error.status} [${duration}ms]`,
          '\nError:', error.error
        );

        // Auto logout on 401
        if (error.status === 401) {
          console.warn('🔒 Token invalid or expired — logging out');
          localStorage.removeItem('token');
          router.navigate(['/login']);
        }
      }
    }),
    finalize(() => {
      const duration = Date.now() - startTime;
      if (duration > 2000) {
        console.warn(`🔴 SLOW REQUEST: [${req.method}] ${req.url} took ${duration}ms`);
      }
    })
  );
};