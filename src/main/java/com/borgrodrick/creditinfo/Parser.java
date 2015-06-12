package com.borgrodrick.creditinfo;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Parser {

    Logger logger = LoggerFactory.getLogger(Parser.class);

    DateExtractor dateExtractor;

    private String date;

    List<String> allMatched = new ArrayList<String>();

    public Parser() {
        dateExtractor = new DateExtractor();
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
            while (it.hasNext()) {
                DataWord row = it.next();
                if (!(row.getDescription() == null || row.getDescription().isEmpty() || row.getDescription().equals(""))) {
                    if (row.getRegion().toLowerCase().equals("assets")) assets.add(row);
                    if (row.getRegion().toLowerCase().equals("equity and liabilities")) liabilities.add(row);
                    if (row.getRegion().toLowerCase().equals("income")) income.add(row);
                    if (row.getRegion().toLowerCase().equals("cashflow")) cashflow.add(row);
                }

            }


            Document doc = Jsoup.parse(input, "UTF-8", "http://creditinfo.com/");

            if (doc.text().toLowerCase().contains("financial statement")) {

                int index = doc.text().toLowerCase().indexOf("financial statements");
                String text = doc.text().substring(index);

                if (text.indexOf("table") != -1) {
                    text = text.substring(0, text.indexOf("table"));
                    date = dateExtractor.getDate(text);

                    logger.info("Date is : " + date);
                }

            }


            List<ReportItem> reportItemsAssets = processAssets(doc.select("table"), assets);

            //check the totals for the assets
            totalAssetMatch(reportItemsAssets, input.getName());

            List<ReportItem> reportItemsLiabilities = processLiabilities(doc.select("table"), liabilities);


            totalLiabilitiesMatch(reportItemsLiabilities, input.getName());


            List<ReportItem> reportItemsIncome = processIncome(doc.select("table"), income);
            List<ReportItem> reportItemsCashflow = processCashflow(doc.select("table"), cashflow);

            List<ReportItem> reportItems = new ArrayList<ReportItem>();


            logger.info("Data for file: " + input.getAbsolutePath());
            if (reportItemsAssets != null) {
                logger.info("  Assets:" + reportItemsAssets.size());
                reportItems.addAll(reportItemsAssets);
            }
            if (reportItemsLiabilities != null) {
                logger.info("  Liabilities:" + reportItemsLiabilities.size());
                reportItems.addAll(reportItemsLiabilities);
            }
            if (reportItemsIncome != null) {
                logger.info("  Income:" + reportItemsIncome.size());
                reportItems.addAll(reportItemsIncome);
            }
            if (reportItemsCashflow != null) {
                logger.info("    Cashflow:" + reportItemsCashflow.size());
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

            if (table.select("tr").get(firstNonEmptyCell).select("td").first().text().toLowerCase().contains("assets")) {
                return processTable(table, assetDataWords, "total assets", "asset");
            }
        }
        return null;
    }

    private List<ReportItem> processIncome(List<Element> tables, List<DataWord> incomeDataWords) {
        for (Element table : tables) {

            int firstNonEmptyCell = getFirstNonEmptyCell(0, table);

            if (table.select("tr").get(firstNonEmptyCell).select("td").first().text().toLowerCase().contains("income")) {
                return processTable(table, incomeDataWords, "", "income");
            } else if (table.select("tr").text().toLowerCase().contains("income")) {
                return processTable(table, incomeDataWords, "", "income");
            }
        }
        return null;
    }

    private List<ReportItem> processCashflow(List<Element> tables, List<DataWord> cashFlowWords) {
        for (Element table : tables) {

            int firstNonEmptyCell = getFirstNonEmptyCell(0, table);

            if (table.select("tr").get(firstNonEmptyCell).select("td").first().text().toLowerCase().contains("cashflow")) {
                return processTable(table, cashFlowWords, "", "cashflow");
            } else if (table.select("tr").get(firstNonEmptyCell).select("td").first().text().toLowerCase().startsWith("cash flow")) {
                return processTable(table, cashFlowWords, "", "cashflow");
            } else if (table.select("tr").text().toLowerCase().contains("cash flow")) {
                return processTable(table, cashFlowWords, "", "cashflow");
            } else if (table.select("tr").text().toLowerCase().contains("cashflow")) {
                return processTable(table, cashFlowWords, "", "cashflow");
            }
        }
        return null;
    }

    private List<ReportItem> processLiabilities(List<Element> tables, List<DataWord> liabilitiesDataWords) {
        for (Element table : tables) {

            int firstNonEmptyCell = getFirstNonEmptyCell(0, table);

            if (table.select("tr").get(firstNonEmptyCell).select("td").first().text().toLowerCase().contains("equity and liabilities")) {
                return processTable(table, liabilitiesDataWords, "total equity and liabilities", "liabilities");
            } else if (table.select("tr").text().toLowerCase().contains("equity and liabilities")) {
                return processTable(table, liabilitiesDataWords, "total equity and liabilities", "liabilities");
            }


        }
        return null;
    }

    private int getFirstNonEmptyCell(int counter, Element table) {
        if (table.select("tr").get(counter).select("td").first().text().trim().isEmpty()) {
            counter = getFirstNonEmptyCell(counter + 1, table);
        }

        return counter;
    }

    private List<ReportItem> processTable(Element table, List<DataWord> dataWords, String endOfTable, String type) {
        List<String> head = new ArrayList<String>();
        List<ReportItem> matched = new ArrayList<ReportItem>();

        List<String> notMatched = new ArrayList<String>();
        boolean isend = false;

        for (Element row : table.select("tr")) {

            Elements tds = row.select("td");

            String tdsString = tds.text();

            if (!endOfTable.isEmpty() && tdsString.toLowerCase().trim().contains(endOfTable)) {
                isend = true;
            }

            if (head.isEmpty()){
                head = dateExtractor.getAllDate(tdsString);
            }


//            //check for table head
//            if (tdsString.toLowerCase().contains("note")) {
//                head.clear();
//                for (Element td : tds) {
//                    if (td.text().toLowerCase().contains("current")) {
//                        head.add(date);
//                    } else {
//                        head.add(td.text());
//                    }
//
//                }
//                if (head.size() > 0) head.remove(0);
//            }


            //check for rest
            else {
                boolean isMatch = false;
                ReportItem reportItem = null;

                for (Element td : tds) {
                    if (!isMatch) {

                        Elements tdText = td.select("p");

                        List<String> tdTextData = new ArrayList<>();

                        for (Element e : tdText) {
                            tdTextData.add(e.text().toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", "").trim());
                        }

                        for (DataWord w : dataWords) {
                            String datawordString = w.Description.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", "").trim();
                            for (String textTrimmed : tdTextData) {
                                if (textTrimmed.equals(datawordString) || textTrimmed.startsWith(datawordString) ||textTrimmed.endsWith(datawordString)){
                                    reportItem = new ReportItem(textTrimmed);
                                    reportItem.setDataWord(w);
                                    allMatched.add(w.getDescription().toLowerCase());
                                    isMatch = true;
                                    reportItem.setTableHead(head);
                                }
                            }
                        }
                    } else if (isMatch) {

                        String value = "";

                        if (td.select("p").size() == 0){
                            value = td.text().trim().replaceAll("[a-zA-Z]", "");
                        }
                        else {
                            value = td.select("p").first().text().trim().replaceAll("[a-zA-Z]", "");
                        }

                        reportItem.addValue(value);
                    }

                    if (!isMatch && td.text().equals(tds.first().text()) && !tds.first().text().trim().isEmpty() && !allMatched.stream().anyMatch((s) -> s.toLowerCase().equals(td.text().toLowerCase()))) {
                        notMatched.add(td.text().toLowerCase());
                    }
                }


                if (reportItem != null && reportItem.getAllValues().size() > 0) {
                    matched.add(reportItem);
                    reportItem.populateMapping();
                }
            }

            if (isend) return matched;
        }

        logger.info("Not Matched for " + type + ": ");

        for (String s : notMatched)
            logger.info("  " + s);

        return matched;
    }

    private boolean totalAssetMatch(List<ReportItem> assets, String filename) {

        if (assets == null) return false;

        Optional<ReportItem> first = assets.stream().filter(x -> x.getDescription().trim().toLowerCase().contains("total assets")).findFirst();

        if (first.isPresent()) {
            HashMap<String, String> yearlyValues = first.get().getYearlyValues();

            List<HashMap<String, String>> rest = assets.stream().filter(x -> !x.getDescription().trim().toLowerCase().contains("total")).map(ReportItem::getYearlyValues).collect(Collectors.toList());


            List<TotalReport> reports = yearlyValues.keySet().stream().map(key -> {

                        TotalReport report = new TotalReport();
                        report.setYear(key);
                        report.setDateProcessed(new Date());
                        report.setDocumentName(filename);

                        Double yearSum = rest.stream().mapToDouble(others -> {
                            String restValue = others.getOrDefault(key, "0");
                            restValue = restValue.replaceAll("[^0-9]", "").trim();
                            if (restValue == null || restValue.isEmpty()) restValue = "0";
                            return Double.parseDouble(restValue);
                        }).sum();


                        String actualSumString = yearlyValues.get(key);
                        actualSumString = actualSumString.replaceAll("[^0-9]", "").trim();
                        if (actualSumString == null || actualSumString.isEmpty()) actualSumString = "0";
                        Double actualSum = Double.parseDouble(actualSumString);

                        report.setActualTotal(actualSum);
                        report.setCalculatedTotal(yearSum);
                        report.setMatched(Double.doubleToLongBits(actualSum) == Double.doubleToLongBits(yearSum));
                        report.setType("Assets");
                        return report;
                    }
            ).collect(Collectors.toList());
            appendToCSV(reports);
        }

        return false;
    }


    private void totalLiabilitiesMatch(List<ReportItem> liabilities, String filename) {

        if (liabilities == null) return;

        int totalEquityIndex = -1;
        int totalLiabilitiesIndex = -1;

        for (int i = 0; i < liabilities.size(); i++) {
            if (liabilities.get(i).getDescription().trim().toLowerCase().equals("total equity")) {
                totalEquityIndex = i;
            } else if (liabilities.get(i).getDescription().trim().toLowerCase().equals("total liabilities")) {
                totalLiabilitiesIndex = i;
            }
        }

        if (totalEquityIndex == -1) return;
        if (totalLiabilitiesIndex == -1) return;

        List<ReportItem> totalEquityList = liabilities.subList(0, totalEquityIndex + 1);
        List<ReportItem> totalLiabilitiesList = liabilities.subList(totalEquityIndex + 1, totalLiabilitiesIndex + 1);

        HashMap<String, String> equityYearlyValues = liabilities.get(totalEquityIndex).getYearlyValues();
        HashMap<String, String> liabilitiesYearlyValues = liabilities.get(totalLiabilitiesIndex).getYearlyValues();

        List<HashMap<String, String>> equityYearlyRest = totalEquityList.stream().filter(x -> !x.getDescription().trim().toLowerCase().contains("total")).map(ReportItem::getYearlyValues).collect(Collectors.toList());
        List<HashMap<String, String>> liabilitiesYearlyRest = totalLiabilitiesList.stream().filter(x -> !x.getDescription().trim().toLowerCase().contains("total")).map(ReportItem::getYearlyValues).collect(Collectors.toList());


        processEquityReport(equityYearlyValues,equityYearlyRest, filename );
        processLiabilitiesReport(liabilitiesYearlyValues,liabilitiesYearlyRest, filename );

    }

    private void processEquityReport(HashMap<String, String> equityYearlyValues, List<HashMap<String, String>> equityYearlyRest, String filename ){
        List<TotalReport> equityReports = equityYearlyValues.keySet().stream().map(key -> {

                    TotalReport report = new TotalReport();
                    report.setYear(key);
                    report.setDateProcessed(new Date());
                    report.setDocumentName(filename);

                    Double yearSum = equityYearlyRest.stream().mapToDouble(others -> {
                        String restValue = others.getOrDefault(key, "0");
                        restValue = restValue.replaceAll("[^0-9]", "").trim();
                        if (restValue == null || restValue.isEmpty()) restValue = "0";
                        return Double.parseDouble(restValue);
                    }).sum();


                    String actualSumString = equityYearlyValues.get(key);
                    actualSumString = actualSumString.replaceAll("[^0-9]", "").trim();
                    if (actualSumString == null || actualSumString.isEmpty()) actualSumString = "0";
                    Double actualSum = Double.parseDouble(actualSumString);

                    report.setActualTotal(actualSum);
                    report.setCalculatedTotal(yearSum);
                    report.setMatched(Double.doubleToLongBits(actualSum) == Double.doubleToLongBits(yearSum));
                    report.setType("Equity");
                    return report;
                }
        ).collect(Collectors.toList());

        appendToCSV(equityReports);
    }


    private void processLiabilitiesReport(HashMap<String, String> liabilitiesYearlyValues, List<HashMap<String, String>> liabilitiesYearlyRest, String filename){
        List<TotalReport> liabilitiesReports = liabilitiesYearlyValues.keySet().stream().map(key -> {

                    TotalReport report = new TotalReport();
                    report.setYear(key);
                    report.setDateProcessed(new Date());
                    report.setDocumentName(filename);

                    Double yearSum = liabilitiesYearlyRest.stream().mapToDouble(others -> {
                        String restValue = others.getOrDefault(key, "0");
                        restValue = restValue.replaceAll("[^0-9]", "").trim();
                        if (restValue == null || restValue.isEmpty()) restValue = "0";
                        return Double.parseDouble(restValue);
                    }).sum();


                    String actualSumString = liabilitiesYearlyValues.get(key);
                    actualSumString = actualSumString.replaceAll("[^0-9]", "").trim();
                    if (actualSumString == null || actualSumString.isEmpty()) actualSumString = "0";
                    Double actualSum = Double.parseDouble(actualSumString);

                    report.setActualTotal(actualSum);
                    report.setCalculatedTotal(yearSum);
                    report.setMatched(Double.doubleToLongBits(actualSum) == Double.doubleToLongBits(yearSum));
                    report.setType("liabilities");
                    return report;
                }
        ).collect(Collectors.toList());

        appendToCSV(liabilitiesReports);
    }



    private void appendToCSV(List<TotalReport>  reports){
        // create mapper and schema
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper.schemaFor(TotalReport.class);
        schema = schema.withColumnSeparator(',');

        try {
            // output writer
            ObjectWriter myObjectWriter = mapper.writer(schema);
            //File tempFile = new File("C:\\creditinfo\\report.csv");
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("C:\\creditinfo\\report.csv", true)));
            myObjectWriter.writeValue(out, reports);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
