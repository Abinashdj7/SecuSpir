import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({ providedIn: 'root' })
export class TransactionService {

  private apiUrl = 'http://localhost:8080/api/accounts';

  constructor(private http: HttpClient) {}

  getHistory(accountId: number) {
    return this.http.get<any[]>(`${this.apiUrl}/${accountId}/transactions`);
  }

  deposit(accountId: number, data: any) {
    return this.http.post<any>(`${this.apiUrl}/${accountId}/transactions/deposit`, data);
  }

  withdraw(accountId: number, data: any) {
    return this.http.post<any>(`${this.apiUrl}/${accountId}/transactions/withdraw`, data);
  }

  transfer(accountId: number, data: any) {
    return this.http.post<any>(`${this.apiUrl}/${accountId}/transactions/transfer`, data);
  }
}