package com.shakespeare;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
//import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.util.GenericOptionsParser;

public class WordCount {

  public static class TokenizerMapper 
         extends Mapper<Object, Text, Text, Text> {
    //static enum CountersEnum { INPUT_WORDS }

    private Text word = new Text();
    
    private Set<String> patternsToSkip=new HashSet<>();
    private Set<String> punctuation=new HashSet<>();

    protected void setup(Context context) throws IOException, InterruptedException{
        FileSystem fstm = FileSystem.get(context.getConfiguration());

        Path path1 = new Path("hdfs://lyyq181850099-master:9000/wordcount/stop-word-list.txt");
        BufferedReader reader1 = new BufferedReader(new InputStreamReader(fstm.open(path1)));
        String line;
        while ((line = reader1.readLine()) != null) {
            patternsToSkip.add(line.toLowerCase());
        }
        reader1.close();

        Path path2 = new Path("hdfs://lyyq181850099-master:9000/wordcount/punctuation.txt");
        BufferedReader reader2 = new BufferedReader(new InputStreamReader(fstm.open(path2)));
        String line2;
        while ((line2 = reader2.readLine()) != null) {
            punctuation.add(line2.toLowerCase());
        }
        reader2.close();
    }

    public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
        StringTokenizer itr = new StringTokenizer(value.toString());
        while (itr.hasMoreTokens()) {
            String wordstr=itr.nextToken().toLowerCase();
            /*for (String pun: punctuation ){
                wordstr=wordstr.replaceAll(pun,"");
            }
            for (String pskip: patternsToSkip){
              wordstr=wordstr.replaceAll(pskip,"");
            }*/
            wordstr=wordstr.replaceAll("\\d+","");
            if(this.patternsToSkip.contains(wordstr)||this.punctuation.contains(wordstr)||wordstr.length()<3)
                continue;
           else{
                word.set(wordstr);
                context.write(word, new Text("1"));
                /*Counter counter = context.getCounter(CountersEnum.class.getName(),
                CountersEnum.INPUT_WORDS.toString());
                counter.increment(1);*/
            }
        }
    }
  }
  
public static class IntSumReducer
    extends Reducer<Text,Text,Text,Text> {
  private IntWritable result = new IntWritable();
  private Text word = new Text();

  private TreeMap<Integer,String> treeMap = new TreeMap<Integer,String>(new Comparator<Integer>(){
    @Override
    public int compare(Integer x,Integer y){return y.compareTo(x);}
  });
  
  public void reduce(Text key, Iterable<Text> values,Context context) 
                    throws IOException, InterruptedException {
      int sum = 0;
      for (Text val : values) {
          sum+= Integer.valueOf(val.toString());
      }
      //result.set(sum);
      treeMap.put(new Integer(sum), key.toString());
      //treeMap.put(sum,key.toString());
      //context.write(key, result);
    }

  protected void cleanup(Context context)
      throws IOException,InterruptedException{
    Set<Map.Entry<Integer, String>> set = treeMap.entrySet();
    int count =1;
    for (Map.Entry<Integer, String> entry : set) {
          this.result.set(entry.getKey());
          this.word.set(count+": "+entry.getValue()+", "+entry.getKey());
          context.write(word, new Text(""));
          count++;
          if(count>100) {return;}
       }
     }
  }
  
  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();

    String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
    if (otherArgs.length != 3) {
      System.err.println("Usage: wordcount <in> <out> -skip");
      System.exit(2);
    }
    
    Job job = Job.getInstance(conf, "word count");
    job.setJarByClass(WordCount.class);
    job.setMapperClass(TokenizerMapper.class);
    job.setCombinerClass(IntSumReducer.class);
    job.setReducerClass(IntSumReducer.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(Text.class);

    Path fileoutPath=new Path(args[1]);
        FileSystem f = FileSystem.get(conf);
        if (f.exists(fileoutPath)) 
        {
            f.delete(fileoutPath, true);
        }

    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, fileoutPath);
    System.exit(job.waitForCompletion(true) ? 0 : 1);
  }
}
