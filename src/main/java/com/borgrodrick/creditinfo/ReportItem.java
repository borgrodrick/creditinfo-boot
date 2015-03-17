package com.borgrodrick.creditinfo;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "reportItem")
public class ReportItem {


    private String description;

    @XmlTransient
    private List<String> value;
    @XmlTransient
    private List <String> tableHead;


    private HashMap<String, String> yearlyValues;


    private String noteId;

    private DataWord dataWord;


    public ReportItem() {

    }

    public ReportItem(String description) {
        this.description = description;
        this.value = new ArrayList<String>();
        this.yearlyValues = new HashMap<String, String>();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void addValue(String value){
        this.value.add(value);
    }

    public List<String> getAllValues(){
        return this.value;
    }

    public List<String> getTableHead() {
        return tableHead;
    }

    public void setTableHead(List<String> tableHead) {
        this.tableHead = tableHead;
    }

    public void populateMapping(){
        if (tableHead== null || value == null || tableHead.size() ==0 || value.size() ==0 )
            return;

        if (tableHead.size() == value.size()){
            for (int i = 0; i < tableHead.size(); i++) {
                yearlyValues.put(tableHead.get(i).replaceAll("[^\\w\\s]","").trim(), value.get(i).replaceAll("[^\\w\\s]","").trim());
            }
        }

        if (tableHead.get(0).toLowerCase().contains("note")){
            this.noteId = value.get(0);
            if (yearlyValues.containsKey(tableHead.get(0))){
                yearlyValues.remove(tableHead.get(0));
            }
        }
    }

    public DataWord getDataWord() {
        return dataWord;
    }

    public void setDataWord(DataWord dataWord) {
        this.dataWord = dataWord;
    }
}
