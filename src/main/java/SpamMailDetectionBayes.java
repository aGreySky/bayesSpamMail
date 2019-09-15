import com.huaban.analysis.jieba.JiebaSegmenter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;

public class SpamMailDetectionBayes {
    public static final String MAIL_INDEX_PATH = "mail/trec06c/full/index";//邮件索引
    public static Map<String, Integer> spamMailMap = new HashMap<>();//垃圾邮件分词表
    public static Map<String, Integer> hamMailMap = new HashMap<>();//正常邮件分词表

    public static Integer spamMailSegNum = 0;//垃圾邮件分词总数量
    public static Integer hamMailSegNum = 0;//正常邮件分词总数量
    public static Map<String, Double> spamMailRateMap = new HashMap<>();//垃圾邮件分词概率表
    public static Map<String, Double> hamMailRateMap = new HashMap<>();//正常邮件分词概率表
    public static double typeThreshold = -0.05d;//分类阈值,初值-0.05
    public static final double MAX_TYPE_THRESHOLD = 1.051d;//分类阈值最大值，浮点数累加会产生误差，故设置多一位小数
    public static final double TYPE_THRESHOLD_INCREASES = 0.05;//分类阈值增幅为0.1

    public static final int TRAIN_MAX_NUM = 50000;//训练邮件最大数
    public static final int TEST_MAX_NUM = 10000;//测试邮件最大数

    public static double probability[] = new double[TEST_MAX_NUM];//测试邮件为垃圾邮件的概率数组
    public static String testRealType[] = new String[TEST_MAX_NUM];//测试邮件真实的类型数组
    public static int trainNum = 0;//实际训练邮件数
    public static Integer spamTrainMailNum = 0;//垃圾邮件总数量
    public static double spamTrainRate = 0d;//垃圾邮件率
    public static int testNum = 0;//实际测试邮件数

    public static double trainTime = 0d;//训练时间
    public static double testTime = 0d;//测试时间

    /*评价指标*/
    public static int TP = 0;//实际为垃圾，预测也为垃圾
    public static int FN = 0;//实际为垃圾，预测却为正常
    public static int FP = 0;//实际为正常，预测却为垃圾
    public static int TN = 0;//实际为正常，预测也为正常

    /*被分对的样本数除以所有的样本数，通常来说，正确率越高，分类器越好*/
    public static double accuracy = 0;//准确率Accuracy = (TP+TN)/(TP+FP+TN+FN)
    /*被分为正例的示例中实际为正例的比例*/
    public static double precision = 0;//精确率precision=TP/(TP+FP)
    /*所有负例中被分对的比例，衡量了分类器对负例的识别能力*/
    public static double specificity = 0;//特效率specificity=TN/N
    /*所有正例中被分对的比例，衡量了分类器对正例的识别能力*/
    public static double recall = 0;//召回率recall=TP/(TP+FN) = 灵敏度（Sensitivity）
    /*F1-Score指标综合了Precision与Recall的产出的结果。
    F1-Score的取值范围从0到1的，1代表模型的输出最好，0代表模型的输出结果最差。*/
    public static double F1_Score = 0;//平衡F分数:准确率和召回率的调和平均数

    /*True Positive Rate(TPR)和False Positive Rate(FPR)分别构成ROC曲线的y轴和x轴。*/
    /*实际正样本中被预测正确的概率*/
    public static double TPR = 0;//TPR=TP/(TP+FN) = recall = Sensitivity
    /*实际负样本中被错误预测为正样本的概率*/
    public static double FPR = 0;//FPR=FP/(FP+TN)

    //ROC坐标集合，键为阈值，值为TPR和FPR组成的数组
    List<Double[]> ROCLocalList = new ArrayList<Double[]>();

    /*Precision和Recall则分别构成了PR曲线的y轴和x轴。*/



    /*public static List<String> clearList = new ArrayList<String>();

    static {
        clearList.add("");
        clearList.add(" ");
        clearList.add("　　");
        clearList.add("\"");
        clearList.add("!");
        clearList.add("$");
        clearList.add("%");
        clearList.add("(");
    }*/


    public static void main(String[] args) throws IOException {
        SpamMailDetectionBayes smc = new SpamMailDetectionBayes();
        smc.TrainMail();
        smc.TestMail();
        smc.evaluationModel();
    }

