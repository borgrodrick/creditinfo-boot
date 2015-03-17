package com.borgrodrick.creditinfo;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class JAXBXMLHandler {

    // Export
    public static void marshal(List<ReportItem> reportItems, File selectedFile)
            throws IOException, JAXBException {
        JAXBContext context;
        BufferedWriter writer = null;
        writer = new BufferedWriter(new FileWriter(selectedFile));
        context = JAXBContext.newInstance(ReportItems.class);
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(new ReportItems(reportItems), writer);
        writer.close();
    }

    // Export
    public static File marshalToFile(List<ReportItem> reportItems, File selectedFile) {
        try{
        JAXBContext context = JAXBContext.newInstance(ReportItems.class);
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(new ReportItems(reportItems), selectedFile);
        return selectedFile;
        } catch (JAXBException j){
            System.out.println("Error parsing file " + selectedFile.getAbsolutePath());
            return selectedFile;
        }

    }
}
