package com.library.dexknife.shell.apkparser.parser;


import com.library.dexknife.shell.apkparser.struct.xml.XmlCData;
import com.library.dexknife.shell.apkparser.struct.xml.XmlNamespaceEndTag;
import com.library.dexknife.shell.apkparser.struct.xml.XmlNamespaceStartTag;
import com.library.dexknife.shell.apkparser.struct.xml.XmlNodeStartTag;

/**
 * callback interface for parse binary xml file.
 *
 * @author dongliu
 */
public interface XmlStreamer {

    void onStartTag(XmlNodeStartTag xmlNodeStartTag);

    void onEndTag(com.library.dexknife.shell.apkparser.struct.xml.XmlNodeEndTag xmlNodeEndTag);

    void onCData(XmlCData xmlCData);

    void onNamespaceStart(XmlNamespaceStartTag tag);

    void onNamespaceEnd(XmlNamespaceEndTag tag);
}