    /**
     * 给定邮件,分词,根据分词结果判断是垃圾邮件的概率
     * P(A|BC)= P（A|B) P(A|C)/P(A)
     * P(Spam|t1,t2,t3……tn)=（P1*P2*……PN）/(P1*P2*……PN+(1-P1)*(1-P2)*……(1-PN))
     */
    public void TestMail() throws IOException {
        long testStartTime = System.currentTimeMillis();
        //垃圾邮件率
        spamTrainRate = (double) spamTrainMailNum / (double) trainNum;
        BufferedReader bufferedReader = new BufferedReader(new FileReader(MAIL_INDEX_PATH));
        int nowIndex = 0;
        String indexLine;
        int start_count = TRAIN_MAX_NUM;//从训练集之后开始
        System.out.println("\n-------------------------------测试------------------------------");
        while ((indexLine = bufferedReader.readLine()) != null && testNum < TEST_MAX_NUM) {
            //跳过训练集
            if (nowIndex < start_count) {
                nowIndex++;
                continue;
            } else {
                //分离出邮件真实类型和文件名索引
                String[] typeAndIndex = indexLine.split(" ");
                String type = typeAndIndex[0];
                List<String> wordList = getWordList(typeAndIndex[1]);
                //该邮件是否手动设置概率的标志
                int flag = 0;
                //P(Spam)*  乘积P(Spam|ti)*P(ti)/P(Spam) (i从1-n)
                //spamTrainRate = P(Spam)
                double rate = spamTrainRate;
                //[1-P(Spam)]*  乘积[1-P(Spam|ti)]*P(ti)/[1-P(Spam)] (i从1-n)
                // 1 - spamTrainRate = [1-P(Spam)]
                double tempRate = 1 - spamTrainRate;
                for (String s : wordList) {
                    if (spamMailRateMap.containsKey(s)) {
                        double spamTmp = spamMailRateMap.get(s);
                        rate *= spamTmp * (((double)(spamMailMap.get(s) + (hamMailMap.containsKey(s)? hamMailMap.get(s): 0))) / (double) (spamMailSegNum + hamMailSegNum)) / spamTrainRate;
                        tempRate *= (1d - spamTmp) * (((double)spamMailMap.get(s)+(hamMailMap.containsKey(s)? hamMailMap.get(s): 0)) / (double) (spamMailSegNum + hamMailSegNum)) / spamTrainRate;;
                        if (testNum == 1 || testNum == 2){
                            System.out.println("rate:"+rate+"\ntempRate:"+tempRate);
                        }
                        //当有一个概率非常趋近于0时，需要进行判断
                        // 判断该邮件更接近垃圾邮件还是正常邮件
                        if (rate < Math.pow(10, -300) || tempRate < Math.pow(10, -300)){
                            if (rate > 1){
                                probability[testNum] = 1d;
                                flag = 2;
                                break;
                            }else if (tempRate > 1){
                                probability[testNum] = 0d;
                                flag = 3;
                                break;
                            }else {
                                rate *= Math.pow(10, 7);
                                tempRate *= Math.pow(10, 7);
                            }
                        }
                    }

                }
                if (flag == 0) { //flag!=0代表已经手动设置过概率
                    probability[testNum] = rate / (rate + tempRate);
                    System.out.println(probability[testNum]);
                }
                testRealType[testNum++] = type;
            }
        }
        //关闭文件读流
        bufferedReader.close();
        long testFinishTime = System.currentTimeMillis();
        testTime = (testFinishTime - testStartTime) / 1000;
        System.out.println("-------------------------------测试完成------------------------------");
    }

    //得到分词集合
    private List getWordList(String index) {
        index = index.replace("..", "mail/trec06c");
        String str = readFile(index);
        JiebaSegmenter jb = new JiebaSegmenter();
        List<String> tempList = jb.sentenceProcess(str);
        List<String> rightList = new ArrayList<>();
        for (String s : tempList) {
            //过滤掉标点符号等无意义分词
            if (s.length() > 1)
                rightList.add(s);
        }
        return rightList;
    }

