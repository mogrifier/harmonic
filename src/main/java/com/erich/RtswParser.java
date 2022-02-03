package com.erich;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.stream.Stream;

/*
Read a data file and put into a 2-d array. Need to skip max/min columns. Columns below
#Timestamp           Source    Bt-med    Bt-min    Bt-max    Bx-med    Bx-min    Bx-max    By-med    By-min    By-max    Bz-med    Bz-min    Bz-max  Phi-mean   Phi-min   Phi-max Theta-med Theta-min Theta-max  Dens-med  Dens-min  Dens-max Speed-med Speed-min Speed-max  Temp-med  Temp-min  Temp-max
Timestamp has a space in it so counts as two columns when parsing.
 2022-01-31 18:00:00      1      6.01      6.01      6.18     -4.27     -4.32     -2.66      3.90      3.61      4.50      1.09      1.06      3.28    132.76    120.53    139.77     11.04     10.29     32.08      4.81      4.79      6.05    460.50    455.50    471.70    147826    146383    195889

 */
public class RtswParser {

    //columns to read from feed
    int[] columns = new int[] {3, 6, 9, 12, 15, 18, 21, 24, 27};
    //need to know how many rows of data in order to make array. other dimension is columns.length
    double[][] rtsw;

    public double[][] readInputToArray(String file) throws FileNotFoundException {
        //read each line into a string vector
        ArrayList<String> rows = new ArrayList<>(500);
        FileReader fileReader = new FileReader(file);
        BufferedReader reader = new BufferedReader(fileReader);
        Stream<String> allData = reader.lines();
        Object[] lines = allData.toArray();
        //initialize array to hold subset of the lines of data post -tokenization
        double[][] data2d = new double[lines.length][columns.length];
        String line;
        String [] fields = new String[29];
        //tokenize on white space and read into array
        for (int i = 0; i < lines.length; i++) {
            //read line
            line = (String)lines[i];
            //parse
            fields = line.split("\\s+");
            //iterate fields and convert each string (in the right columns) to a double. store in array.
            for (int j = 0; j < columns.length; j++) {
                double value = Double.parseDouble(fields[columns[j]]);
                data2d[i][j] = value;
            }
        }

        return data2d;
    }

}
