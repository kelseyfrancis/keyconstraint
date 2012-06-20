package keyconstraint.identifykey.audio.writer;

import java.io.BufferedOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

class AudioOutputStream extends FilterOutputStream {

    AudioOutputStream(OutputStream out) {
        super(out instanceof BufferedOutputStream ? out : new BufferedOutputStream(out, 65536));
    }

    public void writeLittleEndian(int value) throws IOException {
        out.write((value >>>  0) & 0xFF);
        out.write((value >>>  8) & 0xFF);
        out.write((value >>> 16) & 0xFF);
        out.write((value >>> 24) & 0xFF);
    }

    public void writeLittleEndian(short value) throws IOException {
        out.write((value >>>  0) & 0xFF);
        out.write((value >>>  8) & 0xFF);
    }

    public void writeBigEndian(int value) throws IOException {
        out.write((value >>> 24) & 0xFF);
        out.write((value >>> 16) & 0xFF);
        out.write((value >>>  8) & 0xFF);
        out.write((value >>>  0) & 0xFF);
    }

    public void writeBigEndian(short value) throws IOException {
        out.write((value >>>  8) & 0xFF);
        out.write((value >>>  0) & 0xFF);
    }
}
