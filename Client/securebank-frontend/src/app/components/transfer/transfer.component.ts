import { Component, OnInit, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AccountService } from '../../services/account.service';
import { TransactionService } from '../../services/transaction.service';

@Component({
  selector: 'app-transfer',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './transfer.component.html'
})
export class TransferComponent implements OnInit {
  accounts: any[] = [];
  loading = true;
  submitting = false;
  success = '';
  error = '';
  activeTab: 'deposit' | 'withdraw' | 'transfer' = 'deposit';

  depositForm: FormGroup;
  withdrawForm: FormGroup;
  transferForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private accountService: AccountService,
    private transactionService: TransactionService,
    private router: Router,
    private ngZone: NgZone,
    private cdr: ChangeDetectorRef
  ) {
    this.depositForm = this.fb.group({
      accountId: ['', Validators.required],
      amount: ['', [Validators.required, Validators.min(0.01)]],
      description: ['']
    });

    this.withdrawForm = this.fb.group({
      accountId: ['', Validators.required],
      amount: ['', [Validators.required, Validators.min(0.01)]],
      description: ['']
    });

    this.transferForm = this.fb.group({
      accountId: ['', Validators.required],
      receiverAccountNumber: ['', Validators.required],
      amount: ['', [Validators.required, Validators.min(0.01)]],
      description: ['']
    });
  }

  ngOnInit() {
    this.loadAccounts();
  }

  loadAccounts() {
    this.loading = true;
    this.accountService.getMyAccounts().subscribe({
      next: (data: any[]) => {
        this.ngZone.run(() => {
          this.accounts = [...data.filter(a => a.status === 'ACTIVE')];
          this.loading = false;
          this.cdr.markForCheck();
          this.cdr.detectChanges();
        });
      },
      error: () => {
        this.ngZone.run(() => {
          this.loading = false;
          this.cdr.detectChanges();
        });
      }
    });
  }

  setTab(tab: 'deposit' | 'withdraw' | 'transfer') {
    this.activeTab = tab;
    this.success = '';
    this.error = '';
  }

  onDeposit() {
    if (this.depositForm.invalid) return;
    this.submitting = true;
    this.success = '';
    this.error = '';

    const { accountId, amount, description } = this.depositForm.value;
    this.transactionService.deposit(accountId, { amount, description }).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.success = `Successfully deposited €${amount}!`;
          this.submitting = false;
          this.depositForm.reset();
          this.loadAccounts(); // refresh balances
          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          this.error = err.error?.error || 'Deposit failed';
          this.submitting = false;
          this.cdr.detectChanges();
        });
      }
    });
  }

  onWithdraw() {
    if (this.withdrawForm.invalid) return;
    this.submitting = true;
    this.success = '';
    this.error = '';

    const { accountId, amount, description } = this.withdrawForm.value;
    this.transactionService.withdraw(accountId, { amount, description }).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.success = `Successfully withdrew €${amount}!`;
          this.submitting = false;
          this.withdrawForm.reset();
          this.loadAccounts();
          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          this.error = err.error?.error || 'Withdrawal failed';
          this.submitting = false;
          this.cdr.detectChanges();
        });
      }
    });
  }

  onTransfer() {
    if (this.transferForm.invalid) return;
    this.submitting = true;
    this.success = '';
    this.error = '';

    const { accountId, receiverAccountNumber, amount, description } = this.transferForm.value;
    this.transactionService.transfer(accountId, {
      receiverAccountNumber, amount, description
    }).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.success = `Successfully transferred €${amount}!`;
          this.submitting = false;
          this.transferForm.reset();
          this.loadAccounts();
          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          this.error = err.error?.error || 'Transfer failed';
          this.submitting = false;
          this.cdr.detectChanges();
        });
      }
    });
  }

  goBack() {
    this.router.navigate(['/dashboard']);
  }
}