package com.timbershop.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bill_item")
public class BillItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int billId;

    private int timberId;

    private double quantity;

    private double pricePerCft;

    private double amount;

    // Default constructor
    public BillItem() {
    }

    // Constructor
    public BillItem(int billId, int timberId, double quantity,
                    double pricePerCft, double amount) {

        this.billId = billId;
        this.timberId = timberId;
        this.quantity = quantity;
        this.pricePerCft = pricePerCft;
        this.amount = amount;
    }

    // Getters and Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getBillId() {
        return billId;
    }

    public void setBillId(int billId) {
        this.billId = billId;
    }

    public int getTimberId() {
        return timberId;
    }

    public void setTimberId(int timberId) {
        this.timberId = timberId;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getPricePerCft() {
        return pricePerCft;
    }

    public void setPricePerCft(double pricePerCft) {
        this.pricePerCft = pricePerCft;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "BillItem{" +
                "id=" + id +
                ", billId=" + billId +
                ", timberId=" + timberId +
                ", quantity=" + quantity +
                ", pricePerCft=" + pricePerCft +
                ", amount=" + amount +
                '}';
    }
}