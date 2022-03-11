package xfp.pdf.pojo;

import lombok.Data;

import java.util.List;

/**
 * @ClassName MarkPojo
 * @Description 对应配置文件中的json数据
 * @Author WANGHAN756
 * @Date 2021/7/13 10:17
 * @Version 1.0
 **/
@Data
public class MarkPojo {
    private List<TitlePattern> titlePatterns;

    @Data
    public static class TitlePattern{
        private Integer order;
        private Float level;
        private String bold;
        private String pattern;
        private String firstPattern;
    }
}
