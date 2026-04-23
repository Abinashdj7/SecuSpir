import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  form: FormGroup;
  error = '';
  loading = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required] // ← no minLength here
    });
  }

  onSubmit() {
    if (this.form.invalid) return;
    this.loading = true;
    this.error = '';

    console.log('Submitting:', this.form.value); // ← add this to debug

    this.authService.login(this.form.value).subscribe({
      next: (res) => {
        console.log('Login success:', res); // ← and this
        this.loading = false;
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        console.log('Login error:', err); // ← and this
        this.error = err.error?.error || 'Invalid email or password';
        this.loading = false;
      }
    });
  }
}