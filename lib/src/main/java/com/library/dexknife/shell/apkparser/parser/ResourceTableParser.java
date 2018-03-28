package com.library.dexknife.shell.apkparser.parser;


import com.library.dexknife.shell.apkparser.struct.ChunkHeader;
import com.library.dexknife.shell.apkparser.struct.StringPoolHeader;
import com.library.dexknife.shell.apkparser.utils.Pair;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * parse android resource table file.
 * see http://justanapplication.wordpress.com/category/android/android-resources/
 *
 * @author dongliu
 */
public class ResourceTableParser {

    /**
     * By default the data buffer Chunks is buffer little-endian byte order both at runtime and when stored buffer files.
     */
    private ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
    private com.library.dexknife.shell.apkparser.struct.StringPool stringPool;
    private ByteBuffer buffer;
    // the resource table file size
    private com.library.dexknife.shell.apkparser.struct.resource.ResourceTable resourceTable;

    private Set<Locale> locales;

    public ResourceTableParser(ByteBuffer buffer) {
        this.buffer = buffer.duplicate();
        this.buffer.order(byteOrder);
        this.locales = new HashSet<>();
    }

    /**
     * parse resource table file.
     */
    public void parse() {
        // read resource file header.
        com.library.dexknife.shell.apkparser.struct.resource.ResourceTableHeader resourceTableHeader = (com.library.dexknife.shell.apkparser.struct.resource.ResourceTableHeader) readChunkHeader();

        // read string pool chunk
        stringPool = com.library.dexknife.shell.apkparser.utils.ParseUtils.readStringPool(buffer, (StringPoolHeader) readChunkHeader());

        resourceTable = new com.library.dexknife.shell.apkparser.struct.resource.ResourceTable();
        resourceTable.setStringPool(stringPool);

        com.library.dexknife.shell.apkparser.struct.resource.PackageHeader packageHeader = (com.library.dexknife.shell.apkparser.struct.resource.PackageHeader) readChunkHeader();
        for (int i = 0; i < resourceTableHeader.getPackageCount(); i++) {
            Pair<com.library.dexknife.shell.apkparser.struct.resource.ResourcePackage, com.library.dexknife.shell.apkparser.struct.resource.PackageHeader> pair = readPackage(packageHeader);
            resourceTable.addPackage(pair.getLeft());
            packageHeader = pair.getRight();
        }
    }

