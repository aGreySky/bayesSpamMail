# 基于贝叶斯算法的垃圾邮件检测Java实现
### 简介
这是一个垃圾邮件检测项目  
以下实现了两种分类算法  
[SpamMailDetectionBayes](src/main/java/SpamMailDetectionBayes.java)为使用贝叶斯公式的算法实现；
[SpamMailDetectionJointProbability](src/main/java/SpamMailDetectionJointProbability.java)为使用联合概率公式的算法实现；  
[probability-Bayes](probability-Bayes.jpg)、[probability-JointProbability](probability-JointProbability.jpg)为两种方法对应的概率分布图；  
[ROC-Bayes](ROC-Bayes.jpg)、[ROC-JointProbability](ROC-JointProbability.jpg)分别是两种方法对应的ROC曲线图。  
两种方法都能正确进行分类，但贝叶斯法结果较为准确。 
###评价指标

**贝叶斯法**

```
------------------阈值（0.50）-----------------

 ------------------------------------------ 
|                |         实际类         |
|      数量      |------------------------|
|                |   垃圾    |    正常    |
|----------------|------------------------|
|预测类  |  垃圾  |    6217     |     147   |
|       |  正常  |    183      |      3453 |
|----------------|------------------------|

正确率（accuracy）= (TP+TN)/(P+N) = 0.97
特效度（specificity）= TN/N = 0.96
精度（precision）= TP/(TP+FP) = 0.98
召回率（recall）= 灵敏度（sensitive）= TP/(TP+FN) = 0.97
综合分类率（F1）= 2 * precision * recall / (precision + recall) = 0.97
```

**联合概率法**

```
------------------阈值（0.50）-----------------

 ------------------------------------------ 
|                |         实际类         |
|      数量      |------------------------|
|                |   垃圾    |    正常     |
|----------------|------------------------|
|预测类  |  垃圾  |    6207     |     147   |
|       |  正常  |    193      |      3453 |
|----------------|------------------------|

正确率（accuracy）= (TP+TN)/(P+N) = 0.97
特效度（specificity）= TN/N = 0.96
精度（precision）= TP/(TP+FP) = 0.98
召回率（recall）= 灵敏度（sensitive）= TP/(TP+FN) = 0.97
综合分类率（F1）= 2 * precision * recall / (precision + recall) = 0.97
```
### 参考：
邮件数据集：https://plg.uwaterloo.ca/~gvcormac/treccorpus06/about.html  
分词：https://github.com/huaban/jieba-analysis/tree/master/src/main/java/com/huaban/analysis/jieba  
Java图表：http://www.jfree.org/jfreechart/api/javadoc/index.html  
本项目详细讲解CSDN：https://blog.csdn.net/aGreySky/article/details/100745680  
