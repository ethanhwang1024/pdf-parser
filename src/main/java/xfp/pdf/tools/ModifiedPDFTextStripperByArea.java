package xfp.pdf.tools;

import org.apache.pdfbox.pdmodel.PDPage;

import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.text.TextPositionComparator;
import xfp.pdf.pojo.Tu;


import java.awt.geom.Rectangle2D;
import java.io.*;

import java.text.Bidi;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;


/**
 * @ClassName Test
 * @Description TODO
 * @Author WANGHAN756
 * @Date 2021/6/16 9:30
 * @Version 1.0
 **/
public class ModifiedPDFTextStripperByArea extends PDFTextStripper {

    public final List<String> regions = new ArrayList<>();
    public final Map<String, Rectangle2D> regionArea = new HashMap<>();
    public final Map<String, ArrayList<List<TextPosition>>> regionCharacterList = new HashMap<>();
    public final Map<String, StringWriter> regionText = new HashMap<>();


    private List<List<Tu.Tuple2<TextPosition, RenderInfo>>> textPositionsList = null;
    public final Map<TextPosition,RenderInfo> positionRenderInfoMap = new HashMap<>();
//    public final Map<String,List<List<TextPosition>>> contentRegions = new HashMap<>();

    public final Map<String,List<List<Tu.Tuple2<TextPosition,RenderInfo>>>> contentRegions = new HashMap<>();

    public Map<String, List<List<Tu.Tuple2<TextPosition, RenderInfo>>>> getContentRegions() {
        return contentRegions;
    }

    public List<List<Tu.Tuple2<TextPosition, RenderInfo>>> getDetailedTextForRange(String region){
       return contentRegions.get(region);
    }

    public Map<TextPosition, RenderInfo> getPositionRenderInfoMap() {
        return positionRenderInfoMap;
    }

    public ModifiedPDFTextStripperByArea() throws IOException {
        super.setShouldSeparateByBeads(false);
    }

    public final void setShouldSeparateByBeads(boolean aShouldSeparateByBeads) {
    }

    @Override
    public Writer getOutput() {
        return super.getOutput();
    }

    public void addRegion(String regionName, Rectangle2D rect) {
        this.regions.add(regionName);
        this.regionArea.put(regionName, rect);
    }

    public void removeRegion(String regionName) {
        this.regions.remove(regionName);
        this.regionArea.remove(regionName);
    }

    public List<String> getRegions() {
        return this.regions;
    }

    public String getTextForRegion(String regionName) {
        StringWriter text = (StringWriter)this.regionText.get(regionName);
        return text.toString();
    }

    public void extractRegions(PDPage page) throws IOException {
        Iterator var2 = this.regions.iterator();

        while(var2.hasNext()) {
            String region = (String)var2.next();
            this.setStartPage(this.getCurrentPageNo());
            this.setEndPage(this.getCurrentPageNo());
            ArrayList<List<TextPosition>> regionCharactersByArticle = new ArrayList();
            regionCharactersByArticle.add(new ArrayList());
            this.regionCharacterList.put(region, regionCharactersByArticle);
            this.regionText.put(region, new StringWriter());
        }


        if (page.hasContents()) {
            this.processPage(page);
        }

    }

    protected void processTextPosition(TextPosition text) {
        float lineWidth = getGraphicsState().getLineWidth();
        RenderingMode renderingMode = getGraphicsState().getTextState().getRenderingMode();
        RenderInfo renderInfo = new RenderInfo();
        renderInfo.setLineWidth(lineWidth);
        renderInfo.setRenderingMode(renderingMode);
        positionRenderInfoMap.put(text,renderInfo);


        Iterator var2 = this.regionArea.entrySet().iterator();

        while(var2.hasNext()) {
            Map.Entry<String, Rectangle2D> regionAreaEntry = (Map.Entry)var2.next();
            Rectangle2D rect = (Rectangle2D)regionAreaEntry.getValue();
            if (rect.contains((double)text.getX(), (double)text.getY())) {
                this.charactersByArticle = (ArrayList)this.regionCharacterList.get(regionAreaEntry.getKey());
                super.processTextPosition(text);
            }
        }

    }