    // read one package
    private Pair<com.library.dexknife.shell.apkparser.struct.resource.ResourcePackage, com.library.dexknife.shell.apkparser.struct.resource.PackageHeader> readPackage(com.library.dexknife.shell.apkparser.struct.resource.PackageHeader packageHeader) {
        Pair<com.library.dexknife.shell.apkparser.struct.resource.ResourcePackage, com.library.dexknife.shell.apkparser.struct.resource.PackageHeader> pair = new Pair<>();
        //read packageHeader
        com.library.dexknife.shell.apkparser.struct.resource.ResourcePackage resourcePackage = new com.library.dexknife.shell.apkparser.struct.resource.ResourcePackage(packageHeader);
        pair.setLeft(resourcePackage);

        long beginPos = buffer.position();
        // read type string pool
        if (packageHeader.getTypeStrings() > 0) {
            buffer.position((int) (beginPos + packageHeader.getTypeStrings()
                    - packageHeader.getHeaderSize()));
            resourcePackage.setTypeStringPool(com.library.dexknife.shell.apkparser.utils.ParseUtils.readStringPool(buffer,
                    (StringPoolHeader) readChunkHeader()));
        }

        //read key string pool
        if (packageHeader.getKeyStrings() > 0) {
            buffer.position((int) (beginPos + packageHeader.getKeyStrings()
                    - packageHeader.getHeaderSize()));
            resourcePackage.setKeyStringPool(com.library.dexknife.shell.apkparser.utils.ParseUtils.readStringPool(buffer,
                    (StringPoolHeader) readChunkHeader()));
        }


        outer:
        while (buffer.hasRemaining()) {
            ChunkHeader chunkHeader = readChunkHeader();
            switch (chunkHeader.getChunkType()) {
                case com.library.dexknife.shell.apkparser.struct.ChunkType.TABLE_TYPE_SPEC:
                    long typeSpecChunkBegin = buffer.position();
                    com.library.dexknife.shell.apkparser.struct.resource.TypeSpecHeader typeSpecHeader = (com.library.dexknife.shell.apkparser.struct.resource.TypeSpecHeader) chunkHeader;
                    long[] entryFlags = new long[(int) typeSpecHeader.getEntryCount()];
                    for (int i = 0; i < typeSpecHeader.getEntryCount(); i++) {
                        entryFlags[i] = com.library.dexknife.shell.apkparser.utils.Buffers.readUInt(buffer);
                    }

                    com.library.dexknife.shell.apkparser.struct.resource.TypeSpec typeSpec = new com.library.dexknife.shell.apkparser.struct.resource.TypeSpec(typeSpecHeader);



                    typeSpec.setEntryFlags(entryFlags);
                    //id start from 1
                    typeSpec.setName(resourcePackage.getTypeStringPool()
                            .get(typeSpecHeader.getId() - 1));

                    resourcePackage.addTypeSpec(typeSpec);
                    buffer.position((int) (typeSpecChunkBegin + typeSpecHeader.getBodySize()));
                    break;
                case com.library.dexknife.shell.apkparser.struct.ChunkType.TABLE_TYPE:
                    long typeChunkBegin = buffer.position();
                    com.library.dexknife.shell.apkparser.struct.resource.TypeHeader typeHeader = (com.library.dexknife.shell.apkparser.struct.resource.TypeHeader) chunkHeader;
                    // read offsets table
                    long[] offsets = new long[(int) typeHeader.getEntryCount()];
                    for (int i = 0; i < typeHeader.getEntryCount(); i++) {
                        offsets[i] = com.library.dexknife.shell.apkparser.utils.Buffers.readUInt(buffer);
                    }

                    com.library.dexknife.shell.apkparser.struct.resource.Type type = new com.library.dexknife.shell.apkparser.struct.resource.Type(typeHeader);
                    type.setName(resourcePackage.getTypeStringPool().get(typeHeader.getId() - 1));
                    long entryPos = typeChunkBegin + typeHeader.getEntriesStart()
                            - typeHeader.getHeaderSize();
                    buffer.position((int) entryPos);
                    ByteBuffer b = buffer.slice();
                    b.order(byteOrder);
                    type.setBuffer(b);
                    type.setKeyStringPool(resourcePackage.getKeyStringPool());
                    type.setOffsets(offsets);
                    type.setStringPool(stringPool);
                    resourcePackage.addType(type);
                    locales.add(type.getLocale());
                    buffer.position((int) (typeChunkBegin + typeHeader.getBodySize()));
                    break;
                case com.library.dexknife.shell.apkparser.struct.ChunkType.TABLE_PACKAGE:
                    // another package. we should read next package here
                    pair.setRight((com.library.dexknife.shell.apkparser.struct.resource.PackageHeader) chunkHeader);
                    break outer;
                default:
                    throw new com.library.dexknife.shell.apkparser.exception.ParserException("unexpected chunk type:" + chunkHeader.getChunkType());
            }
        }

        return pair;

    }

