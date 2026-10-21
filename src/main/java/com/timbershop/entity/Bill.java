package com.timbershop.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bill")
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int customerId;
    private LocalDateTime billDate;

    private double subtotal;
    private double discountAmount;
    private double gstRate;
    private double gstAmount;
    private double totalAmount;

    public Bill() {
    }

    public Bill(
            int customerId,
            LocalDateTime billDate,
            double totalAmount) {

        this.customerId = customerId;
        this.billDate = billDate;
        this.subtotal = totalAmount;
        this.discountAmount = 0.0;
        this.gstRate = 0.0;
        this.gstAmount = 0.0;
        this.totalAmount = totalAmount;
    }

    public Bill(
            int customerId,
            LocalDateTime billDate,
            double subtotal,
            double discountAmount,
            double gstRate,
            double gstAmount,
            double totalAmount) {

        this.customerId = customerId;
        this.billDate = billDate;
        this.subtotal = subtotal;
        this.discountAmount = discountAmount;
        this.gstRate = gstRate;
        this.gstAmount = gstAmount;
        this.totalAmount = totalAmount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public LocalDateTime getBillDate() {
        return billDate;
    }

    public void setBillDate(LocalDateTime billDate) {
        this.billDate = billDate;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public double getGstRate() {
        return gstRate;
    }

    public void setGstRate(double gstRate) {
        this.gstRate = gstRate;
    }

    public double getGstAmount() {
        return gstAmount;
    }

    public void setGstAmount(double gstAmount) {
        this.gstAmount = gstAmount;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    @Override
    public String toString() {
        return "Bill{" +
                "id=" + id +
                ", customerId=" + customerId +
                ", billDate=" + billDate +
                ", subtotal=" + subtotal +
                ", discountAmount=" + discountAmount +
                ", gstRate=" + gstRate +
                ", gstAmount=" + gstAmount +
                ", totalAmount=" + totalAmount +
                '}';
    }
}