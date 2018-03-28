package com.library.dexknife.shell.apkparser.parser;


import com.library.dexknife.shell.apkparser.bean.AttributeValues;
import com.library.dexknife.shell.apkparser.struct.ResourceEntity;
import com.library.dexknife.shell.apkparser.utils.Utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Android Binary XML format
 * see http://justanapplication.wordpress.com/category/android/android-binary-xml/
 *
 * @author dongliu
 */
public class BinaryXmlParser {


    /**
     * By default the data buffer Chunks is buffer little-endian byte order both at runtime and when stored buffer files.
     */
    private ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
    private com.library.dexknife.shell.apkparser.struct.StringPool stringPool;
    // some attribute name stored by resource id
    private String[] resourceMap;
    private ByteBuffer buffer;
    private XmlStreamer xmlStreamer;
    private final com.library.dexknife.shell.apkparser.struct.resource.ResourceTable resourceTable;
    /**
     * default locale.
     */
    private Locale locale = com.library.dexknife.shell.apkparser.bean.Locales.any;

    public BinaryXmlParser(ByteBuffer buffer, com.library.dexknife.shell.apkparser.struct.resource.ResourceTable resourceTable) {
        this.buffer = buffer.duplicate();
        this.buffer.order(byteOrder);
        this.resourceTable = resourceTable;
    }

    /**
     * Parse binary xml.
     */
    public void parse() {
        com.library.dexknife.shell.apkparser.struct.ChunkHeader chunkHeader = readChunkHeader();
        if (chunkHeader == null) {
            return;
        }
        if (chunkHeader.getChunkType() != com.library.dexknife.shell.apkparser.struct.ChunkType.XML) {
            //TODO: may be a plain xml file.
            return;
        }
        com.library.dexknife.shell.apkparser.struct.xml.XmlHeader xmlHeader = (com.library.dexknife.shell.apkparser.struct.xml.XmlHeader) chunkHeader;

        // read string pool chunk
        chunkHeader = readChunkHeader();
        if (chunkHeader == null) {
            return;
        }
        com.library.dexknife.shell.apkparser.utils.ParseUtils.checkChunkType(com.library.dexknife.shell.apkparser.struct.ChunkType.STRING_POOL, chunkHeader.getChunkType());
        stringPool = com.library.dexknife.shell.apkparser.utils.ParseUtils.readStringPool(buffer, (com.library.dexknife.shell.apkparser.struct.StringPoolHeader) chunkHeader);

        // read on chunk, check if it was an optional XMLResourceMap chunk
        chunkHeader = readChunkHeader();
        if (chunkHeader == null) {
            return;
        }
        if (chunkHeader.getChunkType() == com.library.dexknife.shell.apkparser.struct.ChunkType.XML_RESOURCE_MAP) {
            long[] resourceIds = readXmlResourceMap((com.library.dexknife.shell.apkparser.struct.xml.XmlResourceMapHeader) chunkHeader);
            resourceMap = new String[resourceIds.length];
            for (int i = 0; i < resourceIds.length; i++) {
                resourceMap[i] = com.library.dexknife.shell.apkparser.struct.xml.Attribute.AttrIds.getString(resourceIds[i]);
            }
            chunkHeader = readChunkHeader();
        }

        while (chunkHeader != null) {
                /*if (chunkHeader.chunkType == ChunkType.XML_END_NAMESPACE) {
                    break;
                }*/
            long beginPos = buffer.position();
            switch (chunkHeader.getChunkType()) {
                case com.library.dexknife.shell.apkparser.struct.ChunkType.XML_END_NAMESPACE:
                    com.library.dexknife.shell.apkparser.struct.xml.XmlNamespaceEndTag xmlNamespaceEndTag = readXmlNamespaceEndTag();
                    xmlStreamer.onNamespaceEnd(xmlNamespaceEndTag);
                    break;
                case com.library.dexknife.shell.apkparser.struct.ChunkType.XML_START_NAMESPACE:
                    com.library.dexknife.shell.apkparser.struct.xml.XmlNamespaceStartTag namespaceStartTag = readXmlNamespaceStartTag();
                    xmlStreamer.onNamespaceStart(namespaceStartTag);
                    break;
                case com.library.dexknife.shell.apkparser.struct.ChunkType.XML_START_ELEMENT:
                    com.library.dexknife.shell.apkparser.struct.xml.XmlNodeStartTag xmlNodeStartTag = readXmlNodeStartTag();
                    break;
                case com.library.dexknife.shell.apkparser.struct.ChunkType.XML_END_ELEMENT:
                    com.library.dexknife.shell.apkparser.struct.xml.XmlNodeEndTag xmlNodeEndTag = readXmlNodeEndTag();
                    break;
                case com.library.dexknife.shell.apkparser.struct.ChunkType.XML_CDATA:
                    com.library.dexknife.shell.apkparser.struct.xml.XmlCData xmlCData = readXmlCData();
                    break;
                default:
                    if (chunkHeader.getChunkType() >= com.library.dexknife.shell.apkparser.struct.ChunkType.XML_FIRST_CHUNK &&
                            chunkHeader.getChunkType() <= com.library.dexknife.shell.apkparser.struct.ChunkType.XML_LAST_CHUNK) {
                        com.library.dexknife.shell.apkparser.utils.Buffers.skip(buffer, chunkHeader.getBodySize());
                    } else {
                        throw new com.library.dexknife.shell.apkparser.exception.ParserException("Unexpected chunk type:" + chunkHeader.getChunkType());
                    }
            }
            buffer.position((int) (beginPos + chunkHeader.getBodySize()));
            chunkHeader = readChunkHeader();
        }
    }