    private ChunkHeader readChunkHeader() {
        long begin = buffer.position();

        int chunkType = com.library.dexknife.shell.apkparser.utils.Buffers.readUShort(buffer);
        int headerSize = com.library.dexknife.shell.apkparser.utils.Buffers.readUShort(buffer);
        long chunkSize = com.library.dexknife.shell.apkparser.utils.Buffers.readUInt(buffer);

        switch (chunkType) {
            case com.library.dexknife.shell.apkparser.struct.ChunkType.TABLE:
                com.library.dexknife.shell.apkparser.struct.resource.ResourceTableHeader resourceTableHeader = new com.library.dexknife.shell.apkparser.struct.resource.ResourceTableHeader(chunkType,
                        headerSize, chunkSize);
                resourceTableHeader.setPackageCount(com.library.dexknife.shell.apkparser.utils.Buffers.readUInt(buffer));
                buffer.position((int) (begin + headerSize));
                return resourceTableHeader;
            case com.library.dexknife.shell.apkparser.struct.ChunkType.STRING_POOL:
                StringPoolHeader stringPoolHeader = new StringPoolHeader(chunkType, headerSize,
                        chunkSize);
                stringPoolHeader.setStringCount(com.library.dexknife.shell.apkparser.utils.Buffers.readUInt(buffer));
                stringPoolHeader.setStyleCount(com.library.dexknife.shell.apkparser.utils.Buffers.readUInt(buffer));
                stringPoolHeader.setFlags(com.library.dexknife.shell.apkparser.utils.Buffers.readUInt(buffer));
                stringPoolHeader.setStringsStart(com.library.dexknife.shell.apkparser.utils.Buffers.readUInt(buffer));
                stringPoolHeader.setStylesStart(com.library.dexknife.shell.apkparser.utils.Buffers.readUInt(buffer));
                buffer.position((int) (begin + headerSize));
                return stringPoolHeader;
            case com.library.dexknife.shell.apkparser.struct.ChunkType.TABLE_PACKAGE:
                com.library.dexknife.shell.apkparser.struct.resource.PackageHeader packageHeader = new com.library.dexknife.shell.apkparser.struct.resource.PackageHeader(chunkType, headerSize, chunkSize);
                packageHeader.setId(com.library.dexknife.shell.apkparser.utils.Buffers.readUInt(buffer));
                packageHeader.setName(com.library.dexknife.shell.apkparser.utils.ParseUtils.readStringUTF16(buffer, 128));
                packageHeader.setTypeStrings(com.library.dexknife.shell.apkparser.utils.Buffers.readUInt(buffer));
                packageHeader.setLastPublicType(com.library.dexknife.shell.apkparser.utils.Buffers.readUInt(buffer));
                packageHeader.setKeyStrings(com.library.dexknife.shell.apkparser.utils.Buffers.readUInt(buffer));
                packageHeader.setLastPublicKey(com.library.dexknife.shell.apkparser.utils.Buffers.readUInt(buffer));
                buffer.position((int) (begin + headerSize));
                return packageHeader;
            case com.library.dexknife.shell.apkparser.struct.ChunkType.TABLE_TYPE_SPEC:
                com.library.dexknife.shell.apkparser.struct.resource.TypeSpecHeader typeSpecHeader = new com.library.dexknife.shell.apkparser.struct.resource.TypeSpecHeader(chunkType, headerSize, chunkSize);
                typeSpecHeader.setId(com.library.dexknife.shell.apkparser.utils.Buffers.readUByte(buffer));
                typeSpecHeader.setRes0(com.library.dexknife.shell.apkparser.utils.Buffers.readUByte(buffer));
                typeSpecHeader.setRes1(com.library.dexknife.shell.apkparser.utils.Buffers.readUShort(buffer));
                typeSpecHeader.setEntryCount(com.library.dexknife.shell.apkparser.utils.Buffers.readUInt(buffer));
                buffer.position((int) (begin + headerSize));
                return typeSpecHeader;
            case com.library.dexknife.shell.apkparser.struct.ChunkType.TABLE_TYPE:
                com.library.dexknife.shell.apkparser.struct.resource.TypeHeader typeHeader = new com.library.dexknife.shell.apkparser.struct.resource.TypeHeader(chunkType, headerSize, chunkSize);
                typeHeader.setId(com.library.dexknife.shell.apkparser.utils.Buffers.readUByte(buffer));
                typeHeader.setRes0(com.library.dexknife.shell.apkparser.utils.Buffers.readUByte(buffer));
                typeHeader.setRes1(com.library.dexknife.shell.apkparser.utils.Buffers.readUShort(buffer));
                typeHeader.setEntryCount(com.library.dexknife.shell.apkparser.utils.Buffers.readUInt(buffer));
                typeHeader.setEntriesStart(com.library.dexknife.shell.apkparser.utils.Buffers.readUInt(buffer));
                typeHeader.setConfig(readResTableConfig());
                buffer.position((int) (begin + headerSize));
                return typeHeader;
            case com.library.dexknife.shell.apkparser.struct.ChunkType.NULL:
                //buffer.skip((int) (chunkSize - headerSize));
            default:
                throw new com.library.dexknife.shell.apkparser.exception.ParserException("Unexpected chunk Type:" + Integer.toHexString(chunkType));
        }
    }

    private com.library.dexknife.shell.apkparser.struct.resource.ResTableConfig readResTableConfig() {
        long beginPos = buffer.position();
        com.library.dexknife.shell.apkparser.struct.resource.ResTableConfig config = new com.library.dexknife.shell.apkparser.struct.resource.ResTableConfig();
        long size = com.library.dexknife.shell.apkparser.utils.Buffers.readUInt(buffer);
        com.library.dexknife.shell.apkparser.utils.Buffers.skip(buffer, 4);
        //read locale
        config.setLanguage(new String(com.library.dexknife.shell.apkparser.utils.Buffers.readBytes(buffer, 2)).replace("\0", ""));
        config.setCountry(new String(com.library.dexknife.shell.apkparser.utils.Buffers.readBytes(buffer, 2)).replace("\0", ""));

        long endPos = buffer.position();
        com.library.dexknife.shell.apkparser.utils.Buffers.skip(buffer, (int) (size - (endPos - beginPos)));
        return config;
    }

    public com.library.dexknife.shell.apkparser.struct.resource.ResourceTable getResourceTable() {
        return resourceTable;
    }

    public Set<Locale> getLocales() {
        return this.locales;
    }
}
