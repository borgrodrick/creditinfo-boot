package com.borgrodrick.creditinfo;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Date;

@JsonPropertyOrder({"documentName","year","actualTotal","calculatedTotal","matched","dateProcessed"})
public class TotalReport {

    public  TotalReport (){}


    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public Double getActualTotal() {
        return actualTotal;
    }

    public void setActualTotal(Double actualTotal) {
        this.actualTotal = actualTotal;
    }

    public Double getCalculatedTotal() {
        return calculatedTotal;
    }

    public void setCalculatedTotal(Double calculatedTotal) {
        this.calculatedTotal = calculatedTotal;
    }

    public boolean isMatched() {
        return matched;
    }

    public void setMatched(boolean matched) {
        this.matched = matched;
    }

    @JsonSerialize(using=JsonDateSerializer.class)
    public Date getDateProcessed() {
        return dateProcessed;
    }

    public void setDateProcessed(Date dateProcessed) {
        this.dateProcessed = dateProcessed;
    }

    public String documentName;
    public String year;
    public Double actualTotal;
    public Double calculatedTotal;
    public boolean matched;
    public Date dateProcessed;
}
