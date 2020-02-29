package org.btn.common;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class BtnLog {
    private final static int INFO = 0;
    private final static int ERR  = 1 ;
    private final static int WARN = 2 ;
    private boolean logToFile = false;
    public static String logPath = "";
    private SimpleDateFormat df = new SimpleDateFormat("MM.dd HH:mm:ss");
    String _name;
    long startTime = System.currentTimeMillis();
    long lastLogTime = System.currentTimeMillis();
    public boolean silent = false;
    public BtnLog(String name){ this(name,false); }
    public BtnLog(String name, boolean silient){ this(name,silient,false); }
    public BtnLog(String name, boolean silient, boolean logToFile){
        this._name = name;
        this.silent = silient;
        this.logToFile = logToFile;
    }
    public String time(String title){
        return "Time eclipsed for StopWatcher["+_name+"] from start==["+timeFromStart()+"]"+ title;
    }
    public boolean isSilent(){
        return this.silent;
    }
    public String time(){ return this.time("");}
    public long timeFromStart(){return System.currentTimeMillis() - startTime;}
    public long timeFromLastLog(){return System.currentTimeMillis() - lastLogTime;}
    public void log(String s){logInternal(s,INFO,false); }
    public void log(String s, boolean forcePrint){logInternal(s,INFO,forcePrint); }

    public void err(String s) { logInternal(s,ERR,true); }
    private void logInternal(String s, int type, boolean forcePrint){
        if(!forcePrint && isSilent())
            return;
        String s1 = String.format(Locale.getDefault()
                ,"BtnLog::%s:T[%d]:[%s/FS%d/FL%d]:%s"
                ,this._name
                , Thread.currentThread().getId()
                ,df.format(new Date())
                ,timeFromStart()
                ,timeFromLastLog()
                ,s);

        this.lastLogTime = System.currentTimeMillis();
        if(type == ERR || type == WARN) {
            System.err.println(s1);
        }else {
            System.out.println(s1);
        }
        if(this.logToFile){
            String fileName = logPath+_name+".log";
            File file = new File(fileName);
            if(file.exists() && file.length() > 1024*1024){
                file.renameTo(new File(fileName+ new Date()));
                file = new File(fileName);
            }

            try {
                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));//true for append
                out.println(s1);
                out.close();
            } catch (IOException e) {
                //exception handling left as an exercise for the reader
            }
        }
    }

    public void err(String title, Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String s1 = title +"\n"+sw.toString();
        this.err(s1);
    }

    public void info(String s) {
        this.log(s);
    }
    public void info(String s, boolean forcePrint) {
        this.log(s,forcePrint);
    }
    private long markStartTime = System.currentTimeMillis();
    public void markTime() {
        markStartTime = System.currentTimeMillis();
    }

    public long timeFromLastMark(){
        return System.currentTimeMillis() - markStartTime;
    }
    public void warn(String s) {
        this.logInternal(s,WARN,false);
    }
    public void warn(String s, Throwable t) {
        this.logInternal(s,WARN,false);
    }
}