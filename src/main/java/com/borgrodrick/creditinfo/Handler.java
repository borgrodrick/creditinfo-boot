package com.borgrodrick.creditinfo;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class Handler {

    Logger logger = LoggerFactory.getLogger(Handler.class);

    public Handler(){
        parser = new Parser();
    }

    Parser parser;

    public File handleFile(File input) {


        logger.info("Processing file: " + input.getAbsolutePath());

        List<ReportItem> reportItems = parser.parse(input);

        String filePath = FilenameUtils.getBaseName(input.getName());

        int lastIndexOf = filePath.lastIndexOf("_0");

        filePath = lastIndexOf == -1 ? filePath + ".xml"  : filePath.substring(0, lastIndexOf) + ".xml";

        File output = JAXBXMLHandler.marshalToFile(reportItems, new File(filePath));

        logger.info("Finished file: " + input.getAbsolutePath());

        return output;
    }
}
