package xfp.pdf.thirdparty;

import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.rendering.PageDrawer;
import org.apache.pdfbox.rendering.PageDrawerParameters;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName CustomPageDrawer
 * @Description 继承PageDrawer的子类，做了部分修改以用于获得表格
 * @Author WANGHAN756
 * @Date 2021/6/2 11:19
 * @Version 1.0
 **/
public class CustomPageDrawer extends PageDrawer {

    private List<Shape> tableLines = new ArrayList<>();

    public List<Shape> getTableLines() {
        return tableLines;
    }

    public void setTableLines(List<Shape> tableLines) {
        this.tableLines = tableLines;
    }

    CustomPageDrawer(PageDrawerParameters parameters) throws IOException
    {
        super(parameters);
    }

    /**
     * Color replacement.
     */
    @Override
    protected Paint getPaint(PDColor color) throws IOException
    {
        // if this is the non-stroking color
        if (getGraphicsState().getNonStrokingColor() == color)
        {
            // find red, ignoring alpha channel
            if (color.toRGB() == (Color.RED.getRGB() & 0x00FFFFFF))
            {
                // replace it with blue
                return Color.BLUE;
            }
        }
        return super.getPaint(color);
    }

    /**
     * Glyph bounding boxes.
     */
    @Override
    protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, String unicode,
                             Vector displacement) throws IOException
    {
        // draw glyph
        super.showGlyph(textRenderingMatrix, font, code, unicode, displacement);

        // bbox in EM -> user units
        Shape bbox = new Rectangle2D.Float(0, 0, font.getWidth(code) / 1000, 1);
        AffineTransform at = textRenderingMatrix.createAffineTransform();
        bbox = at.createTransformedShape(bbox);

        // save
        Graphics2D graphics = getGraphics();
        Color color = graphics.getColor();
        Stroke stroke = graphics.getStroke();
        Shape clip = graphics.getClip();

        // draw
        graphics.setClip(graphics.getDeviceConfiguration().getBounds());
        graphics.setColor(Color.BLACK);
        graphics.setStroke(new BasicStroke(.5f));
        graphics.draw(bbox);

        // restore
        graphics.setStroke(stroke);
        graphics.setColor(color);
        graphics.setClip(clip);
    }

    /**
     * Filled path bounding boxes.
     */
    @Override
    public void fillPath(int windingRule) throws IOException
    {
        //printPath();
        //System.out.printf("Fill; windingrule: %s\n\n", windingRule);
        //getLinePath().reset();

        // bbox in user units
        Shape bbox = getLinePath().getBounds2D();

        if(bbox.getBounds2D().getWidth()>1.0f||bbox.getBounds2D().getHeight()>1.0f){
//            System.out.print("fillpath:");
//            System.out.println(bbox);
            tableLines.add(bbox);
        }



        // draw path (note that getLinePath() is now reset)
        super.fillPath(windingRule);

        // save
        Graphics2D graphics = getGraphics();
        Color color = graphics.getColor();
        Stroke stroke = graphics.getStroke();
        Shape clip = graphics.getClip();

        // draw
        graphics.setClip(graphics.getDeviceConfiguration().getBounds());
        graphics.setColor(Color.RED);
        graphics.setStroke(new BasicStroke(.5f));
        if(bbox.getBounds2D().getWidth()>1.0f||bbox.getBounds2D().getHeight()>1.0f){
            graphics.draw(bbox);
        }


        // restore
        graphics.setStroke(stroke);
        graphics.setColor(color);
        graphics.setClip(clip);

        getLinePath().reset();
    }

    @Override
    public void strokePath() throws IOException
    {
        //printPath();
        //System.out.printf("Stroke; unscaled width: %s\n\n", getGraphicsState().getLineWidth());
        // bbox in user units
        Shape bbox = getLinePath().getBounds2D();

        // draw path (note that getLinePath() is now reset)
        // super.fillPath(windingRule);

        // save
        Graphics2D graphics = getGraphics();
        Color color = graphics.getColor();
        Stroke stroke = graphics.getStroke();
        Shape clip = graphics.getClip();

        // draw
        graphics.setClip(graphics.getDeviceConfiguration().getBounds());
        graphics.setColor(Color.RED);
        graphics.setStroke(new BasicStroke(.5f));
        graphics.draw(bbox);

        // restore
        graphics.setStroke(stroke);
        graphics.setColor(color);
        graphics.setClip(clip);

        getLinePath().reset();
    }

    /**
     * Custom annotation rendering.
     */
    @Override
    public void showAnnotation(PDAnnotation annotation) throws IOException
    {
        // save
        saveGraphicsState();
        // 35% alpha
        getGraphicsState().setNonStrokeAlphaConstants(0.35);
        super.showAnnotation(annotation);
        // restore
        restoreGraphicsState();
    }

    int countlines = 0;

    void printPath()
    {

        GeneralPath path = getLinePath();
        PathIterator pathIterator = path.getPathIterator(null);

        double x = 0, y = 0;
        double coords[] = new double[6];
        while (!pathIterator.isDone()) {
            countlines ++;
            switch (pathIterator.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO:
                    System.out.printf("Move to (%s %s)\n", coords[0], (coords[1]));
                    x = coords[0];
                    y = coords[1];
                    break;
                case PathIterator.SEG_LINETO:
                    double width = getEffectiveWidth(coords[0] - x, coords[1] - y);
                    System.out.printf("Line to (%s %s), scaled width %s\n", coords[0], (coords[1]), width);
                    x = coords[0];
                    y = coords[1];
                    break;
                case PathIterator.SEG_QUADTO:
                    System.out.printf("Quad along (%s %s) and (%s %s)\n", coords[0], (coords[1]), coords[2], (coords[3]));
                    x = coords[2];
                    y = coords[3];
                    break;
                case PathIterator.SEG_CUBICTO:
                    System.out.printf("Cubic along (%s %s), (%s %s), and (%s %s)\n", coords[0], (coords[1]), coords[2], (coords[3]), coords[4], (coords[5]));
                    x = coords[4];
                    y = coords[5];
                    break;
                case PathIterator.SEG_CLOSE:
                    System.out.println("Close path");
            }
            pathIterator.next();
        }
        System.out.printf("\n\n..............countlines:%d",countlines);
    }

    double getEffectiveWidth(double dirX, double dirY)
    {
        if (dirX == 0 && dirY == 0)
            return 0;
        Matrix ctm = getGraphicsState().getCurrentTransformationMatrix();
        double widthX = dirY;
        double widthY = -dirX;
        double widthXTransformed = widthX * ctm.getValue(0, 0) + widthY * ctm.getValue(1, 0);
        double widthYTransformed = widthX * ctm.getValue(0, 1) + widthY * ctm.getValue(1, 1);
        double factor = Math.sqrt((widthXTransformed*widthXTransformed + widthYTransformed*widthYTransformed) / (widthX*widthX + widthY*widthY));
        return getGraphicsState().getLineWidth() * factor;
    }
}
