import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;

public class ZipTarDirect {
	static int files = 0;
	static int directories = 0;
	static long size = 0;

	public static void main(String[] args) throws Exception {
		try (var log = new PrintWriter(
				ZonedDateTime.now().format(DateTimeFormatter.ofPattern("uuuuMMddHHmm")) + ".txt")) {
			try {
				Files.list(Path.of("")).map(path -> path.toString()).filter(filename -> filename.endsWith(".zip"))
						.forEach(zip -> {
							log.println(zip);
							var tar = zip.substring(0, zip.length() - 3) + "tar";
							log.println(tar);
							try (var zf = new ZipFile(zip);
									var tf = new DataOutputStream(
											new BufferedOutputStream(new FileOutputStream(tar)))) {
								zf.stream().forEach(entry -> {
									log.println(entry);
									try (var is = zf.getInputStream(entry);
											var baos = new ByteArrayOutputStream();
											var dos = new DataOutputStream(baos)) {
										var name = entry.getName();
										
										if(name.endsWith(".xml"))
											name=name.substring(0,name.length()-4)+".dzi";
										
										dos.writeBytes(name);
										for (var i = name.length(); i < 100; i++)
											dos.write(0);
//										dos.writeBytes(entry.isDirectory()?"0040777":"0100777");
										if (entry.isDirectory()) {
											dos.writeBytes("0040777");
											directories++;
										} else {
											dos.writeBytes("0100777");
											files++;
											size += entry.getSize();
										}
										dos.writeByte(0);
										dos.writeBytes("0000000");
										dos.writeByte(0);
										dos.writeBytes("0000000");
										dos.writeByte(0);
										dos.writeBytes(String.format("%011o", entry.getSize()));
										dos.writeByte(0);
										dos.writeBytes(
												String.format("%011o", entry.getLastModifiedTime().toMillis() / 1000));
										dos.writeByte(0);
										var hdr1 = baos.toByteArray();
										baos.reset();
										dos.writeByte(entry.isDirectory() ? '5' : '0');
										for (var i = 0; i < 100; i++)
											dos.write(0);
										dos.writeBytes("ustar");
										dos.writeByte(0);
										dos.writeBytes("00");
										for (var i = 0; i < 32 + 32 + 8 + 8 + 155 + 12; i++)
											dos.write(0);
										var hdr2 = baos.toByteArray();
										var sum = 8 * ' ';
										for (var b : hdr1)
											sum += b;
										for (var b : hdr2)
											sum += b;
										tf.write(hdr1);
										tf.writeBytes(String.format("%06o", sum));
										tf.writeByte(0);
										tf.writeByte(' ');
										tf.write(hdr2);
										is.transferTo(tf);
										for (var pad = entry.getSize(); pad % 512 != 0; pad++)
											tf.write(0);
									} catch (Exception ex) {
										ex.printStackTrace(log);
									}
								});
								for (var pad = 0; pad < 1024; pad++)
									tf.write(0);
								log.println();
							} catch (Exception ex) {
								ex.printStackTrace(log);
							}
						});
				log.println("Files: " + files);
				log.println("Size:  " + size);
				log.println("Dirs:  " + directories);
				log.println();
				log.println("Log completed");
			} catch (Exception ex) {
				ex.printStackTrace(log);
			}
		}
	}
}
