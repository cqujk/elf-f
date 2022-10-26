package gr.aueb.delorean.chimp;

import java.io.IOException;

/**
 * Decompresses a compressed stream created by the Compressor. Returns pairs of timestamp and floating point value.
 *
 * @author Michael Burman
 */
public class ChimpDecompressor32 {

    private int storedLeadingZeros = Integer.MAX_VALUE;
    private int storedTrailingZeros = 0;
    private int storedVal = 0;
    private boolean first = true;
    private boolean endOfStream = false;

    private InputBitStream in;

    private final static int NAN_INT = 0x7fc00000;

	public final static short[] leadingRepresentation = {0, 8, 12, 16, 18, 20, 22, 24};
    
    public ChimpDecompressor32(byte[] bs) {
    	in = new InputBitStream(bs);
    }

    /**
     * Returns the next pair in the time series, if available.
     *
     * @return Pair if there's next value, null if series is done.
     */
    public Value readPair() {
        try {
			next();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}
        if(endOfStream) {
            return null;
        }
        return new Value(storedVal);
    }

    private void next() throws IOException {
        if (first) {
        	first = false;
            storedVal = in.readInt(32);
            if (storedVal == NAN_INT) {
            	endOfStream = true;
            	return;
            }

        } else {
        	nextValue();
        }
    }

    private void nextValue() throws IOException {

        // Read value
        if (in.readBit() == 1) {
            if (in.readBit() == 1) {
                // New leading zeros
                storedLeadingZeros = leadingRepresentation[in.readInt(3)];
            }
            int significantBits = 32 - storedLeadingZeros;
            if(significantBits == 0) {
                significantBits = 32;
            }
            int value = in.readInt(32 - storedLeadingZeros);
            value = storedVal ^ value;
            if (value == NAN_INT) {
            	endOfStream = true;
            	return;
            } else {
            	storedVal = value;
            }

        } else if (in.readBit() == 1) {
        	storedLeadingZeros = leadingRepresentation[in.readInt(3)];
        	int significantBits = in.readInt(5);
        	if(significantBits == 0) {
                significantBits = 32;
            }
            storedTrailingZeros = 32 - significantBits - storedLeadingZeros;
            int value = in.readInt(32 - storedLeadingZeros - storedTrailingZeros);
            value <<= storedTrailingZeros;
            value = storedVal ^ value;
            if (value == NAN_INT) {
            	endOfStream = true;
            	return;
            } else {
            	storedVal = value;
            }
        }
        // else -> same value as before
    }

}