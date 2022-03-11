package xfp.pdf.thirdparty;

import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.contentstream.operator.DrawObject;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.PDFStreamEngine;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.contentstream.operator.state.Concatenate;
import org.apache.pdfbox.contentstream.operator.state.Restore;
import org.apache.pdfbox.contentstream.operator.state.Save;
import org.apache.pdfbox.contentstream.operator.state.SetGraphicsStateParameters;
import org.apache.pdfbox.contentstream.operator.state.SetMatrix;
import xfp.pdf.pojo.Tu;

import javax.imageio.ImageIO;

/**
 * This is an example on how to get the x/y coordinates of image location and size of image.
 */
public class GetImageEngine extends PDFStreamEngine
{
    public int imageNumber = 1;
    private String picSavePath;

    private final List<Tu.Tuple2<Integer,Rectangle2D.Float>> pics = new ArrayList<>();

    public List<Tu.Tuple2<Integer,Rectangle2D.Float>> getPics(){
        return pics;
    }
    public void clearList(){
        pics.clear();
    }

    public void setPicSavePath(String picSavePath) {
        this.picSavePath = picSavePath;
    }
    public String getPicSavePath(){
        return picSavePath;
    }

    /**
     * @throws IOException If there is an error loading text stripper properties.
     */
    public GetImageEngine(String picSavePath) throws IOException
    {
        this.picSavePath = picSavePath;
        // preparing PDFStreamEngine
        addOperator(new Concatenate());
        addOperator(new DrawObject());
        addOperator(new SetGraphicsStateParameters());
        addOperator(new Save());
        addOperator(new Restore());
        addOperator(new SetMatrix());
    }

    /**
     * @param operator The operation to perform.
     * @param operands The list of arguments.
     *
     * @throws IOException If there is an error processing the operation.
     */
    @Override
    protected void processOperator( Operator operator, List<COSBase> operands) throws IOException
    {
        String operation = operator.getName();
        if( "Do".equals(operation) )
        {
            COSName objectName = (COSName) operands.get( 0 );
            // get the PDF object
            PDXObject xobject = getResources().getXObject( objectName );
            // check if the object is an image object
            if( xobject instanceof PDImageXObject)
            {
                PDImageXObject image = (PDImageXObject)xobject;
                int imageWidth = image.getWidth();
                int imageHeight = image.getHeight();

                Matrix ctmNew = getGraphicsState().getCurrentTransformationMatrix();
//                float imageXScale = ctmNew.getScalingFactorX();
//                float imageYScale = ctmNew.getScalingFactorY();
//                // position of image in the pdf in terms of user space units
//                System.out.println("position in PDF = " + ctmNew.getTranslateX() + ", " + ctmNew.getTranslateY() + " in user space units");
//                // raw size in pixels
//                System.out.println("raw image size  = " + imageWidth + ", " + imageHeight + " in pixels");
//                // displayed size in user space units
//                System.out.println("displayed size  = " + imageXScale + ", " + imageYScale + " in user space units");

                // same image to local
                BufferedImage bImage = image.getImage();
                ImageIO.write(bImage,"PNG",new File(picSavePath + "/"+imageNumber+".png"));
                System.out.println("Image saved.");
                pics.add(new Tu.Tuple2<>(imageNumber,new Rectangle2D.Float(ctmNew.getTranslateX(), ctmNew.getTranslateY(), imageWidth, imageHeight)));


                imageNumber++;
            }
            else if(xobject instanceof PDFormXObject)
            {
                PDFormXObject form = (PDFormXObject)xobject;
                showForm(form);
            }
        }
        else
        {
            super.processOperator( operator, operands);
        }
    }

}