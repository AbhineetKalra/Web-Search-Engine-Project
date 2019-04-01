package edu.nyu.cs.cs2580;
import java.io.*;
import java.util.Scanner;
import java.lang.StringBuilder;
import java.io.File;
import java.util.regex.Pattern;

  public class Parser{

	public String ParseDocument(final File input) throws IOException{
		StringBuilder result = new StringBuilder();
	    StringBuilder body = new StringBuilder();
	    String title = "";
	    if(title != null && !title.isEmpty()) { 
	        title = input.getName().replace("_", " ");
	    }
	    result.append(title);		
		result.append("\t");
	    result.append((body.toString()));
	    result.append("\t0");
	    body.trimToSize();
	    result.trimToSize();
	    body = null;
		return result.toString();
	}

  public String readFileContent(File filename) throws IOException{
      StringBuilder content = new StringBuilder();
      BufferedReader reader = new BufferedReader(new FileReader(filename));
      try 
      {
        String line = null;
        while ((line = reader.readLine()) != null) {
          content.append(" " + line);
        }
      } 
      finally {
        reader.close();
      }
      return content.toString();
  }

  public String Stem(String html) throws IOException{

    // remove special char
    html = html.replaceAll("["+ Pattern.quote("`~!@#$%^&*()_-+=[]\\{}|;':\",./<>?") + "]", " ");
    StringBuilder result = new StringBuilder();
    char[] w = new char[501];
    Stemmer porter = new Stemmer();
    InputStream in = new ByteArrayInputStream( html.getBytes() );
    
    while(true) {
        int ch = in.read();
        if (Character.isLetter((char) ch)) {
          int j = 0;
          while(true) {
              ch = Character.toLowerCase((char) ch);
              w[j] = (char) ch;
              if (j < 500) j++;
              ch = in.read();
              if (!Character.isLetter((char) ch)) {

                for (int c = 0; c < j; c++) porter.add(w[c]);
                porter.stem();
                result.append(porter.toString());
                break;
            }
          }
        }

        if (ch < 0) break;
        result.append((char)ch);
    }
    porter = null;
    return result.toString();
  }

}