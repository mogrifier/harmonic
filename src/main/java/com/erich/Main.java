package com.erich;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class Main {

    double [] sinValues;
    static public final int MAXFREQ = 22050;

    public static void main(String[] args) {
	// write your code here

        Main main = new Main();
        byte[] sum = main.chord();
        //this works- additive synthesis. using a weighted average will allow emphasizing harmonics.
        main.writeWave("chord.wav", sum);
    }


    private int sampleToInt (byte lsb, byte msb) {
        return lsb + 128 + (msb + 128) * 256;
    }


    private byte[] chord() {
        //fixme freq should be a double for accuracy to western scale
        byte[] a3 = writeSineWave("A3sine.wav", 220, 3);
        byte[] c4 = writeSineWave("C4sine.wav", 262, 3);
        byte[] e4 = writeSineWave("E4sine.wav", 330, 3);

        //add the values and divide by 3 to normalize, then write to a file.
        // //What about 16 bit conversion first?? think you have to.
        byte[] sum = new byte[a3.length];

        for (int i = 0; i < a3.length; i+=2) {
            //look at as lsb msb pairs, average, and convert to new LSB MSB value

            int v1 = sampleToInt(a3[i], a3[i + 1]); // a3[i] + 128 + (a3[i+1] + 128) *256;
            int v2 = sampleToInt(c4[i], c4[i + 1]);; //c4[i] + 128 + (c4[i+1] + 128) *256;
            int v3 = sampleToInt(e4[i], e4[i + 1]);; //e4[i] + 128 + (e4[i+1] + 128) *256;
            //compute average
            int avg = (v1 + v2 + v3)/3;
            //lsb
            sum[i] = (byte)((avg % 256) - 128);
            //msb
            sum[i + 1] = (byte)((avg/256) - 128);
        }

        return sum;
    }


    private void computeSine() {
        //generate array of 16 bit data using sin function. Sin expects radians as the input.
        //using 44.1KHz sampling. This is a high precision approach requiring converting 16-bit short
        //to LSB  MSB byte ordering.
        double inc = Math.PI * 2 / MAXFREQ;
        sinValues = new double[MAXFREQ];
        for (int i =0; i < MAXFREQ; i++) {
            sinValues[i] = Math.sin(i * inc);
        }

    }

    private byte[] writeSineWave(String name, int freq, int seconds) {
        computeSine();

        // write samples to file
        //step through the table at higher rate to increase the frequency
        int sampleCount = 0;
        int pos = 0;
        int index = 0;
        int samplePos = 0;
        int depth = 2; //16 bit
        byte[] samples = new byte[44100 * seconds * depth];
        while (sampleCount++ < samples.length/2) {
            //write to new array
            //convert freq value to give desired frequency for value given
            pos = (index++ * freq/2) % sinValues.length;
            //lsb
            samples[samplePos] = (byte)(((((sinValues[pos] + 1)/2) * 65535) % 256) - 128);
            //msb
            samples[samplePos + 1] = (byte)((((sinValues[pos] + 1)/2) * 65535/256) - 128);
            //byte conversion- samples[index++] = (byte)(((sinValues[pos] + 1)/2) * 255 - 128);
            samplePos += 2;
        }

        writeWave(name, samples);

        return samples;
    }


    private void writeWave(String name, byte[] samples) {
        //write array to file. This is a 16 bit LE raw pcm audio file.
        try {
            FileUtils.writeByteArrayToFile( new File(name), samples);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
