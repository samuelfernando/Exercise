/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.shef.zeno.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;

/**
 *
 * @author samf
 */
public class ReadConfig {
    public static HashMap<String, String> readConfig() {
        
        HashMap<String, String> configs = new HashMap<String, String>();
       
        try {
            BufferedReader br;
            br = new BufferedReader(new FileReader("C:\\Users\\samf\\Documents\\NetBeansProjects\\zeno-r25-config.txt"));
            String line;
            while ((line=br.readLine())!=null) {
                System.out.println(line);
                String[] split = line.split("\t");
                configs.put(split[0], split[1]);
            }
        } catch (Exception e) {
            e.printStackTrace();   
        }
        return configs;
        
    }
}
