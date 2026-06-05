import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AccountService {

  private apiUrl = `${environment.apiUrl}/accounts`;

  constructor(private http: HttpClient) {}

  getMyAccounts() {
    return this.http.get<any[]>(this.apiUrl);
  }

  getAccountById(id: number) {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }

  createAccount(data: any) {
    return this.http.post<any>(this.apiUrl, data);
  }
}