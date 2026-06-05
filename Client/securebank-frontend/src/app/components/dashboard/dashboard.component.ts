import { Component, OnInit, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { AccountService } from '../../services/account.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  accounts: any[] = [];
  loading = true;
  error = '';
  showNewAccountModal = false;
  creatingAccount = false;

  constructor(
    private accountService: AccountService,
    private authService: AuthService,
    private router: Router,
    private ngZone: NgZone,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit() {
    this.loadAccounts();
  }

  loadAccounts() {
    this.loading = true;
    this.accountService.getMyAccounts().subscribe({
      next: (data: any[]) => {
        this.ngZone.run(() => {
          this.accounts = [...data]; // ← spread forces new array reference
          this.loading = false;
          this.cdr.markForCheck();  // ← force view update
          this.cdr.detectChanges(); // ← immediately apply changes
        });
      },
      error: () => {
        this.ngZone.run(() => {
          this.error = 'Failed to load accounts';
          this.loading = false;
          this.cdr.detectChanges();
        });
      }
    });
  }

  getTotalBalance(): number {
    return this.accounts.reduce((sum, acc) => sum + acc.balance, 0);
  }

  openAccount(type: string) {
    this.creatingAccount = true;
    this.accountService.createAccount({ accountType: type }).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.showNewAccountModal = false;
          this.creatingAccount = false;
          this.loadAccounts();
        });
      },
      error: () => {
        this.ngZone.run(() => { this.creatingAccount = false; });
      }
    });
  }

  goToTransactions(accountId: number) {
    this.router.navigate(['/accounts', accountId, 'transactions']);
  }

  goToTransfer() {
    this.router.navigate(['/transfer']);
  }

  logout() {
    this.authService.logout();
  }

  getEmail(): string {
    return this.authService.getCurrentUserEmail();
  }
}