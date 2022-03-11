package pdf;


import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.Test;
import xfp.pdf.core.PdfParser;
import xfp.pdf.pojo.BoldStatus;
import xfp.pdf.pojo.ContentPojo;
import xfp.pdf.pojo.SearchPattern;
import xfp.pdf.pojo.Tu;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Application {
    static {
        System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
    }

    private static String inputFilePath = "F:\\notebook\\年报\\page.pdf";

    private static String outputFilePathDir = "F:\\notebook\\年报\\output";


    @Test
    public void parsingUnTaggedPdfWithTableDetection() throws IOException {
        PDDocument pdd = PDDocument.load(new File(inputFilePath));
        parsingUnTaggedPdfWithTableDetection(pdd, inputFilePath, outputFilePathDir);
    }


    private static List<Tu.Tuple3<String,Double,Double>> cellsToList(List<ContentPojo.contentElement.InnerCell> cells){
        List<Tu.Tuple3<String,Double,Double>> list = new ArrayList<>();
        int pos = 0;
        Tu.Tuple3<String, Double, Double> tuple3 = new Tu.Tuple3<>();
        for(ContentPojo.contentElement.InnerCell cell:cells){
            Integer rowIndex = cell.getRow_index();
            if(rowIndex!=pos){
                tuple3 = new Tu.Tuple3<>();
                tuple3.setValue1(cell.getText().replaceAll("\r\n","").trim());
            }
            Integer col_index = cell.getCol_index();
            if(rowIndex!=1&&col_index==3){
                tuple3.setValue2(toDouble(cell.getText()));
            }else if(rowIndex!=1&&col_index==4){
                tuple3.setValue3(toDouble(cell.getText()));
                list.add(tuple3);
            }
            pos = rowIndex;
        }
        return list;
    }

    private static Map<String,Tu.Tuple2<Double,Double>> cellsToMap(List<ContentPojo.contentElement.InnerCell> cells){
        Map<String,Tu.Tuple2<Double,Double>> resultMap = new HashMap<String,Tu.Tuple2<Double,Double>>();
        int pos = 0;
        String prj = "";
        Tu.Tuple2<Double,Double> values = new Tu.Tuple2<>();
        for(ContentPojo.contentElement.InnerCell cell:cells){
            Integer rowIndex = cell.getRow_index();
            if(rowIndex!=pos){
                prj = cell.getText().replaceAll("\r\n","").trim();
                values = new Tu.Tuple2<>();
            }
            Integer col_index = cell.getCol_index();
            if(rowIndex!=1&&col_index==3){
                values.setKey(toDouble(cell.getText()));
            }else if(rowIndex!=1&&col_index==4){
                values.setValue(toDouble(cell.getText()));
                resultMap.put(prj,values);
            }
            pos = rowIndex;
        }
        return resultMap;
    }

    private static void parsingUnTaggedPdfWithTableDetection(PDDocument pdd,String fileName,String outputFileDir) throws IOException {
        ContentPojo contentPojo = PdfParser.parsingUnTaggedPdfWithTableDetection(pdd);

        List<SearchPattern> searchPatterns = new ArrayList<>();
        searchPatterns.add(new SearchPattern("合并资产负债表", BoldStatus.FullBord));
        searchPatterns.add(new SearchPattern("母公司资产负债表", BoldStatus.FullBord));
        searchPatterns.add(new SearchPattern("合并利润表", BoldStatus.FullBord));
        searchPatterns.add(new SearchPattern("母公司利润表",BoldStatus.FullBord));
        searchPatterns.add(new SearchPattern("合并现金流量表", BoldStatus.FullBord));
        searchPatterns.add(new SearchPattern("母公司现金流量表", BoldStatus.FullBord));

        List<ContentPojo.contentElement> contentElements = PdfParser.searchListTableAfterPattern(contentPojo, searchPatterns);


        /**论文中判断数据的变化情况 */
        List<Tu.Tuple3<String, Double, Double>> list = cellsToList(contentElements.get(0).getCells());
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(10);

        for(Tu.Tuple3<String,Double,Double> t3:list){
            String format = String.format("Account Name:%s\t\tCurrent Period:%s\t\tPrevious period:%s", t3.getValue1(), nf.format(t3.getValue2()), nf.format(t3.getValue3()));
            System.out.println(format);
        }

        for(Tu.Tuple3<String,Double,Double> t3:list){
            String prj = t3.getValue1();
            Double current = t3.getValue2();
            Double previous = t3.getValue3();
            if(previous==0.0d && current!=0.0d){
                String line = String.format("Account Name <%s> was 0 in the previous period and %s in current period,which is noteworthy!"
                ,prj,nf.format(current));
                System.out.println(line);
            }else if(previous!=0.0d && current==0.0d){
                String line = String.format("Account Name <%s> was 0 in the current period and %s in previous period,which is noteworthy!"
                ,prj,nf.format(previous));
                System.out.println(line);
            }else if((current - previous) /previous>=0.4){
                String line = String.format("Account Name <%s> was %s in the current period and %s in previous period,increase rate > 0.4,which is noteworthy!"
                        ,prj,nf.format(current),nf.format(previous));
                System.out.println(line);
            }else if((current-previous)/previous<=-0.4){
                String line = String.format("Account Name <%s> was %s in the current period and %s in previous period,decrease rate > 0.4,which is noteworthy!"
                        ,prj,nf.format(current),nf.format(previous));
                System.out.println(line);
            }
        }


        /**论文中计算财务指标举例*/
        Map<String,Tu.Tuple2<Double,Double>> balanceSheetMap = new HashMap<>();
        list.forEach(x->{
            balanceSheetMap.put(x.getValue1(),new Tu.Tuple2<>(x.getValue2(),x.getValue3()));
        });
        Map<String, Tu.Tuple2<Double, Double>> incomeStatementMap  = cellsToMap(contentElements.get(2).getCells());


        SingleMapOperation currentRatioOp  = map1 -> {
            double current = map1.get("流动资产合计").getKey() / map1.get("流动负债合计").getKey();
            double previous = map1.get("流动资产合计").getValue() / map1.get("流动负债合计").getValue();
            return new Tu.Tuple2<>(current, previous);
        };
        SingleMapOperation quickRatioOp = map1->{
            double current = (map1.get("流动资产合计").getKey() - map1.get("存货").getKey())  / map1.get("流动负债合计").getKey();
            double previous = (map1.get("流动资产合计").getValue() - map1.get("存货").getValue()) / map1.get("流动负债合计").getValue();
            return new Tu.Tuple2<>(current, previous);
        };
        TwoMapOperation accountsReceivableTurnoverOp = (map1,map2) -> {
            double avgAccountsReceivable = (map1.get("应收账款").getKey() + map1.get("应收账款").getValue())/2;
            double operationIncome = map2.get("其中：营业收入").getKey();
            return new Tu.Tuple2<>(operationIncome/avgAccountsReceivable,null);
        };
        Tu.Tuple2<Double, Double> currentRatio = currentRatioOp.execute(balanceSheetMap);
        Tu.Tuple2<Double, Double> quickRatio = quickRatioOp.execute(balanceSheetMap);
        Tu.Tuple2<Double, Double> accountsReceivableTurnover = accountsReceivableTurnoverOp.execute(balanceSheetMap, incomeStatementMap);
        System.out.println(currentRatio);
        System.out.println(quickRatio);
        System.out.println(accountsReceivableTurnover);
    }

    private static Double toDouble(String str){
        if("".equals(str.trim())){
            return 0.0d;
        }
        String s = str.trim().replaceAll(",","");
        return Double.valueOf(s);
    }

    interface SingleMapOperation{
        abstract Tu.Tuple2<Double,Double> execute(Map<String,Tu.Tuple2<Double,Double>> map);
    }
    interface TwoMapOperation{
        abstract Tu.Tuple2<Double,Double> execute(Map<String,Tu.Tuple2<Double,Double>> map1,Map<String,Tu.Tuple2<Double,Double>> map2);
    }

}
