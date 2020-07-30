package correcter;

import java.io.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        File send = new File("send.txt");
        File encoded = new File("encoded.txt");
        File received = new File ("received.txt");
        File decoded = new File ("decoded.txt");
        Scanner input = new Scanner(System.in);

        System.out.print("Write a mode: ");
        String mode = input.nextLine();
        switch (mode) {
            case "encode":
                encodeText(send, encoded);
                break;
            case "send":
                generateErrors(encoded, received);
                break;
            case "decode":
                decodeText(received, decoded);
                break;
            default:
                System.out.println("Wrong mode");
        }
    }

    private static void encodeText(File input, File output) {
        try (InputStream inputStream = new FileInputStream(input);
             OutputStream outputStream = new FileOutputStream(output, false)) {

            //reading file
            ArrayList<Integer> bytes = new ArrayList<>();
            int symbol = inputStream.read();
            while (symbol != -1) {
                bytes.add(symbol);
                symbol = inputStream.read();
            }
            byte[] outputBytes = new byte[2];
            int buf;    //using integer instead of byte as buffer for bitwise operations to avoid two's complement issues
            for (Integer b : bytes) {
                buf = 0;                                //writing buffer with zeroes
                buf ^= (b.byteValue() & 0x80) >>> 2;    //writing data bits to buffer
                buf ^= (b.byteValue() & 0x70) >>> 3;
                outputBytes[0] = writeParity(buf);      //calling parity calculation and writing buffer to byte array
                buf = 0;
                buf ^= (b.byteValue() & 0x8) << 2;
                buf ^= (b.byteValue() & 0x7) << 1;
                outputBytes[1] = writeParity(buf);
                outputStream.write(outputBytes);        //writing byte array to target file
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void generateErrors(File input, File output) {
        try (InputStream inputStream = new FileInputStream(input);
             OutputStream outputStream = new FileOutputStream(output, false)) {
            Random random = new Random();
            int symbol = inputStream.read();
            while (symbol != -1) {
                symbol ^= 1 << random.nextInt(8);
                outputStream.write(symbol);
                symbol = inputStream.read();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void decodeText(File input, File output) {
        try (InputStream inputStream = new FileInputStream(input);
             OutputStream outputStream = new FileOutputStream(output, false)) {

            //reading file
            ArrayList<Integer> bytes = new ArrayList<>();
            int symbol = inputStream.read();
            while (symbol != -1) {
                bytes.add(symbol);
                symbol = inputStream.read();
            }
            byte[] outputByteArray = new byte[1];
            int buf;
            int outputByte;
            for (int i = 0; i < bytes.size(); i = i + 2) {
                outputByte = 0;
                buf = correctErrors(bytes.get(i));
                outputByte ^= (buf & 0x20) << 2;
                outputByte ^= (buf & 0xe) << 3;
                buf = correctErrors(bytes.get(i+1));
                outputByte ^= (buf & 0x20) >>> 2;
                outputByte ^= (buf & 0xe) >>> 1;
                outputByteArray[0] = (byte) outputByte;
                outputStream.write(outputByteArray);
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //writes parity bits based on data bits written to the passed buffer
    private static byte writeParity(int buf) {
        int d1 = (buf & 0x20) >>> 5;    //getting individual data bits values
        int d2 = (buf & 0x8) >>> 3;
        int d3 = (buf & 0x4) >>> 2;
        int d4 = (buf & 0x2) >>> 1;
        buf ^= (d1 ^ d2 ^ d4) << 7;     //calculating and writing parity bits
        buf ^= (d1 ^ d3 ^ d4) << 6;
        buf ^= (d2 ^ d3 ^ d4) << 4;
        return (byte) buf;              //converting result to byte for outputStream
    }

    private static int correctErrors(int hammingByte) {
        int d1 = (hammingByte & 0x20) >>> 5;    //getting individual data bits values
        int d2 = (hammingByte & 0x8) >>> 3;
        int d3 = (hammingByte & 0x4) >>> 2;
        int d4 = (hammingByte & 0x2) >>> 1;
        int corruptedBitPosition = 0;             //determining the corrupted bit position
        if (((hammingByte & 0x80) >>> 7) != (d1 ^ d2 ^ d4)) corruptedBitPosition += 1;
        if (((hammingByte & 0x40) >>> 6) != (d1 ^ d3 ^ d4)) corruptedBitPosition += 2;
        if (((hammingByte & 0x10) >>> 4) != (d2 ^ d3 ^ d4)) corruptedBitPosition += 4;
        System.out.print(corruptedBitPosition + " ");
        //if it wasn't the unused 8th bit that was corrupted, reverse the corrupted bit
        if (corruptedBitPosition > 0) hammingByte ^= (1 << (8 - corruptedBitPosition));
        System.out.println(Integer.toString(hammingByte, 2));
        return hammingByte;
    }
}
