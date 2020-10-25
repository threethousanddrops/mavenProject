package com.shakespeare;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class WordCount {

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "word count");
        job.setJarByClass(WordCount.class);
        job.setMapperClass(TokenizerMapper.class);
        job.setReducerClass(IntSumReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        Path outPath=new Path(args[1]);
        FileSystem fs = FileSystem.get(conf);
        if (fs.exists(outPath)) {
            fs.delete(outPath, true);
        }
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, outPath);
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
    public static class TokenizerMapper extends Mapper<Object, Text, Text, Text> {
        private Text word = new Text();
        private Set<String> stopWords=new HashSet<>();
        private Set<String> punc=new HashSet<>();
        protected void setup(Context context) throws IOException, InterruptedException{
            FileSystem fs = FileSystem.get(context.getConfiguration());

            Path path1 = new Path("hdfs://lyyq181850099-master:9000/wordcount/stop-word-list.txt");
            BufferedReader reader1 = new BufferedReader(new InputStreamReader(fs.open(path1)));
            String line;
            while ((line = reader1.readLine()) != null) {
                stopWords.add(line.toLowerCase());
            }
            reader1.close();

            Path path2 = new Path("hdfs://lyyq181850099-master:9000/wordcount/punctuation.txt");
            BufferedReader reader2 = new BufferedReader(new InputStreamReader(fs.open(path2)));
            String line2;
            while ((line2 = reader2.readLine()) != null) {
                punc.add(line2.toLowerCase());
            }
            reader2.close();
        }

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            StringTokenizer itr = new StringTokenizer(value.toString());
            while (itr.hasMoreTokens()) {
                String str=itr.nextToken().toLowerCase();
                str = str.replaceAll("\\d+","");
                for (String pun: this.punc ){
                    str=str.replaceAll(pun,"");
                }
                if(this.stopWords.contains(str)||str.length()==0)
                    continue;
               else{
                    this.word.set(str);
                    context.write(this.word, new Text("1"));
                }
            }

        }
    }
    public static class IntSumReducer extends Reducer<Text, Text, Text, Text> {
        private IntWritable result = new IntWritable();
        private Text word = new Text();
        private static TreeMap<Integer, String> treeMap = new TreeMap<Integer, String>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o2.compareTo(o1);
            }
        });
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException
        {
            int sum = 0;
            for (Text val: values)
            {
                sum+=Integer.valueOf(val.toString());
            }
//            this.result.set(sum);
//            context.write(key, this.result);
            treeMap.put(new Integer(sum), key.toString());
             if(treeMap.size()>100) {
                 treeMap.remove(treeMap.lastKey());
             }
        }

        protected void cleanup(Context context)
            throws IOException,InterruptedException{
            Set<Map.Entry<Integer, String>> set = treeMap.entrySet();
            int i =1;
            for (Map.Entry<Integer, String> entry : set) {
                this.result.set(entry.getKey());
                this.word.set(i+":"+entry.getValue()+", "+entry.getKey());
                context.write(word, new Text(""));
                i++;
            }
        }
    }


}
