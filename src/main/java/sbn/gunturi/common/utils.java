/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sbn.gunturi.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import org.apache.lucene.document.Document;
import sbn.gunturi.manager.TweetsIndexManager;

/**
 *
 * @author vamsigunturi
 */
public class utils {
    
    /**
     * Find all the userId related to the politicians name and surname
     * @param name Politician name
     * @param surname Politician surname
     * @return Values of the best result
     * @throws IOException
     */
    
    public String[] findUserTwitterId(String name, TweetsIndexManager tim) throws IOException {
        // Initialize a new Tweets index builder for the index of the all tweets
        
        String[] name_ary = name.split(" ");
        

        ArrayList<String> fieldValues = new ArrayList<String>();
        // Search user that match name + surname
        fieldValues.add((name_ary[0] + " " + name_ary[1]).toLowerCase());
        // Search user that match surname + name
        fieldValues.add((name_ary[1] + " " + name_ary[0]).toLowerCase());
        
        ArrayList<Document> results = tim.searchForField("name", fieldValues, 10000);
        
        // Variable that will mantain the max number of followers among the users found
        int max = 0;
        // User id related to the max
        String id = "";
        
        // For each document found 
        for (Document doc : results) {
            // check if the user that made it has the more influencer of our actual max
            if (Integer.parseInt(doc.get("followers")) >= max) {
                // And in case take it as new max
                max = Integer.parseInt(doc.get("followers"));
                id = doc.get("screenName");
            }
        }
        // Return the max
        String[] result = {id, new Integer(max).toString()};
        return result;
    }
    
    
    // To write the strings in an array list to an external CSV file
    public void arraylist_to_csv(ArrayList<String> al, String file_name) {
    
        try (PrintWriter writer = new PrintWriter(new File("input/" + file_name))) {
             
             StringBuilder sb = new StringBuilder();
             
             for (String polName: al) {
                    
                     sb.append(polName);
                    
                     sb.append('\n');

             }
             
             writer.write(sb.toString());
             
             System.out.println("done! writing" + file_name);
     

        } catch (FileNotFoundException e) {
              System.out.println(e.getMessage());
        }
    }
    
    
    public ArrayList<String> csv_to_arraylist(String file_name) throws IOException {
         
        ArrayList<String> list = new ArrayList<>();
        
        try(BufferedReader br = new BufferedReader(new FileReader("input/" + file_name))) {
            String line = "";
            while ((line = br.readLine()) != null) {
                list.add(line);
            }
        } catch (FileNotFoundException e) {
          //Some error logging
        }
        
        System.out.println("List count: " + list.size());
        return list;
    }
    
}
