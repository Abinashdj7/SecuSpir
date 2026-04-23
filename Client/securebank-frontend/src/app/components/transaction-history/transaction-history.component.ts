import { Component, OnInit, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { TransactionService } from '../../services/transaction.service';
import { AccountService } from '../../services/account.service';

@Component({
  selector: 'app-transaction-history',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './transaction-history.component.html'
})
export class TransactionHistoryComponent implements OnInit {
  transactions: any[] = [];
  account: any = null;
  loading = true;
  error = '';
  accountId!: number;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private transactionService: TransactionService,
    private accountService: AccountService,
    private ngZone: NgZone,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit() {
    this.accountId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadAccount();
    this.loadTransactions();
  }

  loadAccount() {
    this.accountService.getAccountById(this.accountId).subscribe({
      next: (data: any) => {
        this.ngZone.run(() => {
          this.account = { ...data };
          this.cdr.detectChanges();
        });
      },
      error: () => {
        this.ngZone.run(() => {
          this.error = 'Failed to load account';
          this.cdr.detectChanges();
        });
      }
    });
  }

  loadTransactions() {
    this.loading = true;
    this.transactionService.getHistory(this.accountId).subscribe({
      next: (data: any[]) => {
        this.ngZone.run(() => {
          this.transactions = [...data];
          this.loading = false;
          this.cdr.markForCheck();
          this.cdr.detectChanges();
        });
      },
      error: () => {
        this.ngZone.run(() => {
          this.error = 'Failed to load transactions';
          this.loading = false;
          this.cdr.detectChanges();
        });
      }
    });
  }

  getTransactionIcon(type: string): string {
    switch (type) {
      case 'DEPOSIT': return '💰';
      case 'WITHDRAWAL': return '💸';
      case 'TRANSFER': return '🔁';
      default: return '💳';
    }
  }

  getAmountPrefix(transaction: any): string {
    if (transaction.type === 'DEPOSIT') return '+';
    if (transaction.type === 'WITHDRAWAL') return '-';
    if (transaction.senderAccountNumber === this.account?.accountNumber) return '-';
    return '+';
  }

  getAmountColor(transaction: any): string {
    return this.getAmountPrefix(transaction) === '+' ? 'text-green-400' : 'text-red-400';
  }

  goBack() {
    this.router.navigate(['/dashboard']);
  }

  goToTransfer() {
    this.router.navigate(['/transfer']);
  }
}