package com.erich;

import org.apache.commons.io.FileUtils;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.util.ArrayList;

public class Main {

    private static final int DISSONANT = 0;
    private static final int HARMONIC = 1;
    double [] sinValues;
    static public final int MAXFREQ = 22050;
    static public final int MINFREQ = 54;

    public static void main(String[] args) throws IOException{
	// write your code here

        Main main = new Main();
        //byte[] sum = main.chord();
        //this works- additive synthesis. using a weighted average will allow emphasizing harmonics.
       // main.writeWave("chord.wav", sum);

        //main.audio1Second(true);

        main.audioChannelGenerator(4, HARMONIC);
    }


    private void audioChannelGenerator(int channelCount, int type) throws IOException {

        RtswParser rtsw = new RtswParser();
        double[][] raw = rtsw.readInputToArray("rtsw_plot_data_2022-01-31T18_00_00.txt");
        //need 9 freqs. test values here.
        double[] baseFreq = new double[]{65.41, 110, 392, 440, 329.6, 659.3, 880, 1760, 1319};
        //frequency scaling factor per column. Affects amount of tuning variation
        double[] scale = new double[]{10, 20, 5, 30, 25, 17, 40, 110, 52};
        //now use raw data to generate 1 second audio buffers
        //raw.length vs 200
        for (int j = 0; j < channelCount; j++) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            //too much data- shorten amount used.
            for (int i = 0; i < 500; i++ ) {
                double[] row = raw[i];
                double freq= 0;
                //iterate through each channel
                switch (type) {
                    case DISSONANT:
                        freq = getDissonantFreq(Math.sin(raw[i][j]));
                        break;
                    case HARMONIC:
                        freq = getHarmonicFreq(Math.sin(raw[i][j]));
                        break;
                }
                  // Math.round(MINFREQ + ((Math.sin(raw[i][j]) + 1)/2 * 1365)); //scale[j] ); //removed base freq for fun and change scale

                /*
                Much, much more interesting just using raw frequencies (no base). Very sci-fi.May want a floor (25 or so)

                Now, imagine FM happening.

                 */

                //need random length in seconds
                int time = (int)(Math.random()*5 + 5);
                //save to bytearray, growing each channel successively
                //byte[] myStream = createSineWave(freq, time);
                //System.out.println(myStream.length);
                baos.write(createSineWave(freq, time));

                //by the way, could use ANY type of soundwave form. Could have several and randomly choose.
                //could also use algorithm to make system harmonically aware and generate freqs from a set that go well together.

                //RTSW data is causing creation of almost every whole number freq from 25 to 1390. Hmm.
                //writeWave(freq + "_sine.wav", (byte[])channels[j]);
            }
            //writing a channel's complete audio at a time since exceeding heap memory (even at 10GB)
            writeWaveWithHeader(j + "channel.wav", baos.toByteArray());
            System.out.println("wrote channel file " + j);

        }

    }

    private double getHarmonicFreq(double sin) {

        return MINFREQ * Math.round((sin + 2) * 5);
    }


    private double getDissonantFreq(double sin) {

        return Math.round(MINFREQ + ((sin + 1)/2 * 1365));
    }

    /*
    approach for generating and mixing will be to create files for every channels data.
    Then read in the files a buffer at a time, averaging them out. May provide simpler way to
    fade in and out. Will certainly be easier to produce final mix with vary odd swirling sounds
    due to randomize time length of the chunks of data at a given freq in a channel.

    just drop the individual channels in a DAW and mix, fx, etc.
     */
    private void audio1Second(boolean stereo) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        RtswParser rtsw = new RtswParser();
        double[][] raw = rtsw.readInputToArray("rtsw_plot_data_2022-01-31T18_00_00.txt");
        //need 9 freqs. test values here.
        double[] baseFreq = new double[]{65.41, 110, 392, 440, 329.6, 659.3, 880, 1760, 1319};
        //frequency scaling factor per column. Affects amount of tuning variation
        double[] scale = new double[]{10, 20, 5, 30, 25, 17, 40, 110, 52};
        //now use raw data to generate 1 second audio buffers

        //raw.length vs 200
        for (int i = 0; i < 200; i++ ) {
            double[] row = raw[i];
            Object[] channels = new Object[row.length];

            for (int j = 0; j < row.length; j++) {
                //testing 1 second wave creation
                double freq = Math.round(MINFREQ + ((Math.sin(raw[i][j]) + 1)/2 * 1365)); //scale[j] ); //removed base freq for fun and change scale

                /*
                Much, much more interesting just using raw frequencies (no base). Very sci-fi.May want a floor (25 or so)

                Now, imagine FM happening.

                 */


                channels[j] = createSineWave(freq, 7);
                //RTSW data is causing creation of almost every whole number freq from 25 to 1390. Hmm.
                //writeWave(freq + "_sine.wav", (byte[])channels[j]);
            }

            /*
            Tried to adjust volume using 3/log(freq). skews everything off center. Better choice
            would be to apply a low pass filter to de-emphasize the high values. Not sure
            how to adjust volume properly.

             */

            byte avg[];
            if (stereo) {
                avg = averageStereoChannels(channels);
            }
            else {
                avg = averageChannels(channels);
            }

            //average all for 1 second of data

            //append each byte array to overall data stream of audio bytes
            baos.write(avg);
        }

        //save the resulting audio
        writeWave("average.wav", baos.toByteArray());
    }

    /*
    Averaging requires computing the 16bit value, averaging it, then writing back as LSB MSB
     */
    private byte[] averageChannels(Object[] channels) {
        byte[] average = new byte[((byte[])channels[0]).length];
        int running = 0;
        byte[] temp;
        for (int j = 0; j < average.length; j+=2) {
            running = 0;
            for (int i = 0; i < channels.length; i++) {
                // byte length of 1 second of audio for test
                temp = (byte[])channels[i];
                int chValue = sampleToInt(temp[j], temp[j + 1]);
                running += chValue;
            }

            int avg = running / channels.length;
            //save back as lsb msb
            average[j] = (byte) ((avg % 256) - 128);
            average[j + 1] = (byte)((avg / 256) - 128);
        }

        return average;
    }


    private byte[] averageStereoChannels(Object[] channels) {
        byte[] average = new byte[((byte[])channels[0]).length];
        int runningLeft = 0;
        int runningRight = 0;
        byte[] temp;
        for (int j = 0; j < average.length; j+=4) {
            runningRight = 0;
            runningLeft = 0;
            for (int i = 0; i < 5; i++) {
                // byte length of 1 second of audio for test
                temp = (byte[])channels[i];
                int chValue = sampleToInt(temp[j], temp[j + 1]);
                runningRight += chValue;
            }

            int avgRight = runningRight / 5;

            for (int i = 5; i < channels.length; i++) {
                // byte length of 1 second of audio for test
                temp = (byte[])channels[i];
                int chValue = sampleToInt(temp[j], temp[j + 1]);
                runningLeft += chValue;
            }

            int avgLeft = runningLeft / 4;


            //save back as lsb msb
            average[j] = (byte) ((avgRight % 256) - 128);
            average[j + 1] = (byte)((avgRight / 256) - 128);

            average[j + 2] = (byte) ((avgLeft % 256) - 128);
            average[j + 3] = (byte)((avgLeft/ 256) - 128);
        }

        return average;
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


    private byte[] createSineWave(double freq, int seconds) {
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
            pos = (int)(index++ * freq/2) % sinValues.length;
            //lsb
            samples[samplePos] = (byte)(((((sinValues[pos] + 1)/2) * 65535) % 256) - 128);
            //msb
            samples[samplePos + 1] = (byte)((((sinValues[pos] + 1)/2) * 65535/256) - 128);
            //byte conversion- samples[index++] = (byte)(((sinValues[pos] + 1)/2) * 255 - 128);
            samplePos += 2;
        }

        return samples;
    }


    private byte[] writeSineWave(String name, double freq, int seconds) {
        byte [] samples = createSineWave(freq, seconds);
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


    private void writeWaveWithHeader(String name, byte[] samples) throws IOException {
        File out = new File(name);
        //mono 16 bit audio, 44100Hz, LE format
        final boolean bigEndian = false;
        final boolean signed = true;
        final int bits = 16;
        final int channels = 1;
        final int sampleRate = 44100;

        AudioFormat format = new AudioFormat((float)sampleRate, bits, channels, signed, bigEndian);
        ByteArrayInputStream bais = new ByteArrayInputStream(samples);
        AudioInputStream audioInputStream = new AudioInputStream(bais, format, samples.length);
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, out);
        audioInputStream.close();
    }
}