    private com.library.dexknife.shell.apkparser.struct.xml.XmlCData readXmlCData() {
        com.library.dexknife.shell.apkparser.struct.xml.XmlCData xmlCData = new com.library.dexknife.shell.apkparser.struct.xml.XmlCData();
        int dataRef = buffer.getInt();
        if (dataRef > 0) {
            xmlCData.setData(stringPool.get(dataRef));
        }
        xmlCData.setTypedData(com.library.dexknife.shell.apkparser.utils.ParseUtils.readResValue(buffer, stringPool));
        if (xmlStreamer != null) {
            //TODO: to know more about cdata. some cdata appears buffer xml tags
//            String value = xmlCData.toStringValue(resourceTable, locale);
//            xmlCData.setValue(value);
//            xmlStreamer.onCData(xmlCData);
        }
        return xmlCData;
    }

    private com.library.dexknife.shell.apkparser.struct.xml.XmlNodeEndTag readXmlNodeEndTag() {
        com.library.dexknife.shell.apkparser.struct.xml.XmlNodeEndTag xmlNodeEndTag = new com.library.dexknife.shell.apkparser.struct.xml.XmlNodeEndTag();
        int nsRef = buffer.getInt();
        int nameRef = buffer.getInt();
        if (nsRef > 0) {
            xmlNodeEndTag.setNamespace(stringPool.get(nsRef));
        }
        xmlNodeEndTag.setName(stringPool.get(nameRef));
        if (xmlStreamer != null) {
            xmlStreamer.onEndTag(xmlNodeEndTag);
        }
        return xmlNodeEndTag;
    }

    private com.library.dexknife.shell.apkparser.struct.xml.XmlNodeStartTag readXmlNodeStartTag() {
        int nsRef = buffer.getInt();
        int nameRef = buffer.getInt();
        com.library.dexknife.shell.apkparser.struct.xml.XmlNodeStartTag xmlNodeStartTag = new com.library.dexknife.shell.apkparser.struct.xml.XmlNodeStartTag();
        if (nsRef > 0) {
            xmlNodeStartTag.setNamespace(stringPool.get(nsRef));
        }
        xmlNodeStartTag.setName(stringPool.get(nameRef));

        // read attributes.
        // attributeStart and attributeSize are always 20 (0x14)
        int attributeStart = com.library.dexknife.shell.apkparser.utils.Buffers.readUShort(buffer);
        int attributeSize = com.library.dexknife.shell.apkparser.utils.Buffers.readUShort(buffer);
        int attributeCount = com.library.dexknife.shell.apkparser.utils.Buffers.readUShort(buffer);
        int idIndex = com.library.dexknife.shell.apkparser.utils.Buffers.readUShort(buffer);
        int classIndex = com.library.dexknife.shell.apkparser.utils.Buffers.readUShort(buffer);
        int styleIndex = com.library.dexknife.shell.apkparser.utils.Buffers.readUShort(buffer);

        // read attributes
        com.library.dexknife.shell.apkparser.struct.xml.Attributes attributes = new com.library.dexknife.shell.apkparser.struct.xml.Attributes(attributeCount);
        for (int count = 0; count < attributeCount; count++) {
            com.library.dexknife.shell.apkparser.struct.xml.Attribute attribute = readAttribute();
            if (xmlStreamer != null) {
                String value = attribute.toStringValue(resourceTable, locale);
                if (intAttributes.contains(attribute.getName()) && Utils.isNumeric(value)) {
                    try {
                        value = getFinalValueAsString(attribute.getName(), value);
                    } catch (Exception ignore) {
                    }
                }
                attribute.setValue(value);
                attributes.set(count, attribute);
            }
        }
        xmlNodeStartTag.setAttributes(attributes);

        if (xmlStreamer != null) {
            xmlStreamer.onStartTag(xmlNodeStartTag);
        }

        return xmlNodeStartTag;
    }