    //评价模型
    private void evaluationModel() {

        /*--------------训练效率评估----------------------*/
        System.out.println("\n---------------------------------训练效率------------------------------");
        System.out.println("训练个数：" + trainNum);
        System.out.format("训练时间为：%.1fs\n", trainTime);
        System.out.println("测试个数：" + testNum);
        System.out.format("测试时间为：%.1fs\n", testTime);


        /*--------------模型评价指标----------------------*/
        System.out.println("\n---------------------------------评价指标-------------------------------\n");
        //评估各阈值下的指标，用于绘制ROC曲线
        while(typeThreshold < MAX_TYPE_THRESHOLD) {
            //初始化各个指标
            initTestValue();
            //得到该阈值下的混淆矩阵
            for (int i = 0; i < testNum; i++){
                BigDecimal bg = new BigDecimal(typeThreshold);
                if (probability[i] >= bg.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()) {//概率大于阈值则判定为垃圾
                    if (testRealType[i].equals("spam")) {
                        TP++;
                    } else {
                        FP++;//误判为垃圾
                    }
                    //System.out.format("spam %s\n", typeAndIndex[1]);
                } else {
                    if (testRealType[i].equals("ham")) {
                        TN++;
                    } else {
                        FN++;
                    }
                    //System.out.format("ham %s\n", typeAndIndex[1]);
                }
            }
            System.out.format("\n\n------------------阈值（%.2f）-----------------\n\n ", typeThreshold);

            //绘制混淆矩阵
            System.out.println("------------------------------------------ ");
            System.out.println("|                |         实际类         |");
            System.out.println("|      数量      |------------------------|");
            System.out.println("|                |   垃圾    |    正常    |");
            System.out.println("|----------------|------------------------|");
            System.out.println("|预测类  |  垃圾  |    " + TP + "     |     " + FP + "   |");
            System.out.println("|       |  正常  |    " + FN + "      |      " + TN + "    |");
            System.out.println("|----------------|------------------------|\n");

            //根据混淆矩阵得到常用评价指标
            accuracy = (double) (TP + TN) / (double) (TP + FP + FN + TN);
            System.out.format("正确率（accuracy）= (TP+TN)/(P+N) = %.2f\n", accuracy);
            specificity = (double) TN / (double) (TN + FP);
            System.out.format("特效度（specificity）= TN/N = %.2f\n", specificity);
            precision = (double) TP / (double) (TP + FP);
            System.out.format("精度（precision）= TP/(TP+FP) = %.2f\n", precision);
            recall = (double) TP / (double) (TP + FN);
            System.out.format("召回率（recall）= 灵敏度（sensitive）= TP/(TP+FN) = %.2f\n", recall);
            F1_Score = 2 * precision * recall / (precision + recall);
            System.out.format("综合分类率（F1）= 2 * precision * recall / (precision + recall) = %.2f\n", F1_Score);

            //得到ROC曲线坐标值
            TPR = (double) TP / (double) (TP + FN);
            FPR =  (double) FP / (double)(FP + TN);
            //注入ROC坐标集合
            ROCLocalList.add(new Double[]{FPR, TPR});
            //提高阈值
            typeThreshold += TYPE_THRESHOLD_INCREASES;
        }
        System.out.println("\n-----------------------------概率分布曲线------------------------------");
        System.out.println("概率分布坐标点：");
        //创建Dataset对象
        XYDataset probabilityDataset = new XYSeriesCollection();
        XYSeries pSeries = new XYSeries("Positives");
        XYSeries nSeries = new XYSeries("Negatives");
        for (int i = 0; i < probability.length; i++) {
            int index;
            BigDecimal bg = new BigDecimal(probability[i]);
            double tempProbability = bg.setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();
            if (testRealType[i].equals("spam")){
                if ((index = pSeries.indexOf(tempProbability)) > -1) {
                    pSeries.updateByIndex(index,  pSeries.getY(index).intValue() + 1);
                }else{
                    pSeries.add(tempProbability, new Integer(1));
                }
            }else{
                if ((index = nSeries.indexOf(tempProbability)) > -1) {
                    nSeries.updateByIndex(index, nSeries.getY(index).intValue() + 1);
                }else{
                    nSeries.add(tempProbability, new Integer(1));
                }
            }
        }
        System.out.println("正例坐标：");
        for (int i = 0; i < pSeries.getItems().size();i++){
            System.out.format("(%.2f, %d)", pSeries.getDataItem(i).getX(), pSeries.getDataItem(i).getY());
        }
        System.out.println("\n负例坐标：");
        for (int i = 0; i < nSeries.getItems().size();i++){
            System.out.format("(%.2f, %d)", nSeries.getDataItem(i).getX(), nSeries.getDataItem(i).getY());
        }
        ((XYSeriesCollection) probabilityDataset).addSeries(pSeries);
        ((XYSeriesCollection) probabilityDataset).addSeries(nSeries);
        String probabilityFilePath = "probability-Bayes.jpg";
        createLineChart(probabilityDataset, probabilityFilePath,"垃圾邮件正负例概率分布图","概率","样本数",1000,500);

        //绘制ROC曲线并保存在ROC.jpg
        System.out.println("\n-----------------------------ROC曲线------------------------------");
        System.out.println("ROC曲线坐标点：");
        for (Double[] doubles : ROCLocalList){
            System.out.format("(%.2f, %.2f)", doubles[0], doubles[1]);
        }
        String ROCFilePath = "ROC-Bayes.jpg";
        //创建Dataset对象
        XYDataset xyDataset = new XYSeriesCollection();
        XYSeries xySeries = new XYSeries("ROC");
        for (Double[] doubles : ROCLocalList) {
            xySeries.add(doubles[0],doubles[1]);
        }
        ((XYSeriesCollection) xyDataset).addSeries(xySeries);
        createLineChart(xyDataset, ROCFilePath,"垃圾邮件识别ROC曲线图", "FPR", "TPR",500,500);


    }
    //绘制ROC曲线
    public static void createLineChart(XYDataset ds, String filePath,String title,String xAxis,String yAxis,int width,int height) {
        try {
            // 标题,X坐标,Y坐标,数据集合,orientation,是否显示legend,是否显示tooltip,是否使用url链接
            JFreeChart chart = ChartFactory.createXYLineChart(title, xAxis, yAxis, ds, PlotOrientation.VERTICAL,true, true, false);
            chart.setBackgroundPaint(Color.WHITE);
            Font font = new Font("宋体", Font.BOLD, 12);
            chart.getTitle().setFont(font);
            chart.setBackgroundPaint(Color.WHITE);

            //获得坐标系
            XYPlot xyPlot = chart.getXYPlot();

            //设置标题字体
            chart.getTitle().setFont(font);

            //设置背景颜色
            xyPlot.setBackgroundPaint(Color.WHITE);
            // x轴 // 分类轴网格是否可见
            xyPlot.setDomainGridlinesVisible(true);
            // y轴 //数据轴网格是否可见
            xyPlot.setRangeGridlinesVisible(true);
            // 设置网格竖线颜色
            xyPlot.setDomainGridlinePaint(Color.LIGHT_GRAY);
            // 设置网格横线颜色
            xyPlot.setRangeGridlinePaint(Color.LIGHT_GRAY);


            // 取得Y轴
            ValueAxis rangeAxis = xyPlot.getRangeAxis();
            //设置字体
            rangeAxis.setLabelFont(font);
            rangeAxis.setAutoRange(true);
            // 取得X轴
            ValueAxis valueAxis = xyPlot.getDomainAxis();
            valueAxis.setLabelFont(font);
            valueAxis.setRange(0, 1);

            // 设置X，Y轴坐标上的文字
            valueAxis.setTickLabelFont(font);
            rangeAxis.setTickLabelFont(font);
            // 设置X，Y轴的标题文字
            valueAxis.setLabelFont(font);
            rangeAxis.setLabelFont(font);
            ChartUtilities.saveChartAsJPEG(new File(filePath), chart, width, height);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //初始化指标
    private void initTestValue() {
        TP = 0;
        FP = 0;
        TN = 0;
        FN = 0;
        accuracy = 0d;
        specificity = 0d;
        precision = 0d;
        recall = 0d;
        F1_Score = 0d;
        TPR = 0d;
        FPR = 0d;
    }

    /**
     * 从给定的垃圾邮件、正常邮件语料中建立map <切出来的词,出现的频率> 
     */
    public void TrainMail() throws IOException {
        long trainStartTime = System.currentTimeMillis();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(MAIL_INDEX_PATH));
        //得到训练集概率表
        String indexLine;
        System.out.println("-----------------------------------训练----------------------------------");
        while((indexLine = bufferedReader.readLine())!=null && trainNum < TRAIN_MAX_NUM){
            trainNum++;
            System.out.println(indexLine);
            //分离出邮件真实类型和文件名索引
            String[] typeAndIndex = indexLine.split(" ");
            String type = typeAndIndex[0];
            List<String> wordList = getWordList(typeAndIndex[1]);
            if (type.equals("spam")) {
                spamTrainMailNum ++;
                spamMailSegNum += wordList.size();
                for (String s : wordList) {
                    spamMailMap.put(s, spamMailMap.containsKey(s) ? spamMailMap.get(s) + 1 : 1);
                }
            }else {
                hamMailSegNum += wordList.size();
                for (String s : wordList) {
                    hamMailMap.put(s, hamMailMap.containsKey(s) ? hamMailMap.get(s) + 1 : 1);
                }
            }
        }
        bufferedReader.close();
        //垃圾邮件率
        spamTrainRate = (double) spamTrainMailNum / (double) trainNum;
        getSpamMailRateMap();
        long trainFinishTime = System.currentTimeMillis();
        trainTime = (trainFinishTime - trainStartTime) / 1000;
    }

    /**
     * 邮件中出现ti时,该邮件为垃圾邮件的概率
     * P1(ti)为ti出现在垃圾邮件中的次数/垃圾邮件分词总数
     * P2(ti)为ti出现在正常邮件中的次数/正常邮件分词总数
     * P(ti)为ti出现在所有邮件中的次数/邮件分词总数
     * P(Spam|ti)=P1(ti)/((P1(ti)+P2(ti))
     * P(ham|ti)=P2(ti)/((P1(ti)+P2(ti))
     * P(ti|Spam)=P(Spam|ti)*P(ti)/P(Spam)
     * P(ti|ham)=P(ham|ti)*P(ti)/P(ham)
     *          =[1-P(Spam|ti)]*P(ti)/[1-P(Spam)]
     *
     * P(Spam|t1,t2,...,tn)
     * =P(Spam)*P(t1|Spam)P(t2|Spam)*...*P(tn|Spam)/[P(Spam)*P(t1|Spam)P(t2|Spam)*...*P(tn|Spam)+P(ham)*P( t1|ham)P( t2|ham)*...*P( tn|ham)]
     *
     * P(Spam)*P(t1|Spam)*P(t2|Spam)*...*P(tn|Spam)
     * =P(Spam)*[P(Spam|t1)*P(t1)/P(Spam)]*...*[P(Spam|tn)*P(tn)/P(Spam)]
     *
     * P(ham)*P(t1|ham)*P(t2|ham)*...*P(tn|ham)
     * =[1-P(Spam)]*[1-P(Spam|t1)]*P(t1)/[1-P(Spam)]*...*[1-P(Spam|tn)]*P(tn)/[1-P(Spam)]
     * 如果直接用“ti在垃圾邮件中出现的次数/ti出现的总次数”得到结果的话，在垃圾邮件占多数的情况下，会影响结果。
     * 因此需要用“ti出现的概率代替ti出现的次数”进行计算
     */
    public void getSpamMailRateMap() {
        for (Iterator iter = spamMailMap.keySet().iterator(); iter.hasNext();) {
            String key = (String) iter.next();
            double rate = (double) spamMailMap.get(key) / (double) spamMailSegNum;
            double allRate = rate;
            if (hamMailMap.containsKey(key)) {
                allRate += (double) hamMailMap.get(key) / (double) hamMailSegNum;
            }else{ //如果正常邮件中未出现该词，则默认在正常邮件中出现过1次，防止判断过于绝对
                allRate += 1d / (double) hamMailSegNum;
            }
            spamMailRateMap.put(key, rate / allRate);
          }
    }




    /**
     * 读文件 
     */
    public String readFile(String filePath) {
        StringBuffer str = new StringBuffer();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    new FileInputStream(new File(filePath)), "gbk"));
            String tmp = "";
            int flag = 0;
            while ((tmp = br.readLine()) != null){
                if (flag == 1){
                    str.append(tmp);
                }
                if (tmp.equals("")||tmp.length() == 0){
                    flag = 1;
                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return str.toString();
    }

}