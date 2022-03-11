package xfp.pdf.pojo;

import lombok.ToString;

/**
 * @ClassName Line
 * @Description 行状态，为了去掉页眉页脚和划分段落使用
 * @Author WANGHAN756
 * @Date 2021/6/16 13:40
 * @Version 1.0
 **/
@ToString
public enum LineStatus {
    Normal(0),
    ParaEnd(1),
    Footer(2),
    Header(3);

    LineStatus(int i) {

    }
}
