package pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import xfp.pdf.arrange.MarkPdf;
import xfp.pdf.core.PdfParser;
import xfp.pdf.pojo.ContentPojo;
import xfp.pdf.tools.FileTool;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Pdf2html {

    //所有的pdf的位置
    public static String inputAllPdfPath = "/root/test/allpdf";
    //所有pdf转成的html的位置
    public static String outputAllHtmlPath = "/root/test/allhtml";

    public static void main(String[] args) throws IOException {

        File file = new File(inputAllPdfPath);
        File[] files = file.listFiles();

        // 创建线程池获得多线程支持
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);

        for (File f : files) {
            threadPool.execute(() -> {
                processPdfFile(f);
            });
        }

        threadPool.shutdown();
    }

    private static void processPdfFile(File pdfFile) {
        PDDocument pdd = null;
        try {
            pdd = PDDocument.load(pdfFile);
            ContentPojo contentPojo = PdfParser.parsingUnTaggedPdfWithTableDetection(pdd, true);
            MarkPdf.markTitleSep(contentPojo);
            FileTool.saveHTML(outputAllHtmlPath, contentPojo, pdfFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (pdd != null) {
                try {
                    pdd.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
