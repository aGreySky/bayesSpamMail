import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class test {
    public static void main(String[] args) {
        System.out.println("              混淆矩阵             ");
        System.out.println("------------------------------------------ ");
        System.out.println("|                |         实际类         |");
        System.out.println("|      数量      |------------------------|");
        System.out.println("|                |   垃圾    |    正常    |");
        System.out.println("|----------------|------------------------|");
        System.out.println("|预测类  |  垃圾  |    "+ SpamMailDetectionJointProbability.TP+"      |      "+ SpamMailDetectionJointProbability.FP+"    |");
        System.out.println("|       |  正常  |    "+ SpamMailDetectionJointProbability.FN+"      |      "+ SpamMailDetectionJointProbability.TN+"    |");
        System.out.println("|----------------|------------------------|");
        XYDataset xyDataset = new XYSeriesCollection();
        XYSeries xySeries = new XYSeries("111");
        xySeries.add(0.1,0.2);
        xySeries.add(0.3,0.4);
        ((XYSeriesCollection) xyDataset).addSeries(xySeries);

        //SpamMailDetectionJointProbability.createLineChart(xyDataset,"11.jpg");


        /**
         * 邮件中出现ti时,该邮件为垃圾邮件的概率
         * P1(ti)为ti出现在垃圾邮件中的次数/垃圾邮件分词总数
         * P2(ti)为ti出现在正常邮件中的次数/正常邮件分词总数
         * P(Spam|ti)=P1(ti)/((P1(ti)+P2(ti))
         * P(ham|ti)=P2(ti)/((P1(ti)+P2(ti))
         * 1. P(ti|Spam)=P(Spam|ti)*P(ti)/P(Spam)
         *    P(ti|ham)=P(ham|ti)*P(ti)/P(ham)
         *            =[1-P(Spam|ti)]*P(ti)/[1-P(Spam)]
         * 2. P(ti|Spam)=P1(ti)
         *    P(ti|ham)=P2(ti)
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

        /*if (spamMailRateMap.containsKey(s)) {
            double spamTmp = spamMailRateMap.get(s);
            rate *= spamTmp * (((double)(spamMailMap.get(s) + (hamMailMap.containsKey(s)? hamMailMap.get(s): 0))) / (double) (spamMailSegNum + hamMailSegNum)) / spamTrainRate;
            tempRate *= (1d - spamTmp) * (((double)spamMailMap.get(s)+(hamMailMap.containsKey(s)? hamMailMap.get(s): 0)) / (double) (spamMailSegNum + hamMailSegNum)) / spamTrainRate;;
            //当有一个概率非常趋近于0时，需要进行判断
            // 判断该邮件更接近垃圾邮件还是正常邮件
            if (tempRate == 0d && rate == 0d){//同时趋于0,则概率设为0.5
                probability[testNum] = 0.5d;
            }else if (tempRate == 0d){//正常邮件概率趋于0
                probability[testNum] = 1d;
            }else{//垃圾邮件趋于0
                probability[testNum] = 0d;
            }
            if (tempRate == 0d || rate == 0d){//有趋近，则后续不用进行设置概率
                flag = true;
                break;
            }
        }*/

        /*//放大概率,当概率差量级大于10^300，直接判定结果
                        if (rate < Math.pow(10, -300) || tempRate < Math.pow(10, -300)){
                            if (rate > 0){
                                probability[testNum] = 1d;
                                flag = 2;
                                break;
                            }else if (tempRate > 0){
                                probability[testNum] = 0d;
                                flag = 3;
                                break;
                            }else {
                                rate *= 100;
                                tempRate *= 100;
                            }
                        }*/
    }
}
