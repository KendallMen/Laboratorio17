import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const savedUser = localStorage.getItem('insecure_pay_user');
  if (!savedUser) {
    return next(req);
  }

  try {
    const user = JSON.parse(savedUser) as { token?: string };
    if (!user.token) {
      return next(req);
    }
    return next(req.clone({
      setHeaders: { Authorization: `Bearer ${user.token}` }
    }));
  } catch {
    return next(req);
  }
};
