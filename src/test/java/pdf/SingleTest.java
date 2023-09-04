package pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.dom4j.Document;

import org.junit.Test;
import xfp.pdf.arrange.MarkPdf;
import xfp.pdf.arrange.PdfXml;
import xfp.pdf.core.PdfParser;
import xfp.pdf.pojo.ContentPojo;
import xfp.pdf.pojo.Tu;
import xfp.pdf.tools.FileTool;

import java.io.File;
import java.io.IOException;
import java.util.List;


public class SingleTest {

    static {
        System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
    }

    private static String inputFilePath = "E:\\年报\\海尔智家：海尔智家股份有限公司2020年年度报告.pdf";

    private static String outputFilePathDir = "E:\\年报\\output";

    private static String picSavePath = "E:\\pic";
    @Test
    public void parsingUnTaggedPdfWithTableDetectionAndPicture() throws IOException {
        long start = System.currentTimeMillis();
        PDDocument pdd = PDDocument.load(new File(inputFilePath));

        String s = parsingUnTaggedPdfWithTableDetectionAndPicture(pdd, inputFilePath, outputFilePathDir);
        long end = System.currentTimeMillis();
        System.out.println(end-start);
    }

    private static String parsingUnTaggedPdfWithTableDetectionAndPicture(PDDocument pdd,String fileName,String outputFileDir) throws IOException {
        ContentPojo contentPojo = PdfParser.parsingUnTaggedPdfWithTableDetectionAndPicture(pdd,picSavePath,true);

        FileTool.saveHTML(outputFileDir,contentPojo,fileName);
        return null;
    }

}
