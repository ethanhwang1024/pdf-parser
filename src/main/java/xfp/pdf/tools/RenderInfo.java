package xfp.pdf.tools;

import lombok.Data;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;

/**
 * @ClassName RenderInfo
 * @Description 渲染信息，这些信息可以被用来判断字体粗体等信息，比如RenderingMode
 * @Author WANGHAN756
 * @Date 2021/6/18 14:11
 * @Version 1.0
 **/
@Data
public class RenderInfo {
    private Integer pageNum;
    private Float lineWidth;
    private RenderingMode renderingMode;

    @Override
    public String toString() {
        return "{" +
                "lineWidth=" + lineWidth +
                ", renderingMode=" + renderingMode +
                '}';
    }
}
