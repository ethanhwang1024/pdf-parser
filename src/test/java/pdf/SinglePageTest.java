package pdf;

import org.apache.pdfbox.pdmodel.PDDocument;

import org.junit.Test;
import xfp.pdf.core.UnTaggedAnalyser;
import xfp.pdf.core.UnTaggedContext;
import xfp.pdf.pojo.ContentPojo;
import xfp.pdf.tools.FileTool;

import java.io.File;
import java.io.IOException;
import java.util.List;


public class SinglePageTest {
    static {
        System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
    }

    private static String inputFilePath = "F:\\notebook\\年报\\page.pdf";

    @Test
    public void parsePage() throws IOException {
        PDDocument pdd = PDDocument.load(new File(inputFilePath));

        UnTaggedContext untaggedContext = new UnTaggedContext();
        //PreHeat 6页的信息
        untaggedContext.preHeat(pdd,6);

        ContentPojo contentPojo = new ContentPojo();
        List<ContentPojo.contentElement> page = UnTaggedAnalyser.parsePage(pdd, 1, untaggedContext,"D:/",true);
        contentPojo.setOutList(page);
        FileTool.saveHTML("F:\\notebook\\年报\\output",contentPojo,"test");
        System.out.println(page);
    }
}
