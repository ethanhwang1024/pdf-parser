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

/**
 * @ClassName SingleTest
 * @Description untaggedPdf 单文件解析
 * @Author WANGHAN756
 * @Date 2021/6/23 17:44
 * @Version 1.0
 **/
public class SingleTest {

    static {
        System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
    }

    private static String inputFilePath = "F:\\notebook\\年报\\海尔智家：海尔智家股份有限公司2020年年度报告.pdf";

    private static String outputFilePathDir = "F:\\notebook\\年报\\output";


    @Test
    public void parsingUnTaggedPdfWithTableDetectionAndPicture() throws IOException {
        long start = System.currentTimeMillis();
        PDDocument pdd = PDDocument.load(new File(inputFilePath));

        String s = parsingUnTaggedPdfWithTableDetectionAndPicture(pdd, inputFilePath, outputFilePathDir);
        long end = System.currentTimeMillis();
        System.out.println(end-start);
    }

    private static String parsingUnTaggedPdfWithTableDetectionAndPicture(PDDocument pdd,String fileName,String outputFileDir) throws IOException {
        ContentPojo contentPojo = PdfParser.parsingUnTaggedPdfWithTableDetectionAndPicture(pdd,"D:/pic");

        FileTool.saveHTML(outputFileDir,contentPojo,fileName);
//
//        //null表示使用默认配置文件
//        MarkPdf.markTitle(contentPojo,null);

//        Document doc = PdfXml.buildXml(contentPojo);
//        FileTool.saveXML(outputFileDir,doc,fileName);

//
//        //extractPojo为null读取默认配置文件
//        List<Tu.Tuple2<String, String>> extract = PdfXml.extract(doc, contentPojo,null);
//        for(Tu.Tuple2<String, String> tu:extract){
//            String key = tu.getKey();
//            String value = tu.getValue();
//            System.out.println("===========q============");
//            System.out.println(key);
//            System.out.println("===========a============");
//            System.out.println(value);
//        }
//
//
//        String json = FileTool.saveJson(outputFileDir, contentPojo, fileName);
//        FileTool.saveHTML(outputFileDir,contentPojo,fileName);
//        FileTool.saveText(outputFileDir,contentPojo,fileName);
//
//        return json;
        return null;
    }

}
