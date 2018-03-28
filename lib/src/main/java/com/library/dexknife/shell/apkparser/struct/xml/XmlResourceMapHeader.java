package com.library.dexknife.shell.apkparser.struct.xml;


import com.library.dexknife.shell.apkparser.struct.ChunkHeader;

/**
 * @author dongliu
 */
public class XmlResourceMapHeader extends ChunkHeader {
    public XmlResourceMapHeader(int chunkType, int headerSize, long chunkSize) {
        super(chunkType, headerSize, chunkSize);
    }
}
