package com.securebank.securebank.repo;

import com.securebank.securebank.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t " +
           "LEFT JOIN FETCH t.senderAccount " +
           "LEFT JOIN FETCH t.receiverAccount " +
           "WHERE t.senderAccount.id = :accountId " +
           "OR t.receiverAccount.id = :accountId " +
           "ORDER BY t.createdAt DESC")
    List<Transaction> findAllByAccountId(@Param("accountId") Long accountId);
}