    private static final Set<String> intAttributes = new HashSet<>(
            Arrays.asList("screenOrientation", "configChanges", "windowSoftInputMode",
                    "launchMode", "installLocation", "protectionLevel"));

    //trans int attr value to string
    private String getFinalValueAsString(String attributeName, String str) {
        int value = Integer.parseInt(str);
        switch (attributeName) {
            case "screenOrientation":
                return AttributeValues.getScreenOrientation(value);
            case "configChanges":
                return AttributeValues.getConfigChanges(value);
            case "windowSoftInputMode":
                return AttributeValues.getWindowSoftInputMode(value);
            case "launchMode":
                return AttributeValues.getLaunchMode(value);
            case "installLocation":
                return AttributeValues.getInstallLocation(value);
            case "protectionLevel":
                return AttributeValues.getProtectionLevel(value);
            default:
                return str;
        }
    }

    private com.library.dexknife.shell.apkparser.struct.xml.Attribute readAttribute() {
        int nsRef = buffer.getInt();
        int nameRef = buffer.getInt();
        com.library.dexknife.shell.apkparser.struct.xml.Attribute attribute = new com.library.dexknife.shell.apkparser.struct.xml.Attribute();
        if (nsRef > 0) {
            attribute.setNamespace(stringPool.get(nsRef));
        }

        attribute.setName(stringPool.get(nameRef));
        if (attribute.getName().isEmpty() && resourceMap != null && nameRef < resourceMap.length) {
            // some processed apk file make the string pool value empty, if it is a xmlmap attr.
            attribute.setName(resourceMap[nameRef]);
            //TODO: how to get the namespace of attribute
        }

        int rawValueRef = buffer.getInt();
        if (rawValueRef > 0) {
            attribute.setRawValue(stringPool.get(rawValueRef));
        }
        ResourceEntity resValue = com.library.dexknife.shell.apkparser.utils.ParseUtils.readResValue(buffer, stringPool);
        attribute.setTypedValue(resValue);

        return attribute;
    }

    private com.library.dexknife.shell.apkparser.struct.xml.XmlNamespaceStartTag readXmlNamespaceStartTag() {
        int prefixRef = buffer.getInt();
        int uriRef = buffer.getInt();
        com.library.dexknife.shell.apkparser.struct.xml.XmlNamespaceStartTag nameSpace = new com.library.dexknife.shell.apkparser.struct.xml.XmlNamespaceStartTag();
        if (prefixRef > 0) {
            nameSpace.setPrefix(stringPool.get(prefixRef));
        }
        if (uriRef > 0) {
            nameSpace.setUri(stringPool.get(uriRef));
        }
        return nameSpace;
    }

    private com.library.dexknife.shell.apkparser.struct.xml.XmlNamespaceEndTag readXmlNamespaceEndTag() {
        int prefixRef = buffer.getInt();
        int uriRef = buffer.getInt();
        com.library.dexknife.shell.apkparser.struct.xml.XmlNamespaceEndTag nameSpace = new com.library.dexknife.shell.apkparser.struct.xml.XmlNamespaceEndTag();
        if (prefixRef > 0) {
            nameSpace.setPrefix(stringPool.get(prefixRef));
        }
        if (uriRef > 0) {
            nameSpace.setUri(stringPool.get(uriRef));
        }
        return nameSpace;
    }

