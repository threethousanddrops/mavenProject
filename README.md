# mavenProject

实验编译环境：本地vscode& java14 & apache-maven3.6.3 
实验运行环境：BDKIT

运行命令：hadoop jar target/mavenWordCount-1.0-SNAPSHOT.jar com.shakespeare.WordCount /wordcount/input /wordcount/output -skip
运行截图：
![web](https://github.com/threethousanddrops/mavenProject/blob/main/images/web.png)
![output1](https://github.com/threethousanddrops/mavenProject/blob/main/images/output1.png)
![output2](https://github.com/threethousanddrops/mavenProject/blob/main/images/output2.png)
![output3](https://github.com/threethousanddrops/mavenProject/blob/main/images/output3.png)


一．配置java和maven环境。（由于起初没有记录实验思路的要求，配置环境等过程没有截图）
 ①下载java与maven安装包。
 ②修改环境变量，本地cmd中测试java -version与mvn -v成功。
 ③vscode中安装java相应的extensions插件。
 ④新建本地空白文件夹maven-repository用作maven本地库，
 ⑤打开...\maven\conf\settings.xml文件，查找下面这行代码：
  <localRepository>/path/to/local/repo</localRepository>
 可以看到localRepository节点默认是被注释掉的，需要把它移到注释之外，然后将localRepository节点的值改为创建的目录maven-repository。从maven中获取jar包的时候，maven首先会在本地仓库中查找，如果本地仓库有则返回；如果没有则从远程仓库中获取包，并在本地库中保存。此外，在maven项目中运行mvn install，项目将会自动打包并安装到本地仓库中。
 ⑥运行DOS命令
  mvn help:system
 如果前面的配置成功，maven-repository会出现相应的文件。
 ⑦配置vscode中java.home、maven.executable.path以及java.configuration. maven.userSettings。
 
二．创建maven项目、编写代码。
  ①在本地vscode中create java project，选择maven-archetype-quickstart，创建maven项目。
  ②设置groupid等信息。
  ③编写代码。

三、设计思路
  ①在wordcount2.0的基础上实现功能，首先考虑读入stop-word-list.txt和punctuation.txt，在操作对象文件里除去停词，wordcount2.0只实现了一个停词文件路径读入，要实现同时去除stopword和punctuation，有两个想法：一是设置两个读入的文件路径，加入patternsToskip进行replace；二是将stop-listword.txt通过parseSkipFile读入后消去，而标点符号可以直接通过正则表达式replace掉，不需要另外设置punctuation.txt的读入路径。由于最后在wordcount2.0的框架下没能成功实现读入两个文件，于是直接在class TokenizerMapper中设置setup函数，通过绝对路径（例如：hdfs://lyyq181850099-master:9000 /wordcount /stop-word-list. txt）直接读入两个文件，进行处理。
  ②实现去除所有数字，在class TokenizerMapper中的map函数里通过正则表达式实现忽略数字的功能。
  ③实现降序输出，mapreduce默认的输出形式是升序，此处有两个想法；一是新建立一个sortjob完成降序排列功能，将统计和排序分开进行；二是在reduce一个细节的过程中进行排序，统计sum之后排序再进行context.write。由于java水平不够，新建立sortjob实现过程中发现运行jar速度非常慢，map进展情况没有达到预期且特别占用内存，于是根据查找到的示例尝试用在reduce中建立treemap进行降序处理。
  ④输出过程格式要求为“<排名>：<单词>，<次数>”，尝试在reduce时word.set过程中将word指定为要求输出格式，然后在value值中设置空白输出，可以实现指定要求的降序输出，与此同时在循环中计数，输出一百个之后return。
  ⑤main的实现基本在wordcount2.0的框架上进行，其中一个细节需要注意：在实现输出之前判断output路径是否存在，如果已经存在，则删除已经存在的output文件夹，以免测试过程中不断生成新的output文件夹占用内存。
