package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Logger {
  
  public static String TrackClick(String log){
    try {
      Files.write(Paths.get("data/clickLog.csv"), log.getBytes(), StandardOpenOption.APPEND);
      return "true";
    }catch (Exception e) {
      e.printStackTrace();
      //exception handling left as an exercise for the reader
    }
    return "false";
  }
  
}