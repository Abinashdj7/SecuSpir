import { HttpInterceptorFn } from '@angular/common/http';
import { tap } from 'rxjs/operators';
import { inject } from '@angular/core';
import { Router } from '@angular/router';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('token');
  const router = inject(Router);

  const clonedReq = token
    ? req.clone({ headers: req.headers.set('Authorization', `Bearer ${token}`) })
    : req;

  return next(clonedReq).pipe(
    tap({
      error: (error) => {
        // Don't redirect on auth endpoints — login/register handle their own 401/409 errors
        const isAuthEndpoint = req.url.includes('/api/auth/');
        if (error.status === 401 && !isAuthEndpoint) {
          localStorage.removeItem('token');
          router.navigate(['/login']);
        }
      }
    })
  );
};