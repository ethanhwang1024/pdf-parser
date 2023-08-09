package xfp.pdf.run;

import org.apache.pdfbox.pdmodel.PDDocument;
import xfp.pdf.arrange.MarkPdf;
import xfp.pdf.core.PdfParser;
import xfp.pdf.pojo.ContentPojo;
import xfp.pdf.tools.FileTool;

import java.io.File;
import java.io.IOException;

import java.util.concurrent.*;

public class Pdf2html {

    public static void main(String[] args) throws IOException {

        File file = new File(Path.inputAllPdfPath);
        File[] files = file.listFiles();
        ExecutorService executor = Executors.newFixedThreadPool(8); // 创建一个线程池，这里的5表示最多同时运行5个线程

        for (File f : files) {
            executor.execute(() -> { // 提交一个任务给线程池
                PDDocument pdd = null;
                try {
                    pdd = PDDocument.load(f);
                    ContentPojo contentPojo = PdfParser.parsingUnTaggedPdfWithTableDetection(pdd);
                    MarkPdf.markTitleSep(contentPojo);
                    FileTool.saveHTML(Path.outputAllHtmlPath, contentPojo, f.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    try {
                        pdd.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }
}