    private long[] readXmlResourceMap(com.library.dexknife.shell.apkparser.struct.xml.XmlResourceMapHeader chunkHeader) {
        int count = chunkHeader.getBodySize() / 4;
        long[] resourceIds = new long[count];
        for (int i = 0; i < count; i++) {
            resourceIds[i] = com.library.dexknife.shell.apkparser.utils.Buffers.readUInt(buffer);
        }
        return resourceIds;
    }


    private com.library.dexknife.shell.apkparser.struct.ChunkHeader readChunkHeader() {
        // finished
        if (!buffer.hasRemaining()) {
            return null;
        }

        long begin = buffer.position();
        int chunkType = com.library.dexknife.shell.apkparser.utils.Buffers.readUShort(buffer);
        int headerSize = com.library.dexknife.shell.apkparser.utils.Buffers.readUShort(buffer);
        long chunkSize = com.library.dexknife.shell.apkparser.utils.Buffers.readUInt(buffer);

        switch (chunkType) {
            case com.library.dexknife.shell.apkparser.struct.ChunkType.XML:
                return new com.library.dexknife.shell.apkparser.struct.xml.XmlHeader(chunkType, headerSize, chunkSize);
            case com.library.dexknife.shell.apkparser.struct.ChunkType.STRING_POOL:
                com.library.dexknife.shell.apkparser.struct.StringPoolHeader stringPoolHeader = new com.library.dexknife.shell.apkparser.struct.StringPoolHeader(chunkType, headerSize, chunkSize);
                stringPoolHeader.setStringCount(com.library.dexknife.shell.apkparser.utils.Buffers.readUInt(buffer));
                stringPoolHeader.setStyleCount(com.library.dexknife.shell.apkparser.utils.Buffers.readUInt(buffer));
                stringPoolHeader.setFlags(com.library.dexknife.shell.apkparser.utils.Buffers.readUInt(buffer));
                stringPoolHeader.setStringsStart(com.library.dexknife.shell.apkparser.utils.Buffers.readUInt(buffer));
                stringPoolHeader.setStylesStart(com.library.dexknife.shell.apkparser.utils.Buffers.readUInt(buffer));
                buffer.position((int) (begin + headerSize));
                return stringPoolHeader;
            case com.library.dexknife.shell.apkparser.struct.ChunkType.XML_RESOURCE_MAP:
                buffer.position((int) (begin + headerSize));
                return new com.library.dexknife.shell.apkparser.struct.xml.XmlResourceMapHeader(chunkType, headerSize, chunkSize);
            case com.library.dexknife.shell.apkparser.struct.ChunkType.XML_START_NAMESPACE:
            case com.library.dexknife.shell.apkparser.struct.ChunkType.XML_END_NAMESPACE:
            case com.library.dexknife.shell.apkparser.struct.ChunkType.XML_START_ELEMENT:
            case com.library.dexknife.shell.apkparser.struct.ChunkType.XML_END_ELEMENT:
            case com.library.dexknife.shell.apkparser.struct.ChunkType.XML_CDATA:
                com.library.dexknife.shell.apkparser.struct.xml.XmlNodeHeader header = new com.library.dexknife.shell.apkparser.struct.xml.XmlNodeHeader(chunkType, headerSize, chunkSize);
                header.setLineNum((int) com.library.dexknife.shell.apkparser.utils.Buffers.readUInt(buffer));
                header.setCommentRef((int) com.library.dexknife.shell.apkparser.utils.Buffers.readUInt(buffer));
                buffer.position((int) (begin + headerSize));
                return header;
            case com.library.dexknife.shell.apkparser.struct.ChunkType.NULL:
                //buffer.advanceTo(begin + headerSize);
                //buffer.skip((int) (chunkSize - headerSize));
            default:
                throw new com.library.dexknife.shell.apkparser.exception.ParserException("Unexpected chunk type:" + chunkType);
        }
    }

    public void setLocale(Locale locale) {
        if (locale != null) {
            this.locale = locale;
        }
    }

    public Locale getLocale() {
        return locale;
    }

    public XmlStreamer getXmlStreamer() {
        return xmlStreamer;
    }

    public void setXmlStreamer(XmlStreamer xmlStreamer) {
        this.xmlStreamer = xmlStreamer;
    }
}
