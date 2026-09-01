package com.northgate.ratings.domain;

import java.io.Serializable;

public class Rating implements Serializable {

    private static final long serialVersionUID = 20190412L;

    private String issuerId;
    private String issuerName;
    private String grade;
    private String outlook;
    private String sector;
    private String lastReviewed;

    public Rating() {
    }

    public Rating(String issuerId, String issuerName, String grade, String outlook, String sector,
                  String lastReviewed) {
        this.issuerId = issuerId;
        this.issuerName = issuerName;
        this.grade = grade;
        this.outlook = outlook;
        this.sector = sector;
        this.lastReviewed = lastReviewed;
    }

    public String getIssuerId() {
        return issuerId;
    }

    public void setIssuerId(String issuerId) {
        this.issuerId = issuerId;
    }

    public String getIssuerName() {
        return issuerName;
    }

    public void setIssuerName(String issuerName) {
        this.issuerName = issuerName;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getOutlook() {
        return outlook;
    }

    public void setOutlook(String outlook) {
        this.outlook = outlook;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getLastReviewed() {
        return lastReviewed;
    }

    public void setLastReviewed(String lastReviewed) {
        this.lastReviewed = lastReviewed;
    }
}
