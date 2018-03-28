package com.library.dexknife.shell.utils;


import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class DataProtector {

	private static final int BUFF_SIZE = 1024*1024*5; // 10MB

	public static byte[] encryptXXTEA(byte[] data){
		return XXTEA.encrypt(data,"lcl_apktoolplus");
	}

    @Deprecated
	public static byte[] encrypt(byte[] data){
		String key = "linchaolong";
		int keyLen = key.length();
		int size = data.length;
		
		 int i = 0;
		 int offset = 0;
		 for(; i<size; ++i, ++offset){
			 if (offset >= keyLen){
				offset = 0;
			}
			 data[i] ^= key.charAt(offset);
		 }
		 
		 return data;
	}

	/**
	 * 加密文件
	 * @param file
	 * @param outFile
	 */
	public static void encrypt(File file, File outFile){
		if (!com.library.dexknife.shell.utils.FileHelper.exists(file)){
			com.library.dexknife.shell.utils.Debug.e("file not exists!!! : " + file.getAbsolutePath());
			return;
		}
		try {


//			ByteArrayOutputStream byteOutput=new ByteArrayOutputStream();
//			FileInputStream fis = new FileInputStream(file);
//			byte[] bytes = new byte[1024];
//			int n;
//			while ((n = fis.read(bytes)) != -1) {
//				byteOutput.write(bytes, 0, n);
//			}
//			fis.close();
//			byteOutput.close();
//			FileOutputStream out=new FileOutputStream(outFile) ;
//			byte[] encryptData = encryptXXTEA(byteOutput.toByteArray());
//			out.write(encryptData);
//			out.flush();



			File file1=new File(file.getParent(),outFile.getName());
			FileHelper.copy(file,file1);
			file.delete();
			ZipFile zipFile=new ZipFile(outFile.getAbsolutePath());
			ZipParameters parameters = new ZipParameters();
			parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
			parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);

			// Set password
			parameters.setEncryptFiles(true);
			parameters.setEncryptionMethod(Zip4jConstants.ENC_METHOD_AES);
			parameters.setAesKeyStrength(Zip4jConstants.AES_STRENGTH_256);
			parameters.setPassword("libprotected");
			zipFile.addFile(file1,parameters);
			file1.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

	/**
	 * 写入临时文件
	 *
	 * @throws IOException
	 */
	private static void writeTemp(byte[] bytes, OutputStream outputStream) throws IOException {
		byte[] dexBytes = bytes;
		outputStream.write(dexBytes, 0, dexBytes.length);
		outputStream.flush();
		outputStream.close();
	}
}
