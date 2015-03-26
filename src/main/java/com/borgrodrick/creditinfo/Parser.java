package com.borgrodrick.creditinfo;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Parser {

    Logger logger = LoggerFactory.getLogger(Parser.class);
    
    DateExtractor dateExtractor;

    private String date;

    List<String> allMatched = new ArrayList<String>();

    public Parser (){
        dateExtractor =  new DateExtractor();
    }

    public List<ReportItem> parse(File input) {

        List<DataWord> assets = new ArrayList<DataWord>();
        List<DataWord> liabilities = new ArrayList<DataWord>();
        List<DataWord> income = new ArrayList<DataWord>();
        List<DataWord> cashflow = new ArrayList<DataWord>();

        File csvFile = new File("C:\\creditinfo\\datawords.csv");
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = CsvSchema.emptySchema().withHeader().withColumnSeparator(',');

        try {

            MappingIterator<DataWord> it = mapper.reader(DataWord.class).with(schema).readValues(csvFile);
            while  (it.hasNext()) {
                DataWord row = it.next();
                if (!(row.getDescription() == null || row.getDescription().isEmpty() || row.getDescription().equals(""))) {
                    if (row.getRegion().toLowerCase().equals("assets")) assets.add(row);
                    if (row.getRegion().toLowerCase().equals("equity and liabilities")) liabilities.add(row);
                    if (row.getRegion().toLowerCase().equals("income")) income.add(row);
                    if (row.getRegion().toLowerCase().equals("cashflow")) cashflow.add(row);
                }

            }


            Document doc = Jsoup.parse(input, "UTF-8", "http://creditinfo.com/");

            if (doc.text().toLowerCase().contains("financial statement")){

                int index = doc.text().toLowerCase().indexOf("financial statements");
                String text = doc.text().substring(index);

                if ( text.indexOf("table") != -1){
                    text = text.substring(0, text.indexOf("table"));
                    date = dateExtractor.getDate(text);

                    logger.info("Date is : "  + date);
                }

            }



            List<ReportItem> reportItemsAssets = processAssets(doc.select("table"), assets);
            List<ReportItem> reportItemsLiabilities = processLiabilities(doc.select("table"), liabilities);
            List<ReportItem> reportItemsIncome = processIncome(doc.select("table"), income);
            List<ReportItem> reportItemsCashflow = processCashflow(doc.select("table"), cashflow);

            List<ReportItem> reportItems = new ArrayList<ReportItem>();


            logger.info("Data for file: " + input.getAbsolutePath() );
            if (reportItemsAssets!= null){
                logger.info("  Assets:" + reportItemsAssets.size());
                reportItems.addAll(reportItemsAssets);
            }
            if (reportItemsLiabilities!= null){
                logger.info("  Liabilities:"+reportItemsLiabilities.size());
                reportItems.addAll(reportItemsLiabilities);
            }
            if (reportItemsIncome!= null){
                logger.info("  Income:"+reportItemsIncome.size());
                reportItems.addAll(reportItemsIncome);
            }
            if (reportItemsCashflow!= null){
                logger.info("    Cashflow:"+reportItemsCashflow.size());
                reportItems.addAll(reportItemsCashflow);
            }



            return reportItems;


        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private List<ReportItem> processAssets(List<Element> tables, List<DataWord> assetDataWords) {
        for (Element table : tables) {

            int firstNonEmptyCell = getFirstNonEmptyCell(0, table);

            if (table.select("tr").get(firstNonEmptyCell).select("td").first().text().toLowerCase().contains("assets")){
                return processTable(table,assetDataWords,"total assets", "asset");
            }
        }
        return null;
    }

    private List<ReportItem> processIncome(List<Element> tables, List<DataWord> incomeDataWords) {
        for (Element table : tables) {

            int firstNonEmptyCell = getFirstNonEmptyCell(0, table);

            if (table.select("tr").get(firstNonEmptyCell).select("td").first().text().toLowerCase().contains("income")){
                return processTable(table,incomeDataWords,"", "income");
            }

            else if (table.select("tr").text().toLowerCase().contains("income")){
                return processTable(table, incomeDataWords, "", "income");
            }
        }
        return null;
    }

    private List<ReportItem> processCashflow(List<Element> tables, List<DataWord> cashFlowWords) {
        for (Element table : tables) {

            int firstNonEmptyCell = getFirstNonEmptyCell(0, table);

            if (table.select("tr").get(firstNonEmptyCell).select("td").first().text().toLowerCase().contains("cashflow")){
                return processTable(table,cashFlowWords,"", "cashflow");
            }

            else if (table.select("tr").get(firstNonEmptyCell).select("td").first().text().toLowerCase().startsWith("cash flow")){
                return processTable(table,cashFlowWords,"", "cashflow");
            }

            else if (table.select("tr").text().toLowerCase().contains("cash flow")){
                return processTable(table, cashFlowWords, "", "cashflow");
            }

            else if (table.select("tr").text().toLowerCase().contains("cashflow")){
                return processTable(table, cashFlowWords, "", "cashflow");
            }
        }
        return null;
    }

    private List<ReportItem> processLiabilities(List<Element> tables, List<DataWord> liabilitiesDataWords) {
        for (Element table : tables) {

            int firstNonEmptyCell = getFirstNonEmptyCell(0, table);

            if (table.select("tr").get(firstNonEmptyCell).select("td").first().text().toLowerCase().contains("equity and liabilities")){
                return processTable(table,liabilitiesDataWords,"total equity and liabilities", "liabilities");
            }

            else if (table.select("tr").text().toLowerCase().contains("equity and liabilities")){
                return processTable(table, liabilitiesDataWords, "total equity and liabilities", "liabilities");
            }


        }
        return null;
    }

    private int getFirstNonEmptyCell(int counter, Element table ){
        if (table.select("tr").get(counter).select("td").first().text().trim().isEmpty()){
            counter = getFirstNonEmptyCell(counter+1, table);
        }

        return counter;
    }

    private List<ReportItem> processTable(Element table, List<DataWord> dataWords, String endOfTable, String type){
        List<String> head = new ArrayList<String>();
        List<ReportItem> matched = new ArrayList<ReportItem>();

        List<String> notMatched = new ArrayList<String>();

        for (Element row : table.select("tr")) {

            Elements tds = row.select("td");

            String tdsString = tds.text();

            if (!endOfTable.isEmpty() && tdsString.toLowerCase().trim().equals(endOfTable)){
                return matched;
            }

            //check for table head
            if (tdsString.toLowerCase().contains("note")) {
                head.clear();
                for (Element td : tds) {
                    if (td.text().toLowerCase().contains("current")){
                        head.add(date);
                    }

                    else {
                        head.add(td.text());
                    }

                }
                if (head.size() > 0) head.remove(0);
            }
            //check for rest
            else {
                boolean isMatch = false;
                ReportItem reportItem = null;

                for (Element td : tds) {
                    if (!isMatch && td.text().equals(tds.first().text())) {
                        for (DataWord w : dataWords) {
                            String textTrimmed = td.text().toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", "").trim();
                            if (textTrimmed.equals(w.Description.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", "").trim())) {
                                reportItem = new ReportItem(td.text());
                                reportItem.setDataWord(w);
                                allMatched.add(w.getDescription().toLowerCase());
                                isMatch = true;
                                reportItem.setTableHead(new ArrayList<String>(head));
                            }

                        }
                    } else if (isMatch){
                        reportItem.addValue(td.text().trim().replaceAll("[a-zA-Z]", "").toLowerCase());
                    }

                    if (!isMatch&& td.text().equals(tds.first().text()) && !tds.first().text().trim().isEmpty() && !allMatched.stream().anyMatch((s) -> s.toLowerCase().equals(td.text().toLowerCase()))) {
                        notMatched.add(td.text().toLowerCase());
                    }
                }


                if (reportItem != null && reportItem.getAllValues().size() > 0) {
                    matched.add(reportItem);
                    reportItem.populateMapping();
                }
            }
        }

        logger.info("Not Matched for "+ type +": ");

        for (String s : notMatched)
            logger.info ( "  " +s );

        return matched;
    }
}