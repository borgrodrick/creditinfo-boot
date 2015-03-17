package com.borgrodrick.creditinfo;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "reportItems")
public class ReportItems {

    @XmlElement(name = "reportItem", type = ReportItem.class)
    private List<ReportItem> reportItems = new ArrayList<ReportItem>();

    public ReportItems() {}

    public ReportItems(List<ReportItem> reportItems) {
        this.reportItems = reportItems;
    }

    public List<ReportItem> getReportItems() {
        return reportItems;
    }

    public void setReportItems(List<ReportItem> reportItems) {
        this.reportItems = reportItems;
    }
}