    protected void writePage() throws IOException {
        Iterator var1 = this.regionArea.keySet().iterator();

        //Wanghan????????????????????????
        ContentRegion contentRegion = new ContentRegion();
        textPositionsList = new ArrayList<>();
        while(var1.hasNext()) {
            String region = (String)var1.next();
            this.charactersByArticle = (ArrayList)this.regionCharacterList.get(region);
            this.output = (Writer)this.regionText.get(region);
            newWritePage(contentRegion);
            contentRegions.put(region,textPositionsList);
            textPositionsList = new ArrayList<>();
        }

    }

    private void newWritePage(ContentRegion contentRegion) throws IOException {
        String wordSeparator = " ";
        float maxHeightForLine = -1;
        float maxYForLine = -Float.MAX_VALUE;
        float minYTopForLine = Float.MAX_VALUE;
        float endOfLastTextX = -1f;
        float spacingTolerance = .5f;
        float averageCharTolerance = .3f;
        float lastWordSpacing = -1;
        boolean startOfPage = true;
        boolean startOfArticle;
        PositionWrapper lastPosition = null;
        PositionWrapper lastLineStartPosition = null;
        if(charactersByArticle.size()>0){
            writePageStart();
        }
        //??????
        for(List<TextPosition> textList:charactersByArticle){
            if(getSortByPosition()){
                TextPositionComparator comparator = new TextPositionComparator();
                textList.sort(comparator);
            }
            startArticle();
            startOfArticle = true;
            List<LineItem> line = new ArrayList<>();

//            List<List<TextPosition>> textPositionsList = new ArrayList<>();
            List<Tu.Tuple2<TextPosition,RenderInfo>> positions = new ArrayList<>();

            Iterator<TextPosition> textIter = textList.iterator();
            float previousAveCharWidth = -1;

            //????????????char????????????
            while (textIter.hasNext()){
//                System.out.println("?????????");

                TextPosition position = textIter.next();
                PositionWrapper current = new PositionWrapper(position);
                String characterValue = position.getUnicode();
                //?????????????????????????????????????????????????????????avgCharWidth
                if (lastPosition != null && (position.getFont() != lastPosition.getTextPosition()
                        .getFont()
                        || position.getFontSize() != lastPosition.getTextPosition().getFontSize()))
                {
                    previousAveCharWidth = -1;
                }
                float positionX;
                float positionY;
                float positionWidth;
                float positionHeight;
                //????????????????????????????????????????????????????????????????????????????????????????????????
                if(getSortByPosition()){
                    positionX = position.getXDirAdj();
                    positionY = position.getYDirAdj();
                    positionWidth = position.getWidthDirAdj();
                    positionHeight = position.getHeightDir();
                }else{
                    positionX = position.getX();
                    positionY = position.getY();
                    positionWidth = position.getWidth();
                    positionHeight = position.getHeight();
                }

                int wordCharCount = position.getIndividualWidths().length;
                //??????????????????????????????????????????????????????????????????
                float wordSpacing = position.getWidthOfSpace();
                float deltaSpace;

                if(wordSpacing==0||Float.isNaN(wordSpacing)){
                    deltaSpace = Float.MAX_VALUE;
                }else{
                    if(lastWordSpacing<0){
                        //???????????????
                        deltaSpace = wordSpacing * spacingTolerance;
                    }else{
                        deltaSpace = (wordSpacing + lastWordSpacing) /2f * spacingTolerance;
                    }
                }
                float averageCharWidth;
                if (previousAveCharWidth < 0){
                    averageCharWidth = positionWidth / wordCharCount;
                }else{
                    averageCharWidth = (previousAveCharWidth + positionWidth / wordCharCount) / 2f;
                }
                float deltaCharWidth = averageCharWidth * averageCharTolerance;

                //?????????????????????wordSpacing?????????????????????????????????
                float expectedStartOfNextWordX = -Float.MAX_VALUE;
                if(endOfLastTextX!=-1){
                    expectedStartOfNextWordX = endOfLastTextX + Math.min(deltaSpace,deltaCharWidth);
                }
                if(lastPosition!=null){
                    if(startOfArticle){
                        lastPosition.setArticleStart();
                        startOfArticle = false;
                    }

                    if(!overlap(positionY,positionHeight,maxYForLine,maxHeightForLine)){
                        //???????????????????????????
                        writeLine(normalize(line));
                        line.clear();
                        //Wanghan
                        textPositionsList.add(positions);
                        positions = new ArrayList<>();

                        lastLineStartPosition = handleLineSeparation(current, lastPosition,
                                lastLineStartPosition, maxHeightForLine);
                        expectedStartOfNextWordX = -Float.MAX_VALUE;
                        maxYForLine = -Float.MAX_VALUE;
                        maxHeightForLine = -1;
                        minYTopForLine = Float.MAX_VALUE;
                    }
                    //????????????????????????
                    if (expectedStartOfNextWordX != -Float.MAX_VALUE
                            && expectedStartOfNextWordX < positionX
                            // only bother adding a word separator if the last character was not a word separator
                            && (wordSeparator.isEmpty() || //
                            (lastPosition.getTextPosition().getUnicode() != null
                                    && !lastPosition.getTextPosition().getUnicode()
                                    .endsWith(wordSeparator))))
                    {
                        line.add(LineItem.getWordSeparator());
                        //Wanghan
                        positions.add(new Tu.Tuple2<>(ContentRegion.createTextPositionForWordSep(),null));
                    }

                    //??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                    if (Math.abs(position.getX()
                            - lastPosition.getTextPosition().getX()) > (wordSpacing + deltaSpace))
                    {
                        maxYForLine = -Float.MAX_VALUE;
                        maxHeightForLine = -1;
                        minYTopForLine = Float.MAX_VALUE;
                    }
                }
                if(positionY>=maxYForLine){
                    maxYForLine = positionY;
                }
                //endX???PDF??????????????????????????????x???????????????????????????????????????????????????
                endOfLastTextX = positionX + positionWidth;
                if (characterValue != null)
                {
                    if (startOfPage && lastPosition == null)
                    {
                        writeParagraphStart();// not sure this is correct for RTL?
                    }

                    line.add(new LineItem(position));
                    //??????
                    positions.add(new Tu.Tuple2<>(position,positionRenderInfoMap.get(position)));
                }
                maxHeightForLine = Math.max(maxHeightForLine, positionHeight);
                minYTopForLine = Math.min(minYTopForLine, positionY - positionHeight);
                lastPosition = current;
                if (startOfPage)
                {
                    lastPosition.setParagraphStart();
                    lastPosition.setLineStart();
                    lastLineStartPosition = lastPosition;
                    startOfPage = false;
                }
                lastWordSpacing = wordSpacing;
                previousAveCharWidth = averageCharWidth;
            }
            // print the final line
            if (line.size() > 0)
            {
                writeLine(normalize(line));
                //????????????
                textPositionsList.add(positions);
                writeParagraphEnd();
            }
            endArticle();
        }
        writePageEnd();

    }


    private PositionWrapper handleLineSeparation(PositionWrapper current,
                                                                 PositionWrapper lastPosition, PositionWrapper lastLineStartPosition,
                                                                 float maxHeightForLine) throws IOException
    {
        current.setLineStart();
        isParagraphSeparation(current, lastPosition, lastLineStartPosition, maxHeightForLine);
        lastLineStartPosition = current;
        if (current.isParagraphStart())
        {
            if (lastPosition.isArticleStart())
            {
                if (lastPosition.isLineStart())
                {
                    writeLineSeparator();
                }
                writeParagraphStart();
            }
            else
            {
                writeLineSeparator();
                writeParagraphSeparator();
            }
        }
        else
        {
            writeLineSeparator();
        }
        return lastLineStartPosition;
    }

    private void isParagraphSeparation(PositionWrapper position, PositionWrapper lastPosition,
                                       PositionWrapper lastLineStartPosition, float maxHeightForLine)
    {
        boolean result = false;
        if (lastLineStartPosition == null)
        {
            result = true;
        }
        else
        {
            float yGap = Math.abs(position.getTextPosition().getYDirAdj()
                    - lastPosition.getTextPosition().getYDirAdj());
            float newYVal = multiplyFloat(getDropThreshold(), maxHeightForLine);
            // do we need to flip this for rtl?
            float xGap = position.getTextPosition().getXDirAdj()
                    - lastLineStartPosition.getTextPosition().getXDirAdj();
            float newXVal = multiplyFloat(getIndentThreshold(),
                    position.getTextPosition().getWidthOfSpace());
            float positionWidth = multiplyFloat(0.25f, position.getTextPosition().getWidth());

            if (yGap > newYVal)
            {
                result = true;
            }
            else if (xGap > newXVal)
            {
                // text is indented, but try to screen for hanging indent
                if (!lastLineStartPosition.isParagraphStart())
                {
                    result = true;
                }
                else
                {
                    position.setHangingIndent();
                }
            }
            else if (xGap < -position.getTextPosition().getWidthOfSpace())
            {
                // text is left of previous line. Was it a hanging indent?
                if (!lastLineStartPosition.isParagraphStart())
                {
                    result = true;
                }
            }
            else if (Math.abs(xGap) < positionWidth)
            {
                // current horizontal position is within 1/4 a char of the last
                // linestart. We'll treat them as lined up.
                if (lastLineStartPosition.isHangingIndent())
                {
                    position.setHangingIndent();
                }
                else if (lastLineStartPosition.isParagraphStart())
                {
                    // check to see if the previous line looks like
                    // any of a number of standard list item formats
                    Pattern liPattern = matchListItemPattern(lastLineStartPosition);
                    if (liPattern != null)
                    {
                        Pattern currentPattern = matchListItemPattern(position);
                        if (liPattern == currentPattern)
                        {
                            result = true;
                        }
                    }
                }
            }
        }
        if (result)
        {
//            try {
//                output.write("P");
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            position.setParagraphStart();
        }
    }

    private Pattern matchListItemPattern(PositionWrapper pw)
    {
        TextPosition tp = pw.getTextPosition();
        String txt = tp.getUnicode();
        return matchPattern(txt, getListItemPatterns());
    }

    private float multiplyFloat(float value1, float value2)
    {
        // multiply 2 floats and truncate the resulting value to 3 decimal places
        // to avoid wrong results when comparing with another float
        return Math.round(value1 * value2 * 1000) / 1000f;
    }
    private List<WordWithTextPositions> normalize(List<LineItem> line)
    {
        List<WordWithTextPositions> normalized = new LinkedList<WordWithTextPositions>();
        StringBuilder lineBuilder = new StringBuilder();
        List<TextPosition> wordPositions = new ArrayList<TextPosition>();

        for (LineItem item : line)
        {
            lineBuilder = normalizeAdd(normalized, lineBuilder, wordPositions, item);
        }

        if (lineBuilder.length() > 0)
        {
            normalized.add(createWord(lineBuilder.toString(), wordPositions));
        }
        return normalized;
    }
    private StringBuilder normalizeAdd(List<WordWithTextPositions> normalized,
                                       StringBuilder lineBuilder, List<TextPosition> wordPositions, LineItem item)
    {
        if (item.isWordSeparator())
        {
            normalized.add(
                    createWord(lineBuilder.toString(), new ArrayList<TextPosition>(wordPositions)));
            lineBuilder = new StringBuilder();
            wordPositions.clear();
        }
        else
        {
            TextPosition text = item.getTextPosition();
            lineBuilder.append(text.getUnicode());
            wordPositions.add(text);
        }
        return lineBuilder;
    }

    private WordWithTextPositions createWord(String word, List<TextPosition> wordPositions)
    {
        return new WordWithTextPositions(normalizeWord(word), wordPositions);
    }

    private String normalizeWord(String word)
    {
        StringBuilder builder = null;
        int p = 0;
        int q = 0;
        int strLength = word.length();
        for (; q < strLength; q++)
        {
            // We only normalize if the codepoint is in a given range.
            // Otherwise, NFKC converts too many things that would cause
            // confusion. For example, it converts the micro symbol in
            // extended Latin to the value in the Greek script. We normalize
            // the Unicode Alphabetic and Arabic A&B Presentation forms.
            char c = word.charAt(q);
            if (0xFB00 <= c && c <= 0xFDFF || 0xFE70 <= c && c <= 0xFEFF)
            {
                if (builder == null)
                {
                    builder = new StringBuilder(strLength * 2);
                }
                builder.append(word, p, q);
                // Some fonts map U+FDF2 differently than the Unicode spec.
                // They add an extra U+0627 character to compensate.
                // This removes the extra character for those fonts.
                if (c == 0xFDF2 && q > 0
                        && (word.charAt(q - 1) == 0x0627 || word.charAt(q - 1) == 0xFE8D))
                {
                    builder.append("\u0644\u0644\u0647");
                }
                else
                {
                    // Trim because some decompositions have an extra space, such as U+FC5E
                    builder.append(Normalizer
                            .normalize(word.substring(q, q + 1), Normalizer.Form.NFKC).trim());
                }
                p = q + 1;
            }
        }
        if (builder == null)
        {
            return handleDirection(word);
        }
        else
        {
            builder.append(word, p, q);
            return handleDirection(builder.toString());
        }
    }

    private static Map<Character, Character> MIRRORING_CHAR_MAP = new HashMap<Character, Character>();

    static
    {
        String path = "/org/apache/pdfbox/resources/text/BidiMirroring.txt";
        InputStream input = new BufferedInputStream(PDFTextStripper.class.getResourceAsStream(path));
        try
        {
            parseBidiFile(input);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                input.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private static void parseBidiFile(InputStream inputStream) throws IOException
    {
        LineNumberReader rd = new LineNumberReader(new InputStreamReader(inputStream));

        do
        {
            String s = rd.readLine();
            if (s == null)
            {
                break;
            }

            int comment = s.indexOf('#'); // ignore comments
            if (comment != -1)
            {
                s = s.substring(0, comment);
            }

            if (s.length() < 2)
            {
                continue;
            }

            StringTokenizer st = new StringTokenizer(s, ";");
            int nFields = st.countTokens();
            Character[] fields = new Character[nFields];
            for (int i = 0; i < nFields; i++)
            {
                fields[i] = (char) Integer.parseInt(st.nextToken().trim(), 16);
            }

            if (fields.length == 2)
            {
                // initialize the MIRRORING_CHAR_MAP
                MIRRORING_CHAR_MAP.put(fields[0], fields[1]);
            }

        } while (true);
    }
    private String handleDirection(String word)
    {
        Bidi bidi = new Bidi(word, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);

        // if there is pure LTR text no need to process further
        if (!bidi.isMixed() && bidi.getBaseLevel() == Bidi.DIRECTION_LEFT_TO_RIGHT)
        {
            return word;
        }

        // collect individual bidi information
        int runCount = bidi.getRunCount();
        byte[] levels = new byte[runCount];
        Integer[] runs = new Integer[runCount];

        for (int i = 0; i < runCount; i++)
        {
            levels[i] = (byte)bidi.getRunLevel(i);
            runs[i] = i;
        }

        // reorder individual parts based on their levels
        Bidi.reorderVisually(levels, 0, runs, 0, runCount);

        // collect the parts based on the direction within the run
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < runCount; i++)
        {
            int index = runs[i];
            int start = bidi.getRunStart(index);
            int end = bidi.getRunLimit(index);

            int level = levels[index];

            if ((level & 1) != 0)
            {
                while (--end >= start)
                {
                    char character = word.charAt(end);
                    if (Character.isMirrored(word.codePointAt(end)))
                    {
                        if (MIRRORING_CHAR_MAP.containsKey(character))
                        {
                            result.append(MIRRORING_CHAR_MAP.get(character));
                        }
                        else
                        {
                            result.append(character);
                        }
                    }
                    else
                    {
                        result.append(character);
                    }
                }
            }
            else
            {
                result.append(word, start, end);
            }
        }

        return result.toString();
    }


    private void writeLine(List<WordWithTextPositions> line)
            throws IOException
    {
        int numberOfStrings = line.size();
        for (int i = 0; i < numberOfStrings; i++)
        {
            WordWithTextPositions word = line.get(i);
            writeString(word.getText(), word.getTextPositions());
            if (i < numberOfStrings - 1)
            {
                writeWordSeparator();
            }
        }
    }
    private static final class WordWithTextPositions
    {
        String text;
        List<TextPosition> textPositions;

        WordWithTextPositions(String word, List<TextPosition> positions)
        {
            text = word;
            textPositions = positions;
        }

        public String getText()
        {
            return text;
        }

        public List<TextPosition> getTextPositions()
        {
            return textPositions;
        }
    }

//    public static void main(String[] args) {
//        boolean overlap = overlap(500, 1, -Float.MAX_VALUE, -1);
//        System.out.println(overlap);
//    }
    private boolean overlap(float y1, float height1, float y2, float height2)
    {
        return within(y1, y2, .1f) || y2 <= y1 && y2 >= y1 - height1
                || y1 <= y2 && y1 >= y2 - height2;
    }
    private boolean within(float first, float second, float variance)
    {
        return second < first + variance && second > first - variance;
    }

    private static final class PositionWrapper
    {
        private boolean isLineStart = false;
        private boolean isParagraphStart = false;
        private boolean isPageBreak = false;
        private boolean isHangingIndent = false;
        private boolean isArticleStart = false;

        private TextPosition position = null;

        /**
         * Constructs a PositionWrapper around the specified TextPosition object.
         *
         * @param position the text position.
         */
        PositionWrapper(TextPosition position)
        {
            this.position = position;
        }

        /**
         * Returns the underlying TextPosition object.
         *
         * @return the text position
         */
        public TextPosition getTextPosition()
        {
            return position;
        }

        public boolean isLineStart()
        {
            return isLineStart;
        }

        /**
         * Sets the isLineStart() flag to true.
         */
        public void setLineStart()
        {
            this.isLineStart = true;
        }

        public boolean isParagraphStart()
        {
            return isParagraphStart;
        }

        /**
         * sets the isParagraphStart() flag to true.
         */
        public void setParagraphStart()
        {
            this.isParagraphStart = true;
        }

        public boolean isArticleStart()
        {
            return isArticleStart;
        }

        /**
         * Sets the isArticleStart() flag to true.
         */
        public void setArticleStart()
        {
            this.isArticleStart = true;
        }

        public boolean isPageBreak()
        {
            return isPageBreak;
        }

        /**
         * Sets the isPageBreak() flag to true.
         */
        public void setPageBreak()
        {
            this.isPageBreak = true;
        }

        public boolean isHangingIndent()
        {
            return isHangingIndent;
        }

        /**
         * Sets the isHangingIndent() flag to true.
         */
        public void setHangingIndent()
        {
            this.isHangingIndent = true;
        }
    }

    private static final class LineItem
    {
        public static LineItem WORD_SEPARATOR = new LineItem();

        public static LineItem getWordSeparator()
        {
            return WORD_SEPARATOR;
        }

        private final TextPosition textPosition;

        private LineItem()
        {
            textPosition = null;
        }

        LineItem(TextPosition textPosition)
        {
            this.textPosition = textPosition;
        }

        public TextPosition getTextPosition()
        {
            return textPosition;
        }

        public boolean isWordSeparator()
        {
            return textPosition == null;
        }
    }


    public List<List<TextPosition>> GetCharactersByArticle() {
        return getCharactersByArticle();
    }
}


